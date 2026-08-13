import { isPlatformBrowser } from '@angular/common';
import { Component, PLATFORM_ID, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { EstadisticasDashboard } from '../../core/models/estadisticas.model';
import { Tarea } from '../../core/models/tarea.model';
import { WidgetWearable } from '../../core/models/widget-wearable.model';
import { DashboardService } from '../../core/services/dashboard.service';
import { WidgetService } from '../../core/services/widget.service';
import { StatCard } from '../../shared/components/stat-card/stat-card';
import { TaskCard } from '../../shared/components/task-card/task-card';

@Component({
  selector: 'app-dashboard',
  imports: [StatCard, TaskCard],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard {
  private readonly dashboardService = inject(DashboardService);
  private readonly widgetService = inject(WidgetService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly estadisticas = signal<EstadisticasDashboard | null>(null);
  readonly tareas = signal<Tarea[]>([]);
  readonly wearable = signal<WidgetWearable | null>(null);
  readonly cargando = signal(true);
  readonly error = signal('');

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.cargarDashboard();
    } else {
      this.cargando.set(false);
    }
  }

  private cargarDashboard(): void {
    forkJoin({
      estadisticas: this.dashboardService.getEstadisticas(),
      tareasResponse: this.dashboardService.getTareas(),
      wearable: this.widgetService.getWearableWidget()
    }).subscribe({
      next: ({ estadisticas, tareasResponse, wearable }) => {
        this.estadisticas.set(estadisticas);
        this.tareas.set(tareasResponse.tareas);
        this.wearable.set(wearable);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la informacion del dashboard. Revisa que el backend Fastify este encendido.');
        this.cargando.set(false);
      }
    });
  }
}
