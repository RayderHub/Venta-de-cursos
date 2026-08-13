-- Eliminar campo duplicado comentario (mantener solo comment)
ALTER TABLE curso_reviews DROP COLUMN IF EXISTS comentario;
