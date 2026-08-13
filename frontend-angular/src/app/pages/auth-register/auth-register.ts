import { Component, signal, computed, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-auth-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './auth-register.html',
  styleUrl: './auth-register.css'
})
export class AuthRegister {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(45)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]],
    terms: [false, Validators.requiredTrue]
  }, { validators: this.passwordMatchValidator });

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

  readonly fullNameError = computed(() => {
    const control = this.form.controls.fullName;
    if (control.hasError('required')) return 'Ingresa tu nombre completo';
    if (control.hasError('minlength')) return 'Minimo 3 caracteres';
    if (control.hasError('maxlength')) return 'Maximo 45 caracteres';
    return '';
  });

  private passwordMatchValidator(control: AbstractControl): Record<string, boolean> | null {
    const password = control.get('password')?.value;
    const confirm = control.get('confirmPassword')?.value;
    return password === confirm ? null : { passwordMismatch: true };
  }

  async onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    const { email, password, fullName } = this.form.getRawValue();
    const { data, error } = await this.auth.signUp(email, password, fullName);

    this.loading.set(false);

    if (error) {
      this.error.set(this.getErrorMessage(error.message));
      return;
    }

    if (data?.session) {
      const profile = data.user ? await this.auth.loadProfile(data.user.id) : null;
      await this.router.navigateByUrl(this.auth.getHomeRouteForRole(profile?.role_id));
    } else {
      this.success.set('Cuenta creada. Revisa tu email para confirmar la cuenta antes de iniciar sesión.');
      this.form.reset();
    }
  }

  private getErrorMessage(msg: string): string {
    if (msg.includes('already been registered')) return 'Este email ya está registrado. Intenta iniciar sesión.';
    if (msg.includes('Password should be')) return 'La contraseña debe tener al menos 6 caracteres';
    return 'Error al crear la cuenta. Intenta de nuevo.';
  }
}
