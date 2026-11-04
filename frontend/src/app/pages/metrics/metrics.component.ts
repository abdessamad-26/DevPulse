import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
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
        <div class="card muted" *ngIf="loading()" role="status">Loading metrics…</div>
        <div class="card error-state" *ngIf="loadError() as message" role="alert">
          <p>{{ message }}</p>
          <button class="btn btn-sm" type="button" (click)="retry()">Retry</button>
        </div>

        <div class="card chart-card" *ngIf="chartMetrics().length && !loading() && !loadError()">
          <div class="row-between chart-heading">
            <div>
              <h3>Metric trend</h3>
              <p class="muted">{{ selectedMetric() || metricNames()[0] }}</p>
            </div>
            <label class="field chart-select">
              <span>Metric</span>
              <select [ngModel]="selectedMetric()" (ngModelChange)="selectedMetric.set($event)">
                @for (name of metricNames(); track name) {
                  <option [value]="name">{{ name }}</option>
                }
              </select>
            </label>
          </div>
          <svg viewBox="0 0 600 180" role="img" aria-label="Metric values over time" class="metric-chart">
            <line x1="24" y1="20" x2="24" y2="150" class="chart-axis" />
            <line x1="24" y1="150" x2="584" y2="150" class="chart-axis" />
            <polyline [attr.points]="chartPoints()" class="chart-line" />
          </svg>
          <div class="row-between muted chart-labels">
            <span>{{ chartRange().min }}{{ chartMetrics()[0]?.unit ? ' ' + chartMetrics()[0].unit : '' }}</span>
            <span>{{ chartRange().max }}{{ chartMetrics()[0]?.unit ? ' ' + chartMetrics()[0].unit : '' }}</span>
          </div>
        </div>

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

        <div class="table-wrap card" *ngIf="metrics().length && !loading() && !loadError()">
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
        <div class="empty-state" *ngIf="loaded() && !loading() && !loadError() && !metrics().length">No metrics ingested for this project yet.</div>
      </ng-container>
    </section>
  `,
  styles: [`
    .chart-card { margin-bottom: 16px; }
    .chart-heading { align-items: flex-end; }
    .chart-heading h3, .chart-heading p { margin-bottom: 4px; }
    .chart-select { min-width: 180px; }
    .metric-chart { display: block; width: 100%; max-height: 220px; overflow: visible; }
    .chart-axis { stroke: rgba(148,163,184,.35); stroke-width: 1; }
    .chart-line { fill: none; stroke: var(--accent-a); stroke-width: 3; stroke-linecap: round; stroke-linejoin: round; }
    .chart-labels { font-size: .82rem; }
  `],
})
export class MetricsComponent {
  readonly projectContext = inject(ProjectContext);
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  readonly metrics = signal<MetricPoint[]>([]);
  readonly loaded = signal(false);
  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);
  private activeRequest: Subscription | null = null;

  metricName = '';
  value: number | null = null;
  serviceName = '';
  unit = '';

  constructor() {
    effect(() => {
      const projectId = this.projectContext.selectedId();
      this.activeRequest?.unsubscribe();
      this.metrics.set([]);
      this.loaded.set(false);
      this.loadError.set(null);
      if (!projectId) {
        this.loading.set(false);
        return;
      }
      this.reload(projectId);
    });
  }

  readonly selectedMetric = signal('');
  readonly metricNames = computed(() => [...new Set(this.metrics().map((metric) => metric.metricName))]);
  readonly chartMetrics = computed(() => {
    const name = this.selectedMetric() || this.metricNames()[0];
    return this.metrics()
      .filter((metric) => metric.metricName === name)
      .slice()
      .sort((left, right) => left.capturedAt.localeCompare(right.capturedAt));
  });
  readonly chartRange = computed(() => {
    const values = this.chartMetrics().map((metric) => metric.metricValue);
    return {
      min: values.length ? Math.min(...values) : 0,
      max: values.length ? Math.max(...values) : 0,
    };
  });
  readonly chartPoints = computed(() => {
    const metrics = this.chartMetrics();
    if (!metrics.length) return '';
    const { min, max } = this.chartRange();
    const span = max - min || 1;
    return metrics.map((metric, index) => {
      const x = metrics.length === 1 ? 304 : 24 + (560 * index) / (metrics.length - 1);
      const y = 150 - ((metric.metricValue - min) / span) * 130;
      return `${x},${y}`;
    }).join(' ');
  });

  private reload(projectId: number): void {
    this.activeRequest?.unsubscribe();
    this.loading.set(true);
    this.loadError.set(null);
    this.activeRequest = this.api.metrics(projectId).subscribe({
      next: (page) => {
        this.metrics.set(page.content);
        if (!page.content.some((metric) => metric.metricName === this.selectedMetric())) {
          this.selectedMetric.set('');
        }
        this.loaded.set(true);
        this.loading.set(false);
        this.activeRequest = null;
      },
      error: () => {
        this.loaded.set(true);
        this.loading.set(false);
        this.loadError.set('Could not load metrics. Check your connection and try again.');
        this.activeRequest = null;
      },
    });
  }

  retry(): void {
    const projectId = this.projectContext.selectedId();
    if (projectId) this.reload(projectId);
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
