import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Course, courseImagePath } from '../../core/data/academy-data';
import { AuthService } from '../../core/services/auth.service';
import { CarritoService } from '../../core/services/carrito.service';
import { InscripcionesService } from '../../core/services/inscripciones.service';
import { PopupsService } from '../../core/services/popups.service';

@Component({
  selector: 'app-cart',
  imports: [RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.css'
})
export class Cart {
  readonly carrito = inject(CarritoService);
  private readonly auth = inject(AuthService);
  private readonly inscripciones = inject(InscripcionesService);
  private readonly popups = inject(PopupsService);
  private readonly router = inject(Router);

  readonly couponCode = signal('');
  readonly discount = signal(0);
  readonly couponMessage = signal('');
  readonly checkoutMessage = signal('');
  readonly error = signal('');
  readonly processing = signal(false);

  readonly discountAmount = computed(() => this.carrito.total() * (this.discount() / 100));
  readonly total = computed(() => Math.max(this.carrito.total() - this.discountAmount(), 0));

  imageSrc(course: Course): string {
    return courseImagePath(course.image);
  }

  eliminar(cursoId: number): void {
    this.carrito.remove(cursoId);
  }

  vaciar(): void {
    this.carrito.clear();
    this.discount.set(0);
    this.couponCode.set('');
    this.couponMessage.set('');
  }

  async aplicarCupon(): Promise<void> {
    this.error.set('');
    this.couponMessage.set('');
    const cupon = await this.popups.buscarCupon(this.couponCode());

    if (!cupon) {
      this.discount.set(0);
      this.couponMessage.set('Cupon no valido o inactivo.');
      return;
    }

    this.discount.set(Number(cupon.descuento));
    this.couponCode.set(cupon.codigo);
    this.couponMessage.set(`Cupon ${cupon.codigo} aplicado: ${cupon.descuento}% de descuento.`);
  }

  async checkout(): Promise<void> {
    this.error.set('');
    this.checkoutMessage.set('');

    if (this.carrito.items().length === 0) return;

    if (!this.auth.isAuthenticated()) {
      await this.router.navigate(['/login'], { queryParams: { returnUrl: '/carrito' } });
      return;
    }

    this.processing.set(true);
    const ids = this.carrito.items().map((course) => course.id);
    const { error } = await this.inscripciones.inscribirVarios(ids);
    this.processing.set(false);

    if (error) {
      this.error.set(error.message);
      return;
    }

    this.carrito.clear();
    this.checkoutMessage.set('Compra simulada completada. Tus cursos ya estan en el dashboard.');
    await this.router.navigate(['/usuario/dashboard']);
  }
}
