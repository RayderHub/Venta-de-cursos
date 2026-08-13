import { Injectable, inject } from '@angular/core';
import { AuthService, Profile } from './auth.service';

export interface UsuarioAdmin extends Profile {
  email: string;
}

export interface UserInvitation {
  id: number;
  email: string;
  full_name: string | null;
  role_id: string;
  status: 'Pendiente' | 'Usada' | 'Cancelada';
  created_at: string;
  used_at: string | null;
}

export interface UserInvitationInput {
  email: string;
  full_name: string;
  role_id: string;
}

export const ROLES = [
  { id: 'student', label: 'Estudiante' },
  { id: 'teacher', label: 'Profesor' },
  { id: 'admin', label: 'Administrador' }
];

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private auth = inject(AuthService);

  private client() {
    return this.auth.getSupabase();
  }

  async list(): Promise<Profile[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client.from('profiles').select('*').order('created_at', { ascending: true });
    if (error || !data) return [];
    return data as Profile[];
  }

  async cambiarRol(userId: string, roleId: string): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('profiles').update({ role_id: roleId }).eq('id', userId);
    return { error: error ? new Error(error.message) : null };
  }

  async cambiarEstado(userId: string, isActive: boolean): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('profiles').update({ is_active: isActive }).eq('id', userId);
    return { error: error ? new Error(error.message) : null };
  }

  async actualizarPerfil(userId: string, updates: Partial<Pick<Profile, 'full_name' | 'bio' | 'role_id' | 'is_active'>>): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('profiles').update(updates).eq('id', userId);
    return { error: error ? new Error(error.message) : null };
  }

  async eliminarUsuario(userId: string): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.rpc('admin_delete_user', { target_user_id: userId });
    return { error: error ? new Error(error.message) : null };
  }

  async listInvitations(): Promise<UserInvitation[]> {
    const client = this.client();
    if (!client) return [];

    const { data, error } = await client
      .from('user_invitations')
      .select('*')
      .order('created_at', { ascending: false });

    if (error || !data) return [];
    return data as UserInvitation[];
  }

  async crearInvitacion(input: UserInvitationInput): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client.from('user_invitations').insert({
      email: input.email.trim().toLowerCase(),
      full_name: input.full_name.trim() || null,
      role_id: input.role_id,
      status: 'Pendiente',
      created_by: this.auth.user()?.id ?? null
    });
    return { error: error ? new Error(error.message) : null };
  }

  async cancelarInvitacion(invitationId: number): Promise<{ error: Error | null }> {
    const client = this.client();
    if (!client) return { error: new Error('Supabase no disponible') };

    const { error } = await client
      .from('user_invitations')
      .update({ status: 'Cancelada' })
      .eq('id', invitationId);
    return { error: error ? new Error(error.message) : null };
  }

  rolLabel(roleId: string): string {
    return ROLES.find((rol) => rol.id === roleId)?.label ?? roleId;
  }
}
