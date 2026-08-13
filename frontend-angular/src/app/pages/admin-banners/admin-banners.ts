import { Component, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminShell } from '../../shared/components/admin-shell/admin-shell';
import { BannerDb, BannersService, BannerInput } from '../../core/services/banners.service';

@Component({
  selector: 'app-admin-banners-page',
  imports: [AdminShell, FormsModule],
  templateUrl: './admin-banners.html',
  styleUrl: './admin-banners.css'
})
export class AdminBannersPage {
  private readonly bannersService = inject(BannersService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly banners = signal<BannerDb[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly editando = signal<BannerDb | null>(null);

  form: BannerInput = { titulo: '', subtitulo: '', descripcion: '', imagen: 'green', enlace: '', estado: 'Activo' };

  readonly imagenes = ['green', 'purple', 'dark', 'photo'];

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    } else {
      this.cargando.set(false);
    }
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    this.banners.set(await this.bannersService.list());
    this.cargando.set(false);
  }

  imagenUrl(imagen: string): string {
    const paths: Record<string, string> = {
      green: '/assets/images/banner-green.svg',
      purple: '/assets/images/banner-purple.svg',
      dark: '/assets/images/course-dashboard.svg',
      photo: '/assets/images/promo-instructor.svg'
    };
    return paths[imagen] ?? paths['green'];
  }

  editar(banner: BannerDb): void {
    this.editando.set(banner);
    this.form = {
      titulo: banner.titulo,
      subtitulo: banner.subtitulo,
      descripcion: banner.descripcion ?? '',
      imagen: banner.imagen,
      enlace: banner.enlace,
      estado: banner.estado
    };
  }

  cancelarEdicion(): void {
    this.editando.set(null);
    this.form = { titulo: '', subtitulo: '', descripcion: '', imagen: 'green', enlace: '', estado: 'Activo' };
  }

  async guardar(): Promise<void> {
    if (!this.form.titulo.trim()) {
      this.error.set('El titulo del banner es obligatorio.');
      return;
    }

    const editando = this.editando();
    const result = editando
      ? await this.bannersService.actualizar(editando.id, this.form)
      : await this.bannersService.crear(this.form);

    if (result.error) {
      this.error.set(result.error.message);
    } else {
      this.error.set('');
      this.cancelarEdicion();
      await this.cargar();
    }
  }

  async cambiarEstado(banner: BannerDb): Promise<void> {
    const nuevo = banner.estado === 'Activo' ? 'Inactivo' : 'Activo';
    const { error } = await this.bannersService.actualizar(banner.id, { estado: nuevo });
    if (!error) banner.estado = nuevo;
  }

  async eliminar(bannerId: number): Promise<void> {
    const confirmado = confirm('Eliminar este banner?');
    if (!confirmado) return;

    const { error } = await this.bannersService.eliminar(bannerId);
    if (error) {
      this.error.set(error.message);
    } else {
      this.banners.set(this.banners().filter((banner) => banner.id !== bannerId));
    }
  }
}
