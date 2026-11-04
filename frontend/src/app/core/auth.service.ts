import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { AuthResponse, RegisterPayload, User } from './models';

const STORAGE_KEY = 'devpulse.session';

interface Session {
  token: string;
  user: User;
}

function readSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }

    const stored = JSON.parse(raw) as Session & { refreshToken?: string };
    const session: Session = { token: stored.token, user: stored.user };
    if (stored.refreshToken) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    }
    return session;
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<Session | null>(readSession());
  private refreshRequest: Observable<AuthResponse> | null = null;

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
    if (this.refreshRequest) {
      return this.refreshRequest;
    }

    if (!this.session()) {
      this.logout();
      return throwError(() => new Error('No refresh token available'));
    }

    const request = this.http.post<AuthResponse>('/api/auth/refresh', null, { withCredentials: true }).pipe(
      tap((res) => this.store(res)),
      catchError((error) => {
        this.logout();
        return throwError(() => error);
      }),
      finalize(() => {
        this.refreshRequest = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    this.refreshRequest = request;
    return request;
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password }, { withCredentials: true }).pipe(
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
    const hadSession = this.session() !== null;
    this.session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // storage unavailable: nothing to clean
    }
    if (hadSession) {
      this.http.post<void>('/api/auth/logout', null, { withCredentials: true }).subscribe({
        error: () => console.warn('Could not revoke the refresh token on the server.')
      });
    }
    void this.router.navigateByUrl('/login');
  }

  updateUser(user: User): void {
    const current = this.session();
    if (!current) return;
    const next = { ...current, user };
    this.session.set(next);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    } catch {
      // The in-memory session still reflects the updated profile.
    }
  }

  private store(res: AuthResponse): void {
    const session: Session = { token: res.token, user: res.user };
    this.session.set(session);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // storage unavailable: session stays in memory only
    }
  }
}
