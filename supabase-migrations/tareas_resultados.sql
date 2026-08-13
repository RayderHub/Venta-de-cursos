-- Columna de porcentaje de aprobacion por tarea (usada para desbloquear lecciones siguientes)
ALTER TABLE tareas ADD COLUMN IF NOT EXISTS porcentaje_aprobacion INTEGER NOT NULL DEFAULT 60;

-- Tabla de resultados de tareas por alumno
CREATE TABLE IF NOT EXISTS tareas_resultados (
  id BIGSERIAL PRIMARY KEY,
  usuario_id UUID NOT NULL,
  tarea_id BIGINT NOT NULL REFERENCES tareas(id) ON DELETE CASCADE,
  calificacion NUMERIC(5,2) NOT NULL DEFAULT 0,
  aprobada BOOLEAN NOT NULL DEFAULT FALSE,
  respuestas JSONB DEFAULT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (usuario_id, tarea_id)
);

-- Indices para mejor rendimiento
CREATE INDEX IF NOT EXISTS idx_tareas_resultados_usuario ON tareas_resultados(usuario_id);
CREATE INDEX IF NOT EXISTS idx_tareas_resultados_tarea ON tareas_resultados(tarea_id);

-- Habilitar RLS (Row Level Security)
ALTER TABLE tareas_resultados ENABLE ROW LEVEL SECURITY;

-- Politicas de seguridad: cada alumno solo accede a sus propios resultados
CREATE POLICY "Alumno lee sus resultados de tareas" ON tareas_resultados FOR SELECT USING (auth.uid() = usuario_id);
CREATE POLICY "Alumno inserta sus resultados de tareas" ON tareas_resultados FOR INSERT WITH CHECK (auth.uid() = usuario_id);
CREATE POLICY "Alumno actualiza sus resultados de tareas" ON tareas_resultados FOR UPDATE USING (auth.uid() = usuario_id);
CREATE POLICY "Alumno elimina sus resultados de tareas" ON tareas_resultados FOR DELETE USING (auth.uid() = usuario_id);
