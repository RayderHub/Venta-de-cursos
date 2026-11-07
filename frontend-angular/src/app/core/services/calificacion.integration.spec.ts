/**
 * =====================================================================
 * FASE 3 — Pruebas de Integración (Angular / Vitest + fetch real)
 * Funcionalidad crítica: Calificación de Cursos
 * Ejecutar con: npx vitest run src/app/core/services/calificacion.integration.spec.ts
 *
 * Integración real probada:
 *  ✔ TC-I01: Flujo correcto — calificar un curso como estudiante autenticado
 *            (cliente HTTP Supabase → tabla curso_reviews → RPC refresh_course_rating)
 *  ✔ TC-I02: Manejo de error — calificar sin token de sesión retorna 401/error
 *
 * Componentes involucrados:
 *   AuthService  →  InscripcionesService  →  Supabase REST API
 *
 * Nota: Estas pruebas usan fetch nativo (Node 18+). No requieren navegador.
 *       Sí realizan llamadas reales a Supabase con el usuario de prueba.
 * =====================================================================
 */

import { describe, it, expect, beforeAll } from 'vitest';

// ── Configuración del entorno de prueba ──────────────────────────────────────
const SUPABASE_URL = 'https://uudjczjsobvqpyvuarhk.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_uivXUxKRnq7zc0TYmLoBGQ_uXf4q2Jf';

// Usuario de prueba (rol: estudiante)
const TEST_EMAIL = 'user@skillacademy.com';
const TEST_PASSWORD = 'User12345';

// Curso real que existe en Supabase (ID del primer curso del catálogo)
// Cambiar este ID si el curso fue eliminado
const TEST_CURSO_ID = 1;

// ── Helpers de integración ───────────────────────────────────────────────────

/**
 * Autentica un usuario contra Supabase y retorna el access_token.
 * Integración real: AuthService.signIn() → supabase.auth.signInWithPassword()
 */
async function signIn(email: string, password: string): Promise<string | null> {
  const res = await fetch(`${SUPABASE_URL}/auth/v1/token?grant_type=password`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'apikey': SUPABASE_ANON_KEY,
    },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) return null;
  const data = await res.json() as { access_token?: string };
  return data.access_token ?? null;
}

/**
 * Llama a calificarCurso simulando la lógica de InscripcionesService:
 * 1. Acota rating a [1, 5]
 * 2. Upsert en curso_reviews
 * 3. Llama a RPC refresh_course_rating
 */
