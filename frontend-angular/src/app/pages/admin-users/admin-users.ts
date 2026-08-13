import { Component, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminShell } from '../../shared/components/admin-shell/admin-shell';
import { AuthService, Profile } from '../../core/services/auth.service';
import { UsuariosService, ROLES } from '../../core/services/usuarios.service';

@Component({
  selector: 'app-admin-users-page',
  imports: [AdminShell, RouterLink, FormsModule],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.css'
})
export class AdminUsersPage {
  private readonly usuarios = inject(UsuariosService);
  private readonly auth = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly users = signal<Profile[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly message = signal('');
  readonly rolFilter = signal('');
  readonly busqueda = signal('');
  readonly panel = signal<'none' | 'invite' | 'edit'>('none');
  readonly editando = signal<Profile | null>(null);

  inviteForm = { email: '', full_name: '', role_id: 'student' };
  editForm = { full_name: '', bio: '', role_id: 'student', is_active: true };

  readonly roles = ROLES;

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    } else {
      this.cargando.set(false);
    }
  }

  async cargar(): Promise<void> {
    this.cargando.set(true);
    const users = await this.usuarios.list();
    this.users.set(users);
    this.cargando.set(false);
  }

  filtered(): Profile[] {
    const filter = this.rolFilter();
    const query = this.busqueda().trim().toLowerCase();
    return this.users().filter((user) => {
      const matchesRole = !filter || user.role_id === filter;
      const matchesQuery = !query
        || (user.full_name ?? '').toLowerCase().includes(query)
        || (user.email ?? '').toLowerCase().includes(query);

      return matchesRole && matchesQuery;
    });
  }

  esUsuarioActual(userId: string): boolean {
    return this.auth.user()?.id === userId;
  }

  async cambiarRol(userId: string, roleId: string): Promise<void> {
    if (this.esUsuarioActual(userId)) {
      this.error.set('No puedes cambiar tu propio rol desde el panel.');
      return;
    }
    const { error } = await this.usuarios.cambiarRol(userId, roleId);
    if (error) {
      this.error.set(error.message);
      return;
    }
    this.users.update((items) =>
      items.map((item) => (item.id === userId ? { ...item, role_id: roleId } : item))
    );
  }

  abrirInvitacion(): void {
    this.error.set('');
    this.message.set('');
    this.editando.set(null);
    this.inviteForm = { email: '', full_name: '', role_id: 'student' };
    this.panel.set('invite');
  }

  abrirEditar(user: Profile): void {
    this.error.set('');
    this.message.set('');
    this.editando.set(user);
    this.editForm = {
      full_name: user.full_name ?? '',
      bio: user.bio ?? '',
      role_id: user.role_id,
      is_active: user.is_active
    };
    this.panel.set('edit');
  }

  cerrarPanel(): void {
    this.panel.set('none');
    this.editando.set(null);
  }

  async crearInvitacion(): Promise<void> {
    this.error.set('');
    this.message.set('');
    if (!this.inviteForm.email.trim()) {
      this.error.set('El email es obligatorio.');
      return;
    }

    const { error } = await this.usuarios.crearInvitacion(this.inviteForm);
    if (error) {
      this.error.set(error.message);
      return;
    }

    this.message.set('Invitacion creada. Cuando el usuario se registre con ese email, recibira el rol asignado.');
    this.cerrarPanel();
    await this.cargar();
  }

  async guardarEdicion(): Promise<void> {
    const user = this.editando();
    if (!user) return;

    this.error.set('');
    this.message.set('');
    if (this.esUsuarioActual(user.id) && !this.editForm.is_active) {
      this.error.set('No puedes desactivar tu propia cuenta.');
      return;
    }
    if (this.esUsuarioActual(user.id) && this.editForm.role_id !== user.role_id) {
      this.error.set('No puedes cambiar tu propio rol desde el panel.');
      return;
    }
    const { error } = await this.usuarios.actualizarPerfil(user.id, {
      full_name: this.editForm.full_name.trim() || null,
      bio: this.editForm.bio.trim() || null,
      role_id: this.editForm.role_id,
      is_active: this.editForm.is_active
    });

    if (error) {
      this.error.set(error.message);
      return;
    }

    this.message.set('Usuario actualizado.');
    this.cerrarPanel();
    await this.cargar();
  }

  async eliminar(user: Profile): Promise<void> {
    if (this.esUsuarioActual(user.id)) {
      this.error.set('No puedes eliminar tu propia cuenta desde el panel.');
      return;
    }
    const confirmado = confirm(`Eliminar a ${this.nombre(user)}? Esta accion borra su cuenta de autenticacion y datos relacionados.`);
    if (!confirmado) return;

    const { error } = await this.usuarios.eliminarUsuario(user.id);
    if (error) {
      this.error.set(error.message);
      return;
    }

    this.message.set('Usuario eliminado.');
    await this.cargar();
  }

  async cambiarEstado(userId: string, activo: boolean): Promise<void> {
    if (this.esUsuarioActual(userId) && !activo) {
      this.error.set('No puedes desactivar tu propia cuenta.');
      return;
    }
    const { error } = await this.usuarios.cambiarEstado(userId, activo);
    if (error) {
      this.error.set(error.message);
      return;
    }
    this.users.update((items) =>
      items.map((item) => (item.id === userId ? { ...item, is_active: activo } : item))
    );
  }

  nombre(user: Profile): string {
    return user.full_name || user.email || 'Usuario';
  }
}
