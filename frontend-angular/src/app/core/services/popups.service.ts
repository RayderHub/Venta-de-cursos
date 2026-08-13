import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';

export interface PopupDb {
  id: number;
  titulo: string;
  mensaje: string | null;
  imagen: string;
  enlace: string | null;
  activo: boolean;
  created_at: string;
}

export interface PopupInput {
  titulo: string;
  mensaje?: string;
  imagen?: string;
  enlace?: string;
  activo?: boolean;
}

export interface CuponDb {
  id: number;
  codigo: string;
  descuento: number;
  activo: boolean;
  created_at: string;
}

export interface CuponInput {
  codigo: string;
  descuento: number;
  activo?: boolean;
}

@Injectable({ providedIn: 'root' })
export class PopupsService {
  private auth = inject(AuthService);

  private client() {
    return this.auth.getSupabase();
  }

  async listPopups(): Promise<PopupDb[]> {
    const client = this.client();
    if (!client) return [];
    const { data, error } = await client.from('popups').select('*').order('id', { ascending: true });
    if (error || !data) return [];
    return data as PopupDb[];
  }

  async crearPopup(input: PopupInput): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };
    const { error } = await client.from('popups').insert(input);
    return { error: error ? new Error(error.message) : null };
  }

  async actualizarPopup(id: number, updates: Partial<PopupInput>): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };
    const { error } = await client.from('popups').update(updates).eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async eliminarPopup(id: number): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };
    const { error } = await client.from('popups').delete().eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async listCupones(): Promise<CuponDb[]> {
    const client = this.client();
    if (!client) return [];
    const { data, error } = await client.from('cupones').select('*').order('id', { ascending: true });
    if (error || !data) return [];
    return data as CuponDb[];
  }

  async buscarCupon(codigo: string): Promise<CuponDb | null> {
    const client = this.client();
    const normalized = codigo.trim().toUpperCase();
    if (!client || !normalized) return null;

    const { data, error } = await client
      .from('cupones')
      .select('*')
      .eq('codigo', normalized)
      .eq('activo', true)
      .maybeSingle();

    if (error || !data) return null;
    return data as CuponDb;
  }

  async crearCupon(input: CuponInput): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };
    const { error } = await client.from('cupones').insert(input);
    return { error: error ? new Error(error.message) : null };
  }

  async actualizarCupon(id: number, updates: Partial<CuponInput>): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };
    const { error } = await client.from('cupones').update(updates).eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }

  async eliminarCupon(id: number): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };
    const { error } = await client.from('cupones').delete().eq('id', id);
    return { error: error ? new Error(error.message) : null };
  }
}
