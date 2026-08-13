const fastify = require('fastify')({ logger: true });
const {
  indexarCursos,
  buscarCursos,
  reiniciarIndice,
  asegurarIndice
} = require('./elasticsearch_service');

const supabaseUrl = process.env.SUPABASE_URL || 'https://uudjczjsobvqpyvuarhk.supabase.co';
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY || 'sb_publishable_uivXUxKRnq7zc0TYmLoBGQ_uXf4q2Jf';

const smartTvWidget = {
  titulo: 'Oferta Especial',
  mensaje: '20% de descuento en cursos de Excel',
  curso: 'Excel Avanzado',
  categoria: 'Productividad',
  descuento: '20%',
  imagen: '/assets/banners/excel-tv.png',
  enlace: '/cursos/excel-avanzado',
  estado: 'activo'
};

let wearableWidget = {
  nombre: 'SkillAcademy Wear Progress',
  dispositivo: 'Smartwatch / Wear OS',
  titulo: 'Continua tu aprendizaje',
  mensaje: 'Tienes una tarea pendiente del curso Excel Avanzado.',
  curso: 'Excel Avanzado',
  progreso: 65,
  accion: 'Ver dashboard',
  enlace: '/dashboard#tareas',
  estado: 'activo'
};

const fallbackPromocionesWearable = [
  {
    id: 1,
    titulo: 'Promocion de Excel Avanzado',
    subtitulo: 'Promocion activa',
    mensaje: 'Accede a Excel Avanzado con precio especial y practica dashboards ejecutivos.',
    imagen: 'green',
    enlace: '/catalogo',
    estado: 'Activo',
    tipo: 'promocion'
  }
];

const tareas = [
  {
    id: 1,
    titulo: 'Completar modulo de formulas',
    curso: 'Excel Avanzado',
    fecha: '2026-06-22',
    prioridad: 'Alta',
    estado: 'Pendiente',
    progreso: 65,
    dispositivo: 'Web y smartwatch'
  },
  {
    id: 2,
    titulo: 'Revisar leccion de presentaciones',
    curso: 'PowerPoint Profesional',
    fecha: '2026-06-24',
    prioridad: 'Media',
    estado: 'En progreso',
    progreso: 40,
    dispositivo: 'Web'
  },
  {
    id: 3,
    titulo: 'Responder evaluacion de diseno',
    curso: 'Diseno Grafico Basico',
    fecha: '2026-06-26',
    prioridad: 'Media',
    estado: 'Pendiente',
    progreso: 20,
    dispositivo: 'Web y celular'
  },
  {
    id: 4,
    titulo: 'Publicar banner de promocion',
    curso: 'Administracion SkillAcademy',
    fecha: '2026-06-28',
    prioridad: 'Baja',
    estado: 'Completada',
    progreso: 100,
    dispositivo: 'Smart TV'
  }
];

function getDashboardStats() {
  const completadas = tareas.filter((tarea) => tarea.estado === 'Completada').length;
  const pendientes = tareas.length - completadas;
  const progresoPromedio = Math.round(
    tareas.reduce((total, tarea) => total + tarea.progreso, 0) / tareas.length
  );

  return {
    totalTareas: tareas.length,
    completadas,
    pendientes,
    progresoPromedio,
    wearableActivo: wearableWidget.estado === 'activo',
    promocionActiva: smartTvWidget.estado === 'activo'
  };
}

async function getPromocionesWearable() {
  if (!supabaseUrl || !supabaseAnonKey) {
    return fallbackPromocionesWearable;
  }

  const endpoint = `${supabaseUrl}/rest/v1/banners?select=id,titulo,subtitulo,descripcion,imagen,enlace,estado&estado=eq.Activo&order=id.asc`;
  const response = await fetch(endpoint, {
    headers: {
      apikey: supabaseAnonKey,
      Authorization: `Bearer ${supabaseAnonKey}`
    }
  });

  if (!response.ok) {
    throw new Error(`Supabase respondio ${response.status}`);
  }

  const rows = await response.json();
  if (!Array.isArray(rows) || rows.length === 0) {
    return fallbackPromocionesWearable;
  }

  return rows.map((banner) => ({
    id: Number(banner.id),
    titulo: banner.titulo,
    subtitulo: banner.subtitulo || 'Promocion activa',
    mensaje: banner.descripcion || banner.subtitulo || 'Tienes una promocion disponible en SkillAcademy.',
    imagen: banner.imagen || 'green',
    enlace: banner.enlace || '/catalogo',
    estado: banner.estado || 'Activo',
    tipo: 'promocion'
  }));
}

