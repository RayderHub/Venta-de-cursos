from html import escape
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "practicas"


def paragraph(text="", style=None, bold=False):
    style_xml = f'<w:pPr><w:pStyle w:val="{style}"/></w:pPr>' if style else ""
    bold_xml = "<w:b/>" if bold else ""
    lines = str(text).split("\n")
    run_content = "<w:br/>".join(f'<w:t xml:space="preserve">{escape(line)}</w:t>' for line in lines)
    return (
        "<w:p>"
        f"{style_xml}"
        "<w:r>"
        f"<w:rPr>{bold_xml}</w:rPr>"
        f"{run_content}"
        "</w:r>"
        "</w:p>"
    )


def code_block(text):
    lines = text.strip("\n").split("\n")
    run_content = "<w:br/>".join(f'<w:t xml:space="preserve">{escape(line)}</w:t>' for line in lines)
    return (
        "<w:p>"
        "<w:pPr><w:spacing w:before=\"80\" w:after=\"80\"/></w:pPr>"
        "<w:r>"
        "<w:rPr><w:rFonts w:ascii=\"Consolas\" w:hAnsi=\"Consolas\"/><w:sz w:val=\"18\"/></w:rPr>"
        f"{run_content}"
        "</w:r>"
        "</w:p>"
    )


def table(rows):
    table_rows = []
    for index, row in enumerate(rows):
        cells = []
        for cell in row:
            bold = "<w:b/>" if index == 0 else ""
            cells.append(
                "<w:tc>"
                "<w:tcPr><w:tcW w:w=\"3000\" w:type=\"dxa\"/></w:tcPr>"
                "<w:p><w:r>"
                f"<w:rPr>{bold}</w:rPr>"
                f"<w:t xml:space=\"preserve\">{escape(str(cell))}</w:t>"
                "</w:r></w:p>"
                "</w:tc>"
            )
        table_rows.append(f"<w:tr>{''.join(cells)}</w:tr>")

    return (
        "<w:tbl>"
        "<w:tblPr>"
        "<w:tblW w:w=\"0\" w:type=\"auto\"/>"
        "<w:tblBorders>"
        "<w:top w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"D9E2DC\"/>"
        "<w:left w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"D9E2DC\"/>"
        "<w:bottom w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"D9E2DC\"/>"
        "<w:right w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"D9E2DC\"/>"
        "<w:insideH w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"D9E2DC\"/>"
        "<w:insideV w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"D9E2DC\"/>"
        "</w:tblBorders>"
        "</w:tblPr>"
        f"{''.join(table_rows)}"
        "</w:tbl>"
    )


def build_document(blocks):
    body_parts = []
    for block in blocks:
        kind = block[0]
        if kind == "h1":
            body_parts.append(paragraph(block[1], "Heading1"))
        elif kind == "h2":
            body_parts.append(paragraph(block[1], "Heading2"))
        elif kind == "p":
            body_parts.append(paragraph(block[1]))
        elif kind == "table":
            body_parts.append(table(block[1]))
        elif kind == "code":
            body_parts.append(code_block(block[1]))

    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">'
        "<w:body>"
        f"{''.join(body_parts)}"
        '<w:sectPr><w:pgSz w:w="12240" w:h="15840"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="708" w:footer="708" w:gutter="0"/></w:sectPr>'
        "</w:body>"
        "</w:document>"
    )


CONTENT_TYPES = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>'''


RELS = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>'''


DOC_RELS = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>'''


STYLES = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:rPr><w:rFonts w:ascii="Arial" w:hAnsi="Arial"/><w:sz w:val="22"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:basedOn w:val="Normal"/>
    <w:pPr><w:spacing w:before="120" w:after="180"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Arial" w:hAnsi="Arial"/><w:b/><w:sz w:val="32"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading2">
    <w:name w:val="heading 2"/>
    <w:basedOn w:val="Normal"/>
    <w:pPr><w:spacing w:before="180" w:after="100"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Arial" w:hAnsi="Arial"/><w:b/><w:sz w:val="26"/></w:rPr>
  </w:style>
</w:styles>'''


