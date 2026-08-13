import { Component, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BannerRow, bannerImagePath } from '../../core/data/academy-data';
import { BannerDb, BannersService } from '../../core/services/banners.service';
import { PublicHeader } from '../../shared/components/public-header/public-header';

@Component({
  selector: 'app-pricing',
  imports: [PublicHeader, RouterLink],
  templateUrl: './pricing.html',
  styleUrl: './pricing.css'
})
export class Pricing {
  private readonly bannersService = inject(BannersService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly promociones = signal<BannerDb[]>([]);
  readonly cargando = signal(true);
  readonly selected = signal<BannerDb | null>(null);

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    } else {
      this.cargando.set(false);
    }
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    const banners = await this.bannersService.list();
    this.promociones.set(banners.filter((banner) => banner.estado === 'Activo'));
    this.cargando.set(false);
  }

  imageSrc(imagen: string): string {
    return bannerImagePath(imagen as BannerRow['image']);
  }

  open(promo: BannerDb): void {
    this.selected.set(promo);
  }

  close(): void {
    this.selected.set(null);
  }
}
