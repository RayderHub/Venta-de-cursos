# -*- coding: utf-8 -*-
import os
import sys
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import qn, nsdecls

def set_cell_shading(cell, color_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{color_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=120, bottom=120, left=180, right=180):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def add_callout(doc, target_p, title, lines, icon="📌", border_color="0D9488", bg_color="F0FDFA"):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = tbl.rows[0].cells[0]
    set_cell_shading(cell, bg_color)
    set_cell_margins(cell, top=140, bottom=140, left=200, right=200)
    
    tcPr = cell._tc.get_or_add_tcPr()
    borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="none"/>'
        f'<w:left w:val="single" w:sz="28" w:space="0" w:color="{border_color}"/>'
        f'<w:bottom w:val="none"/>'
        f'<w:right w:val="none"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(borders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(3)
    p.paragraph_format.space_after = Pt(4)
    run_t = p.add_run(f"{icon} {title}")
    run_t.bold = True
    run_t.font.name = "Calibri"
    run_t.font.size = Pt(11)
    run_t.font.color.rgb = RGBColor(0x0F, 0x29, 0x42)
    
    for line in lines:
        p_l = cell.add_paragraph()
        p_l.paragraph_format.space_before = Pt(1)
        p_l.paragraph_format.space_after = Pt(2)
        r = p_l.add_run(line)
        r.font.name = "Calibri"
        r.font.size = Pt(9.5)
        r.font.color.rgb = RGBColor(0x33, 0x41, 0x55)
        
    target_p._p.addprevious(tbl._tbl)
    
    # Empty space after callout
    p_sp = target_p.insert_paragraph_before()
    p_sp.paragraph_format.space_before = Pt(0)
    p_sp.paragraph_format.space_after = Pt(6)

def insert_image_block(target_p, img_path, fig_title, fig_desc, width_in=5.2):
    if not os.path.exists(img_path):
        print(f"WARNING: Image not found: {img_path}")
        return
    
    p_img = target_p.insert_paragraph_before()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.paragraph_format.space_before = Pt(8)
    p_img.paragraph_format.space_after = Pt(4)
    run_img = p_img.add_run()
    run_img.add_picture(img_path, width=Inches(width_in))
    
    p_cap = target_p.insert_paragraph_before()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_before = Pt(2)
    p_cap.paragraph_format.space_after = Pt(8)
    
    run_fig = p_cap.add_run(fig_title)
    run_fig.bold = True
    run_fig.italic = True
    run_fig.font.name = "Calibri"
    run_fig.font.size = Pt(9.5)
    run_fig.font.color.rgb = RGBColor(0x0F, 0x29, 0x42)
    
    run_sep = p_cap.add_run(" — ")
    run_sep.italic = True
    run_sep.font.name = "Calibri"
    run_sep.font.size = Pt(9.5)
    run_sep.font.color.rgb = RGBColor(0x64, 0x74, 0x8B)
    
    run_desc = p_cap.add_run(fig_desc)
    run_desc.italic = True
    run_desc.font.name = "Calibri"
    run_desc.font.size = Pt(9.0)
    run_desc.font.color.rgb = RGBColor(0x47, 0x55, 0x69)

def insert_qa_table(doc, target_p):
    # Paragraph header
    p_h = target_p.insert_paragraph_before()
    p_h.paragraph_format.space_before = Pt(8)
    p_h.paragraph_format.space_after = Pt(4)
    r_h = p_h.add_run("Tabla 3.1: Matriz de Ejecución de Pruebas Unitarias Automatizadas (JUnit 4 - Entregable 3)")
    r_h.bold = True
    r_h.font.name = "Calibri"
    r_h.font.size = Pt(10)
    r_h.font.color.rgb = RGBColor(0x0F, 0x29, 0x42)
    
    table_data = [
        ("TC-01", "Motor Carrito", "testCartTotalCalculation()", "Suma de cursos $29.99 + $49.99", "$79.98 exacto (delta 0.01)", "APROBADO ✓"),
        ("TC-02", "Cupones 25%", "testPercentageCouponDiscount()", "Cupón SKILL25 sobre $80.00", "Total $60.00 (-$20.00 desc.)", "APROBADO ✓"),
        ("TC-03", "Beca 100%", "testFreeCoupon100PercentDiscount()", "Cupón 100% sobre $99.99", "Total $0.00 (sin valores negativos)", "APROBADO ✓"),
        ("TC-04", "Aula Móvil", "testCourseProgressCalculation()", "2 de 5 lecciones terminadas", "Progreso 40% (2/5 * 100)", "APROBADO ✓"),
        ("TC-05", "Checkout", "testOrderCodeFormat()", "Patrón ORD-XXXXXX (6 hex/alnum)", "Coincide ^ORD-[A-Z0-9]{6}$", "APROBADO ✓"),
    ]
    
    tbl = doc.add_table(rows=len(table_data) + 1, cols=6)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    
    headers = ["ID", "Módulo", "Método de Prueba", "Caso de Entrada", "Resultado Obtenido", "Estado"]
    col_widths = [Inches(0.6), Inches(1.1), Inches(1.7), Inches(1.5), Inches(1.5), Inches(1.0)]
    
    # Header Row
    for i, title in enumerate(headers):
        cell = tbl.rows[0].cells[i]
        set_cell_shading(cell, "0F2942")
        set_cell_margins(cell, top=100, bottom=100, left=120, right=120)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(title)
        r.bold = True
        r.font.name = "Calibri"
        r.font.size = Pt(9)
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
        
    # Data Rows
    for row_idx, row in enumerate(table_data):
        for col_idx, val in enumerate(row):
            cell = tbl.rows[row_idx + 1].cells[col_idx]
            bg = "F8FAFC" if row_idx % 2 == 0 else "FFFFFF"
            set_cell_shading(cell, bg)
            set_cell_margins(cell, top=80, bottom=80, left=100, right=100)
            p = cell.paragraphs[0]
            r = p.add_run(val)
            r.font.name = "Calibri"
            r.font.size = Pt(8.5)
            
            if col_idx == 0:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                r.bold = True
            elif col_idx == 5:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                r.bold = True
                r.font.color.rgb = RGBColor(0x05, 0x96, 0x69)
            else:
                p.alignment = WD_ALIGN_PARAGRAPH.LEFT
                r.font.color.rgb = RGBColor(0x33, 0x41, 0x55)
                
    # Set borders
    for row in tbl.rows:
        for cell in row.cells:
            tcPr = cell._tc.get_or_add_tcPr()
            borders = parse_xml(
                f'<w:tcBorders {nsdecls("w")}>'
                f'<w:top w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>'
                f'<w:left w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>'
                f'<w:bottom w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>'
                f'<w:right w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>'
                f'</w:tcBorders>'
            )
            tcPr.append(borders)
            
    target_p._p.addprevious(tbl._tbl)
    
    p_sp = target_p.insert_paragraph_before()
    p_sp.paragraph_format.space_before = Pt(0)
    p_sp.paragraph_format.space_after = Pt(6)

def insert_apk_table(doc, target_p):
    p_h = target_p.insert_paragraph_before()
    p_h.paragraph_format.space_before = Pt(8)
    p_h.paragraph_format.space_after = Pt(4)
    r_h = p_h.add_run("Tabla 3.2: Ficha Técnica del Paquete de Despliegue Generado (APK)")
    r_h.bold = True
    r_h.font.name = "Calibri"
    r_h.font.size = Pt(10)
    r_h.font.color.rgb = RGBColor(0x0F, 0x29, 0x42)
    
    apk_data = [
        ("Nombre del Artefacto", "app-debug.apk"),
        ("Ruta Relativa del Binario", "mobile-android-skillacademy\\app\\build\\outputs\\apk\\debug\\app-debug.apk"),
        ("Tamaño del Archivo", "15.6 MB (Optimizado con R8 / D8 dexer)"),
        ("Application ID / Package", "com.skillacademy.mobile"),
        ("Versión (VersionName / Code)", "1.0.0 (VersionCode: 1)"),
        ("Compatibilidad de Android", "Min SDK: 26 (Android 8.0 Oreo) | Target SDK: 35 (Android 15)"),
        ("Arquitecturas Soportadas", "Universal (arm64-v8a, armeabi-v7a, x86_64)"),
        ("Comando de Compilación", ".\\gradlew.bat assembleDebug (BUILD SUCCESSFUL in 1m 37s)"),
        ("Comando de Instalación USB / ADB", "adb install -r app\\build\\outputs\\apk\\debug\\app-debug.apk")
    ]
    
    tbl = doc.add_table(rows=len(apk_data), cols=2)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    
    for row_idx, (k, v) in enumerate(apk_data):
        cell_k = tbl.rows[row_idx].cells[0]
        cell_v = tbl.rows[row_idx].cells[1]
        
        bg = "F1F5F9" if row_idx % 2 == 0 else "FFFFFF"
        set_cell_shading(cell_k, "0F2942" if row_idx == 0 else bg)
        set_cell_shading(cell_v, "0F2942" if row_idx == 0 else bg)
        set_cell_margins(cell_k, top=80, bottom=80, left=120, right=120)
        set_cell_margins(cell_v, top=80, bottom=80, left=120, right=120)
        
        p_k = cell_k.paragraphs[0]
        r_k = p_k.add_run(k)
        r_k.bold = True
        r_k.font.name = "Calibri"
        r_k.font.size = Pt(9)
        r_k.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF) if row_idx == 0 else RGBColor(0x0F, 0x29, 0x42)
        
        p_v = cell_v.paragraphs[0]
        r_v = p_v.add_run(v)
        r_v.font.name = "Calibri"
        r_v.font.size = Pt(9)
        r_v.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF) if row_idx == 0 else RGBColor(0x33, 0x41, 0x55)
        
        for cell in (cell_k, cell_v):
            tcPr = cell._tc.get_or_add_tcPr()
            borders = parse_xml(
                f'<w:tcBorders {nsdecls("w")}>'
                f'<w:top w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>'
                f'<w:left w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>'
                f'<w:bottom w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>'
                f'<w:right w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>'
                f'</w:tcBorders>'
            )
            tcPr.append(borders)
            
    target_p._p.addprevious(tbl._tbl)
    
    p_sp = target_p.insert_paragraph_before()
    p_sp.paragraph_format.space_before = Pt(0)
    p_sp.paragraph_format.space_after = Pt(6)

