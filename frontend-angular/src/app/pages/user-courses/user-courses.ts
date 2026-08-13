import { Component, computed, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Course, courseImagePath } from '../../core/data/academy-data';
import { AuthService } from '../../core/services/auth.service';
import { InscripcionesService } from '../../core/services/inscripciones.service';
import { LeccionesService } from '../../core/services/lecciones.service';
import { TareaDb, TareasService } from '../../core/services/tareas.service';

@Component({
  selector: 'app-user-courses',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './user-courses.html',
  styleUrl: './user-courses.css'
})
export class UserCourses {
  readonly auth = inject(AuthService);
  private readonly inscripciones = inject(InscripcionesService);
  private readonly leccionesService = inject(LeccionesService);
  private readonly tareasService = inject(TareasService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);

  readonly tab = signal<'cursos' | 'progreso'>('cursos');
  readonly enrolled = signal<{ curso: Course; progreso: number }[]>([]);
  readonly tareas = signal<TareaDb[]>([]);
  readonly cargando = signal(true);
  readonly profileMenuOpen = signal(false);
  readonly ratingMessage = signal('');
  readonly modalReembolso = signal(false);
  readonly cursoReembolso = signal<Course | null>(null);
  readonly motivoReembolso = signal('');
  readonly correoReembolso = signal('');
  readonly ratingModal = signal(false);
  readonly ratingCursoId = signal<number | null>(null);
  readonly ratingStars = signal(0);
  readonly ratingHoverCursoId = signal<number | null>(null);
  readonly ratingHoverStars = signal(0);
  readonly ratingComment = signal('');
  readonly userRatings = signal<Record<number, { stars: number; comment: string }>>({});

  readonly studentName = computed(() => this.auth.profile()?.full_name || this.auth.user()?.email || 'Estudiante');
  readonly avatarUrl = computed(() => this.auth.profile()?.avatar_url || '');
  readonly enrolledList = computed(() => this.enrolled());

