import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { Tarea, TareaContenido, TareaContenidoInput, TareaContenidoTipo, TareaResultado } from '../models/tarea.model';

export interface TareaInput {
  titulo: string;
  curso_id: number | null;
  curso: string;
  fecha: string;
  prioridad: Tarea['prioridad'];
  estado: Tarea['estado'];
  progreso: number;
  porcentaje_aprobacion: number;
}

export interface TareaDb extends TareaInput {
  id: number;
  creado_por: string | null;
  created_at: string;
}

@Injectable({ providedIn: 'root' })
export class TareasService {
  private auth = inject(AuthService);

  private client() {
    return this.auth.getSupabase();
  }

  private toTarea(t: TareaDb): Tarea {
    return {
      id: t.id,
      titulo: t.titulo,
      curso: t.curso,
      fecha: t.fecha,
      prioridad: t.prioridad,
      estado: t.estado,
      progreso: t.progreso
    };
  }

  async list(): Promise<Tarea[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client.from('tareas').select('*').order('fecha', { ascending: true });
    if (error || !data) return [];
    return (data as TareaDb[]).map((tarea) => this.toTarea(tarea));
  }

  async listDb(): Promise<TareaDb[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client.from('tareas').select('*').order('fecha', { ascending: true });
    if (error || !data) return [];
    return data as TareaDb[];
  }

  async listByCurso(cursoId: number): Promise<TareaDb[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client
      .from('tareas')
      .select('*')
      .eq('curso_id', cursoId)
      .order('fecha', { ascending: true });

    if (error || !data) return [];
    return data as TareaDb[];
  }

  async listParaAlumno(cursoIds: number[]): Promise<TareaDb[]> {
    const client = this.client();
    const ids = [...new Set(cursoIds)].filter((id) => Number.isFinite(id));
    if (!client || ids.length === 0) return [];

    const { data, error } = await client
      .from('tareas')
      .select('*')
      .in('curso_id', ids)
      .order('fecha', { ascending: true });

    if (error || !data) return [];
    return data as TareaDb[];
  }

  async crear(input: TareaInput): Promise<{ data: TareaDb | null; error: Error | null }> {
    const client = this.client();
    const user = this.auth.user();
    if (!client) return { data: null, error: new Error('Supabase no disponible') };

    const { data, error } = await client.from('tareas').insert({ ...input, creado_por: user?.id ?? null }).select().single();
    return { data: (data as TareaDb) ?? null, error: error ? new Error(error.message) : null };
  }

  async actualizar(id: number, updates: Partial<TareaInput>): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('tareas').update(updates).eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async eliminar(id: number): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('tareas').delete().eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async listContenido(tareaId: number): Promise<TareaContenido[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client
      .from('tarea_contenido')
      .select('*')
      .eq('tarea_id', tareaId)
      .order('orden', { ascending: true });

    if (error || !data) return [];
    return data as TareaContenido[];
  }

  async agregarContenido(input: TareaContenidoInput): Promise<{ data: TareaContenido | null; error: Error | null }> {
    const client = this.client();
    if (!client) return { data: null, error: new Error('Supabase no disponible') };

    const { data, error } = await client.from('tarea_contenido').insert(input).select().single();
    return { data: (data as TareaContenido) ?? null, error: error ? new Error(error.message) : null };
  }

  async actualizarContenido(id: number, updates: Partial<Pick<TareaContenidoInput, 'tipo' | 'contenido' | 'orden'>>): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('tarea_contenido').update(updates).eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async eliminarContenido(id: number): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('tarea_contenido').delete().eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  tiposContenido(): { valor: TareaContenidoTipo; etiqueta: string; icono: string }[] {
    return [
      { valor: 'titulo', etiqueta: 'Titulo', icono: 'H' },
      { valor: 'texto', etiqueta: 'Texto', icono: 'T' },
      { valor: 'opcion_multiple', etiqueta: 'Opcion multiple', icono: 'OPC' },
      { valor: 'respuesta_texto', etiqueta: 'Respuesta texto', icono: 'RTX' }
    ];
  }

  async resultadosDeTareas(tareaIds: number[]): Promise<TareaResultado[]> {
    const client = this.client();
    const user = this.auth.user();
    const ids = [...new Set(tareaIds)].filter((id) => Number.isFinite(id));
    if (!client || !user || ids.length === 0) return [];

    const { data, error } = await client
      .from('tareas_resultados')
      .select('*')
      .in('tarea_id', ids);

    if (error || !data) return [];
    return data as TareaResultado[];
  }

  async guardarResultado(tareaId: number, calificacion: number, aprobada: boolean, respuestas: unknown): Promise<{ data: TareaResultado | null; error: Error | null }> {
    const client = this.client();
    const user = this.auth.user();
    if (!client || !user) return { data: null, error: new Error('Supabase no disponible') };

    const { data, error } = await client
      .from('tareas_resultados')
      .upsert(
        { usuario_id: user.id, tarea_id: tareaId, calificacion, aprobada, respuestas },
        { onConflict: 'usuario_id,tarea_id', ignoreDuplicates: false }
      )
      .select()
      .single();

    return { data: (data as TareaResultado) ?? null, error: error ? new Error(error.message) : null };
  }
}
