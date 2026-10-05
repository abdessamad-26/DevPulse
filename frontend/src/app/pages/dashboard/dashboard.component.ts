import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApiService } from '../../core/api.service';
import { ProjectContext } from '../../core/project-context.service';
import { Alert, Deployment, Incident, ServiceItem } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Dashboard</h1>
          <p *ngIf="projectContext.selected() as project">Operational overview for <strong>{{ project.name }}</strong>.</p>
          <p *ngIf="!projectContext.selected()">No project selected yet.</p>
        </div>
      </header>

      <div class="empty-state" *ngIf="projectContext.loaded() && !projectContext.projects().length">
        No projects yet — create one from the Projects page to see live data here.
      </div>

      <ng-container *ngIf="projectContext.selected()">
        <div class="summary-grid">
          <div class="summary-card">
            <span>Services</span>
            <strong>{{ services().length }}</strong>
          </div>
          <div class="summary-card">
            <span>Open incidents</span>
            <strong>{{ openIncidents().length }}</strong>
          </div>
          <div class="summary-card">
            <span>Active alerts</span>
            <strong>{{ openAlerts().length }}</strong>
          </div>
          <div class="summary-card">
            <span>Last deployment</span>
            <strong>{{ lastDeployment()?.version || '—' }}</strong>
          </div>
        </div>

        <div class="card-grid">
          <div class="card">
            <h3>Recent incidents</h3>
            <div class="stack" *ngIf="incidents().length; else noIncidents">
              @for (i of incidents().slice(0, 5); track i.id) {
                <div class="row-between">
                  <span>{{ i.title }}</span>
                  <span class="badge" [class]="'badge-' + i.severity.toLowerCase()">{{ i.severity }}</span>
                </div>
              }
            </div>
            <ng-template #noIncidents><p class="muted">No incidents recorded for this project.</p></ng-template>
          </div>

          <div class="card">
            <h3>Recent deployments</h3>
            <div class="stack" *ngIf="deployments().length; else noDeployments">
              @for (d of deployments().slice(0, 5); track d.id) {
                <div class="row-between">
                  <span>{{ d.version }} · {{ d.environment }}</span>
                  <span class="badge" [class]="'badge-' + d.status.toLowerCase()">{{ d.status }}</span>
                </div>
              }
            </div>
            <ng-template #noDeployments><p class="muted">No deployments recorded for this project.</p></ng-template>
          </div>

          <div class="card">
            <h3>Service health</h3>
            <div class="stack" *ngIf="services().length; else noServices">
              @for (s of services(); track s.id) {
                <div class="row-between">
                  <span>{{ s.name }}</span>
                  <span class="badge" [class]="'badge-' + s.healthStatus.toLowerCase()">{{ s.healthStatus }}</span>
                </div>
              }
            </div>
            <ng-template #noServices><p class="muted">No services registered for this project.</p></ng-template>
          </div>
        </div>
      </ng-container>
    </section>
  `,
})
export class DashboardComponent {
  readonly projectContext = inject(ProjectContext);
  private readonly api = inject(ApiService);

  readonly services = signal<ServiceItem[]>([]);
  readonly incidents = signal<Incident[]>([]);
  readonly alerts = signal<Alert[]>([]);
  readonly deployments = signal<Deployment[]>([]);

  readonly openIncidents = computed(() => this.incidents().filter((i) => !['RESOLVED', 'CLOSED'].includes(i.status.toUpperCase())));
  readonly openAlerts = computed(() => this.alerts().filter((a) => a.status.toUpperCase() === 'OPEN'));
  readonly lastDeployment = computed(() => this.deployments()[0] ?? null);

  constructor() {
    effect(() => {
      const projectId = this.projectContext.selectedId();
      if (!projectId) {
        this.services.set([]);
        this.incidents.set([]);
        this.alerts.set([]);
        this.deployments.set([]);
        return;
      }
      forkJoin({
        services: this.api.services(projectId).pipe(catchError(() => of({ content: [] as ServiceItem[] }))),
        incidents: this.api.incidents(projectId).pipe(catchError(() => of({ content: [] as Incident[] }))),
        alerts: this.api.alerts(projectId).pipe(catchError(() => of({ content: [] as Alert[] }))),
        deployments: this.api.deployments(projectId).pipe(catchError(() => of({ content: [] as Deployment[] }))),
      }).subscribe(({ services, incidents, alerts, deployments }) => {
        this.services.set(services.content);
        this.incidents.set(incidents.content);
        this.alerts.set(alerts.content);
        this.deployments.set(deployments.content);
      });
    });
  }
}