def write_docx(file_name, blocks):
    document_xml = build_document(blocks)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    output = OUTPUT_DIR / file_name
    with ZipFile(output, "w", ZIP_DEFLATED) as docx:
        docx.writestr("[Content_Types].xml", CONTENT_TYPES)
        docx.writestr("_rels/.rels", RELS)
        docx.writestr("word/_rels/document.xml.rels", DOC_RELS)
        docx.writestr("word/document.xml", document_xml)
        docx.writestr("word/styles.xml", STYLES)


practice_7 = [
    ("h1", "Práctica 7: Widget wearable SkillAcademy"),
    ("h2", "Nombre del widget"),
    ("p", "SkillAcademy Wear Progress"),
    ("h2", "Dispositivo inteligente seleccionado"),
    ("p", "Smartwatch / Wear OS. Se toma como referencia un wearable compatible con Flutter mediante widgets sencillos y tarjetas de aviso."),
    ("h2", "Propósito del widget"),
    ("p", "El widget tiene como propósito mostrar en el reloj avisos breves sobre tareas pendientes, progreso de cursos y recordatorios de estudio. No busca cargar todo el sitio en el smartwatch, solo enviar información corta y útil para que el usuario sepa qué debe revisar."),
    ("h2", "Enlace con wearable del catálogo de Flutter"),
    ("table", [
        ["Elemento", "Descripción"],
        ["Tecnología", "Flutter para construir la interfaz del widget visual."],
        ["Wearable", "Smartwatch con Wear OS."],
        ["Referencia", "https://pub.dev/packages/wear"],
        ["Uso en SkillAcademy", "Mostrar una tarjeta con título, mensaje, progreso y enlace al dashboard."],
    ]),
    ("h2", "Endpoint propuesto"),
    ("table", [
        ["Método", "Ruta", "Función"],
        ["GET", "/api/widget/wearable", "Enviar al smartwatch el aviso activo y el enlace al dashboard de tareas."],
    ]),
    ("h2", "Datos que transferirá el endpoint"),
    ("table", [
        ["Dato", "Tipo", "Propósito"],
        ["nombre", "Texto", "Identificar el widget."],
        ["dispositivo", "Texto", "Indicar el wearable al que va dirigido."],
        ["titulo", "Texto", "Mostrar el encabezado del aviso."],
        ["mensaje", "Texto", "Mostrar una descripción breve."],
        ["curso", "Texto", "Indicar el curso relacionado."],
        ["progreso", "Número", "Mostrar el avance del usuario."],
        ["accion", "Texto", "Texto del botón principal."],
        ["enlace", "Texto / URL", "Abrir el dashboard o la tarea relacionada."],
        ["estado", "Texto", "Indicar si el aviso está activo."],
    ]),
    ("h2", "Ejemplo de respuesta JSON"),
    ("code", '''{
  "nombre": "SkillAcademy Wear Progress",
  "dispositivo": "Smartwatch / Wear OS",
  "titulo": "Continua tu aprendizaje",
  "mensaje": "Tienes una tarea pendiente del curso Excel Avanzado.",
  "curso": "Excel Avanzado",
  "progreso": 65,
  "accion": "Ver dashboard",
  "enlace": "/dashboard#tareas",
  "estado": "activo"
}'''),
    ("h2", "Funcionamiento del widget"),
    ("p", "El backend de SkillAcademy consulta el aviso o tarea activa. Después expone la información en /api/widget/wearable. El smartwatch recibe el título, mensaje, progreso y enlace. Si el usuario toca el botón, se abre el dashboard de tareas para continuar desde una pantalla más cómoda."),
    ("h2", "Pruebas básicas"),
    ("table", [
        ["Prueba", "Resultado esperado"],
        ["Entrar a /api/widget/wearable", "Debe devolver un JSON con información del widget wearable."],
        ["Revisar campo estado", "Debe aparecer como activo."],
        ["Revisar campo progreso", "Debe mostrar un número de avance."],
        ["Revisar campo enlace", "Debe apuntar al dashboard de tareas."],
    ]),
]


