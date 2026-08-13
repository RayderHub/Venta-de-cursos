export type ContenidoTipo = 'titulo' | 'texto' | 'imagen' | 'tarjeta' | 'formulario';

export interface ContenidoLeccion {
  id: number;
  leccion_id: number;
  tipo: ContenidoTipo;
  contenido: string;
  orden: number;
  created_at: string;
}

export interface ContenidoInput {
  tipo: ContenidoTipo;
  contenido: string;
  orden: number;
}

export interface Leccion {
  id: number;
  curso_id: number;
  titulo: string;
  descripcion: string | null;
  orden: number;
  created_at: string;
  contenido?: ContenidoLeccion[];
}

export interface LeccionInput {
  curso_id: number;
  titulo: string;
  descripcion: string | null;
  orden: number;
}
