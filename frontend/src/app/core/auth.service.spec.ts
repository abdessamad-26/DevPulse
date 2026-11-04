import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthResponse } from './models';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let authService: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.setItem('devpulse.session', JSON.stringify({
      token: 'access-token',
      refreshToken: 'refresh-token',
      user: { id: 1, email: 'user@example.com', firstName: 'Test', lastName: 'User', role: 'DEVELOPER' }
    }));

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });
    authService = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.removeItem('devpulse.session');
  });

  it('shares one server refresh request between concurrent callers', () => {
    const firstResults: AuthResponse[] = [];
    const secondResults: AuthResponse[] = [];

    authService.refreshToken().subscribe((response) => firstResults.push(response));
    authService.refreshToken().subscribe((response) => secondResults.push(response));

    const request = httpTesting.expectOne('/api/auth/refresh');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    expect(request.request.withCredentials).toBe(true);

    const response: AuthResponse = {
      token: 'rotated-access-token',
      user: { id: 1, email: 'user@example.com', firstName: 'Test', lastName: 'User', role: 'DEVELOPER' }
    };
    request.flush(response);

    expect(firstResults).toEqual([response]);
    expect(secondResults).toEqual([response]);
    expect(JSON.parse(localStorage.getItem('devpulse.session') ?? '{}'))
      .toEqual({ token: 'rotated-access-token', user: response.user });
  });

  it('sends login credentials with cookies enabled and does not persist a refresh token', () => {
    authService.login('user@example.com', 'password').subscribe();

    const request = httpTesting.expectOne('/api/auth/login');
    expect(request.request.withCredentials).toBe(true);
    request.flush({
      token: 'new-access-token',
      user: { id: 1, email: 'user@example.com', firstName: 'Test', lastName: 'User', role: 'DEVELOPER' }
    });

    expect(JSON.parse(localStorage.getItem('devpulse.session') ?? '{}'))
      .not.toHaveProperty('refreshToken');
  });

  it('removes an existing refresh token from the persisted session', () => {
    expect(JSON.parse(localStorage.getItem('devpulse.session') ?? '{}'))
      .toEqual({
        token: 'access-token',
        user: { id: 1, email: 'user@example.com', firstName: 'Test', lastName: 'User', role: 'DEVELOPER' }
      });
  });
});
