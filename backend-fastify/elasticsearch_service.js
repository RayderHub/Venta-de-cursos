const supabaseUrl = process.env.SUPABASE_URL || 'https://uudjczjsobvqpyvuarhk.supabase.co';
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV1ZGpjempzb2J2cXB5dnVhcmhrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU5Njc4NTksImV4cCI6MjEwMTU0Mzg1OX0.czxrebaQNQ5UgctU7kLjL2l2HsaBWHmwhgQvANNZlDI';

const rawElasticsearchUrl = process.env.ELASTICSEARCH_URL || 'https://033722bf76:5439d2352b6e3c4a035c@determined-alder-1tpnwhzm.us-east-1.bonsaisearch.net';
const ELASTICSEARCH_URL = /^https?:\/\//i.test(rawElasticsearchUrl) ? rawElasticsearchUrl : `http://${rawElasticsearchUrl}`;
const INDEX_NAME = process.env.ELASTICSEARCH_INDEX || 'skillacademy_cursos';

// Helper HTTP universal que soporta tanto Elasticsearch como OpenSearch (Bonsai.io)
function getEsConfig() {
  const parsed = new URL(ELASTICSEARCH_URL);
  const baseUrl = parsed.origin;
  const headers = { 'Content-Type': 'application/json' };
  if (parsed.username || parsed.password) {
    headers['Authorization'] = 'Basic ' + Buffer.from(`${decodeURIComponent(parsed.username)}:${decodeURIComponent(parsed.password)}`).toString('base64');
  }
  return { baseUrl, headers };
}

async function esFetch(path, options = {}) {
  const { baseUrl, headers: defaultHeaders } = getEsConfig();
  const url = `${baseUrl}${path.startsWith('/') ? path : '/' + path}`;
  const headers = { ...defaultHeaders, ...(options.headers || {}) };
  return fetch(url, { ...options, headers });
}

async function asegurarIndice() {
  try {
    const res = await esFetch(`/${INDEX_NAME}`, { method: 'HEAD' });
    if (res.status === 200) return true;

    // Crear índice si no existe
    const createRes = await esFetch(`/${INDEX_NAME}`, {
      method: 'PUT',
      body: JSON.stringify({
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
      })
    });
    return createRes.ok;
  } catch (err) {
    console.error('Error al asegurar índice:', err.message);
    return false;
  }
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

  let bulkLines = '';
  for (const curso of cursos) {
    bulkLines += JSON.stringify({ index: { _index: INDEX_NAME, _id: String(curso.id) } }) + '\n';
    bulkLines += JSON.stringify({
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
    }) + '\n';
  }

  const res = await esFetch(`/${INDEX_NAME}/_bulk?refresh=true`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-ndjson' },
    body: bulkLines
  });

  if (!res.ok) {
    throw new Error(`Bonsai/ES bulk fallo con status ${res.status}`);
  }

  const data = await res.json();
  const fallidos = (data.items || []).filter(item => item.index && item.index.error).length;

  return { indexado: cursos.length - fallidos, total: cursos.length, fallidos };
}

// Memoria caché para resultados de búsqueda con TTL de 60 segundos
// Evita sobrecargar el clúster remoto de Bonsai en peticiones concurrentes idénticas
const searchCache = new Map();
const CACHE_TTL_MS = 60 * 1000;
let indiceYaVerificado = false;

async function reindexarSiVacio() {
  if (indiceYaVerificado) return { indexado: 0, total: 0 };
  try {
    await asegurarIndice();
    const res = await esFetch(`/${INDEX_NAME}/_count`);
    if (res.ok) {
      const data = await res.json();
      indiceYaVerificado = true;
      if ((data.count || 0) === 0) {
        return indexarCursos();
      }
      return { indexado: 0, total: data.count };
    }
  } catch (err) {
    console.warn('Advertencia en reindexarSiVacio:', err.message);
  }
  return { indexado: 0, total: 0 };
}

async function buscarCursos({ q, desde = 0, tamaño = 24 }) {
  await reindexarSiVacio();

  const queryLimpia = (q || '').trim();
  const cacheKey = `${queryLimpia.toLowerCase()}:${desde}:${tamaño}`;

  // Comprobar si existe en caché en memoria y sigue vigente
  const enCache = searchCache.get(cacheKey);
  if (enCache && (Date.now() - enCache.timestamp < CACHE_TTL_MS)) {
    return { ...enCache.data, desdeCache: true };
  }

  const consulta = queryLimpia
    ? {
        bool: {
          should: [
            // Coincidencia exacta con mayor peso
            { match_phrase: { titulo: { query: queryLimpia, boost: 6 } } },
            // Coincidencia fuzzy tolerante a errores tipográficos (ej. "escel" -> "excel")
            { match: { titulo: { query: queryLimpia, fuzziness: 'AUTO', prefix_length: 0, boost: 4 } } },
            { match: { instructor: { query: queryLimpia, fuzziness: 'AUTO', prefix_length: 0, boost: 2 } } },
            { match: { categoria: { query: queryLimpia, fuzziness: 'AUTO', prefix_length: 0, boost: 2 } } },
            { match: { descripcion: { query: queryLimpia, fuzziness: 'AUTO', prefix_length: 0 } } },
            { prefix: { titulo: { value: queryLimpia.toLowerCase() } } }
          ],
          minimum_should_match: 1
        }
      }
    : { match_all: {} };

  try {
    const res = await esFetch(`/${INDEX_NAME}/_search`, {
      method: 'POST',
      body: JSON.stringify({
        from: desde,
        size: Math.min(Number(tamaño) || 24, 50),
        query: consulta
      })
    });

    if (!res.ok) {
      throw new Error(`ES respondio ${res.status}`);
    }

    const data = await res.json();
    const hits = (data.hits?.hits || []).map(hit => ({
      _score: hit._score ?? 0,
      ...hit._source
    }));

    const total = typeof data.hits?.total === 'object' ? data.hits.total.value : (data.hits?.total ?? hits.length);
    const resultado = { total, cursos: hits };

    // Guardar en caché con expiración
    searchCache.set(cacheKey, { timestamp: Date.now(), data: resultado });
    if (searchCache.size > 200) {
      // Limitar tamaño de caché para proteger memoria en micro-contenedores
      const primerClave = searchCache.keys().next().value;
      searchCache.delete(primerClave);
    }

    return resultado;
  } catch (err) {
    console.warn('Fallo en busqueda Elasticsearch/Bonsai, ejecutando fallback Supabase:', err.message);
    const todos = await listarCursosSupabase();
    const filtrados = q && q.trim()
      ? todos.filter(c => 
          (c.titulo && c.titulo.toLowerCase().includes(q.toLowerCase())) ||
          (c.instructor && c.instructor.toLowerCase().includes(q.toLowerCase())) ||
          (c.categoria && c.categoria.toLowerCase().includes(q.toLowerCase())) ||
          (c.descripcion && c.descripcion.toLowerCase().includes(q.toLowerCase()))
        )
      : todos;

    return {
      total: filtrados.length,
      cursos: filtrados.slice(desde, desde + tamaño)
    };
  }
}

async function reiniciarIndice() {
  try {
    await esFetch(`/${INDEX_NAME}`, { method: 'DELETE' });
  } catch (_) {}
  return indexarCursos();
}

module.exports = {
  ELASTICSEARCH_URL,
  INDEX_NAME,
  asegurarIndice,
  listarCursosSupabase,
  indexarCursos,
  reindexarSiVacio,
  buscarCursos,
  reiniciarIndice
};
