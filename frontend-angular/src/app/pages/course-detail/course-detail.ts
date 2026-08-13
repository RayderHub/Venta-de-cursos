import { Component, computed, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Course, courseImagePath } from '../../core/data/academy-data';
import { CursosService } from '../../core/services/cursos.service';
import { InscripcionesService } from '../../core/services/inscripciones.service';
import { CarritoService } from '../../core/services/carrito.service';
import { AuthService } from '../../core/services/auth.service';
import { LeccionesService } from '../../core/services/lecciones.service';
import { PublicHeader } from '../../shared/components/public-header/public-header';

type DetailTab = 'descripcion' | 'lecciones' | 'requisitos';

@Component({
  selector: 'app-course-detail',
  imports: [PublicHeader, RouterLink],
  templateUrl: './course-detail.html',
  styleUrl: './course-detail.css'
})
export class CourseDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly cursos = inject(CursosService);
  private readonly inscripciones = inject(InscripcionesService);
  private readonly leccionesService = inject(LeccionesService);
  readonly carrito = inject(CarritoService);
  readonly auth = inject(AuthService);

  private readonly id = Number(this.route.snapshot.paramMap.get('id')) || 1;
  readonly course = signal<Course | null>(null);
  readonly comprando = signal(false);
  readonly message = signal('');
  readonly activeTab = signal<DetailTab>('descripcion');
  readonly yaLoTiene = signal(false);
  readonly reviews = signal<{ usuario_id: string; rating: number; comment: string | null; usuario_nombre: string }[]>([]);
  readonly lecciones = signal<{ id: number; titulo: string; descripcion: string | null }[]>([]);
  readonly promedioRating = computed(() => {
    const reviews = this.reviews();
    if (reviews.length === 0) return 0;
    return reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length;
  });
  readonly isInCart = computed(() => {
    const course = this.course();
    return course ? this.carrito.has(course.id) : false;
  });

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    }
  }

  private async cargar(): Promise<void> {
    const course = await this.cursos.get(this.id);
    if (course) {
      this.course.set(course);
      const enrolled = await this.inscripciones.listMisCursos();
      this.yaLoTiene.set(enrolled.some((c) => c.curso.id === this.id));
      await this.cargarReviews();
      await this.cargarLecciones();
    }
  }

  private async cargarLecciones(): Promise<void> {
    const lecciones = await this.leccionesService.listByCurso(this.id);
    this.lecciones.set(lecciones.map((l) => ({ id: l.id, titulo: l.titulo, descripcion: l.descripcion })));
  }

  private async cargarReviews(): Promise<void> {
    const client = this.auth.getSupabase();
    if (!client) return;
    const { data } = await client
      .from('curso_reviews')
      .select('usuario_id, rating, comment, created_at')
      .eq('curso_id', this.id)
      .order('created_at', { ascending: false })
      .limit(10);
    if (data && data.length > 0) {
      const userIds = [...new Set(data.map((r: any) => r.usuario_id))];
      const { data: perfiles } = await client
        .from('profiles')
        .select('id, full_name')
        .in('id', userIds);
      const perfilMap = new Map((perfiles || []).map((p: any) => [p.id, p.full_name]));
      this.reviews.set(
        data.map((r: any) => ({
          usuario_id: r.usuario_id,
          rating: r.rating,
          comment: r.comment,
          usuario_nombre: perfilMap.get(r.usuario_id) || 'Usuario'
        }))
      );
    } else {
      this.reviews.set([]);
    }
  }

  imageSrc(): string {
    return courseImagePath(this.course()?.image ?? 'code');
  }

  async comprar(): Promise<void> {
    const course = this.course();
    if (!course) return;

    if (!this.auth.isAuthenticated()) {
      await this.router.navigate(['/login'], { queryParams: { returnUrl: `/curso/${this.id}` } });
      return;
    }

    this.comprando.set(true);
    this.message.set('');
    const { error } = await this.inscripciones.inscribir(this.id);
    this.comprando.set(false);

    if (error) {
      this.message.set(error.message);
    } else {
      await this.router.navigate(['/usuario/dashboard']);
    }
  }

  agregarAlCarrito(): void {
    const course = this.course();
    if (!course) return;

    if (this.isInCart()) {
      this.message.set('Este curso ya esta en tu carrito.');
      return;
    }
    this.carrito.add(course);
    this.message.set('Curso agregado al carrito.');
  }

  tab(tab: DetailTab): void {
    this.activeTab.set(tab);
  }
}
