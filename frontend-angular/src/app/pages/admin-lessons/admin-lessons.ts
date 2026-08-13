import { Component, inject, signal } from '@angular/core';
import { AdminShell } from '../../shared/components/admin-shell/admin-shell';
import { CursosService } from '../../core/services/cursos.service';
import { LessonBuilder } from '../../shared/components/lesson-builder/lesson-builder';

@Component({
  selector: 'app-admin-lessons',
  standalone: true,
  imports: [AdminShell, LessonBuilder],
  template: `
    <app-admin-shell title="Lecciones y Contenido">
      <section class="lessons-admin-section">
        <app-lesson-builder [cursos]="cursos()" />
      </section>
    </app-admin-shell>
  `,
  styles: [`
    .lessons-admin-section { margin-top: 16px; }
  `]
})
export class AdminLessons {
  private cursosService = inject(CursosService);
  readonly cursos = signal<{ id: number; titulo: string; categoria: string; nivel: string }[]>([]);

  constructor() {
    void this.cargarCursos();
  }

  async cargarCursos(): Promise<void> {
    const data = await this.cursosService.listTodos();
    this.cursos.set(data.map((c) => ({ id: c.id, titulo: c.titulo, categoria: c.categoria, nivel: c.nivel })));
  }
}
