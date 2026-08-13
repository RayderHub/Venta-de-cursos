import { Component, computed, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Course } from '../../core/data/academy-data';
import { CursosService } from '../../core/services/cursos.service';
import { CategoriasService } from '../../core/services/categorias.service';
import { ElasticsearchService } from '../../core/services/elasticsearch.service';
import { CourseCard } from '../../shared/components/course-card/course-card';
import { PublicHeader } from '../../shared/components/public-header/public-header';

@Component({
  selector: 'app-catalog',
  imports: [PublicHeader, CourseCard],
  templateUrl: './catalog.html',
  styleUrl: './catalog.css'
})
export class Catalog {
  private readonly cursos = inject(CursosService);
  private readonly categoriasService = inject(CategoriasService);
  private readonly buscador = inject(ElasticsearchService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly route = inject(ActivatedRoute);

  readonly selectedCategory = signal('Todas');
  readonly selectedLevels = signal<string[]>([]);
  readonly search = signal('');
  readonly maxPrice = signal(0);
  readonly sortMode = signal('recommended');
  readonly courses = signal<Course[]>([]);
  readonly categoriasCrud = signal<string[]>([]);
  readonly cargando = signal(true);
  readonly buscando = signal(false);
  readonly buscadorCaido = signal(false);
  readonly resultadosBuscador = signal<Course[] | null>(null);

  private debounceTimer: ReturnType<typeof setTimeout> | null = null;

  readonly categories = computed(() => {
    const set = new Set(this.categoriasCrud());
    this.courses().forEach((course) => set.add(course.category));
    return Array.from(set);
  });

  readonly levels = computed(() => {
    const set = new Set(this.courses().map((course) => course.level));
    return Array.from(set);
  });

  readonly highestPrice = computed(() => {
    return Math.ceil(Math.max(...this.fuenteCursos().map((course) => course.price), 0));
  });

  readonly activePriceLimit = computed(() => this.maxPrice() || this.highestPrice());

  readonly hasActiveFilters = computed(() => {
    return this.selectedCategory() !== 'Todas'
      || this.selectedLevels().length > 0
      || !!this.search().trim()
      || this.activePriceLimit() !== this.highestPrice()
      || this.sortMode() !== 'recommended';
  });

  private readonly fuenteCursos = computed(() => this.resultadosBuscador() ?? this.courses());

  readonly courseList = computed(() => {
    const category = this.selectedCategory();
    const levels = this.selectedLevels();
    const query = this.search().trim().toLowerCase();
    const priceLimit = this.activePriceLimit();

    const filtered = this.fuenteCursos().filter((course) => {
      const matchesCategory = category === 'Todas' || course.category === category;
      const matchesLevel = levels.length === 0 || levels.includes(course.level);
      const matchesPrice = course.price <= priceLimit;
      const matchesSearch = !query
        || course.title.toLowerCase().includes(query)
        || course.instructor.toLowerCase().includes(query)
        || course.category.toLowerCase().includes(query);

      return matchesCategory && matchesLevel && matchesPrice && matchesSearch;
    });

    return [...filtered].sort((a, b) => {
      if (this.sortMode() === 'price-low') {
        return a.price - b.price;
      }

      if (this.sortMode() === 'price-high') {
        return b.price - a.price;
      }

      if (this.sortMode() === 'rating') {
        return b.rating - a.rating;
      }

      return a.id - b.id;
    });
  });

  constructor() {
    this.route.queryParamMap.subscribe((params) => {
      this.selectedCategory.set(params.get('categoria') ?? 'Todas');
      const q = params.get('q') ?? '';
      this.sortMode.set(params.get('orden') ?? 'recommended');
      if (isPlatformBrowser(this.platformId) && q.trim()) {
        this.onSearchChange(q);
      } else {
        this.search.set(q);
      }
    });

    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    } else {
      this.cargando.set(false);
    }
  }

  private async cargar(): Promise<void> {
    const [data, categorias] = await Promise.all([
      this.cursos.listPublicos(),
      this.categoriasService.nombresActivas()
    ]);
    if (data.length > 0) this.courses.set(data);
    this.categoriasCrud.set(categorias);
    this.maxPrice.set(this.highestPrice());
    this.cargando.set(false);
  }

  onSearchChange(value: string): void {
    this.search.set(value);

    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }

    if (!value.trim()) {
      this.resultadosBuscador.set(null);
      this.buscando.set(false);
      this.buscadorCaido.set(false);
      return;
    }

    this.buscando.set(true);
    this.debounceTimer = setTimeout(() => {
      void this.buscarEnElasticsearch(value.trim());
    }, 400);
  }

  private async buscarEnElasticsearch(query: string): Promise<void> {
    try {
      const { cursos } = await this.buscador.buscar(query);
      this.resultadosBuscador.set(cursos);
      this.buscadorCaido.set(false);
    } catch {
      this.buscadorCaido.set(true);
    } finally {
      this.buscando.set(false);
    }
  }

  toggleLevel(level: string): void {
    const selected = this.selectedLevels();
    this.selectedLevels.set(selected.includes(level)
      ? selected.filter((item) => item !== level)
      : [...selected, level]);
  }

  clearFilters(): void {
    this.selectedCategory.set('Todas');
    this.selectedLevels.set([]);
    this.search.set('');
    this.maxPrice.set(this.highestPrice());
    this.sortMode.set('recommended');
    this.resultadosBuscador.set(null);
    this.buscando.set(false);
    this.buscadorCaido.set(false);
  }
}
