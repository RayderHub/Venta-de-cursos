-- =============================================================
-- SkillAcademy - ESQUEMA COMPLETO (base de datos nueva)
-- Pega TODO este script en el SQL Editor de Supabase y ejecutalo.
-- Crea: auth profiles + trigger, cursos, tareas, banners,
-- inscripciones, popups, cupones, RLS por rol y datos semilla.
-- =============================================================

-- -------------------------------------------------------------
-- Profiles (perfil de cada usuario) + auto-creacion al registrarse
-- -------------------------------------------------------------
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text,
  full_name text,
  avatar_url text,
  bio text,
  role_id text not null default 'student',
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conname = 'profiles_full_name_max_length'
  ) then
    alter table public.profiles
      add constraint profiles_full_name_max_length
      check (full_name is null or char_length(full_name) <= 45);
  end if;
end;
$$;

-- -------------------------------------------------------------
-- Invitaciones creadas desde /admin/usuarios
-- Al registrarse con el mismo email, el usuario recibe el rol invitado.
-- -------------------------------------------------------------
create table if not exists public.user_invitations (
  id bigint generated always as identity primary key,
  email text not null,
  full_name text,
  role_id text not null default 'student',
  status text not null default 'Pendiente', -- Pendiente | Usada | Cancelada
  created_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  used_at timestamptz
);

create unique index if not exists user_invitations_pending_email_idx
  on public.user_invitations (lower(email))
  where status = 'Pendiente';

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  invitation_id bigint;
  invitation_name text;
  invitation_role text;
begin
  select id, full_name, role_id
  into invitation_id, invitation_name, invitation_role
  from public.user_invitations
  where lower(email) = lower(new.email)
    and status = 'Pendiente'
  order by created_at desc
  limit 1;

  insert into public.profiles (id, email, full_name, role_id, avatar_url)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data ->> 'full_name', invitation_name, split_part(new.email, '@', 1)),
    coalesce(invitation_role, new.raw_user_meta_data ->> 'role_id', 'student'),
    new.raw_user_meta_data ->> 'avatar_url'
  )
  on conflict (id) do nothing;

  if invitation_id is not null then
    update public.user_invitations
    set status = 'Usada', used_at = now()
    where id = invitation_id;
  end if;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- -------------------------------------------------------------
-- Storage para avatars de perfil (usado por /perfil)
-- -------------------------------------------------------------
insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', true)
on conflict (id) do update set public = excluded.public;

drop policy if exists "avatars_select_public" on storage.objects;
create policy "avatars_select_public" on storage.objects
  for select
  using (bucket_id = 'avatars');

drop policy if exists "avatars_insert_own" on storage.objects;
create policy "avatars_insert_own" on storage.objects
  for insert
  to authenticated
  with check (
    bucket_id = 'avatars'
    and (storage.foldername(name))[1] = 'avatars'
    and split_part(storage.filename(name), '.', 1) = auth.uid()::text
  );

drop policy if exists "avatars_update_own" on storage.objects;
create policy "avatars_update_own" on storage.objects
  for update
  to authenticated
  using (
    bucket_id = 'avatars'
    and (storage.foldername(name))[1] = 'avatars'
    and split_part(storage.filename(name), '.', 1) = auth.uid()::text
  )
  with check (
    bucket_id = 'avatars'
    and (storage.foldername(name))[1] = 'avatars'
    and split_part(storage.filename(name), '.', 1) = auth.uid()::text
  );

drop policy if exists "avatars_delete_own" on storage.objects;
create policy "avatars_delete_own" on storage.objects
  for delete
  to authenticated
  using (
    bucket_id = 'avatars'
    and (storage.foldername(name))[1] = 'avatars'
    and split_part(storage.filename(name), '.', 1) = auth.uid()::text
  );

