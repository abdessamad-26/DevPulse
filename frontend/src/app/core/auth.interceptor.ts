import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const isAuthEndpoint = (url: string): boolean => url.startsWith('/api/auth/');

const withBearer = (req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> =>
  token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const isProtected = req.url.startsWith('/api/') && !isAuthEndpoint(req.url);

  const outgoing = isProtected ? withBearer(req, auth.token()) : req;

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || !isProtected) {
        return throwError(() => error);
      }

      if (error.status === 401 && auth.user()) {
        // refreshToken() already logs the user out when the refresh itself fails,
        // so only the retried request needs handling below.
        return auth.refreshToken().pipe(
          switchMap(() =>
            next(withBearer(req, auth.token())).pipe(
              catchError((retryError: unknown) => {
                // A 401 right after a successful refresh means the session is unusable.
                // Any other failure (403, 404, 5xx, network) belongs to the original
                // call and must NOT sign the user out.
                if (retryError instanceof HttpErrorResponse && retryError.status === 401) {
                  auth.logout();
                }
                return throwError(() => retryError);
              })
            )
          )
        );
      }

      if (error.status === 401) {
        auth.logout();
      }

      return throwError(() => error);
    })
  );
};
