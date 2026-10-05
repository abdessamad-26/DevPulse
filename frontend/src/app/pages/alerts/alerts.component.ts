import { CommonModule } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ProjectContext } from '../../core/project-context.service';
import { Alert, AlertRule } from '../../core/models';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Alerts</h1>
          <p *ngIf="projectContext.selected() as project">Alert rules and triggered alerts for <strong>{{ project.name }}</strong>.</p>
        </div>
        <div class="page-actions" *ngIf="auth.canWrite() && projectContext.selected()">
          <button type="button" class="btn btn-primary" (click)="showForm.set(!showForm())">
            {{ showForm() ? 'Cancel' : 'New rule' }}
          </button>
        </div>
      </header>

      <div class="empty-state" *ngIf="!projectContext.selected()">Select or create a project first.</div>

      <ng-container *ngIf="projectContext.selected()">
        <div class="card" *ngIf="showForm()">
          <h3>Create an alert rule</h3>
          <p class="muted" style="margin-top:-6px;">A single metric point breaching the threshold opens an alert immediately (no sliding time window yet).</p>
          <form (ngSubmit)="submitRule()" #form="ngForm">
            <div class="form-grid two-col">
              <label class="field">
                <span>Name</span>
                <input type="text" [(ngModel)]="ruleName" name="ruleName" required />
              </label>
              <label class="field">
                <span>Metric</span>
                <input type="text" [(ngModel)]="ruleMetric" name="ruleMetric" placeholder="cpu_usage_percent" required />
              </label>
              <label class="field">
                <span>Operator</span>
                <select [(ngModel)]="ruleOperator" name="ruleOperator" required>
                  <option value=">">&gt;</option>
                  <option value="<">&lt;</option>
                  <option value=">=">&gt;=</option>
                  <option value="<=">&lt;=</option>
                  <option value="==">==</option>
                </select>
              </label>
              <label class="field">
                <span>Threshold</span>
                <input type="number" [(ngModel)]="ruleThreshold" name="ruleThreshold" step="any" required />
              </label>
              <label class="field">
                <span>Severity</span>
                <select [(ngModel)]="ruleSeverity" name="ruleSeverity" required>
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                  <option value="CRITICAL">CRITICAL</option>
                </select>
              </label>
            </div>
            <div class="row" style="margin-top:16px;">
              <button type="submit" class="btn btn-primary" [disabled]="!form.valid || submittingRule()">Create rule</button>
            </div>
            <p class="error-state" *ngIf="ruleError()">{{ ruleError() }}</p>
          </form>
        </div>

        <div class="card">
          <h3>Rules</h3>
          <div class="table-wrap" *ngIf="rules().length; else noRules">
            <table class="data">
              <thead><tr><th>Name</th><th>Condition</th><th>Severity</th><th>Enabled</th></tr></thead>
              <tbody>
                @for (r of rules(); track r.id) {
                  <tr>
                    <td><strong>{{ r.name }}</strong></td>
                    <td class="muted">{{ r.metric }} {{ r.operator }} {{ r.threshold }}</td>
                    <td><span class="badge" [class]="'badge-' + r.severity.toLowerCase()">{{ r.severity }}</span></td>
                    <td><span class="badge" [class]="r.enabled ? 'badge-healthy' : 'badge-neutral'">{{ r.enabled ? 'Yes' : 'No' }}</span></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <ng-template #noRules><p class="muted">No alert rules configured yet.</p></ng-template>
        </div>

        <div class="card">
          <h3>Triggered alerts</h3>
          <div class="table-wrap" *ngIf="alerts().length; else noAlerts">
            <table class="data">
              <thead><tr><th>Message</th><th>Severity</th><th>Status</th><th>Created</th><th></th></tr></thead>
              <tbody>
                @for (a of alerts(); track a.id) {
                  <tr>
                    <td>{{ a.message }}</td>
                    <td><span class="badge" [class]="'badge-' + a.severity.toLowerCase()">{{ a.severity }}</span></td>
                    <td><span class="badge" [class]="'badge-' + a.status.toLowerCase()">{{ a.status }}</span></td>
                    <td class="muted">{{ a.createdAt | date: 'short' }}</td>
                    <td>
                      <button type="button" class="btn btn-sm" *ngIf="auth.canWrite() && a.status === 'OPEN'" (click)="ack(a)">Acknowledge</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <ng-template #noAlerts><p class="muted">No alerts triggered for this project yet.</p></ng-template>
        </div>
      </ng-container>
    </section>
  `,
})
export class AlertsComponent {
  readonly projectContext = inject(ProjectContext);
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  readonly rules = signal<AlertRule[]>([]);
  readonly alerts = signal<Alert[]>([]);
  readonly showForm = signal(false);
  readonly submittingRule = signal(false);
  readonly ruleError = signal<string | null>(null);

  ruleName = '';
  ruleMetric = '';
  ruleOperator = '>';
  ruleThreshold: number | null = null;
  ruleSeverity = 'MEDIUM';

  constructor() {
    effect(() => {
      const projectId = this.projectContext.selectedId();
      if (!projectId) {
        this.rules.set([]);
        this.alerts.set([]);
        return;
      }
      this.reload(projectId);
    });
  }

  private reload(projectId: number): void {
    this.api.alertRules(projectId).subscribe({ next: (page) => this.rules.set(page.content) });
    this.api.alerts(projectId).subscribe({ next: (page) => this.alerts.set(page.content) });
  }

  submitRule(): void {
    const projectId = this.projectContext.selectedId();
    if (!projectId || this.ruleThreshold === null) return;
    this.submittingRule.set(true);
    this.ruleError.set(null);
    this.api
      .createAlertRule(projectId, {
        name: this.ruleName,
        metric: this.ruleMetric,
        operator: this.ruleOperator,
        threshold: this.ruleThreshold,
        severity: this.ruleSeverity,
        enabled: true,
      })
      .subscribe({
        next: () => {
          this.submittingRule.set(false);
          this.showForm.set(false);
          this.ruleName = '';
          this.ruleMetric = '';
          this.ruleThreshold = null;
          this.reload(projectId);
        },
        error: (err) => {
          this.submittingRule.set(false);
          this.ruleError.set(err?.error?.message ?? 'Failed to create rule.');
        },
      });
  }

  ack(alert: Alert): void {
    this.api.acknowledgeAlert(alert.id).subscribe({
      next: (updated) => this.alerts.update((list) => list.map((a) => (a.id === updated.id ? updated : a))),
    });
  }
}
