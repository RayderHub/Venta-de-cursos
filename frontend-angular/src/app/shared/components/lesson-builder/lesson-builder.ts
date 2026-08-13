import { Component, input, inject, signal, viewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LeccionesService } from '../../../core/services/lecciones.service';
import { AuthService } from '../../../core/services/auth.service';
import { Leccion, ContenidoLeccion, ContenidoTipo } from '../../../core/models/leccion.model';

@Component({
  selector: 'app-lesson-builder',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './lesson-builder.html',
  styleUrl: './lesson-builder.css'
})
export class LessonBuilder implements AfterViewInit, OnDestroy {
  private leccionesService = inject(LeccionesService);
  private auth = inject(AuthService);
  private mo: MutationObserver | null = null;

  ngAfterViewInit(): void {
    this.mo = new MutationObserver((mutations) => {
      mutations.forEach((m) => {
        m.addedNodes.forEach((node) => {
          if (node instanceof HTMLElement && node.classList.contains('modal-overlay')) {
            this.mo?.disconnect();
            document.body.appendChild(node);
            this.mo?.observe(document.body, { childList: true, subtree: true });
          }
        });
      });
    });
    this.mo.observe(document.body, { childList: true, subtree: true });
  }

  ngOnDestroy(): void {
    this.mo?.disconnect();
  }

  readonly cursos = input.required<{ id: number; titulo: string; categoria: string; nivel: string }[]>();
  readonly cursoSeleccionado = signal<{ id: number; titulo: string; categoria: string; nivel: string } | null>(null);
  readonly lecciones = signal<Leccion[]>([]);
  readonly leccionExpandida = signal<number | null>(null);
  readonly contenidoLeccion = signal<ContenidoLeccion[]>([]);
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly exito = signal('');
  readonly JSON = JSON;
  readonly fileInput = viewChild<ElementRef<HTMLInputElement>>('fileInput');
  hoverInsert = signal(-1);
  hoverBlock = signal<number | null>(null);

  readonly tiposContenido: { valor: ContenidoTipo; etiqueta: string; icono: string }[] = [
    { valor: 'titulo', etiqueta: 'Titulo', icono: 'H' },
    { valor: 'texto', etiqueta: 'Texto', icono: 'T' },
    { valor: 'imagen', etiqueta: 'Imagen', icono: 'IMG' },
    { valor: 'tarjeta', etiqueta: 'Tarjeta', icono: 'TRJ' },
    { valor: 'formulario', etiqueta: 'Formulario', icono: 'FRM' }
  ];

  nuevaLeccionTitulo = '';
  nuevaLeccionDescripcion = '';
  editandoLeccionId: number | null = null;
  editandoLeccionTitulo = '';
  editandoLeccionDescripcion = '';

  modalAbierto = signal<ContenidoTipo | null>(null);
  modalPosicion = 0;
  modalTitulo = '';
  modalTexto = '';
  modalDescripcion = '';
  modalCampos: { etiqueta: string; tipo: string; placeholder: string }[] = [];
  modalImagenPreview = '';
  editandoContenidoId: number | null = null;
  contenidoEliminar = signal<ContenidoLeccion | null>(null);
  mostrandoSelector = signal(false);

  async seleccionarCurso(curso: { id: number; titulo: string; categoria: string; nivel: string } | undefined): Promise<void> {
    if (!curso) {
      this.cursoSeleccionado.set(null);
      this.lecciones.set([]);
      this.leccionExpandida.set(null);
      this.contenidoLeccion.set([]);
      return;
    }
    this.cursoSeleccionado.set(curso);
    this.leccionExpandida.set(null);
    this.contenidoLeccion.set([]);
    this.cargando.set(true);
    try {
      const data = await this.leccionesService.listByCurso(curso.id);
      this.lecciones.set(data);
    } catch {
      this.error.set('No se pudieron cargar las lecciones.');
      setTimeout(() => this.error.set(''), 3000);
    } finally {
      this.cargando.set(false);
    }
  }

