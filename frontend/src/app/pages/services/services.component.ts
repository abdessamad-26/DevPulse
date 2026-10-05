import { CommonModule } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ProjectContext } from '../../core/project-context.service';
import { ServiceItem } from '../../core/models';

@Component({
  selector: 'app-services',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Services</h1>
          <p *ngIf="projectContext.selected() as project">Service inventory for <strong>{{ project.name }}</strong>.</p>
        </div>
        <div class="page-actions" *ngIf="auth.canWrite() && projectContext.selected()">
          <button type="button" class="btn btn-primary" (click)="showForm.set(!showForm())">
            {{ showForm() ? 'Cancel' : 'Register service' }}
          </button>
        </div>
      </header>

      <div class="empty-state" *ngIf="!projectContext.selected()">Select or create a project first.</div>

      <ng-container *ngIf="projectContext.selected()">
        <div class="card" *ngIf="showForm()">
          <h3>Register a service</h3>
          <form (ngSubmit)="submit()" #form="ngForm">
            <div class="form-grid two-col">
              <label class="field">
                <span>Name</span>
                <input type="text" [(ngModel)]="name" name="name" required />
              </label>
              <label class="field">
                <span>Type</span>
                <input type="text" [(ngModel)]="type" name="type" placeholder="http, worker, …" />
              </label>
              <label class="field">
                <span>Health status</span>
                <select [(ngModel)]="healthStatus" name="healthStatus">
                  <option value="HEALTHY">HEALTHY</option>
                  <option value="WARNING">WARNING</option>
                  <option value="DOWN">DOWN</option>
                </select>
              </label>
            </div>
            <div class="row" style="margin-top:16px;">
              <button type="submit" class="btn btn-primary" [disabled]="!form.valid || submitting()">Register</button>
            </div>
            <p class="error-state" *ngIf="submitError()">{{ submitError() }}</p>
          </form>
        </div>

        <div class="table-wrap card" *ngIf="services().length; else empty">
          <table class="data">
            <thead><tr><th>Name</th><th>Type</th><th>Health</th><th>Registered</th></tr></thead>
            <tbody>
              @for (s of services(); track s.id) {
                <tr>
                  <td><strong>{{ s.name }}</strong></td>
                  <td class="muted">{{ s.type || '—' }}</td>
                  <td><span class="badge" [class]="'badge-' + s.healthStatus.toLowerCase()">{{ s.healthStatus }}</span></td>
                  <td class="muted">{{ s.createdAt | date: 'medium' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <ng-template #empty>
          <div class="empty-state" *ngIf="loaded()">No services registered for this project yet.</div>
        </ng-template>
      </ng-container>
    </section>
  `,
})
export class ServicesComponent {
  readonly projectContext = inject(ProjectContext);
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  readonly services = signal<ServiceItem[]>([]);
  readonly loaded = signal(false);
  readonly showForm = signal(false);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);

  name = '';
  type = '';
  healthStatus = 'HEALTHY';

  constructor() {
    effect(() => {
      const projectId = this.projectContext.selectedId();
      if (!projectId) {
        this.services.set([]);
        return;
      }
      this.reload(projectId);
    });
  }

  private reload(projectId: number): void {
    this.api.services(projectId).subscribe({
      next: (list) => {
        this.services.set(list);
        this.loaded.set(true);
      },
      error: () => this.loaded.set(true),
    });
  }

  submit(): void {
    const projectId = this.projectContext.selectedId();
    if (!projectId) return;
    this.submitting.set(true);
    this.submitError.set(null);
    this.api.createService(projectId, { name: this.name, type: this.type || undefined, healthStatus: this.healthStatus }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.showForm.set(false);
        this.name = '';
        this.type = '';
        this.reload(projectId);
      },
      error: (err) => {
        this.submitting.set(false);
        this.submitError.set(err?.error?.message ?? 'Failed to register service.');
      },
    });
  }
}