  readonly stats = computed(() => {
    const cursos = this.enrolled();
    const promedio = cursos.length
      ? Math.round(cursos.reduce((total, item) => total + item.progreso, 0) / cursos.length)
      : 0;

    return {
      cursos: cursos.length,
      tareas: this.tareas().filter((tarea) => tarea.estado !== 'Completada').length,
      promedio
    };
  });

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.cargar();
    } else {
      this.cargando.set(false);
    }
  }

  private async cargar(): Promise<void> {
    const data = await this.inscripciones.listMisCursos();
    const tareas = await this.tareasService.listParaAlumno(data.map((item) => item.curso.id));
    this.tareas.set(tareas);
    const conProgreso = await Promise.all(
      data.map(async (item) => {
        const porContenido = await this.progresoContenidoCurso(item.curso.id, tareas);
        return { ...item, progreso: porContenido > 0 ? porContenido : item.progreso };
      })
    );
    this.enrolled.set(conProgreso);
    const ratings = await this.inscripciones.listMisCalificaciones();
    this.userRatings.set(ratings);
    this.cargando.set(false);
  }

  private async progresoContenidoCurso(cursoId: number, tareas: TareaDb[]): Promise<number> {
    const lecciones = await this.leccionesService.listByCurso(cursoId);
    const tareasCurso = tareas.filter((tarea) => tarea.curso_id === cursoId);
    const total = lecciones.length + tareasCurso.length;
    if (total === 0) return 0;

    const leccionesVistas = await this.leccionesService.leccionesVistasDeIds(lecciones.map((l) => l.id));
    const vistasSet = new Set(leccionesVistas);
    const leccionesCompletadas = lecciones.filter((l) => vistasSet.has(l.id)).length;

    const resultados = await this.tareasService.resultadosDeTareas(tareasCurso.map((t) => t.id));
    const tareasAprobadas = resultados.filter((r) => r.aprobada).length;

    return Math.round(((leccionesCompletadas + tareasAprobadas) / total) * 100);
  }

  async desinscribir(cursoId: number): Promise<void> {
    await this.inscripciones.desinscribir(cursoId);
    this.enrolled.set(this.enrolled().filter((item) => item.curso.id !== cursoId));
  }

  imageSrc(course: Course): string {
    return courseImagePath(course.image);
  }

  async cambiarProgreso(cursoId: number, progreso: number): Promise<void> {
    const bounded = Math.max(0, Math.min(100, progreso));
    await this.inscripciones.actualizarProgreso(cursoId, bounded);
    this.enrolled.set(this.enrolled().map((item) =>
      item.curso.id === cursoId ? { ...item, progreso: bounded } : item
    ));
  }

  async calificar(cursoId: number, rating: number): Promise<void> {
    const { error } = await this.inscripciones.calificarCurso(cursoId, rating);
    this.ratingMessage.set(error ? error.message : 'Calificacion guardada.');
  }

  abrirReembolso(curso: Course): void {
    this.cursoReembolso.set(curso);
    this.motivoReembolso.set('');
    this.correoReembolso.set('');
    this.modalReembolso.set(true);
  }

  cerrarReembolso(): void {
    this.modalReembolso.set(false);
    this.cursoReembolso.set(null);
    this.motivoReembolso.set('');
    this.correoReembolso.set('');
  }

  async enviarReembolso(): Promise<void> {
    const curso = this.cursoReembolso();
    if (!curso) return;
    await this.inscripciones.desinscribir(curso.id);
    this.enrolled.set(this.enrolled().filter((item) => item.curso.id !== curso.id));
    this.cerrarReembolso();
  }

  openRating(cursoId: number): void {
    const existing = this.userRatings()[cursoId];
    this.ratingCursoId.set(cursoId);
    this.ratingStars.set(existing?.stars || 0);
    this.ratingHoverCursoId.set(null);
    this.ratingHoverStars.set(0);
    this.ratingComment.set(existing?.comment || '');
    this.ratingModal.set(true);
  }

  setRatingHover(cursoId: number, stars: number): void {
    this.ratingHoverCursoId.set(cursoId);
    this.ratingHoverStars.set(stars);
  }

  clearRatingHover(): void {
    this.ratingHoverCursoId.set(null);
    this.ratingHoverStars.set(0);
  }

  setRating(stars: number): void {
    this.ratingStars.set(stars);
  }

  submitRating(): void {
    const cursoId = this.ratingCursoId();
    if (!cursoId || this.ratingStars() === 0) return;
    const rating = {
      stars: this.ratingStars(),
      comment: this.ratingComment().trim()
    };
    this.userRatings.update((r) => ({ ...r, [cursoId]: rating }));
    this.inscripciones.calificarCurso(cursoId, rating.stars, rating.comment);
    this.ratingModal.set(false);
    this.ratingCursoId.set(null);
    this.ratingStars.set(0);
    this.ratingHoverCursoId.set(null);
    this.ratingHoverStars.set(0);
    this.ratingComment.set('');
  }

  closeRating(): void {
    this.ratingModal.set(false);
    this.ratingCursoId.set(null);
    this.ratingStars.set(0);
    this.ratingHoverCursoId.set(null);
    this.ratingHoverStars.set(0);
    this.ratingComment.set('');
  }

  getStarClass(cursoId: number, star: number): string {
    const saved = this.userRatings()[cursoId];
    if (this.ratingHoverCursoId() === cursoId && this.ratingHoverStars() > 0) {
      return star <= this.ratingHoverStars() ? 'star filled' : 'star empty';
    }
    if (saved && saved.stars > 0) {
      return star <= saved.stars ? 'star filled' : 'star empty';
    }
    return 'star empty';
  }

  hasRating(cursoId: number): boolean {
    const saved = this.userRatings()[cursoId];
    return !!saved && saved.stars > 0;
  }

  async salir(): Promise<void> {
    await this.auth.signOut();
    await this.router.navigate(['/home']);
  }
}
