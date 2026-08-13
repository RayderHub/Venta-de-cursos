import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';

export interface BannerDb {
  id: number;
  titulo: string;
  subtitulo: string;
  descripcion: string | null;
  imagen: string;
  enlace: string;
  estado: 'Activo' | 'Inactivo';
  created_at: string;
}

export interface BannerInput {
  titulo: string;
  subtitulo: string;
  descripcion: string;
  imagen: string;
  enlace: string;
  estado: 'Activo' | 'Inactivo';
}

@Injectable({ providedIn: 'root' })
export class BannersService {
  private auth = inject(AuthService);

  private client() {
    return this.auth.getSupabase();
  }

  async list(): Promise<BannerDb[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client.from('banners').select('*').order('id', { ascending: true });
    if (error || !data) return [];
    return data as BannerDb[];
  }

  async crear(input: BannerInput): Promise<{ data: BannerDb | null; error: Error | null }> {
    const client = this.client();
    if (!client) return { data: null, error: new Error('Supabase no disponible') };

    const { data, error } = await client.from('banners').insert(input).select().single();
    return { data: (data as BannerDb) ?? null, error: error ? new Error(error.message) : null };
  }

  async actualizar(id: number, updates: Partial<BannerInput>): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('banners').update(updates).eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async eliminar(id: number): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('banners').delete().eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }
}
