import { Component, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminShell } from '../../shared/components/admin-shell/admin-shell';
import { CategoriaDb, CategoriaInput, CategoriasService } from '../../core/services/categorias.service';

@Component({
  selector: 'app-admin-categories-page',
  imports: [AdminShell, FormsModule],
  templateUrl: './admin-categories.html',
  styleUrl: './admin-categories.css'
})
export class AdminCategoriesPage {
  private readonly categoriasService = inject(CategoriasService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly categorias = signal<CategoriaDb[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly editando = signal<CategoriaDb | null>(null);
  readonly eliminando = signal<CategoriaDb | null>(null);

  form: CategoriaInput = { nombre: '', descripcion: '', estado: 'Activa' };

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    } else {
      this.cargando.set(false);
    }
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    this.categorias.set(await this.categoriasService.list());
    this.cargando.set(false);
  }

  nueva(): void {
    this.editando.set(null);
    this.form = { nombre: '', descripcion: '', estado: 'Activa' };
  }

  editar(categoria: CategoriaDb): void {
    this.editando.set(categoria);
    this.form = {
      nombre: categoria.nombre,
      descripcion: categoria.descripcion ?? '',
      estado: categoria.estado
    };
  }

  async guardar(): Promise<void> {
    if (!this.form.nombre.trim()) {
      this.error.set('El nombre de la categoria es obligatorio.');
      return;
    }

    const payload = {
      nombre: this.form.nombre.trim(),
      descripcion: this.form.descripcion.trim(),
      estado: this.form.estado
    };
    const editando = this.editando();
    const result = editando
      ? await this.categoriasService.actualizar(editando.id, payload)
      : await this.categoriasService.crear(payload);

    if (result.error) {
      this.error.set(result.error.message);
      return;
    }

    this.error.set('');
    this.nueva();
    await this.cargar();
  }

  pedirEliminar(categoria: CategoriaDb): void {
    this.eliminando.set(categoria);
  }

  cancelarEliminar(): void {
    this.eliminando.set(null);
  }

  async confirmarEliminar(): Promise<void> {
    const categoria = this.eliminando();
    if (!categoria) return;

    const { error } = await this.categoriasService.eliminar(categoria.id);
    if (error) {
      this.error.set(error.message);
      return;
    }

    this.categorias.set(this.categorias().filter((item) => item.id !== categoria.id));
    this.eliminando.set(null);
  }
}
