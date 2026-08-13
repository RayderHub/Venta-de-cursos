import { Component, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AdminShell } from '../../shared/components/admin-shell/admin-shell';
import { CourseForm } from '../../shared/components/course-form/course-form';
import { CursosService } from '../../core/services/cursos.service';
import { CategoriasService } from '../../core/services/categorias.service';
import { Profile } from '../../core/services/auth.service';
import { UsuariosService } from '../../core/services/usuarios.service';
import { CursoDb, CursoInput } from '../../core/models/curso.model';
import { courseImagePath } from '../../core/data/academy-data';

@Component({
  selector: 'app-admin-courses-page',
  imports: [AdminShell, CourseForm],
  templateUrl: './admin-courses.html',
  styleUrl: './admin-courses.css'
})
export class AdminCoursesPage {
  private readonly cursos = inject(CursosService);
  private readonly categoriasService = inject(CategoriasService);
  private readonly usuarios = inject(UsuariosService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly courses = signal<CursoDb[]>([]);
  readonly profesores = signal<Profile[]>([]);
  readonly categorias = signal<string[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly busqueda = signal('');
  readonly mostrarForm = signal(false);
  readonly editando = signal<CursoDb | null>(null);
  readonly cursoEliminando = signal<CursoDb | null>(null);
  readonly eliminando = signal(false);
  readonly creando = signal(false);

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    } else {
      this.cargando.set(false);
    }
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    const [courses, categorias, usuarios] = await Promise.all([
      this.cursos.listTodos(),
      this.categoriasService.nombresActivas(),
      this.usuarios.list()
    ]);

    this.courses.set(courses);
    this.categorias.set(categorias);
    this.profesores.set(usuarios.filter((usuario) => usuario.role_id === 'teacher' && usuario.is_active));
    this.cargando.set(false);
  }

  filtered(): CursoDb[] {
    const query = this.busqueda().toLowerCase();
    if (!query) return this.courses();
    return this.courses().filter(
      (curso) =>
        curso.titulo.toLowerCase().includes(query) ||
        curso.categoria.toLowerCase().includes(query) ||
        curso.instructor.toLowerCase().includes(query)
    );
  }

  imageSrc(imagen: string): string {
    return courseImagePath(imagen);
  }

  fmt(precio: number): string {
    return Number(precio).toFixed(2);
  }

  ratingStars(rating: number): string {
    const rounded = Math.round(Number(rating) || 0);
    return '\u2605'.repeat(rounded).padEnd(5, '\u2606');
  }

  abrirNuevo(): void {
    this.editando.set(null);
    this.mostrarForm.set(true);
  }

  abrirEditar(curso: CursoDb): void {
    this.editando.set(curso);
    this.mostrarForm.set(true);
  }

  async guardar(input: CursoInput): Promise<void> {
    if (this.creando()) return;

    this.creando.set(true);
    const editando = this.editando();
    const result = editando
      ? await this.cursos.actualizar(editando.id, input)
      : await this.cursos.crear(input);

    this.creando.set(false);

    if (result.error) {
      this.error.set(result.error.message);
    } else {
      this.error.set('');
      this.mostrarForm.set(false);
      await this.cargar();
    }
  }

  pedirEliminar(curso: CursoDb): void {
    this.cursoEliminando.set(curso);
  }

  cancelarEliminar(): void {
    if (!this.eliminando()) {
      this.cursoEliminando.set(null);
    }
  }

  async confirmarEliminar(): Promise<void> {
    const curso = this.cursoEliminando();
    if (!curso) return;

    this.eliminando.set(true);
    const { error } = await this.cursos.eliminar(curso.id);
    this.eliminando.set(false);

    if (error) {
      this.error.set(error.message);
    } else {
      this.courses.set(this.courses().filter((item) => item.id !== curso.id));
      this.cursoEliminando.set(null);
    }
  }
}
