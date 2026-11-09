import { Injectable } from '@angular/core';
import { Course } from '../data/academy-data';

export interface BusquedaResultado {
  total: number;
  cursos: Course[];
}

export interface CursoIndexado {
  id: number;
  titulo: string;
  categoria: string;
  nivel: string;
  instructor: string;
  instructor_id: string | null;
  precio: number;
  old_precio: number | null;
  imagen: string;
  descripcion: string;
  rating: number;
  reviews: number;
}

@Injectable({ providedIn: 'root' })
export class ElasticsearchService {
  private readonly relativeBase = '/api/cursos/search';
  private readonly directBase = 'https://skillacademy-backend-at8o.onrender.com/api/cursos/search';

  private aCourse(c: CursoIndexado): Course {
    return {
      id: c.id,
      title: c.titulo,
      category: c.categoria,
      instructor: c.instructor,
      instructorId: c.instructor_id ?? undefined,
      description: c.descripcion,
      rating: Number(c.rating) || 0,
      reviews: c.reviews || 0,
      price: Number(c.precio) || 0,
      oldPrice: c.old_precio != null ? Number(c.old_precio) : undefined,
      level: c.nivel,
      image: c.imagen || 'code'
    };
  }

  async buscar(q: string): Promise<BusquedaResultado> {
    const params = new URLSearchParams();
    if (q.trim()) params.set('q', q.trim());
    const queryStr = params.toString() ? `?${params.toString()}` : '';

    let body: any = null;

    // 1. Intentar vía rewrite de Vercel
    try {
      const res = await fetch(`${this.relativeBase}${queryStr}`);
      const contentType = res.headers.get('content-type') || '';
      if (res.ok && contentType.includes('application/json')) {
        body = await res.json();
      }
    } catch {
      // Ignorar y pasar a conexión directa
    }

    // 2. Si falló o devolvió HTML de SPA, consultar directo al backend en Render
    if (!body || !Array.isArray(body.cursos)) {
      const directRes = await fetch(`${this.directBase}${queryStr}`);
      if (!directRes.ok) {
        throw new Error(`Buscador respondio ${directRes.status}`);
      }
      body = await directRes.json();
    }

    const cursos = Array.isArray(body.cursos) ? body.cursos.map((c: CursoIndexado) => this.aCourse(c)) : [];
    return { total: Number(body.total) || cursos.length, cursos };
  }
}
