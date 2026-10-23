# -*- coding: utf-8 -*-
"""
add_practica_5_and_phase2_callout.py
====================================
Actualiza el documento maestro SkillAcademy_Mobile_Native_Entregables.docx:
  1) Modifica el Callout de la Fase II (Tabla 28) para que sea idéntico en estilo
     al recuadro de la Fase 1, resumiendo los Entregables 4, 5 y 6, la Práctica 5
     (AccesoSeguro / Código Seguro) y la Práctica 6 (Interacción entre Dispositivos).
  2) Inserta la sección completa de la PRÁCTICA 5 DE LABORATORIO (Código Seguro,
     módulo AccesoSeguro en LoginScreen/MainActivity, tabla de 7 requisitos,
     justificación detallada de Base de Datos Relacional SQL frente a NoSQL Firestore,
     código comentado, cajas de evidencias de capturas P5-01 a P5-04,
     cuestionario teórico de la P5 de 3 preguntas y cuestionario de la P6 de 7 preguntas).
  3) Sincroniza el documento en documentacion/ y entregables/.
"""

import os
import shutil
import docx
from docx.shared import Pt, RGBColor, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls

DOC_PATH = r"c:\Users\Rayder\Desktop\SkillAcademy_Practicas_5_6_Endpoint\documentacion\SkillAcademy_Mobile_Native_Entregables.docx"
ENTREGABLES_PATH = r"c:\Users\Rayder\Desktop\SkillAcademy_Practicas_5_6_Endpoint\entregables\SkillAcademy_Mobile_Native_Entregables.docx"

COLOR_PRIMARY = RGBColor(0x0F, 0x29, 0x42)   # Navy corporativo
COLOR_SECONDARY = RGBColor(0x02, 0x84, 0xC7) # Azul secundario
COLOR_ACCENT = RGBColor(0x0D, 0x94, 0x88)    # Teal / Esmeralda
COLOR_TEXT = RGBColor(0x1E, 0x29, 0x3B)      # Gris texto principal
COLOR_MUTED = RGBColor(0x64, 0x74, 0x8B)     # Gris secundario
HEX_ACCENT = "0D9488"
HEX_PRIMARY = "0F2942"
HEX_SECONDARY = "0284C7"
HEX_BORDER = "CBD5E1"
HEX_CODE_BG = "0F172A"

def set_cell_shading(cell, color_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{color_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=140, bottom=140, left=200, right=200):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/>'
        f'<w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/>'
        f'</w:tcMar>'
    )
    tcPr.append(tcMar)

def set_cell_borders(cell, top="single", bottom="single", left="single", right="single", color=HEX_BORDER, sz="4"):
    tcPr = cell._tc.get_or_add_tcPr()
    borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="{top}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:left w:val="{left}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:bottom w:val="{bottom}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:right w:val="{right}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(borders)

def build_p(target_p, text="", size=10, bold=False, italic=False, color=COLOR_TEXT, align=WD_ALIGN_PARAGRAPH.LEFT, space_before=3, space_after=3):
    p = target_p.insert_paragraph_before()
    p.alignment = align
    p.paragraph_format.space_before = Pt(space_before)
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.line_spacing = 1.15
    if text:
        r = p.add_run(text)
        r.font.name = "Calibri"
        r.font.size = Pt(size)
        r.bold = bold
        r.italic = italic
        r.font.color.rgb = color
    return p

def build_h1(target_p, text, color=COLOR_PRIMARY):
    return build_p(target_p, text, size=14, bold=True, color=color, space_before=16, space_after=6)

def build_h2(target_p, text, color=COLOR_SECONDARY):
    return build_p(target_p, text, size=12, bold=True, color=color, space_before=12, space_after=4)

def build_h3(target_p, text, color=COLOR_ACCENT):
    return build_p(target_p, text, size=10.5, bold=True, color=color, space_before=8, space_after=3)

def build_bullet(target_p, bold_prefix, text):
    p = target_p.insert_paragraph_before()
    p.paragraph_format.left_indent = Inches(0.25)
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.line_spacing = 1.15
    
    r_sym = p.add_run("▪ ")
    r_sym.font.name = "Calibri"
    r_sym.font.size = Pt(9.5)
    r_sym.font.color.rgb = COLOR_PRIMARY
    
    if bold_prefix:
        r_pre = p.add_run(bold_prefix + ": ")
        r_pre.bold = True
        r_pre.font.name = "Calibri"
        r_pre.font.size = Pt(9.5)
        r_pre.font.color.rgb = COLOR_PRIMARY
        
    r_txt = p.add_run(text)
    r_txt.font.name = "Calibri"
    r_txt.font.size = Pt(9.5)
    r_txt.font.color.rgb = COLOR_TEXT
    return p