practice_8 = [
    ("h1", "Práctica 8: Dashboard de tareas con Flutter + web"),
    ("h2", "Descripción general"),
    ("p", "Se creó un dashboard web para SkillAcademy que muestra tareas, estadísticas y un widget conectado con wearable. La aplicación mantiene la continuidad del proyecto porque usa Fastify como backend y conserva la línea visual definida: encabezado oscuro, acentos verdes, tarjetas limpias y diseño responsive."),
    ("h2", "Tecnologías utilizadas"),
    ("table", [
        ["Tecnología", "Uso dentro de la práctica"],
        ["Node.js + Fastify", "Servir los endpoints y la página del dashboard."],
        ["HTML, CSS y JavaScript", "Construir la vista web responsive del dashboard."],
        ["Flutter", "Se agregó el archivo StatsWidget.dart como componente visual de estadísticas."],
        ["Endpoint JSON", "Compartir los datos entre backend, dashboard web y widget wearable."],
    ]),
    ("h2", "Endpoints agregados"),
    ("table", [
        ["Método", "Ruta", "Función"],
        ["GET", "/dashboard", "Mostrar la página principal del dashboard."],
        ["GET", "/api/dashboard/tareas", "Enviar la lista de tareas del usuario."],
        ["GET", "/api/dashboard/estadisticas", "Enviar resumen de tareas, pendientes y progreso."],
        ["GET", "/api/widget/wearable", "Enviar el aviso activo para smartwatch."],
        ["GET", "/api/widget/smart-tv", "Mantener el widget de Smart TV de la práctica 6."],
    ]),
    ("h2", "Archivos creados o modificados"),
    ("table", [
        ["Archivo", "Descripción"],
        ["endpoint_widget_fastify.js", "Se agregaron endpoints para dashboard, tareas, estadísticas y wearable."],
        ["public/index.html", "Estructura visual del dashboard."],
        ["public/styles.css", "Diseño responsive con tarjetas, botones, estados hover y paleta verde."],
        ["public/dashboard.js", "Consumo de endpoints y renderizado de tareas."],
        ["flutter_stats_widget/StatsWidget.dart", "Componente Flutter de estadísticas."],
        ["respuesta_widget_wearable.json", "Ejemplo de respuesta del widget wearable."],
    ]),
    ("h2", "Componente Flutter de estadísticas"),
    ("p", "El archivo StatsWidget.dart representa el componente visual que puede compilarse en Flutter Web o reutilizarse dentro de una aplicación Flutter. Usa las mismas métricas del endpoint /api/dashboard/estadisticas: total de tareas, pendientes, completadas y progreso promedio."),
    ("h2", "Diseño del dashboard"),
    ("p", "El dashboard organiza primero el resumen general, después las tareas activas y al lado el preview del widget wearable. En pantallas pequeñas, las secciones se acomodan en una sola columna para evitar textos cortados o elementos encimados."),
    ("h2", "Pruebas básicas"),
    ("table", [
        ["Prueba", "Resultado esperado"],
        ["Ejecutar npm start", "El servidor debe iniciar en el puerto 3000."],
        ["Abrir /dashboard", "Debe mostrarse el dashboard con estadísticas y tareas."],
        ["Consultar /api/dashboard/tareas", "Debe devolver la lista de tareas en JSON."],
        ["Consultar /api/dashboard/estadisticas", "Debe devolver totales y progreso promedio."],
        ["Reducir el ancho de pantalla", "El dashboard debe adaptarse a móvil sin encimar contenido."],
    ]),
    ("h2", "Resultado"),
    ("p", "La práctica queda integrada al proyecto actual sin eliminar la funcionalidad anterior. El endpoint de Smart TV continúa disponible y ahora el proyecto también cuenta con dashboard de tareas, widget wearable y componente de estadísticas estilo Flutter."),
]


