import { Injectable, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID, inject } from '@angular/core';
import { Course } from '../data/academy-data';

const STORAGE_KEY = 'skillacademy_carrito';

@Injectable({ providedIn: 'root' })
export class CarritoService {
  private platformId = inject(PLATFORM_ID);
  readonly items = signal<Course[]>([]);

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.load();
    }
  }

  private load(): void {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) this.items.set(JSON.parse(raw));
    } catch {
      this.items.set([]);
    }
  }

  private save(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.items()));
    } catch {
      /* storage no disponible */
    }
  }

  readonly total = () => this.items().reduce((acc, item) => acc + item.price, 0);

  add(course: Course): void {
    if (this.items().some((item) => item.id === course.id)) return;
    this.items.set([...this.items(), course]);
    this.save();
  }

  remove(courseId: number): void {
    this.items.set(this.items().filter((item) => item.id !== courseId));
    this.save();
  }

  clear(): void {
    this.items.set([]);
    this.save();
  }

  has(courseId: number): boolean {
    return this.items().some((item) => item.id === courseId);
  }
}
