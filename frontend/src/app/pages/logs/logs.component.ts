import { CommonModule } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { ProjectContext } from '../../core/project-context.service';
import { LogEntry } from '../../core/models';

@Component({
  selector: 'app-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Logs</h1>
          <p *ngIf="projectContext.selected() as project">Log search for <strong>{{ project.name }}</strong>.</p>
        </div>
      </header>

      <div class="empty-state" *ngIf="!projectContext.selected()">Select or create a project first.</div>

      <ng-container *ngIf="projectContext.selected()">
        <div class="card">
          <div class="form-grid" style="grid-template-columns: repeat(auto-fit, minmax(160px,1fr)) auto;">
            <label class="field">
              <span>Service</span>
              <input type="text" [(ngModel)]="service" name="service" (change)="search()" />
            </label>
            <label class="field">
              <span>Level</span>
              <select [(ngModel)]="level" name="level" (change)="search()">
                <option value="">All</option>
                <option value="INFO">INFO</option>
                <option value="WARN">WARN</option>
                <option value="ERROR">ERROR</option>
              </select>
            </label>
            <label class="field">
              <span>Search text</span>
              <input type="text" [(ngModel)]="q" name="q" (keyup.enter)="search()" />
            </label>
            <div class="field" style="justify-content:flex-end;">
              <span>&nbsp;</span>
              <button type="button" class="btn btn-primary" (click)="search()">Search</button>
            </div>
          </div>
        </div>

        <div class="table-wrap card" *ngIf="logs().length; else empty">
          <table class="data">
            <thead><tr><th>Level</th><th>Service</th><th>Message</th><th>Time</th></tr></thead>
            <tbody>
              @for (l of logs(); track l.id) {
                <tr>
                  <td><span class="badge" [class]="badgeFor(l.level)">{{ l.level || '—' }}</span></td>
                  <td class="muted">{{ l.serviceName || '—' }}</td>
                  <td>{{ l.message }}</td>
                  <td class="muted">{{ l.timestamp | date: 'short' }}</td>
                </tr>
              }
            </tbody>
          </table>
          <div class="row-between" style="margin-top:14px;">
            <button type="button" class="btn btn-sm" [disabled]="page() === 0" (click)="changePage(-1)">Previous</button>
            <span class="muted">Page {{ page() + 1 }} / {{ Math.max(totalPages(), 1) }}</span>
            <button type="button" class="btn btn-sm" [disabled]="page() + 1 >= totalPages()" (click)="changePage(1)">Next</button>
          </div>
        </div>
        <ng-template #empty>
          <div class="empty-state" *ngIf="loaded()">No log entries match the current filters.</div>
        </ng-template>
      </ng-container>
    </section>
  `,
})
export class LogsComponent {
  readonly projectContext = inject(ProjectContext);
  private readonly api = inject(ApiService);
  readonly Math = Math;

  readonly logs = signal<LogEntry[]>([]);
  readonly loaded = signal(false);
  readonly page = signal(0);
  readonly totalPages = signal(0);

  service = '';
  level = '';
  q = '';

  constructor() {
    effect(() => {
      const projectId = this.projectContext.selectedId();
      if (!projectId) {
        this.logs.set([]);
        return;
      }
      this.page.set(0);
      this.reload(projectId);
    });
  }

  badgeFor(level: string | null): string {
    const lvl = (level || '').toUpperCase();
    if (lvl === 'ERROR') return 'badge-critical';
    if (lvl === 'WARN') return 'badge-warning';
    return 'badge-neutral';
  }

  private reload(projectId: number): void {
    this.api.logs(projectId, { service: this.service || undefined, level: this.level || undefined, q: this.q || undefined }, this.page(), 20).subscribe({
      next: (res) => {
        this.logs.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loaded.set(true);
      },
      error: () => this.loaded.set(true),
    });
  }

  search(): void {
    const projectId = this.projectContext.selectedId();
    if (!projectId) return;
    this.page.set(0);
    this.reload(projectId);
  }

  changePage(delta: number): void {
    const projectId = this.projectContext.selectedId();
    if (!projectId) return;
    this.page.update((p) => p + delta);
    this.reload(projectId);
  }
}
