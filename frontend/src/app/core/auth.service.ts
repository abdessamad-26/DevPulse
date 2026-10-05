import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, of, switchMap, tap, throwError } from 'rxjs';
import { AuthResponse, RegisterPayload, User } from './models';

const STORAGE_KEY = 'devpulse.session';

interface Session {
  token: string;
  refreshToken: string;
  user: User;
}

function readSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<Session | null>(readSession());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly canWrite = computed(() => {
    const role = this.user()?.role;
    return role === 'ADMIN' || role === 'DEVELOPER';
  });

  token(): string | null {
    return this.session()?.token ?? null;
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.session()?.refreshToken;
    if (!refreshToken) {
      this.logout();
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http.post<AuthResponse>('/api/auth/refresh', { refreshToken }).pipe(
      tap((res) => this.store(res)),
      catchError((error) => {
        this.logout();
        return throwError(() => error);
      })
    );
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password }).pipe(
      tap((res) => this.store(res))
    );
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<{ id: number; email: string; firstName: string; lastName: string; role: string | null }>(
      '/api/auth/register',
      payload
    ).pipe(
      switchMap(() => this.login(payload.email, payload.password))
    );
  }

  logout(): void {
    this.session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // storage unavailable: nothing to clean
    }
    void this.router.navigateByUrl('/login');
  }

  private store(res: AuthResponse): void {
    const session: Session = { token: res.token, refreshToken: res.refreshToken, user: res.user };
    this.session.set(session);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // storage unavailable: session stays in memory only
    }
  }
}
