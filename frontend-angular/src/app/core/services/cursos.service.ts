import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { Course } from '../data/academy-data';
import { CursoDb, CursoInput } from '../models/curso.model';

@Injectable({ providedIn: 'root' })
export class CursosService {
  private auth = inject(AuthService);

  private client() {
    return this.auth.getSupabase();
  }

  toCourse(c: CursoDb): Course {
    return {
      id: c.id,
      title: c.titulo,
      category: c.categoria,
      instructor: c.instructor,
      instructorId: c.instructor_id,
      description: c.descripcion,
      rating: Number(c.rating),
      reviews: c.reviews,
      price: Number(c.precio),
      oldPrice: c.old_precio != null ? Number(c.old_precio) : undefined,
      level: c.nivel,
      image: c.imagen || 'code'
    };
  }

  private toDb(c: Course | CursoDb): CursoDb {
    if ('titulo' in c) return c;

    return {
      id: c.id,
      titulo: c.title,
      categoria: c.category,
      nivel: c.level,
      instructor: c.instructor,
      instructor_id: c.instructorId ?? null,
      precio: c.price,
      old_precio: c.oldPrice ?? null,
      imagen: c.image,
      descripcion: c.description ?? null,
      estado: 'Publicado',
      rating: c.rating,
      reviews: c.reviews,
      created_at: ''
    };
  }

  async listPublicos(): Promise<Course[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client
      .from('cursos')
      .select('*')
      .eq('estado', 'Publicado')
      .order('id', { ascending: true });

    if (error || !data || data.length === 0) return [];

    const cursos = data as CursoDb[];
    const activos = await this.filtrarInstructoresActivos(cursos);
    return activos.map((curso) => this.toCourse(curso));
  }

  async get(id: number): Promise<Course | null> {
    const client = this.client();
    if (!client) return null;

    const { data, error } = await client.from('cursos').select('*').eq('id', id).single();
    if (error || !data) return null;
    return this.toCourse(data as CursoDb);
  }

  async listTodos(): Promise<CursoDb[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client.from('cursos').select('*').order('id', { ascending: true });
    if (error || !data) return [];
    return data as CursoDb[];
  }

  async listMios(): Promise<CursoDb[]> {
    const client = this.client();
    const user = this.auth.user();
    if (!client || !user) return [];

    const { data, error } = await client
      .from('cursos')
      .select('*')
      .eq('instructor_id', user.id)
      .order('id', { ascending: true });

    if (error || !data) return [];
    return data as CursoDb[];
  }

  async listPublicosDe(instructorId: string | number): Promise<Course[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client
      .from('cursos')
      .select('*')
      .eq('instructor_id', String(instructorId))
      .eq('estado', 'Publicado')
      .order('id', { ascending: true });

    if (error || !data || data.length === 0) return [];

    const cursos = data as CursoDb[];
    const activos = await this.filtrarInstructoresActivos(cursos);
    return activos.map((curso) => this.toCourse(curso));
  }

  private async filtrarInstructoresActivos(cursos: CursoDb[]): Promise<CursoDb[]> {
    const client = this.client();
    const instructorIds = cursos
      .map((curso) => curso.instructor_id)
      .filter((id): id is string => !!id);

    if (instructorIds.length === 0 || !client) return cursos;

    const { data: perfiles, error } = await client
      .from('profiles')
      .select('id, is_active')
      .in('id', instructorIds);

    if (error || !perfiles || perfiles.length === 0) return cursos;

    const inactivos = new Set(
      perfiles
        .filter((perfil) => perfil.is_active === false)
        .map((perfil) => perfil.id)
    );

    return cursos.filter((curso) => !curso.instructor_id || !inactivos.has(curso.instructor_id));
  }

  async crear(input: CursoInput): Promise<{ data: CursoDb | null; error: Error | null }> {
    const client = this.client();
    const user = this.auth.user();
    if (!client) return { data: null, error: new Error('Supabase no disponible') };

    const row = { ...input, instructor_id: input.instructor_id ?? user?.id ?? null };
    const { data, error } = await client.from('cursos').insert(row).select().single();
    return { data: (data as CursoDb) ?? null, error: error ? new Error(error.message) : null };
  }

  async actualizar(id: number, updates: Partial<CursoInput>): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('cursos').update(updates).eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async eliminar(id: number): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('cursos').delete().eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async countAlumnos(cursoId: number): Promise<number> {
    const client = this.client();
    if (!client) return 0;

    const { count, error } = await client
      .from('inscripciones')
      .select('*', { count: 'exact', head: true })
      .eq('curso_id', cursoId);

    if (error) return 0;
    return count ?? 0;
  }
}
