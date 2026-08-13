import { isPlatformBrowser } from '@angular/common';
import { Component, PLATFORM_ID, inject, signal } from '@angular/core';
import { WidgetWearable } from '../../core/models/widget-wearable.model';
import { WidgetService } from '../../core/services/widget.service';

@Component({
  selector: 'app-wearable-widget',
  templateUrl: './wearable-widget.html',
  styleUrl: './wearable-widget.css'
})
export class WearableWidget {
  private readonly widgetService = inject(WidgetService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly widget = signal<WidgetWearable | null>(null);
  readonly error = signal('');

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.widgetService.getWearableWidget().subscribe({
        next: (widget) => this.widget.set(widget),
        error: () => this.error.set('No se pudo cargar el widget wearable. Revisa que el backend Fastify este encendido.')
      });
    }
  }
}
