import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiService } from './api.service';
import { Project, ProjectPayload } from './models';

const SELECTED_KEY = 'devpulse.selectedProject';

function readSelected(): number | null {
  try {
    const raw = localStorage.getItem(SELECTED_KEY);
    const n = raw ? Number(raw) : NaN;
    return Number.isFinite(n) ? n : null;
  } catch {
    return null;
  }
}

/** Holds the user's projects and the project currently being observed by every page. */
@Injectable({ providedIn: 'root' })
export class ProjectContext {
  private readonly api = inject(ApiService);

  readonly projects = signal<Project[]>([]);
  readonly loaded = signal(false);
  readonly error = signal<string | null>(null);
  private readonly selectedIdSignal = signal<number | null>(readSelected());

  readonly selected = computed(() => {
    const list = this.projects();
    const id = this.selectedIdSignal();
    return list.find((p) => p.id === id) ?? list[0] ?? null;
  });
  readonly selectedId = computed(() => this.selected()?.id ?? null);

  load(): void {
    this.api.projects().subscribe({
      next: (result) => {
        const list = result.content;
        this.projects.set(list);
        this.loaded.set(true);
        this.error.set(null);

        const currentSelected = this.selectedIdSignal();
        const hasCurrentSelection = list.some((project) => project.id === currentSelected);
        if (!hasCurrentSelection) {
          const fallbackProject = list[0];
          if (fallbackProject) {
            this.select(fallbackProject.id);
          }
        }
      },
      error: () => {
        this.loaded.set(true);
        this.error.set('Impossible de charger les projets (backend indisponible ?).');
      },
    });
  }

  select(id: number): void {
    this.selectedIdSignal.set(id);
    try {
      localStorage.setItem(SELECTED_KEY, String(id));
    } catch {
      /* storage unavailable */
    }
  }

  create(payload: ProjectPayload): Observable<Project> {
    return this.api.createProject(payload).pipe(
      tap((project) => {
        this.projects.update((list) => [...list, project]);
        this.select(project.id);
      }),
    );
  }

  reset(): void {
    this.projects.set([]);
    this.loaded.set(false);
  }
}
