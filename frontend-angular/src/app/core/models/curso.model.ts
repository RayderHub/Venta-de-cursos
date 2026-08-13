export type CursoEstado = 'Publicado' | 'Borrador' | 'Archivado';

export interface CursoDb {
  id: number;
  titulo: string;
  categoria: string;
  nivel: string;
  instructor: string;
  instructor_id: string | null;
  precio: number;
  old_precio: number | null;
  imagen: string;
  descripcion: string | null;
  estado: CursoEstado;
  rating: number;
  reviews: number;
  created_at: string;
  students?: number;
}

export interface CursoInput {
  titulo: string;
  categoria: string;
  nivel: string;
  instructor: string;
  instructor_id: string | null;
  precio: number;
  old_precio: number | null;
  imagen: string;
  descripcion: string;
  estado: CursoEstado;
}

export const CATEGORIAS = ['Excel', 'Programacion', 'Diseno', 'Productividad'];
export const NIVELES = ['Principiante', 'Intermedio', 'Avanzado'];
export const IMAGENES = ['excel', 'code', 'laptop', 'screen', 'design', 'purple', 'banner'];
