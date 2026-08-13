import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';

export interface CursoItem {
  id: number;
  curso_id: number;
  tipo: 'leccion' | 'tarea';
  item_id: number;
  posicion: number;
  created_at: string;
}

export interface CursoItemInput {
  tipo: 'leccion' | 'tarea';
  item_id: number;
}

@Injectable({ providedIn: 'root' })
export class CursoItemsService {
  private auth = inject(AuthService);

  private client() {
    return this.auth.getSupabase();
  }

  async listByCurso(cursoId: number): Promise<CursoItem[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client
      .from('curso_items')
      .select('*')
      .eq('curso_id', cursoId)
      .order('posicion', { ascending: true });

    if (error || !data) return [];
    return data as CursoItem[];
  }

  async guardarOrden(cursoId: number, items: CursoItemInput[]): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const filas = items.map((item, index) => ({
      curso_id: cursoId,
      tipo: item.tipo,
      item_id: item.item_id,
      posicion: index + 1
    }));

    const { error: delError } = await client.from('curso_items').delete().eq('curso_id', cursoId);
    if (delError) return { error: new Error(delError.message) };

    if (filas.length === 0) return { error: null };

    const { error } = await client.from('curso_items').insert(filas);
    return { error: error ? new Error(error.message) : null };
  }
}
