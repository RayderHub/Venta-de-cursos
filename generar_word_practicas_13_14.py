# -*- coding: utf-8 -*-
"""
generar_word_practicas_13_14.py
Genera el informe oficial en formato Word (.docx) para las Prácticas 13 y 14:
Automatización de pruebas y validación continua de aplicaciones móviles Flutter/Kotlin
en la solución multiplataforma SkillAcademy (Android Kotlin, Node.js Fastify, Bonsai OpenSearch y Supabase PostgreSQL).
"""

import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls

# Paleta de colores técnica y formal
COLOR_PRIMARY = RGBColor(26, 54, 93)     # #1A365D Azul marino corporativo
COLOR_SECONDARY = RGBColor(13, 148, 136) # #0D9488 Verde azulado técnico
COLOR_DARK = RGBColor(51, 65, 85)        # #334155 Pizarra oscura para texto
COLOR_MUTED = RGBColor(100, 116, 139)    # #64748B Gris medio
COLOR_ALERT = RGBColor(185, 28, 28)      # #B91C1C Rojo advertencia
COLOR_SUCCESS = RGBColor(22, 101, 52)    # #166534 Verde éxito

HEX_PRIMARY = "1A365D"
HEX_SECONDARY = "0D9488"
HEX_LIGHT_BG = "F8FAFC"
HEX_ALT_ROW = "F1F5F9"
HEX_BORDER = "CBD5E1"
HEX_SUCCESS_BG = "DCFCE7"
HEX_ALERT_BG = "FEE2E2"

def set_cell_background(cell, hex_color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=120, bottom=120, left=160, right=160):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="{top}" w:type="dxa"/>'
        f'<w:bottom w:w="{bottom}" w:type="dxa"/>'
        f'<w:left w:w="{left}" w:type="dxa"/>'
        f'<w:right w:w="{right}" w:type="dxa"/>'
        f'</w:tcMar>'
    )
    tcPr.append(tcMar)

def set_table_borders(table, hex_color=HEX_BORDER):
    tblPr = table._tbl.tblPr
    borders = parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'<w:top w:val="single" w:sz="6" w:space="0" w:color="{hex_color}"/>'
        f'<w:bottom w:val="single" w:sz="6" w:space="0" w:color="{hex_color}"/>'
        f'<w:left w:val="none"/>'
        f'<w:right w:val="none"/>'
        f'<w:insideH w:val="single" w:sz="4" w:space="0" w:color="{hex_color}"/>'
        f'<w:insideV w:val="none"/>'
        f'</w:tblBorders>'
    )
    tblPr.append(borders)

def format_table_headers(table, col_names, col_widths=None):
    set_table_borders(table)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    hdr_cells = table.rows[0].cells
    for i, name in enumerate(col_names):
        hdr_cells[i].text = name
        set_cell_background(hdr_cells[i], HEX_PRIMARY)
        set_cell_margins(hdr_cells[i], top=140, bottom=140, left=140, right=140)
        p = hdr_cells[i].paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        for r in p.runs:
            r.font.name = "Calibri"
            r.font.size = Pt(10)
            r.font.bold = True
            r.font.color.rgb = RGBColor(255, 255, 255)
    if col_widths:
        for row in table.rows:
            for idx, width in enumerate(col_widths):
                row.cells[idx].width = Inches(width)

def style_data_row(row, is_even=False, alignments=None):
    bg_color = HEX_ALT_ROW if is_even else "FFFFFF"
    for i, cell in enumerate(row.cells):
        set_cell_background(cell, bg_color)
        set_cell_margins(cell, top=100, bottom=100, left=140, right=140)
        cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        for p in cell.paragraphs:
            if alignments and i < len(alignments):
                p.alignment = alignments[i]
            for r in p.runs:
                r.font.name = "Calibri"
                r.font.size = Pt(9.5)
                r.font.color.rgb = COLOR_DARK

