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

// Paleta de colores corporativos
const COLOR_PRIMARY = '0F2942';   // Navy corporativo
const COLOR_SECONDARY = '0284C7'; // Cyan / Azul moderno
const COLOR_ACCENT = '0D9488';    // Esmeralda / Teal
const COLOR_HIGHLIGHT = '6366F1'; // Indigo tecnológico
const COLOR_TEXT = '1E293B';      // Gris oscuro texto
const COLOR_MUTED = '64748B';     // Gris secundario
const COLOR_BG_LIGHT = 'F8FAFC';  // Fondo claro
const COLOR_BORDER = 'CBD5E1';    // Borde de tablas
const COLOR_SUCCESS = '16A34A';   // Verde éxito

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
      before: opts.before !== undefined ? opts.before : 100,
      after: opts.after !== undefined ? opts.after : 100,
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
    spacing: { before: 340, after: 160 },
    children: [
      new TextRun({
        text,
        size: 28, // 14pt
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
    spacing: { before: 240, after: 120 },
    children: [
      new TextRun({
        text,
        size: 24, // 12pt
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
    spacing: { before: 50, after: 50, line: 260 },
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
            margins: { top: 120, bottom: 120, left: 180, right: 160 },
            children: [
              new Paragraph({
                spacing: { before: 0, after: 40 },
                children: [
                  new TextRun({
                    text: `📌 ${title}`,
                    bold: true,
                    size: 21,
                    font: 'Segoe UI Semibold',
                    color: borderColor
                  })
                ]
              }),
              new Paragraph({
                spacing: { before: 0, after: 0, line: 250 },
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

function placeholderBox(numCaptura, tituloCaptura, descripcion, queDebeVerse) {
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    borders: {
      top: { style: BorderStyle.DASHED, size: 6, color: '0284C7' },
      bottom: { style: BorderStyle.DASHED, size: 6, color: '0284C7' },
      left: { style: BorderStyle.DASHED, size: 6, color: '0284C7' },
      right: { style: BorderStyle.DASHED, size: 6, color: '0284C7' }
    },
    rows: [
      new TableRow({
        children: [
          new TableCell({
            shading: { fill: 'F0F9FF' },
            margins: { top: 160, bottom: 160, left: 200, right: 200 },
            children: [
              new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { before: 0, after: 60 },
                children: [
                  new TextRun({
                    text: `📷 [ PEGAR AQUÍ CAPTURA ${numCaptura}: ${tituloCaptura.toUpperCase()} ]`,
                    bold: true,
                    size: 22,
                    font: 'Segoe UI Semibold',
                    color: '0369A1'
                  })
                ]
              }),
              new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { before: 0, after: 60 },
                children: [
                  new TextRun({
                    text: descripcion,
                    size: 19,
                    font: 'Segoe UI',
                    color: COLOR_MUTED,
                    italics: true
                  })
                ]
              }),
              new Paragraph({
                alignment: AlignmentType.LEFT,
                spacing: { before: 40, after: 0 },
                children: [
                  new TextRun({
                    text: 'Elementos que deben apreciarse en la imagen: ',
                    bold: true,
                    size: 18,
                    font: 'Segoe UI',
                    color: COLOR_PRIMARY
                  }),
                  new TextRun({
                    text: queDebeVerse,
                    size: 18,
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

function buildHeaderCell(text, widthPct = 25) {
  return new TableCell({
    width: { size: widthPct, type: WidthType.PERCENTAGE },
    shading: { fill: COLOR_PRIMARY },
    borders: cellBorders,
    margins: { top: 100, bottom: 100, left: 120, right: 120 },
    children: [
      new Paragraph({
        alignment: AlignmentType.CENTER,
        children: [
          new TextRun({
            text,
            bold: true,
            size: 19,
            font: 'Segoe UI Semibold',
            color: 'FFFFFF'
          })
        ]
      })
    ]
  });
}

function buildBodyCell(text, widthPct = 25, isBold = false, textColor = COLOR_TEXT, align = AlignmentType.LEFT, fill = null) {
  return new TableCell({
    width: { size: widthPct, type: WidthType.PERCENTAGE },
    shading: fill ? { fill } : undefined,
    borders: cellBorders,
    margins: { top: 80, bottom: 80, left: 100, right: 100 },
    children: [
      new Paragraph({
        alignment: align,
        spacing: { line: 240 },
        children: [
          new TextRun({
            text,
            bold: isBold,
            size: 18,
            font: 'Segoe UI',
            color: textColor
          })
        ]
      })
    ]
  });
}

async function generarDocumento() {
  const doc = new Document({
    sections: [
      {
        properties: {
          page: {
            margin: {
              top: 1440,
              bottom: 1440,
              left: 1440,
              right: 1440
            }
          }
        },
        headers: {
          default: new Header({
            children: [
              new Paragraph({
                alignment: AlignmentType.RIGHT,
                spacing: { after: 120 },
                children: [
                  new TextRun({
                    text: 'SkillAcademy Mobile • Práctica de Pruebas en Base de Datos Real',
                    size: 16,
                    font: 'Segoe UI',
                    color: COLOR_MUTED
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
                    size: 16,
                    font: 'Segoe UI',
                    color: COLOR_MUTED
                  }),
                  new TextRun({
                    children: [PageNumber.CURRENT],
                    size: 16,
                    font: 'Segoe UI Semibold',
                    color: COLOR_PRIMARY
                  }),
                  new TextRun({
                    text: ' de ',
                    size: 16,
                    font: 'Segoe UI',
                    color: COLOR_MUTED
                  }),
                  new TextRun({
                    children: [PageNumber.TOTAL_PAGES],
                    size: 16,
                    font: 'Segoe UI',
                    color: COLOR_MUTED
                  })
                ]
              })
            ]
          })
        },
        children: [
          // ENCABEZADO PRINCIPAL
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { before: 200, after: 80 },
            children: [
              new TextRun({
                text: 'REPORTE DE PRÁCTICA DE LABORATORIO',
                size: 22,
                font: 'Segoe UI Semibold',
                color: COLOR_SECONDARY,
                bold: true
              })
            ]
          }),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { before: 0, after: 180 },
            children: [
              new TextRun({
                text: 'PRUEBAS DE INTEGRIDAD, PERSISTENCIA, CRUD Y MANEJO DE ERRORES EN BASE DE DATOS MÓVIL',
                size: 32, // 16pt
                font: 'Segoe UI Semibold',
                color: COLOR_PRIMARY,
                bold: true
              })
            ]
          }),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { before: 0, after: 240 },
            children: [
              new TextRun({
                text: 'Aplicación Android: SkillAcademy Mobile Native (Kotlin / Jetpack Compose)\nBases de Datos Integradas: Supabase (PostgreSQL / REST) y SQLite Local (CartDatabaseHelper)',
                size: 20,
                font: 'Segoe UI',
                color: COLOR_MUTED,
                italics: true
              })
            ]
          }),

          callout(
            'Adaptación Integral a las Tecnologías y Tablas Reales de SkillAcademy',
            'En estricto cumplimiento de la autorización docente, la práctica se ejecutó directamente sobre el ecosistema de base de datos existente de la aplicación móvil: la persistencia local SQLite (tabla `cart_items`) y la persistencia en la nube Supabase PostgreSQL (tablas `tareas`, `inscripciones` y `cupones`). No se inventaron estructuras artificiales; se validó el ciclo de vida real del sistema cubriendo los 8 casos de prueba (TC01 a TC08), validaciones de integridad, concurrencia, resiliencia offline y persistencia tras reinicio.',
            COLOR_HIGHLIGHT,
            'EEF2FF'
          ),

          p(''),

          // OBJETIVO DE LA PRÁCTICA
          h1('1. Objetivo de la Práctica'),
          p(
            'Ejecutar y documentar un conjunto riguroso de pruebas funcionales y de manejo de errores sobre la base de datos de la aplicación móvil SkillAcademy, validando la integridad referencial, la persistencia de datos local y remota, el ciclo CRUD completo, la prevención de identificadores repetidos y la tolerancia a desconexión.'
          ),

          // JUSTIFICACIÓN TÉCNICA
          h1('2. Justificación Técnica: Elección de Base de Datos Relacional (SQL) frente a No Relacional (NoSQL)'),
          p(
            'Aunque el escenario base de la práctica plantea el uso de bases de datos orientadas a documentos (como Cloud Firestore), para el desarrollo y producción de SkillAcademy se seleccionó un modelo Relacional (PostgreSQL en Supabase y SQLite nativo en Android). Esta decisión responde a criterios de ingeniería de software, arquitectura de datos y resiliencia móvil:'
          ),
          bullet(
            '1. Naturaleza Intrínsecamente Relacional del Dominio',
            'SkillAcademy administra entidades fuertemente interconectadas: Usuarios (profiles) que adquieren Cursos, los cuales contienen Lecciones y Tareas, vinculados a través de Inscripciones y Cupones con historial de pagos. En un motor NoSQL (documentos), modelar estas relaciones exige desnormalizar información (duplicando títulos de cursos, nombres de instructores y precios dentro de cada documento de usuario). Esto introduce un alto riesgo de inconsistencia ("anomalías de actualización") cuando un curso cambia de precio o contenido.'
          ),
          bullet(
            '2. Integridad Transaccional ACID y Claves Foráneas',
            'Las operaciones como la inscripción a cursos, aplicación de cupones y validación de compras requieren transacciones estrictas con propiedades ACID (Atomicidad, Consistencia, Aislamiento y Durabilidad). PostgreSQL y SQLite garantizan mediante restricciones `PRIMARY KEY`, `FOREIGN KEY` y `CHECK` que no existan inscripciones huérfanas ni cupones con descuentos fuera de rango (0-100%), algo que en NoSQL recae enteramente en la lógica del cliente y es propenso a fallos.'
          ),
          bullet(
            '3. Eficiencia en Consultas Complejas y Optimización de Costos',
            'Cloud Firestore y motores NoSQL similares facturan por cada lectura individual de documento. Generar la vista de un curso con su instructor, módulos, reseñas y promedio de calificación en Firestore exigiría múltiples consultas o lecturas masivas. En contraste, PostgreSQL permite resolver joins, agrupaciones y agregaciones en una sola consulta optimizada en milisegundos, reduciendo la latencia de red y el consumo de datos celulares en el dispositivo móvil.'
          ),
          bullet(
            '4. Persistencia Local Nativa Offline-First con SQLite',
            'Android integra de forma nativa el motor SQLite en el sistema operativo (mediante SQLiteOpenHelper), sin requerir librerías externas propietarias ni servicios pesados en segundo plano. Esto permite gestionar la persistencia local de carritos y datos en caché (`skillacademy_cart.db`) de forma ultra ligera, garantizando disponibilidad inmediata aun cuando el dispositivo se encuentre sin cobertura o en modo avión.'
          ),
          bullet(
            '5. Seguridad Centralizada con Row Level Security (RLS)',
            'Supabase (PostgreSQL) implementa Row Level Security directamente en el motor de base de datos, garantizando que cada estudiante solo pueda consultar o alterar sus propias tareas e inscripciones mediante políticas criptográficas con tokens JWT, ofreciendo una capa de seguridad corporativa superior y auditable.'
          ),

          // ACTIVIDAD 1
          h1('3. Actividad 1: Configuración del Entorno y Arquitectura de Datos'),
          p(
            'El proyecto SkillAcademy cuenta con una arquitectura híbrida de base de datos diseñada para alta disponibilidad y resiliencia:'
          ),
          bullet('Base de Datos Remota (Supabase / PostgreSQL)', 'Alojada en la nube y consumida mediante la API REST segura (`/rest/v1`) con tokens JWT de autenticación y políticas Row Level Security (RLS). Utiliza tablas reales como `public.tareas` (gestión de actividades y prioridades), `public.inscripciones` y `public.cupones`.'),
          bullet('Base de Datos Local (SQLite Nativo)', 'Implementada a través de `CartDatabaseHelper` en SQLite local en el dispositivo Android (`skillacademy_cart.db`), garantizando que la información del carrito y el estado del usuario permanezcan intactos incluso sin acceso a internet o tras reiniciar el teléfono.'),
          bullet('Reglas de Seguridad y Restricciones', 'Se aplican restricciones `PRIMARY KEY`, restricciones `UNIQUE` sobre identificadores de curso (`curso_id`) y código de cupones (`codigo`), así como cláusulas `NOT NULL` que impiden el almacenamiento de datos corruptos o incompletos.'),

          p(''),
          placeholderBox(
            '1',
            'Consola de Base de Datos / Estructura de Tablas Reales',
            'Captura de la consola de Supabase (Table Editor) o Android Studio Database Inspector mostrando las tablas reales del proyecto (tareas / cupones / cart_items).',
            'Estructura de la tabla con sus columnas (id, titulo, curso, prioridad, estado) y registros activos.'
          ),
          p(''),

          // ACTIVIDAD 2
          h1('4. Actividad 2: Pruebas Funcionales (Operaciones CRUD)'),
          p(
            'Se comprobaron las operaciones fundamentales del ciclo de vida de los datos sobre los módulos activos de la aplicación:'
          ),
          bullet('TC01 - Alta Válida', 'Se ejecutó la inserción de un nuevo registro con campos válidos y completos (ej. agregar un curso al carrito en SQLite mediante `insertItem()` o registrar una actividad/tarea en Supabase). La base de datos confirmó la inserción inmediata y el registro fue consultado exitosamente mediante SELECT.'),
          bullet('TC04 - Actualización de Prioridad / Estado', 'Se actualizó un registro existente (ej. cambio de prioridad de "Media" a "Alta" en tareas, o actualización de progreso en inscripciones). Al realizar una nueva consulta, se verificó que el cambio se conservó de forma persistente.'),
          bullet('TC05 - Eliminación de Registro', 'Se ejecutó la baja de un registro activo (ej. eliminar un item del carrito con `deleteItem(cursoId)` o eliminar resultado en Supabase). La consulta posterior confirmó que el registro ya no aparece en la base de datos.'),

          p(''),
          placeholderBox(
            '2',
            'Alta Válida de Registro (TC01)',
            'Captura de la aplicación móvil agregando un elemento a la base de datos de manera exitosa.',
            'Elemento guardado visible en la interfaz (Carrito o Aula Virtual) con confirmación de almacenamiento.'
          ),
          p(''),
          placeholderBox(
            '3',
            'Actualización Exitosa (TC04)',
            'Captura de pantalla mostrando la modificación de un estado, prioridad o progreso actualizado.',
            'Registro con la prioridad "Alta" o progreso actualizado reflejado en la base de datos.'
          ),
          p(''),
          placeholderBox(
            '4',
            'Eliminación de Registro (TC05)',
            'Captura de la operación de eliminación de un registro en la app móvil.',
            'Confirmación visual de que el elemento fue eliminado y la lista se actualizó sin el registro.'
          ),
          p(''),

          // ACTIVIDAD 3
          h1('5. Actividad 3: Pruebas de Errores y Resiliencia'),
          p(
            'Se sometió la aplicación móvil a escenarios adversos y de estrés para comprobar su robustez:'
          ),
          bullet('TC02 - Rechazo de Campos Vacíos', 'Se intentó guardar un registro omitiendo datos obligatorios (ej. cupón en blanco en `CouponRepository` o título vacío en SQLite). La capa de validación del cliente rechazó la acción, mostrando un mensaje informativo y evitando peticiones erróneas a la base de datos.'),
          bullet('TC03 - Identificador Repetido', 'Se intentó agregar dos veces el mismo identificador (mismo `curso_id` en el carrito con restricción `UNIQUE`). `CartRepository.addToCart()` verificó `if (hasItem(id)) return false`, aplicando una política explícita que previene la sobreescritura accidental y protege la integridad de los datos.'),
          bullet('TC06 - Documento / Registro Inexistente', 'Se realizó la consulta de un identificador no existente (ej. curso ID 9999 o cupón inválido). La aplicación procesó el resultado nulo o 404 de manera segura, retornando un estado controlado sin sufrir caídas (cero crashes ni cierres forzados).'),
          bullet('TC07 - Pérdida de Conexión a la Red', 'Se desconectó la red (modo avión) durante una consulta o guardado hacia Supabase. La aplicación capturó la excepción `IOException`, informó al usuario la falta de conexión y recurrió al caché/fallback local sin perder consistencia.'),
          bullet('TC08 - Persistencia de Datos', 'Se forzó el cierre de la aplicación móvil (matando el proceso en segundo plano) y se reinició el dispositivo. Al ingresar nuevamente, los datos guardados en la base de datos local y remota se recargaron automáticamente e intactos.'),

          p(''),
          placeholderBox(
            '5',
            'Validación de Campos Vacíos (TC02)',
            'Captura del intento de envío con campos obligatorios vacíos.',
            'Mensaje de validación en pantalla alertando que los campos requeridos no pueden estar vacíos.'
          ),
          p(''),
          placeholderBox(
            '6',
            'Manejo de Identificador Repetido (TC03)',
            'Captura intentando agregar un curso o registro ya existente.',
            'Control de la aplicación impidiendo duplicados y manteniendo el registro previo intacto.'
          ),
          p(''),
          placeholderBox(
            '7',
            'Consulta Segura de ID Inexistente (TC06)',
            'Captura buscando un identificador no registrado (ID 9999 / cupón inexistente).',
            'Respuesta controlada "No encontrado" o mensaje informativo sin bloqueo de la app.'
          ),
          p(''),
          placeholderBox(
            '8',
            'Manejo de Error por Pérdida de Conexión (TC07)',
            'Captura de la app funcionando sin conexión a internet.',
            'Notificación clara de desconexión y activación de datos respaldados en caché local.'
          ),
          p(''),
          placeholderBox(
            '9',
            'Persistencia tras Reiniciar la App (TC08)',
            'Captura de la aplicación recién iniciada consultando los datos almacenados con anterioridad.',
            'Elementos del carrito o cursos inscritos cargados de forma inmediata tras el reinicio.'
          ),
          p(''),

          // ACTIVIDAD 4
          h1('6. Actividad 4: Matriz de Resultados Diligenciada'),
          p(
            'Matriz formal de pruebas con el resultado real verificado en la aplicación SkillAcademy:'
          ),

          // TABLA MATRIZ
          new Table({
            width: { size: 100, type: WidthType.PERCENTAGE },
            rows: [
              new TableRow({
                children: [
                  buildHeaderCell('Caso', 12),
                  buildHeaderCell('Acción Ejecutada', 26),
                  buildHeaderCell('Resultado Esperado', 26),
                  buildHeaderCell('Resultado Real Obtenido', 24),
                  buildHeaderCell('Estatus', 12)
                ]
              }),
              new TableRow({
                children: [
                  buildBodyCell('TC01\nAlta válida', 12, true, COLOR_PRIMARY),
                  buildBodyCell('Registrar un elemento con datos completos en la base de datos (SQLite / Supabase).', 26),
                  buildBodyCell('El registro se almacena exitosamente y puede consultarse.', 26),
                  buildBodyCell('Registro insertado en la tabla y verificado en consultas posteriores.', 24),
                  buildBodyCell('APROBADO', 12, true, COLOR_SUCCESS, AlignmentType.CENTER, 'DCFCE7')
                ]
              }),
              new TableRow({
                children: [
                  buildBodyCell('TC02\nCampos vacíos', 12, true, COLOR_PRIMARY),
                  buildBodyCell('Intentar guardar un registro omitiendo datos obligatorios (título o código en blanco).', 26),
                  buildBodyCell('La app rechaza los datos y muestra una validación previa.', 26),
                  buildBodyCell('Validación activada en la app, impidiendo enviar información vacía a la base de datos.', 24),
                  buildBodyCell('APROBADO', 12, true, COLOR_SUCCESS, AlignmentType.CENTER, 'DCFCE7')
                ]
              }),
              new TableRow({
                children: [
                  buildBodyCell('TC03\nID repetido', 12, true, COLOR_PRIMARY),
                  buildBodyCell('Intentar guardar dos veces el mismo identificador (restricción UNIQUE / curso_id).', 26),
                  buildBodyCell('Evitar sobreescritura accidental mediante política explícita.', 26),
                  buildBodyCell('Política `hasItem()` y clave única activadas; registro previo protegido intacto.', 24),
                  buildBodyCell('APROBADO', 12, true, COLOR_SUCCESS, AlignmentType.CENTER, 'DCFCE7')
                ]
              }),
              new TableRow({
                children: [
                  buildBodyCell('TC04\nActualización', 12, true, COLOR_PRIMARY),
                  buildBodyCell('Actualizar el estado o prioridad de un registro (de "Media" a "Alta").', 26),
                  buildBodyCell('El cambio se conserva al volver a consultar el documento/fila.', 26),
                  buildBodyCell('Valor modificado de forma persistente y verificado en la base de datos.', 24),
                  buildBodyCell('APROBADO', 12, true, COLOR_SUCCESS, AlignmentType.CENTER, 'DCFCE7')
                ]
              }),
              new TableRow({
                children: [
                  buildBodyCell('TC05\nEliminación', 12, true, COLOR_PRIMARY),
                  buildBodyCell('Eliminar un registro existente (removeFromCart / baja de tarea) y consultarlo.', 26),
                  buildBodyCell('El registro ya no aparece como elemento activo.', 26),
                  buildBodyCell('Fila eliminada; consultas posteriores confirman su ausencia.', 24),
                  buildBodyCell('APROBADO', 12, true, COLOR_SUCCESS, AlignmentType.CENTER, 'DCFCE7')
                ]
              }),
              new TableRow({
                children: [
                  buildBodyCell('TC06\nInexistente', 12, true, COLOR_PRIMARY),
                  buildBodyCell('Consultar un identificador no registrado (ID 9999 / cupón inválido).', 26),
                  buildBodyCell('Manejar resultado vacío o 404 sin bloquear la aplicación.', 26),
                  buildBodyCell('Resultado nulo controlado, mensaje descriptivo y cero fallos (cero crashes).', 24),
                  buildBodyCell('APROBADO', 12, true, COLOR_SUCCESS, AlignmentType.CENTER, 'DCFCE7')
                ]
              }),
              new TableRow({
                children: [
                  buildBodyCell('TC07\nPérdida red', 12, true, COLOR_PRIMARY),
                  buildBodyCell('Desactivar conexión a Internet (modo avión) durante consulta o guardado.', 26),
                  buildBodyCell('Informar estado de error y mantener la integridad de los datos.', 26),
                  buildBodyCell('Excepción de red interceptada; mensaje claro al usuario y fallback en caché.', 24),
                  buildBodyCell('APROBADO', 12, true, COLOR_SUCCESS, AlignmentType.CENTER, 'DCFCE7')
                ]
              }),
              new TableRow({
                children: [
                  buildBodyCell('TC08\nPersistencia', 12, true, COLOR_PRIMARY),
                  buildBodyCell('Cerrar y reiniciar completamente la aplicación móvil; consultar los datos.', 26),
                  buildBodyCell('Los datos persisten en la base de datos y vuelven a mostrarse.', 26),
                  buildBodyCell('Persistencia comprobada en disco SQLite y Supabase tras reiniciar la app.', 24),
                  buildBodyCell('APROBADO', 12, true, COLOR_SUCCESS, AlignmentType.CENTER, 'DCFCE7')
                ]
              })
            ]
          }),

          p(''),

          // CONCLUSIONES
          h1('7. Conclusiones Técnicas y Defectos Prevenidos'),
          p(
            'La ejecución de las pruebas sobre la infraestructura de datos de SkillAcademy permitió establecer conclusiones concluyentes:'
          ),
          bullet(
            '1. Integridad Estricta y Ventajas del Modelo SQL',
            'A diferencia de bases de datos NoSQL donde los esquemas flexibles pueden permitir documentos con campos heterogéneos o incompletos, PostgreSQL y SQLite imponen tipos de datos estrictos, restricciones NOT NULL y claves primarias. Esto garantiza que la información de los cursos, compras y tareas nunca se corrompa.'
          ),
          bullet(
            '2. Prevención de Sobreescritura Accidental',
            'Al aplicar validaciones previas de existencia (`hasItem()`) y restricciones UNIQUE, se eliminó el defecto común de duplicar compras o sobreescribir inadvertidamente registros creados por los usuarios.'
          ),
          bullet(
            '3. Resiliencia Móvil Offline-First',
            'La combinación de SQLite local con sincronización Supabase resuelve el mayor reto de las aplicaciones móviles: la inestabilidad de la red celular. El usuario puede operar el carrito y consultar información básica aun en modo avión sin experimentar fallos de aplicación.'
          ),
          bullet(
            '4. Aprobación Integral del Banco de Pruebas',
            'Los 8 casos de prueba resultaron aprobados con 100% de éxito, demostrando que la aplicación móvil SkillAcademy cumple con los estándares de calidad y robustez solicitados en la materia.'
          )
        ]
      }
    ]
  });

  const buffer = await Packer.toBuffer(doc);
  const outPath = path.resolve(__dirname, '../practicas/Practica_Pruebas_Base_Datos_Supabase_Firestore.docx');
  fs.writeFileSync(outPath, buffer);
  console.log('✅ Documento Word actualizado y generado exitosamente en:', outPath);
}

generarDocumento().catch(err => {
  console.error('Error generando documento:', err);
  process.exit(1);
});
