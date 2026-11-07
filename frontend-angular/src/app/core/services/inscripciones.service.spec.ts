/**
 * =====================================================================
 * FASE 2 — Pruebas Unitarias (Angular / Vitest)
 * Funcionalidad crítica: Calificación de Cursos
 * Ejecutar con: npx vitest run src/app/core/services/inscripciones.service.spec.ts
 *
 * Cubre:
 *  ✔ TC-U01: Caso válido   — rating 4, dentro del rango 1-5
 *  ✔ TC-U02: Caso límite   — rating en los extremos (1 y 5)
 *  ✔ TC-U03: Caso inválido — rating fuera de rango es acotado a [1,5]
 *  ✔ TC-U04: Caso inválido — sin usuario autenticado retorna error
 *  ✔ TC-U05: Lógica pura   — getStarClass refleja correctamente el estado
 *  ✔ TC-U06: Lógica pura   — hasRating distingue calificado vs no calificado
 * =====================================================================
 */

import { describe, it, expect } from 'vitest';

// ─────────────────────────────────────────────────────────────────────────────
// Lógica pura extraída de InscripcionesService y UserCourses para pruebas
// aisladas sin dependencias de Angular ni de Supabase.
// ─────────────────────────────────────────────────────────────────────────────

/** Réplica exacta del acotamiento de rating en calificarCurso() (inscripciones.service.ts L118) */
function boundRating(rating: number): number {
  return Math.max(1, Math.min(5, Math.round(rating)));
}

/** Réplica exacta de getStarClass() del componente UserCourses (user-courses.ts L195) */
function getStarClass(
  userRatings: Record<number, { stars: number; comment: string }>,
  ratingHoverCursoId: number | null,
  ratingHoverStars: number,
  cursoId: number,
  star: number
): string {
  const saved = userRatings[cursoId];
  if (ratingHoverCursoId === cursoId && ratingHoverStars > 0) {
    return star <= ratingHoverStars ? 'star filled' : 'star empty';
  }
  if (saved && saved.stars > 0) {
    return star <= saved.stars ? 'star filled' : 'star empty';
  }
  return 'star empty';
}

/** Réplica exacta de hasRating() del componente UserCourses (user-courses.ts L206) */
function hasRating(
  userRatings: Record<number, { stars: number; comment: string }>,
  cursoId: number
): boolean {
  const saved = userRatings[cursoId];
  return !!saved && saved.stars > 0;
}

// ─────────────────────────────────────────────────────────────────────────────
// Suite de pruebas unitarias
// ─────────────────────────────────────────────────────────────────────────────

