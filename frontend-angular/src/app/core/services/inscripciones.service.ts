import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { CursosService } from './cursos.service';
import { Course } from '../data/academy-data';
import { CursoDb } from '../models/curso.model';

export interface InscripcionDb {
  id: number;
  usuario_id: string;
  curso_id: number;
  progreso: number;
  created_at: string;
}

export interface AlumnoInscrito {
  usuario_id: string;
  nombre: string;
  email: string;
  curso_id: number;
  progreso: number;
  creado: string;
  rating: number;
  avatar_url?: string;
}

@Injectable({ providedIn: 'root' })
export class InscripcionesService {
  private auth = inject(AuthService);
  private cursos = inject(CursosService);

  private client() {
    return this.auth.getSupabase();
  }

  async listMisCursos(): Promise<{ curso: Course; progreso: number }[]> {
    const client = this.client();
    const user = this.auth.user();
    if (!client || !user) return [];

    const { data, error } = await client
      .from('inscripciones')
      .select('*, curso:cursos(*)')
      .eq('usuario_id', user.id)
      .order('id', { ascending: true });

    if (error || !data) return [];

    return data.flatMap((row) => {
      const curso = (row as { curso: CursoDb | null }).curso;
      if (!curso) return [];
      return [{ curso: this.cursos.toCourse(curso), progreso: (row as InscripcionDb).progreso }];
    });
  }

  async estaInscrito(cursoId: number): Promise<boolean> {
    const client = this.client();
    const user = this.auth.user();
    if (!client || !user) return false;

    const { data, error } = await client
      .from('inscripciones')
      .select('id')
      .eq('usuario_id', user.id)
      .eq('curso_id', cursoId)
      .maybeSingle();

    return !error && !!data;
  }

  async inscribir(cursoId: number): Promise<{ error: Error | null }> {
    const client = this.client();
    const user = this.auth.user();
    if (!client || !user) return { error: new Error('Inicia sesión para comprar') };

    const { error } = await client.from('inscripciones').upsert(
      { usuario_id: user.id, curso_id: cursoId, progreso: 0 },
      { onConflict: 'usuario_id,curso_id', ignoreDuplicates: true }
    );
    return { error: error ? new Error(error.message) : null };
  }

  async inscribirVarios(cursoIds: number[]): Promise<{ inscritos: number; error: Error | null }> {
    const client = this.client();
    const user = this.auth.user();
    const uniqueIds = [...new Set(cursoIds)];
    if (!client || !user) return { inscritos: 0, error: new Error('Inicia sesion para comprar') };
    if (uniqueIds.length === 0) return { inscritos: 0, error: null };

    const rows = uniqueIds.map((cursoId) => ({ usuario_id: user.id, curso_id: cursoId, progreso: 0 }));
    const { error } = await client
      .from('inscripciones')
      .upsert(rows, { onConflict: 'usuario_id,curso_id', ignoreDuplicates: true });

    return { inscritos: uniqueIds.length, error: error ? new Error(error.message) : null };
  }

  async desinscribir(cursoId: number): Promise<{ error: Error | null }> {
    const client = this.client();
    const user = this.auth.user();
    if (!client || !user) return { error: new Error('No autorizado') };

    const { error } = await client.from('inscripciones').delete().eq('usuario_id', user.id).eq('curso_id', cursoId);
    return { error: error ? new Error(error.message) : null };
  }

  async actualizarProgreso(cursoId: number, progreso: number): Promise<{ error: Error | null }> {
    const client = this.client();
    const user = this.auth.user();
    if (!client || !user) return { error: new Error('No autorizado') };

    const { error } = await client.from('inscripciones').update({ progreso }).eq('usuario_id', user.id).eq('curso_id', cursoId);
    return { error: error ? new Error(error.message) : null };
  }

  async calificarCurso(cursoId: number, rating: number, comment?: string): Promise<{ error: Error | null }> {
    const client = this.client();
    const user = this.auth.user();
    const bounded = Math.max(1, Math.min(5, Math.round(rating)));
    if (!client || !user) return { error: new Error('Inicia sesion para calificar') };

    const { error } = await client
      .from('curso_reviews')
      .upsert(
        { usuario_id: user.id, curso_id: cursoId, rating: bounded, comment: comment || null },
        { onConflict: 'usuario_id,curso_id' }
      );

    if (error) return { error: new Error(error.message) };

    const { error: refreshError } = await client.rpc('refresh_course_rating', { target_curso_id: cursoId });
    return { error: refreshError ? new Error(refreshError.message) : null };
  }

  async listMisCalificaciones(): Promise<Record<number, { stars: number; comment: string }>> {
    const result: Record<number, { stars: number; comment: string }> = {};
    const client = this.client();
    const user = this.auth.user();
    if (!client || !user) return result;

    const { data } = await client
      .from('curso_reviews')
      .select('curso_id, rating, comment')
      .eq('usuario_id', user.id);

    if (data) {
      data.forEach((r: any) => {
        result[r.curso_id] = { stars: r.rating, comment: r.comment || '' };
      });
    }
    return result;
  }

  async alumnosDeCurso(cursoId: number): Promise<AlumnoInscrito[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client
      .from('inscripciones')
      .select('*')
      .eq('curso_id', cursoId);

    if (error || !data) return [];

    const userIds = (data as { usuario_id: string }[]).map((row) => row.usuario_id);
    const perfiles = await this.cargarPerfiles(userIds);
    const ratings = await this.cargarRatings(cursoId, userIds);

    return (data as InscripcionDb[]).map((row) => ({
      usuario_id: row.usuario_id,
      curso_id: row.curso_id,
      progreso: row.progreso,
      creado: row.created_at,
      nombre: perfiles.get(row.usuario_id)?.full_name || perfiles.get(row.usuario_id)?.email?.split('@')[0] || 'Estudiante',
      email: perfiles.get(row.usuario_id)?.email || 'sin correo',
      rating: ratings.get(row.usuario_id) || 0,
      avatar_url: perfiles.get(row.usuario_id)?.avatar_url || undefined
    }));
  }

  private async cargarRatings(cursoId: number, userIds: string[]): Promise<Map<string, number>> {
    const result = new Map<string, number>();
    const client = this.client();
    if (!client || userIds.length === 0) return result;
    const { data } = await client
      .from('curso_reviews')
      .select('usuario_id, rating')
      .eq('curso_id', cursoId)
      .in('usuario_id', userIds);
    if (data) {
      data.forEach((r: any) => result.set(r.usuario_id, r.rating));
    }
    return result;
  }

  private async cargarPerfiles(ids: string[]): Promise<Map<string, { full_name: string | null; email: string; avatar_url: string | null }>> {
    const client = this.client();
    const result = new Map<string, { full_name: string | null; email: string; avatar_url: string | null }>();
    if (!client || ids.length === 0) return result;

    const { data, error } = await client.from('profiles').select('id, full_name, email, avatar_url').in('id', ids);
    if (error || !data) return result;

    data.forEach((perfil) => result.set(perfil.id, { full_name: perfil.full_name, email: perfil.email, avatar_url: perfil.avatar_url }));
    return result;
  }
}
