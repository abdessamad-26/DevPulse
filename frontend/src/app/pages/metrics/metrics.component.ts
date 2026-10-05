import { CommonModule } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ProjectContext } from '../../core/project-context.service';
import { MetricPoint } from '../../core/models';

@Component({
  selector: 'app-metrics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Metrics</h1>
          <p *ngIf="projectContext.selected() as project">Recent metric points ingested for <strong>{{ project.name }}</strong>.</p>
        </div>
        <div class="page-actions" *ngIf="auth.canWrite() && projectContext.selected()">
          <button type="button" class="btn btn-primary" (click)="showForm.set(!showForm())">
            {{ showForm() ? 'Cancel' : 'Ingest metric' }}
          </button>
        </div>
      </header>

      <div class="empty-state" *ngIf="!projectContext.selected()">Select or create a project first.</div>

      <ng-container *ngIf="projectContext.selected()">
        <div class="card" *ngIf="showForm()">
          <h3>Ingest a metric point</h3>
          <p class="muted" style="margin-top:-6px;">This immediately runs against the project's enabled alert rules.</p>
          <form (ngSubmit)="submit()" #form="ngForm">
            <div class="form-grid two-col">
              <label class="field">
                <span>Metric name</span>
                <input type="text" [(ngModel)]="metricName" name="metricName" placeholder="cpu_usage_percent" required />
              </label>
              <label class="field">
                <span>Value</span>
                <input type="number" [(ngModel)]="value" name="value" step="any" required />
              </label>
              <label class="field">
                <span>Service (optional)</span>
                <input type="text" [(ngModel)]="serviceName" name="serviceName" />
              </label>
              <label class="field">
                <span>Unit (optional)</span>
                <input type="text" [(ngModel)]="unit" name="unit" placeholder="%" />
              </label>
            </div>
            <div class="row" style="margin-top:16px;">
              <button type="submit" class="btn btn-primary" [disabled]="!form.valid || submitting()">Ingest</button>
            </div>
            <p class="error-state" *ngIf="submitError()">{{ submitError() }}</p>
          </form>
        </div>

        <div class="table-wrap card" *ngIf="metrics().length; else empty">
          <table class="data">
            <thead><tr><th>Metric</th><th>Value</th><th>Service</th><th>Captured</th></tr></thead>
            <tbody>
              @for (m of metrics(); track m.id) {
                <tr>
                  <td><strong>{{ m.metricName }}</strong></td>
                  <td>{{ m.metricValue }}{{ m.unit ? ' ' + m.unit : '' }}</td>
                  <td class="muted">{{ m.serviceName || '—' }}</td>
                  <td class="muted">{{ m.capturedAt | date: 'short' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <ng-template #empty>
          <div class="empty-state" *ngIf="loaded()">No metrics ingested for this project yet.</div>
        </ng-template>
      </ng-container>
    </section>
  `,
})
export class MetricsComponent {
  readonly projectContext = inject(ProjectContext);
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  readonly metrics = signal<MetricPoint[]>([]);
  readonly loaded = signal(false);
  readonly showForm = signal(false);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);

  metricName = '';
  value: number | null = null;
  serviceName = '';
  unit = '';

  constructor() {
    effect(() => {
      const projectId = this.projectContext.selectedId();
      if (!projectId) {
        this.metrics.set([]);
        return;
      }
      this.reload(projectId);
    });
  }

  private reload(projectId: number): void {
    this.api.metrics(projectId).subscribe({
      next: (list) => {
        this.metrics.set(list);
        this.loaded.set(true);
      },
      error: () => this.loaded.set(true),
    });
  }

  submit(): void {
    const projectId = this.projectContext.selectedId();
    if (!projectId || this.value === null) return;
    this.submitting.set(true);
    this.submitError.set(null);
    this.api
      .ingestMetrics(projectId, [
        { metricName: this.metricName, value: this.value, serviceName: this.serviceName || undefined, unit: this.unit || undefined },
      ])
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.showForm.set(false);
          this.metricName = '';
          this.value = null;
          this.serviceName = '';
          this.unit = '';
          this.reload(projectId);
        },
        error: (err) => {
          this.submitting.set(false);
          this.submitError.set(err?.error?.message ?? 'Failed to ingest metric.');
        },
      });
  }
}
