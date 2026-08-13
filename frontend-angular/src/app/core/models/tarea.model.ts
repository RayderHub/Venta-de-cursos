export interface Tarea {
  id: number;
  titulo: string;
  curso: string;
  fecha: string;
  prioridad: 'Alta' | 'Media' | 'Baja';
  estado: 'Pendiente' | 'En progreso' | 'Completada';
  progreso: number;
}

export type TareaContenidoTipo = 'titulo' | 'texto' | 'opcion_multiple' | 'respuesta_texto';

export interface TareaContenido {
  id: number;
  tarea_id: number;
  tipo: TareaContenidoTipo;
  contenido: string;
  orden: number;
  created_at: string;
}

export interface TareaContenidoInput {
  tarea_id: number;
  tipo: TareaContenidoTipo;
  contenido: string;
  orden: number;
}

export interface TareaResultado {
  id: number;
  usuario_id: string;
  tarea_id: number;
  calificacion: number;
  aprobada: boolean;
  respuestas: unknown;
  created_at: string;
}