  async agregarLeccion(): Promise<void> {
    const curso = this.cursoSeleccionado();
    if (!curso || !this.nuevaLeccionTitulo.trim()) return;

    this.guardando.set(true);
    try {
      const result = await this.leccionesService.crearLeccion({
        curso_id: curso.id,
        titulo: this.nuevaLeccionTitulo.trim(),
        descripcion: this.nuevaLeccionDescripcion.trim() || null,
        orden: this.lecciones().length + 1
      });

      if (result.error) {
        this.error.set(result.error.message);
      } else {
        this.nuevaLeccionTitulo = '';
        this.nuevaLeccionDescripcion = '';
        await this.seleccionarCurso(curso);
      }
    } finally {
      this.guardando.set(false);
    }
  }

  iniciarEdicionLeccion(leccion: Leccion, event: Event): void {
    event.stopPropagation();
    this.editandoLeccionId = leccion.id;
    this.editandoLeccionTitulo = leccion.titulo;
    this.editandoLeccionDescripcion = leccion.descripcion || '';
  }

  async guardarEdicionLeccion(): Promise<void> {
    if (!this.editandoLeccionId || !this.editandoLeccionTitulo.trim()) return;

    this.guardando.set(true);
    try {
      const result = await this.leccionesService.actualizarLeccion(this.editandoLeccionId, {
        titulo: this.editandoLeccionTitulo.trim(),
        descripcion: this.editandoLeccionDescripcion.trim() || null
      });

      if (result.error) {
        this.error.set(result.error.message);
      } else {
        this.cancelarEdicionLeccion();
        const curso = this.cursoSeleccionado();
        if (curso) await this.seleccionarCurso(curso);
      }
    } finally {
      this.guardando.set(false);
    }
  }

  cancelarEdicionLeccion(): void {
    this.editandoLeccionId = null;
    this.editandoLeccionTitulo = '';
    this.editandoLeccionDescripcion = '';
  }

  async eliminarLeccion(leccion: Leccion, event: Event): Promise<void> {
    event.stopPropagation();
    if (!confirm(`Eliminar leccion "${leccion.titulo}"? Se eliminara todo su contenido.`)) return;

    this.guardando.set(true);
    try {
      const result = await this.leccionesService.eliminarLeccion(leccion.id);
      if (result.error) {
        this.error.set(result.error.message);
      } else {
        const curso = this.cursoSeleccionado();
        if (curso) await this.seleccionarCurso(curso);
      }
    } finally {
      this.guardando.set(false);
    }
  }

  async expandirLeccion(leccion: Leccion): Promise<void> {
    if (this.leccionExpandida() === leccion.id) {
      this.leccionExpandida.set(null);
      this.contenidoLeccion.set([]);
      return;
    }

    this.leccionExpandida.set(leccion.id);
    this.cargando.set(true);
    try {
      const data = await this.leccionesService.getWithContenido(leccion.id);
      const contenido = (data?.contenido || []).sort((a, b) => a.orden - b.orden);
      this.contenidoLeccion.set(contenido);
    } catch {
      this.error.set('No se pudo cargar el contenido.');
    } finally {
      this.cargando.set(false);
    }
  }

  abrirModalConTipo(tipo: ContenidoTipo, posicion: number): void {
    this.modalPosicion = posicion;
    this.modalTitulo = '';
    this.modalTexto = '';
    this.modalDescripcion = '';
    this.modalImagenPreview = '';
    this.modalCampos = [{ etiqueta: '', tipo: 'texto', placeholder: '' }];
    this.editandoContenidoId = null;
    this.mostrandoSelector.set(false);
    this.modalAbierto.set(tipo);
  }

  abrirSelectorPosicion(posicion: number): void {
    this.modalPosicion = posicion;
    this.modalTitulo = '';
    this.modalTexto = '';
    this.modalDescripcion = '';
    this.modalImagenPreview = '';
    this.modalCampos = [{ etiqueta: '', tipo: 'texto', placeholder: '' }];
    this.editandoContenidoId = null;
    this.mostrandoSelector.set(true);
    this.modalAbierto.set('texto');
  }

