import { CommonModule } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ProjectContext } from '../../core/project-context.service';
import { Incident } from '../../core/models';

@Component({
  selector: 'app-incidents',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Incidents</h1>
          <p *ngIf="projectContext.selected() as project">Incident tracking for <strong>{{ project.name }}</strong>.</p>
        </div>
        <div class="page-actions" *ngIf="auth.canWrite() && projectContext.selected()">
          <button type="button" class="btn btn-primary" (click)="showForm.set(!showForm())">
            {{ showForm() ? 'Cancel' : 'Report incident' }}
          </button>
        </div>
      </header>

      <div class="empty-state" *ngIf="!projectContext.selected()">Select or create a project first.</div>

      <ng-container *ngIf="projectContext.selected()">
        <div class="card" *ngIf="showForm()">
          <h3>Report an incident</h3>
          <form (ngSubmit)="submit()" #form="ngForm">
            <div class="form-grid two-col">
              <label class="field">
                <span>Title</span>
                <input type="text" [(ngModel)]="title" name="title" required />
              </label>
              <label class="field">
                <span>Severity</span>
                <select [(ngModel)]="severity" name="severity" required>
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                  <option value="CRITICAL">CRITICAL</option>
                </select>
              </label>
            </div>
            <label class="field" style="margin-top:14px;">
              <span>Description</span>
              <textarea [(ngModel)]="description" name="description"></textarea>
            </label>
            <div class="row" style="margin-top:16px;">
              <button type="submit" class="btn btn-primary" [disabled]="!form.valid || submitting()">Create incident</button>
            </div>
            <p class="error-state" *ngIf="submitError()">{{ submitError() }}</p>
          </form>
        </div>

        <div class="table-wrap card" *ngIf="incidents().length; else empty">
          <table class="data">
            <thead><tr><th>Title</th><th>Severity</th><th>Status</th><th>Root cause</th><th>Detected</th><th></th></tr></thead>
            <tbody>
              @for (i of incidents(); track i.id) {
                <tr>
                  <td><strong>{{ i.title }}</strong><div class="muted" style="font-size:.8rem;">{{ i.serviceName || '—' }}</div></td>
                  <td><span class="badge" [class]="'badge-' + i.severity.toLowerCase()">{{ i.severity }}</span></td>
                  <td>
                    <select
                      *ngIf="auth.canWrite(); else statusBadge"
                      [ngModel]="i.status"
                      (ngModelChange)="updateStatus(i, $event)"
                      [name]="'status-' + i.id"
                    >
                      <option value="OPEN">OPEN</option>
                      <option value="INVESTIGATING">INVESTIGATING</option>
                      <option value="RESOLVED">RESOLVED</option>
                    </select>
                    <ng-template #statusBadge><span class="badge" [class]="'badge-' + i.status.toLowerCase()">{{ i.status }}</span></ng-template>
                  </td>
                  <td class="muted" style="max-width:260px;">{{ i.rootCause || '—' }}</td>
                  <td class="muted">{{ i.detectedAt | date: 'short' }}</td>
                  <td>
                    <button type="button" class="btn btn-sm" *ngIf="auth.canWrite()" [disabled]="analyzing() === i.id" (click)="analyze(i)">
                      {{ analyzing() === i.id ? 'Analyzing…' : 'Analyze' }}
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <ng-template #empty>
          <div class="empty-state" *ngIf="loaded()">No incidents recorded for this project.</div>
        </ng-template>
      </ng-container>
    </section>
  `,
})
export class IncidentsComponent {
  readonly projectContext = inject(ProjectContext);
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  readonly incidents = signal<Incident[]>([]);
  readonly loaded = signal(false);
  readonly showForm = signal(false);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly analyzing = signal<number | null>(null);

  title = '';
  severity = 'MEDIUM';
  description = '';

  constructor() {
    effect(() => {
      const projectId = this.projectContext.selectedId();
      if (!projectId) {
        this.incidents.set([]);
        return;
      }
      this.reload(projectId);
    });
  }

  private reload(projectId: number): void {
    this.api.incidents(projectId).subscribe({
      next: (list) => {
        this.incidents.set(list);
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
    this.api.createIncident({ projectId, title: this.title, severity: this.severity, description: this.description || undefined }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.showForm.set(false);
        this.title = '';
        this.description = '';
        this.reload(projectId);
      },
      error: (err) => {
        this.submitting.set(false);
        this.submitError.set(err?.error?.message ?? 'Failed to create incident.');
      },
    });
  }

  updateStatus(incident: Incident, status: string): void {
    this.api.updateIncidentStatus(incident.id, status).subscribe({
      next: (updated) => {
        this.incidents.update((list) => list.map((i) => (i.id === updated.id ? updated : i)));
      },
    });
  }

  analyze(incident: Incident): void {
    this.analyzing.set(incident.id);
    this.api.analyzeIncident(incident.id).subscribe({
      next: (updated) => {
        this.analyzing.set(null);
        this.incidents.update((list) => list.map((i) => (i.id === updated.id ? updated : i)));
      },
      error: () => this.analyzing.set(null),
    });
  }
}