def insert_code_block(doc, target_p, code_str, title="Fragmento de Código"):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = tbl.rows[0].cells[0]
    set_cell_shading(cell, HEX_CODE_BG)
    set_cell_margins(cell, top=120, bottom=120, left=180, right=180)
    set_cell_borders(cell, top="single", bottom="single", left="single", right="single", color="334155", sz="6")
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(4)
    r_t = p.add_run(f"💻 {title}")
    r_t.bold = True
    r_t.font.name = "Consolas"
    r_t.font.size = Pt(9)
    r_t.font.color.rgb = RGBColor(0x38, 0xBD, 0xF8)
    
    p_c = cell.add_paragraph()
    p_c.paragraph_format.space_before = Pt(2)
    p_c.paragraph_format.space_after = Pt(2)
    r_c = p_c.add_run(code_str)
    r_c.font.name = "Consolas"
    r_c.font.size = Pt(8.5)
    r_c.font.color.rgb = RGBColor(0xF1, 0xF5, 0xF9)
    
    target_p._p.addprevious(tbl._tbl)

def insert_placeholder_box(doc, target_p, num_captura, titulo_captura, descripcion, que_debe_verse):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = tbl.rows[0].cells[0]
    set_cell_shading(cell, "F0F9FF")
    set_cell_margins(cell, top=140, bottom=140, left=200, right=200)
    set_cell_borders(cell, top="dashed", bottom="dashed", left="dashed", right="dashed", color=HEX_SECONDARY, sz="8")
    
    p = cell.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(6)
    r_t = p.add_run(f"📷 [ PEGAR AQUÍ CAPTURA {num_captura}: {titulo_captura.upper()} ]")
    r_t.bold = True
    r_t.font.name = "Calibri"
    r_t.font.size = Pt(10.5)
    r_t.font.color.rgb = RGBColor(0x02, 0x84, 0xC7)
    
    specs = [
        ("📋 Propósito de la prueba:", f" {descripcion}"),
        ("🔍 Elementos visibles requeridos:", f" {que_debe_verse}"),
        ("💡 Instrucción:", " Haz clic sobre este recuadro y presiona CTRL + V para pegar la captura de pantalla.")
    ]
    for lbl, val in specs:
        p_s = cell.add_paragraph()
        p_s.paragraph_format.space_before = Pt(1)
        p_s.paragraph_format.space_after = Pt(2)
        r_l = p_s.add_run(lbl)
        r_l.bold = True
        r_l.font.name = "Calibri"
        r_l.font.size = Pt(9)
        r_l.font.color.rgb = RGBColor(0x03, 0x69, 0xA1)
        
        r_v = p_s.add_run(val)
        r_v.font.name = "Calibri"
        r_v.font.size = Pt(9)
        r_v.font.color.rgb = COLOR_TEXT
        
    target_p._p.addprevious(tbl._tbl)

def insert_table_data(doc, target_p, headers, rows_data):
    tbl = doc.add_table(rows=len(rows_data) + 1, cols=len(headers))
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    
    # Header
    for c_idx, h_text in enumerate(headers):
        cell = tbl.rows[0].cells[c_idx]
        set_cell_shading(cell, "0F2942")
        set_cell_margins(cell, top=100, bottom=100, left=120, right=120)
        set_cell_borders(cell, top="single", bottom="single", left="single", right="single", color=HEX_BORDER, sz="4")
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(h_text)
        r.bold = True
        r.font.name = "Calibri"
        r.font.size = Pt(9)
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
        
    # Data Rows
    for r_idx, row in enumerate(rows_data):
        bg = "F8FAFC" if r_idx % 2 == 1 else "FFFFFF"
        for c_idx, val in enumerate(row):
            cell = tbl.rows[r_idx + 1].cells[c_idx]
            set_cell_shading(cell, bg)
            set_cell_margins(cell, top=80, bottom=80, left=120, right=120)
            set_cell_borders(cell, top="single", bottom="single", left="single", right="single", color=HEX_BORDER, sz="4")
            p = cell.paragraphs[0]
            if c_idx == 0:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                r = p.add_run(str(val))
                r.bold = True
            else:
                p.alignment = WD_ALIGN_PARAGRAPH.LEFT
                r = p.add_run(str(val))
            r.font.name = "Calibri"
            r.font.size = Pt(8.5)
            r.font.color.rgb = COLOR_TEXT
            
    target_p._p.addprevious(tbl._tbl)

