import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';
import { ShellComponent } from './layout/shell.component';
import { LoginComponent } from './pages/auth/login.component';
import { RegisterComponent } from './pages/auth/register.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent) },
      { path: 'projects', loadComponent: () => import('./pages/projects/projects.component').then((m) => m.ProjectsComponent) },
      { path: 'services', loadComponent: () => import('./pages/services/services.component').then((m) => m.ServicesComponent) },
      { path: 'incidents', loadComponent: () => import('./pages/incidents/incidents.component').then((m) => m.IncidentsComponent) },
      { path: 'metrics', loadComponent: () => import('./pages/metrics/metrics.component').then((m) => m.MetricsComponent) },
      { path: 'logs', loadComponent: () => import('./pages/logs/logs.component').then((m) => m.LogsComponent) },
      { path: 'deployments', loadComponent: () => import('./pages/deployments/deployments.component').then((m) => m.DeploymentsComponent) },
      { path: 'alerts', loadComponent: () => import('./pages/alerts/alerts.component').then((m) => m.AlertsComponent) },
      { path: 'chaos', loadComponent: () => import('./pages/chaos/chaos.component').then((m) => m.ChaosComponent) },
      { path: 'settings', loadComponent: () => import('./pages/settings/settings.component').then((m) => m.SettingsComponent) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '/dashboard' },
];