-- -------------------------------------------------------------
-- Cursos
-- -------------------------------------------------------------
create table if not exists public.cursos (
  id bigint generated always as identity primary key,
  titulo text not null,
  categoria text not null default 'Programacion',
  nivel text not null default 'Intermedio',
  instructor text not null default 'SkillAcademy',
  instructor_id uuid references auth.users(id) on delete set null,
  precio numeric not null default 0,
  old_precio numeric,
  imagen text not null default 'code',
  descripcion text,
  estado text not null default 'Borrador', -- Publicado | Borrador | Archivado
  rating numeric not null default 5,
  reviews integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- -------------------------------------------------------------
-- Categorias de cursos
-- -------------------------------------------------------------
create table if not exists public.categorias (
  id bigint generated always as identity primary key,
  nombre text not null unique,
  descripcion text default '',
  estado text not null default 'Activa', -- Activa | Inactiva
  created_at timestamptz not null default now()
);

-- -------------------------------------------------------------
-- Tareas (gestionadas por profesor/admin)
-- -------------------------------------------------------------
create table if not exists public.tareas (
  id bigint generated always as identity primary key,
  titulo text not null,
  curso_id bigint references public.cursos(id) on delete set null,
  curso text not null default '',
  fecha date not null default current_date,
  prioridad text not null default 'Media', -- Alta | Media | Baja
  estado text not null default 'Pendiente', -- Pendiente | En progreso | Completada
  progreso integer not null default 0,
  dispositivo text not null default 'Web',
  creado_por uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now()
);

-- -------------------------------------------------------------
-- Banners promocionales
-- -------------------------------------------------------------
create table if not exists public.banners (
  id bigint generated always as identity primary key,
  titulo text not null,
  subtitulo text default '',
  descripcion text default '',
  imagen text not null default 'green', -- green | purple | dark | photo
  enlace text default '',
  estado text not null default 'Activo', -- Activo | Inactivo
  created_at timestamptz not null default now()
);

alter table public.banners add column if not exists descripcion text default '';

-- -------------------------------------------------------------
-- Inscripciones (estudiante -> curso)
-- -------------------------------------------------------------
create table if not exists public.inscripciones (
  id bigint generated always as identity primary key,
  usuario_id uuid not null references auth.users(id) on delete cascade,
  curso_id bigint not null references public.cursos(id) on delete cascade,
  progreso integer not null default 0,
  created_at timestamptz not null default now(),
  unique (usuario_id, curso_id)
);

-- -------------------------------------------------------------
-- Popups / ventanas emergentes
-- -------------------------------------------------------------
create table if not exists public.popups (
  id bigint generated always as identity primary key,
  titulo text not null,
  mensaje text default '',
  imagen text not null default 'green', -- green | purple | dark | photo
  enlace text default '',
  activo boolean not null default true,
  created_at timestamptz not null default now()
);

-- -------------------------------------------------------------
-- Cupones de descuento
-- -------------------------------------------------------------
create table if not exists public.cupones (
  id bigint generated always as identity primary key,
  codigo text not null unique,
  descuento numeric not null default 10,
  activo boolean not null default true,
  created_at timestamptz not null default now()
);

-- -------------------------------------------------------------
-- Funcion auxiliar: rol del usuario autenticado
-- -------------------------------------------------------------
create or replace function public.current_role()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select coalesce((select role_id from public.profiles where id = auth.uid()), 'student');
$$;

create or replace function public.protect_profile_admin_fields()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    return new;
  end if;

  if (old.role_id is distinct from new.role_id or old.is_active is distinct from new.is_active)
    and public.current_role() <> 'admin' then
    raise exception 'Solo un administrador puede cambiar rol o estado';
  end if;

  return new;
end;
$$;

drop trigger if exists profiles_protect_admin_fields on public.profiles;
create trigger profiles_protect_admin_fields
  before update on public.profiles
  for each row execute function public.protect_profile_admin_fields();

create or replace function public.admin_delete_user(target_user_id uuid)
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
begin
  if auth.uid() is null or public.current_role() <> 'admin' then
    raise exception 'Solo un administrador puede eliminar usuarios';
  end if;

  if target_user_id = auth.uid() then
    raise exception 'No puedes eliminar tu propia cuenta desde este panel';
  end if;

  delete from auth.users where id = target_user_id;
end;
$$;

-- -------------------------------------------------------------
-- RLS: Categorias (publico ve activas; admin gestiona)
-- -------------------------------------------------------------
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

-- -------------------------------------------------------------
-- RLS: Cursos
--   Publico: solo Publicado. Profesor: los suyos. Admin: todo.
-- -------------------------------------------------------------
alter table public.cursos enable row level security;

drop policy if exists "cursos_select" on public.cursos;
create policy "cursos_select" on public.cursos
  for select
  using (estado = 'Publicado' or auth.uid() = instructor_id or public.current_role() in ('admin', 'teacher'));

drop policy if exists "cursos_insert" on public.cursos;
create policy "cursos_insert" on public.cursos
  for insert
  with check (public.current_role() in ('admin', 'teacher'));

drop policy if exists "cursos_update" on public.cursos;
create policy "cursos_update" on public.cursos
  for update
  using (public.current_role() = 'admin' or (public.current_role() = 'teacher' and auth.uid() = instructor_id));

drop policy if exists "cursos_delete" on public.cursos;
create policy "cursos_delete" on public.cursos
  for delete
  using (public.current_role() = 'admin' or (public.current_role() = 'teacher' and auth.uid() = instructor_id));

-- -------------------------------------------------------------
-- RLS: Tareas (profesor y admin gestionan; todos ven las suyas)
-- -------------------------------------------------------------
alter table public.tareas enable row level security;

drop policy if exists "tareas_select" on public.tareas;
create policy "tareas_select" on public.tareas
  for select
  using (
    public.current_role() = 'admin'
    or auth.uid() = creado_por
    or exists (
      select 1 from public.cursos c
      where c.id = tareas.curso_id and c.instructor_id = auth.uid()
    )
    or exists (
      select 1 from public.inscripciones i
      where i.curso_id = tareas.curso_id and i.usuario_id = auth.uid()
    )
  );

drop policy if exists "tareas_insert" on public.tareas;
create policy "tareas_insert" on public.tareas
  for insert
  with check (
    public.current_role() = 'admin'
    or (
      public.current_role() = 'teacher'
      and (creado_por is null or creado_por = auth.uid())
      and (
        curso_id is null
        or exists (
          select 1 from public.cursos c
          where c.id = tareas.curso_id and c.instructor_id = auth.uid()
        )
      )
    )
  );

drop policy if exists "tareas_update" on public.tareas;
create policy "tareas_update" on public.tareas
  for update
  using (
    public.current_role() = 'admin'
    or auth.uid() = creado_por
    or exists (
      select 1 from public.cursos c
      where c.id = tareas.curso_id and c.instructor_id = auth.uid()
    )
  );

drop policy if exists "tareas_delete" on public.tareas;
create policy "tareas_delete" on public.tareas
  for delete
  using (
    public.current_role() = 'admin'
    or auth.uid() = creado_por
    or exists (
      select 1 from public.cursos c
      where c.id = tareas.curso_id and c.instructor_id = auth.uid()
    )
  );

-- -------------------------------------------------------------
-- RLS: Banners (publico ve activos; admin gestiona)
-- -------------------------------------------------------------
alter table public.banners enable row level security;

drop policy if exists "banners_select" on public.banners;
create policy "banners_select" on public.banners
  for select
  using (estado = 'Activo' or public.current_role() = 'admin');

drop policy if exists "banners_insert" on public.banners;
create policy "banners_insert" on public.banners
  for insert
  with check (public.current_role() = 'admin');

drop policy if exists "banners_update" on public.banners;
create policy "banners_update" on public.banners
  for update
  using (public.current_role() = 'admin');

drop policy if exists "banners_delete" on public.banners;
create policy "banners_delete" on public.banners
  for delete
  using (public.current_role() = 'admin');

-- -------------------------------------------------------------
-- RLS: Inscripciones (estudiante gestiona las suyas; profesor ve las de sus cursos)
-- -------------------------------------------------------------
alter table public.inscripciones enable row level security;

drop policy if exists "inscripciones_select" on public.inscripciones;
create policy "inscripciones_select" on public.inscripciones
  for select
  using (auth.uid() = usuario_id
    or public.current_role() = 'admin'
    or exists (
      select 1 from public.cursos c
      where c.id = inscripciones.curso_id and c.instructor_id = auth.uid()
    ));

drop policy if exists "inscripciones_insert" on public.inscripciones;
create policy "inscripciones_insert" on public.inscripciones
  for insert
  with check (auth.uid() = usuario_id and public.current_role() = 'student');

drop policy if exists "inscripciones_update" on public.inscripciones;
create policy "inscripciones_update" on public.inscripciones
  for update
  using (auth.uid() = usuario_id or public.current_role() = 'admin');

drop policy if exists "inscripciones_delete" on public.inscripciones;
create policy "inscripciones_delete" on public.inscripciones
  for delete
  using (auth.uid() = usuario_id or public.current_role() = 'admin');

-- -------------------------------------------------------------
-- RLS: Profiles (admin gestiona roles/estado; cada quien su perfil)
--   Se permite leer perfiles de profesores con cursos publicados
--   para que cualquier persona pueda ver su perfil publico.
-- -------------------------------------------------------------
alter table public.profiles enable row level security;

drop policy if exists "profiles_select" on public.profiles;
create policy "profiles_select" on public.profiles
  for select
  using (
    auth.uid() = id
    or public.current_role() = 'admin'
    or exists (
      select 1 from public.cursos c
      where c.instructor_id = profiles.id and c.estado = 'Publicado'
    )
  );

drop policy if exists "profiles_update" on public.profiles;
create policy "profiles_update" on public.profiles
  for update
  using (auth.uid() = id or public.current_role() = 'admin');

-- -------------------------------------------------------------
-- RLS: Invitaciones de usuarios (solo admin)
-- -------------------------------------------------------------
alter table public.user_invitations enable row level security;

drop policy if exists "user_invitations_select" on public.user_invitations;
create policy "user_invitations_select" on public.user_invitations
  for select
  using (public.current_role() = 'admin');

drop policy if exists "user_invitations_insert" on public.user_invitations;
create policy "user_invitations_insert" on public.user_invitations
  for insert
  with check (public.current_role() = 'admin');

drop policy if exists "user_invitations_update" on public.user_invitations;
create policy "user_invitations_update" on public.user_invitations
  for update
  using (public.current_role() = 'admin');

drop policy if exists "user_invitations_delete" on public.user_invitations;
create policy "user_invitations_delete" on public.user_invitations
  for delete
  using (public.current_role() = 'admin');

-- -------------------------------------------------------------
-- RLS: Popups (admin gestiona; publico ve activos)
-- -------------------------------------------------------------
alter table public.popups enable row level security;

drop policy if exists "popups_select" on public.popups;
create policy "popups_select" on public.popups
  for select
  using (activo = true or public.current_role() = 'admin');

drop policy if exists "popups_insert" on public.popups;
create policy "popups_insert" on public.popups
  for insert
  with check (public.current_role() = 'admin');

drop policy if exists "popups_update" on public.popups;
create policy "popups_update" on public.popups
  for update
  using (public.current_role() = 'admin');

drop policy if exists "popups_delete" on public.popups;
create policy "popups_delete" on public.popups
  for delete
  using (public.current_role() = 'admin');

-- -------------------------------------------------------------
-- RLS: Cupones (admin gestiona)
-- -------------------------------------------------------------
alter table public.cupones enable row level security;

drop policy if exists "cupones_select" on public.cupones;
create policy "cupones_select" on public.cupones
  for select
  using (activo = true or public.current_role() = 'admin');

drop policy if exists "cupones_insert" on public.cupones;
create policy "cupones_insert" on public.cupones
  for insert
  with check (public.current_role() = 'admin');

drop policy if exists "cupones_update" on public.cupones;
create policy "cupones_update" on public.cupones
  for update
  using (public.current_role() = 'admin');

drop policy if exists "cupones_delete" on public.cupones;
create policy "cupones_delete" on public.cupones
  for delete
  using (public.current_role() = 'admin');

-- -------------------------------------------------------------
-- Trigger: actualizar updated_at en cursos
-- -------------------------------------------------------------
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists cursos_updated_at on public.cursos;
create trigger cursos_updated_at
  before update on public.cursos
  for each row execute function public.touch_updated_at();

drop trigger if exists profiles_updated_at on public.profiles;
create trigger profiles_updated_at
  before update on public.profiles
  for each row execute function public.touch_updated_at();

-- =============================================================
-- DATOS SEMILLA (idempotente)
-- =============================================================

insert into public.categorias (nombre, descripcion, estado)
select * from (values
  ('Excel', 'Cursos de hojas de calculo, reportes y automatizacion.', 'Activa'),
  ('Programacion', 'Cursos de desarrollo web, frontend y logica de software.', 'Activa'),
  ('Diseno', 'Cursos de UI, branding y productos digitales.', 'Activa'),
  ('Productividad', 'Cursos para organizar mejor el trabajo y los procesos.', 'Activa')
) as v(nombre, descripcion, estado)
on conflict (nombre) do update set
  descripcion = excluded.descripcion,
  estado = excluded.estado;

insert into public.cursos (titulo, categoria, nivel, instructor, precio, old_precio, imagen, descripcion, estado, rating, reviews)
select * from (values
  ('Excel Avanzado', 'Excel', 'Avanzado', 'Nora Excel', 25, 26, 'excel', 'Domina formulas, tablas dinamicas y automatizacion con VBA.', 'Publicado', 5, 3),
  ('Programacion Web desde Cero', 'Programacion', 'Intermedio', 'Carlos Herrera', 35, 36, 'code', 'Aprende HTML, CSS y JavaScript construyendo proyectos reales.', 'Publicado', 5, 3),
  ('Diseno de Interfaces Digitales', 'Diseno', 'Principiante', 'SkillAcademy', 14, 19, 'laptop', 'Principios de UX/UI para crear interfaces modernas.', 'Publicado', 5, 3),
  ('Habilidades Tecnologicas Profesionales', 'Programacion', 'Avanzado', 'Camila Academy', 19, 76, 'screen', 'Ruta completa de habilidades digitales para el trabajo.', 'Publicado', 5, 4),
  ('Desarrollo Frontend Practico', 'Programacion', 'Intermedio', 'Renato Pena', 76, null, 'banner', 'Proyectos practicos de frontend con frameworks modernos.', 'Borrador', 5, 3),
  ('Excel para Reportes Ejecutivos', 'Excel', 'Avanzado', 'Nestor Ramos', 38, 76, 'design', 'Elabora reportes ejecutivos con tecnicas avanzadas de Excel.', 'Publicado', 5, 4),
  ('Componentes Visuales Modernos', 'Diseno', 'Principiante', 'SkillAcademy', 36, null, 'purple', 'Construye componentes visuales reutilizables y accesibles.', 'Borrador', 5, 4),
  ('Productividad con Herramientas Digitales', 'Productividad', 'Intermedio', 'Gerardo Gomez', 18, null, 'laptop', 'Organiza tu trabajo con herramientas de productividad.', 'Publicado', 5, 3)
) as v(titulo, categoria, nivel, instructor, precio, old_precio, imagen, descripcion, estado, rating, reviews)
where not exists (select 1 from public.cursos);

insert into public.tareas (titulo, curso_id, curso, fecha, prioridad, estado, progreso, dispositivo)
select * from (values
  ('Completar modulo de formulas', null::bigint, 'Excel Avanzado', '2026-06-22'::date, 'Alta', 'Pendiente', 65, 'Web y smartwatch'),
  ('Revisar leccion de presentaciones', null::bigint, 'PowerPoint Profesional', '2026-06-24'::date, 'Media', 'En progreso', 40, 'Web'),
  ('Responder evaluacion de diseno', null::bigint, 'Diseno Grafico Basico', '2026-06-26'::date, 'Media', 'Pendiente', 20, 'Web y celular'),
  ('Publicar banner de promocion', null::bigint, 'Administracion SkillAcademy', '2026-06-28'::date, 'Baja', 'Completada', 100, 'Smart TV')
) as v(titulo, curso_id, curso, fecha, prioridad, estado, progreso, dispositivo)
where not exists (select 1 from public.tareas);

update public.tareas t
set curso_id = c.id
from public.cursos c
where t.curso_id is null
  and lower(t.curso) = lower(c.titulo);

insert into public.banners (titulo, subtitulo, descripcion, imagen, enlace, estado)
select * from (values
  ('Promocion de Excel Avanzado', 'Promocion activa', 'Accede al curso Excel Avanzado con precio especial y practica tablas dinamicas, formulas y dashboards ejecutivos.', 'green', '/catalogo', 'Activo'),
  ('Descuento en cursos tecnologicos', 'Descuento tecnologico', 'Promocion para cursos de programacion y automatizacion disponibles en el catalogo.', 'purple', '/catalogo', 'Activo'),
  ('Nuevas habilidades digitales', 'Banner informativo', 'Conoce rutas para mejorar tus habilidades digitales con profesores activos de SkillAcademy.', 'dark', '/catalogo', 'Inactivo'),
  ('Ruta de aprendizaje profesional', 'Cursos destacados', 'Explora cursos recomendados para construir una ruta de aprendizaje profesional.', 'photo', '/catalogo', 'Activo')
) as v(titulo, subtitulo, descripcion, imagen, enlace, estado)
where not exists (select 1 from public.banners);

insert into public.popups (titulo, mensaje, imagen, enlace, activo)
select * from (values
  ('Descuento por tiempo limitado', 'Aprovecha 20% en tus cursos favoritos con el codigo SKILL20.', 'green', '/promociones', true)
) as v(titulo, mensaje, imagen, enlace, activo)
where not exists (select 1 from public.popups);

insert into public.cupones (codigo, descuento, activo)
select * from (values
  ('SKILL20', 20, true),
  ('BIENVENIDA', 10, true)
) as v(codigo, descuento, activo)
where not exists (select 1 from public.cupones);

insert into public.user_invitations (email, full_name, role_id, status)
select * from (values
  ('profesor.demo@skillacademy.local', 'Profesor Demo', 'teacher', 'Pendiente'),
  ('admin.demo@skillacademy.local', 'Admin Demo', 'admin', 'Pendiente')
) as v(email, full_name, role_id, status)
where not exists (select 1 from public.user_invitations);
