import { Component, computed, inject, signal, viewChild, ElementRef, afterNextRender, Injector, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CursosService } from '../../core/services/cursos.service';
import { LeccionesService } from '../../core/services/lecciones.service';
import { TareasService, TareaDb } from '../../core/services/tareas.service';
import { CursoItemsService } from '../../core/services/curso-items.service';
import { CursoDb } from '../../core/models/curso.model';
import { Leccion, ContenidoLeccion } from '../../core/models/leccion.model';
import { TareaContenido, TareaResultado } from '../../core/models/tarea.model';

type SidebarItem =
  | { tipo: 'leccion'; key: string; created_at: string; posicion: number | null; leccion: Leccion }
  | { tipo: 'tarea'; key: string; created_at: string; posicion: number | null; tarea: TareaDb };

@Component({
  selector: 'app-course-learn',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './course-learn.html',
  styleUrl: './course-learn.css'
})
export class CourseLearn implements OnInit {
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);
  private cursosService = inject(CursosService);
  private leccionesService = inject(LeccionesService);
  private tareasService = inject(TareasService);
  private cursoItemsService = inject(CursoItemsService);
  private injector = inject(Injector);

  readonly curso = signal<CursoDb | null>(null);
  readonly lecciones = signal<Leccion[]>([]);
  readonly tareas = signal<TareaDb[]>([]);
  readonly secuencia = signal<Map<string, number>>(new Map());
  readonly leccionActiva = signal<Leccion | null>(null);
  readonly tareaActiva = signal<TareaDb | null>(null);
  readonly contenido = signal<ContenidoLeccion[]>([]);
  readonly contenidoTarea = signal<TareaContenido[]>([]);
  readonly respuestasOpcion = signal<Record<number, string>>({});
  readonly respuestasTexto = signal<Record<number, string>>({});
  readonly resultados = signal<Record<number, TareaResultado>>({});
  readonly finalizando = signal(false);
  readonly mostrarResultadoFinal = signal(false);
  readonly calificacionFinal = signal(0);
  readonly vistas = signal<Set<number>>(new Set());
  readonly tarjetasVolteadas = signal<Set<number>>(new Set());
  readonly cargando = signal(false);
  readonly cargandoCurso = signal(true);
  readonly sidebarOpen = signal(true);
  readonly learnContent = viewChild<ElementRef<HTMLElement>>('learnContent');

  readonly itemsSidebar = computed<SidebarItem[]>(() => {
    const secuencia = this.secuencia();
    const leccionesItems: SidebarItem[] = this.lecciones().map((leccion) => ({
      tipo: 'leccion',
      key: `lec-${leccion.id}`,
      created_at: leccion.created_at || '',
      posicion: secuencia.get(`lec-${leccion.id}`) ?? null,
      leccion
    }));
    const tareasItems: SidebarItem[] = this.tareas().map((tarea) => ({
      tipo: 'tarea',
      key: `tar-${tarea.id}`,
      created_at: tarea.created_at || '',
      posicion: secuencia.get(`tar-${tarea.id}`) ?? null,
      tarea
    }));
    return [...leccionesItems, ...tareasItems].sort(
      (a, b) => {
        const pa = a.posicion ?? Number.MAX_SAFE_INTEGER;
        const pb = b.posicion ?? Number.MAX_SAFE_INTEGER;
        return pa - pb || a.created_at.localeCompare(b.created_at) || a.key.localeCompare(b.key);
      }
    );
  });

  readonly actividadesBloqueadas = computed<Set<string>>(() => {
    const bloqueadas = new Set<string>();
    const resultados = this.resultados();
    const pendientes: TareaDb[] = [];
    for (const item of this.itemsSidebar()) {
      const bloqueada = pendientes.length > 0;
      if (item.tipo === 'tarea') {
        const aprobada = resultados[item.tarea.id]?.aprobada === true;
        if (aprobada) {
          const idx = pendientes.findIndex((t) => t.id === item.tarea.id);
          if (idx !== -1) pendientes.splice(idx, 1);
        } else {
          pendientes.push(item.tarea);
        }
      }
      if (bloqueada) bloqueadas.add(item.key);
    }
    return bloqueadas;
  });

  readonly filas = computed<ContenidoLeccion[][]>(() => {
    const bloques = this.contenido();
    const filas: ContenidoLeccion[][] = [];
    let fila: ContenidoLeccion[] = [];
    for (const bloque of bloques) {
      if (bloque.tipo === 'tarjeta') {
        fila.push(bloque);
      } else {
        if (fila.length > 0) {
          filas.push(fila);
          fila = [];
        }
        filas.push([bloque]);
      }
    }
    if (fila.length > 0) filas.push(fila);
    return filas;
  });

  ngOnInit(): void {
    const cursoId = Number(this.route.snapshot.paramMap.get('id'));
    if (cursoId) {
      void this.cargarCurso(cursoId);
    }
  }

  async cargarCurso(cursoId: number): Promise<void> {
    this.cargandoCurso.set(true);
    this.cargando.set(true);
    try {
      const curso = await this.cursosService.get(cursoId);
      if (curso) {
        this.curso.set({
          id: curso.id,
          titulo: curso.title,
          categoria: curso.category,
          nivel: curso.level,
          instructor: curso.instructor,
          instructor_id: curso.instructorId ?? null,
          precio: curso.price,
          old_precio: curso.oldPrice ?? null,
          imagen: curso.image,
          descripcion: curso.description ?? null,
          estado: 'Publicado',
          rating: curso.rating,
          reviews: curso.reviews,
          created_at: ''
        });
      }
      const lecciones = await this.leccionesService.listByCurso(cursoId);
      this.lecciones.set(lecciones);
      const tareas = await this.tareasService.listParaAlumno([cursoId]);
      this.tareas.set(tareas);
      const secuencia = await this.cursoItemsService.listByCurso(cursoId);
      this.secuencia.set(new Map(secuencia.map((item) => [`${item.tipo === 'leccion' ? 'lec' : 'tar'}-${item.item_id}`, item.posicion])));
      if (this.auth.user()) {
        const vistas = await this.leccionesService.leccionesVistasDeCurso(cursoId);
        this.vistas.set(new Set(vistas));
        const resultados = await this.tareasService.resultadosDeTareas(tareas.map((t) => t.id));
        this.resultados.set(Object.fromEntries(resultados.map((r) => [r.tarea_id, r])));
      }
      const items = this.itemsSidebar();
      if (items.length > 0) {
        const primero = items[0];
        if (primero.tipo === 'leccion') {
          await this.seleccionarLeccion(primero.leccion);
        } else {
          await this.seleccionarTarea(primero.tarea);
        }
      }
    } finally {
      this.cargando.set(false);
      this.cargandoCurso.set(false);
    }
  }

  async seleccionarLeccion(leccion: Leccion): Promise<void> {
    if (this.esLeccionBloqueada(leccion.id)) return;
    this.leccionActiva.set(leccion);
    this.tareaActiva.set(null);
    this.contenidoTarea.set([]);
    this.mostrarResultadoFinal.set(false);
    const contentEl = this.learnContent()?.nativeElement;
    if (contentEl) contentEl.scrollTop = 0;
    this.cargando.set(true);
    try {
      const data = await this.leccionesService.getWithContenido(leccion.id);
      const contenido = (data?.contenido || []).sort((a, b) => a.orden - b.orden);
      this.contenido.set(contenido);
    } finally {
      this.cargando.set(false);
    }
    afterNextRender(() => this.comprobarFinLeccion(), { injector: this.injector });
  }

  onLearnScroll(): void {
    this.comprobarFinLeccion();
  }

  private comprobarFinLeccion(): void {
    const el = this.learnContent()?.nativeElement;
    const leccion = this.leccionActiva();
    if (!el || !leccion) return;
    if (this.esVista(leccion.id)) return;
    const umbral = 24;
    if (el.scrollHeight - el.scrollTop - el.clientHeight <= umbral) {
      this.marcarLeccionCompletada(leccion.id);
    }
  }

  private marcarLeccionCompletada(leccionId: number): void {
    this.vistas.update((v) => new Set(v).add(leccionId));
    if (this.auth.user()) {
      void this.leccionesService.marcarVista(leccionId);
    }
  }

  async seleccionarTarea(tarea: TareaDb): Promise<void> {
    if (this.esTareaBloqueada(tarea.id)) return;
    this.tareaActiva.set(tarea);
    this.leccionActiva.set(null);
    this.contenido.set([]);
    this.respuestasOpcion.set({});
    this.respuestasTexto.set({});
    this.mostrarResultadoFinal.set(false);
    this.cargando.set(true);
    try {
      this.contenidoTarea.set(await this.tareasService.listContenido(tarea.id));
    } finally {
      this.cargando.set(false);
    }
  }

  esLeccionBloqueada(leccionId: number): boolean {
    return this.actividadesBloqueadas().has(`lec-${leccionId}`);
  }

  esTareaBloqueada(tareaId: number): boolean {
    return this.actividadesBloqueadas().has(`tar-${tareaId}`);
  }

  esTareaAprobada(tareaId: number): boolean {
    return this.resultados()[tareaId]?.aprobada === true;
  }

  porcentajeAprobacion(tarea: TareaDb): number {
    return tarea.porcentaje_aprobacion || 60;
  }

  resultadoTarea(tareaId: number): TareaResultado | null {
    return this.resultados()[tareaId] ?? null;
  }

  calcularCalificacion(): number {
    const preguntas = this.contenidoTarea().filter((b) => b.tipo === 'opcion_multiple' || b.tipo === 'respuesta_texto');
    if (preguntas.length === 0) return 0;
    let aciertos = 0;
    for (const pregunta of preguntas) {
      if (pregunta.tipo === 'respuesta_texto') {
        if (this.getRespuestaTexto(pregunta.id).trim().length > 0) aciertos++;
      } else {
        const data = this.parseContenidoTarea(pregunta.contenido);
        if (data.respuesta_correcta && this.opcionSeleccionada(pregunta.id, data.respuesta_correcta)) aciertos++;
      }
    }
    return Math.round((aciertos / preguntas.length) * 100);
  }

  async finalizarTarea(): Promise<void> {
    const tarea = this.tareaActiva();
    if (!tarea || this.finalizando()) return;
    this.finalizando.set(true);
    try {
      const calificacion = this.calcularCalificacion();
      const porcentaje = this.porcentajeAprobacion(tarea);
      const aprobada = calificacion >= porcentaje;
      const respuestas = {
        opciones: this.respuestasOpcion(),
        textos: this.respuestasTexto()
      };
      const { data, error } = await this.tareasService.guardarResultado(tarea.id, calificacion, aprobada, respuestas);
      if (error || !data) {
        console.error('Error al guardar el resultado:', error);
        return;
      }
      this.resultados.update((r) => ({ ...r, [tarea.id]: data }));
      this.calificacionFinal.set(calificacion);
      this.mostrarResultadoFinal.set(true);
      if (aprobada) {
        await this.irASiguiente();
      }
    } finally {
      this.finalizando.set(false);
    }
  }

  private async irASiguiente(): Promise<void> {
    const tarea = this.tareaActiva();
    if (!tarea) return;
    const items = this.itemsSidebar();
    const idx = items.findIndex((i) => i.tipo === 'tarea' && i.tarea.id === tarea.id);
    const siguiente = items[idx + 1];
    if (!siguiente) return;
    if (siguiente.tipo === 'leccion') {
      await this.seleccionarLeccion(siguiente.leccion);
    } else {
      await this.seleccionarTarea(siguiente.tarea);
    }
  }

  reintentarTarea(): void {
    this.mostrarResultadoFinal.set(false);
  }

  respuestaDePregunta(bloqueId: number): string {
    return this.respuestasOpcion()[bloqueId] ?? this.getRespuestaTexto(bloqueId);
  }

  respuestaCorrectaDe(bloque: TareaContenido): string {
    return this.parseContenidoTarea(bloque.contenido).respuesta_correcta ?? '';
  }

  esCorrecta(bloque: TareaContenido): boolean {
    if (bloque.tipo === 'respuesta_texto') {
      return this.getRespuestaTexto(bloque.id).trim().length > 0;
    }
    const correcta = this.parseContenidoTarea(bloque.contenido).respuesta_correcta;
    return !!correcta && this.opcionSeleccionada(bloque.id, correcta);
  }

  claseOpcion(bloque: TareaContenido, opcion: string): string {
    const correcta = this.respuestaCorrectaDe(bloque);
    if (correcta === opcion) return 'correct';
    if (this.opcionSeleccionada(bloque.id, opcion)) return 'wrong';
    return '';
  }

  esLeccionActiva(item: SidebarItem): boolean {
    return item.tipo === 'leccion' && this.leccionActiva()?.id === item.leccion.id;
  }

  esTareaActiva(item: SidebarItem): boolean {
    return item.tipo === 'tarea' && this.tareaActiva()?.id === item.tarea.id;
  }

  numeroLeccion(item: SidebarItem): number {
    return this.itemsSidebar().filter((i) => i.tipo === 'leccion').findIndex((i) => i.key === item.key) + 1;
  }

  esVista(leccionId: number): boolean {
    return this.vistas().has(leccionId);
  }

  esTarjetaVolteada(bloqueId: number): boolean {
    return this.tarjetasVolteadas().has(bloqueId);
  }

  voltearTarjeta(bloqueId: number): void {
    this.tarjetasVolteadas.update((v) => {
      const nuevo = new Set(v);
      if (nuevo.has(bloqueId)) {
        nuevo.delete(bloqueId);
      } else {
        nuevo.add(bloqueId);
      }
      return nuevo;
    });
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((v) => !v);
  }

  getTarjetaData(contenido: string): { titulo: string; descripcion: string; imagen?: string } {
    try {
      const parsed = JSON.parse(contenido);
      return { titulo: parsed.titulo || '', descripcion: parsed.descripcion || '', imagen: parsed.imagen || '' };
    } catch { return { titulo: '', descripcion: contenido, imagen: '' }; }
  }

  getFormData(contenido: string): { titulo: string; campos: any[] } {
    try { return JSON.parse(contenido); } catch { return { titulo: '', campos: [] }; }
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

  opcionSeleccionada(bloqueId: number, opcion: string): boolean {
    return this.respuestasOpcion()[bloqueId] === opcion;
  }

  toggleOpcion(bloqueId: number, opcion: string): void {
    this.respuestasOpcion.update((r) => {
      const nuevo = { ...r };
      if (nuevo[bloqueId] === opcion) {
        delete nuevo[bloqueId];
      } else {
        nuevo[bloqueId] = opcion;
      }
      return nuevo;
    });
  }

  setRespuestaTexto(bloqueId: number, valor: string): void {
    this.respuestasTexto.update((r) => ({ ...r, [bloqueId]: valor }));
  }

  getRespuestaTexto(bloqueId: number): string {
    return this.respuestasTexto()[bloqueId] ?? '';
  }
}
