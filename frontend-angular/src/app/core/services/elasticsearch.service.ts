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
  private readonly base = '/api/cursos/search';

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

    const response = await fetch(`${this.base}?${params.toString()}`);
    if (!response.ok) {
      throw new Error(`Buscador respondio ${response.status}`);
    }

    const body = await response.json();
    const cursos = Array.isArray(body.cursos) ? body.cursos.map((c: CursoIndexado) => this.aCourse(c)) : [];
    return { total: Number(body.total) || cursos.length, cursos };
  }
}
