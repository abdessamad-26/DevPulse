import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AuditLogEntry, CreatedIngestionKey, IngestionKey, ProjectMember } from '../../core/models';
import { ProjectContext } from '../../core/project-context.service';

type AccessTab = 'members' | 'keys' | 'audit';

@Component({
  selector: 'app-access',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Project access</h1>
          <p *ngIf="projectContext.selected() as project">Members, ingestion credentials, and audit history for <strong>{{ project.name }}</strong>.</p>
          <p *ngIf="!projectContext.selected() && auth.user()?.role === 'ADMIN'">Review the global audit history.</p>
        </div>
      </header>

      <div class="empty-state" *ngIf="!projectContext.selected() && auth.user()?.role !== 'ADMIN'">
        Select a project first.
      </div>

      <ng-container *ngIf="projectContext.selected() || auth.user()?.role === 'ADMIN'">
        <div class="tabs" role="tablist" aria-label="Project access sections">
          <button type="button" class="btn" [class.btn-primary]="tab() === 'members'" (click)="selectTab('members')">Members</button>
          <button type="button" class="btn" [class.btn-primary]="tab() === 'keys'" (click)="selectTab('keys')">Ingestion keys</button>
          <button type="button" class="btn" [class.btn-primary]="tab() === 'audit'" (click)="selectTab('audit')">Audit log</button>
        </div>

        <p class="error-state" *ngIf="error()">{{ error() }}</p>

        <ng-container *ngIf="tab() === 'members' && projectContext.selectedId()">
          <div class="card" *ngIf="canManage()">
            <h3>Add a project member</h3>
            <form (ngSubmit)="addMember()" #memberForm="ngForm">
              <div class="form-grid two-col">
                <label class="field">
                  <span>Account email</span>
                  <input type="email" [(ngModel)]="memberEmail" name="memberEmail" required />
                </label>
                <label class="field">
                  <span>Role</span>
                  <select [(ngModel)]="memberRole" name="memberRole">
                    <option value="DEVELOPER">DEVELOPER</option>
                    <option value="VIEWER">VIEWER</option>
                  </select>
                </label>
              </div>
              <div class="row" style="margin-top:16px;">
                <button class="btn btn-primary" type="submit" [disabled]="!memberForm.valid || busy()">Add member</button>
              </div>
            </form>
          </div>

          <div class="table-wrap card">
            <table class="data">
              <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Joined</th><th *ngIf="canManage()">Actions</th></tr></thead>
              <tbody>
                @for (member of members(); track member.userId) {
                  <tr>
                    <td>{{ member.firstName }} {{ member.lastName }}</td>
                    <td>{{ member.email }}</td>
                    <td>
                      <select *ngIf="canManage() && member.role !== 'OWNER'; else memberRoleLabel"
                              [ngModel]="member.role"
                              (ngModelChange)="changeRole(member, $event)">
                        <option value="DEVELOPER">DEVELOPER</option>
                        <option value="VIEWER">VIEWER</option>
                      </select>
                      <ng-template #memberRoleLabel><span class="badge badge-neutral">{{ member.role }}</span></ng-template>
                    </td>
                    <td>{{ member.joinedAt | date:'short' }}</td>
                    <td *ngIf="canManage()">
                      <button *ngIf="member.role !== 'OWNER'" class="btn btn-sm btn-danger" type="button" (click)="removeMember(member)">Remove</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
            <div class="empty-state" *ngIf="!members().length && !loading()">No members found.</div>
          </div>
        </ng-container>

        <ng-container *ngIf="tab() === 'keys' && projectContext.selectedId()">
          <div class="card" *ngIf="canManage()">
            <h3>Create an ingestion key</h3>
            <form (ngSubmit)="createKey()" #keyForm="ngForm">
              <label class="field">
                <span>Key name</span>
                <input type="text" [(ngModel)]="keyName" name="keyName" maxlength="80" required />
              </label>
              <div class="row" style="margin-top:16px;">
                <button class="btn btn-primary" type="submit" [disabled]="!keyForm.valid || busy()">Create key</button>
              </div>
            </form>
          </div>

          <div class="card" *ngIf="newKey() as created">
            <h3>Copy this key now</h3>
            <p class="muted">The complete secret is shown only once.</p>
            <input class="key-value" type="text" [value]="created.key" readonly aria-label="New ingestion key" />
          </div>

          <div class="table-wrap card">
            <table class="data">
              <thead><tr><th>Name</th><th>Prefix</th><th>Created</th><th>Last used</th><th *ngIf="canManage()">Actions</th></tr></thead>
              <tbody>
                @for (key of keys(); track key.id) {
                  <tr>
                    <td>{{ key.name }}</td>
                    <td><code>{{ key.prefix }}…</code></td>
                    <td>{{ key.createdAt | date:'short' }}</td>
                    <td>{{ key.lastUsedAt ? (key.lastUsedAt | date:'short') : 'Never' }}</td>
                    <td *ngIf="canManage()"><button class="btn btn-sm btn-danger" type="button" (click)="revokeKey(key)">Revoke</button></td>
                  </tr>
                }
              </tbody>
            </table>
            <div class="empty-state" *ngIf="!keys().length && !loading()">No ingestion keys created.</div>
          </div>
        </ng-container>

        <ng-container *ngIf="tab() === 'audit'">
          <div class="table-wrap card">
            <table class="data">
              <thead><tr><th>When</th><th>Action</th><th>Entity</th><th>Actor ID</th><th>Details</th></tr></thead>
              <tbody>
                @for (entry of auditEntries(); track entry.id) {
                  <tr>
                    <td>{{ entry.createdAt | date:'short' }}</td>
                    <td><strong>{{ entry.action }}</strong></td>
                    <td>{{ entry.entityType || '—' }}{{ entry.entityId ? ' #' + entry.entityId : '' }}</td>
                    <td>{{ entry.actorUserId ?? 'System' }}</td>
                    <td class="muted">{{ entry.details || '—' }}</td>
                  </tr>
                }
              </tbody>
            </table>
            <div class="empty-state" *ngIf="!auditEntries().length && !loading()">No audit events found.</div>
          </div>
        </ng-container>
      </ng-container>
    </section>
  `,
  styles: [`
    .tabs { display: flex; gap: 8px; margin: 0 0 16px; flex-wrap: wrap; }
    .key-value { width: 100%; font-family: monospace; }
  `],
})
export class AccessComponent {
  readonly projectContext = inject(ProjectContext);
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  readonly tab = signal<AccessTab>('members');
  readonly members = signal<ProjectMember[]>([]);
  readonly keys = signal<IngestionKey[]>([]);
  readonly auditEntries = signal<AuditLogEntry[]>([]);
  readonly newKey = signal<CreatedIngestionKey | null>(null);
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly canManage = computed(() => this.auth.user()?.role === 'ADMIN'
    || this.members().some((member) => member.role === 'OWNER' && member.userId === this.auth.user()?.id));

  memberEmail = '';
  memberRole: 'DEVELOPER' | 'VIEWER' = 'DEVELOPER';
  keyName = '';

  constructor() {
    effect(() => {
      const projectId = this.projectContext.selectedId();
      const activeTab = this.tab();
      if (!projectId && !(activeTab === 'audit' && this.auth.user()?.role === 'ADMIN')) {
        this.members.set([]);
        this.keys.set([]);
        this.auditEntries.set([]);
        return;
      }
      this.load(activeTab, projectId);
    });
  }

  selectTab(tab: AccessTab): void {
    this.error.set(null);
    this.tab.set(tab);
  }

  addMember(): void {
    const projectId = this.projectContext.selectedId();
    if (!projectId) return;
    this.busy.set(true);
    this.error.set(null);
    this.api.addProjectMember(projectId, { email: this.memberEmail, role: this.memberRole }).subscribe({
      next: (member) => {
        this.members.update((members) => [...members, member]);
        this.memberEmail = '';
        this.busy.set(false);
      },
      error: (error) => this.fail(error),
    });
  }

  changeRole(member: ProjectMember, role: 'DEVELOPER' | 'VIEWER'): void {
    const projectId = this.projectContext.selectedId();
    if (!projectId || role === member.role) return;
    this.busy.set(true);
    this.error.set(null);
    this.api.updateProjectMemberRole(projectId, member.userId, role).subscribe({
      next: (updated) => {
        this.members.update((members) => members.map((item) => item.userId === updated.userId ? updated : item));
        this.busy.set(false);
      },
      error: (error) => this.fail(error),
    });
  }

  removeMember(member: ProjectMember): void {
    const projectId = this.projectContext.selectedId();
    if (!projectId || !window.confirm(`Remove ${member.email} from this project?`)) return;
    this.busy.set(true);
    this.error.set(null);
    this.api.removeProjectMember(projectId, member.userId).subscribe({
      next: () => {
        this.members.update((members) => members.filter((item) => item.userId !== member.userId));
        this.busy.set(false);
      },
      error: (error) => this.fail(error),
    });
  }

  createKey(): void {
    const projectId = this.projectContext.selectedId();
    if (!projectId) return;
    this.busy.set(true);
    this.error.set(null);
    this.api.createIngestionKey(projectId, this.keyName).subscribe({
      next: (key) => {
        this.newKey.set(key);
        this.keyName = '';
        this.keys.update((keys) => [key, ...keys]);
        this.busy.set(false);
      },
      error: (error) => this.fail(error),
    });
  }

  revokeKey(key: IngestionKey): void {
    const projectId = this.projectContext.selectedId();
    if (!projectId || !window.confirm(`Revoke the ingestion key "${key.name}"?`)) return;
    this.busy.set(true);
    this.error.set(null);
    this.api.revokeIngestionKey(projectId, key.id).subscribe({
      next: () => {
        this.keys.update((keys) => keys.filter((item) => item.id !== key.id));
        this.busy.set(false);
      },
      error: (error) => this.fail(error),
    });
  }

  private load(tab: AccessTab, projectId: number | null): void {
    this.error.set(null);
    this.loading.set(true);
    const failed = (error: unknown) => this.fail(error);
    if (tab === 'members' && projectId) {
      this.api.projectMembers(projectId).subscribe({
        next: (members) => { this.members.set(members); this.loading.set(false); },
        error: failed,
      });
    } else if (tab === 'keys' && projectId) {
      this.api.ingestionKeys(projectId).subscribe({
        next: (keys) => { this.keys.set(keys); this.loading.set(false); },
        error: failed,
      });
    } else if (tab === 'audit') {
      const request = this.auth.user()?.role === 'ADMIN'
        ? this.api.globalAuditLogs()
        : projectId ? this.api.projectAuditLogs(projectId) : null;
      if (!request) {
        this.auditEntries.set([]);
        this.loading.set(false);
        return;
      }
      request.subscribe({
        next: (page) => { this.auditEntries.set(page.content); this.loading.set(false); },
        error: failed,
      });
    } else {
      this.loading.set(false);
    }
  }

  private fail(error: unknown): void {
    const body = error instanceof HttpErrorResponse ? error.error : null;
    const message = body && typeof body === 'object'
      ? ('error' in body && typeof body.error === 'string'
        ? body.error
        : 'message' in body && typeof body.message === 'string' ? body.message : null)
      : null;
    this.error.set(message ?? 'The request failed. Please try again.');
    this.busy.set(false);
    this.loading.set(false);
  }
}
