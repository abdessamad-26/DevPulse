import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const isAuthEndpoint = (url: string): boolean => url.startsWith('/api/auth/');

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const isProtected = req.url.startsWith('/api/') && !isAuthEndpoint(req.url);

  const outgoing = token && isProtected
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || !isProtected) {
        return throwError(() => error);
      }

      if (error.status === 401 && auth.user()) {
        return auth.refreshToken().pipe(
          switchMap(() => {
            const retryToken = auth.token();
            const retryReq = retryToken
              ? req.clone({ setHeaders: { Authorization: `Bearer ${retryToken}` } })
              : req;
            return next(retryReq);
          }),
          catchError((refreshError) => {
            auth.logout();
            return throwError(() => refreshError);
          })
        );
      }

      if (error.status === 401) {
        auth.logout();
      }

      return throwError(() => error);
    })
  );
};
