-- Tabla de lecciones
CREATE TABLE IF NOT EXISTS lecciones (
  id BIGSERIAL PRIMARY KEY,
  curso_id BIGINT NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
  titulo TEXT NOT NULL,
  descripcion TEXT,
  orden INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tabla de contenido de lecciones
CREATE TABLE IF NOT EXISTS contenido_leccion (
  id BIGSERIAL PRIMARY KEY,
  leccion_id BIGINT NOT NULL REFERENCES lecciones(id) ON DELETE CASCADE,
  tipo TEXT NOT NULL CHECK (tipo IN ('titulo', 'texto', 'imagen', 'tarjeta', 'formulario')),
  contenido TEXT NOT NULL DEFAULT '',
  orden INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices para mejor rendimiento
CREATE INDEX IF NOT EXISTS idx_lecciones_curso ON lecciones(curso_id);
CREATE INDEX IF NOT EXISTS idx_contenido_leccion ON contenido_leccion(leccion_id);

-- Habilitar RLS (Row Level Security)
ALTER TABLE lecciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE contenido_leccion ENABLE ROW LEVEL SECURITY;

-- Políticas de seguridad para lecciones
CREATE POLICY "Lectura publica de lecciones" ON lecciones FOR SELECT USING (true);
CREATE POLICY "Admin puede insertar lecciones" ON lecciones FOR INSERT WITH CHECK (true);
CREATE POLICY "Admin puede actualizar lecciones" ON lecciones FOR UPDATE USING (true);
CREATE POLICY "Admin puede eliminar lecciones" ON lecciones FOR DELETE USING (true);

-- Políticas de seguridad para contenido
CREATE POLICY "Lectura publica de contenido" ON contenido_leccion FOR SELECT USING (true);
CREATE POLICY "Admin puede insertar contenido" ON contenido_leccion FOR INSERT WITH CHECK (true);
CREATE POLICY "Admin puede actualizar contenido" ON contenido_leccion FOR UPDATE USING (true);
CREATE POLICY "Admin puede eliminar contenido" ON contenido_leccion FOR DELETE USING (true);
