import { Component, inject, signal, PLATFORM_ID, computed } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PublicHeader } from '../../shared/components/public-header/public-header';
import { CourseCard } from '../../shared/components/course-card/course-card';
import { AuthService, Profile } from '../../core/services/auth.service';
import { CursosService } from '../../core/services/cursos.service';
import { Course } from '../../core/data/academy-data';

@Component({
  selector: 'app-instructor-profile',
  standalone: true,
  imports: [PublicHeader, CourseCard, RouterLink],
  templateUrl: './instructor-profile.html',
  styleUrl: './instructor-profile.css'
})
export class InstructorProfilePage {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly cursos = inject(CursosService);
  private readonly platformId = inject(PLATFORM_ID);

  private readonly id = String(this.route.snapshot.paramMap.get('id') ?? '');

  readonly perfil = signal<Profile | null>(null);
  readonly courses = signal<Course[]>([]);
  readonly cargando = signal(true);
  readonly noExiste = signal(false);
  readonly ratings = signal<Record<number, number>>({});

  readonly profesorRating = computed(() => {
    const ratings = this.ratings();
    const ids = Object.keys(ratings);
    if (ids.length === 0) return 0;
    const sum = ids.reduce((acc, id) => acc + ratings[+id], 0);
    return sum / ids.length;
  });

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      void this.cargar();
    } else {
      this.cargando.set(false);
    }
  }

  private async cargar(): Promise<void> {
    const client = this.auth.getSupabase();
    if (!client || !this.id) {
      this.noExiste.set(true);
      this.cargando.set(false);
      return;
    }

    const { data, error } = await client.from('profiles').select('*').eq('id', this.id).single();

    if (error || !data) {
      this.noExiste.set(true);
      this.cargando.set(false);
      return;
    }

    this.perfil.set(data as Profile);
    const cursos = await this.cursos.listPublicosDe(this.id);
    this.courses.set(cursos);

    const ratingsMap: Record<number, number> = {};
    for (const curso of cursos) {
      const { data: reviews } = await client
        .from('curso_reviews')
        .select('rating')
        .eq('curso_id', curso.id);
      if (reviews && reviews.length > 0) {
        const sum = reviews.reduce((acc: number, r: any) => acc + r.rating, 0);
        ratingsMap[curso.id] = sum / reviews.length;
      }
    }
    this.ratings.set(ratingsMap);
    this.cargando.set(false);
  }

  avatarFallback(): string {
    const name = this.perfil()?.full_name || this.perfil()?.email || 'I';
    const initial = name.charAt(0).toUpperCase();
    const svg =
      `<svg xmlns="http://www.w3.org/2000/svg" width="180" height="180">` +
      `<rect width="180" height="180" rx="90" fill="#101923"/>` +
      `<text x="90" y="118" font-size="72" fill="#50d67a" text-anchor="middle" font-family="Arial" font-weight="bold">${initial}</text>` +
      `</svg>`;
    return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
  }
}
