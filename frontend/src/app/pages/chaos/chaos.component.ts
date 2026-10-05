import { CommonModule } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ProjectContext } from '../../core/project-context.service';
import { ChaosSimulation } from '../../core/models';

@Component({
  selector: 'app-chaos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Chaos engineering</h1>
          <p *ngIf="projectContext.selected() as project">Failure-injection simulations for <strong>{{ project.name }}</strong>.</p>
        </div>
      </header>

      <div class="empty-state" *ngIf="!projectContext.selected()">Select or create a project first.</div>

      <ng-container *ngIf="projectContext.selected()">
        <div class="card" *ngIf="auth.canWrite()">
          <h3>Trigger a simulation</h3>
          <p class="muted" style="margin-top:-6px;">
            Simulation only — no real infrastructure is affected. It records the event and opens a matching incident
            to demonstrate detection → investigation → resolution. Blocked on production projects unless
            <code>CHAOS_ALLOW_PRODUCTION=true</code> is set server-side.
          </p>
          <form (ngSubmit)="submit()" #form="ngForm">
            <div class="form-grid two-col">
              <label class="field">
                <span>Action</span>
                <select [(ngModel)]="action" name="action" required>
                  <option value="KILL_POD">KILL_POD</option>
                  <option value="CPU_LOAD">CPU_LOAD</option>
                  <option value="LATENCY">LATENCY</option>
                  <option value="HTTP_500">HTTP_500</option>
                  <option value="DB_FAILURE">DB_FAILURE</option>
                </select>
              </label>
              <label class="field">
                <span>Target service (optional)</span>
                <input type="text" [(ngModel)]="targetService" name="targetService" />
              </label>
            </div>
            <div class="row" style="margin-top:16px;">
              <button type="submit" class="btn btn-primary" [disabled]="submitting()">Trigger</button>
            </div>
            <p class="error-state" *ngIf="submitError()">{{ submitError() }}</p>
          </form>
        </div>

        <div class="table-wrap card" *ngIf="history().length; else empty">
          <table class="data">
            <thead><tr><th>Action</th><th>Target</th><th>Status</th><th>Incident</th><th>Triggered</th></tr></thead>
            <tbody>
              @for (h of history(); track h.id) {
                <tr>
                  <td><strong>{{ h.action }}</strong></td>
                  <td class="muted">{{ h.targetService || '—' }}</td>
                  <td><span class="badge badge-neutral">{{ h.status }}</span></td>
                  <td class="muted">{{ h.resultingIncidentId ? '#' + h.resultingIncidentId : '—' }}</td>
                  <td class="muted">{{ h.createdAt | date: 'short' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <ng-template #empty>
          <div class="empty-state" *ngIf="loaded()">No chaos simulations recorded for this project yet.</div>
        </ng-template>
      </ng-container>
    </section>
  `,
})
export class ChaosComponent {
  readonly projectContext = inject(ProjectContext);
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  readonly history = signal<ChaosSimulation[]>([]);
  readonly loaded = signal(false);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);

  action = 'KILL_POD';
  targetService = '';

  constructor() {
    effect(() => {
      const projectId = this.projectContext.selectedId();
      if (!projectId) {
        this.history.set([]);
        return;
      }
      this.reload(projectId);
    });
  }

  private reload(projectId: number): void {
    this.api.chaosHistory(projectId).subscribe({
      next: (page) => {
        this.history.set(page.content);
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
    this.api.triggerChaos(projectId, { action: this.action, targetService: this.targetService || undefined }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.reload(projectId);
      },
      error: (err) => {
        this.submitting.set(false);
        this.submitError.set(err?.error?.message ?? 'Failed to trigger simulation.');
      },
    });
  }
}