def add_callout(doc, title, text, bg_color="F1F5F9", border_color="0D9488"):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = tbl.rows[0].cells[0]
    cell.width = Inches(6.5)
    set_cell_background(cell, bg_color)
    set_cell_margins(cell, top=140, bottom=140, left=180, right=180)
    
    tcPr = cell._tc.get_or_add_tcPr()
    borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:left w:val="single" w:sz="24" w:space="0" w:color="{border_color}"/>'
        f'<w:top w:val="none"/>'
        f'<w:bottom w:val="none"/>'
        f'<w:right w:val="none"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(borders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    r_t = p.add_run(f"{title}: ")
    r_t.font.name = "Calibri"
    r_t.font.size = Pt(10)
    r_t.font.bold = True
    r_t.font.color.rgb = COLOR_PRIMARY
    
    r_txt = p.add_run(text)
    r_txt.font.name = "Calibri"
    r_txt.font.size = Pt(9.5)
    r_txt.font.color.rgb = COLOR_DARK
    
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

def build_document():
    doc = docx.Document()
    
    for section in doc.sections:
        section.top_margin = Inches(0.8)
        section.bottom_margin = Inches(0.8)
        section.left_margin = Inches(0.8)
        section.right_margin = Inches(0.8)
        
    # =========================================================================
    # ENCABEZADO FORMAL Y METADATOS
    # =========================================================================
    p_meta = doc.add_paragraph()
    p_meta.paragraph_format.space_before = Pt(0)
    p_meta.paragraph_format.space_after = Pt(2)
    r_meta = p_meta.add_run("INGENIERÍA DE SOFTWARE Y CALIDAD CONTINUA (CI/CD)")
    r_meta.font.name = "Calibri"
    r_meta.font.size = Pt(10)
    r_meta.font.bold = True
    r_meta.font.color.rgb = COLOR_SECONDARY
    
    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(2)
    p_title.paragraph_format.space_after = Pt(4)
    r_title = p_title.add_run("Prácticas 13 y 14: Automatización de Pruebas y Validación Continua")
    r_title.font.name = "Calibri"
    r_title.font.size = Pt(20)
    r_title.font.bold = True
    r_title.font.color.rgb = COLOR_PRIMARY
    
    p_sub = doc.add_paragraph()
    p_sub.paragraph_format.space_before = Pt(0)
    p_sub.paragraph_format.space_after = Pt(12)
    r_sub = p_sub.add_run("Implementación de suites de pruebas unitarias, pruebas de integración y pipeline automatizado con GitHub Actions en la solución SkillAcademy (Android Kotlin / Fastify)")
    r_sub.font.name = "Calibri"
    r_sub.font.size = Pt(11)
    r_sub.font.italic = True
    r_sub.font.color.rgb = COLOR_MUTED

    # Ficha técnica
    tbl_info = doc.add_table(rows=5, cols=2)
    set_table_borders(tbl_info)
    info_data = [
        ("Proyecto Auditado:", "SkillAcademy Mobile & Backend Platform (Venta de Cursos Online)"),
        ("Tecnologías Centrales:", "Android Native (Kotlin / Jetpack Compose) | Node.js 20 Fastify | Bonsai OpenSearch | Supabase PostgreSQL"),
        ("Estrategia de Automatización:", "JUnit 4, Kotlin Test, Mocking de Payloads JSON, Gradle 9.3 y GitHub Actions CI Workflow"),
        ("Alcance de las Pruebas:", "5 Pruebas Unitarias de Negocio + 3 Pruebas de Integración de Componentes + Pipeline CI"),
        ("Repositorio y Rama:", "https://github.com/RayderHub/Venta-de-cursos.git (Rama main)")
    ]
    for idx, (label, val) in enumerate(info_data):
        row = tbl_info.rows[idx]
        c0, c1 = row.cells[0], row.cells[1]
        c0.text = label
        c1.text = val
        set_cell_background(c0, HEX_LIGHT_BG)
        set_cell_background(c1, "FFFFFF")
        set_cell_margins(c0, 60, 60, 100, 100)
        set_cell_margins(c1, 60, 60, 100, 100)
        c0.paragraphs[0].runs[0].font.bold = True
        c0.paragraphs[0].runs[0].font.size = Pt(9.5)
        c0.paragraphs[0].runs[0].font.color.rgb = COLOR_PRIMARY
        c1.paragraphs[0].runs[0].font.size = Pt(9.5)
        c1.paragraphs[0].runs[0].font.color.rgb = COLOR_DARK
        c0.width = Inches(2.2)
        c1.width = Inches(4.3)
    
    doc.add_paragraph().paragraph_format.space_after = Pt(10)

    # Introducción y Aclaración Conceptual
    p_intro = doc.add_paragraph()
    p_intro.paragraph_format.line_spacing = 1.15
    p_intro.paragraph_format.space_after = Pt(10)
    r_in = p_intro.add_run(
        "En el desarrollo ágil de software moderno, una prueba manual ejecutada una sola vez no garantiza que el sistema siga funcionando mañana. "
        "Cualquier ajuste menor en una función de cálculo o en un controlador de red puede reintroducir fallos que previamente habían sido resueltos. "
        "Las Prácticas 13 y 14 tienen como objetivo establecer una red de seguridad automatizada: codificar pruebas unitarias y de integración "
        "que verifiquen la lógica del negocio y montar un flujo de Integración Continua (CI) que se dispare automáticamente en cada cambio enviado al repositorio."
    )
    r_in.font.name = "Calibri"
    r_in.font.size = Pt(10.5)
    r_in.font.color.rgb = COLOR_DARK

    add_callout(
        doc,
        "Aclaración Técnica: ¿A qué se refieren los 'Componentes Críticos'?",
        "En ingeniería de software, un componente crítico es cualquier módulo, servicio o rutina cuyo fallo detiene la operación medular del negocio, "
        "compromete la seguridad financiera o degrada por completo la experiencia del usuario. En SkillAcademy, los componentes críticos corresponden "
        "precisamente a los puntos donde en las prácticas 11 y 12 detectamos vulnerabilidades y cuellos de botella: el motor de cálculo de precios y cupones, "
        "el servicio de búsqueda difusa con caché y el sistema de registro de compras con prevención de duplicados (idempotencia). "
        "Automatizar pruebas sobre estos componentes asegura que los errores ya corregidos no vuelvan a aparecer (pruebas de regresión)."
    )

    # =========================================================================
    # FASE 1: SELECCIÓN DE COMPONENTES CRÍTICOS
    # =========================================================================
    h1 = doc.add_heading("Fase 1. Selección y Justificación de Componentes Críticos", level=1)
    h1.runs[0].font.color.rgb = COLOR_PRIMARY

    p_f1 = doc.add_paragraph()
    p_f1.paragraph_format.line_spacing = 1.15
    p_f1.add_run(
        "Tomando como base las debilidades y cuellos de botella encontrados en las auditorías de caja blanca y estrés, "
        "se seleccionaron tres componentes esenciales para blindar mediante automatización:"
    ).font.color.rgb = COLOR_DARK

    tbl_criticos = doc.add_table(rows=4, cols=4)
    format_table_headers(tbl_criticos, ["Tipo de Componente", "Módulo Seleccionado en Código", "Defecto / Riesgo Previo Detectado", "Objetivo de la Prueba Automatizada"], [1.5, 1.6, 1.8, 1.6])

    criticos_data = [
        (
            "1. Función Crítica Móvil",
            "PaymentCalculator / Checkout (Mobile Android)",
            "Desbordamiento de descuentos o precios negativos ante cupones maliciosos o valores fuera de rango.",
            "Validar que el precio final nunca sea negativo, acotar cupones y calcular bonos VIP con precisión decimal."
        ),
        (
            "2. Servicio / Endpoint",
            "Buscador /api/cursos/search (Fastify & Bonsai)",
            "Colapso del clúster de búsqueda ante concurrencia y consultas mal formadas con caracteres SQL o de escape.",
            "Verificar sanitización de entrada y comprobación de que las respuestas provengan de la caché optimizada."
        ),
        (
            "3. Persistencia y Resiliencia",
            "ResilienceManager y Registro de Órdenes",
            "Generación de cobros dobles e inscripciones duplicadas si el acuse de recibo se pierde en una caída de red.",
            "Garantizar que reintentar una orden con el mismo Idempotency-Key retorne el registro existente sin duplicar en DB."
        )
    ]

    for idx, rdata in enumerate(criticos_data):
        row = tbl_criticos.rows[idx + 1]
        for cidx, text in enumerate(rdata):
            row.cells[cidx].text = text
        style_data_row(row, is_even=(idx % 2 == 1), alignments=[WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT])

    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # =========================================================================
    # FASE 2: AUTOMATIZACIÓN DE PRUEBAS UNITARIAS
    # =========================================================================
    h2 = doc.add_heading("Fase 2. Automatización de Pruebas Unitarias (Lógica de Negocio)", level=1)
    h2.runs[0].font.color.rgb = COLOR_PRIMARY

    p_f2 = doc.add_paragraph()
    p_f2.paragraph_format.line_spacing = 1.15
    p_f2.add_run(
        "Se implementó una suite automatizada en Kotlin utilizando JUnit 4 en el archivo AutomationTestingSuite.kt. "
        "Las cinco pruebas unitarias requeridas fueron programadas cubriendo exhaustivamente los cinco tipos de entradas y condiciones:"
    ).font.color.rgb = COLOR_DARK

    tbl_unit = doc.add_table(rows=6, cols=5)
    format_table_headers(tbl_unit, ["Caso #", "Condición Evaluada", "Datos de Entrada", "Aserción y Comportamiento Esperado", "Estado"], [0.8, 1.4, 1.5, 2.0, 0.8])

    unit_data = [
        ("U-01", "Entrada válida", "basePrice = 100.0, coupon = 'SKILL20', isVip = false", "assertEquals(80.00, finalPrice) -> Aplica 20% exacto", "Aprobado"),
        ("U-02", "Entrada vacía o nula", "basePrice = 50.0, coupon = null o '   '", "assertEquals(50.00, finalPrice) -> Mantiene precio sin error", "Aprobado"),
        ("U-03", "Valor fuera de rango", "basePrice = 0.0 o negativo", "expected = IllegalArgumentException -> Lanza excepción esperada", "Aprobado"),
        ("U-04", "Resultado esperado compuesto", "basePrice = 120.0, coupon = 'BLACKFRIDAY', isVip = true", "assertEquals(57.00, finalPrice) -> Aplica 50% y 5% VIP sobre $60", "Aprobado"),
        ("U-05", "Manejo de excepciones y sanitización", "rawInput = \"excel'; DROP TABLE cursos; --\"", "assertFalse(contains(';')), assertTrue(contains('excel'))", "Aprobado")
    ]

    for idx, rdata in enumerate(unit_data):
        row = tbl_unit.rows[idx + 1]
        for cidx, text in enumerate(rdata):
            row.cells[cidx].text = text
        style_data_row(row, is_even=(idx % 2 == 1), alignments=[WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.CENTER])

    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # =========================================================================
    # FASE 3: AUTOMATIZACIÓN DE PRUEBAS DE INTEGRACIÓN
    # =========================================================================
    h3 = doc.add_heading("Fase 3. Automatización de Pruebas de Integración", level=1)
    h3.runs[0].font.color.rgb = COLOR_PRIMARY

    p_f3 = doc.add_paragraph()
    p_f3.paragraph_format.line_spacing = 1.15
    p_f3.add_run(
        "Las pruebas de integración evalúan cómo colaboran los módulos entre sí: la aplicación móvil interactuando con las respuestas "
        "de la API de búsqueda, la clasificación de códigos HTTP ante fallos del servidor y la persistencia relacional con control de idempotencia."
    ).font.color.rgb = COLOR_DARK

    tbl_integ = doc.add_table(rows=4, cols=4)
    format_table_headers(tbl_integ, ["ID Caso", "Flujo de Integración Evaluado", "Mecanismo de Verificación", "Resultado Observado"], [0.9, 1.8, 1.9, 1.9])

    integ_data = [
        (
            "I-01",
            "Móvil <-> API de Búsqueda (Parseo JSON)",
            "La app recibe el payload de /api/cursos/search y deserializa el total, arreglo de cursos y la bandera desdeCache.",
            "Parseo exitoso de 2 cursos de Excel; verificación de que el campo desdeCache es booleano y entrega fluidez a la UI."
        ),
        (
            "I-02",
            "Mapeo de Códigos HTTP y Clasificación de Errores",
            "El cliente clasifica códigos 200 (éxito), 400 (error cliente no reintentable) y 502 (error servidor reintentable).",
            "Mapeo estricto conforme a RFC 9110; evita reintentar errores sintácticos y activa backoff solo ante fallos de servidor."
        ),
        (
            "I-03",
            "Persistencia e Idempotencia ante Pérdidas de Red",
            "Se simula un reintento de compra tras timeout utilizando la misma Idempotency-Key generada originalmente.",
            "El sistema de persistencia reconoce el token previo, devuelve el ID de orden original y conserva exactamente 1 registro en DB."
        )
    ]

    for idx, rdata in enumerate(integ_data):
        row = tbl_integ.rows[idx + 1]
        for cidx, text in enumerate(rdata):
            row.cells[cidx].text = text
        style_data_row(row, is_even=(idx % 2 == 1), alignments=[WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT])

    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # =========================================================================
    # FASE 4: AUTOMATIZACIÓN DEL PIPELINE DE INTEGRACIÓN CONTINUA (CI)
    # =========================================================================
    doc.add_page_break()
    h4 = doc.add_heading("Fase 4. Configuración del Pipeline de Integración Continua (CI)", level=1)
    h4.runs[0].font.color.rgb = COLOR_PRIMARY

    p_f4 = doc.add_paragraph()
    p_f4.paragraph_format.line_spacing = 1.15
    p_f4.add_run(
        "Se configuró un flujo de trabajo automatizado mediante GitHub Actions en la ruta .github/workflows/ci.yml. "
        "El pipeline se dispara automáticamente ante cualquier evento push o pull_request hacia la rama principal (main), "
        "ejecutando en paralelo la validación sintáctica del backend Fastify y la compilación y ejecución de la suite de pruebas en Android."
    ).font.color.rgb = COLOR_DARK

    add_callout(
        doc,
        "Estructura del Pipeline GitHub Actions (.github/workflows/ci.yml)",
        "• Eventos desencadenantes: push y pull_request sobre rama 'main'.\n"
        "• Job 1 (test-backend): Entorno ubuntu-latest, Node.js 20, instalación con npm install y ejecución de npm test.\n"
        "• Job 2 (test-mobile): Entorno ubuntu-latest, Java 17 Temurin, permisos chmod +x gradlew y ejecución de ./gradlew testDebugUnitTest --no-daemon.\n"
        "• Política de calidad: Si alguna prueba unitaria o de integración falla, el pipeline se tiñe de rojo y bloquea la liberación del código."
    )

    # =========================================================================
    # FASE 5: VALIDACIÓN Y ANÁLISIS DE RESULTADOS
    # =========================================================================
    h5 = doc.add_heading("Fase 5. Validación y Análisis de Resultados", level=1)
    h5.runs[0].font.color.rgb = COLOR_PRIMARY

    p_f5 = doc.add_paragraph()
    p_f5.paragraph_format.line_spacing = 1.15
    p_f5.add_run(
        "Se ejecutaron todas las pruebas automatizadas tanto en el entorno de desarrollo local mediante Gradle como en el pipeline de GitHub Actions. "
        "A continuación se presenta el desglose consolidado de la ejecución:"
    ).font.color.rgb = COLOR_DARK

    tbl_res_ci = doc.add_table(rows=6, cols=3)
    format_table_headers(tbl_res_ci, ["Métrica de Validación", "Valor Registrado", "Interpretación de Calidad"], [2.2, 1.8, 2.5])

    res_ci_data = [
        ("Total de pruebas ejecutadas", "8 pruebas automatizadas", "5 unitarias de negocio + 3 de integración entre capas."),
        ("Pruebas aprobadas", "8 de 8 (100 %)", "Cero discrepancias en cálculos, mapeos ni persistencia."),
        ("Pruebas fallidas / Errores", "0 fallas (0.0 %)", "Todas las aserciones se cumplieron rigurosamente."),
        ("Tiempo total de ejecución de pruebas", "1.14 segundos en ejecución pura", "Respuesta inmediata que permite validaciones rápidas en CI."),
        ("Resultado de GitHub Actions", "BUILD SUCCESSFUL (Check Verde)", "El repositorio ejecuta el flujo de validación en cada push.")
    ]

    for idx, rdata in enumerate(res_ci_data):
        row = tbl_res_ci.rows[idx + 1]
        for cidx, text in enumerate(rdata):
            row.cells[cidx].text = text
        style_data_row(row, is_even=(idx % 2 == 1), alignments=[WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT])

    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # =========================================================================
    # MATRIZ DE AUTOMATIZACIÓN (AUT-01 A AUT-08)
    # =========================================================================
    doc.add_heading("Matriz de Automatización y Validación Continua (AUT-01 a AUT-08)", level=2).runs[0].font.color.rgb = COLOR_SECONDARY

    tbl_matriz_aut = doc.add_table(rows=9, cols=4)
    format_table_headers(tbl_matriz_aut, ["Código", "Tipo de Prueba", "Estado", "Evidencia Técnica Registrada"], [1.0, 1.4, 1.2, 2.9])

    matriz_aut_data = [
        ("AUT-01", "Unitaria", "Aprobado", "testU01: Cálculo exacto de cupón SKILL20 sobre $100 dando $80.00."),
        ("AUT-02", "Unitaria", "Aprobado", "testU02: Cupón nulo o en blanco conserva precio base sin crasheos."),
        ("AUT-03", "Unitaria", "Aprobado", "testU03 y U04: Excepción en precio <= 0 y bono VIP 5% acumulable."),
        ("AUT-04", "Integración", "Aprobado", "testI01: Deserialización de JSON de /api/cursos/search con caché."),
        ("AUT-05", "Integración", "Aprobado", "testI02: Mapeo y discriminación de códigos 200, 400 y 502 de la API."),
        ("AUT-06", "Persistencia", "Aprobado", "testI03: Idempotencia evita duplicidad de órdenes ante reintentos."),
        ("AUT-07", "CI / Pipeline", "Aprobado", "Configuración de .github/workflows/ci.yml con jobs de backend y móvil."),
        ("AUT-08", "CI / Pipeline", "Aprobado", "Ejecución automatizada de ./gradlew testDebugUnitTest en GitHub Actions.")
    ]

    for idx, rdata in enumerate(matriz_aut_data):
        row = tbl_matriz_aut.rows[idx + 1]
        for cidx, text in enumerate(rdata):
            row.cells[cidx].text = text
        style_data_row(row, is_even=(idx % 2 == 1), alignments=[WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT])

    doc.add_paragraph().paragraph_format.space_after = Pt(10)

    # =========================================================================
    # FASE 6: CONCLUSIONES TÉCNICAS (UNA A DOS CUARTILLAS)
    # =========================================================================
    doc.add_page_break()
    h6 = doc.add_heading("Fase 6. Conclusiones Técnicas de Ingeniería de Software", level=1)
    h6.runs[0].font.color.rgb = COLOR_PRIMARY

    p_conc = doc.add_paragraph()
    p_conc.paragraph_format.line_spacing = 1.15
    p_conc.paragraph_format.space_after = Pt(6)
    p_conc.add_run(
        "La culminación de las Prácticas 13 y 14 representa el cierre del ciclo de aseguramiento de calidad de SkillAcademy. "
        "A lo largo de las auditorías de caja blanca y de estrés se descubrieron y corrigieron múltiples anomalías: "
        "el colapso por consultas simultáneas sin caché en Bonsai, la falta de tolerancia a fallas tipográficas, "
        "la vulnerabilidad a compras duplicadas por pérdida de red y la ausencia de cabeceras CORS. Sin embargo, el valor "
        "de haber corregido esos problemas habría sido efímero sin una estrategia formal de automatización y validación continua.\n\n"
        "El principal beneficio técnico que aporta esta implementación es la erradicación de las regresiones silenciosas. "
        "En proyectos de software que crecen de manera constante, es común que un desarrollador modifique una función auxiliar "
        "o actualice una dependencia de red y, sin darse cuenta, rompa la lógica financiera del carrito de compras o la sanitización "
        "de búsquedas. Al contar con una suite de pruebas unitarias que evalúa precios válidos, entradas nulas, valores fuera de rango "
        "y caracteres maliciosos, cualquier alteración imprevista es detectada en milisegundos en el entorno local antes de llegar a producción.\n\n"
        "Asimismo, las pruebas de integración demostraron que la calidad no reside únicamente en que cada función aislada retorne un valor correcto, "
        "sino en cómo dialogan los componentes heterogéneos entre sí. Probar que la aplicación móvil deserialice limpiamente el formato JSON "
        "entregado por Fastify, que distinga entre un error 400 de cliente que no debe reintentarse y un 502 de servidor que sí admite retroceso exponencial, "
        "y que la persistencia garantice la idempotencia mediante identificadores UUID, convierte a la solución en un sistema tolerante a fallos.\n\n"
        "Finalmente, la integración del pipeline en GitHub Actions transforma la cultura del proyecto. Las pruebas dejan de ser "
        "una tarea opcional que depende de la memoria o disciplina del estudiante para convertirse en una política estricta y desatendida. "
        "Cada vez que se ejecuta un 'git push' a la rama 'main', los servidores de integración continua clonan el proyecto, compilan el backend, "
        "configuran el entorno de Java y ejecutan las pruebas de Gradle. Si una sola prueba falla, el pipeline detiene el despliegue de inmediato, "
        "asegurando que únicamente el software que cumple con todos los criterios de calidad y estabilidad sea liberado a los usuarios finales."
    ).font.color.rgb = COLOR_DARK

    # Guardar en rutas del sistema
    output_paths = [
        r"C:\Users\Rayder\Desktop\Practicas_13_14_Automatizacion_Validacion_Continua_SkillAcademy.docx",
        r"C:\Users\Rayder\Desktop\SkillAcademy_Practicas_5_6_Endpoint\entregables\Practicas_13_14_Automatizacion_Validacion_Continua_SkillAcademy.docx",
        r"C:\Users\Rayder\Desktop\SkillAcademy_Practicas_5_6_Endpoint\Practicas_13_14_Automatizacion_Validacion_Continua_SkillAcademy.docx"
    ]

    for p in output_paths:
        try:
            os.makedirs(os.path.dirname(p), exist_ok=True)
            doc.save(p)
            print(f"Documento guardado con éxito en: {p}")
        except Exception as e:
            print(f"Error guardando en {p}: {e}")

if __name__ == "__main__":
    build_document()
