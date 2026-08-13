-- Tabla de contenido de tareas
CREATE TABLE IF NOT EXISTS tarea_contenido (
  id BIGSERIAL PRIMARY KEY,
  tarea_id BIGINT NOT NULL REFERENCES tareas(id) ON DELETE CASCADE,
  tipo TEXT NOT NULL CHECK (tipo IN ('titulo', 'texto', 'opcion_multiple', 'respuesta_texto')),
  contenido TEXT NOT NULL DEFAULT '',
  orden INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índice para mejor rendimiento
CREATE INDEX IF NOT EXISTS idx_tarea_contenido ON tarea_contenido(tarea_id);

-- Habilitar RLS (Row Level Security)
ALTER TABLE tarea_contenido ENABLE ROW LEVEL SECURITY;

-- Políticas de seguridad
CREATE POLICY "Lectura publica de contenido de tareas" ON tarea_contenido FOR SELECT USING (true);
CREATE POLICY "Profesor puede insertar contenido de tareas" ON tarea_contenido FOR INSERT WITH CHECK (true);
CREATE POLICY "Profesor puede actualizar contenido de tareas" ON tarea_contenido FOR UPDATE USING (true);
CREATE POLICY "Profesor puede eliminar contenido de tareas" ON tarea_contenido FOR DELETE USING (true);
