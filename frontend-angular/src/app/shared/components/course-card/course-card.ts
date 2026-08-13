import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Course, courseImagePath } from '../../../core/data/academy-data';
import { CarritoService } from '../../../core/services/carrito.service';

@Component({
  selector: 'app-course-card',
  imports: [RouterLink],
  templateUrl: './course-card.html',
  styleUrl: './course-card.css'
})
export class CourseCard {
  readonly course = input.required<Course>();
  readonly compact = input(false);
  readonly carrito = inject(CarritoService);

  imageSrc(): string {
    return courseImagePath(this.course().image);
  }

  agregarAlCarrito(): void {
    this.carrito.add(this.course());
  }
}
