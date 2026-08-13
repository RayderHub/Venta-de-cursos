import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { CursosService } from '../services/cursos.service';
import { InscripcionesService } from '../services/inscripciones.service';

export const authGuard: CanActivateFn = async (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await auth.waitUntilReady();
  
  if (auth.isAuthenticated() && auth.profile()?.is_active !== false) {
    return true;
  }

  if (auth.isAuthenticated()) {
    await auth.signOut();
  }
  
  return router.createUrlTree(['/login'], { 
    queryParams: { returnUrl: state.url } 
  });
};

export const adminGuard: CanActivateFn = async (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await auth.waitUntilReady();
  
  if (auth.isAuthenticated() && auth.profile()?.is_active !== false && auth.isAdmin()) {
    return true;
  }
  
  return router.parseUrl(auth.getHomeRouteForRole());
};

export const teacherGuard: CanActivateFn = async (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await auth.waitUntilReady();

  if (auth.isAuthenticated() && auth.profile()?.is_active !== false && auth.isTeacher()) {
    return true;
  }

  return router.parseUrl(auth.getHomeRouteForRole());
};

export const studentGuard: CanActivateFn = async (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await auth.waitUntilReady();

  if (auth.isAuthenticated() && auth.profile()?.is_active !== false && auth.userRole() === 'student') {
    return true;
  }

  return router.parseUrl(auth.getHomeRouteForRole());
};

export const guestGuard: CanActivateFn = async (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await auth.waitUntilReady();
  
  if (!auth.isAuthenticated()) {
    return true;
  }
  
  return router.parseUrl(auth.getHomeRouteForRole());
};

export const cursoAccesoGuard: CanActivateFn = async (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const cursos = inject(CursosService);
  const inscripciones = inject(InscripcionesService);

  await auth.waitUntilReady();

  const cursoId = Number(route.paramMap.get('id'));
  const loginUrl = router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url }
  });

  if (!auth.isAuthenticated() || auth.profile()?.is_active === false) {
    if (auth.isAuthenticated()) {
      await auth.signOut();
    }
    return loginUrl;
  }

  const user = auth.user();
  const curso = cursoId ? await cursos.get(cursoId) : null;
  if (!user || !curso) {
    return router.parseUrl('/catalogo');
  }

  if (auth.isAdmin()) return true;

  const esInstructor = curso.instructorId && String(curso.instructorId) === user.id;
  if (esInstructor) return true;

  const inscrito = await inscripciones.estaInscrito(cursoId);
  if (inscrito) return true;

  return router.parseUrl(`/curso/${cursoId}`);
};
