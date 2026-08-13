-- Tabla de secuencia unificada (lecciones + tareas) por curso
-- Permite que el profesor defina un orden intercalado entre tipos.
CREATE TABLE IF NOT EXISTS curso_items (
  id BIGSERIAL PRIMARY KEY,
  curso_id BIGINT NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
  tipo TEXT NOT NULL CHECK (tipo IN ('leccion', 'tarea')),
  item_id BIGINT NOT NULL,
  posicion INTEGER NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (curso_id, tipo, item_id),
  UNIQUE (curso_id, posicion)
);

CREATE INDEX IF NOT EXISTS idx_curso_items_curso ON curso_items(curso_id);

ALTER TABLE curso_items ENABLE ROW LEVEL SECURITY;

-- Backfill: para cursos existentes, construye la secuencia intercalando
-- lecciones y tareas por created_at (equivalente al sidebar actual).
-- Solo se ejecuta cuando la tabla esta vacia (una sola vez, datos legados).
DO $$
BEGIN
  IF (SELECT COUNT(*) FROM curso_items) = 0 THEN
    INSERT INTO curso_items (curso_id, tipo, item_id, posicion)
    SELECT
      sub.curso_id,
      sub.tipo,
      sub.item_id,
      ROW_NUMBER() OVER (PARTITION BY sub.curso_id ORDER BY sub.created_at, sub.sort_id) AS posicion
    FROM (
      SELECT curso_id, 'leccion' AS tipo, id AS item_id, created_at, id AS sort_id FROM lecciones
      UNION ALL
      SELECT curso_id, 'tarea' AS tipo, id AS item_id, created_at, id AS sort_id FROM tareas
    ) sub;
  END IF;
END $$;

-- RLS: lectura para admin, instructor del curso o alumno inscrito
CREATE POLICY "curso_items_select" ON public.curso_items
  FOR SELECT
  USING (
    public.current_role() = 'admin'
    OR EXISTS (
      SELECT 1 FROM public.cursos c
      WHERE c.id = curso_items.curso_id AND c.instructor_id = auth.uid()
    )
    OR EXISTS (
      SELECT 1 FROM public.inscripciones i
      WHERE i.curso_id = curso_items.curso_id AND i.usuario_id = auth.uid()
    )
  );

CREATE POLICY "curso_items_insert" ON public.curso_items
  FOR INSERT
  WITH CHECK (
    public.current_role() = 'admin'
    OR EXISTS (
      SELECT 1 FROM public.cursos c
      WHERE c.id = curso_items.curso_id AND c.instructor_id = auth.uid()
    )
  );

CREATE POLICY "curso_items_update" ON public.curso_items
  FOR UPDATE
  USING (
    public.current_role() = 'admin'
    OR EXISTS (
      SELECT 1 FROM public.cursos c
      WHERE c.id = curso_items.curso_id AND c.instructor_id = auth.uid()
    )
  );

CREATE POLICY "curso_items_delete" ON public.curso_items
  FOR DELETE
  USING (
    public.current_role() = 'admin'
    OR EXISTS (
      SELECT 1 FROM public.cursos c
      WHERE c.id = curso_items.curso_id AND c.instructor_id = auth.uid()
    )
  );
