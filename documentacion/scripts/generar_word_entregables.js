const fs = require('fs');
const path = require('path');
const {
  Document,
  Packer,
  Paragraph,
  TextRun,
  HeadingLevel,
  Table,
  TableRow,
  TableCell,
  WidthType,
  AlignmentType,
  BorderStyle,
  Header,
  Footer,
  PageNumber,
  PageBreak
} = require('docx');

// Paleta de colores corporativos SkillAcademy
const COLOR_PRIMARY = '0F2942';   // Navy oscuro elegante
const COLOR_SECONDARY = '0284C7'; // Cyan / Azul moderno
const COLOR_ACCENT = '0D9488';    // Teal / Esmeralda
const COLOR_HIGHLIGHT = '6366F1'; // Indigo tecnológico
const COLOR_TEXT = '1E293B';      // Gris oscuro para texto base
const COLOR_MUTED = '64748B';     // Gris secundario
const COLOR_BG_LIGHT = 'F8FAFC';  // Fondo claro de filas alternadas
const COLOR_BORDER = 'CBD5E1';    // Borde de tablas

const tableBorderStyle = {
  style: BorderStyle.SINGLE,
  size: 4,
  color: COLOR_BORDER
};

const cellBorders = {
  top: tableBorderStyle,
  bottom: tableBorderStyle,
  left: tableBorderStyle,
  right: tableBorderStyle
};

function p(text, opts = {}) {
  return new Paragraph({
    alignment: opts.align || AlignmentType.LEFT,
    spacing: {
      before: opts.before !== undefined ? opts.before : 120,
      after: opts.after !== undefined ? opts.after : 120,
      line: 276
    },
    children: [
      new TextRun({
        text,
        size: opts.size || 22,
        font: 'Segoe UI',
        color: opts.color || COLOR_TEXT,
        bold: !!opts.bold,
        italics: !!opts.italics
      })
    ]
  });
}

function h1(text, color = COLOR_PRIMARY) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 360, after: 180 },
    children: [
      new TextRun({
        text,
        size: 30, // 15pt
        font: 'Segoe UI Semibold',
        color: color,
        bold: true
      })
    ]
  });
}

function h2(text, color = COLOR_SECONDARY) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 260, after: 140 },
    children: [
      new TextRun({
        text,
        size: 25, // 12.5pt
        font: 'Segoe UI Semibold',
        color: color,
        bold: true
      })
    ]
  });
}

function h3(text, color = COLOR_ACCENT) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    spacing: { before: 200, after: 100 },
    children: [
      new TextRun({
        text,
        size: 23, // 11.5pt
        font: 'Segoe UI Semibold',
        color: color,
        bold: true
      })
    ]
  });
}

function bullet(prefixBold, text) {
  return new Paragraph({
    bullet: { level: 0 },
    spacing: { before: 60, after: 60, line: 260 },
    children: [
      new TextRun({
        text: prefixBold ? prefixBold + ': ' : '',
        bold: true,
        size: 21,
        font: 'Segoe UI',
        color: COLOR_PRIMARY
      }),
      new TextRun({
        text: text,
        size: 21,
        font: 'Segoe UI',
        color: COLOR_TEXT
      })
    ]
  });
}

function callout(title, body, borderColor = COLOR_ACCENT, bgFill = 'F0FDFA') {
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    borders: {
      top: { style: BorderStyle.NONE },
      bottom: { style: BorderStyle.NONE },
      right: { style: BorderStyle.NONE },
      left: { style: BorderStyle.SINGLE, size: 24, color: borderColor }
    },
    rows: [
      new TableRow({
        children: [
          new TableCell({
            shading: { fill: bgFill },
            margins: { top: 140, bottom: 140, left: 200, right: 180 },
            children: [
              new Paragraph({
                spacing: { before: 0, after: 60 },
                children: [
                  new TextRun({
                    text: `📌 ${title}`,
                    bold: true,
                    size: 22,
                    font: 'Segoe UI Semibold',
                    color: borderColor
                  })
                ]
              }),
              new Paragraph({
                spacing: { before: 0, after: 0, line: 260 },
                children: [
                  new TextRun({
                    text: body,
                    size: 20,
                    font: 'Segoe UI',
                    color: COLOR_TEXT
                  })
                ]
              })
            ]
          })
        ]
      })
    ]
  });
}

function buildTable(headers, rows, colWidthPercents, headerColor = COLOR_PRIMARY) {
  const headerRow = new TableRow({
    isHeader: true,
    children: headers.map((h, i) => new TableCell({
      width: { size: colWidthPercents[i], type: WidthType.PERCENTAGE },
      shading: { fill: headerColor },
      borders: cellBorders,
      margins: { top: 120, bottom: 120, left: 120, right: 120 },
      children: [
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [
            new TextRun({
              text: h,
              bold: true,
              size: 20,
              font: 'Segoe UI Semibold',
              color: 'FFFFFF'
            })
          ]
        })
      ]
    }))
  });

  const bodyRows = rows.map((r, rowIndex) => new TableRow({
    children: r.map((cellText, i) => new TableCell({
      width: { size: colWidthPercents[i], type: WidthType.PERCENTAGE },
      shading: { fill: rowIndex % 2 === 0 ? 'FFFFFF' : COLOR_BG_LIGHT },
      borders: cellBorders,
      margins: { top: 100, bottom: 100, left: 120, right: 120 },
      children: [
        new Paragraph({
          children: [
            new TextRun({
              text: String(cellText),
              size: 19,
              font: 'Segoe UI',
              color: COLOR_TEXT
            })
          ]
        })
      ]
    }))
  }));

  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    rows: [headerRow, ...bodyRows]
  });
}

