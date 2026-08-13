-- Politicas RLS para profiles (permitir lectura publica)
DROP POLICY IF EXISTS "Lectura publica profiles" ON profiles;
CREATE POLICY "Lectura publica profiles" ON profiles FOR SELECT USING (true);

-- Politicas RLS para inscripciones (permitir lectura)
DROP POLICY IF EXISTS "Lectura inscripciones" ON inscripciones;
CREATE POLICY "Lectura inscripciones" ON inscripciones FOR SELECT USING (true);
