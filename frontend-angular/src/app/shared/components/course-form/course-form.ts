import { Component, effect, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Course, courseImagePath } from '../../../core/data/academy-data';
import { Profile } from '../../../core/services/auth.service';
import { CursoDb, CursoInput, CATEGORIAS, NIVELES, CursoEstado } from '../../../core/models/curso.model';

@Component({
  selector: 'app-course-form',
  imports: [ReactiveFormsModule],
  templateUrl: './course-form.html',
  styleUrl: './course-form.css'
})
export class CourseForm {
  readonly curso = input<CursoDb | null>(null);
  readonly defaultInstructor = input('SkillAcademy');
  readonly lockedInstructorId = input<string | null>(null);
  readonly lockedInstructorName = input('');
  readonly lockInstructor = input(false);
  readonly professors = input<Profile[]>([]);
  readonly categorias = input<string[]>(CATEGORIAS);
  readonly save = output<CursoInput>();
  readonly close = output<void>();

  private fb = new FormBuilder();

  form = this.fb.nonNullable.group({
    titulo: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    categoria: ['Programacion', [Validators.required, Validators.maxLength(50)]],
    nivel: ['Intermedio', [Validators.required, Validators.maxLength(20)]],
    instructor: ['SkillAcademy', [Validators.required, Validators.maxLength(100)]],
    instructor_id: [null as string | null],
    precio: [0, [Validators.required, Validators.min(0)]],
    old_precio: [null as number | null],
    imagen: ['code', [Validators.required, Validators.maxLength(200)]],
    descripcion: ['', [Validators.maxLength(500)]],
    estado: ['Borrador', [Validators.required, Validators.maxLength(20)]]
  });

  readonly niveles = NIVELES;
  readonly estados: CursoEstado[] = ['Publicado', 'Borrador', 'Archivado'];

  constructor() {
    effect(() => {
      const course = this.curso();
      const lockedName = this.lockedInstructorName() || this.defaultInstructor();
      const lockedId = this.lockedInstructorId();
      const instructorIdControl = this.form.controls.instructor_id;

      if (this.lockInstructor()) {
        instructorIdControl.clearValidators();
      } else {
        instructorIdControl.setValidators(Validators.required);
      }
      instructorIdControl.updateValueAndValidity({ emitEvent: false });

      if (course) {
        this.form.patchValue({
          titulo: course.titulo,
          categoria: course.categoria,
          nivel: course.nivel,
          instructor: this.lockInstructor() ? lockedName : course.instructor,
          instructor_id: this.lockInstructor() ? lockedId : course.instructor_id,
          precio: Number(course.precio),
          old_precio: course.old_precio != null ? Number(course.old_precio) : null,
          imagen: course.imagen,
          descripcion: course.descripcion ?? '',
          estado: course.estado
        });
      } else {
        this.form.reset({
          titulo: '',
          categoria: 'Programacion',
          nivel: 'Intermedio',
          instructor: this.lockInstructor() ? lockedName : this.defaultInstructor(),
          instructor_id: this.lockInstructor() ? lockedId : null,
          precio: 0,
          old_precio: null,
          imagen: 'code',
          descripcion: '',
          estado: 'Borrador'
        });
      }
    });
  }

  instructorLabel(profile: Profile): string {
    return profile.full_name || profile.email || 'Profesor';
  }

  imagePreview(): string {
    return courseImagePath(this.form.controls.imagen.value as Course['image']);
  }

  onImageFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.form.controls.imagen.setErrors({ image: true });
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        this.form.controls.imagen.setValue(reader.result);
        this.form.controls.imagen.updateValueAndValidity();
      }
    };
    reader.readAsDataURL(file);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const selectedProfessor = this.professors().find((professor) => professor.id === value.instructor_id);
    const lockedName = this.lockedInstructorName() || this.defaultInstructor();
    const instructorId = this.lockInstructor() ? this.lockedInstructorId() : value.instructor_id || null;
    const instructor = this.lockInstructor()
      ? lockedName
      : selectedProfessor
        ? this.instructorLabel(selectedProfessor)
        : value.instructor;

    this.save.emit({
      titulo: value.titulo,
      categoria: value.categoria,
      nivel: value.nivel,
      instructor,
      instructor_id: instructorId,
      precio: Number(value.precio),
      old_precio: value.old_precio != null ? Number(value.old_precio) : null,
      imagen: value.imagen,
      descripcion: value.descripcion,
      estado: value.estado as CursoEstado
    });
  }
}
