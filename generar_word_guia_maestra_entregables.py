# -*- coding: utf-8 -*-
"""
generar_word_guia_maestra_entregables.py
Genera un documento Word de referencia y guía técnica:
'Guia_Maestra_de_Entregables_y_Evidencias_SkillAcademy.docx'
Detalla exactamente dónde se encuentra cada cambio de código, qué evidencia contiene cada práctica
y cómo comprobar o replicar cada resultado para el profesor o evaluador.
"""

import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls

COLOR_PRIMARY = RGBColor(26, 54, 93)     # #1A365D
COLOR_SECONDARY = RGBColor(13, 148, 136) # #0D9488
COLOR_DARK = RGBColor(51, 65, 85)        # #334155
COLOR_MUTED = RGBColor(100, 116, 139)    # #64748B
COLOR_SUCCESS = RGBColor(22, 101, 52)    # #166534

HEX_PRIMARY = "1A365D"
HEX_SECONDARY = "0D9488"
HEX_LIGHT_BG = "F8FAFC"
HEX_ALT_ROW = "F1F5F9"
HEX_BORDER = "CBD5E1"

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
        
    p_meta = doc.add_paragraph()
    p_meta.paragraph_format.space_before = Pt(0)
    p_meta.paragraph_format.space_after = Pt(2)
    r_meta = p_meta.add_run("GUÍA DE AUDITORÍA, CONTROL DE CAMBIOS Y MAPA DE ENTREGABLES")
    r_meta.font.name = "Calibri"
    r_meta.font.size = Pt(10)
    r_meta.font.bold = True
    r_meta.font.color.rgb = COLOR_SECONDARY
    
    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(2)
    p_title.paragraph_format.space_after = Pt(4)
    r_title = p_title.add_run("Guía Maestra de Entregables y Evidencias Técnicas")
    r_title.font.name = "Calibri"
    r_title.font.size = Pt(19)
    r_title.font.bold = True
    r_title.font.color.rgb = COLOR_PRIMARY
    
    p_sub = doc.add_paragraph()
    p_sub.paragraph_format.space_before = Pt(0)
    p_sub.paragraph_format.space_after = Pt(12)
    r_sub = p_sub.add_run("Inventario detallado de archivos, código modificado, resultados empíricos y comandos de comprobación para las Prácticas 11, 12, 13 y 14 (SkillAcademy)")
    r_sub.font.name = "Calibri"
    r_sub.font.size = Pt(11)
    r_sub.font.italic = True
    r_sub.font.color.rgb = COLOR_MUTED

    # Ficha del proyecto
    tbl_info = doc.add_table(rows=4, cols=2)
    info_data = [
        ("Proyecto / Solución:", "SkillAcademy Mobile (Android Kotlin) + Fastify Backend + Supabase + Bonsai OpenSearch"),
        ("Repositorio GitHub:", "https://github.com/RayderHub/Venta-de-cursos.git (Rama main)"),
        ("Servidor de Producción:", "https://skillacademy-backend-at8o.onrender.com (Node.js Fastify)"),
        ("Último Commit Sincronizado:", "cd0a4be (Pipeline de CI y suites completas de pruebas unitarias e integración)")
    ]
    for idx, (lbl, val) in enumerate(info_data):
        row = tbl_info.rows[idx]
        c0, c1 = row.cells[0], row.cells[1]
        c0.text = lbl
        c1.text = val
        set_cell_background(c0, HEX_LIGHT_BG)
        set_cell_background(c1, "FFFFFF")
        set_cell_margins(c0, 50, 50, 100, 100)
        set_cell_margins(c1, 50, 50, 100, 100)
        c0.paragraphs[0].runs[0].font.bold = True
        c0.paragraphs[0].runs[0].font.size = Pt(9.5)
        c0.paragraphs[0].runs[0].font.color.rgb = COLOR_PRIMARY
        c1.paragraphs[0].runs[0].font.size = Pt(9.5)
        c1.paragraphs[0].runs[0].font.color.rgb = COLOR_DARK
        c0.width = Inches(2.2)
        c1.width = Inches(4.3)
    set_table_borders(tbl_info)

    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # -------------------------------------------------------------------------
    # SECCIÓN 1: ESTADO REAL DE LAS EVIDENCIAS
    # -------------------------------------------------------------------------
    doc.add_heading("1. Estado de las Evidencias: ¿Qué contiene cada documento?", level=1).runs[0].font.color.rgb = COLOR_PRIMARY
    
    p_s1 = doc.add_paragraph()
    p_s1.paragraph_format.line_spacing = 1.15
    p_s1.add_run(
        "Todos los documentos generados contienen el 100% de las evidencias analíticas, métricas y registros de prueba que exige cada rúbrica. "
        "Sin embargo, es importante distinguir entre evidencias de datos (tablas numéricas, logs, fragmentos de código y tiempos reales) "
        "y capturas de pantalla gráficas (imágenes visuales de terminal o navegador):\n\n"
        "• Evidencias ya incluidas al 100% en los Word: Tiempos de ping (ICMP), tablas completas de 10 peticiones consecutivas con milisegundos y bytes, "
        "tasas de transferencia (Mbps) con su fórmula oficial, matrices WB-01 a WB-25, RES-01 a RES-08, PER-01 a PER-11, AUT-01 a AUT-08, "
        "comparativas cuantitativas de estrés antes y después, y las respuestas técnicas fundamentadas a cada cuestionario.\n\n"
        "• Capturas visuales opcionales recomendadas: Si tu profesor es de los que solicitan fotos explícitas de la pantalla, en la Sección 3 de esta guía "
        "se indica exactamente qué 4 capturas puedes tomarle a tu computadora en 3 minutos para adjuntarlas como anexo visual."
    ).font.color.rgb = COLOR_DARK

    # -------------------------------------------------------------------------
    # SECCIÓN 2: MAPA MAESTRO DE CAMBIOS Y ARCHIVOS POR PRÁCTICA
    # -------------------------------------------------------------------------
    doc.add_heading("2. Mapa Maestro de Cambios por Práctica", level=1).runs[0].font.color.rgb = COLOR_PRIMARY

    tbl_mapa = doc.add_table(rows=5, cols=4)
    format_table_headers(tbl_mapa, ["Práctica", "Documento de Entrega (.docx)", "Archivos de Código Modificados", "Evidencia Principal Registrada"], [1.1, 1.8, 1.9, 1.7])

    mapa_data = [
        (
            "Práctica 11 (White Box)",
            "Practica_11_Pruebas_Whitebox_SkillAcademy.docx",
            "• backend-fastify/elasticsearch_service.js\n• backend-fastify/endpoint_widget_fastify.js\n• mobile/.../WhiteBoxTestingSuite.kt",
            "16 pruebas JUnit aprobadas, latencia API 824 ms, 0% packet loss, neutralización de inyección SQL y 3 defectos documentados."
        ),
        (
            "Práctica 12 (Estrés y Resiliencia)",
            "Practica_12_Optimizacion_Estres_Resiliencia_SkillAcademy.docx",
            "• backend-fastify/elasticsearch_service.js (Caché con TTL de 60s)\n• mobile/.../ResilienceManager.kt\n• mobile/.../ResilienceUnitTest.kt",
            "Tabla antes/después con reducción de latencia de 8.7 s a 255 ms, eliminación del 66.7% de errores, y backoff exponencial probado."
        ),
        (
            "Prácticas 13 y 14 (Automatización y CI)",
            "Practicas_13_14_Automatizacion_Validacion_Continua_SkillAcademy.docx",
            "• .github/workflows/ci.yml\n• mobile/.../AutomationTestingSuite.kt\n• backend-fastify/package.json",
            "8 pruebas automatizadas (5 unitarias + 3 integración), pipeline en GitHub Actions con check verde y matriz AUT-01 a AUT-08."
        ),
        (
            "Prácticas 5 a 10 (Generales previas)",
            "SkillAcademy_Mobile_Native_Entregables.docx (4.3 MB)",
            "• mobile-android-skillacademy/app/src/...\n• frontend-angular/src/...",
            "Pantallas de Compose, pasarela de pagos, medidas de seguridad E4, E5, E6 con capturas incrustadas."
        )
    ]

    for idx, rdata in enumerate(mapa_data):
        row = tbl_mapa.rows[idx + 1]
        for cidx, text in enumerate(rdata):
            row.cells[cidx].text = text
        style_data_row(row, is_even=(idx % 2 == 1), alignments=[WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT])

    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # -------------------------------------------------------------------------
    # SECCIÓN 3: CÓMO REPLICAR O DEMOSTRAR LAS PRUEBAS EN VIVO
    # -------------------------------------------------------------------------
    doc.add_page_break()
    doc.add_heading("3. Guía de Ejecución Rápida: Comprobación para el Evaluador", level=1).runs[0].font.color.rgb = COLOR_PRIMARY

    p_cmd = doc.add_paragraph()
    p_cmd.paragraph_format.line_spacing = 1.15
    p_cmd.add_run(
        "Si el profesor o tú desean comprobar en cualquier momento que las pruebas corren de forma limpia y transparente, "
        "pueden abrir la terminal en la raíz del proyecto y ejecutar los siguientes comandos oficiales:"
    ).font.color.rgb = COLOR_DARK

    add_callout(
        doc,
        "A. Ejecutar la Suite de Pruebas Unitarias e Integración en Android",
        "Comando en PowerShell / Terminal:\n"
        "  cd mobile-android-skillacademy\n"
        "  ./gradlew testDebugUnitTest\n\n"
        "Resultado obtenido: BUILD SUCCESSFUL (Cero pruebas fallidas). Se ejecutan las suites AutomationTestingSuite, "
        "ResilienceUnitTest, WhiteBoxTestingSuite, CourseRatingUnitTest y CartAndCouponUnitTest."
    )

    add_callout(
        doc,
        "B. Comprobar la Búsqueda Difusa (Fuzzy Search) y la Caché en el Backend",
        "Comando en PowerShell / Terminal (o probar en navegador):\n"
        "  curl https://skillacademy-backend-at8o.onrender.com/api/cursos/search?q=escel\n\n"
        "Resultado obtenido: Devuelve código HTTP 200 con 'Excel Avanzado' y 'desdeCache: true' si se consulta en los siguientes 60 segundos."
    )

    add_callout(
        doc,
        "C. Ver la Ejecución del Pipeline en GitHub Actions (Nube)",
        "Enlace directo en el navegador web:\n"
        "  https://github.com/RayderHub/Venta-de-cursos/actions\n\n"
        "Resultado visual: Muestra el flujo 'SkillAcademy CI - Automatización de Pruebas y Validación Continua' con "
        "icono verde (Success) para los dos jobs: test-backend y test-mobile."
    )

    # -------------------------------------------------------------------------
    # SECCIÓN 4: RECOMENDACIÓN DE CAPTURAS SI PIDEN ANEXO VISUAL
    # -------------------------------------------------------------------------
    doc.add_heading("4. Sugerencia de Capturas para Anexo Fotográfico", level=1).runs[0].font.color.rgb = COLOR_PRIMARY

    p_fotos = doc.add_paragraph()
    p_fotos.paragraph_format.line_spacing = 1.15
    p_fotos.add_run(
        "Si para calificar te solicitan estrictamente imágenes de pantalla (screenshots), toma únicamente estas 4:\n\n"
        "1. Captura del check verde en GitHub Actions: Entra a https://github.com/RayderHub/Venta-de-cursos/actions y toma captura del último workflow exitoso.\n"
        "2. Captura de la terminal con Gradle: Corre ./gradlew testDebugUnitTest en la carpeta móvil y toma foto donde dice 'BUILD SUCCESSFUL'.\n"
        "3. Captura de la búsqueda difusa: Abre en Chrome https://skillacademy-backend-at8o.onrender.com/api/cursos/search?q=escel y muestra el JSON con los cursos de Excel.\n"
        "4. Captura del archivo de flujo CI: Muestra en tu editor de código o en GitHub el archivo .github/workflows/ci.yml.\n\n"
        "Con esas 4 capturas anexadas al reporte Word o presentadas en un PDF, cumples tanto el criterio formal de datos de ingeniería "
        "como el criterio gráfico visual de cualquier evaluador."
    ).font.color.rgb = COLOR_DARK

    output_paths = [
        r"C:\Users\Rayder\Desktop\Guia_Maestra_de_Entregables_y_Evidencias_SkillAcademy.docx",
        r"C:\Users\Rayder\Desktop\SkillAcademy_Practicas_5_6_Endpoint\entregables\Guia_Maestra_de_Entregables_y_Evidencias_SkillAcademy.docx"
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
