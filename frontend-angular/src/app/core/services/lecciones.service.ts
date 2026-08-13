import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { Leccion, LeccionInput, ContenidoLeccion, ContenidoInput } from '../models/leccion.model';

@Injectable({ providedIn: 'root' })
export class LeccionesService {
  private auth = inject(AuthService);

  private client() {
    return this.auth.getSupabase();
  }

  async listByCurso(cursoId: number): Promise<Leccion[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client
      .from('lecciones')
      .select('*')
      .eq('curso_id', cursoId)
      .order('orden', { ascending: true });

    if (error || !data) return [];
    return data as Leccion[];
  }

  async getWithContenido(leccionId: number): Promise<Leccion | null> {
    const client = this.client();
    if (!client) return null;

    const { data, error } = await client
      .from('lecciones')
      .select('*, contenido_leccion(*)')
      .eq('id', leccionId)
      .order('orden', { referencedTable: 'contenido_leccion', ascending: true })
      .single();

    if (error || !data) return null;

    const raw = data as any;
    return {
      id: raw.id,
      curso_id: raw.curso_id,
      titulo: raw.titulo,
      descripcion: raw.descripcion,
      orden: raw.orden,
      created_at: raw.created_at,
      contenido: raw.contenido_leccion || []
    } as Leccion;
  }

  async crearLeccion(input: LeccionInput): Promise<{ data: Leccion | null; error: Error | null }> {
    const client = this.client();
    if (!client) return { data: null, error: new Error('Supabase no disponible') };

    const { data, error } = await client
      .from('lecciones')
      .insert(input)
      .select()
      .single();

    return { data: (data as Leccion) ?? null, error: error ? new Error(error.message) : null };
  }

  async actualizarLeccion(id: number, updates: Partial<LeccionInput>): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('lecciones').update(updates).eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async eliminarLeccion(id: number): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('lecciones').delete().eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async agregarContenido(leccionId: number, input: ContenidoInput): Promise<{ data: ContenidoLeccion | null; error: Error | null }> {
    const client = this.client();
    if (!client) return { data: null, error: new Error('Supabase no disponible') };

    const { data, error } = await client
      .from('contenido_leccion')
      .insert({ ...input, leccion_id: leccionId })
      .select()
      .single();

    return { data: (data as ContenidoLeccion) ?? null, error: error ? new Error(error.message) : null };
  }

  async actualizarContenido(id: number, updates: Partial<ContenidoInput>): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('contenido_leccion').update(updates).eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async eliminarContenido(id: number): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('contenido_leccion').delete().eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async marcarVista(leccionId: number): Promise<{ error: Error | null }> {
    const client = this.client();
    const user = this.auth.user();
    if (!client || !user) return { error: new Error('Inicia sesion para marcar la leccion como vista') };

    const { error } = await client
      .from('lecciones_vistas')
      .upsert(
        { usuario_id: user.id, leccion_id: leccionId },
        { onConflict: 'usuario_id,leccion_id', ignoreDuplicates: true }
      );

    return { error: error ? new Error(error.message) : null };
  }

  async leccionesVistasDeIds(leccionIds: number[]): Promise<number[]> {
    const client = this.client();
    const user = this.auth.user();
    const ids = [...new Set(leccionIds)].filter((id) => Number.isFinite(id));
    if (!client || !user || ids.length === 0) return [];

    const { data, error } = await client
      .from('lecciones_vistas')
      .select('leccion_id')
      .in('leccion_id', ids);

    if (error || !data) return [];

    return (data as { leccion_id: number }[]).map((vista) => vista.leccion_id);
  }

  async leccionesVistasDeCurso(cursoId: number): Promise<number[]> {
    const lecciones = await this.listByCurso(cursoId);
    return this.leccionesVistasDeIds(lecciones.map((leccion) => leccion.id));
  }

  async progresoDeCurso(cursoId: number): Promise<number> {
    const lecciones = await this.listByCurso(cursoId);
    if (lecciones.length === 0) return 0;

    const vistas = await this.leccionesVistasDeIds(lecciones.map((leccion) => leccion.id));
    const idsVistas = new Set(vistas);

    const vistasCount = lecciones.filter((leccion) => idsVistas.has(leccion.id)).length;

    return Math.round((vistasCount / lecciones.length) * 100);
  }

  async reordenarLecciones(cursoId: number, leccionIds: number[]): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const updates = leccionIds.map((id, index) =>
      client.from('lecciones').update({ orden: index + 1 }).eq('id', id)
    );

    const results = await Promise.all(updates);
    const firstError = results.find((r) => r.error);
    return { error: firstError?.error ? new Error(firstError.error.message) : null };
  }
}
