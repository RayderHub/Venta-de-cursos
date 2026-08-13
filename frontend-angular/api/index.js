// Vercel ejecuta esta función como CommonJS (el proyecto no declara "type": "module"),
// así que se usa import() dinámico para cargar el bundle ESM del SSR (server.mjs).
// El import estático se transpilaría a require() y Node lanzaría ERR_REQUIRE_ESM.
const serverEntry = '../dist/frontend-angular/server/server.mjs';

module.exports = async function handler(req, res) {
  const { reqHandler } = await import(serverEntry);
  return reqHandler(req, res);
};
