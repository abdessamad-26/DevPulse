import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { ProjectContext } from '../../core/project-context.service';
import { LogEntry, Page, Project } from '../../core/models';
import { LogsComponent } from './logs.component';

describe('LogsComponent', () => {
  const project: Project = {
    id: 7,
    name: 'Payments',
    description: null,
    repository: null,
    environment: 'development',
    status: 'ACTIVE',
    createdAt: '2026-10-23T00:00:00',
    updatedAt: '2026-10-23T00:00:00',
  };

  it('shows request errors and allows retrying successfully', () => {
    const entry: LogEntry = {
      id: 1,
      projectId: project.id,
      serviceName: 'payments-api',
      environment: 'development',
      level: 'INFO',
      message: 'Service started',
      timestamp: '2026-10-23T00:00:00',
    };
    const page: Page<LogEntry> = {
      content: [entry],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    };
    let response: Observable<Page<LogEntry>> = throwError(() => new Error('network unavailable'));
    const api = {
      logs: () => response,
    };

    TestBed.configureTestingModule({
      imports: [LogsComponent],
      providers: [
        { provide: ApiService, useValue: api },
        {
          provide: ProjectContext,
          useValue: { selected: signal(project), selectedId: signal(project.id) },
        },
      ],
    });

    const fixture = TestBed.createComponent(LogsComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Could not load logs');
    expect(fixture.nativeElement.textContent).not.toContain('No log entries match');

    response = of(page);
    const retry = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.btn.btn-sm');
    expect(retry).toBeDefined();
    retry?.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Service started');
    expect(fixture.nativeElement.textContent).not.toContain('Could not load logs');
  });
});
