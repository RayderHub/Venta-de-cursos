import { isPlatformBrowser } from '@angular/common';
import { Component, PLATFORM_ID, inject, signal } from '@angular/core';
import { WidgetSmartTv } from '../../core/models/widget-smart-tv.model';
import { WidgetService } from '../../core/services/widget.service';

@Component({
  selector: 'app-smart-tv-widget',
  templateUrl: './smart-tv-widget.html',
  styleUrl: './smart-tv-widget.css'
})
export class SmartTvWidget {
  private readonly widgetService = inject(WidgetService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly widget = signal<WidgetSmartTv | null>(null);
  readonly error = signal('');

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.widgetService.getSmartTvWidget().subscribe({
        next: (widget) => this.widget.set(widget),
        error: () => this.error.set('No se pudo cargar el widget de Smart TV. Revisa que el backend Fastify este encendido.')
      });
    }
  }
}
