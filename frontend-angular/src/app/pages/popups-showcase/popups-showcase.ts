import { isPlatformBrowser } from '@angular/common';
import { Component, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BannerRow, bannerImagePath } from '../../core/data/academy-data';
import { PopupDb, PopupsService } from '../../core/services/popups.service';
import { PublicHeader } from '../../shared/components/public-header/public-header';

@Component({
  selector: 'app-popups-showcase',
  imports: [RouterLink, PublicHeader],
  templateUrl: './popups-showcase.html',
  styleUrl: './popups-showcase.css'
})
export class PopupsShowcase {
  private readonly popupsService = inject(PopupsService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly popups = signal<PopupDb[]>([]);
  readonly closedIds = signal<number[]>([]);
  readonly cargando = signal(true);
  readonly visibles = computed(() => this.popups().filter((popup) => popup.activo && !this.closedIds().includes(popup.id)));

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    }
  }

  async cargar(): Promise<void> {
    this.cargando.set(true);
    this.popups.set(await this.popupsService.listPopups());
    this.cargando.set(false);
  }

  cerrar(id: number): void {
    this.closedIds.set([...this.closedIds(), id]);
  }

  imageSrc(imagen: string): string {
    return bannerImagePath(imagen as BannerRow['image']);
  }
}
