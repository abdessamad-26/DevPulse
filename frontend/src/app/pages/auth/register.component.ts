import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="auth-card">
      <div class="brand">
        <span class="brand-mark">D</span>
        <div>
          <strong>DevPulse</strong>
          <small>Create account</small>
        </div>
      </div>

      <h1>Register</h1>
      <p>Set up a new operations workspace.</p>

      <form (ngSubmit)="submit()" novalidate>
        <div class="split">
          <label>
            <span>First name</span>
            <input type="text" [(ngModel)]="firstName" name="firstName" required />
          </label>
          <label>
            <span>Last name</span>
            <input type="text" [(ngModel)]="lastName" name="lastName" required />
          </label>
        </div>

        <label>
          <span>Email</span>
          <input type="email" [(ngModel)]="email" name="email" required />
        </label>

        <label>
          <span>Password</span>
          <input type="password" [(ngModel)]="password" name="password" required />
        </label>

        <button type="submit" class="primary">Create account</button>
      </form>

      <p class="muted">Already have an account? <a routerLink="/login">Sign in</a></p>
    </section>
  `,
  styles: [
    `
      :host { display: grid; place-items: center; min-height: 100vh; }
      .auth-card { width: min(520px, calc(100vw - 32px)); background: rgba(15,23,42,0.95); border: 1px solid rgba(148,163,184,0.12); border-radius: 20px; padding: 28px; box-shadow: 0 20px 60px rgba(2,6,23,0.5); }
      .brand { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
      .brand-mark { width: 42px; height: 42px; border-radius: 12px; display: grid; place-items: center; background: linear-gradient(135deg, #38bdf8, #8b5cf6); font-weight: 800; }
      .brand strong, .brand small { display: block; }
      .brand small { color: #94a3b8; }
      h1 { margin: 0; font-size: 2rem; }
      p { color: #cbd5e1; }
      form { display: flex; flex-direction: column; gap: 18px; margin-top: 20px; }
      .split { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
      label { display: flex; flex-direction: column; gap: 8px; font-weight: 600; }
      input { background: rgba(15,23,42,0.8); border: 1px solid rgba(148,163,184,0.2); border-radius: 10px; padding: 12px 14px; color: white; }
      button.primary { border: none; border-radius: 10px; padding: 12px 14px; font-weight: 700; cursor: pointer; background: linear-gradient(135deg, #38bdf8, #8b5cf6); color: white; }
      .muted { margin-top: 16px; text-align: center; }
      .muted a { color: #7dd3fc; text-decoration: none; }
      @media (max-width: 640px) { .split { grid-template-columns: 1fr; } }
    `
  ]
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  firstName = 'Ops';
  lastName = 'Engineer';
  email = 'demo@devpulse.local';
  password = 'DemoPass123!';

  submit(): void {
    this.auth.register({
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      password: this.password
    }).subscribe({
      next: () => this.router.navigateByUrl('/dashboard'),
      error: () => {
        console.error('Registration failed for demo user');
      }
    });
  }
}
