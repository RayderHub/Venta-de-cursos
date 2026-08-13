import { Component, inject, computed } from '@angular/core';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../core/services/carrito.service';

@Component({
  selector: 'app-public-header',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './public-header.html',
  styleUrl: './public-header.css'
})
export class PublicHeader {
  readonly auth = inject(AuthService);
  readonly carrito = inject(CarritoService);
  private router = inject(Router);

  readonly spaceLink = computed(() => {
    return this.auth.getHomeRouteForRole();
  });

  readonly roleLabel = computed(() => {
    const role = this.auth.userRole();
    if (role === 'admin') return 'Administrador';
    if (role === 'teacher') return 'Profesor';
    return 'Estudiante';
  });

  async onLogout() {
    await this.auth.signOut();
    await this.router.navigate(['/home']);
  }

  async goToSpace(): Promise<void> {
    const user = this.auth.user();
    let role = this.auth.profile()?.role_id;

    if (user && !role) {
      const profile = await this.auth.loadProfile(user.id);
      role = profile?.role_id;
    }

    await this.router.navigateByUrl(this.auth.getHomeRouteForRole(role));
  }
}
