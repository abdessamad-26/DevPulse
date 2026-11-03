import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AdminUser, RoleName } from '../../core/models';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Settings</h1>
          <p>Manage your account and, if you are an administrator, user access.</p>
        </div>
      </header>

      <div class="card">
        <h3>Profile</h3>
        <form (ngSubmit)="saveProfile()" #profileForm="ngForm">
          <div class="form-grid two-col">
            <label class="field">
              <span>First name</span>
              <input type="text" [(ngModel)]="firstName" name="firstName" maxlength="100" required />
            </label>
            <label class="field">
              <span>Last name</span>
              <input type="text" [(ngModel)]="lastName" name="lastName" maxlength="100" required />
            </label>
            <label class="field">
              <span>Email</span>
              <input type="email" [value]="auth.user()?.email || ''" readonly />
            </label>
            <div class="field">
              <span>Role</span>
              <p class="role-value"><span class="badge" [class]="'badge-' + (auth.user()?.role || 'neutral').toLowerCase()">{{ auth.user()?.role }}</span></p>
            </div>
          </div>
          <div class="row actions">
            <button class="btn btn-primary" type="submit" [disabled]="!profileForm.valid || profileBusy()">Save profile</button>
            <button type="button" class="btn btn-danger" (click)="auth.logout()">Log out</button>
          </div>
          <p class="error-state" *ngIf="profileError()" role="alert">{{ profileError() }}</p>
          <p class="success-state" *ngIf="profileSuccess()" role="status">{{ profileSuccess() }}</p>
        </form>
      </div>

      <div class="card">
        <h3>Change password</h3>
        <p class="muted">Use your current password to confirm this change. Your other signed-in sessions will be revoked.</p>
        <form (ngSubmit)="changePassword()" #passwordForm="ngForm">
          <div class="form-grid two-col">
            <label class="field">
              <span>Current password</span>
              <input type="password" [(ngModel)]="currentPassword" name="currentPassword" autocomplete="current-password" required />
            </label>
            <span></span>
            <label class="field">
              <span>New password</span>
              <input type="password" [(ngModel)]="newPassword" name="newPassword" autocomplete="new-password" minlength="12" maxlength="72" required />
              <small class="muted">At least 12 characters and no more than 72 UTF-8 bytes.</small>
            </label>
            <label class="field">
              <span>Confirm new password</span>
              <input type="password" [(ngModel)]="confirmPassword" name="confirmPassword" autocomplete="new-password" minlength="12" maxlength="72" required />
            </label>
          </div>
          <p class="error-state" *ngIf="newPassword && confirmPassword && newPassword !== confirmPassword">Passwords do not match.</p>
          <div class="row actions">
            <button class="btn btn-primary" type="submit" [disabled]="!passwordForm.valid || newPassword !== confirmPassword || passwordBusy()">Change password</button>
            <button class="btn" type="button" *ngIf="passwordSuccess()" (click)="auth.logout()">Log out and sign in again</button>
          </div>
          <p class="error-state" *ngIf="passwordError()" role="alert">{{ passwordError() }}</p>
          <p class="success-state" *ngIf="passwordSuccess()" role="status">{{ passwordSuccess() }}</p>
        </form>
      </div>

      <div class="card" *ngIf="auth.user()?.role === 'ADMIN'">
        <div class="row-between">
          <div>
            <h3>User administration</h3>
            <p class="muted">Change global roles or disable an account. You cannot change your own admin access or remove the last active administrator.</p>
          </div>
          <button class="btn btn-sm" type="button" (click)="loadUsers(userPage())">Refresh</button>
        </div>
        <p class="error-state" *ngIf="adminError()" role="alert">{{ adminError() }}</p>
        <div class="card muted" *ngIf="adminLoading()" role="status">Loading users…</div>
        <div class="table-wrap" *ngIf="!adminLoading() && adminUsers().length">
          <table class="data">
            <thead><tr><th>User</th><th>Role</th><th>Status</th><th>Created</th><th>Actions</th></tr></thead>
            <tbody>
              @for (user of adminUsers(); track user.id) {
                <tr>
                  <td><strong>{{ user.firstName }} {{ user.lastName }}</strong><br /><span class="muted">{{ user.email }}</span></td>
                  <td>
                    <select [ngModel]="user.role" [name]="'role-' + user.id" [disabled]="isCurrentAdmin(user) || adminBusy()" (ngModelChange)="updateUser(user, $event, user.enabled)">
                      <option value="ADMIN">ADMIN</option>
                      <option value="DEVELOPER">DEVELOPER</option>
                      <option value="VIEWER">VIEWER</option>
                    </select>
                  </td>
                  <td><span class="badge" [class]="user.enabled ? 'badge-healthy' : 'badge-neutral'">{{ user.enabled ? 'Enabled' : 'Disabled' }}</span></td>
                  <td>{{ user.createdAt | date:'short' }}</td>
                  <td>
                    <button class="btn btn-sm" type="button" [disabled]="isCurrentAdmin(user) || adminBusy()" (click)="updateUser(user, user.role, !user.enabled)">
                      {{ user.enabled ? 'Disable' : 'Enable' }}
                    </button>
                    <span class="muted" *ngIf="isCurrentAdmin(user)">You</span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
          <div class="row-between pager">
            <button class="btn btn-sm" type="button" [disabled]="userPage() === 0 || adminBusy()" (click)="loadUsers(userPage() - 1)">Previous</button>
            <span class="muted">Page {{ userPage() + 1 }} of {{ Math.max(userTotalPages(), 1) }}</span>
            <button class="btn btn-sm" type="button" [disabled]="userPage() + 1 >= userTotalPages() || adminBusy()" (click)="loadUsers(userPage() + 1)">Next</button>
          </div>
        </div>
        <div class="empty-state" *ngIf="!adminLoading() && !adminUsers().length">No users found.</div>
      </div>

      <div class="card" *ngIf="auth.user()?.role !== 'ADMIN'">
        <h3>Project access</h3>
        <p class="muted">Manage project members, ingestion keys, and audit history from the Access page.</p>
      </div>
    </section>
  `,
  styles: [`
    .actions { margin-top: 18px; }
    .role-value { margin: 0; }
    .pager { margin-top: 14px; }
  `],
})
export class SettingsComponent {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  readonly Math = Math;

  readonly profileBusy = signal(false);
  readonly profileError = signal<string | null>(null);
  readonly profileSuccess = signal<string | null>(null);
  readonly passwordBusy = signal(false);
  readonly passwordError = signal<string | null>(null);
  readonly passwordSuccess = signal<string | null>(null);
  readonly adminUsers = signal<AdminUser[]>([]);
  readonly adminLoading = signal(false);
  readonly adminBusy = signal(false);
  readonly adminError = signal<string | null>(null);
  readonly userPage = signal(0);
  readonly userTotalPages = signal(0);

  firstName = this.auth.user()?.firstName ?? '';
  lastName = this.auth.user()?.lastName ?? '';
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';

  constructor() {
    if (this.auth.user()?.role === 'ADMIN') this.loadUsers(0);
  }

  saveProfile(): void {
    this.profileBusy.set(true);
    this.profileError.set(null);
    this.profileSuccess.set(null);
    this.api.updateProfile({ firstName: this.firstName.trim(), lastName: this.lastName.trim() }).subscribe({
      next: (user) => {
        this.auth.updateUser(user);
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.profileBusy.set(false);
        this.profileSuccess.set('Profile updated.');
      },
      error: (error) => {
        this.profileBusy.set(false);
        this.profileError.set(this.errorMessage(error, 'Could not update your profile.'));
      },
    });
  }

  changePassword(): void {
    this.passwordBusy.set(true);
    this.passwordError.set(null);
    this.passwordSuccess.set(null);
    this.api.changePassword({ currentPassword: this.currentPassword, newPassword: this.newPassword }).subscribe({
      next: () => {
        this.passwordBusy.set(false);
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.passwordSuccess.set('Password changed. Your other sessions were revoked. Sign in again to start a new session.');
      },
      error: (error) => {
        this.passwordBusy.set(false);
        this.passwordError.set(this.errorMessage(error, 'Could not change your password.'));
      },
    });
  }

  loadUsers(page: number): void {
    this.adminLoading.set(true);
    this.adminError.set(null);
    this.api.adminUsers(page).subscribe({
      next: (response) => {
        this.adminUsers.set(response.content);
        this.userPage.set(response.number);
        this.userTotalPages.set(response.totalPages);
        this.adminLoading.set(false);
      },
      error: (error) => {
        this.adminLoading.set(false);
        this.adminError.set(this.errorMessage(error, 'Could not load users.'));
      },
    });
  }

  updateUser(user: AdminUser, role: RoleName, enabled: boolean): void {
    if (this.isCurrentAdmin(user)) return;
    this.adminBusy.set(true);
    this.adminError.set(null);
    this.api.updateAdminUser(user.id, { role, enabled }).subscribe({
      next: (updated) => {
        this.adminUsers.update((users) => users.map((item) => item.id === updated.id ? updated : item));
        this.adminBusy.set(false);
      },
      error: (error) => {
        this.adminBusy.set(false);
        this.adminError.set(this.errorMessage(error, 'Could not update this user.'));
      },
    });
  }

  isCurrentAdmin(user: AdminUser): boolean {
    return user.id === this.auth.user()?.id;
  }

  private errorMessage(error: unknown, fallback: string): string {
    const body = error instanceof HttpErrorResponse ? error.error : null;
    if (!body || typeof body !== 'object') return fallback;
    if ('message' in body && typeof body.message === 'string') return body.message;
    if ('error' in body && typeof body.error === 'string') return body.error;
    return fallback;
  }
}