function codeBox(title, codeText) {
  const lines = codeText.trim().split('\n');
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    borders: {
      top: { style: BorderStyle.SINGLE, size: 4, color: 'CBD5E1' },
      bottom: { style: BorderStyle.SINGLE, size: 4, color: 'CBD5E1' },
      left: { style: BorderStyle.SINGLE, size: 16, color: COLOR_SECONDARY },
      right: { style: BorderStyle.SINGLE, size: 4, color: 'CBD5E1' }
    },
    rows: [
      new TableRow({
        children: [
          new TableCell({
            shading: { fill: '0F172A' },
            margins: { top: 80, bottom: 80, left: 140, right: 140 },
            children: [
              new Paragraph({
                spacing: { before: 0, after: 0 },
                children: [
                  new TextRun({
                    text: `💻 ${title}`,
                    bold: true,
                    size: 19,
                    font: 'Consolas',
                    color: '38BDF8'
                  })
                ]
              })
            ]
          })
        ]
      }),
      new TableRow({
        children: [
          new TableCell({
            shading: { fill: '1E293B' },
            margins: { top: 100, bottom: 100, left: 140, right: 140 },
            children: lines.map(line => new Paragraph({
              spacing: { before: 0, after: 0, line: 220 },
              children: [
                new TextRun({
                  text: line,
                  size: 18,
                  font: 'Consolas',
                  color: 'E2E8F0'
                })
              ]
            }))
          })
        ]
      })
    ]
  });
}

function divider() {
  return new Paragraph({
    spacing: { before: 180, after: 180 },
    alignment: AlignmentType.CENTER,
    children: [
      new TextRun({
        text: '— • — • —',
        color: COLOR_MUTED,
        size: 18
      })
    ]
  });
}

