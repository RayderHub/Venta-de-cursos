import { Component, effect, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TareaInput, TareaDb } from '../../../core/services/tareas.service';

@Component({
  selector: 'app-tarea-form',
  imports: [ReactiveFormsModule],
  templateUrl: './tarea-form.html',
  styleUrl: './tarea-form.css'
})
export class TareaForm {
  readonly tarea = input<TareaDb | null>(null);
  readonly cursos = input<{ id: number; titulo: string }[]>([]);
  readonly guardando = input(false);
  readonly save = output<TareaInput>();
  readonly close = output<void>();

  private fb = new FormBuilder();

  form = this.fb.nonNullable.group({
    titulo: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    curso_id: [null as number | null, Validators.required],
    fecha: [new Date().toISOString().slice(0, 10), Validators.required],
    prioridad: ['Media', Validators.required],
    estado: ['Pendiente', Validators.required],
    progreso: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
    porcentaje_aprobacion: [60, [Validators.required, Validators.min(1), Validators.max(100)]]
  });

  readonly prioridades = ['Alta', 'Media', 'Baja'];
  readonly estados = ['Pendiente', 'En progreso', 'Completada'];

  constructor() {
    effect(() => {
      const tarea = this.tarea();
      if (tarea) {
        this.form.patchValue({
          titulo: tarea.titulo,
          curso_id: tarea.curso_id,
          fecha: tarea.fecha,
          prioridad: tarea.prioridad,
          estado: tarea.estado,
          progreso: tarea.progreso,
          porcentaje_aprobacion: tarea.porcentaje_aprobacion ?? 60
        });
      } else {
        this.form.reset({
          titulo: '',
          curso_id: null,
          fecha: new Date().toISOString().slice(0, 10),
          prioridad: 'Media',
          estado: 'Pendiente',
          progreso: 0,
          porcentaje_aprobacion: 60
        });
      }
    });
  }

  cursoTitulo(): string {
    const cursoId = this.form.controls.curso_id.value;
    const curso = this.cursos().find((item) => item.id === cursoId);
    return curso?.titulo ?? '';
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.save.emit({
      titulo: value.titulo.trim(),
      curso_id: value.curso_id,
      curso: this.cursoTitulo(),
      fecha: value.fecha,
      prioridad: value.prioridad as TareaInput['prioridad'],
      estado: value.estado as TareaInput['estado'],
      progreso: Number(value.progreso),
      porcentaje_aprobacion: Number(value.porcentaje_aprobacion)
    });
  }
}
