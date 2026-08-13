-- SkillAcademy seed de datos reales para Supabase
-- Ejecuta primero skillacademy_schema.sql. Luego pega este script en SQL Editor.
-- Credenciales demo para Auth: todos usan password Demo123456!

create extension if not exists pgcrypto;

create table if not exists public.categorias (
  id bigint generated always as identity primary key,
  nombre text not null unique,
  descripcion text default '',
  estado text not null default 'Activa',
  created_at timestamptz not null default now()
);

alter table public.categorias enable row level security;

drop policy if exists "categorias_select" on public.categorias;
create policy "categorias_select" on public.categorias
  for select
  using (estado = 'Activa' or public.current_role() = 'admin');

drop policy if exists "categorias_insert" on public.categorias;
create policy "categorias_insert" on public.categorias
  for insert
  with check (public.current_role() = 'admin');

drop policy if exists "categorias_update" on public.categorias;
create policy "categorias_update" on public.categorias
  for update
  using (public.current_role() = 'admin');

drop policy if exists "categorias_delete" on public.categorias;
create policy "categorias_delete" on public.categorias
  for delete
  using (public.current_role() = 'admin');

alter table public.banners add column if not exists descripcion text default '';

-- Usuarios Auth demo. En un proyecto productivo es mejor crearlos desde Supabase Auth UI/API.
insert into auth.users (
  instance_id, id, aud, role, email, encrypted_password,
  email_confirmed_at, created_at, updated_at,
  raw_app_meta_data, raw_user_meta_data
) values
  ('00000000-0000-0000-0000-000000000000', '11111111-1111-1111-1111-111111111111', 'authenticated', 'authenticated', 'nora.excel@skillacademy.dev', crypt('Demo123456!', gen_salt('bf')), now(), now(), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Nora Excel","role_id":"teacher"}'),
  ('00000000-0000-0000-0000-000000000000', '22222222-2222-2222-2222-222222222222', 'authenticated', 'authenticated', 'renato.codigo@skillacademy.dev', crypt('Demo123456!', gen_salt('bf')), now(), now(), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Renato Codigo","role_id":"teacher"}'),
  ('00000000-0000-0000-0000-000000000000', '33333333-3333-3333-3333-333333333333', 'authenticated', 'authenticated', 'lucia.diseno@skillacademy.dev', crypt('Demo123456!', gen_salt('bf')), now(), now(), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Lucia Diseno","role_id":"teacher"}'),
  ('00000000-0000-0000-0000-000000000000', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'authenticated', 'authenticated', 'mariana.alumna@skillacademy.dev', crypt('Demo123456!', gen_salt('bf')), now(), now(), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Mariana Alumna","role_id":"student"}'),
  ('00000000-0000-0000-0000-000000000000', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'authenticated', 'authenticated', 'julian.alumno@skillacademy.dev', crypt('Demo123456!', gen_salt('bf')), now(), now(), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Julian Alumno","role_id":"student"}'),
  ('00000000-0000-0000-0000-000000000000', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'authenticated', 'authenticated', 'sofia.alumna@skillacademy.dev', crypt('Demo123456!', gen_salt('bf')), now(), now(), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Sofia Alumna","role_id":"student"}')
on conflict (id) do update set
  email = excluded.email,
  raw_user_meta_data = excluded.raw_user_meta_data,
  updated_at = now();

insert into public.profiles (id, email, full_name, role_id, avatar_url, bio)
values
  ('11111111-1111-1111-1111-111111111111', 'nora.excel@skillacademy.dev', 'Nora Excel', 'teacher', '/assets/images/avatar-woman.svg', 'Especialista en automatizacion, reportes y dashboards con Excel.'),
  ('22222222-2222-2222-2222-222222222222', 'renato.codigo@skillacademy.dev', 'Renato Codigo', 'teacher', '/assets/images/avatar-man.svg', 'Frontend developer enfocado en proyectos practicos con Angular y JavaScript.'),
  ('33333333-3333-3333-3333-333333333333', 'lucia.diseno@skillacademy.dev', 'Lucia Diseno', 'teacher', '/assets/images/avatar-woman.svg', 'Disenadora UI con experiencia en branding, prototipos y sistemas visuales.'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'mariana.alumna@skillacademy.dev', 'Mariana Alumna', 'student', '/assets/images/avatar-woman.svg', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'julian.alumno@skillacademy.dev', 'Julian Alumno', 'student', '/assets/images/avatar-man.svg', null),
  ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'sofia.alumna@skillacademy.dev', 'Sofia Alumna', 'student', '/assets/images/avatar-woman.svg', null)
on conflict (id) do update set
  email = excluded.email,
  full_name = excluded.full_name,
  role_id = excluded.role_id,
  avatar_url = excluded.avatar_url,
  bio = excluded.bio,
  updated_at = now();

create table if not exists public.curso_reviews (
  id bigint generated always as identity primary key,
  usuario_id uuid not null references auth.users(id) on delete cascade,
  curso_id bigint not null references public.cursos(id) on delete cascade,
  rating integer not null check (rating between 1 and 5),
  comentario text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (usuario_id, curso_id)
);

alter table public.curso_reviews enable row level security;

drop policy if exists "curso_reviews_select" on public.curso_reviews;
create policy "curso_reviews_select" on public.curso_reviews
  for select using (true);

drop policy if exists "curso_reviews_insert_own_enrolled" on public.curso_reviews;
create policy "curso_reviews_insert_own_enrolled" on public.curso_reviews
  for insert to authenticated
  with check (
    auth.uid() = usuario_id
    and exists (
      select 1 from public.inscripciones i
      where i.usuario_id = auth.uid() and i.curso_id = curso_reviews.curso_id
    )
  );

drop policy if exists "curso_reviews_update_own" on public.curso_reviews;
create policy "curso_reviews_update_own" on public.curso_reviews
  for update to authenticated
  using (auth.uid() = usuario_id)
  with check (auth.uid() = usuario_id);

create or replace function public.refresh_course_rating(target_curso_id bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  update public.cursos c
  set rating = coalesce((
        select round(avg(r.rating)::numeric, 1)
        from public.curso_reviews r
        where r.curso_id = target_curso_id
      ), 5),
      reviews = (
        select count(*)
        from public.curso_reviews r
        where r.curso_id = target_curso_id
      )
  where c.id = target_curso_id;
end;
$$;

truncate table public.curso_reviews restart identity cascade;
truncate table public.inscripciones restart identity cascade;
truncate table public.tareas restart identity cascade;
truncate table public.cursos restart identity cascade;
truncate table public.categorias restart identity cascade;

insert into public.categorias (nombre, descripcion, estado)
values
  ('Excel', 'Cursos de hojas de calculo, reportes y automatizacion.', 'Activa'),
  ('Programacion', 'Cursos de desarrollo web, frontend, dashboards y APIs.', 'Activa'),
  ('Diseno', 'Cursos de UI, branding y productos digitales.', 'Activa'),
  ('Productividad', 'Cursos para mejorar procesos y organizacion profesional.', 'Activa');

insert into public.cursos (titulo, categoria, nivel, instructor, instructor_id, precio, old_precio, imagen, descripcion, estado)
values
  ('Excel Avanzado', 'Excel', 'Avanzado', 'Nora Excel', '11111111-1111-1111-1111-111111111111', 25, 26, 'excel', 'Domina tablas dinamicas, formulas avanzadas, Power Query y dashboards ejecutivos.', 'Publicado'),
  ('Automatizacion de Reportes en Excel', 'Excel', 'Intermedio', 'Nora Excel', '11111111-1111-1111-1111-111111111111', 32, null, 'screen', 'Crea reportes reutilizables con plantillas, validaciones y flujos de actualizacion.', 'Publicado'),
  ('Desarrollo Frontend Practico', 'Programacion', 'Intermedio', 'Renato Codigo', '22222222-2222-2222-2222-222222222222', 76, null, 'code', 'Construye interfaces modernas con componentes, rutas, servicios y consumo de APIs.', 'Publicado'),
  ('Angular para Dashboards', 'Programacion', 'Avanzado', 'Renato Codigo', '22222222-2222-2222-2222-222222222222', 88, 110, 'laptop', 'Aprende patrones de dashboards, guards, formularios y datos en tiempo real.', 'Publicado'),
  ('Diseno UI para Productos Digitales', 'Diseno', 'Principiante', 'Lucia Diseno', '33333333-3333-3333-3333-333333333333', 45, null, 'design', 'Bases visuales, jerarquia, color y componentes para productos digitales.', 'Publicado'),
  ('Branding Visual Express', 'Diseno', 'Intermedio', 'Lucia Diseno', '33333333-3333-3333-3333-333333333333', 39, 55, 'purple', 'Crea piezas visuales consistentes para marcas, cursos y promociones.', 'Publicado');

insert into public.inscripciones (usuario_id, curso_id, progreso)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 1, 65),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 3, 30),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 1, 80),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 5, 45),
  ('cccccccc-cccc-cccc-cccc-cccccccccccc', 2, 25),
  ('cccccccc-cccc-cccc-cccc-cccccccccccc', 4, 55);

insert into public.curso_reviews (usuario_id, curso_id, rating, comentario)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 1, 5, 'Muy claro y practico.'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 1, 4, 'Buen curso para reforzar formulas.'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 3, 5, 'Los proyectos ayudan mucho.'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 5, 4, 'Buen inicio para UI.'),
  ('cccccccc-cccc-cccc-cccc-cccccccccccc', 4, 5, 'Excelente estructura.'),
  ('cccccccc-cccc-cccc-cccc-cccccccccccc', 2, 4, 'Muy util para reportes.');

select public.refresh_course_rating(id) from public.cursos;

insert into public.tareas (titulo, curso_id, curso, fecha, prioridad, estado, progreso, dispositivo, creado_por)
values
  ('Completar modulo de formulas avanzadas', 1, 'Excel Avanzado', current_date + 2, 'Alta', 'Pendiente', 65, 'Web y smartwatch', '11111111-1111-1111-1111-111111111111'),
  ('Publicar dashboard final', 4, 'Angular para Dashboards', current_date + 5, 'Media', 'En progreso', 55, 'Web', '22222222-2222-2222-2222-222222222222'),
  ('Entregar moodboard visual', 5, 'Diseno UI para Productos Digitales', current_date + 4, 'Media', 'Pendiente', 45, 'Web', '33333333-3333-3333-3333-333333333333');

truncate table public.banners restart identity cascade;

insert into public.banners (titulo, subtitulo, descripcion, imagen, enlace, estado)
values
  ('Promocion de Excel Avanzado', 'Promocion activa', 'Accede a Excel Avanzado con precio especial para practicar formulas, tablas dinamicas, Power Query y dashboards ejecutivos con ejercicios reales.', 'green', '/catalogo', 'Activo'),
  ('Ruta Frontend Practica', 'Cursos tecnologicos', 'Promocion para estudiantes que quieren avanzar de fundamentos web a dashboards Angular consumiendo APIs y organizando componentes.', 'purple', '/catalogo', 'Activo'),
  ('Diseno UI para Productos Digitales', 'Oferta de diseno', 'Aprende jerarquia visual, color, sistemas de componentes y branding con cursos publicados por Lucia Diseno.', 'photo', '/catalogo', 'Activo');
