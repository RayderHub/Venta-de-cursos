import { Component, computed, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Course } from '../../core/data/academy-data';
import { CursosService } from '../../core/services/cursos.service';
import { CategoriasService } from '../../core/services/categorias.service';
import { PublicHeader } from '../../shared/components/public-header/public-header';

@Component({
  selector: 'app-categories',
  imports: [PublicHeader, RouterLink],
  templateUrl: './categories.html',
  styleUrl: './categories.css'
})
export class Categories {
  private readonly cursos = inject(CursosService);
  private readonly categoriasService = inject(CategoriasService);
  private readonly platformId = inject(PLATFORM_ID);
  readonly courses = signal<Course[]>([]);
  readonly categoriasCrud = signal<string[]>([]);
  readonly cargando = signal(true);
  readonly categories = computed(() => {
    const set = new Set(this.categoriasCrud());
    this.courses().forEach((course) => set.add(course.category));
    return Array.from(set);
  });

  count(category: string): number {
    return this.courses().filter((course) => course.category === category).length;
  }

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    }
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    const [data, categorias] = await Promise.all([
      this.cursos.listPublicos(),
      this.categoriasService.nombresActivas()
    ]);
    if (data.length > 0) this.courses.set(data);
    this.categoriasCrud.set(categorias);
    this.cargando.set(false);
  }
}
