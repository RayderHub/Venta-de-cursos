import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-perfil',
  imports: [FormsModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css'
})
export class PerfilPage {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly nombre = signal('');
  readonly bio = signal('');
  readonly avatarUrl = signal('');
  readonly guardando = signal(false);
  readonly message = signal('');
  readonly error = signal('');

  readonly isTeacher = computed(() => this.auth.isTeacher());
  readonly roleLabel = computed(() => {
    const role = this.auth.userRole();
    if (role === 'admin') return 'Administrador';
    if (role === 'teacher') return 'Profesor';
    return 'Estudiante';
  });
  readonly email = computed(() => this.auth.user()?.email ?? '');

  constructor() {
    effect(() => {
      const profile = this.auth.profile();
      if (!profile) return;
      this.nombre.set(profile.full_name ?? '');
      this.bio.set(profile.bio ?? '');
      this.avatarUrl.set(profile.avatar_url ?? '');
    });
  }

  avatarFallback(): string {
    const initial = (this.nombre() || this.email() || 'U').charAt(0).toUpperCase();
    const svg =
      `<svg xmlns="http://www.w3.org/2000/svg" width="180" height="180">` +
      `<rect width="180" height="180" rx="90" fill="#101923"/>` +
      `<text x="90" y="118" font-size="72" fill="#50d67a" text-anchor="middle" font-family="Arial" font-weight="bold">${initial}</text>` +
      `</svg>`;
    return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
  }

  async onFile(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const dataUrl = await this.readAsDataUrl(file);
    const client = this.auth.getSupabase();
    const user = this.auth.user();
    let url = dataUrl;

    if (client && user) {
      const ext = (file.name.split('.').pop() || 'jpg').replace(/[^a-z0-9]/gi, '');
      const path = `avatars/${user.id}.${ext}`;
      const { error } = await client.storage.from('avatars').upload(path, file, { upsert: true });
      if (!error) {
        const { data: publicData } = client.storage.from('avatars').getPublicUrl(path);
        url = publicData.publicUrl;
      }
    }

    this.avatarUrl.set(url);
    input.value = '';
  }

  private readAsDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result));
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  async guardar(): Promise<void> {
    this.guardando.set(true);
    this.message.set('');
    this.error.set('');

    const updates: Record<string, string | null> = {
      full_name: this.nombre().trim() || null,
      avatar_url: this.avatarUrl().trim() || null
    };
    if (this.isTeacher()) {
      updates['bio'] = this.bio().trim() || null;
    }

    const { error } = await this.auth.updateProfile(updates as Partial<import('../../core/services/auth.service').Profile>);
    this.guardando.set(false);

    if (error) {
      this.error.set(error.message);
    } else {
      this.message.set('Perfil actualizado correctamente.');
      await this.router.navigateByUrl(this.auth.getHomeRouteForRole());
    }
  }
}
