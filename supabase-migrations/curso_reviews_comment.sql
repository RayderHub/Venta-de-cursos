-- Agregar columna comment a curso_reviews
ALTER TABLE curso_reviews ADD COLUMN IF NOT EXISTS comment TEXT;

-- Crear indices para mejor rendimiento
CREATE INDEX IF NOT EXISTS idx_curso_reviews_curso ON curso_reviews(curso_id);
CREATE INDEX IF NOT EXISTS idx_curso_reviews_usuario ON curso_reviews(usuario_id);