fastify.get('/api/widget/smart-tv', async () => smartTvWidget);

fastify.get('/api/widget/wearable', async () => wearableWidget);

fastify.get('/api/widget/wearable/promociones', async () => {
  const promociones = await getPromocionesWearable();
  return {
    nombre: 'SkillAcademy Wear Promotions',
    dispositivo: 'Wear OS',
    total: promociones.length,
    promociones
  };
});

fastify.get('/api/widget/wearable/notificacion', async () => {
  const promociones = await getPromocionesWearable();
  return promociones[0] || fallbackPromocionesWearable[0];
});

fastify.put('/api/widget/wearable', async (request, reply) => {
  const body = request.body || {};
  wearableWidget = {
    ...wearableWidget,
    nombre: body.nombre || wearableWidget.nombre,
    dispositivo: body.dispositivo || wearableWidget.dispositivo,
    titulo: body.titulo || wearableWidget.titulo,
    mensaje: body.mensaje || wearableWidget.mensaje,
    curso: body.curso || wearableWidget.curso,
    progreso: Number.isFinite(Number(body.progreso)) ? Math.max(0, Math.min(100, Number(body.progreso))) : wearableWidget.progreso,
    accion: body.accion || wearableWidget.accion,
    enlace: body.enlace || wearableWidget.enlace,
    estado: body.estado || wearableWidget.estado
  };

  return reply.send(wearableWidget);
});

fastify.get('/api/dashboard/tareas', async () => ({ tareas }));

fastify.get('/api/dashboard/estadisticas', async () => getDashboardStats());

fastify.get('/api/cursos/search', async (request, reply) => {
  const { q = '', desde = 0, tamano = 24 } = request.query;

  try {
    const resultado = await buscarCursos({
      q: String(q),
      desde: Number(desde) || 0,
      tamaño: Math.min(Number(tamano) || 24, 100)
    });
    return resultado;
  } catch (error) {
    request.log.error(error);
    return reply.code(502).send({
      error: 'Buscador no disponible',
      mensaje: 'El servicio de Elasticsearch no esta disponible. Intenta mas tarde.'
    });
  }
});

fastify.post('/api/cursos/search/indexar', async (request, reply) => {
  try {
    const resultado = await indexarCursos();
    return { ok: true, ...resultado };
  } catch (error) {
    request.log.error(error);
    return reply.code(502).send({
      error: 'Buscador no disponible',
      mensaje: 'No se pudieron indexar los cursos en Elasticsearch.'
    });
  }
});

fastify.post('/api/cursos/search/reindexar', async (request, reply) => {
  try {
    const resultado = await reiniciarIndice();
    return { ok: true, ...resultado };
  } catch (error) {
    request.log.error(error);
    return reply.code(502).send({
      error: 'Buscador no disponible',
      mensaje: 'No se pudo reconstruir el indice de Elasticsearch.'
    });
  }
});

fastify.get('/api/health', async () => {
  let elasticsearch = 'no-configurado';
  try {
    await asegurarIndice();
    elasticsearch = 'disponible';
  } catch {
    elasticsearch = 'no-disponible';
  }

  return {
    estado: 'ok',
    servidor: 'SkillAcademy API',
    elasticsearch
  };
});

const PORT = Number(process.env.PORT) || 3000;

fastify.listen({ port: PORT, host: '0.0.0.0' }, (err, address) => {
  if (err) {
    fastify.log.error(err);
    process.exit(1);
  }
  console.log(`Servidor iniciado en ${address}`);
});