  seleccionarTipoModal(tipo: ContenidoTipo): void {
    this.mostrandoSelector.set(false);
    this.modalAbierto.set(tipo);
  }

  textoBotonGuardar(): string {
    if (this.guardando()) return 'Guardando...';
    if (this.editandoContenidoId) return 'Guardar cambios';
    return 'Agregar';
  }

  abrirModalEdicion(contenido: ContenidoLeccion): void {
    this.editandoContenidoId = contenido.id;
    this.modalAbierto.set(contenido.tipo);
    this.modalPosicion = this.contenidoLeccion().findIndex((c) => c.id === contenido.id);

    switch (contenido.tipo) {
      case 'titulo':
        this.modalTexto = contenido.contenido;
        break;
      case 'texto':
        this.modalTexto = contenido.contenido;
        break;
      case 'imagen':
        this.modalImagenPreview = contenido.contenido;
        break;
      case 'tarjeta': {
        const tarjeta = this.parseTarjeta(contenido.contenido);
        this.modalTitulo = tarjeta.titulo;
        this.modalDescripcion = tarjeta.descripcion;
        this.modalImagenPreview = tarjeta.imagen || '';
        break;
      }
      case 'formulario': {
        const form = this.parseFormulario(contenido.contenido);
        this.modalTitulo = form.titulo;
        this.modalCampos = form.campos.length > 0
          ? form.campos.map((c: any) => ({ etiqueta: c.etiqueta, tipo: c.tipo, placeholder: c.placeholder || '' }))
          : [{ etiqueta: '', tipo: 'texto', placeholder: '' }];
        break;
      }
    }
  }

  cerrarModal(): void {
    this.modalAbierto.set(null);
    this.editandoContenidoId = null;
  }

  agregarCampoFormulario(): void {
    this.modalCampos.push({ etiqueta: '', tipo: 'texto', placeholder: '' });
  }

