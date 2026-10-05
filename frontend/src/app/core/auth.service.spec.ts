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
    expect(request.request.body).toEqual({ refreshToken: 'refresh-token' });

    const response: AuthResponse = {
      token: 'rotated-access-token',
      refreshToken: 'rotated-refresh-token',
      user: { id: 1, email: 'user@example.com', firstName: 'Test', lastName: 'User', role: 'DEVELOPER' }
    };
    request.flush(response);

    expect(firstResults).toEqual([response]);
    expect(secondResults).toEqual([response]);
    expect(JSON.parse(localStorage.getItem('devpulse.session') ?? '{}').refreshToken)
      .toBe('rotated-refresh-token');
  });
});