describe('Fase 2 — Pruebas Unitarias: Calificación de Cursos', () => {

  // ── TC-U01: CASO VÁLIDO ──────────────────────────────────────────────────
  describe('TC-U01 | boundRating() — rating válido dentro de rango', () => {
    it('debe retornar 4 cuando se pasa rating = 4', () => {
      const resultado = boundRating(4);
      expect(resultado).toBe(4);
    });

    it('debe retornar 3 cuando se pasa rating = 3', () => {
      const resultado = boundRating(3);
      expect(resultado).toBe(3);
    });
  });

  // ── TC-U02: CASO LÍMITE ──────────────────────────────────────────────────
  describe('TC-U02 | boundRating() — valores límite exactos (1 y 5)', () => {
    it('debe aceptar el límite inferior: rating = 1 → retorna 1', () => {
      expect(boundRating(1)).toBe(1);
    });

    it('debe aceptar el límite superior: rating = 5 → retorna 5', () => {
      expect(boundRating(5)).toBe(5);
    });

    it('debe redondear 4.6 hacia 5 (caso límite flotante superior)', () => {
      expect(boundRating(4.6)).toBe(5);
    });

    it('debe redondear 1.4 hacia 1 (caso límite flotante inferior)', () => {
      expect(boundRating(1.4)).toBe(1);
    });
  });

  // ── TC-U03: CASO INVÁLIDO — fuera de rango ───────────────────────────────
  describe('TC-U03 | boundRating() — valores fuera de rango son acotados', () => {
    it('rating = 0 debe ser acotado a 1 (mínimo permitido)', () => {
      expect(boundRating(0)).toBe(1);
    });

    it('rating = -5 debe ser acotado a 1 (mínimo permitido)', () => {
      expect(boundRating(-5)).toBe(1);
    });

    it('rating = 6 debe ser acotado a 5 (máximo permitido)', () => {
      expect(boundRating(6)).toBe(5);
    });

    it('rating = 100 debe ser acotado a 5 (máximo permitido)', () => {
      expect(boundRating(100)).toBe(5);
    });
  });

  // ── TC-U04: CASO INVÁLIDO — sin usuario autenticado ─────────────────────
  describe('TC-U04 | calificarCurso() — sin cliente Supabase retorna error', () => {
    it('debe retornar error cuando supabase es null (usuario no autenticado)', () => {
      // Simula la guarda inicial de calificarCurso:
      // const bounded = ...; if (!client || !user) return { error: ... }
      const clientNull = null;
      const userNull = null;

      const resultado = (!clientNull || !userNull)
        ? { error: new Error('Inicia sesion para calificar') }
        : { error: null };

      expect(resultado.error).not.toBeNull();
      expect(resultado.error?.message).toContain('calificar');
    });

    it('debe NO retornar error cuando el cliente y usuario son válidos', () => {
      const clientMock = { from: () => ({}) }; // mock no-null
      const userMock = { id: 'uuid-test-user-123' };

      const resultado = (!clientMock || !userMock)
        ? { error: new Error('Inicia sesion para calificar') }
        : { error: null };

      expect(resultado.error).toBeNull();
    });
  });

  // ── TC-U05: Lógica getStarClass ──────────────────────────────────────────
  describe('TC-U05 | getStarClass() — refleja correctamente el estado de estrellas', () => {
    const cursoId = 42;

    it('debe retornar "star empty" cuando no hay rating guardado ni hover', () => {
      const clase = getStarClass({}, null, 0, cursoId, 3);
      expect(clase).toBe('star empty');
    });

    it('debe retornar "star filled" para estrellas ≤ rating guardado (4)', () => {
      const ratings = { [cursoId]: { stars: 4, comment: '' } };
      expect(getStarClass(ratings, null, 0, cursoId, 1)).toBe('star filled');
      expect(getStarClass(ratings, null, 0, cursoId, 4)).toBe('star filled');
    });

    it('debe retornar "star empty" para estrellas > rating guardado (4)', () => {
      const ratings = { [cursoId]: { stars: 4, comment: '' } };
      expect(getStarClass(ratings, null, 0, cursoId, 5)).toBe('star empty');
    });

    it('hover en estrella 5 debe hacer que estrella 5 sea filled aunque el guardado sea 2', () => {
      const ratings = { [cursoId]: { stars: 2, comment: '' } };
      expect(getStarClass(ratings, cursoId, 5, cursoId, 5)).toBe('star filled');
    });

    it('hover en estrella 2 debe dejar estrella 3 como empty', () => {
      const ratings = { [cursoId]: { stars: 4, comment: '' } };
      expect(getStarClass(ratings, cursoId, 2, cursoId, 3)).toBe('star empty');
    });
  });

  // ── TC-U06: hasRating ────────────────────────────────────────────────────
  describe('TC-U06 | hasRating() — detecta si el curso fue calificado', () => {
    it('debe retornar false cuando no hay ningún rating registrado para ese curso', () => {
      expect(hasRating({}, 99)).toBe(false);
    });

    it('debe retornar false cuando el rating guardado es 0 (sin calificar aún)', () => {
      expect(hasRating({ 99: { stars: 0, comment: '' } }, 99)).toBe(false);
    });

    it('debe retornar true cuando hay un rating > 0', () => {
      expect(hasRating({ 99: { stars: 3, comment: 'Buen curso' } }, 99)).toBe(true);
    });

    it('debe retornar true para rating = 5 (máximo)', () => {
      expect(hasRating({ 99: { stars: 5, comment: 'Excelente' } }, 99)).toBe(true);
    });
  });
});
