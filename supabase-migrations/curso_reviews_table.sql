-- Tabla de reviews/calificaciones
CREATE TABLE IF NOT EXISTS curso_reviews (
  id BIGSERIAL PRIMARY KEY,
  usuario_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  curso_id BIGINT NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
  rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
  comment TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(usuario_id, curso_id)
);

-- Indices
CREATE INDEX IF NOT EXISTS idx_curso_reviews_curso ON curso_reviews(curso_id);
CREATE INDEX IF NOT EXISTS idx_curso_reviews_usuario ON curso_reviews(usuario_id);

-- RLS
ALTER TABLE curso_reviews ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Lectura publica reviews" ON curso_reviews;
CREATE POLICY "Lectura publica reviews" ON curso_reviews FOR SELECT USING (true);

DROP POLICY IF EXISTS "Usuario puede insertar review" ON curso_reviews;
CREATE POLICY "Usuario puede insertar review" ON curso_reviews FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Usuario puede actualizar review" ON curso_reviews;
CREATE POLICY "Usuario puede actualizar review" ON curso_reviews FOR UPDATE USING (true);

DROP POLICY IF EXISTS "Usuario puede eliminar review" ON curso_reviews;
CREATE POLICY "Usuario puede eliminar review" ON curso_reviews FOR DELETE USING (true);
