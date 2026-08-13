import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { WidgetSmartTv } from '../models/widget-smart-tv.model';
import { WidgetWearable } from '../models/widget-wearable.model';

@Injectable({ providedIn: 'root' })
export class WidgetService {
  private readonly http = inject(HttpClient);

  getSmartTvWidget(): Observable<WidgetSmartTv> {
    return this.http.get<WidgetSmartTv>('/api/widget/smart-tv');
  }

  getWearableWidget(): Observable<WidgetWearable> {
    return this.http.get<WidgetWearable>('/api/widget/wearable');
  }

  updateWearableWidget(widget: Partial<WidgetWearable>): Observable<WidgetWearable> {
    return this.http.put<WidgetWearable>('/api/widget/wearable', widget);
  }
}