async function generateDocx() {
  const doc = new Document({
    styles: {
      default: {
        document: {
          run: {
            font: 'Segoe UI',
            size: 22,
            color: COLOR_TEXT
          }
        }
      }
    },
    sections: [
      {
        properties: {
          page: {
            margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 }
          }
        },
        headers: {
          default: new Header({
            children: [
              new Paragraph({
                alignment: AlignmentType.RIGHT,
                children: [
                  new TextRun({
                    text: 'SkillAcademy | Construcción de App Móvil Nativa en 3 Entregables',
                    font: 'Segoe UI',
                    size: 16,
                    color: COLOR_MUTED,
                    italics: true
                  })
                ]
              })
            ]
          })
        },
        footers: {
          default: new Footer({
            children: [
              new Paragraph({
                alignment: AlignmentType.RIGHT,
                children: [
                  new TextRun({
                    text: 'Página ',
                    font: 'Segoe UI',
                    size: 16,
                    color: COLOR_MUTED
                  }),
                  new TextRun({
                    children: [PageNumber.CURRENT],
                    font: 'Segoe UI',
                    size: 16,
                    color: COLOR_MUTED
                  }),
                  new TextRun({
                    text: ' de ',
                    font: 'Segoe UI',
                    size: 16,
                    color: COLOR_MUTED
                  }),
                  new TextRun({
                    children: [PageNumber.TOTAL_PAGES],
                    font: 'Segoe UI',
                    size: 16,
                    color: COLOR_MUTED
                  })
                ]
              })
            ]
          })
        },
        children: [
          // ================= PORTADA =================
          new Paragraph({ spacing: { before: 600 } }),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { before: 200, after: 120 },
            children: [
              new TextRun({
                text: 'SKILLACADEMY',
                size: 48,
                bold: true,
                color: COLOR_PRIMARY,
                font: 'Segoe UI Black'
              })
            ]
          }),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { before: 0, after: 260 },
            children: [
              new TextRun({
                text: 'DOCUMENTACIÓN DE PROYECTO & PLAN DE CONSTRUCCIÓN',
                size: 24,
                bold: true,
                color: COLOR_SECONDARY,
                font: 'Segoe UI Semibold'
              })
            ]
          }),
          divider(),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { before: 180, after: 140 },
            children: [
              new TextRun({
                text: 'APLICACIÓN MÓVIL NATIVA DE VENTAS DIVIDIDA EN 3 ENTREGABLES',
                size: 30,
                bold: true,
                color: COLOR_PRIMARY,
                font: 'Segoe UI'
              })
            ]
          }),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { before: 0, after: 360 },
            children: [
              new TextRun({
                text: 'Marco de Referencia Web Existente, Delimitación de Plataformas y\nConstrucción Progresiva de la App Móvil (Diseño UI/UX, Lógica y Checkout)',
                size: 22,
                color: COLOR_TEXT,
                italics: true,
                font: 'Segoe UI'
              })
            ]
          }),
          new Paragraph({ spacing: { before: 300 } }),
          buildTable(
            ['Metadato del Proyecto', 'Detalle de Control'],
            [
              ['Nombre del Proyecto', 'SkillAcademy E-Learning Ecosystem'],
              ['Tipo de Solución', 'Migración y Desarrollo de App Móvil Nativa de Ventas'],
              ['Estructura del Proyecto', 'Documentación Base + 3 Entregables de la App Móvil'],
              ['Entregable 1 de la App', 'Diseño UI/UX, Maquetación Visual, Design System y Pantallas'],
              ['Entregable 2 de la App', 'Lógica de Negocio, Integración API/Supabase y Motor del Carrito'],
              ['Entregable 3 de la App', 'Pasarela de Pago, Checkout Transaccional, Aula Móvil y Despliegue'],
              ['Rol de la Plataforma Web', 'Permanencia 100% de la gestión administrativa, cursos, cupones y banners'],
              ['Versión', '2.5 - Producción y Entrega Académica / Profesional']
            ],
            [35, 65]
          ),
          new Paragraph({ spacing: { before: 400 } }),
          callout(
            'Organización del Documento',
            'Este documento técnico contiene en su primera sección la documentación y análisis integral del ecosistema web existente (Angular + Fastify + Supabase) y los requerimientos del proyecto. Posteriormente, se divide la construcción de la aplicación móvil nativa en 3 entregables ejecutables de software: Entregable 1 (Diseño y Maquetación Visual), Entregable 2 (Lógica, API y Carrito) y Entregable 3 (Checkout, Pasarela, Aula Móvil y Despliegue).'
          ),
          new Paragraph({ children: [new PageBreak()] }),

          // ================= ÍNDICE DE ESTRUCTURA =================
          h1('Estructura General del Documento'),
          p('Para responder con exactitud a los requerimientos técnicos y de entrega, el documento se organiza de la siguiente manera:'),
          bullet('MARCO GENERAL', 'Documentación Técnica y Análisis del Sistema Web Existente (Diagnóstico de Angular, Fastify y Supabase; justificación de la separación de plataformas; requerimientos IEEE 830).'),
          bullet('ENTREGABLE 1 DE LA APP MÓVIL', 'Diseño UI/UX, Maquetación Visual, Design System y Vistas de Pantalla (Estructura visual nativa en Jetpack Compose, navegación BottomBar/TopBar, maquetación de pantallas de Home, Catálogo, Ficha de Curso, Carrito y Perfil).'),
          bullet('ENTREGABLE 2 DE LA APP MÓVIL', 'Lógica de Negocio, Integración con Base de Datos/API y Motor del Carrito (Supabase Auth, repositorio de catálogo, búsqueda predictiva Elasticsearch, persistencia local con Room DB y validador reactivo de cupones).'),
          bullet('ENTREGABLE 3 DE LA APP MÓVIL', 'Pasarela de Pago, Checkout Transaccional, Aula Móvil, Pruebas y Despliegue (Cobro seguro, inserción en la tabla de inscripciones, pantalla de éxito, reproductor de lecciones en "Mis Cursos", pipeline CI/CD y manual de administración web-móvil).'),
          divider(),

          // =========================================================================
          // SECCIÓN PREVIA: DOCUMENTACIÓN TÉCNICA Y MARCO ARQUITECTÓNICO
          // =========================================================================
          h1('MARCO GENERAL: DOCUMENTACIÓN TÉCNICA Y ANÁLISIS DEL SISTEMA EXISTENTE'),
          
          h2('1. Diagnóstico del Sistema Web Existente (SkillAcademy Web)'),
          p('La plataforma educativa SkillAcademy cuenta actualmente con una arquitectura web dividida en tres capas principales:'),
          bullet('Frontend Web (Angular 17+)', 'Construido con componentes standalone, señales reactivas (Signals) y sistema de rutas protegido por guards (guestGuard, authGuard, studentGuard, teacherGuard, adminGuard). Centraliza tanto las vistas públicas de catálogo (/catalogo, /curso/:id, /carrito) como el centro de control administrativo (/admin/dashboard, /admin/cursos, /admin/usuarios, /admin/banners, /admin/popups, /admin/lecciones).'),
          bullet('Backend de Servicios e Integración (Fastify)', 'Servidor REST de alta velocidad en Node.js que proporciona endpoints de consulta para widgets, agregación de estadísticas (/api/dashboard/estadisticas) y conexión a Elasticsearch (/api/cursos/search) para búsqueda difusa con tolerancia a errores ortográficos.'),
          bullet('Persistencia y Seguridad (Supabase PostgreSQL)', 'Base de datos relacional con Row Level Security (RLS) habilitado. Gestiona las tablas de profiles, cursos, categorias, curso_items, lecciones, lecciones_vistas, tareas, banners, popups, cupones e inscripciones.'),
          p('El análisis de usabilidad revela que la interacción comercial (explorar cursos, comparar precios, aprovechar descuentos flash y pagar) es significativamente más eficiente en un entorno móvil nativo, aprovechando la fluidez táctil, el inicio de sesión biométrico y la inmediatez de las notificaciones push.'),

          h2('2. Justificación Arquitectónica: Venta en Móvil Nativo vs. Administración en Web'),
          p('Se establece una división estricta de responsabilidades funcionales y de plataforma:'),
          buildTable(
            ['Criterio de Separación', 'Aplicación Móvil Nativa (Venta & Estudiante)', 'Plataforma Web (Administración & Docente)'],
            [
              [
                'Enfoque de Negocio',
                'Conversión comercial, adquisición de alumnos, fidelización y consumo rápido de cursos.',
                'Gestión operativa, control financiero, moderación de contenidos y configuración de campañas.'
              ],
              [
                'Audiencia Objetivo',
                'Público general, prospectos compradores y estudiantes matriculados.',
                'Administradores del sistema, coordinadores académicos y profesores.'
              ],
              [
                'Experiencia de Interfaz',
                'Optimizada para una mano (Thumb Zone), animaciones a 60 fps, pagos con un toque (Google Pay/Apple Pay).',
                'Interfaces de alta densidad de datos (DataTables, filtros multidimensionales, formularios masivos).'
              ],
              [
                'Políticas de Seguridad',
                'Acceso limitado mediante RLS: el alumno solo ve cursos públicos y sus propias inscripciones/compras.',
                'Acceso con rol admin o teacher validado por current_role() para mutar cursos, precios y cupones.'
              ]
            ],
            [22, 39, 39]
          ),

          new Paragraph({ spacing: { before: 140 } }),
          callout(
            'Principio Operativo Central',
            'La aplicación móvil nativa no requiere implementar módulos de creación o edición de cursos, subida de contenidos o creación de cupones. Todo esto continúa administrándose desde el panel web de Angular, el cual alimenta de manera automática la base de datos de Supabase que la app móvil consume en tiempo real.'
          ),

          h2('3. Requerimientos Formales del Sistema Móvil (IEEE 830)'),
          buildTable(
            ['Tipo / Código', 'Nombre del Requerimiento', 'Descripción Técnica y Criterio de Aceptación'],
            [
              ['RF-V01', 'Exploración de Cursos', 'Presentar catálogo filtrable por categoría (Excel, Programación, Diseño, Productividad) con precio normal y precio rebajado.'],
              ['RF-V02', 'Búsqueda Rápida', 'Integración con Elasticsearch / Fastify para búsqueda predictiva en tiempo real con sugerencias mientras se escribe.'],
              ['RF-V03', 'Ficha Multimedia del Curso', 'Visualización de trailer de video, temario de lecciones, valoración de reseñas y perfil del instructor.'],
              ['RF-V04', 'Carrito de Compras Nativo', 'Gestión de cursos seleccionados con persistencia local en el dispositivo (Room DB).'],
              ['RF-V05', 'Cupones de Descuento', 'Validación asíncrona contra la tabla cupones de Supabase y recálculo instantáneo del importe total.'],
              ['RF-V06', 'Checkout y Pasarela', 'Procesamiento de orden de compra y registro automático en la tabla inscripciones mediante Supabase SDK.'],
              ['RF-V07', 'Banners y Popups', 'Recepción de promociones activas gestionadas desde la web mediante las tablas banners y popups.'],
              ['RF-V08', 'Aula Móvil (Mis Cursos)', 'Listado de cursos adquiridos para continuar el aprendizaje desde el celular.'],
              ['RNF-01', 'Rendimiento Táctil', 'Tasa de refresco constante a 60 fps en listas de cursos y tiempo de respuesta inferior a 150 ms en interacción.'],
              ['RNF-02', 'Seguridad de Credenciales', 'Almacenamiento cifrado de tokens JWT y llaves de sesión utilizando Android Keystore / iOS Keychain.']
            ],
            [15, 25, 60]
          ),

          new Paragraph({ children: [new PageBreak()] }),

          // =========================================================================
          // ENTREGABLE 1 DE LA APLICACIÓN
          // =========================================================================
          h1('ENTREGABLE 1 DE LA APP MÓVIL: DISEÑO UI/UX, MAQUETACIÓN VISUAL, DESIGN SYSTEM Y VISTAS DE PANTALLA', COLOR_PRIMARY),
          
          callout(
            'Objetivo del Entregable 1',
            'Entregar la maquetación visual completa, el sistema de diseño nativo (Material 3 / Jetpack Compose) y las pantallas interactivas de la aplicación móvil de ventas, listas para recibir datos mock y navegar fluidamente.',
            COLOR_PRIMARY,
            'F0F9FF'
          ),

          h2('1.1 Sistema de Diseño Móvil (Design System SkillAcademy Mobile)'),
          p('Se implementa un tema nativo de alto contraste que asegura máxima visibilidad y atractivo comercial:'),
          bullet('Paleta de Colores', 'Primario: Navy Profundo (#0F2942); Acento Comercial: Esmeralda (#0D9488) para CTAs de compra y badges de descuento; Secundario: Cyan (#0284C7); Superficie: Blanco (#FFFFFF) y Slate Claro (#F8FAFC); Alertas: Rojo Coral (#EF4444); Estrellas de Review: Ámbar (#F59E0B).'),
          bullet('Tipografía Jerárquica', 'Familia Inter / Segoe UI optimizada para pantallas móviles: Título de Sección (22sp bold), Nombre de Curso (16sp semibold), Precio Destacado (18sp bold esmeralda), Precio Anterior (14sp tachado gris), Subtítulo (13sp regular).'),
          bullet('Componentes UI Reutilizables', 'Diseñados como composables independientes: SkillCourseCard, PromoBannerCarousel, CategoryChip, DiscountBadge, StickyBuyBar y RatingBar.'),

          h2('1.2 Arquitectura Visual de Pantallas y Navegación'),
          p('La estructura de navegación está gobernada por un Scaffold con barra de navegación inferior (Bottom Navigation Bar) fija con 4 pestañas esenciales:'),
          buildTable(
            ['Pestaña / Destino', 'Icono', 'Función Principal en la App Móvil de Ventas'],
            [
              ['Inicio (Home)', 'Home / Sparkles', 'Banners promocionales rotativos, cursos destacados, ofertas relámpago con cuenta regresiva.'],
              ['Catálogo', 'Grid / Search', 'Explorador completo de cursos con chips de categoría, barra de búsqueda y selector de nivel.'],
              ['Mis Cursos', 'Play / BookOpen', 'Acceso a los cursos ya comprados por el usuario, indicando porcentaje de avance.'],
              ['Perfil', 'User / Settings', 'Datos del estudiante, historial de pedidos, certificados obtenidos y ajustes.']
            ],
            [25, 20, 55]
          ),
          p('En la parte superior se incluye una TopAppBar global que muestra el logo de SkillAcademy y el icono de Carrito de Compras con indicador numérico animado (badge).'),

          h2('1.3 Maquetación Detallada de Pantallas de Venta'),
          
          h3('1.3.1 Pantalla Home Comercial'),
          p('Construida con un LazyColumn que incluye:'),
          bullet('Header Promocional', 'Carrusel horizontal con swipe táctil de banners (300dp de altura) con degradado visual y textos llamativos ("20% OFF en Excel", "Aprende a Programar").'),
          bullet('Categorías Rápidas', 'LazyRow con chips interactivos que filtran de inmediato el catálogo (Excel, Programación, Diseño, Productividad).'),
          bullet('Cursos Más Vendidos', 'Carrusel de tarjetas con imagen de portada, estrellas de valoración (ej. 4.9 ★), etiqueta "Más Vendido" y precio actual.'),
          bullet('Modal Popup Promocional', 'Diálogo emergente estilizado que se activa al abrir la app con cupones flash disponibles.'),

          h3('1.3.2 Pantalla de Ficha Técnica del Curso (Course Detail)'),
          bullet('Contenedor Multimedia', 'Área de video trailer en proporción 16:9 con botón de reproducción táctil.'),
          bullet('Detalles Académicos', 'Nombre del curso, nivel (Principiante / Intermedio / Avanzado), número de estudiantes inscritos y perfil del instructor.'),
          bullet('Temario Interactivo', 'Accordion expandible que desglosa los módulos y lecciones (reutilizando la estructura de curso_items y lecciones).'),
          bullet('Sticky Bottom Bar', 'Barra flotante inferior que nunca desaparece al hacer scroll, mostrando el precio del curso y los botones "Añadir al Carrito" y "Comprar Ya".'),

          h3('1.3.3 Pantalla de Carrito de Compras'),
          bullet('Lista de Cursos en Carrito', 'Cards con botón de swipe lateral para eliminar elementos rápidamente.'),
          bullet('Sección de Cupón', 'Campo de texto con botón "Aplicar" para validar descuentos promocionales.'),
          bullet('Desglose de Montos', 'Filas con Subtotal, Descuento Aplicado (en verde esmeralda) y Total Final.'),
          bullet('Botón de Pago', 'Botón ancho completo con fondo esmeralda: "Proceder al Checkout".'),

          h2('1.4 Código Representativo de Maquetación (Jetpack Compose)'),
          p('A continuación se ilustra la maquetación del componente reutilizable de tarjeta de venta:'),
          codeBox(
            'CourseCard.kt - Tarjeta Comercial Reutilizable',
`@Composable
fun SkillCourseCard(
    curso: CursoUiModel,
    onCourseClick: (Long) -> Unit,
    onAddToCartClick: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCourseClick(curso.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = curso.imagenUrl,
                    contentDescription = curso.titulo,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Crop
                )
                if (curso.oldPrecio != null && curso.oldPrecio > curso.precio) {
                    val descuento = ((1 - curso.precio / curso.oldPrecio) * 100).toInt()
                    Badge(
                        modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
                        containerColor = Color(0xFF0D9488)
                    ) {
                        Text("-$descuento%", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(curso.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(curso.instructor, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$" + curso.precio, style = MaterialTheme.typography.titleLarge, color = Color(0xFF0D9488), fontWeight = FontWeight.ExtraBold)
                    if (curso.oldPrecio != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$" + curso.oldPrecio, style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough), color = Color.Gray)
                    }
                }
            }
        }
    }
}`
          ),

          h2('1.5 Criterios de Aceptación y Validación del Entregable 1'),
          bullet('Cobertura de Vistas', 'Las 5 pantallas principales (Home, Catálogo, Ficha de Curso, Carrito, Mis Cursos) maquetadas al 100%.'),
          bullet('Adaptabilidad', 'Correcta visualización en resoluciones HD, FHD y densidades de pantalla desde 360dp hasta 600dp de ancho.'),
          bullet('Diseño Interactivo', 'Navegación fluida entre pantallas mediante Compose Navigation sin caídas de frame.'),

          new Paragraph({ children: [new PageBreak()] }),

          // =========================================================================
          // ENTREGABLE 2 DE LA APLICACIÓN
          // =========================================================================
          h1('ENTREGABLE 2 DE LA APP MÓVIL: FUNCIONALIDAD, CONEXIÓN CON APIS, PERSISTENCIA LOCAL Y MOTOR DE VENTA', COLOR_SECONDARY),
          
          callout(
            'Objetivo del Entregable 2',
            'Implementar la capa de lógica de negocio, arquitectura MVVM, conectividad con Supabase PostgreSQL y Fastify Elasticsearch, persistencia local con Room Database y el motor del carrito con validación reactiva de cupones.',
            COLOR_SECONDARY,
            'F0F9FF'
          ),

          h2('2.1 Arquitectura de Software y Capa de Datos Móvil'),
          p('Se estructura el código bajo el patrón Clean Architecture en tres capas desacopladas:'),
          bullet('Data Layer', 'Repositorios concretos (CoursesRepositoryImpl, CartRepositoryImpl, AuthRepositoryImpl) que interactúan con el cliente de Supabase (PostgREST), Fastify API y la base de datos local SQLite/Room.'),
          bullet('Domain Layer', 'Casos de uso atómicos (GetPublishedCoursesUseCase, SearchCoursesUseCase, ApplyCouponUseCase, AddToCartUseCase, CalculateCartTotalUseCase).'),
          bullet('Presentation Layer', 'ViewModels que administran StateFlow reactivos consumidos por las vistas de Jetpack Compose del Entregable 1.'),

          h2('2.2 Autenticación y Manejo Seguro de Sesiones'),
          p('La aplicación móvil se conecta al mismo módulo de autenticación de Supabase utilizado por la plataforma web:'),
          bullet('Inicio de Sesión y Registro', 'Autenticación con correo y contraseña. Al registrarse un nuevo usuario móvil, se ejecuta el trigger on_auth_user_created en PostgreSQL, creándole automáticamente su perfil en la tabla profiles con rol student.'),
          bullet('Almacenamiento Cifrado de Tokens', 'El JWT de sesión y el Refresh Token se guardan en EncryptedSharedPreferences respaldado por Android Keystore, garantizando protección contra clonación de sesiones.'),

          h2('2.3 Consumo de Catálogo y Búsqueda Predictiva'),
          p('La app implementa dos fuentes de consulta para la búsqueda y visualización de cursos:'),
          bullet('Consulta Principal a Supabase', 'Descarga de cursos con estado "Publicado": /rest/v1/cursos?estado=eq.Publicado&select=id,titulo,categoria,nivel,instructor,precio,old_precio,imagen,rating,reviews.'),
          bullet('Búsqueda Avanzada con Elasticsearch', 'Para la barra de búsqueda se consulta el endpoint /api/cursos/search del backend Fastify. Si el servicio de Elasticsearch experimenta latencia o caída, el repositorio conmuta automáticamente a una consulta con filtro ilike en Supabase, asegurando alta disponibilidad.'),

          h2('2.4 Motor del Carrito de Compras y Persistencia Local (Room DB)'),
          p('El carrito de compras debe persistir aun cuando la app se cierre o el teléfono se apague:'),
          bullet('Entidad Local CartItemEntity', 'Almacena id, cursoId, titulo, precio, imagen y fechaAgregado.'),
          bullet('Gestión Reactiva con Flow', 'El CartViewModel expone un StateFlow con la lista de ítems, el número de elementos para el badge superior y el cálculo automático del subtotal.'),

          h2('2.5 Motor de Cupones de Descuento en Tiempo Real'),
          p('Para la validación de cupones se implementa la lógica que conecta con la tabla cupones de Supabase:'),
          codeBox(
            'CouponValidator.kt - Validación y Recálculo Financiero',
`class CartViewModel(
    private val cartRepo: CartRepository,
    private val supabase: SupabaseClient
) : ViewModel() {
    private val _couponState = MutableStateFlow<CouponUiState>(CouponUiState.Idle)
    val couponState: StateFlow<CouponUiState> = _couponState.asStateFlow()

    fun applyCoupon(codigo: String) {
        viewModelScope.launch {
            _couponState.value = CouponUiState.Validating
            try {
                val cupon = supabase.from("cupones")
                    .select { filter { eq("codigo", codigo.trim().uppercase()); eq("activo", true) } }
                    .decodeSingleOrNull<CuponDto>()

                if (cupon != null) {
                    val porcentaje = cupon.descuento
                    _couponState.value = CouponUiState.Applied(codigo = cupon.codigo, porcentaje = porcentaje)
                } else {
                    _couponState.value = CouponUiState.Error("Cupón inválido o expirado")
                }
            } catch (e: Exception) {
                _couponState.value = CouponUiState.Error("Error al validar el cupón")
            }
        }
    }
}`
          ),

          h2('2.6 Criterios de Aceptación del Entregable 2'),
          bullet('Integración con Supabase', 'Descarga exitosa de cursos reales de la base de datos sin errores de serialización.'),
          bullet('Persistencia del Carrito', 'Los cursos agregados al carrito se mantienen intactos al reiniciar la aplicación.'),
          bullet('Cálculo de Cupones', 'Los cupones válidos descuentan exactamente el porcentaje registrado en la base de datos; los códigos erróneos emiten feedback claro al usuario.'),

          new Paragraph({ children: [new PageBreak()] }),

          // =========================================================================
          // ENTREGABLE 3 DE LA APLICACIÓN
          // =========================================================================
          h1('ENTREGABLE 3 DE LA APP MÓVIL: PASARELA DE PAGO, CHECKOUT TRANSACCIONAL, AULA MÓVIL, PRUEBAS Y DESPLIEGUE', COLOR_ACCENT),
          
          callout(
            'Objetivo del Entregable 3',
            'Completar el flujo transaccional de venta (checkout, simulación/pasarela de pagos, inserción de inscripciones en Supabase con RLS), activar el aula móvil post-compra (Mis Cursos con reproductor), ejecutar el plan integral de QA y generar los paquetes de producción (.aab/.apk) con pipeline de despliegue.',
            COLOR_ACCENT,
            'F0FDF4'
          ),

          h2('3.1 Proceso de Checkout Transaccional y Pasarela de Pagos'),
          p('El flujo de cierre de venta garantiza una experiencia de pago segura y sin fricciones:'),
          bullet('Verificación Previa de Sesión', 'Si el usuario no ha iniciado sesión al momento de presionar "Proceder al Pago", se abre una hoja inferior (BottomSheet) de autenticación rápida. Una vez autenticado, retorna inmediatamente al checkout sin perder los cursos del carrito.'),
          bullet('Selección de Medio de Pago', 'Soporta pasarela de tarjeta de crédito/débito tokenizada, Google Pay mediante Google Pay API y simulación educativa de pago instantáneo.'),
          bullet('Confirmación Biométrica', 'Integración con BiometricPrompt API para solicitar la huella dactilar o reconocimiento facial antes de efectuar la transacción.'),

          h2('3.2 Formalización de la Compra y Alta en Inscripciones'),
          p('Una vez aprobada la transacción, la app móvil formaliza la compra directamente en la base de datos de Supabase:'),
          bullet('Llamada Transaccional a inscripciones', 'Se ejecuta una inserción en la tabla inscripciones para cada curso adquirido: INSERT INTO inscripciones (usuario_id, curso_id, progreso) VALUES (auth.uid(), curso_id, 0).'),
          bullet('Validación por Políticas RLS', 'La política curso_items_select e inscripciones en PostgreSQL valida que el usuario autenticado (auth.uid()) sea el propietario del registro.'),
          bullet('Limpieza del Carrito y Pantalla de Éxito', 'Se vacía la base de datos local de Room y se muestra la pantalla de felicitaciones con animación Lottie y número de orden.'),

          h2('3.3 Aula Móvil Post-Venta: Pestaña "Mis Cursos"'),
          p('Inmediatamente después de la compra, el usuario puede comenzar a estudiar en su dispositivo:'),
          bullet('Listado de Cursos Inscritos', 'Consulta en tiempo real a Supabase: /rest/v1/inscripciones?usuario_id=eq.{id}&select=curso_id,progreso,cursos(*)'),
          bullet('Reproductor de Video Nativo (Media3 / ExoPlayer)', 'Reproducción fluida de las lecciones del curso con soporte para pausa, avance de 10 segundos y cambio de velocidad (1x, 1.25x, 1.5x).'),
          bullet('Progreso de Aprendizaje', 'Al finalizar cada lección se actualiza la tabla lecciones_vistas y el progreso general en la tabla inscripciones.'),

          h2('3.4 Notificaciones Push Promocionales'),
          p('Integración con Firebase Cloud Messaging (FCM):'),
          bullet('Alertas de Descuentos Flash', 'Cuando el administrador publica una nueva oferta o banner en la plataforma web, el backend dispara una notificación push a los dispositivos móviles para incentivar compras de impulso.'),
          bullet('Recordatorios de Carrito Abandonado', 'Si el usuario deja cursos en su carrito por más de 24 horas, la app genera una notificación local recordándole el descuento disponible.'),

          h2('3.5 Plan Integral de Pruebas de Calidad (QA)'),
          buildTable(
            ['Nivel de Prueba', 'Herramienta Empleada', 'Objetivo y Criterio de Éxito'],
            [
              ['Pruebas Unitarias', 'JUnit 5 + MockK', 'Validar cálculos matemáticos de totales de carrito con múltiples cupones y casos de prueba extremos.'],
              ['Pruebas de Integración', 'Ktor MockEngine', 'Verificar comportamiento de repositorios ante respuestas 401 Unauthorized y 502 de servicios.'],
              ['Pruebas E2E de Venta', 'Maestro / Compose Test', 'Simular el flujo completo: Búsqueda -> Selección de Curso -> Carrito -> Cupón -> Pago -> Curso visible en Mis Cursos.'],
              ['Pruebas de Rendimiento', 'Android Profiler', 'Comprobar que el uso de memoria no exceda 120 MB y no existan fugas en el reproductor de video.']
            ],
            [22, 28, 50]
          ),

          h2('3.6 Estrategia de Empaquetado, Publicación y Despliegue'),
          bullet('Generación de Paquete Final', 'Compilación de Android App Bundle (.aab) firmado con clave de producción protegida en almacén Keystore.'),
          bullet('Pipeline CI/CD en GitHub Actions', 'Construcción desatendida ante cada push a la rama main: ejecución de linters, pruebas automáticas y entrega hacia tracks de prueba cerrada en Google Play Console con Fastlane.'),
          bullet('Sincronización Web Continua', 'El manual de operación web incluido en la documentación asegura que cualquier curso, categoría o cupón creado en Angular impacte de inmediato en la app móvil sin necesidad de volver a compilar.'),

          h2('3.7 Criterios de Aceptación y Cierre del Entregable 3'),
          bullet('Venta de Punta a Punta', 'El usuario puede comprar un curso real o simulado y verlo instantáneamente habilitado en su cuenta.'),
          bullet('Estabilidad Garantizada', 'Tasa de sesiones libres de errores (Crash-free users) superior al 99.5% en fase de pruebas.'),
          bullet('Entregable Compilado', 'Archivo APK instalable y código fuente documentado y listo para distribución.'),

          new Paragraph({ children: [new PageBreak()] }),

          // =========================================================================
          // CONCLUSIONES Y MATRIZ RESUMEN
          // =========================================================================
          h1('MATRIZ DE CONSOLIDACIÓN DE LOS 3 ENTREGABLES'),
          p('A continuación se sintetiza la relación entre los 3 entregables de construcción de la aplicación móvil nativa:'),
          buildTable(
            ['Componente de la App Móvil', 'Entregable 1 (Diseño UI/UX)', 'Entregable 2 (Lógica & Datos)', 'Entregable 3 (Checkout & Aula)'],
            [
              ['Pantalla Home & Banners', 'Maquetación de carrusel y tarjetas.', 'Conexión a tabla banners en Supabase.', 'Notificación push al lanzar nuevo banner.'],
              ['Catálogo & Búsqueda', 'Diseño de chips y layout de lista.', 'Integración Fastify / Elasticsearch.', 'Filtros dinámicos en vivo y analytics.'],
              ['Ficha de Curso', 'Estructura de accordion y video frame.', 'Lectura de curso_items y reviews.', 'Inicio de video player tras compra.'],
              ['Carrito de Compras', 'Diseño de cards y botón checkout.', 'Persistencia Room DB y cupón dinámico.', 'Vaciado automático tras pago exitoso.'],
              ['Checkout y Pagos', 'Modal de selección de métodos.', 'Validación de usuario y total final.', 'Transacción de cobro y alta en inscripciones.'],
              ['Aula Móvil (Mis Cursos)', 'Diseño de cards de progreso.', 'Query de inscripciones del alumno.', 'Reproducción de lecciones con ExoPlayer.']
            ],
            [22, 26, 26, 26]
          ),
          new Paragraph({ spacing: { before: 200 } }),
          callout(
            'Conclusión del Proyecto',
            'Al estructurar la aplicación móvil nativa en estos 3 entregables consecutivos (Diseño Visual -> Lógica y Persistencia -> Checkout y Despliegue), se garantiza un desarrollo ordenado, verificable y modular. La plataforma web continúa siendo el corazón administrativo y de gestión de cursos de SkillAcademy, permitiendo que la aplicación móvil se enfoque con máxima pureza y rendimiento en la conversión comercial y la satisfacción del estudiante.',
            COLOR_PRIMARY,
            'F8FAFC'
          )
        ]
      }
    ]
  });

  const buffer = await Packer.toBuffer(doc);
  const outputPath = path.resolve(__dirname, '..', 'SkillAcademy_Mobile_Native_Entregables.docx');
  fs.writeFileSync(outputPath, buffer);
  console.log(`Documento Word actualizado exitosamente en: ${outputPath} (${buffer.length} bytes)`);
}

generateDocx().catch(err => {
  console.error('Error al generar el documento Word:', err);
  process.exit(1);
});
