import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { ProjectContext } from '../../core/project-context.service';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Projects</h1>
          <p>Every project you own, with its environment and lifecycle status.</p>
        </div>
        <div class="page-actions" *ngIf="auth.canWrite()">
          <button type="button" class="btn btn-primary" (click)="showForm.set(!showForm())">
            {{ showForm() ? 'Cancel' : 'New project' }}
          </button>
        </div>
      </header>

      <div class="card" *ngIf="showForm()">
        <h3>Create a project</h3>
        <form (ngSubmit)="submit()" #form="ngForm">
          <div class="form-grid two-col">
            <label class="field">
              <span>Name</span>
              <input type="text" [(ngModel)]="name" name="name" required />
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
              <span>Repository</span>
              <input type="text" [(ngModel)]="repository" name="repository" placeholder="github.com/org/repo" />
            </label>
            <label class="field">
              <span>Description</span>
              <input type="text" [(ngModel)]="description" name="description" />
            </label>
          </div>
          <div class="row" style="margin-top: 16px;">
            <button type="submit" class="btn btn-primary" [disabled]="!form.valid || submitting()">Create project</button>
          </div>
          <p class="error-state" *ngIf="submitError()">{{ submitError() }}</p>
        </form>
      </div>

      <div class="error-state" *ngIf="projectContext.error()">{{ projectContext.error() }}</div>

      <div class="table-wrap card" *ngIf="projectContext.projects().length; else empty">
        <table class="data">
          <thead>
            <tr><th>Name</th><th>Environment</th><th>Status</th><th>Repository</th><th>Created</th></tr>
          </thead>
          <tbody>
            @for (p of projectContext.projects(); track p.id) {
              <tr [class.selected]="p.id === projectContext.selectedId()">
                <td><a href="javascript:void(0)" (click)="projectContext.select(p.id)"><strong>{{ p.name }}</strong></a></td>
                <td><span class="badge badge-neutral">{{ p.environment }}</span></td>
                <td><span class="badge" [class]="'badge-' + p.status.toLowerCase()">{{ p.status }}</span></td>
                <td class="muted">{{ p.repository || '—' }}</td>
                <td class="muted">{{ p.createdAt | date: 'medium' }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <ng-template #empty>
        <div class="empty-state" *ngIf="projectContext.loaded()">No projects yet. Create your first one to get started.</div>
      </ng-template>
    </section>
  `,
})
export class ProjectsComponent {
  readonly projectContext = inject(ProjectContext);
  readonly auth = inject(AuthService);

  readonly showForm = signal(false);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);

  name = '';
  environment = 'development';
  repository = '';
  description = '';

  submit(): void {
    this.submitting.set(true);
    this.submitError.set(null);
    this.projectContext
      .create({ name: this.name, environment: this.environment, repository: this.repository, description: this.description })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.showForm.set(false);
          this.name = '';
          this.repository = '';
          this.description = '';
        },
        error: (err) => {
          this.submitting.set(false);
          this.submitError.set(err?.error?.message ?? 'Failed to create project.');
        },
      });
  }
}