practice_9 = [
    ("h1", "Práctica 9: Widget Smart TV para actualización desde Git"),
    ("h2", "Descripción general"),
    ("p", "La práctica documenta la actualización del sitio SkillAcademy para incorporar un widget pensado para Smart TV. La actualización se administra desde Git para mantener control de versiones, historial de cambios y una ruta clara de integración antes de publicar la funcionalidad en el sitio web."),
    ("h2", "Objetivo de la actualización"),
    ("p", "El objetivo es permitir que una Smart TV consulte información promocional o educativa del sitio mediante una vista web ligera y un endpoint JSON. La Smart TV no necesita cargar todo el sistema administrativo; solo debe visualizar datos listos para pantalla grande, como promoción activa, curso destacado, mensaje principal, imagen y enlace de acción."),
    ("h2", "Versión de Git propuesta"),
    ("table", [
        ["Elemento", "Descripción"],
        ["Repositorio", "SkillAcademy_Practicas_5_6_Endpoint"],
        ["Rama sugerida", "feature/widget-smart-tv-update"],
        ["Etiqueta sugerida", "v0.9-smart-tv-widget"],
        ["Commit principal", "docs: documentar actualizacion de widget smart tv"],
        ["Carpeta afectada", "frontend-angular, backend-fastify y documentacion"],
    ]),
    ("h2", "Archivos relacionados dentro del proyecto"),
    ("table", [
        ["Archivo", "Función"],
        ["backend-fastify/endpoint_widget_fastify.js", "Expone el endpoint /api/widget/smart-tv con la información del widget."],
        ["frontend-angular/src/app/pages/smart-tv-widget", "Contiene la vista Angular preparada para mostrar el widget en pantalla grande."],
        ["frontend-angular/src/app/core/services/widget.service.ts", "Consume el endpoint del backend desde Angular."],
        ["frontend-angular/proxy.conf.json", "Redirige las peticiones /api al backend local en desarrollo."],
        ["documentacion/practicas", "Guarda la evidencia documental de la práctica."],
    ]),
    ("h2", "Flujo de conexión con Smart TV"),
    ("p", "La Smart TV abre la ruta web /smart-tv desde el navegador integrado o desde una aplicación web embebida. Angular carga el componente del widget y solicita la información al servicio WidgetService. El servicio consulta /api/widget/smart-tv mediante el proxy de Angular. Fastify responde con un JSON preparado para pantalla grande y la interfaz muestra el contenido con tipografía visible, imagen promocional y botón de acción."),
    ("h2", "Endpoint usado para el widget"),
    ("table", [
        ["Método", "Ruta", "Uso"],
        ["GET", "/api/widget/smart-tv", "Enviar la promoción activa o el contenido destacado a la vista de Smart TV."],
    ]),
    ("h2", "Datos que debe entregar el widget"),
    ("table", [
        ["Dato", "Tipo", "Detalle"],
        ["titulo", "Texto", "Encabezado principal visible en la Smart TV."],
        ["mensaje", "Texto", "Descripción corta de la promoción o aviso."],
        ["curso", "Texto", "Curso relacionado con el contenido mostrado."],
        ["categoria", "Texto", "Área académica o categoría del curso."],
        ["descuento", "Texto", "Promoción visible para el usuario."],
        ["imagen", "Texto / URL", "Recurso visual usado en la pantalla."],
        ["enlace", "Texto / URL", "Ruta que dirige al detalle del curso o promoción."],
        ["estado", "Texto", "Indica si el widget está activo o pausado."],
    ]),
    ("h2", "Ejemplo de respuesta JSON"),
    ("code", '''{
  "titulo": "Oferta Especial",
  "mensaje": "20% de descuento en cursos de Excel",
  "curso": "Excel Avanzado",
  "categoria": "Productividad",
  "descuento": "20%",
  "imagen": "/assets/banners/excel-tv.png",
  "enlace": "/cursos/excel-avanzado",
  "estado": "activo"
}'''),
    ("h2", "Detalle de la actualización"),
    ("p", "La actualización debe mantener separadas las responsabilidades del sistema. El backend conserva la información del widget y la entrega como JSON. El frontend solamente consume la respuesta y la adapta a la pantalla. Git permite revisar el cambio antes de unirlo a la rama principal, dejando evidencia de archivos modificados, pruebas realizadas y versión liberada."),
    ("h2", "Control de publicación desde Git"),
    ("table", [
        ["Paso", "Acción"],
        ["1", "Crear rama feature/widget-smart-tv-update desde la rama principal."],
        ["2", "Actualizar o validar endpoint /api/widget/smart-tv en Fastify."],
        ["3", "Validar que Angular consuma el servicio desde /smart-tv."],
        ["4", "Registrar cambios con commits descriptivos."],
        ["5", "Revisar la funcionalidad en navegador de escritorio y Smart TV."],
        ["6", "Unir a la rama principal y generar etiqueta v0.9-smart-tv-widget."],
    ]),
    ("h2", "Posibles errores a considerar"),
    ("table", [
        ["Error", "Tratamiento"],
        ["La Smart TV no carga /smart-tv", "Verificar conexión de red, URL publicada y compatibilidad del navegador."],
        ["El endpoint responde vacío", "Validar que estado sea activo y que el backend esté ejecutándose en el puerto correcto."],
        ["La imagen no aparece", "Revisar ruta del recurso y usar una imagen optimizada para pantalla grande."],
        ["CORS o proxy incorrecto", "Confirmar proxy.conf.json en desarrollo y reglas CORS en producción."],
        ["Texto cortado en pantalla", "Ajustar CSS responsive y probar en resolución 1920x1080."],
    ]),
    ("h2", "Pruebas básicas"),
    ("table", [
        ["Prueba", "Resultado esperado"],
        ["Consultar /api/widget/smart-tv", "Debe devolver el JSON del widget con estado activo."],
        ["Abrir /smart-tv", "Debe mostrarse el widget con título, mensaje, imagen y acción."],
        ["Probar en pantalla grande", "El contenido debe verse legible y sin elementos encimados."],
        ["Cambiar estado a pausado", "La vista debe poder ocultar o reemplazar el contenido activo."],
        ["Revisar historial Git", "Debe existir evidencia de rama, commit y versión del cambio."],
    ]),
    ("h2", "Resultado esperado"),
    ("p", "Al finalizar, SkillAcademy cuenta con una actualización documentada para presentar contenido del sitio en una Smart TV. La práctica deja definido el flujo de versionamiento, la conexión entre Angular y Fastify, la estructura del JSON y las pruebas mínimas para liberar la funcionalidad sin afectar el resto del sistema."),
]


