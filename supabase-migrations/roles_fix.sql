-- Fix de roles: handle_new_user debe leer role_id del raw_user_meta_data
-- y sincronizar los profiles existentes desde auth.users.

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

-- Sincronizar roles existentes desde el metadata de auth.users.
-- Solo promueve (nunca degrada): si el metadata indica teacher/admin y el
-- profile quedó en student, lo corrige. Un rol asignado manualmente por el
-- admin (metadata student pero profile teacher) se respeta.
-- Se ignora el trigger profiles_protect_admin_fields porque este script
-- se ejecuta como superusuario (auth.uid() es null y el trigger retorna new).
update public.profiles p
set role_id = u.raw_user_meta_data ->> 'role_id',
    updated_at = now()
from auth.users u
where u.id = p.id
  and u.raw_user_meta_data ->> 'role_id' in ('teacher', 'admin')
  and p.role_id = 'student';

-- Verificacion rapida
select p.email, p.role_id, u.raw_user_meta_data ->> 'role_id' as metadata_role
from public.profiles p
join auth.users u on u.id = p.id
order by p.email;