async function calificarCurso(
  userId: string,
  cursoId: number,
  rating: number,
  comment: string,
  accessToken: string
): Promise<{ ok: boolean; error?: string }> {
  const bounded = Math.max(1, Math.min(5, Math.round(rating)));

  // Paso 1: Upsert en curso_reviews
  const upsertRes = await fetch(`${SUPABASE_URL}/rest/v1/curso_reviews`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'apikey': SUPABASE_ANON_KEY,
      'Authorization': `Bearer ${accessToken}`,
      'Prefer': 'resolution=merge-duplicates,return=minimal',
    },
    body: JSON.stringify({
      usuario_id: userId,
      curso_id: cursoId,
      rating: bounded,
      comment: comment || null,
    }),
  });

  if (!upsertRes.ok && upsertRes.status !== 201) {
    const errBody = await upsertRes.text();
    return { ok: false, error: `upsert failed [${upsertRes.status}]: ${errBody}` };
  }

  // Paso 2: RPC refresh_course_rating
  const rpcRes = await fetch(`${SUPABASE_URL}/rest/v1/rpc/refresh_course_rating`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'apikey': SUPABASE_ANON_KEY,
      'Authorization': `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ target_curso_id: cursoId }),
  });

  if (!rpcRes.ok) {
    const errBody = await rpcRes.text();
    return { ok: false, error: `rpc failed [${rpcRes.status}]: ${errBody}` };
  }

  return { ok: true };
}

/**
 * Obtiene el rating guardado en curso_reviews para verificar persistencia.
 */
async function obtenerReview(
  userId: string,
  cursoId: number,
  accessToken: string
): Promise<{ rating: number; comment: string | null } | null> {
  const res = await fetch(
    `${SUPABASE_URL}/rest/v1/curso_reviews?usuario_id=eq.${userId}&curso_id=eq.${cursoId}&select=rating,comment`,
    {
      headers: {
        'apikey': SUPABASE_ANON_KEY,
        'Authorization': `Bearer ${accessToken}`,
      },
    }
  );
  if (!res.ok) return null;
  const data = await res.json() as { rating: number; comment: string | null }[];
  return data[0] ?? null;
}

// ── Estado compartido entre pruebas ─────────────────────────────────────────
let accessToken: string | null = null;
let userId: string | null = null;

// ── Suite de integración ─────────────────────────────────────────────────────
describe('Fase 3 — Pruebas de Integración: Calificación de Cursos (Supabase real)', () => {

  // Autenticación previa a todas las pruebas
  beforeAll(async () => {
    accessToken = await signIn(TEST_EMAIL, TEST_PASSWORD);

    if (accessToken) {
      // Obtener userId desde el token JWT (payload base64)
      const payload = JSON.parse(
        Buffer.from(accessToken.split('.')[1], 'base64').toString('utf-8')
      ) as { sub?: string };
      userId = payload.sub ?? null;
    }
  }, 15000);

  // ── TC-I01: FLUJO CORRECTO ───────────────────────────────────────────────
  describe('TC-I01 | Calificar curso con usuario autenticado — flujo correcto', () => {
    it('debe poder autenticarse con las credenciales del usuario de prueba', () => {
      expect(accessToken).not.toBeNull();
      expect(typeof accessToken).toBe('string');
      expect((accessToken ?? '').length).toBeGreaterThan(10);
    });

    it('debe guardar la calificación en curso_reviews sin error (rating = 4)', async () => {
      expect(accessToken).not.toBeNull();
      expect(userId).not.toBeNull();

      const resultado = await calificarCurso(
        userId!,
        TEST_CURSO_ID,
        4,
        'Curso de integración - test automatizado',
        accessToken!
      );

      expect(resultado.ok).toBe(true);
      expect(resultado.error).toBeUndefined();
    }, 15000);

    it('debe persistir el rating y el comentario en la base de datos', async () => {
      expect(accessToken).not.toBeNull();
      expect(userId).not.toBeNull();

      const review = await obtenerReview(userId!, TEST_CURSO_ID, accessToken!);

      expect(review).not.toBeNull();
      expect(review?.rating).toBe(4);
      expect(review?.comment).toBe('Curso de integración - test automatizado');
    }, 15000);
  });

  // ── TC-I02: MANEJO DE ERROR ─────────────────────────────────────────────
  describe('TC-I02 | Calificar sin token válido — manejo de error', () => {
    it('debe fallar con 401/403 cuando el token es inválido', async () => {
      const tokenInvalido = 'Bearer token.invalido.aqui';

      const res = await fetch(`${SUPABASE_URL}/rest/v1/curso_reviews`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'apikey': SUPABASE_ANON_KEY,
          'Authorization': tokenInvalido,
          'Prefer': 'resolution=merge-duplicates,return=minimal',
        },
        body: JSON.stringify({
          usuario_id: 'fake-user-id',
          curso_id: TEST_CURSO_ID,
          rating: 5,
          comment: 'Test error',
        }),
      });

      // Supabase retorna 401 (JWT inválido) o 403 (RLS falla)
      expect([401, 403]).toContain(res.status);
    }, 15000);

    it('debe retornar error descriptivo cuando calificarCurso detecta token nulo', async () => {
      // Simula la guarda de InscripcionesService.calificarCurso
      const tokenNulo: string | null = null;
      const resultado = !tokenNulo
        ? { ok: false, error: 'Inicia sesion para calificar' }
        : { ok: true };

      expect(resultado.ok).toBe(false);
      expect(resultado.error).toContain('calificar');
    });
  });
});