  eliminarCampoFormulario(index: number): void {
    this.modalCampos.splice(index, 1);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.error.set('Por favor selecciona un archivo de imagen valido.');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      this.modalImagenPreview = e.target?.result as string;
    };
    reader.readAsDataURL(file);
  }

  async guardarModal(): Promise<void> {
    const tipo = this.modalAbierto();
    if (!tipo) return;

    let contenido = '';

    switch (tipo) {
      case 'titulo':
        if (!this.modalTexto.trim()) { this.error.set('El titulo es obligatorio.'); return; }
        contenido = this.modalTexto.trim();
        break;
      case 'texto':
        if (!this.modalTexto.trim()) { this.error.set('El texto es obligatorio.'); return; }
        contenido = this.modalTexto.trim();
        break;
      case 'imagen':
        if (!this.modalImagenPreview) { this.error.set('Selecciona una imagen.'); return; }
        contenido = this.modalImagenPreview;
        break;
      case 'tarjeta':
        if (!this.modalTitulo.trim()) { this.error.set('El titulo es obligatorio.'); return; }
        contenido = JSON.stringify({ titulo: this.modalTitulo.trim(), descripcion: this.modalDescripcion.trim(), imagen: this.modalImagenPreview || '' });
        break;
      case 'formulario':
        if (!this.modalTitulo.trim()) { this.error.set('El titulo es obligatorio.'); return; }
        const campos = this.modalCampos.filter((c) => c.etiqueta.trim());
        if (campos.length === 0) { this.error.set('Agrega al menos un campo.'); return; }
        contenido = JSON.stringify({ titulo: this.modalTitulo.trim(), campos });
        break;
    }

    this.guardando.set(true);
    this.error.set('');

    try {
      if (this.editandoContenidoId) {
        const result = await this.leccionesService.actualizarContenido(this.editandoContenidoId, { contenido });
        if (result.error) {
          this.error.set(result.error.message);
          return;
        }
      } else {
        const leccionId = this.leccionExpandida();
        if (!leccionId) return;

        const existente = await this.leccionesService.getWithContenido(leccionId);
        if (existente?.contenido) {
          const bloquesDespues = existente.contenido.filter((c) => c.orden > this.modalPosicion);
          bloquesDespues.sort((a, b) => b.orden - a.orden);
          for (const bloque of bloquesDespues) {
            await this.leccionesService.actualizarContenido(bloque.id, { orden: bloque.orden + 1 });
          }
        }

        const result = await this.leccionesService.agregarContenido(leccionId, {
          tipo,
          contenido,
          orden: this.modalPosicion + 1
        });

        if (result.error) {
          this.error.set(result.error.message);
          return;
        }
      }

      this.cerrarModal();
      const leccionId = this.leccionExpandida();
      if (leccionId) {
        const data = await this.leccionesService.getWithContenido(leccionId);
        const contenido = (data?.contenido || []).sort((a, b) => a.orden - b.orden);
        this.contenidoLeccion.set(contenido);
      }
      this.exito.set(this.editandoContenidoId ? 'Contenido actualizado.' : 'Contenido agregado.');
      setTimeout(() => this.exito.set(''), 3000);
    } finally {
      this.guardando.set(false);
    }
  }

  async eliminarContenido(contenido: ContenidoLeccion): Promise<void> {
    this.guardando.set(true);
    try {
      await this.leccionesService.eliminarContenido(contenido.id);
      const leccionId = this.leccionExpandida();
      if (leccionId) {
        const data = await this.leccionesService.getWithContenido(leccionId);
        if (data?.contenido) {
          const sorted = [...data.contenido].sort((a, b) => a.orden - b.orden);
          for (let i = 0; i < sorted.length; i++) {
            if (sorted[i].orden !== i + 1) {
              await this.leccionesService.actualizarContenido(sorted[i].id, { orden: i + 1 });
            }
          }
        }
        const finalData = await this.leccionesService.getWithContenido(leccionId);
        this.contenidoLeccion.set((finalData?.contenido || []).sort((a, b) => a.orden - b.orden));
      }
      this.exito.set('Bloque eliminado.');
      setTimeout(() => this.exito.set(''), 3000);
    } finally {
      this.guardando.set(false);
      this.contenidoEliminar.set(null);
    }
  }

  confirmarEliminar(contenido: ContenidoLeccion): void {
    this.contenidoEliminar.set(contenido);
  }

  cancelarEliminar(): void {
    this.contenidoEliminar.set(null);
  }

  getTipoEtiqueta(tipo: ContenidoTipo): string {
    return this.tiposContenido.find((t) => t.valor === tipo)?.etiqueta || tipo;
  }

  getTipoIcono(tipo: ContenidoTipo): string {
    return this.tiposContenido.find((t) => t.valor === tipo)?.icono || '?';
  }

  getTarjetaTitulo(contenido: string): string { return this.parseTarjeta(contenido).titulo; }
  getTarjetaDescripcion(contenido: string): string { return this.parseTarjeta(contenido).descripcion; }
  getTarjetaImagen(contenido: string): string { return this.parseTarjeta(contenido).imagen || ''; }
  getFormTitulo(contenido: string): string { return this.parseFormulario(contenido).titulo; }
  getFormCamposCount(contenido: string): number { return this.parseFormulario(contenido).campos.length; }

  parseTarjeta(contenido: string): { titulo: string; descripcion: string; imagen?: string } {
    try {
      const parsed = JSON.parse(contenido);
      return { titulo: parsed.titulo || '', descripcion: parsed.descripcion || '', imagen: parsed.imagen || '' };
    } catch { return { titulo: '', descripcion: contenido, imagen: '' }; }
  }

  parseFormulario(contenido: string): { titulo: string; campos: any[] } {
    try { return JSON.parse(contenido); } catch { return { titulo: '', campos: [] }; }
  }
}
