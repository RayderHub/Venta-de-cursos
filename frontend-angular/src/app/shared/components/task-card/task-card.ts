import { Component, input } from '@angular/core';
import { Tarea } from '../../../core/models/tarea.model';

@Component({
  selector: 'app-task-card',
  templateUrl: './task-card.html',
  styleUrl: './task-card.css'
})
export class TaskCard {
  readonly tarea = input.required<Tarea>();
}
