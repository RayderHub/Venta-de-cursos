import { Component, signal, computed, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-auth-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './auth-login.html',
  styleUrl: './auth-login.css'
})
export class AuthLogin {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly returnUrl = this.route.snapshot.queryParams['returnUrl'] || '';

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    remember: [false]
  });

  readonly emailError = computed(() => {
    const control = this.form.controls.email;
    if (control.hasError('required')) return 'Email es requerido';
    if (control.hasError('email')) return 'Email inválido';
    return '';
  });

  readonly passwordError = computed(() => {
    const control = this.form.controls.password;
    if (control.hasError('required')) return 'Contraseña es requerida';
    if (control.hasError('minlength')) return 'Mínimo 6 caracteres';
    return '';
  });

  async onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    const { email, password } = this.form.getRawValue();
    const { data, error } = await this.auth.signIn(email, password);

    this.loading.set(false);

    if (error) {
      this.error.set(this.getErrorMessage(error.message));
    } else {
      const profile = data?.user ? await this.auth.loadProfile(data.user.id) : null;
      if (profile?.is_active === false) {
        await this.auth.signOut();
        this.error.set('Tu cuenta esta inactiva. Contacta al administrador.');
        return;
      }
      const roleRoute = this.auth.getHomeRouteForRole(profile?.role_id);
      await this.router.navigateByUrl(this.safeReturnUrlForRole(profile?.role_id) || roleRoute);
    }
  }

  private safeReturnUrlForRole(roleId: string | null | undefined): string {
    if (!this.returnUrl.startsWith('/') || this.returnUrl.startsWith('//')) return '';
    if (roleId === 'admin') return this.returnUrl.startsWith('/admin') ? this.returnUrl : '';
    if (roleId === 'teacher') return this.returnUrl.startsWith('/profesor') ? this.returnUrl : '';
    return this.returnUrl.startsWith('/usuario')
      || this.returnUrl.startsWith('/curso/')
      || this.returnUrl === '/carrito'
      || this.returnUrl === '/perfil'
      ? this.returnUrl
      : '';
  }

  private getErrorMessage(msg: string): string {
    if (msg.includes('Invalid login credentials')) return 'Email o contraseña incorrectos';
    if (msg.includes('Email not confirmed')) return 'Confirma tu email antes de entrar';
    return 'Error al iniciar sesión. Intenta de nuevo.';
  }
}
