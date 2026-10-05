import { CommonModule } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ProjectContext } from '../../core/project-context.service';
import { Deployment } from '../../core/models';

@Component({
  selector: 'app-deployments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Deployments</h1>
          <p *ngIf="projectContext.selected() as project">Deployment history for <strong>{{ project.name }}</strong>.</p>
        </div>
        <div class="page-actions" *ngIf="auth.canWrite() && projectContext.selected()">
          <button type="button" class="btn btn-primary" (click)="showForm.set(!showForm())">
            {{ showForm() ? 'Cancel' : 'Record deployment' }}
          </button>
        </div>
      </header>

      <div class="empty-state" *ngIf="!projectContext.selected()">Select or create a project first.</div>

      <ng-container *ngIf="projectContext.selected()">
        <div class="card" *ngIf="showForm()">
          <h3>Record a deployment</h3>
          <form (ngSubmit)="submit()" #form="ngForm">
            <div class="form-grid two-col">
              <label class="field">
                <span>Version</span>
                <input type="text" [(ngModel)]="version" name="version" placeholder="v1.2.0" required />
              </label>
              <label class="field">
                <span>Environment</span>
                <select [(ngModel)]="environment" name="environment" required>
                  <option value="development">development</option>
                  <option value="staging">staging</option>
                  <option value="production">production</option>
                </select>
              </label>
              <label class="field">
                <span>Status</span>
                <select [(ngModel)]="status" name="status" required>
                  <option value="SUCCESS">SUCCESS</option>
                  <option value="FAILED">FAILED</option>
                  <option value="IN_PROGRESS">IN_PROGRESS</option>
                </select>
              </label>
              <label class="field">
                <span>Branch (optional)</span>
                <input type="text" [(ngModel)]="branch" name="branch" />
              </label>
            </div>
            <div class="row" style="margin-top:16px;">
              <button type="submit" class="btn btn-primary" [disabled]="!form.valid || submitting()">Record</button>
            </div>
            <p class="error-state" *ngIf="submitError()">{{ submitError() }}</p>
          </form>
        </div>

        <div class="table-wrap card" *ngIf="deployments().length; else empty">
          <table class="data">
            <thead><tr><th>Version</th><th>Environment</th><th>Status</th><th>Author</th><th>Recorded</th></tr></thead>
            <tbody>
              @for (d of deployments(); track d.id) {
                <tr>
                  <td><strong>{{ d.version }}</strong><div class="muted" style="font-size:.8rem;">{{ d.branch || '—' }}</div></td>
                  <td><span class="badge badge-neutral">{{ d.environment }}</span></td>
                  <td><span class="badge" [class]="'badge-' + d.status.toLowerCase()">{{ d.status }}</span></td>
                  <td class="muted">{{ d.author || '—' }}</td>
                  <td class="muted">{{ d.createdAt | date: 'short' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <ng-template #empty>
          <div class="empty-state" *ngIf="loaded()">No deployments recorded for this project yet.</div>
        </ng-template>
      </ng-container>
    </section>
  `,
})
export class DeploymentsComponent {
  readonly projectContext = inject(ProjectContext);
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  readonly deployments = signal<Deployment[]>([]);
  readonly loaded = signal(false);
  readonly showForm = signal(false);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);

  version = '';
  environment = 'development';
  status = 'SUCCESS';
  branch = '';

  constructor() {
    effect(() => {
      const projectId = this.projectContext.selectedId();
      if (!projectId) {
        this.deployments.set([]);
        return;
      }
      this.reload(projectId);
    });
  }

  private reload(projectId: number): void {
    this.api.deployments(projectId).subscribe({
      next: (list) => {
        this.deployments.set(list);
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
    this.api
      .recordDeployment({ projectId, version: this.version, environment: this.environment, status: this.status, branch: this.branch || undefined })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.showForm.set(false);
          this.version = '';
          this.branch = '';
          this.reload(projectId);
        },
        error: (err) => {
          this.submitting.set(false);
          this.submitError.set(err?.error?.message ?? 'Failed to record deployment.');
        },
      });
  }
}
