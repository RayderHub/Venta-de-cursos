import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EstadisticasDashboard } from '../models/estadisticas.model';
import { Tarea } from '../models/tarea.model';

interface TareasResponse {
  tareas: Tarea[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  getEstadisticas(): Observable<EstadisticasDashboard> {
    return this.http.get<EstadisticasDashboard>('/api/dashboard/estadisticas');
  }

  getTareas(): Observable<TareasResponse> {
    return this.http.get<TareasResponse>('/api/dashboard/tareas');
  }
}
