import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { CATEGORIAS } from '../models/curso.model';

export interface CategoriaDb {
  id: number;
  nombre: string;
  descripcion: string | null;
  estado: 'Activa' | 'Inactiva';
  created_at: string;
}

export interface CategoriaInput {
  nombre: string;
  descripcion: string;
  estado: 'Activa' | 'Inactiva';
}

@Injectable({ providedIn: 'root' })
export class CategoriasService {
  private readonly auth = inject(AuthService);

  private client() {
    return this.auth.getSupabase();
  }

  private fallback(): CategoriaDb[] {
    return CATEGORIAS.map((nombre, index) => ({
      id: index + 1,
      nombre,
      descripcion: null,
      estado: 'Activa',
      created_at: ''
    }));
  }

  async list(): Promise<CategoriaDb[]> {
    const client = this.client();
    if (!client) return this.fallback();

    const { data, error } = await client.from('categorias').select('*').order('nombre', { ascending: true });
    if (error || !data) return this.fallback();
    return data as CategoriaDb[];
  }

  async nombresActivas(): Promise<string[]> {
    const categorias = await this.list();
    const activas = categorias.filter((categoria) => categoria.estado === 'Activa').map((categoria) => categoria.nombre);
    return activas.length > 0 ? activas : CATEGORIAS;
  }

  async crear(input: CategoriaInput): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('categorias').insert(input);
    return { error: error ? new Error(error.message) : null };
  }

  async actualizar(id: number, updates: Partial<CategoriaInput>): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('categorias').update(updates).eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async eliminar(id: number): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('categorias').delete().eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }
}
