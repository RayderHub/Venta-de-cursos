-- Tabla de lecciones vistas (progreso del alumno por leccion)
CREATE TABLE IF NOT EXISTS lecciones_vistas (
  id BIGSERIAL PRIMARY KEY,
  usuario_id UUID NOT NULL,
  leccion_id BIGINT NOT NULL REFERENCES lecciones(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (usuario_id, leccion_id)
);

-- Indices para mejor rendimiento
CREATE INDEX IF NOT EXISTS idx_lecciones_vistas_usuario ON lecciones_vistas(usuario_id);
CREATE INDEX IF NOT EXISTS idx_lecciones_vistas_leccion ON lecciones_vistas(leccion_id);

-- Habilitar RLS (Row Level Security)
ALTER TABLE lecciones_vistas ENABLE ROW LEVEL SECURITY;

-- Politicas de seguridad: cada alumno solo accede a sus propias vistas
CREATE POLICY "Alumno lee sus lecciones vistas" ON lecciones_vistas FOR SELECT USING (auth.uid() = usuario_id);
CREATE POLICY "Alumno inserta sus lecciones vistas" ON lecciones_vistas FOR INSERT WITH CHECK (auth.uid() = usuario_id);
CREATE POLICY "Alumno actualiza sus lecciones vistas" ON lecciones_vistas FOR UPDATE USING (auth.uid() = usuario_id);
CREATE POLICY "Alumno elimina sus lecciones vistas" ON lecciones_vistas FOR DELETE USING (auth.uid() = usuario_id);
