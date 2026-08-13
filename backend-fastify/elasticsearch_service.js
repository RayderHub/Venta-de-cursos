const { Client } = require('@elastic/elasticsearch');

const supabaseUrl = process.env.SUPABASE_URL || 'https://uudjczjsobvqpyvuarhk.supabase.co';
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY || 'sb_publishable_uivXUxKRnq7zc0TYmLoBGQ_uXf4q2Jf';

const ELASTICSEARCH_URL = process.env.ELASTICSEARCH_URL || 'http://localhost:9200';
const INDEX_NAME = process.env.ELASTICSEARCH_INDEX || 'skillacademy_cursos';

let client = null;

function getClient() {
  if (!client) {
    client = new Client({ node: ELASTICSEARCH_URL });
  }
  return client;
}

async function asegurarIndice() {
  const es = getClient();
  const existe = await es.indices.exists({ index: INDEX_NAME });

  if (existe) return;

  await es.indices.create({
    index: INDEX_NAME,
    mappings: {
      properties: {
        id: { type: 'long' },
        titulo: { type: 'text', analyzer: 'standard', fields: { keyword: { type: 'keyword' } } },
        categoria: { type: 'text', analyzer: 'standard', fields: { keyword: { type: 'keyword' } } },
        nivel: { type: 'text', analyzer: 'standard', fields: { keyword: { type: 'keyword' } } },
        instructor: { type: 'text', analyzer: 'standard', fields: { keyword: { type: 'keyword' } } },
        instructor_id: { type: 'keyword' },
        precio: { type: 'float' },
        old_precio: { type: 'float' },
        imagen: { type: 'keyword' },
        descripcion: { type: 'text', analyzer: 'standard' },
        rating: { type: 'float' },
        reviews: { type: 'integer' }
      }
    }
  });
}

async function listarCursosSupabase() {
  const endpoint = `${supabaseUrl}/rest/v1/cursos?select=id,titulo,categoria,nivel,instructor,instructor_id,precio,old_precio,imagen,descripcion,rating,reviews,estado&estado=eq.Publicado&order=id.asc`;
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
  if (!Array.isArray(rows)) {
    throw new Error('Supabase no devolvio un arreglo de cursos');
  }

  return rows;
}

async function indexarCursos() {
  await asegurarIndice();

  const cursos = await listarCursosSupabase();
  if (cursos.length === 0) {
    return { indexado: 0, total: 0 };
  }

  const es = getClient();
  const operaciones = cursos.flatMap((curso) => [
    { index: { _index: INDEX_NAME, _id: String(curso.id) } },
    {
      id: curso.id,
      titulo: curso.titulo,
      categoria: curso.categoria,
      nivel: curso.nivel,
      instructor: curso.instructor,
      instructor_id: curso.instructor_id ?? null,
      precio: Number(curso.precio) || 0,
      old_precio: curso.old_precio != null ? Number(curso.old_precio) : null,
      imagen: typeof curso.imagen === 'string' && curso.imagen.startsWith('data:') ? '' : (curso.imagen || 'code'),
      descripcion: curso.descripcion || '',
      rating: Number(curso.rating) || 0,
      reviews: curso.reviews || 0
    }
  ]);

  const resultado = await es.bulk({ operations: operaciones });
  const fallidos = resultado.items.filter((item) => item.index && item.index.error).length;

  return { indexado: cursos.length - fallidos, total: cursos.length, fallidos };
}

async function buscarCursos({ q, desde = 0, tamaño = 24 }) {
  await asegurarIndice();

  const es = getClient();
  const consulta = q && q.trim()
    ? {
        bool: {
          should: [
            { match: { titulo: { query: q, boost: 3 } } },
            { match: { instructor: { query: q, boost: 2 } } },
            { match: { categoria: { query: q } } },
            { match: { descripcion: { query: q } } },
            { prefix: { titulo: q } }
          ]
        }
      }
    : { match_all: {} };

  const response = await es.search({
    index: INDEX_NAME,
    from: desde,
    size: tamaño,
    query: consulta
  });

  const hits = response.hits.hits.map((hit) => ({
    _score: hit._score ?? 0,
    ...hit._source
  }));

  return {
    total: typeof response.hits.total === 'object' ? response.hits.total.value : response.hits.total,
    cursos: hits
  };
}

async function reiniciarIndice() {
  const es = getClient();
  await es.indices.delete({ index: INDEX_NAME }, { ignore: [404] });
  return indexarCursos();
}

module.exports = {
  ELASTICSEARCH_URL,
  INDEX_NAME,
  getClient,
  asegurarIndice,
  listarCursosSupabase,
  indexarCursos,
  buscarCursos,
  reiniciarIndice
};
