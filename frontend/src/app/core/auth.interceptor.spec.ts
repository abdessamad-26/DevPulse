import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthResponse } from './models';

const SESSION_KEY = 'devpulse.session';

const user: AuthResponse['user'] = {
  id: 1,
  email: 'user@example.com',
  firstName: 'Test',
  lastName: 'User',
  role: 'DEVELOPER'
};

const refreshedSession: AuthResponse = {
  token: 'new-access-token',
  refreshToken: 'new-refresh-token',
  user
};

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.setItem(SESSION_KEY, JSON.stringify({
      token: 'access-token',
      refreshToken: 'refresh-token',
      user
    }));

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Fails the test if an unexpected request was made (for example a logout call).
    httpTesting.verify();
    localStorage.removeItem(SESSION_KEY);
  });

  it('adds the bearer token to protected API calls but not to auth endpoints', () => {
    http.get('/api/projects').subscribe();
    const protectedRequest = httpTesting.expectOne('/api/projects');
    expect(protectedRequest.request.headers.get('Authorization')).toBe('Bearer access-token');
    protectedRequest.flush([]);

    http.post('/api/auth/login', {}).subscribe();
    const loginRequest = httpTesting.expectOne('/api/auth/login');
    expect(loginRequest.request.headers.has('Authorization')).toBe(false);
    loginRequest.flush({});
  });

  it('refreshes the token and retries the request once after a 401', () => {
    const results: unknown[] = [];
    http.get('/api/projects').subscribe((body) => results.push(body));

    httpTesting.expectOne('/api/projects')
      .flush({ error: 'expired' }, { status: 401, statusText: 'Unauthorized' });
    httpTesting.expectOne('/api/auth/refresh').flush(refreshedSession);

    const retry = httpTesting.expectOne('/api/projects');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer new-access-token');
    retry.flush([{ id: 1 }]);

    expect(results).toEqual([[{ id: 1 }]]);
  });

  it('does not sign the user out when the retried request fails with a non-auth error', () => {
    let failedStatus: number | undefined;
    http.get('/api/projects').subscribe({
      error: (error: HttpErrorResponse) => {
        failedStatus = error.status;
      }
    });

    httpTesting.expectOne('/api/projects')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    httpTesting.expectOne('/api/auth/refresh').flush(refreshedSession);
    httpTesting.expectOne('/api/projects')
      .flush({}, { status: 500, statusText: 'Internal Server Error' });

    expect(failedStatus).toBe(500);
    // The (rotated) session must still be stored: no logout happened.
    const stored = JSON.parse(localStorage.getItem(SESSION_KEY) ?? 'null');
    expect(stored?.refreshToken).toBe('new-refresh-token');
  });
});
