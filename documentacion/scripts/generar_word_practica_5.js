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
const COLOR_ACCENT = '0D9488';    // Esmeralda / Teal de seguridad
const COLOR_HIGHLIGHT = '6366F1'; // Indigo tecnológico
const COLOR_TEXT = '1E293B';      // Gris oscuro texto
const COLOR_MUTED = '64748B';     // Gris secundario
const COLOR_BG_LIGHT = 'F8FAFC';  // Fondo claro
const COLOR_BORDER = 'CBD5E1';    // Borde de tablas
const COLOR_CODE_BG = '0F172A';   // Fondo oscuro de código
const COLOR_CODE_BODY = '1E293B';

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
              }),
              new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { before: 180, after: 180 },
                children: [
                  new TextRun({
                    text: '(Haz clic aquí y presiona Ctrl + V para pegar la captura de pantalla)',
                    size: 18,
                    font: 'Segoe UI',
                    color: '0284C7',
                    bold: true
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
      margins: { top: 100, bottom: 100, left: 100, right: 100 },
      children: [
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [
            new TextRun({
              text: h,
              bold: true,
              size: 19,
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
      margins: { top: 90, bottom: 90, left: 100, right: 100 },
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
            shading: { fill: COLOR_CODE_BG },
            margins: { top: 70, bottom: 70, left: 120, right: 120 },
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
            shading: { fill: COLOR_CODE_BODY },
            margins: { top: 90, bottom: 90, left: 120, right: 120 },
            children: lines.map(line => new Paragraph({
              spacing: { before: 0, after: 0, line: 220 },
              children: [
                new TextRun({
                  text: line || ' ',
                  size: 17,
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

async function main() {
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
                    text: 'SkillAcademy | Práctica 5: Código Seguro en Aplicaciones Móviles',
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
          new Paragraph({ spacing: { before: 400 } }),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { before: 100, after: 80 },
            children: [
              new TextRun({
                text: 'SKILLACADEMY MOBILE',
                size: 38,
                bold: true,
                color: COLOR_PRIMARY,
                font: 'Segoe UI Black'
              })
            ]
          }),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { before: 0, after: 200 },
            children: [
              new TextRun({
                text: 'REPORTE DE LABORATORIO DE CÓDIGO SEGURO',
                size: 24,
                bold: true,
                color: COLOR_ACCENT,
                font: 'Segoe UI Semibold'
              })
            ]
          }),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { before: 0, after: 300 },
            children: [
              new TextRun({
                text: 'PRÁCTICA 5: Prácticas de Código Seguro en Aplicaciones Móviles\nMódulo de Autenticación Defensiva "AccesoSeguro"',
                size: 22,
                color: COLOR_SECONDARY,
                bold: true
              })
            ]
          }),

          buildTable(
            ['Metadato', 'Detalle'],
            [
              ['Asignatura:', 'Desarrollo de Aplicaciones Móviles / Seguridad de Software'],
              ['Plataforma:', 'Android (Android Studio, Kotlin, Jetpack Compose, Material 3)'],
              ['Proyecto:', 'SkillAcademy Android — Módulo de Acceso Seguro'],
              ['Fecha:', 'Octubre 2026'],
              ['Estándar de Seguridad:', 'OWASP Mobile Top 10 / MASVS (Mobile Application Security Verification Standard)']
            ],
            [35, 65],
            COLOR_PRIMARY
          ),

          new Paragraph({ spacing: { before: 200, after: 200 } }),
          new Paragraph({ children: [new PageBreak()] }),

          // ================= SECCIÓN 1: OBJETIVO Y CONTEXTO =================
          h1('1. Objetivo de la Práctica'),
          p('Implementar y comprobar medidas básicas de seguridad en una aplicación móvil Android, mediante la validación rigurosa de entradas en la interfaz del cliente, el manejo seguro de credenciales en memoria, el ocultamiento visual de información confidencial y la prevención activa de la exposición de datos sensibles en los registros de ejecución (Logcat) y mensajes de error del sistema.'),

          h1('2. Descripción del Caso y Vulnerabilidades Resueltas'),
          p('Durante la revisión y auditoría técnica de una versión previa del módulo de inicio de sesión de la aplicación móvil interna, se identificaron tres fallos de seguridad importantes:'),
          bullet('Vulnerabilidad 1 (Falta de Validación de Entradas)', 'La aplicación aceptaba campos vacíos o compuestos únicamente por espacios en blanco, además de cadenas arbitrariamente extensas sin límite superior, lo cual exponía la app a anomalías de lógica, denegación de servicio local por sobrecarga de memoria o inyecciones.'),
          bullet('Vulnerabilidad 2 (Fuga de Credenciales en Registros)', 'Se imprimían contraseñas y datos confidenciales en texto plano en la consola de Logcat, haciéndolos legibles para cualquier proceso de depuración ADB o software de diagnóstico.'),
          bullet('Vulnerabilidad 3 (Mensajes de Error Técnicos y Detallados)', 'Las respuestas del servidor o del cliente indicaban si el correo existía o no, o revelaban detalles de excepciones internas, posibilitando ataques de enumeración de usuarios (User Enumeration).'),
          p('Para mitigar estas vulnerabilidades se desarrolló e integró en la aplicación móvil de SkillAcademy la arquitectura AccesoSeguro, que aplica directrices de código seguro basadas en el estándar internacional OWASP MASVS.'),

          new Paragraph({ spacing: { before: 100, after: 100 } }),
          callout(
            'Principio de Seguridad Aplicado',
            'Defensa en Profundidad (Defense-in-Depth): Las validaciones del lado del cliente protegen la experiencia del usuario y evitan envíos basura inmediatos, mientras que el resguardo de Logcat y los mensajes genéricos blindan la privacidad de las credenciales frente a atacantes.'
          ),

          new Paragraph({ children: [new PageBreak()] }),

          // ================= SECCIÓN 3: MATRIZ DE REQUISITOS =================
          h1('3. Especificación de Requisitos y Mecanismos de Seguridad'),
          p('A continuación se detalla cómo se satisfacen los 7 requerimientos de la práctica dentro de la aplicación:'),

          buildTable(
            ['N°', 'Requisito de la Práctica', 'Implementación en SkillAcademy', 'Mecanismo de Seguridad'],
            [
              [
                '1',
                'Mostrar campos para usuario y contraseña',
                'Campos OutlinedTextField interactivos con tipografía y soporte de foco reactivo.',
                'Aislamiento de inputs con controles de teclado adaptados (Email / Password).'
              ],
              [
                '2',
                'Validar que ambos campos contengan información',
                'Validación con email.trim().isEmpty() y password.isEmpty(). Rechazo explícito de solo espacios.',
                'Impide el envío de cadenas vacías o compuestas de caracteres invisibles / espacios.'
              ],
              [
                '3',
                'Rechazar entradas que superen longitud definida',
                'Usuario: Máx 50 caracteres (MAX_USER_LENGTH = 50). Contraseña: Mín 8 y Máx 32 (MAX_PASSWORD_LENGTH = 32).',
                'Previene ataques de desbordamiento lógico, saturación de memoria RAM y claves débiles.'
              ],
              [
                '4',
                'Ocultar caracteres de la contraseña',
                'PasswordVisualTransformation() activado por defecto con alternador de visibilidad.',
                'Mitigación contra espionaje visual en el entorno físico (Shoulder Surfing).'
              ],
              [
                '5',
                'Mensajes de error genéricos',
                'mapAuthError() devuelve "Usuario o contraseña incorrectos", sin trazas de servidor.',
                'Evita enumeración de usuarios (OWASP A07: Identification Failures).'
              ],
              [
                '6',
                'Evitar imprimir contraseñas en Logcat',
                'Auditoría con tag AccesoSeguro; la clave se mantiene en memoria y NUNCA se escribe en Logcat.',
                'Cumplimiento con OWASP MASVS-STORAGE (Cero credenciales en logs).'
              ],
              [
                '7',
                'Mensaje de acceso simulado válido',
                'Reconocimiento seguro de datos de prueba (usuario_prueba / Seguro1234#) emitiendo Toast.',
                'Validación de flujo de acceso funcional y simulado sin comprometer el backend.'
              ]
            ],
            [6, 32, 34, 28],
            COLOR_PRIMARY
          ),

          new Paragraph({ spacing: { before: 150, after: 150 } }),
          new Paragraph({ children: [new PageBreak()] }),

          // ================= SECCIÓN 4: EVIDENCIAS Y CAPTURAS =================
          h1('4. Evidencias de Pruebas y Colocación de Capturas'),
          p('A continuación se disponen los 4 recuadros designados para insertar las capturas de pantalla de la aplicación ejecutándose en el emulador de Android Studio:'),

          h2('4.1. Captura de la Interfaz en el Emulador'),
          p('Muestra la pantalla de inicio de sesión de AccesoSeguro en estado inicial con los campos limpios y los contadores de longitud máxima visibles:'),
          placeholderBox(
            '1',
            'Interfaz de Inicio de Sesión en el Emulador',
            'Pantalla principal del módulo AccesoSeguro ejecutándose en Android Studio Emulator.',
            'Campo "Usuario o Email" con contador (0/50), campo "Contraseña" oculto con contador (0/32) y diseño con estilos de SkillAcademy.'
          ),

          new Paragraph({ spacing: { before: 200, after: 200 } }),

          h2('4.2. Captura de Prueba con Entradas Inválidas'),
          p('Comprueba el rechazo de entradas erróneas: usuario con puros espacios en blanco y contraseña con longitud insuficiente menor a 8 caracteres:'),
          placeholderBox(
            '2',
            'Prueba de Seguridad con Entradas Inválidas',
            'Demostración de los mensajes de advertencia ante entradas no permitidas.',
            'Texto en rojo: "El usuario no puede estar vacío ni contener solo espacios" o "La contraseña debe tener al menos 8 caracteres", con el botón de acción bloqueado.'
          ),

          new Paragraph({ children: [new PageBreak()] }),

          h2('4.3. Captura de Acceso Simulado Concedido'),
          p('Se introducen las credenciales de prueba seguras (ej. usuario_prueba con contraseña Seguro1234#) y la aplicación emite el mensaje visual Toast de acceso concedido:'),
          placeholderBox(
            '3',
            'Mensaje de Acceso Simulado Exitoso',
            'Comprobación del requisito 7 con datos de prueba válidos.',
            'Toast visible en el emulador: "¡Acceso simulado concedido! Bienvenido al sistema seguro." y redirección exitosa.'
          ),

          new Paragraph({ spacing: { before: 200, after: 200 } }),

          h2('4.4. Captura de Auditoría Logcat Sin Credenciales'),
          p('Comprueba en la consola Logcat de Android Studio filtrada por tag:AccesoSeguro que la autenticación se audita sin imprimir nunca la contraseña:'),
          placeholderBox(
            '4',
            'Logcat Demostrando Ausencia de Credenciales',
            'Auditoría en tiempo real en la pestaña Logcat de Android Studio.',
            'Filtro tag:AccesoSeguro mostrando: "Validando usuario (longitud: X car.)" y "MEDIDA DE SEGURIDAD: La contraseña se procesa en memoria y NUNCA se registra en Logcat".'
          ),

          new Paragraph({ children: [new PageBreak()] }),

          // ================= SECCIÓN 5: CÓDIGO FUENTE =================
          h1('5. Código Fuente con Comentarios Explicativos'),

          h2('5.1. Archivo MainActivity.kt (Entregable 2)'),
          p('El archivo MainActivity.kt documenta formalmente la arquitectura de seguridad y las reglas aplicadas en el ciclo de vida de la aplicación:'),

          codeBox(
            'MainActivity.kt — Documentación de Reglas de Código Seguro',
`package com.skillacademy.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.skillacademy.mobile.ui.screens.MainAppScreen
import com.skillacademy.mobile.ui.theme.SkillAcademyTheme

/**
 * =========================================================================================
 * PRÁCTICA 5: Prácticas de código seguro en aplicaciones móviles (AccesoSeguro)
 * =========================================================================================
 * 
 * REGLAS Y VALIDACIONES DE SEGURIDAD IMPLEMENTADAS EN LA APLICACIÓN:
 * 
 * 1. Campos de Entrada (Usuario y Contraseña):
 *    - Presentación de campos dedicados con controles de foco e interfaz reactiva (Jetpack Compose).
 * 
 * 2. Validación de Contenido Obligatorio y Prevención de Bypass de Espacios:
 *    - Se valida que ningún campo esté vacío.
 *    - Se rechazan entradas compuestas únicamente por espacios en blanco mediante trim().isEmpty().
 * 
 * 3. Restricción y Rechazo por Longitud Definida (Defensa en Profundidad):
 *    - Campo Usuario / Email: Límite máximo definido de 50 caracteres (MAX_USER_LENGTH = 50).
 *      Evita desbordamientos lógicos, inyecciones y saturación de búferes o memoria.
 *    - Campo Contraseña: Longitud mínima obligatoria de 8 caracteres (MIN_PASSWORD_LENGTH = 8)
 *      y longitud máxima de 32 caracteres (MAX_PASSWORD_LENGTH = 32).
 *      Se previene el uso de contraseñas cortas o triviales vulnerables a fuerza bruta.
 * 
 * 4. Ocultamiento Visual de Credenciales:
 *    - Uso de PasswordVisualTransformation() para enmascarar los caracteres en pantalla,
 *      mitigando riesgos de hombros curiosos (shoulder surfing).
 * 
 * 5. Mensajes de Error Genéricos (Prevención de Enumeración):
 *    - En caso de credenciales inválidas, el sistema emite respuestas neutras como
 *      "Usuario o contraseña incorrectos", evitando revelar si el usuario existe o no
 *      en la base de datos (OWASP A07: Identification and Authentication Failures).
 * 
 * 6. Prevención de Fuga de Credenciales en Registros (Logcat):
 *    - Se audita el evento con el tag "AccesoSeguro" indicando longitudes o identificadores
 *      parcialmente anonimizados, garantizando que contraseñas en texto plano y tokens
 *      NUNCA se impriman en Logcat (OWASP MASVS-STORAGE / Sensitive Data Exposure).
 * 
 * 7. Acceso Simulado y Verificación de Credenciales de Prueba:
 *    - Reconocimiento seguro de cuentas de prueba (ej. usuario_prueba / admin@seguro.com)
 *      con retroalimentación visual de "¡Acceso simulado concedido!" ante credenciales válidas.
 * =========================================================================================
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SkillAcademyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen()
                }
            }
        }
    }
}`
          ),

          new Paragraph({ spacing: { before: 150, after: 150 } }),
          new Paragraph({ children: [new PageBreak()] }),

          h2('5.2. Módulo de Autenticación y Validación (LoginScreen.kt)'),
          p('Fragmentos clave donde se ejecutan las validaciones en tiempo real y el registro seguro en Logcat:'),

          codeBox(
            'LoginScreen.kt — Validaciones, Límites de Longitud y Logcat Seguro',
`// Constantes de seguridad definidas a nivel superior
private const val MAX_USER_LENGTH = 50
private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_LENGTH = 32

// 1. Reglas de Validación Reactiva
val emailError = when {
    emailTouched && email.trim().isEmpty() -> "El usuario no puede estar vacío ni contener solo espacios"
    emailTouched && email.length > MAX_USER_LENGTH -> "El usuario no puede superar los $MAX_USER_LENGTH caracteres"
    authMode == 1 && emailTouched && (!email.contains("@") || !email.contains(".")) -> "Email inválido"
    else -> ""
}

val passwordError = when {
    passwordTouched && password.isEmpty() -> "Contraseña es requerida"
    passwordTouched && password.length < MIN_PASSWORD_LENGTH -> "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres"
    passwordTouched && password.length > MAX_PASSWORD_LENGTH -> "La contraseña no puede superar los $MAX_PASSWORD_LENGTH caracteres"
    else -> ""
}

// 2. Control de Entrada en Tiempo Real
OutlinedTextField(
    value = email,
    onValueChange = {
        if (it.length <= MAX_USER_LENGTH) {
            email = it
            emailTouched = true
            errorMsg = ""
        }
    },
    // ...
)

// 3. Ocultamiento Visual de la Contraseña
visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()

// 4. Auditoría Segura en Logcat (Cero contraseñas en logs)
Log.i("AccesoSeguro", "=== Evento de Autenticación ===")
Log.i("AccesoSeguro", "Validando usuario (longitud: \${email.trim().length} car.)")
Log.i("AccesoSeguro", "MEDIDA DE SEGURIDAD: La contraseña se procesa en memoria y NUNCA se registra en Logcat.")

// 5. Mensajes de Error Genéricos
private fun mapAuthError(msg: String): String = when {
    msg.contains("Invalid login credentials") || msg.contains("invalid_credentials") ->
        "Usuario o contraseña incorrectos. Por favor, verifique sus datos."
    else ->
        "Acceso no autorizado. Verifique sus credenciales e intente de nuevo."
}`
          ),

          new Paragraph({ children: [new PageBreak()] }),

          // ================= SECCIÓN 6: CUESTIONARIO TEÓRICO =================
          h1('6. Cuestionario Teórico de la Práctica'),

          h2('Pregunta 1: ¿Por qué no deben registrarse contraseñas en Logcat?'),
          p('Respuesta Técnica Detallada:', { bold: true, color: COLOR_PRIMARY }),
          p('Logcat es un mecanismo de registro compartido a nivel del sistema operativo Android donde convergen los mensajes emitidos por el núcleo del sistema y por todas las aplicaciones instaladas. Registrar contraseñas o información sensible en Logcat compromete la seguridad por tres razones principales:'),
          bullet('1. Acceso mediante depuración física o malware', 'Cualquier persona que conecte el dispositivo a una computadora con depuración USB habilitada (adb logcat) puede leer en tiempo real las contraseñas en texto claro sin requerir contraseñas adicionales ni permisos de root en entornos de depuración.'),
          bullet('2. Fuga en volcados de fallos y telemetría', 'Cuando una aplicación se bloquea o envía informes de telemetría a servicios en la nube (ej. Firebase Crashlytics), es común que se adjunten las últimas líneas del Logcat. Si la contraseña fue registrada, viajará y quedará almacenada en servidores de terceros.'),
          bullet('3. Incumplimiento de normativas de seguridad', 'El estándar OWASP Mobile Top 10 (M4: Insecure Authentication) y el estándar OWASP MASVS-STORAGE prohíben explícitamente almacenar credenciales en registros de depuración.'),

          new Paragraph({ spacing: { before: 150, after: 150 } }),

          h2('Pregunta 2: ¿Qué diferencia existe entre ocultar una contraseña y cifrarla?'),
          p('Respuesta Técnica Detallada:', { bold: true, color: COLOR_PRIMARY }),
          p('Ambos mecanismos pertenecen a capas distintas del modelo de seguridad y persiguen fines diferentes:'),

          buildTable(
            ['Criterio de Comparación', 'Ocultar una Contraseña (Masking Visual)', 'Cifrar una Contraseña (Criptografía)'],
            [
              [
                'Concepto y Definición',
                'Es una máscara visual en la interfaz de usuario (UI) que reemplaza los caracteres reales por puntos o asteriscos (PasswordVisualTransformation).',
                'Es la aplicación de un algoritmo criptográfico reversible (ej. AES-256) o irreversible con sal (Hashing: bcrypt, Argon2) sobre los bytes del texto.'
              ],
              [
                'Capa de Operación',
                'Capa de Presentación / Pantalla del dispositivo.',
                'Capa de Datos / Almacenamiento local, memoria persistente y capa de transporte de red.'
              ],
              [
                'Propósito de Protección',
                'Evitar el espionaje visual de miradas indiscretas alrededor del usuario (Shoulder Surfing).',
                'Garantizar la confidencialidad de la credencial en reposo o tránsito frente a atacantes con acceso a los datos.'
              ],
              [
                'Estado en Memoria',
                'La contraseña continúa existiendo en texto plano en la variable de memoria del proceso.',
                'La contraseña se transforma matemáticamente en un criptograma o hash ininteligible sin la llave correspondiente.'
              ]
            ],
            [25, 37, 38],
            COLOR_PRIMARY
          ),

          new Paragraph({ spacing: { before: 150, after: 150 } }),
          new Paragraph({ children: [new PageBreak()] }),

          h2('Pregunta 3: ¿Por qué la validación del lado del cliente no es suficiente para autenticar a un usuario?'),
          p('Respuesta Técnica Detallada:', { bold: true, color: COLOR_PRIMARY }),
          p('La validación del lado del cliente es una práctica indispensable para mejorar la experiencia de usuario y filtrar envíos erróneos inmediatos, pero bajo ninguna circunstancia debe considerarse suficiente para la autenticación por los siguientes fundamentos:'),
          bullet('1. El cliente es un entorno no confiable (Untrusted Environment)', 'El código compilado que corre en el dispositivo del usuario final puede ser manipulado o alterado. Mediante ingeniería inversa o herramientas de instrumentación dinámica en tiempo de ejecución (como Frida, Xposed o APKTool), un atacante puede alterar el flujo del código y omitir cualquier sentencia de validación como if (!isFormValid).'),
          bullet('2. Bypass mediante herramientas de red', 'Un atacante no necesita utilizar la interfaz gráfica de la aplicación. Puede interceptar el tráfico mediante un proxy (Burp Suite, OWASP ZAP) o generar peticiones directas vía cURL o Postman enviando valores maliciosos o de longitud desmedida que la app en pantalla habría bloqueado.'),
          bullet('3. El servidor es la única autoridad de autenticación', 'El dispositivo móvil no posee la base de datos de usuarios ni debe almacenar las claves maestras. Es responsabilidad absoluta del servidor backend validar nuevamente todas las entradas, aplicar políticas de bloqueo contra ataques de fuerza bruta (Rate Limiting) y validar los hashes criptográficos antes de expedir tokens de sesión autenticados.'),

          new Paragraph({ spacing: { before: 180, after: 180 } }),

          // ================= SECCIÓN 7: CONCLUSIONES =================
          h1('7. Conclusiones'),
          bullet('1. Seguridad por Diseño', 'La implementación del módulo AccesoSeguro demuestra que la seguridad en Android debe planificarse desde la capa visual hasta el tratamiento de variables en memoria y los logs de depuración.'),
          bullet('2. Mitigación Efectiva de Ataques', 'La incorporación de límites estrictos de longitud y el bloqueo de entradas de solo espacios previene ataques de saturación y asegura la consistencia de los datos antes de enviarlos a la red.'),
          bullet('3. Cumplimiento de Buenas Prácticas', 'El resguardo de Logcat y el uso de mensajes de error genéricos cumplen estrictamente con las recomendaciones de OWASP MASVS, protegiendo las credenciales frente a ataques de enumeración y fugas accidentales.')
        ]
      }
    ]
  });

  const buffer = await Packer.toBuffer(doc);
  
  // Guardar en documentacion/practicas/
  const targetDir1 = path.join(__dirname, '..', 'practicas');
  if (!fs.existsSync(targetDir1)) fs.mkdirSync(targetDir1, { recursive: true });
  const targetFile1 = path.join(targetDir1, 'Practica_5_AccesoSeguro_Codigo_Seguro.docx');
  fs.writeFileSync(targetFile1, buffer);
  console.log(`Documento Word generado exitosamente en: ${targetFile1}`);

  // Guardar copia en documentacion/
  const targetDir2 = path.join(__dirname, '..');
  const targetFile2 = path.join(targetDir2, 'Practica_5_Codigo_Seguro_SkillAcademy.docx');
  fs.writeFileSync(targetFile2, buffer);
  console.log(`Copia guardada en: ${targetFile2}`);

  // Guardar copia en artifacts
  const artifactDir = 'C:/Users/Rayder/.gemini/antigravity-ide/brain/ba97428f-d5db-48c6-9b2b-8ca1b99917ec';
  const artifactFile = path.join(artifactDir, 'Practica_5_AccesoSeguro_Codigo_Seguro.docx');
  fs.writeFileSync(artifactFile, buffer);
  console.log(`Copia guardada en artefactos: ${artifactFile}`);
}

main().catch(err => {
  console.error("Error al generar el documento:", err);
  process.exit(1);
});