def update_table_28(doc):
    t28 = doc.tables[28]
    cell = t28.rows[0].cells[0]
    
    # Limpiar párrafos existentes en cell
    p_first = cell.paragraphs[0]
    for p_extra in cell.paragraphs[1:]:
        p_extra._p.getparent().remove(p_extra._p)
    p_first.text = ""
    
    # Formatear el contenedor tal como Table 1
    set_cell_shading(cell, "F0FDFA")
    set_cell_margins(cell, top=140, bottom=140, left=200, right=200)
    set_cell_borders(cell, top="none", bottom="none", right="none", left="single", color=HEX_ACCENT, sz="28")
    
    # Título con icono de portapapeles
    p_first.paragraph_format.space_before = Pt(2)
    p_first.paragraph_format.space_after = Pt(6)
    r_icon = p_first.add_run("📋 ")
    r_icon.bold = True
    r_icon.font.name = "Calibri"
    r_icon.font.size = Pt(11)
    
    r_title = p_first.add_run("Estructura Formal de la Fase II: Entregables de Seguridad (4, 5 y 6) y Prácticas de Laboratorio")
    r_title.bold = True
    r_title.font.name = "Calibri"
    r_title.font.size = Pt(11)
    r_title.font.color.rgb = COLOR_PRIMARY
    
    bullets = [
        ("ENTREGABLE 4", "Seguridad en el Dispositivo Móvil, Criptografía de Datos en Reposo (AES-256 GCM con Android KeyStore en EncryptedSharedPreferences), Desactivación de Backups ADB (allowBackup=\"false\"), Enmascaramiento de Tarjetas (PAN Masking), Ocultamiento de Pantalla con FLAG_SECURE y Destrucción Segura de Tokens en Memoria."),
        ("ENTREGABLE 5", "Seguridad en Comunicaciones de Red y Control de Acceso, Forzado de HTTPS y TLS 1.3 con network_security_config.xml (cleartextTrafficPermitted=\"false\"), Mitigación de Ataques Man-in-the-Middle (MitM), Prevención de Inyecciones SQL/NoSQL en Supabase REST y Bloqueo de Fuerza Bruta mediante Retardo Exponencial (Exponential Backoff)."),
        ("ENTREGABLE 6", "Gestión de Vulnerabilidades, Protección contra Ingeniería Inversa mediante Ofuscación y Reducción con ProGuard / R8 (proguard-rules.pro), Detección en Tiempo de Ejecución de Dispositivos Rooteados y Emuladores (RootDetector.kt), y Matriz de Evaluación de Riesgos formal bajo la norma ISO/IEC 27001:2022."),
        ("PRÁCTICA 5 (CÓDIGO SEGURO EN LOGIN - ACCESOSEGURO)", "Implementación formal de código seguro en el inicio de sesión y registro de SkillAcademy (validaciones reactivas, rechazo de cadenas vacías/espacios en blanco, límites estrictos de longitud 50/32/8, enmascaramiento visual de contraseñas, mensajes de error genéricos para prevenir enumeración de usuarios, auditoría en Logcat con cero credenciales en texto claro, simulación de acceso con credenciales de prueba, justificación de arquitectura SQL frente a NoSQL y las 3 preguntas teóricas de análisis)."),
        ("PRÁCTICA 6 (INTERACCIÓN ENTRE DISPOSITIVOS Y ANÁLISIS DE INTERFAZ)", "Análisis técnico de la interacción entre dispositivos (justificación de exclusión de chat P2P vs sincronización Cloud Supabase), nombres de interfaz (Scrolling, Estándar Americano, Scaffold), tipos de navegación (Bottom Navigation Bar, Stack, BottomSheet) y análisis del flujo unificado de inicio de sesión/registro frente al anti-patrón \"pointless\"."),
        ("JUSTIFICACIÓN DE BASE DE DATOS (SQL VS NOSQL)", "Fundamentación técnica de la selección de PostgreSQL (Supabase) y SQLite nativo en lugar de NoSQL (Cloud Firestore), basada en integridad relacional estricta, transacciones ACID, optimización de consultas/costos por lectura y seguridad centralizada con Row Level Security (RLS).")
    ]
    
    for prefix, b_text in bullets:
        p_b = cell.add_paragraph()
        p_b.paragraph_format.left_indent = Inches(0.2)
        p_b.paragraph_format.space_before = Pt(1.5)
        p_b.paragraph_format.space_after = Pt(2)
        p_b.paragraph_format.line_spacing = 1.15
        
        r_dot = p_b.add_run("• ")
        r_dot.bold = True
        r_dot.font.name = "Calibri"
        r_dot.font.size = Pt(9.5)
        r_dot.font.color.rgb = COLOR_PRIMARY
        
        r_pre = p_b.add_run(prefix + ": ")
        r_pre.bold = True
        r_pre.font.name = "Calibri"
        r_pre.font.size = Pt(9.5)
        r_pre.font.color.rgb = COLOR_PRIMARY
        
        r_txt = p_b.add_run(b_text)
        r_txt.font.name = "Calibri"
        r_txt.font.size = Pt(9.5)
        r_txt.font.color.rgb = COLOR_TEXT

