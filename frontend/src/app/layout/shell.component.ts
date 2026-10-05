import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ProjectContext } from '../core/project-context.service';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

const NAV_ITEMS: NavItem[] = [
  { path: '/dashboard', label: 'Dashboard', icon: '📊' },
  { path: '/projects', label: 'Projects', icon: '🗂️' },
  { path: '/services', label: 'Services', icon: '🧩' },
  { path: '/incidents', label: 'Incidents', icon: '🚨' },
  { path: '/metrics', label: 'Metrics', icon: '📈' },
  { path: '/logs', label: 'Logs', icon: '📜' },
  { path: '/deployments', label: 'Deployments', icon: '🚀' },
  { path: '/alerts', label: 'Alerts', icon: '🔔' },
  { path: '/chaos', label: 'Chaos', icon: '🌀' },
  { path: '/settings', label: 'Settings', icon: '⚙️' },
];

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand">
          <span class="brand-mark">D</span>
          <strong>DevPulse</strong>
        </div>

        <nav class="nav">
          @for (item of navItems; track item.path) {
            <a [routerLink]="item.path" routerLinkActive="active" class="nav-link">
              <span class="icon">{{ item.icon }}</span>
              <span>{{ item.label }}</span>
            </a>
          }
        </nav>

        <div class="sidebar-footer">
          <div class="project-picker" *ngIf="projectContext.projects().length">
            <label class="field">
              <span>Project</span>
              <select [ngModel]="projectContext.selectedId()" (ngModelChange)="projectContext.select($event)" name="project">
                @for (p of projectContext.projects(); track p.id) {
                  <option [ngValue]="p.id">{{ p.name }}</option>
                }
              </select>
            </label>
          </div>
        </div>
      </aside>

      <div class="content-area">
        <header class="topbar">
          <div class="topbar-title">
            <span class="muted">DevPulse</span>
          </div>
          <div class="topbar-user" *ngIf="auth.user() as user">
            <div class="user-meta">
              <strong>{{ user.firstName }} {{ user.lastName }}</strong>
              <span class="badge" [class]="'badge-' + (user.role || 'neutral').toLowerCase()">{{ user.role }}</span>
            </div>
            <button type="button" class="btn btn-sm" (click)="auth.logout()">Log out</button>
          </div>
        </header>

        <main class="content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [
    `
      .shell { display: flex; min-height: 100vh; }
      .sidebar { width: 240px; flex-shrink: 0; background: rgba(8,13,26,0.95); border-right: 1px solid var(--border); display: flex; flex-direction: column; padding: 20px 14px; gap: 24px; }
      .brand { display: flex; align-items: center; gap: 10px; padding: 0 6px; }
      .brand-mark { width: 34px; height: 34px; border-radius: 10px; display: grid; place-items: center; background: var(--accent-grad); font-weight: 800; }
      .nav { display: flex; flex-direction: column; gap: 2px; flex: 1; }
      .nav-link { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 10px; color: var(--muted); text-decoration: none; font-weight: 600; font-size: .92rem; }
      .nav-link:hover { background: rgba(148,163,184,0.08); color: var(--text); }
      .nav-link.active { background: rgba(56,189,248,0.12); color: var(--accent-a); }
      .icon { width: 20px; text-align: center; }
      .sidebar-footer { border-top: 1px solid var(--border); padding-top: 14px; }
      .project-picker select { width: 100%; }

      .content-area { flex: 1; display: flex; flex-direction: column; min-width: 0; }
      .topbar { display: flex; justify-content: space-between; align-items: center; padding: 16px 32px; border-bottom: 1px solid var(--border); }
      .topbar-user { display: flex; align-items: center; gap: 12px; }
      .user-meta { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
      .user-meta strong { font-size: .88rem; }
      .content { flex: 1; }

      @media (max-width: 900px) {
        .shell { flex-direction: column; }
        .sidebar { width: 100%; flex-direction: row; align-items: center; overflow-x: auto; }
        .nav { flex-direction: row; }
        .sidebar-footer { display: none; }
      }
    `
  ]
})
export class ShellComponent {
  readonly auth = inject(AuthService);
  readonly projectContext = inject(ProjectContext);
  readonly navItems = NAV_ITEMS;

  constructor() {
    this.projectContext.load();
  }
}
