import { Component, computed, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { CursosService } from '../../core/services/cursos.service';
import { CategoriasService } from '../../core/services/categorias.service';
import { TareasService, TareaDb, TareaInput } from '../../core/services/tareas.service';
import { LeccionesService } from '../../core/services/lecciones.service';
import { CursoItemsService } from '../../core/services/curso-items.service';
import { InscripcionesService, AlumnoInscrito } from '../../core/services/inscripciones.service';
import { CursoDb, CursoInput } from '../../core/models/curso.model';
import { Leccion } from '../../core/models/leccion.model';
import { TareaContenido, TareaContenidoTipo } from '../../core/models/tarea.model';
import { courseImagePath } from '../../core/data/academy-data';
import { CourseForm } from '../../shared/components/course-form/course-form';
import { TareaForm } from '../../shared/components/tarea-form/tarea-form';
import { LessonBuilder } from '../../shared/components/lesson-builder/lesson-builder';

type Tab = 'panel' | 'cursos' | 'lecciones' | 'tareas' | 'alumnos';

interface ActividadOrden {
  tipo: 'leccion' | 'tarea';
  item_id: number;
  titulo: string;
  subtitulo: string;
  created_at: string;
  posicion: number;
}

@Component({
  selector: 'app-profesor-dashboard',
  imports: [RouterLink, FormsModule, CourseForm, TareaForm, LessonBuilder],
  templateUrl: './profesor-dashboard.html',
  styleUrl: './profesor-dashboard.css'
})
export class ProfesorDashboard {
  private readonly auth = inject(AuthService);
  private readonly cursos = inject(CursosService);
  private readonly categoriasService = inject(CategoriasService);
  private readonly tareasService = inject(TareasService);
  private readonly leccionesService = inject(LeccionesService);
  private readonly cursoItemsService = inject(CursoItemsService);
  private readonly inscripciones = inject(InscripcionesService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);

  readonly tab = signal<Tab>('panel');
  readonly misCursos = signal<CursoDb[]>([]);
  readonly tareas = signal<TareaDb[]>([]);
  readonly categorias = signal<string[]>([]);
  readonly alumnos = signal<AlumnoInscrito[]>([]);
  readonly cursoAlumnosId = signal<number | null>(null);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly courseSearch = signal('');
  readonly profileMenuOpen = signal(false);
  readonly cursoRatings = signal<Record<number, number>>({});
  readonly alumnoPerfil = signal<AlumnoInscrito | null>(null);
  readonly cursosAlumno = signal<{ id: number; titulo: string; rating: number }[]>([]);

  async verPerfilAlumno(alumno: AlumnoInscrito): Promise<void> {
    this.alumnoPerfil.set(alumno);
    const client = this.auth.getSupabase();
    if (!client) return;

    const { data: perfil } = await client
      .from('profiles')
      .select('full_name, email, avatar_url')
      .eq('id', alumno.usuario_id)
      .single();

    if (perfil) {
      this.alumnoPerfil.update((a) => a ? ({
        ...a,
        nombre: perfil.full_name || a.nombre,
        email: perfil.email || a.email,
        avatar_url: perfil.avatar_url || undefined
      }) : a);
    }

    const { data: inscripciones } = await client
      .from('inscripciones')
      .select('curso_id')
      .eq('usuario_id', alumno.usuario_id);

    if (!inscripciones || inscripciones.length === 0) {
      this.cursosAlumno.set([]);
      return;
    }

    const cursosIds = inscripciones.map((i: any) => i.curso_id);
    const misCursosIds = this.misCursos().map((c) => c.id);
    const cursosComunes = cursosIds.filter((id: number) => misCursosIds.includes(id));

    const cursosConRating: { id: number; titulo: string; rating: number }[] = [];
    for (const cursoId of cursosComunes) {
      const { data: review } = await client
        .from('curso_reviews')
        .select('rating')
        .eq('curso_id', cursoId)
        .eq('usuario_id', alumno.usuario_id)
        .single();

      const curso = this.misCursos().find((c) => c.id === cursoId);
      cursosConRating.push({
        id: cursoId,
        titulo: curso?.titulo || 'Curso',
        rating: review?.rating || 0
      });
    }

    this.cursosAlumno.set(cursosConRating);
  }

  cerrarPerfilAlumno(): void {
    this.alumnoPerfil.set(null);
    this.cursosAlumno.set([]);
  }

  readonly profesorRating = computed(() => {
    const ratings = this.cursoRatings();
    const ids = Object.keys(ratings);
    if (ids.length === 0) return 0;
    const sum = ids.reduce((acc, id) => acc + ratings[+id], 0);
    return sum / ids.length;
  });

  readonly mostrarCursoForm = signal(false);
  readonly cursoEditando = signal<CursoDb | null>(null);
  readonly cursoEliminando = signal<CursoDb | null>(null);
  readonly eliminandoCurso = signal(false);
  readonly modalActividadesCurso = signal<CursoDb | null>(null);
  readonly actividades = signal<ActividadOrden[]>([]);
  readonly guardandoOrden = signal(false);
  readonly mostrarTareaForm = signal(false);
  readonly tareaEditando = signal<TareaDb | null>(null);
  readonly guardandoTarea = signal(false);
  readonly tareaExpandida = signal<number | null>(null);
  readonly contenidoTarea = signal<TareaContenido[]>([]);
  readonly guardandoContenido = signal(false);
  readonly modalContenidoTipo = signal<TareaContenidoTipo | null>(null);
  readonly modalContenidoEditar = signal<TareaContenido | null>(null);
  readonly modalContenidoTitulo = signal('');
  readonly modalContenidoTexto = signal('');
  readonly modalContenidoOpciones = signal<string[]>([]);
  readonly modalContenidoRespuestaCorrecta = signal('');
  readonly modalContenidoNuevaOpcion = signal('');
  readonly contenidoEliminando = signal<TareaContenido | null>(null);
  readonly tiposContenidoTarea = this.tareasService.tiposContenido();

  readonly tareaConContenido = computed(() => {
    const expandida = this.tareaExpandida();
    return this.tareas().find((t) => t.id === expandida) ?? null;
  });

  readonly teacherName = computed(() => this.auth.profile()?.full_name || 'Profesor');
  readonly teacherId = computed(() => this.auth.user()?.id ?? null);
  readonly avatarUrl = computed(() => this.auth.profile()?.avatar_url || '');

  readonly perfilUrl = computed(() => {
    const id = this.auth.user()?.id;
    return id ? `/instructor/${id}` : null;
  });

  readonly filteredCursos = computed(() => {
    const query = this.courseSearch().trim().toLowerCase();
    if (!query) return this.misCursos();
    return this.misCursos().filter((curso) =>
      curso.titulo.toLowerCase().includes(query)
      || curso.categoria.toLowerCase().includes(query)
      || curso.estado.toLowerCase().includes(query)
    );
  });

  readonly metrics = computed(() => {
    const cursosCount = this.misCursos().length;
    const alumnos = this.misCursos().reduce((acc, curso) => acc + (curso.students ?? 0), 0);
    const pendientes = this.tareas().filter((t) => t.estado !== 'Completada').length;
    return [
      { label: 'Cursos Publicados', value: String(this.misCursos().filter((c) => c.estado === 'Publicado').length) },
      { label: 'Cursos Totales', value: String(cursosCount) },
      { label: 'Tareas Activas', value: String(pendientes) },
      { label: 'Estudiantes Inscritos', value: String(alumnos) }
    ];
  });

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    } else {
      this.cargando.set(false);
    }
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    const [cursos, tareas, categorias] = await Promise.all([
      this.cursos.listMios(),
      this.tareasService.listDb(),
      this.categoriasService.nombresActivas()
    ]);

    for (const curso of cursos) {
      curso.students = await this.cursos.countAlumnos(curso.id);
    }

    this.misCursos.set(cursos);
    this.tareas.set(tareas);
    this.categorias.set(categorias);

    const client = this.auth.getSupabase();
    const ratingsMap: Record<number, number> = {};
    if (client) {
      for (const curso of cursos) {
        const { data: reviews } = await client
          .from('curso_reviews')
          .select('rating')
          .eq('curso_id', curso.id);
        if (reviews && reviews.length > 0) {
          const sum = reviews.reduce((acc: number, r: any) => acc + r.rating, 0);
          ratingsMap[curso.id] = sum / reviews.length;
        }
      }
    }
    this.cursoRatings.set(ratingsMap);

    if (cursos.length > 0) {
      await this.verAlumnos(cursos[0].id);
    }
    this.cargando.set(false);
  }

  imageSrc(imagen: string): string {
    return courseImagePath(imagen);
  }

  fmt(precio: number): string {
    return Number(precio).toFixed(2);
  }

  setTab(tab: Tab): void {
    this.tab.set(tab);
    if (tab === 'alumnos' && this.cursoAlumnosId() === null && this.misCursos().length > 0) {
      void this.verAlumnos(this.misCursos()[0].id);
    }
  }

  async verAlumnos(cursoId: string | number): Promise<void> {
    const id = Number(cursoId);
    this.cursoAlumnosId.set(id);
    this.alumnos.set(await this.inscripciones.alumnosDeCurso(id));
  }

  async abrirModalActividades(curso: CursoDb): Promise<void> {
    const [lecciones, tareas, secuencia] = await Promise.all([
      this.leccionesService.listByCurso(curso.id),
      this.tareasService.listByCurso(curso.id),
      this.cursoItemsService.listByCurso(curso.id)
    ]);

    const secMap = new Map(secuencia.map((item) => [`${item.tipo === 'leccion' ? 'lec' : 'tar'}-${item.item_id}`, item.posicion]));

    const items: ActividadOrden[] = [
      ...lecciones.map((l: Leccion) => ({
        tipo: 'leccion' as const,
        item_id: l.id,
        titulo: l.titulo,
        subtitulo: 'Leccion',
        created_at: l.created_at || '',
        posicion: 0
      })),
      ...tareas.map((t) => ({
        tipo: 'tarea' as const,
        item_id: t.id,
        titulo: t.titulo,
        subtitulo: 'Tarea',
        created_at: t.created_at || '',
        posicion: 0
      }))
    ];

    items.sort((a, b) => {
      const pa = secMap.get(`${a.tipo === 'leccion' ? 'lec' : 'tar'}-${a.item_id}`) ?? Number.MAX_SAFE_INTEGER;
      const pb = secMap.get(`${b.tipo === 'leccion' ? 'lec' : 'tar'}-${b.item_id}`) ?? Number.MAX_SAFE_INTEGER;
      return pa - pb || a.created_at.localeCompare(b.created_at) || a.titulo.localeCompare(b.titulo);
    });

    items.forEach((item, index) => { item.posicion = index + 1; });

    this.modalActividadesCurso.set(curso);
    this.actividades.set(items);
  }

  cerrarModalActividades(): void {
    if (this.guardandoOrden()) return;
    this.modalActividadesCurso.set(null);
    this.actividades.set([]);
  }

  subirActividad(index: number): void {
    if (index <= 0) return;
    this.actividades.update((items) => {
      const nueva = [...items];
      [nueva[index - 1], nueva[index]] = [nueva[index], nueva[index - 1]];
      nueva.forEach((item, i) => { item.posicion = i + 1; });
      return nueva;
    });
  }

  bajarActividad(index: number): void {
    const items = this.actividades();
    if (index >= items.length - 1) return;
    this.actividades.update((lista) => {
      const nueva = [...lista];
      [nueva[index + 1], nueva[index]] = [nueva[index], nueva[index + 1]];
      nueva.forEach((item, i) => { item.posicion = i + 1; });
      return nueva;
    });
  }

  async guardarOrdenActividades(): Promise<void> {
    const curso = this.modalActividadesCurso();
    if (!curso || this.guardandoOrden()) return;

    this.guardandoOrden.set(true);
    const { error } = await this.cursoItemsService.guardarOrden(
      curso.id,
      this.actividades().map((item) => ({ tipo: item.tipo, item_id: item.item_id }))
    );
    this.guardandoOrden.set(false);

    if (error) {
      this.error.set(error.message);
    } else {
      this.error.set('');
      this.modalActividadesCurso.set(null);
      this.actividades.set([]);
    }
  }

  abrirNuevoCurso(): void {
    this.cursoEditando.set(null);
    this.mostrarCursoForm.set(true);
  }

  nuevoCurso(): void {
    this.setTab('cursos');
    this.abrirNuevoCurso();
  }

  abrirEditarCurso(curso: CursoDb): void {
    this.cursoEditando.set(curso);
    this.mostrarCursoForm.set(true);
  }

  async guardarCurso(input: CursoInput): Promise<void> {
    const editando = this.cursoEditando();
    const payload = {
      ...input,
      instructor: this.teacherName(),
      instructor_id: this.teacherId()
    };
    const result = editando
      ? await this.cursos.actualizar(editando.id, payload)
      : await this.cursos.crear(payload);

    if (result.error) {
      this.error.set(result.error.message);
    } else {
      this.error.set('');
      this.mostrarCursoForm.set(false);
      await this.cargar();
    }
  }

  pedirEliminarCurso(curso: CursoDb): void {
    this.cursoEliminando.set(curso);
  }

  cancelarEliminarCurso(): void {
    if (!this.eliminandoCurso()) {
      this.cursoEliminando.set(null);
    }
  }

  async confirmarEliminarCurso(): Promise<void> {
    const curso = this.cursoEliminando();
    if (!curso) return;

    this.eliminandoCurso.set(true);
    const { error } = await this.cursos.eliminar(curso.id);
    this.eliminandoCurso.set(false);
    if (error) {
      this.error.set(error.message);
    } else {
      this.misCursos.set(this.misCursos().filter((item) => item.id !== curso.id));
      this.cursoEliminando.set(null);
    }
  }

  abrirNuevaTarea(): void {
    this.tareaEditando.set(null);
    this.mostrarTareaForm.set(true);
  }

  abrirEditarTarea(tarea: TareaDb): void {
    this.tareaEditando.set(tarea);
    this.mostrarTareaForm.set(true);
  }

  async guardarTarea(input: TareaInput): Promise<void> {
    if (this.guardandoTarea()) return;

    this.guardandoTarea.set(true);
    const editando = this.tareaEditando();
    const result = editando
      ? await this.tareasService.actualizar(editando.id, input)
      : await this.tareasService.crear(input);
    this.guardandoTarea.set(false);

    if (result.error) {
      this.error.set(result.error.message);
    } else {
      this.error.set('');
      this.mostrarTareaForm.set(false);
      await this.cargar();
    }
  }

  async eliminarTarea(tareaId: number): Promise<void> {
    const confirmado = confirm('Eliminar esta tarea?');
    if (!confirmado) return;

    const { error } = await this.tareasService.eliminar(tareaId);
    if (error) {
      this.error.set(error.message);
    } else {
      this.tareas.set(this.tareas().filter((tarea) => tarea.id !== tareaId));
    }
  }

  estadoTarea(tarea: TareaDb): void {
    const siguiente = tarea.estado === 'Completada' ? 'Pendiente' : 'Completada';
    void this.tareasService.actualizar(tarea.id, { estado: siguiente, progreso: siguiente === 'Completada' ? 100 : tarea.progreso });
    tarea.estado = siguiente;
    tarea.progreso = siguiente === 'Completada' ? 100 : tarea.progreso;
  }

  async toggleTarea(tarea: TareaDb): Promise<void> {
    if (this.tareaExpandida() === tarea.id) {
      this.tareaExpandida.set(null);
      this.contenidoTarea.set([]);
      return;
    }
    this.tareaExpandida.set(tarea.id);
    await this.cargarContenidoTarea(tarea.id);
  }

  async cargarContenidoTarea(tareaId: number): Promise<void> {
    this.contenidoTarea.set(await this.tareasService.listContenido(tareaId));
  }

  abrirAgregarContenidoTarea(tipo: TareaContenidoTipo): void {
    this.modalContenidoEditar.set(null);
    this.modalContenidoTitulo.set('');
    this.modalContenidoTexto.set('');
    this.modalContenidoOpciones.set(['', '']);
    this.modalContenidoRespuestaCorrecta.set('');
    this.modalContenidoNuevaOpcion.set('');
    this.modalContenidoTipo.set(tipo);
  }

  abrirEditarContenidoTarea(contenido: TareaContenido): void {
    this.modalContenidoEditar.set(contenido);
    this.modalContenidoTipo.set(contenido.tipo);
    this.modalContenidoNuevaOpcion.set('');

    const parsed = this.parseContenidoTarea(contenido.contenido);
    if (contenido.tipo === 'titulo' || contenido.tipo === 'texto') {
      this.modalContenidoTitulo.set(parsed.titulo);
      this.modalContenidoTexto.set(contenido.contenido);
    } else if (contenido.tipo === 'opcion_multiple') {
      this.modalContenidoTitulo.set(parsed.titulo);
      this.modalContenidoOpciones.set(parsed.opciones.length > 0 ? parsed.opciones : ['', '']);
      this.modalContenidoRespuestaCorrecta.set(parsed.respuesta_correcta ?? '');
    } else if (contenido.tipo === 'respuesta_texto') {
      this.modalContenidoTitulo.set(parsed.titulo);
      this.modalContenidoTexto.set('');
    }
  }

  cerrarModalContenidoTarea(): void {
    this.modalContenidoTipo.set(null);
    this.modalContenidoEditar.set(null);
  }

  agregarOpcionMultiple(): void {
    this.modalContenidoOpciones.update((opciones) => [...opciones, '']);
  }

  quitarOpcionMultiple(index: number): void {
    this.modalContenidoOpciones.update((opciones) => {
      const nueva = opciones.filter((_, i) => i !== index);
      if (this.modalContenidoRespuestaCorrecta() === opciones[index]) {
        this.modalContenidoRespuestaCorrecta.set('');
      }
      return nueva;
    });
  }

  getOpcionMultiple(index: number): string {
    return this.modalContenidoOpciones()[index] ?? '';
  }

  setOpcionMultiple(index: number, valor: string): void {
    this.modalContenidoOpciones.update((opciones) => {
      const nueva = [...opciones];
      nueva[index] = valor;
      if (this.modalContenidoRespuestaCorrecta() === opciones[index]) {
        this.modalContenidoRespuestaCorrecta.set('');
      }
      return nueva;
    });
  }

  onNuevaOpcionEnter(): void {
    const valor = this.modalContenidoNuevaOpcion().trim();
    if (!valor) return;
    this.modalContenidoOpciones.update((opciones) => [...opciones, valor]);
    this.modalContenidoNuevaOpcion.set('');
  }

  getTipoContenidoEtiqueta(tipo: TareaContenidoTipo): string {
    return this.tiposContenidoTarea.find((t) => t.valor === tipo)?.etiqueta || tipo;
  }

  getIconoTipoContenidoTarea(tipo: TareaContenidoTipo): string {
    return this.tiposContenidoTarea.find((t) => t.valor === tipo)?.icono || '?';
  }

  parseContenidoTarea(contenido: string): { titulo: string; opciones: string[]; respuesta_correcta?: string } {
    try {
      const parsed = JSON.parse(contenido);
      return {
        titulo: parsed.titulo ?? '',
        opciones: Array.isArray(parsed.opciones) ? parsed.opciones : [],
        respuesta_correcta: parsed.respuesta_correcta
      };
    } catch {
      return { titulo: contenido, opciones: [] };
    }
  }

  getTituloContenidoTarea(contenido: TareaContenido): string {
    if (contenido.tipo === 'titulo' || contenido.tipo === 'respuesta_texto') {
      return contenido.contenido;
    }
    return this.parseContenidoTarea(contenido.contenido).titulo || contenido.contenido;
  }

  async guardarContenidoTarea(): Promise<void> {
    const tipo = this.modalContenidoTipo();
    const tareaId = this.tareaExpandida();
    if (!tipo || tareaId === null) return;

    let contenido = '';
    if (tipo === 'titulo' || tipo === 'texto' || tipo === 'respuesta_texto') {
      const texto = (tipo === 'titulo' ? this.modalContenidoTitulo() : this.modalContenidoTexto()).trim();
      if (!texto) return;
      contenido = texto;
    } else if (tipo === 'opcion_multiple') {
      const titulo = this.modalContenidoTitulo().trim();
      const opciones = this.modalContenidoOpciones().map((opcion) => opcion.trim()).filter((opcion) => opcion.length > 0);
      const respuestaCorrecta = this.modalContenidoRespuestaCorrecta();
      if (!titulo) return;
      if (opciones.length < 2) return;
      if (!respuestaCorrecta) return;
      contenido = JSON.stringify({ titulo, opciones, respuesta_correcta: respuestaCorrecta });
    }

    this.guardandoContenido.set(true);
    try {
      const editando = this.modalContenidoEditar();
      if (editando) {
        const result = await this.tareasService.actualizarContenido(editando.id, { contenido });
        if (result.error) {
          this.error.set(result.error.message);
          return;
        }
      } else {
        const existente = await this.tareasService.listContenido(tareaId);
        const orden = existente.length > 0 ? Math.max(...existente.map((c) => c.orden)) + 1 : 1;
        const result = await this.tareasService.agregarContenido({ tarea_id: tareaId, tipo, contenido, orden });
        if (result.error) {
          this.error.set(result.error.message);
          return;
        }
      }
      this.cerrarModalContenidoTarea();
      await this.cargarContenidoTarea(tareaId);
    } finally {
      this.guardandoContenido.set(false);
    }
  }

  pedirEliminarContenidoTarea(contenido: TareaContenido): void {
    this.contenidoEliminando.set(contenido);
  }

  cancelarEliminarContenidoTarea(): void {
    if (!this.guardandoContenido()) {
      this.contenidoEliminando.set(null);
    }
  }

  async confirmarEliminarContenidoTarea(): Promise<void> {
    const contenido = this.contenidoEliminando();
    const tareaId = this.tareaExpandida();
    if (!contenido || tareaId === null) return;

    this.guardandoContenido.set(true);
    const { error } = await this.tareasService.eliminarContenido(contenido.id);
    this.guardandoContenido.set(false);
    this.contenidoEliminando.set(null);
    if (error) {
      this.error.set(error.message);
    } else {
      await this.cargarContenidoTarea(tareaId);
    }
  }

  async salir(): Promise<void> {
    await this.auth.signOut();
    await this.router.navigate(['/home']);
  }
}
