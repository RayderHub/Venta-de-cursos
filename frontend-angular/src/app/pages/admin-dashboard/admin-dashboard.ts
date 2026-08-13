import { Component, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { AdminShell } from '../../shared/components/admin-shell/admin-shell';
import { AuthService } from '../../core/services/auth.service';
import { CursosService } from '../../core/services/cursos.service';
import { WidgetService } from '../../core/services/widget.service';
import { WidgetWearable } from '../../core/models/widget-wearable.model';
import { CursoDb } from '../../core/models/curso.model';

interface Actividad {
  nombre: string;
  email: string;
  curso: string;
  progreso: number;
}

interface Metricas {
  usuarios: number;
  cursos: number;
  inscripciones: number;
  bannersActivos: number;
}

interface Analytics {
  visitantesHoy: number;
  visitantesChange: number;
  paginasVistas: number;
  paginasChange: number;
  sesiones: number;
  sesionesChange: number;
  tasaRebote: number;
  reboteChange: number;
}

@Component({
  selector: 'app-admin-dashboard-page',
  imports: [AdminShell],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css'
})
export class AdminDashboardPage {
  private readonly auth = inject(AuthService);
  private readonly cursosService = inject(CursosService);
  private readonly widgetService = inject(WidgetService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly metricas = signal<Metricas>({ usuarios: 0, cursos: 0, inscripciones: 0, bannersActivos: 0 });
  readonly analytics = signal<Analytics>({
    visitantesHoy: 0,
    visitantesChange: 0,
    paginasVistas: 0,
    paginasChange: 0,
    sesiones: 0,
    sesionesChange: 0,
    tasaRebote: 0,
    reboteChange: 0
  });
  readonly actividad = signal<Actividad[]>([]);
  readonly cursos = signal<CursoDb[]>([]);
  readonly wearable = signal<WidgetWearable | null>(null);
  readonly wearableStatus = signal('');
  readonly cargando = signal(true);

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    } else {
      this.cargando.set(false);
    }
  }

  bar(value: number): number {
    return Math.min(value, 100);
  }

  private async cargar(): Promise<void> {
    const client = this.auth.getSupabase();
    if (!client) {
      this.cargando.set(false);
      return;
    }

    const [usuarios, cursos, inscripciones, banners, cursosData, wearable] = await Promise.all([
      client.from('profiles').select('*', { count: 'exact', head: true }),
      client.from('cursos').select('*', { count: 'exact', head: true }),
      client.from('inscripciones').select('*', { count: 'exact', head: true }),
      client.from('banners').select('*', { count: 'exact', head: true }).eq('estado', 'Activo'),
      this.cursosService.listTodos(),
      firstValueFrom(this.widgetService.getWearableWidget()).catch(() => null)
    ]);

    this.cursos.set(cursosData);
    this.wearable.set(wearable);

    this.metricas.set({
      usuarios: usuarios.count ?? 0,
      cursos: cursos.count ?? 0,
      inscripciones: inscripciones.count ?? 0,
      bannersActivos: banners.count ?? 0
    });

    const { data } = await client
      .from('inscripciones')
      .select('*, curso:cursos(titulo)')
      .order('created_at', { ascending: false })
      .limit(6);

    if (data) {
      const userIds = [...new Set(data.map((row) => row.usuario_id))];
      const { data: perfiles } = await client.from('profiles').select('id, full_name, email').in('id', userIds);
      const perfilMap = new Map((perfiles ?? []).map((perfil) => [perfil.id, perfil]));

      this.actividad.set(
        data.map((row) => {
          const perfil = perfilMap.get(row.usuario_id);
          return {
            nombre: perfil?.full_name ?? 'Estudiante',
            email: perfil?.email ?? 'sin correo',
            curso: (row.curso as { titulo: string } | null)?.titulo ?? 'Curso eliminado',
            progreso: row.progreso
          };
        })
      );
    }

    this.cargando.set(false);
    void this.cargarAnalytics();
  }

  private async cargarAnalytics(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 800));
    this.analytics.set({
      visitantesHoy: 142,
      visitantesChange: 12,
      paginasVistas: 387,
      paginasChange: 8,
      sesiones: 198,
      sesionesChange: 5,
      tasaRebote: 34,
      reboteChange: -3
    });
  }

  updateWearableField(field: keyof WidgetWearable, value: string | number): void {
    const current = this.wearable();
    if (!current) return;
    this.wearable.set({ ...current, [field]: field === 'progreso' ? Number(value) : value });
  }

  selectWearableCourse(cursoId: string): void {
    const curso = this.cursos().find((item) => String(item.id) === cursoId);
    const current = this.wearable();
    if (!curso || !current) return;
    this.wearable.set({
      ...current,
      curso: curso.titulo,
      mensaje: `Tienes una tarea pendiente del curso ${curso.titulo}.`,
      enlace: `/curso/${curso.id}`
    });
  }

  async guardarWearable(): Promise<void> {
    const current = this.wearable();
    if (!current) return;
    this.wearableStatus.set('Guardando...');
    try {
      const updated = await firstValueFrom(this.widgetService.updateWearableWidget(current));
      this.wearable.set(updated);
      this.wearableStatus.set('Widget smartwatch actualizado.');
    } catch {
      this.wearableStatus.set('No se pudo guardar. Revisa que Fastify este encendido.');
    }
  }
}