practice_10 = [
    ("h1", "Práctica 10: Actualización de base estable para el sitio"),
    ("h2", "Descripción general"),
    ("p", "La práctica documenta el implemento de una actualización de base estable para SkillAcademy. Esta actualización busca preparar el sitio para trabajar con datos persistentes, reducir información fija dentro del código y establecer un calendario de mantenimiento que permita liberar cambios con menor riesgo."),
    ("h2", "Objetivo de la base estable"),
    ("p", "El objetivo es definir una base técnica confiable para que cursos, tareas, usuarios, widgets y estadísticas puedan consultarse desde una base de datos. Actualmente el proyecto conserva datos de ejemplo en el backend y en archivos del frontend; la actualización propone centralizar esa información en una fuente persistente y documentar el tratamiento de errores comunes."),
    ("h2", "Base de datos propuesta"),
    ("table", [
        ["Elemento", "Detalle"],
        ["Motor", "PostgreSQL"],
        ["Paquete relacionado", "pg"],
        ["Variable de entorno", "DATABASE_URL"],
        ["Entorno local", "postgresql://skillacademy_user:password@localhost:5432/skillacademy"],
        ["Entorno producción", "URL segura definida en el servidor de despliegue"],
    ]),
    ("h2", "Enlace de conexión sugerido"),
    ("code", '''DATABASE_URL=postgresql://skillacademy_user:password@localhost:5432/skillacademy

Uso previsto:
- Backend Fastify lee DATABASE_URL desde variables de entorno.
- El backend consulta PostgreSQL mediante pg.
- Angular no se conecta directo a la base de datos; consume endpoints /api.'''),
    ("h2", "Módulos que deben migrarse a datos persistentes"),
    ("table", [
        ["Módulo", "Datos principales", "Endpoint relacionado"],
        ["Cursos", "titulo, categoria, precio, imagen, descripcion", "/api/cursos"],
        ["Usuarios", "nombre, correo, rol, estado", "/api/usuarios"],
        ["Tareas", "titulo, curso, fecha, prioridad, progreso", "/api/dashboard/tareas"],
        ["Widgets", "tipo, titulo, mensaje, enlace, estado", "/api/widget/smart-tv"],
        ["Estadísticas", "totales, pendientes, completadas, progreso", "/api/dashboard/estadisticas"],
    ]),
    ("h2", "Elementos solucionados con la actualización"),
    ("table", [
        ["Elemento", "Solución documentada"],
        ["Datos fijos en código", "Mover información repetida a tablas de base de datos."],
        ["Cambios manuales por archivo", "Actualizar contenido desde registros y no desde recompilación del frontend."],
        ["Widgets sin historial", "Guardar fecha de creación, estado y última modificación."],
        ["Tareas sin persistencia", "Registrar tareas por usuario y curso."],
        ["Falta de trazabilidad", "Agregar calendario de actualización y control de versiones."],
    ]),
    ("h2", "Posibles errores y tratamiento"),
    ("table", [
        ["Error", "Causa probable", "Tratamiento"],
        ["ECONNREFUSED", "PostgreSQL apagado o puerto incorrecto", "Validar servicio, host, puerto y variable DATABASE_URL."],
        ["Credenciales inválidas", "Usuario o contraseña incorrectos", "Actualizar secreto y probar conexión antes de desplegar."],
        ["Tabla inexistente", "Migración pendiente", "Ejecutar script de migración antes de levantar el backend."],
        ["Respuesta vacía", "No existen registros activos", "Mostrar estado sin datos y registrar aviso en logs."],
        ["Datos duplicados", "Carga inicial repetida", "Usar identificadores únicos y validaciones de inserción."],
        ["CORS o proxy", "Frontend apunta a un host equivocado", "Revisar proxy en desarrollo y configuración CORS en producción."],
    ]),
    ("h2", "Actualización propuesta para el backend"),
    ("p", "El backend debe mantener Fastify como servidor principal y agregar un módulo de conexión a base de datos. Las rutas actuales pueden conservar su forma para no romper el frontend, pero su origen de datos cambiaría de arreglos estáticos a consultas SQL. Si la base de datos falla, el backend debe responder con un mensaje claro y código HTTP adecuado."),
    ("h2", "Rutas mínimas para la base estable"),
    ("table", [
        ["Método", "Ruta", "Propósito"],
        ["GET", "/api/health", "Confirmar que backend y base de datos están disponibles."],
        ["GET", "/api/cursos", "Listar cursos disponibles."],
        ["GET", "/api/dashboard/tareas", "Listar tareas persistentes del usuario."],
        ["GET", "/api/dashboard/estadisticas", "Calcular estadísticas desde la base."],
        ["GET", "/api/widget/smart-tv", "Consultar widget activo para Smart TV."],
        ["GET", "/api/widget/wearable", "Consultar aviso activo para smartwatch."],
    ]),
    ("h2", "Calendario de actualizaciones"),
    ("table", [
        ["Fecha", "Actividad", "Responsable"],
        ["2026-07-06", "Crear respaldo del proyecto y definir estructura inicial de tablas.", "Desarrollo"],
        ["2026-07-08", "Agregar conexión DATABASE_URL y prueba /api/health.", "Backend"],
        ["2026-07-10", "Migrar cursos, widgets y tareas de ejemplo a PostgreSQL.", "Backend"],
        ["2026-07-13", "Actualizar servicios Angular para validar respuestas reales.", "Frontend"],
        ["2026-07-15", "Ejecutar pruebas de errores, datos vacíos y conexión perdida.", "QA"],
        ["2026-07-17", "Liberar versión estable v1.0-base-estable.", "Equipo"],
    ]),
    ("h2", "Respaldo y recuperación"),
    ("p", "Antes de cada actualización se debe guardar un respaldo de la base de datos y una etiqueta de Git. Si una migración falla, el equipo puede regresar a la etiqueta estable anterior y restaurar el respaldo. Esta estrategia evita que un error de datos afecte las vistas principales del sitio."),
    ("h2", "Checklist de validación"),
    ("table", [
        ["Validación", "Resultado esperado"],
        ["DATABASE_URL existe", "El backend puede leer la variable de entorno."],
        ["/api/health responde", "Debe indicar estado correcto de servidor y base de datos."],
        ["Widgets activos", "Smart TV y wearable reciben información desde la base."],
        ["Dashboard carga tareas", "La lista se obtiene desde datos persistentes."],
        ["Errores controlados", "El usuario no ve trazas técnicas ni mensajes inseguros."],
        ["Rollback probado", "Existe respaldo y etiqueta de versión estable."],
    ]),
    ("h2", "Resultado esperado"),
    ("p", "La actualización deja definido un camino estable para que SkillAcademy avance de datos de ejemplo a una base persistente. La documentación cubre conexión, errores, elementos solucionados, calendario de liberación y criterios de validación para mantener el sitio actualizado sin perder estabilidad."),
]


if __name__ == "__main__":
    write_docx("Practica_7_SkillAcademy_Widget_Wearable.docx", practice_7)
    write_docx("Practica_8_SkillAcademy_Dashboard_Tareas.docx", practice_8)
    write_docx("Practica_9_SkillAcademy_Widget_Smart_TV_Git.docx", practice_9)
    write_docx("Practica_10_SkillAcademy_Base_Estable.docx", practice_10)
