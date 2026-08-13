import { Component, computed, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BannerRow, Course, bannerImagePath } from '../../core/data/academy-data';
import { BannerDb, BannersService } from '../../core/services/banners.service';
import { CursosService } from '../../core/services/cursos.service';
import { CourseCard } from '../../shared/components/course-card/course-card';
import { PublicHeader } from '../../shared/components/public-header/public-header';
import { PublicFooter } from '../../shared/components/public-footer/public-footer';

@Component({
  selector: 'app-home',
  imports: [PublicHeader, PublicFooter, CourseCard, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {
  private readonly cursos = inject(CursosService);
  private readonly banners = inject(BannersService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly courses = signal<Course[]>([]);
  readonly promos = signal<BannerDb[]>([]);
  readonly promoIndex = signal(0);
  readonly cargando = signal(true);
  readonly featured = computed(() => this.courses().slice(0, 4));
  readonly bestSellers = computed(() => this.courses().slice(2, 8));
  readonly activePromo = computed(() => {
    const promos = this.promos();
    return promos.length > 0 ? promos[this.promoIndex() % promos.length] : null;
  });

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    }
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    const [data, promos] = await Promise.all([this.cursos.listPublicos(), this.banners.list()]);
    if (data.length > 0) this.courses.set(data);
    this.promos.set(promos.filter((promo) => promo.estado === 'Activo'));
    this.cargando.set(false);
  }

  promoImage(imagen: string): string {
    return bannerImagePath(imagen as BannerRow['image']);
  }

  movePromo(direction: -1 | 1): void {
    const total = this.promos().length;
    if (total === 0) return;
    this.promoIndex.set((this.promoIndex() + direction + total) % total);
  }
}
