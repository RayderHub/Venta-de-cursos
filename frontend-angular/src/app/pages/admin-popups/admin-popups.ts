import { Component, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminShell } from '../../shared/components/admin-shell/admin-shell';
import { PopupsService, PopupDb, PopupInput, CuponDb, CuponInput } from '../../core/services/popups.service';

@Component({
  selector: 'app-admin-popups-page',
  imports: [AdminShell, FormsModule],
  templateUrl: './admin-popups.html',
  styleUrl: './admin-popups.css'
})
export class AdminPopupsPage {
  private readonly popupsService = inject(PopupsService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly popups = signal<PopupDb[]>([]);
  readonly cupones = signal<CuponDb[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');

  readonly editandoPopup = signal<PopupDb | null>(null);
  readonly editandoCupon = signal<CuponDb | null>(null);

  popupForm: PopupInput = { titulo: '', mensaje: '', imagen: 'green', enlace: '', activo: true };
  cuponForm: CuponInput = { codigo: '', descuento: 10, activo: true };

  readonly imagenes = ['green', 'purple', 'dark', 'photo'];

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    } else {
      this.cargando.set(false);
    }
  }

  async cargar(): Promise<void> {
    this.cargando.set(true);
    const [popups, cupones] = await Promise.all([this.popupsService.listPopups(), this.popupsService.listCupones()]);
    this.popups.set(popups);
    this.cupones.set(cupones);
    this.cargando.set(false);
  }

  abrirNuevoPopup(): void {
    this.editandoPopup.set(null);
    this.popupForm = { titulo: '', mensaje: '', imagen: 'green', enlace: '', activo: true };
  }

  abrirEditarPopup(popup: PopupDb): void {
    this.editandoPopup.set(popup);
    this.popupForm = {
      titulo: popup.titulo,
      mensaje: popup.mensaje ?? '',
      imagen: popup.imagen,
      enlace: popup.enlace ?? '',
      activo: popup.activo
    };
  }

  async guardarPopup(): Promise<void> {
    if (!this.popupForm.titulo.trim()) {
      this.error.set('El titulo del popup es obligatorio.');
      return;
    }

    const editando = this.editandoPopup();
    const result = editando
      ? await this.popupsService.actualizarPopup(editando.id, this.popupForm)
      : await this.popupsService.crearPopup(this.popupForm);

    if (result.error) {
      this.error.set(result.error.message);
    } else {
      this.error.set('');
      this.abrirNuevoPopup();
      await this.cargar();
    }
  }

  async togglePopup(popup: PopupDb): Promise<void> {
    const { error } = await this.popupsService.actualizarPopup(popup.id, { activo: !popup.activo });
    if (!error) popup.activo = !popup.activo;
  }

  async eliminarPopup(popupId: number): Promise<void> {
    const confirmado = confirm('Eliminar este popup?');
    if (!confirmado) return;
    const { error } = await this.popupsService.eliminarPopup(popupId);
    if (error) {
      this.error.set(error.message);
    } else {
      this.popups.set(this.popups().filter((popup) => popup.id !== popupId));
    }
  }

  abrirNuevoCupon(): void {
    this.editandoCupon.set(null);
    this.cuponForm = { codigo: '', descuento: 10, activo: true };
  }

  abrirEditarCupon(cupon: CuponDb): void {
    this.editandoCupon.set(cupon);
    this.cuponForm = { codigo: cupon.codigo, descuento: Number(cupon.descuento), activo: cupon.activo };
  }

  async guardarCupon(): Promise<void> {
    const codigo = this.cuponForm.codigo.trim().toUpperCase();
    if (!codigo) {
      this.error.set('El codigo del cupon es obligatorio.');
      return;
    }

    const editando = this.editandoCupon();
    const result = editando
      ? await this.popupsService.actualizarCupon(editando.id, { ...this.cuponForm, codigo })
      : await this.popupsService.crearCupon({ ...this.cuponForm, codigo });

    if (result.error) {
      this.error.set(result.error.message);
    } else {
      this.error.set('');
      this.abrirNuevoCupon();
      await this.cargar();
    }
  }

  async toggleCupon(cupon: CuponDb): Promise<void> {
    const { error } = await this.popupsService.actualizarCupon(cupon.id, { activo: !cupon.activo });
    if (!error) cupon.activo = !cupon.activo;
  }

  async eliminarCupon(cuponId: number): Promise<void> {
    const confirmado = confirm('Eliminar este cupon?');
    if (!confirmado) return;
    const { error } = await this.popupsService.eliminarCupon(cuponId);
    if (error) {
      this.error.set(error.message);
    } else {
      this.cupones.set(this.cupones().filter((cupon) => cupon.id !== cuponId));
    }
  }
}
