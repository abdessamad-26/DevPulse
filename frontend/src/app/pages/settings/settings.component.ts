import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Settings</h1>
          <p>Account and workspace information.</p>
        </div>
      </header>

      <div class="card" *ngIf="auth.user() as user">
        <h3>Account</h3>
        <div class="form-grid two-col">
          <div>
            <span class="muted" style="font-size:.8rem;">Name</span>
            <p style="margin:4px 0 0;">{{ user.firstName }} {{ user.lastName }}</p>
          </div>
          <div>
            <span class="muted" style="font-size:.8rem;">Email</span>
            <p style="margin:4px 0 0;">{{ user.email }}</p>
          </div>
          <div>
            <span class="muted" style="font-size:.8rem;">Role</span>
            <p style="margin:4px 0 0;"><span class="badge" [class]="'badge-' + (user.role || 'neutral').toLowerCase()">{{ user.role }}</span></p>
          </div>
        </div>
        <div class="row" style="margin-top:20px;">
          <button type="button" class="btn btn-danger" (click)="auth.logout()">Log out</button>
        </div>
      </div>

      <div class="card">
        <h3>Roadmap</h3>
        <p class="muted">
          Admin-only user management (<code>/api/users</code>) and self-service password changes are not
          implemented yet — the backend currently issues the DEVELOPER role to every new registration and has
          no admin endpoints for account management.
        </p>
      </div>
    </section>
  `,
})
export class SettingsComponent {
  readonly auth = inject(AuthService);
}