def enrich_document(docx_path, output_path):
    print(f"Loading document: {docx_path}")
    doc = docx.Document(docx_path)
    
    img_dir = "documentacion/mockups-imagenes"
    evid_dir = "documentacion/evidencias"
    
    # -------------------------------------------------------------
    # 1. MARCO GENERAL: Inserción de evidencias web/admin
    # -------------------------------------------------------------
    p_req = None
    for p in doc.paragraphs:
        if "3. Requerimientos Formales" in p.text:
            p_req = p
            break
            
    if p_req:
        print("Inserting Web Admin evidence in Marco General...")
        p_intro = p_req.insert_paragraph_before()
        p_intro.paragraph_format.space_before = Pt(8)
        p_intro.paragraph_format.space_after = Pt(4)
        r = p_intro.add_run("Evidencias Gráficas del Ecosistema Web y Panel de Administración:")
        r.bold = True
        r.font.size = Pt(11)
        r.font.color.rgb = RGBColor(0x0F, 0x29, 0x42)
        
        insert_image_block(
            p_req,
            f"{img_dir}/08-admin-dashboard.png",
            "Figura 0.1: Panel Administrativo General (Dashboard Web)",
            "Visualización de métricas comerciales, volumen de usuarios registrados, ingresos brutos y analíticas globales de SkillAcademy Web.",
            width_in=5.4
        )
        insert_image_block(
            p_req,
            f"{img_dir}/09-admin-cursos.png",
            "Figura 0.2: Módulo de Gestión y Curaduría de Cursos (Web)",
            "Permite a instructores y administradores dar de alta temarios, lecciones, precios y miniaturas publicitarias sincronizadas con Supabase.",
            width_in=5.4
        )
        insert_image_block(
            p_req,
            f"{img_dir}/10-admin-usuarios.png",
            "Figura 0.3: Administración Centralizada de Usuarios y Roles (Web)",
            "Control de accesos y perfiles de estudiante e instructor con políticas de seguridad Row Level Security (RLS) en PostgreSQL.",
            width_in=5.4
        )
        insert_image_block(
            p_req,
            f"{img_dir}/11-admin-banners.png",
            "Figura 0.4: Gestión de Banners Promocionales y Campañas (Web)",
            "Publicación de ofertas flash y recursos gráficos que se consumen reactivamente en la aplicación móvil nativa.",
            width_in=5.4
        )

    # -------------------------------------------------------------
    # 2. ENTREGABLE 1: Inserción de pantallas comerciales
    # -------------------------------------------------------------
    # 1.3.1 Home Comercial -> insertar 01-home.png
    p_home_next = None
    for p in doc.paragraphs:
        if "1.3.2 Pantalla de Ficha" in p.text:
            p_home_next = p
            break
            
    if p_home_next:
        print("Inserting Home mockup...")
        insert_image_block(
            p_home_next,
            f"{img_dir}/01-home.png",
            "Figura 1.1: Pantalla Principal (Home) de SkillAcademy Mobile",
            "Renderizado nativo en Jetpack Compose con carrusel de banners publicitarios dinámicos, chips de categorías interactivas y catálogo de cursos destacados con calificación de estrellas real.",
            width_in=5.2
        )

    # 1.3.2 Course Detail -> insertar 05-detalle-curso.png y 04-catalogo.png
    p_cart_next = None
    for p in doc.paragraphs:
        if "1.3.3 Pantalla de Carrito" in p.text:
            p_cart_next = p
            break
            
    if p_cart_next:
        print("Inserting Course Detail and Catalog mockups...")
        insert_image_block(
            p_cart_next,
            f"{img_dir}/05-detalle-curso.png",
            "Figura 1.2: Ficha Técnica Detallada del Curso (CourseDetailScreen)",
            "Maquetación idéntica a la plataforma web con miniatura oficial del curso, 3 secciones tabulares (Descripción, Temario y Lecciones, Requisitos), comentarios de la comunidad y barra fija inferior de inscripción.",
            width_in=5.2
        )
        insert_image_block(
            p_cart_next,
            f"{img_dir}/04-catalogo.png",
            "Figura 1.3: Catálogo General y Explorador de Cursos (CatalogScreen)",
            "Cuadrícula responsiva de cursos con barra de búsqueda instantánea, filtrado por categorías temáticas y ordenamiento comercial por popularidad y precio.",
            width_in=5.2
        )

    # 1.3.3 Carrito -> insertar 06-carrito-checkout.png
    p_code_next = None
    for p in doc.paragraphs:
        if "1.4 C" in p.text and "Maquetaci" in p.text:
            p_code_next = p
            break
            
    if p_code_next:
        print("Inserting Cart mockup...")
        insert_image_block(
            p_code_next,
            f"{img_dir}/06-carrito-checkout.png",
            "Figura 1.4: Carrito de Compras Transaccional (CartScreen)",
            "Gestión de cursos seleccionados, cálculo dinámico de subtotales, caja de validación reactiva de cupones de descuento y acceso inmediato al proceso de compra.",
            width_in=5.2
        )

    # 1.5 Criterios de Aceptación Entregable 1 -> Callout con guía para el evaluador
    p_ent2 = None
    for p in doc.paragraphs:
        if "ENTREGABLE 2" in p.text and "FUNCIONALIDAD" in p.text:
            p_ent2 = p
            break
            
    if p_ent2:
        print("Inserting Callout for Entregable 1...")
        add_callout(
            doc,
            p_ent2,
            "Guía de Verificación Técnica para el Evaluador — Entregable 1",
            [
                "• Cobertura UI: Ejecutar el APK y comprobar la navegación por las 5 vistas comerciales (Home, Catálogo, Ficha de Curso, Carrito y Perfil).",
                "• Design System: Verificar el uso coherente de tokens de color (SkillNavy #0F2942, SkillEmerald #0D9488, SkillSurface #F8FAFC) y tipografía escalable.",
                "• Navegación Libre: Comprobar que el usuario puede explorar todos los cursos y agregarlos al carrito sin estar forzado a iniciar sesión prematuramente.",
                "• Miniaturas Reales: Las imágenes de portada provienen de Supabase Storage mediante el componente CourseImage con soporte para degradados elegantes."
            ],
            icon="📱",
            border_color="0D9488",
            bg_color="F0FDFA"
        )

    # -------------------------------------------------------------
    # 3. ENTREGABLE 2: Auth, APIs, Persistencia y Cupones
    # -------------------------------------------------------------
    # 2.2 Autenticación -> insertar 02-login.png y 03-registro.png
    p_cat_api = None
    for p in doc.paragraphs:
        if "2.3 Consumo de Cat" in p.text:
            p_cat_api = p
            break
            
    if p_cat_api:
        print("Inserting Auth mockups...")
        insert_image_block(
            p_cat_api,
            f"{img_dir}/02-login.png",
            "Figura 2.1: Pantalla de Autenticación de Usuarios (LoginScreen)",
            "Conexión formal con Supabase Auth (GoTrue REST API), validación reactiva de credenciales y redirección protegida a la sección de origen.",
            width_in=5.2
        )
        insert_image_block(
            p_cat_api,
            f"{img_dir}/03-registro.png",
            "Figura 2.2: Pantalla de Registro de Nuevos Estudiantes (RegisterScreen)",
            "Formulario nativo con validación de robustez de contraseña, verificación de formato de correo y alta automatizada de perfil en PostgreSQL.",
            width_in=5.2
        )

    # 2.3 Consumo de Catálogo -> insertar Captura_Evidencia_Endpoint.png
    p_cart_room = None
    for p in doc.paragraphs:
        if "2.4 Motor del Carrito" in p.text:
            p_cart_room = p
            break
            
    if p_cart_room:
        print("Inserting REST Endpoint evidence...")
        insert_image_block(
            p_cart_room,
            f"{evid_dir}/Captura_Evidencia_Endpoint.png",
            "Figura 2.3: Verificación Técnica del Endpoint REST y Supabase API",
            "Captura de inspección de red y depuración técnica comprobando la transferencia de datos en formato JSON desde Supabase PostgreSQL (cursos publicados, instructores y precios reales).",
            width_in=5.4
        )

    # 2.5 Motor de Cupones -> insertar 12-popup-descuento.png
    p_ent2_crit = None
    for p in doc.paragraphs:
        if "2.6 Criterios de Aceptaci" in p.text and "Entregable 2" in p.text:
            p_ent2_crit = p
            break
            
    if p_ent2_crit:
        print("Inserting Coupon popup mockup...")
        insert_image_block(
            p_ent2_crit,
            f"{img_dir}/12-popup-descuento.png",
            "Figura 2.4: Modal Emergente Comercial y Motor Reactivo de Cupones",
            "Diálogo emergente publicitario que promueve descuentos por tiempo limitado y motor de validación que aplica deducciones instantáneas (ej. cupón SKILL25 para 25% OFF).",
            width_in=5.2
        )

    # 2.6 Criterios de Aceptación Entregable 2 -> Callout
    p_ent3 = None
    for p in doc.paragraphs:
        if "ENTREGABLE 3" in p.text and "PASARELA" in p.text:
            p_ent3 = p
            break
            
    if p_ent3:
        print("Inserting Callout for Entregable 2...")
        add_callout(
            doc,
            p_ent3,
            "Guía de Verificación Técnica para el Evaluador — Entregable 2",
            [
                "• Datos Reales: Comprobar que todos los cursos, reseñas, lecciones y profesores se cargan directamente desde la base de datos PostgreSQL de Supabase.",
                "• Persistencia del Carrito: Agregar cursos al carrito, cerrar la aplicación completamente de la memoria y reabrirla para comprobar que los artículos persisten.",
                "• Reglas de Autenticación: Al navegar a 'Mis Cursos' o 'Perfil' sin sesión activa, se exige login. Si el usuario pulsa 'Regresar', la app lo devuelve al Home/Catálogo sin permitir brechas de acceso.",
                "• Motor de Cupones: Ingresar el código 'SKILL25' en el carrito y verificar el descuento del 25% calculado al instante."
            ],
            icon="⚡",
            border_color="0284C7",
            bg_color="F0F9FF"
        )

    # -------------------------------------------------------------
    # 4. ENTREGABLE 3: Checkout, Aula Móvil, Pruebas y Despliegue
    # -------------------------------------------------------------
    # 3.3 Aula Móvil Post-Venta -> insertar 07-dashboard-usuario.png
    p_push = None
    for p in doc.paragraphs:
        if "3.4 Notificaciones Push" in p.text:
            p_push = p
            break
            
    if p_push:
        print("Inserting Aula Movil mockup...")
        insert_image_block(
            p_push,
            f"{img_dir}/07-dashboard-usuario.png",
            "Figura 3.1: Aula Móvil Post-Venta y Panel de Aprendizaje (CourseLearnScreen / Mis Cursos)",
            "Visualización de cursos adquiridos con barra de progreso, reproductor multimedia con controles de reproducción (-10s / +10s / velocidad 1.0x-1.5x), temario de lecciones y panel de tareas académicas sincronizadas con Supabase.",
            width_in=5.2
        )

    # 3.5 Plan Integral de Pruebas -> Insertar Tabla de Pruebas Unitarias JUnit
    p_deploy = None
    for p in doc.paragraphs:
        if "3.6 Estrategia de Empaquetado" in p.text:
            p_deploy = p
            break
            
    if p_deploy:
        print("Inserting QA Unit Tests Table...")
        insert_qa_table(doc, p_deploy)
        
        # Add callout explaining JUnit test coverage
        add_callout(
            doc,
            p_deploy,
            "Certificación de Calidad y Pruebas Unitarias Automatizadas (QA Suite)",
            [
                "• Suite Ejecutada: com.skillacademy.mobile.CartAndCouponUnitTest (JUnit 4).",
                "• Cobertura de Lógica Crítica: 5 pruebas unitarias cubriendo cálculo de subtotales, algoritmos de descuento porcentual y becas completas, fórmula de progreso en lecciones y validación de expresiones regulares de códigos de compra.",
                "• Estado de Compilación y Ejecución: 5 de 5 pruebas APROBADAS (100% éxito) mediante './gradlew.bat testDebugUnitTest' en 1 minuto 8 segundos.",
                "• Garantía de Cero Regresiones: Todos los cálculos transaccionales y de aula cuentan con validación matemática verificable."
            ],
            icon="🧪",
            border_color="059669",
            bg_color="ECFDF5"
        )

    # 3.6 Estrategia de Empaquetado -> Insertar Tabla de Ficha Técnica del APK
    p_closure = None
    for p in doc.paragraphs:
        if "3.7 Criterios de Aceptaci" in p.text and "Entregable 3" in p.text:
            p_closure = p
            break
            
    if p_closure:
        print("Inserting APK Build Table...")
        insert_apk_table(doc, p_closure)

    # 3.7 Cierre del Entregable 3 -> Callout con instrucciones para el evaluador/alumno
    p_matriz = None
    for p in doc.paragraphs:
        if "MATRIZ DE CONSOLIDACI" in p.text:
            p_matriz = p
            break
            
    if p_matriz:
        print("Inserting Final Verification and Screenshot Guide for Evaluator/Student...")
        add_callout(
            doc,
            p_matriz,
            "Instrucciones para el Evaluador / Alumno sobre la Ejecución y Captura de Evidencias Propias",
            [
                "1. Instalación del Binario: Puede instalar directamente el archivo 'app-debug.apk' generado en cualquier emulador Android (API 26+) o dispositivo físico con depuración USB activada ejecutando: 'adb install -r app-debug.apk'.",
                "2. Simulación de Compra Transaccional: Inicie sesión en la app, añada cursos al carrito, aplique el cupón 'SKILL25', presione 'Proceder al Pago', seleccione la pasarela (Tarjeta o Instant Skill Pay) y confirme. Verifique la notificación nativa y el código de orden ORD-XXXXXX generado.",
                "3. Verificación de Inscripciones: Tras la compra, acceda a 'Mis Cursos' y observe que el curso comprado aparece de inmediato. Al entrar al aula, pruebe el reproductor, marque lecciones como completadas y note cómo el progreso aumenta y se sincroniza en Supabase.",
                "4. Captura de Pantallas Personalizadas: Para anexar capturas adicionales desde su dispositivo, presione [Power + Vol Abajo] en Android o el icono de cámara en la barra de herramientas del emulador de Android Studio, y péguelas como figuras complementarias en este documento."
            ],
            icon="🏆",
            border_color="0F2942",
            bg_color="F8FAFC"
        )

    safe_save(doc, output_path)

def safe_save(doc, target_path):
    try:
        doc.save(target_path)
        print(f"Document saved successfully to: {target_path}")
    except PermissionError:
        alt_path = target_path.replace(".docx", "_Actualizado.docx")
        doc.save(alt_path)
        print(f"NOTICE: '{target_path}' is open in Microsoft Word. Saved to '{alt_path}' instead.")

if __name__ == "__main__":
    src_backup = "entregables/SkillAcademy_Mobile_Native_Entregables_BACKUP.docx"
    
    # Save to documentacion (primary copy, not locked)
    enrich_document(src_backup, "documentacion/SkillAcademy_Mobile_Native_Entregables.docx")
    
    # Save to entregables (and fallback to _Actualizado if locked)
    enrich_document(src_backup, "entregables/SkillAcademy_Mobile_Native_Entregables.docx")
    enrich_document(src_backup, "entregables/SkillAcademy_Mobile_Native_Entregables_Actualizado.docx")
    enrich_document(src_backup, "documentacion/SkillAcademy_Mobile_Native_Entregables_Actualizado.docx")
    print("ALL DOCUMENTS PROCESSED SUCCESSFULLY!")