def main():
    if not os.path.exists(DOC_PATH):
        print(f"Error: No existe el archivo {DOC_PATH}")
        return

    doc = docx.Document(DOC_PATH)
    
    # 1. Actualizar Table 28 (Callout de Fase II)
    update_table_28(doc)
    print("Tabla 28 actualizada exitosamente con el resumen formal de Fase II.")
    
    # 2. Localizar el punto de inserción: encabezado de Entregable 4
    target_idx = None
    for i, p in enumerate(doc.paragraphs):
        if "ENTREGABLE 4 DE LA APP MÓVIL:" in p.text.upper():
            target_idx = i
            break
            
    if target_idx is None:
        print("Error: No se encontró el encabezado de ENTREGABLE 4")
        return
        
    target_p = doc.paragraphs[target_idx]
    print(f"Insertando sección de Práctica 5 antes de párrafo [{target_idx}]: {target_p.text[:60]}")
    
    # =========================================================================
    # SECCIÓN: PRÁCTICA 5 DE LABORATORIO (CÓDIGO SEGURO EN LOGIN + BASE DE DATOS)
    # =========================================================================
    build_h1(target_p, "PRÁCTICA 5: PRÁCTICAS DE CÓDIGO SEGURO EN APLICACIONES MÓVILES (ACCESOSEGURO)")
    build_p(
        target_p,
        "La Práctica 5 de laboratorio establece las directrices fundamentales de codificación segura para el módulo de acceso y autenticación "
        "de la aplicación móvil (módulo AccesoSeguro integrado en LoginScreen.kt y MainActivity.kt). "
        "Su objetivo es mitigar los tres defectos más comunes detectados en auditorías de código: la aceptación de entradas no validadas, "
        "la fuga de credenciales en los registros de depuración del sistema (Logcat) y la emisión de mensajes de error técnicos que facilitan ataques de enumeración."
    )
    
    # 5.1 Requisitos Implementados
    build_h2(target_p, "5.1 Especificación de Requisitos y Controles de Seguridad Implementados")
    build_p(
        target_p,
        "A continuación se detalla la matriz de cumplimiento con los 7 requisitos normativos solicitados en la práctica, "
        "alineados a los estándares de OWASP Mobile Application Security Verification Standard (MASVS):"
    )
    
    headers_req = ["N°", "Requisito de la Práctica", "Implementación en SkillAcademy (AccesoSeguro)", "Mecanismo de Seguridad"]
    rows_req = [
        ["1", "Mostrar campos para usuario y contraseña", "Campos dedicados OutlinedTextField para Usuario/Email y Contraseña", "Separación visual y controles de foco independientes."],
        ["2", "Validar que ambos campos contengan información", "Comprobación reactiva: email.trim().isEmpty() y password.isEmpty()", "Rechaza el envío nulo y bloquea entradas compuestas solo por espacios en blanco."],
        ["3", "Rechazar entradas que superen longitud definida", "MAX_USER_LENGTH = 50, MIN_PASSWORD_LENGTH = 8, MAX_PASSWORD_LENGTH = 32", "Previene saturación de búfer, desbordamiento de memoria y contraseñas débiles."],
        ["4", "Ocultar los caracteres de la contraseña", "PasswordVisualTransformation con botón alternador de visibilidad", "Mitiga miradas indiscretas sobre la pantalla física (Shoulder Surfing)."],
        ["5", "Mensajes de error genéricos", "Función mapAuthError(): 'Usuario o contraseña incorrectos'", "Previene la enumeración de usuarios (OWASP A07) al no revelar si la cuenta existe."],
        ["6", "Evitar imprimir contraseñas en Logcat", "Auditoría en Logcat con tag 'AccesoSeguro' sin credenciales en texto plano", "Cumplimiento con OWASP MASVS-STORAGE: Cero credenciales en logs del SO."],
        ["7", "Mensaje de acceso simulado válido", "Validación con credenciales de prueba seguras (admin@seguro.com / Seguro1234#)", "Emisión de Toast de confirmación: '¡Acceso simulado concedido!'."]
    ]
    insert_table_data(doc, target_p, headers_req, rows_req)
    build_p(target_p, "")

    # 5.2 Justificación Base de Datos SQL vs NoSQL
    build_h2(target_p, "5.2 Justificación Técnica: Elección de Base de Datos Relacional (SQL) frente a No Relacional (NoSQL)")
    build_p(
        target_p,
        "Aunque en el planteamiento inicial de prácticas de laboratorio se contemplaba el uso de bases de datos no relacionales orientadas a documentos "
        "(como Cloud Firestore), para el desarrollo formal de SkillAcademy se seleccionó un modelo Relacional (PostgreSQL en Supabase y SQLite nativo en Android). "
        "Esta decisión fue autorizada y se fundamenta sólidamente en los siguientes criterios de arquitectura de software y resiliencia:"
    )
    
    build_bullet(target_p, "1. Naturaleza Intrínsecamente Relacional del Dominio Educativo", 
                 "SkillAcademy administra entidades altamente interconectadas: Usuarios (profiles) que adquieren Cursos, los cuales contienen Módulos, Lecciones y Quizes, vinculados a través de Inscripciones y Cupones con historial de pagos. "
                 "En un motor NoSQL (documentos), modelar estas relaciones exige desnormalizar información (duplicando nombres de cursos, precios y datos de instructores en cada documento). "
                 "Esto introduce un altísimo riesgo de inconsistencia ('anomalías de actualización') cuando un curso actualiza su precio o contenido.")
    
    build_bullet(target_p, "2. Integridad Transaccional ACID y Restricciones Estrictas", 
                 "Las operaciones críticas de la plataforma (como la inscripción a un curso, el descuento por cupón y la validación de checkout) exigen transacciones atómicas con garantías ACID. "
                 "PostgreSQL y SQLite imponen mediante el motor restricciones PRIMARY KEY, FOREIGN KEY, NOT NULL y CHECK, impidiendo la existencia de inscripciones huérfanas o cupones inválidos. "
                 "En bases de datos NoSQL, estas restricciones deben programarse manualmente en el cliente o mediante funciones backend, haciéndolas frágiles y propensas a errores.")

    build_bullet(target_p, "3. Eficiencia en Consultas Complejas y Optimización de Costos", 
                 "Servicios NoSQL como Cloud Firestore facturan por cada lectura individual de documento. Generar la vista de un curso con su instructor, módulos, lecciones y reseñas requeriría decenas de lecturas dispersas por cada visualización de pantalla. "
                 "En contraste, PostgreSQL resuelve joins, agregaciones y promedios de calificación en una sola consulta SQL optimizada en milisegundos, reduciendo el consumo de ancho de banda celular y la latencia en el teléfono.")

    build_bullet(target_p, "4. Persistencia Local Nativa Offline-First con SQLite", 
                 "Android incorpora el motor SQLite de forma nativa a través de SQLiteOpenHelper sin requerir librerías propietarias pesadas ni sincronizadores invasivos de terceros. "
                 "Esto permite que la base de datos local del carrito (skillacademy_cart.db) funcione con cero latencia y disponibilidad garantizada en modo avión o pérdida de cobertura.")

    build_bullet(target_p, "5. Seguridad Corporativa con Row Level Security (RLS)", 
                 "Supabase (PostgreSQL) implementa seguridad a nivel de fila (Row Level Security) directamente en el motor de base de datos. Cada estudiante solo puede consultar o alterar sus propias inscripciones y tareas mediante validación criptográfica de tokens JWT, "
                 "otorgando una garantía de aislamiento de datos corporativa superior y fácilmente auditable bajo la norma ISO/IEC 27001.")

    build_p(target_p, "")

    # 5.3 Código Fuente Comentado
    build_h2(target_p, "5.3 Código Fuente Representativo del Módulo de Acceso Seguro")
    build_p(
        target_p,
        "A continuación se ilustran los fragmentos de código fuente donde se aplican las directrices de código seguro en Kotlin/Jetpack Compose:"
    )

    code_main_activity = (
        "// MainActivity.kt — Declaración del Módulo AccesoSeguro y Reglas de Codificación Segura\n"
        "package com.skillacademy.mobile\n\n"
        "/**\n"
        " * PRÁCTICA 5: Prácticas de código seguro en aplicaciones móviles (AccesoSeguro)\n"
        " * 1. Campos dedicados: Usuario/Email y Contraseña con interfaz reactiva.\n"
        " * 2. Validación de espacios: trim().isEmpty() previene cadenas vacías o solo espacios.\n"
        " * 3. Límites estrictos: MAX_USER_LENGTH = 50, MIN_PASSWORD_LENGTH = 8, MAX_PASSWORD_LENGTH = 32.\n"
        " * 4. Ocultamiento visual: PasswordVisualTransformation() previene Shoulder Surfing.\n"
        " * 5. Errores genéricos: mapAuthError() emite 'Usuario o contraseña incorrectos' (Anti-Enumeración).\n"
        " * 6. Registros limpios: Tag 'AccesoSeguro' en Logcat NUNCA imprime credenciales ni tokens.\n"
        " * 7. Acceso simulado: Concede acceso mediante Toast ante credenciales de prueba válidas.\n"
        " */\n"
        "class MainActivity : ComponentActivity() {\n"
        "    override fun onCreate(savedInstanceState: Bundle?) {\n"
        "        super.onCreate(savedInstanceState)\n"
        "        setContent {\n"
        "            SkillAcademyTheme {\n"
        "                Surface(modifier = Modifier.fillMaxSize()) {\n"
        "                    MainAppScreen()\n"
        "                }\n"
        "            }\n"
        "        }\n"
        "    }\n"
        "}"
    )
    insert_code_block(doc, target_p, code_main_activity, "MainActivity.kt — Documentación de Reglas de AccesoSeguro")

    code_login_screen = (
        "// LoginScreen.kt — Validaciones Reactivas, Enmascaramiento y Auditoría Segura\n"
        "private const val MAX_USER_LENGTH = 50\n"
        "private const val MIN_PASSWORD_LENGTH = 8\n"
        "private const val MAX_PASSWORD_LENGTH = 32\n\n"
        "// Validación de usuario: rechazo de espacios y longitud\n"
        "val emailError = when {\n"
        "    emailTouched && email.trim().isEmpty() -> \"El usuario no puede estar vacío ni contener solo espacios\"\n"
        "    emailTouched && email.length > MAX_USER_LENGTH -> \"El usuario no puede superar los $MAX_USER_LENGTH caracteres\"\n"
        "    else -> \"\"\n"
        "}\n\n"
        "// Validación de contraseña: longitud mínima de 8 y máxima de 32\n"
        "val passwordError = when {\n"
        "    passwordTouched && password.isEmpty() -> \"Contraseña requerida\"\n"
        "    passwordTouched && password.length < MIN_PASSWORD_LENGTH -> \"La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres\"\n"
        "    passwordTouched && password.length > MAX_PASSWORD_LENGTH -> \"La contraseña no puede superar los $MAX_PASSWORD_LENGTH caracteres\"\n"
        "    else -> \"\"\n"
        "}\n\n"
        "// Ocultamiento visual con PasswordVisualTransformation\n"
        "visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()\n\n"
        "// Auditoría en Logcat: Se registra el evento SIN imprimir la contraseña\n"
        "Log.i(\"AccesoSeguro\", \"=== Evento de Autenticación ===\")\n"
        "Log.i(\"AccesoSeguro\", \"Validando usuario (longitud: ${email.trim().length} car.)\")\n"
        "Log.i(\"AccesoSeguro\", \"MEDIDA DE SEGURIDAD: La contraseña se procesa en memoria y NUNCA se registra en Logcat.\")\n\n"
        "// Mensaje genérico para prevenir enumeración de usuarios (OWASP A07)\n"
        "private fun mapAuthError(msg: String): String = \"Usuario o contraseña incorrectos. Por favor, verifique sus datos.\""
    )
    insert_code_block(doc, target_p, code_login_screen, "LoginScreen.kt — Lógica de AccesoSeguro y Sanitización de Logcat")
    build_p(target_p, "")

    # 5.4 Cajas de Evidencias / Capturas de Práctica 5
    build_h2(target_p, "5.4 Banco de Evidencias: Capturas Requeridas de la Práctica 5")
    build_p(
        target_p,
        "A continuación se disponen los espacios dedicados para la inserción de las 4 capturas de pantalla de prueba de la Práctica 5:"
    )

    insert_placeholder_box(
        doc, target_p,
        "P5-01",
        "Interfaz de Inicio de Sesión en el Emulador (AccesoSeguro)",
        "Captura de la pantalla de login ejecutándose en el emulador de Android Studio.",
        "Campos de 'Usuario o Email' y 'Contraseña', contadores de caracteres (0/50 y 0/32), y contraseña oculta con puntos por defecto."
    )
    build_p(target_p, "")

    insert_placeholder_box(
        doc, target_p,
        "P5-02",
        "Prueba con Entradas Inválidas (Rechazo de Espacios y Longitud)",
        "Demostración de las validaciones de seguridad activándose en tiempo real.",
        "Mensaje de error en rojo al ingresar únicamente espacios ('El usuario no puede estar vacío ni contener solo espacios') o contraseña menor a 8 caracteres."
    )
    build_p(target_p, "")

    insert_placeholder_box(
        doc, target_p,
        "P5-03",
        "Mensaje de Acceso Simulado Concedido (Datos de Prueba Válidos)",
        "Comprobación de inicio de sesión exitoso con credenciales de prueba.",
        "Mensaje flotante (Toast) visible en pantalla confirmando: '¡Acceso simulado concedido! Bienvenido al sistema seguro.'"
    )
    build_p(target_p, "")

    insert_placeholder_box(
        doc, target_p,
        "P5-04",
        "Auditoría de Logcat Demostrando Cero Credenciales Registradas",
        "Captura de la ventana de Logcat en Android Studio filtrada por tag:AccesoSeguro.",
        "Registros del evento de autenticación donde se observa la longitud del usuario y la leyenda explícita de que la contraseña NO se imprimió en Logcat."
    )
    build_p(target_p, "")

    # 5.5 Cuestionario Teórico Resuelto de Práctica 5
    build_h2(target_p, "5.5 Cuestionario Teórico de Análisis: Práctica 5 (Código Seguro)")
    
    build_h3(target_p, "Pregunta 1: ¿Por qué no deben registrarse contraseñas en Logcat?")
    build_p(
        target_p,
        "Respuesta: Logcat es un búfer circular compartido a nivel del sistema operativo Android donde convergen los mensajes de depuración del sistema y de todas las aplicaciones activas. "
        "Registrar contraseñas o tokens en Logcat constituye una vulnerabilidad crítica por tres motivos:\n"
        "1. Persistencia y accesibilidad no autorizada: Cualquier persona con acceso físico al teléfono o mediante depuración USB (adb logcat) puede extraer los registros en texto claro sin privilegios de superusuario en compilaciones de depuración.\n"
        "2. Exposición en reportes de fallos: Frecuentemente los volcados de Logcat se adjuntan en reportes de fallos (crash analytics) o sistemas de telemetría hacia servidores de terceros. Si una contraseña se imprime en el log, queda almacenada en servidores externos fuera del control de la aplicación.\n"
        "3. Violación de estándares internacionales: El estándar OWASP Mobile Top 10 (M4: Insecure Authentication / MASVS-STORAGE) y normativas como GDPR y PCI-DSS prohíben explícitamente almacenar o emitir credenciales en bitácoras de depuración."
    )

    build_h3(target_p, "Pregunta 2: ¿Qué diferencia existe entre ocultar una contraseña y cifrarla?")
    build_p(
        target_p,
        "Respuesta: Ambos conceptos pertenecen a capas de seguridad totalmente distintas y tienen propósitos diferentes:\n"
        "• Ocultar una contraseña (Masking visual): Es una transformación estética en la capa de interfaz gráfica (UI) que reemplaza los caracteres legibles por símbolos (puntos •••• o asteriscos) mediante PasswordVisualTransformation. "
        "Su único fin es mitigar las miradas indiscretas en el entorno físico del usuario (Shoulder Surfing). Sin embargo, en la memoria del teléfono la contraseña sigue residiendo como texto plano.\n"
        "• Cifrar una contraseña (Encryption / Cryptography): Es la aplicación de un algoritmo matemático criptográfico (reversible como AES-256 en reposo, o irreversible como Hashing seguro con sal tipo bcrypt o PBKDF2 en backend). "
        "Opera en la capa de datos y transporte (memoria, disco y red). Si un atacante intercepta el texto cifrado o el hash, le resulta matemáticamente inviable descifrar la contraseña original sin la clave o sin un coste computacional astronómico."
    )

    build_h3(target_p, "Pregunta 3: ¿Por qué la validación del lado del cliente no es suficiente para autenticar a un usuario?")
    build_p(
        target_p,
        "Respuesta: La validación del lado del cliente es una práctica de usabilidad y primera línea de defensa (Defense in Depth), pero NUNCA es suficiente por sí sola para autenticar debido a:\n"
        "1. El cliente es un entorno no confiable (Untrusted Environment): El código ejecutado en un teléfono Android puede ser desensamblado, modificado o manipulado mediante ingeniería inversa (herramientas como APKTool, Frida u Objection) para saltarse cualquier validación local 'if (!isFormValid)'.\n"
        "2. Omisión de la interfaz por atacantes: Un atacante puede omitir totalmente la aplicación móvil e interactuar directamente contra la API REST (mediante Postman, cURL o proxies interceptores como Burp Suite), enviando peticiones con credenciales malformadas o inyecciones que la interfaz gráfica habría bloqueado.\n"
        "3. La autoridad de autenticación reside en el servidor: El dispositivo móvil no posee la base de datos de usuarios ni los secretos criptográficos para verificar firmas. Corresponde exclusivamente al servidor backend validar nuevamente todas las entradas, aplicar políticas de bloqueo por fuerza bruta (Rate Limiting), comparar los hashes y generar los tokens de sesión seguros (JWT)."
    )
    build_p(target_p, "")

    # 5.6 Cuestionario Teórico Resuelto de Práctica 6 (Consolidación)
    build_h2(target_p, "5.6 Cuestionario Teórico de Análisis: Práctica 6 (Interacción entre Dispositivos)")
    
    p6_qa = [
        ("Pregunta 1: ¿Qué significa la interacción entre dispositivos móviles?",
         "Es la capacidad de dos o más dispositivos móviles independientes de intercambiar datos, eventos o estados a través de un medio de comunicación (inalámbrico, por red local o mediante un canal compartido en la nube), permitiendo el trabajo colaborativo, la sincronización de tareas o la continuidad operativa en tiempo real."),
        ("Pregunta 2: ¿Qué tecnologías permiten intercambiar información entre dispositivos?",
         "• Corta distancia / Redes de proximidad: Bluetooth Classic, BLE (Bluetooth Low Energy), Wi-Fi Direct y NFC.\n• Red local y amplia: Redes Wi-Fi locales mediante Sockets TCP/UDP, protocolos HTTP/REST, WebSockets, MQTT y servicios de mensajería en la nube (Supabase Realtime, Firebase Cloud Messaging)."),
        ("Pregunta 3: ¿Qué diferencia existe entre una simulación local y una comunicación real?",
         "• Simulación local: Ocurre dentro de la memoria y el ciclo de vida del mismo proceso en el dispositivo (o emulador); no sufre latencia de red, pérdida de paquetes, interferencias físicas ni requiere permisos de hardware de radiofrecuencia.\n• Comunicación real: Transmite paquetes de bytes a través de antenas físicas (Wi-Fi/Bluetooth/Celular); está expuesta a caídas de señal, ruido electromagnético, consumo de batería, requerimientos de emparejamiento y necesidades de cifrado en tránsito."),
        ("Pregunta 4: ¿Qué función cumple el estado de la aplicación en Jetpack Compose?",
         "Representa la fuente única de la verdad (Single Source of Truth). Al modificar variables de estado declaradas con mutableStateOf o mutableStateListOf, Jetpack Compose ejecuta automáticamente un proceso de recomposición, redibujando en pantalla únicamente los componentes visuales afectados sin necesidad de manipular vistas manualmente."),
        ("Pregunta 5: ¿Por qué es importante validar los mensajes antes de enviarlos?",
         "Previene el consumo innecesario de ancho de banda y batería en llamadas de red infructuosas, evita inconsistencias en la interfaz gráfica (burbujas vacías o con formato corrupto) y protege al sistema contra inyecciones de datos o cargas útiles malformadas."),
        ("Pregunta 6: ¿Qué cambios serían necesarios para implementar comunicación real mediante Bluetooth o Wi-Fi?",
         "1. Permisos en tiempo de ejecución: Declarar y solicitar en Android permisos como BLUETOOTH_CONNECT, BLUETOOTH_SCAN y NEARBY_WIFI_DEVICES.\n2. Arquitectura de Sockets: Configurar un rol Servidor (BluetoothServerSocket / ServerSocket) a la escucha y un rol Cliente (BluetoothSocket / Socket) para la conexión.\n3. Hilos secundarios: Gestionar la lectura y escritura de flujos (InputStream/OutputStream) mediante Corrutinas en Dispatchers.IO para no bloquear el hilo principal (UI Thread)."),
        ("Pregunta 7: ¿Qué ventajas tendría utilizar un servidor intermediario para intercambiar mensajes?",
         "• Desacoplamiento total: Los dispositivos no necesitan estar en la misma red física ni mantener una conexión directa activa.\n• Persistencia y cola de mensajes: Si el destinatario está desconectado, el servidor almacena el mensaje y lo entrega cuando recupere la conectividad (Store-and-Forward).\n• Seguridad y gobernanza: El servidor autentica a los usuarios, cifra el canal, audita las transacciones y previene ataques directos entre terminales.")
    ]

    for q_t, ans_t in p6_qa:
        build_h3(target_p, q_t)
        build_p(target_p, "Respuesta: " + ans_t)
        
    build_p(target_p, "")

    # Guardar documento actualizado
    doc.save(DOC_PATH)
    print(f"Documento maestro guardado exitosamente en: {DOC_PATH}")
    
    # Copiar a entregables/
    try:
        shutil.copy2(DOC_PATH, ENTREGABLES_PATH)
        print(f"Copia sincronizada exitosamente en: {ENTREGABLES_PATH}")
    except Exception as e:
        print(f"Aviso al copiar: {e}")

if __name__ == "__main__":
    main()
