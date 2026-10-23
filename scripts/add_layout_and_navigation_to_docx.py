# -*- coding: utf-8 -*-
"""
add_layout_and_navigation_to_docx.py
===================================
Agrega DOS CAMPOS NUEVOS a cada bloque de captura/pantalla del documento
SkillAcademy_Mobile_Native_Entregables.docx:

  1) 📐 Tipo de Acomodado en Pantalla (Scrolling / Estándar Americano / Scaffold / etc.)
  2) 🧭 Tipo de Navegación (BottomNavigationBar / Scaffold + Back / Tab Bar / Modal / etc.)

Y, además, debajo de cada imagen incrustada (pie de foto), agrega un pie de foto
que indica el TIPO DE NAVEGACIÓN empleada en esa pantalla.

La información se extrae directamente del código fuente Kotlin/Jetpack Compose
de la aplicación móvil nativa (mobile-android-skillacademy).
"""

import os
import re
import sys
import docx
from docx.shared import Pt, RGBColor, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn

# ==============================================================================
# DATOS SEMÁNTICOS DE CADA PANTALLA (obtenidos del código fuente Kotlin/Compose)
# ==============================================================================
#
# CRITERIO DE ACOMODADO (basado en patrones Material Design y reglas clase):
#   • SCROLLING: El layout principal usa LazyColumn, Column(verticalScroll),
#     o la pantalla tiene más contenido que el alto visible (requiere swipe).
#   • ESTÁNDAR AMERICANO (Standard / Fixed): Layout con Box fijo centrado
#     (pantalla de estado vacío, loading, formularios compactos con
#      verticalScroll que caben en una pantalla sin swiperear).
#   • SCAFFOLD (con barras fijas): Usa Scaffold con TopAppBar + BottomBar
#     y/o Sticky Buy Bar (barra flotante inferior fija).
#
# CRITERIO DE NAVEGACIÓN (patrones nativos Android/Material3):
#   • BOTTOM NAVIGATION BAR: Destino persistente de la barra inferior.
#   • TOP APP BAR (Back): Navegación jerárquica con flecha ← en TopAppBar.
#   • TAB ROW SWIPE / HORIZONTAL PAGER: Pestañas horizontales internas.
#   • MODAL BOTTOM SHEET: Hoja modal deslizable desde abajo (pasarela).
#   • ALERT DIALOG / DIALOG: Cuadro de diálogo emergente centrado.
#   • FULL SCREEN (Auth Guard): Pantalla de auth a pantalla completa sin
#     barras ni scaffold (formulario flotante).

PANTALLAS_DB = {
    # =========================================================
    # ENTREGABLE 1 — PANTALLAS COMERCIALES
    # =========================================================
    "HOMESCREEN": {
        "acomodado": (
            "SCROLLING (LazyColumn vertical + 2 × LazyRow horizontal "
            "de banners y chips). Material Design 3 List Pattern."
        ),
        "navegacion": (
            "Scaffold + SkillTopBar (Carrito + Práctica3) + SkillBottomBar "
            "(Inicio / Catálogo / Mis Cursos / Perfil). Navegación por pestañas "
            "persistentes. Tap en curso → CourseDetailScreen. Tap en chip/banner "
            "→ CatalogScreen con categoría pre-seleccionada."
        ),
        "img_navegacion": (
            "BottomNavigationBar (4 destinos) + TopAppBar con badge de carrito. "
            "LazyRow horizontal (banner/chips) + LazyColumn vertical (cursos)."
        ),
    },
    "CATALOGSCREEN": {
        "acomodado": (
            "SCROLLING (LazyColumn vertical sobre LazyRow horizontal de "
            "filtros + SearchBar fija en parte superior). Material 3 Standard "
            "Search & Filter Pattern."
        ),
        "navegacion": (
            "Scaffold + SkillTopBar + SkillBottomBar. La pantalla es un "
            "destino de tab de la barra inferior. Tap en curso → CourseDetailScreen "
            "(navegación jerárquica ascendente). Búsqueda reactiva en tiempo real."
        ),
        "img_navegacion": (
            "BottomNavigationBar. Filtros por Categoría (Chips horizontales) + "
            "Nivel + SearchBar. LazyColumn scrollable con resultados."
        ),
    },
    "COURSEDETAILSCREEN": {
        "acomodado": (
            "SCAFFOLD + SCROLLING + STICKY BOTTOM BAR (Bottom Buy Bar). "
            "Scaffold con TopAppBar + barra fija inferior 'Al Carrito/Comprar Ya'. "
            "Contenido interno: HorizontalPager (carrusel) + LazyColumn + "
            "3 Tabs (Descripción / Lecciones / Requisitos). "
            "Estándar Americano 'Detail View + Sticky CTA'."
        ),
        "navegacion": (
            "Scaffold con TopAppBar (flecha ← BackHandler hacia Home/Catálogo) "
            "+ Sticky Buy Bar inferior. Navegación jerárquica (Up) + 3 Tabs "
            "interiores (TabRow) + HorizontalPager para galería. CTA 'Comprar Ya' "
            "lanza Auth (login) o inscripción directa → MyCourses."
        ),
        "img_navegacion": (
            "TopAppBar ← (Navegación Jerárquica). Sticky Bottom Bar (Compra). "
            "3 Tabs internas (TabRow). HorizontalPager de galería + LazyColumn."
        ),
    },
    "CARTSCREEN": {
        "acomodado": (
            "SCAFFOLD + SCROLLING + STICKY CHECKOUT BAR. "
            "LazyColumn (ítems + cupón) + Surface fija inferior con "
            "Resumen Financiero y Botón CTA (Checkout). "
            "Estándar Americano 'Cart with Sticky Footer'."
        ),
        "navegacion": (
            "Scaffold + SkillTopBar + SkillBottomBar (destino Carrito, accesible "
            "también por tap en icono del TopAppBar). Botón Checkout → LoginScreen "
            "(si sin sesión) → luego PaymentGatewayBottomSheet (Modal). "
            "BackHandler → Home."
        ),
        "img_navegacion": (
            "Scaffold Top + Bottom NavigationBar. LazyColumn de ítems/cupones. "
            "Sticky Footer CTA de Checkout → Modal Bottom Sheet (Pasarela)."
        ),
    },

    # =========================================================
    # ENTREGABLE 2 — AUTENTICACIÓN, DATOS Y CUPONES
    # =========================================================
    "LOGINSCREEN": {
        "acomodado": (
            "ESTÁNDAR AMERICANO (Full-Screen Card Pattern). "
            "Box centrado con Card flotante + Column(verticalScroll) interior. "
            "Background radial fijo. Todo el formulario (login + registro) cabe "
            "en pantalla alta; el scroll solo se usa en teclado abierto."
        ),
        "navegacion": (
            "Auth Guard a Pantalla Completa (sin Scaffold ni barras). "
            "Tab Switcher interior: Iniciar Sesión ↔ Crear Cuenta (Segmented "
            "Control). Botón superior 'Volver a la tienda' + BackHandler físico "
            "→ regresan al origen (Home/Catálogo) con seguridad sin fuga a "
            "rutas protegidas. Éxito auth → retorna a authRedirectTarget."
        ),
        "img_navegacion": (
            "Full-Screen Auth. Sin Top/Bottom Bars. BackHandler + Volver. "
            "Segmented Control Login/Registro. Redirect al target protegido."
        ),
    },
    "REGISTERSCREEN": {
        "acomodado": (
            "ESTÁNDAR AMERICANO (comparta card de LoginScreen con más campos). "
            "Box centrado + Column(verticalScroll). Misma card flotante con "
            "campos extra Nombre + Confirmar Contraseña."
        ),
        "navegacion": (
            "Misma Auth Guard de LoginScreen. Switcher Login ↔ Registro dentro "
            "de la misma Screen. Onboarding: signUp + loadProfile + saveSession "
            "→ retorna al destino que disparó la protección (Cart / MyCourses)."
        ),
        "img_navegacion": (
            "Incluida en LoginScreen. Misma navegación auth guard + switcher."
        ),
    },
    "CONSUMO DATOS REALES": {  # Evidencia #07 (3 fotos: app + tabla Supabase + esquema)
        "acomodado": (
            "SCROLLING (HomeScreen LazyColumn) + FIXED (Dashboard Supabase Web). "
            "Evidencia compuesta: 3 figuras (Móvil + Tabla SQL + Esquema de tabla)."
        ),
        "navegacion": (
            "No es una pantalla única; es una evidencia técnica. "
            "La parte móvil sigue navegación HomeScreen (BottomNav). "
            "La parte web corresponde al Dashboard Web de Supabase navegable."
        ),
        "img_navegacion": (
            "Evidencia compuesta 3-en-1: App Home (LazyColumn+BottomNav) + "
            "Supabase Table Editor + Schema Designer. No navega entre sí."
        ),
    },
    "CUPONES": {  # Evidencia #08
        "acomodado": (
            "MISMA que CartScreen: SCAFFOLD + SCROLLING + STICKY CHECKOUT BAR."
        ),
        "navegacion": (
            "Misma de CartScreen. Además: Motor de cupones con TextField + "
            "Button 'Aplicar' → llamada async a CouponRepository (Supabase REST) "
            "con estados Idle/Validating/Applied/Error y recálculo inmediato."
        ),
        "img_navegacion": (
            "CartScreen con sección de cupones expandida: TextField + Botón "
            "Aplicar + Badge verde de cupón aplicado + Footer CTA Sticky."
        ),
    },

    # =========================================================
    # PRÁCTICA 3 — CONSULTA WEB / HTTP
    # =========================================================
    "CONSULTA WEB": {
        "acomodado": (
            "SCROLLING (Column + verticalScroll) sobre Scaffold. "
            "Patrón 'Single-Screen HTTP Tester' con URL input + Botón Consultar "
            "+ Tarjeta de resultado / loading / error."
        ),
        "navegacion": (
            "Scaffold con TopAppBar (flecha ← Back) + sin BottomBar. "
            "Destino accesible desde Home (chip tarjeta) y TopBar icono. "
            "BackHandler/Up → pantalla anterior. 3 atajos internos (Chips) "
            "para cargar URLs de prueba P1/P2/P3."
        ),
        "img_navegacion": (
            "TopAppBar ←. Pantalla scrollable simple. Chips de atajo URL. "
            "Botón CONSULTAR dispara transición de estado Loading→Success/Error."
        ),
    },
    "PRUEBA 2": {  # Sin conexión
        "acomodado": (
            "MISMA ConsultaWebScreen (SCROLLING Scaffold). "
            "HomeScreen con Banner Card de Error 'No fue posible obtener la "
            "información' (SCROLLING LazyColumn)."
        ),
        "navegacion": (
            "Navegación offline controlada. HomeScreen muestra Banner de error "
            "con botón REINTENTAR. ConsultaWeb dispara mensaje RF06. BackHandler "
            "seguro (no fuga a pantallas protegidas sin datos)."
        ),
        "img_navegacion": (
            "ConsultaWebScreen: Estado Error RF06 rojo (Sin conexión) + "
            "HomeScreen Banner Error + Reintentar. Misma nav TopAppBar ←."
        ),
    },
    "PRUEBA 3": {  # URL modificada 404
        "acomodado": (
            "MISMA ConsultaWebScreen (SCROLLING Scaffold)."
        ),
        "navegacion": (
            "Atajo de chip 'URL Modificada (P3)' + Botón Consultar → lanza HTTP "
            "GET contra URL inexistente → manejo de status code 404 y mensaje "
            "de error controlado sin excepción no manejada ni crash."
        ),
        "img_navegacion": (
            "ConsultaWebScreen: Estado Error 404 URL Modificada. Chip atajo P3."
        ),
    },

    # =========================================================
    # ENTREGABLE 3 — PASARELA, AULA Y PERFIL
    # =========================================================
    "PAYMENTGATEWAYBOTTOMSHEET": {
        "acomodado": (
            "MODAL BOTTOM SHEET + SCROLLING INTERNO (Column con AnimatedVisibility "
            "de formularios de pago). Estándar Americano 'Checkout Modal'."
        ),
        "navegacion": (
            "ModalBottomSheet de Material3 (deslizable desde abajo, drag handle). "
            "Navigation Pattern: 'Bottom Sheet Modal'. Tap fuera o deslizar hacia "
            "abajo → onDismissRequest. Selector de métodos de pago con 3 chips "
            "(Tarjeta/Google Pay/Instant Pay) que alternan contenido animado "
            "(AnimatedVisibility). Confirmar → inscripciones + Notificación + "
            "Diálogo de éxito + goto MyCourses."
        ),
        "img_navegacion": (
            "Modal Bottom Sheet (deslizable). 3 Payment Method Chips + "
            "AnimatedVisibility de campos. Drag Handle superior + dim back."
        ),
    },
    "CHECKOUT EXITO": {  # Diálogo Orden ORD-XXXXXX
        "acomodado": (
            "ESTÁNDAR AMERICANO (AlertDialog / Dialog centrado). "
            "Card 24.dp padding con fondo dim overlay. Compacto: 1 pantalla."
        ),
        "navegacion": (
            "Compose Dialog (modal emergente centrado superpuesto). Bloquea "
            "interacción con pantalla inferior. Botón CTA 'Ir a Mis Cursos y "
            "Aprender' + dismissRequest → limpia carrito y navega a "
            "MyCoursesScreen (Bottom Nav destino)."
        ),
        "img_navegacion": (
            "Dialog modal centrado con scrim. Botón CTA → MyCoursesScreen "
            "(Bottom Navigation bar destino)."
        ),
    },
    "MYCOURSESSCREEN": {
        "acomodado": (
            "SCROLLING (LazyColumn vertical) + 2 ESTADOS FIXED: Loading "
            "(Box Centered CircularProgress) y Empty State (Box + Column). "
            "Patrón 'List Detail / My Items'."
        ),
        "navegacion": (
            "Scaffold + SkillTopBar + SkillBottomBar (destino 'Mis Cursos'). "
            "Ruta PROTEGIDA (Auth Guard doble). Tap en tarjeta de curso → "
            "CourseLearnScreen (TopAppBar ← Back). Empty State CTA → "
            "CatalogScreen."
        ),
        "img_navegacion": (
            "Bottom Navigation Bar (Mis Cursos). LazyColumn tarjetas con "
            "LinearProgressIndicator. CTA Continuar Aprendiendo → "
            "CourseLearnScreen (TopAppBar ← Jerárquica)."
        ),
    },
    "COURSELEARNSCREEN": {
        "acomodado": (
            "SCAFFOLD + SCROLLING (LazyColumn internas por Tab) + "
            "STICKY PROGRESS BAR superior + TAB ROW de 2 pestañas. "
            "Estándar Americano 'Learning Shell / Classroom'."
        ),
        "navegacion": (
            "Scaffold con TopAppBar (flecha ← Back a MyCourses) + LinearProgress "
            "Indicator fijo + TabRow de 2 tabs: LECCIONES / TAREAS. Navegación "
            "interna por pestañas + selección fila en listado lateral actualiza "
            "tarjeta principal (single page master/detail). Botón 'Marcar como "
            "completada' → actualiza progreso en Supabase y recalcula %."
        ),
        "img_navegacion": (
            "TopAppBar ← Jerárquica. Tab Row (2 Tabs). LazyColumn de lecciones "
            "seleccionables (Master/Detail interno). Sticky Progress Bar."
        ),
    },
    "PROFILESCREEN": {
        "acomodado": (
            "SCROLLING (Column + verticalScroll) con 3 Cards: Header / "
            "Edición Perfil / Cuenta y Sesión. LazyRow horizontal de avatares. "
            "Estándar Americano 'Settings / Profile Page'."
        ),
        "navegacion": (
            "Scaffold + SkillTopBar + SkillBottomBar (destino 'Perfil'). "
            "Ruta PROTEGIDA. LazyRow de preset avatares (horizontales). "
            "Botón Guardar Cambios → PATCH a Supabase profiles. "
            "Botón Cerrar Sesión → AlertDialog confirmación → clearSession "
            "→ HomeScreen y limpia carrito."
        ),
        "img_navegacion": (
            "Bottom Navigation Bar (Perfil). Column verticalScroll con "
            "3 tarjetas + LazyRow avatares + AlertDialog Logout confirmación."
        ),
    },
}

# Paleta de colores del documento original (SkillAcademy theme)
COLOR_NAVY = RGBColor(0x0F, 0x29, 0x42)
COLOR_EMERALD = RGBColor(0x0D, 0x94, 0x88)
COLOR_CYAN = RGBColor(0x02, 0x84, 0xC7)
COLOR_TEXT = RGBColor(0x1E, 0x29, 0x3B)
COLOR_MUTED = RGBColor(0x64, 0x74, 0x8B)
COLOR_LABEL = RGBColor(0x0F, 0x29, 0x42)


def buscar_entrada_db(texto_tabla):
    """Devuelve la entrada de PANTALLAS_DB que coincida con alguna de las palabras clave."""
    t = texto_tabla.upper()
    keys_match_order = [
        ("PAYMENTGATEWAY", "PAYMENTGATEWAYBOTTOMSHEET"),
        ("PASARELA DE PAGO", "PAYMENTGATEWAYBOTTOMSHEET"),
        ("CONFIRMACIÓN DE COMPRA", "CHECKOUT EXITO"),
        ("CONFIRMACION DE COMPRA", "CHECKOUT EXITO"),
        ("CÓDIGO DE ORDEN", "CHECKOUT EXITO"),
        ("EXITO TRAS COMPLETAR", "CHECKOUT EXITO"),
        ("MIS CURSOS INSCRITOS", "MYCOURSESSCREEN"),
        ("MYCOURSESSCREEN", "MYCOURSESSCREEN"),
        ("AULA VIRTUAL", "COURSELEARNSCREEN"),
        ("COURSELEARNSCREEN", "COURSELEARNSCREEN"),
        ("AULA MÓVIL POST-VENTA: PANTALLA DE MIS CURSOS", "MYCOURSESSCREEN"),
        ("AULA MOVIL POST-VENTA: PANTALLA DE MIS CURSOS", "MYCOURSESSCREEN"),
        ("PROFILESCREEN", "PROFILESCREEN"),
        ("PERFIL", "PROFILESCREEN"),
        ("LOGINSCREEN", "LOGINSCREEN"),
        ("INICIO DE SESIÓN", "LOGINSCREEN"),
        ("INICIO DE SESION", "LOGINSCREEN"),
        ("REGISTERSCREEN", "REGISTERSCREEN"),
        ("REGISTRO DE NUEVOS", "REGISTERSCREEN"),
        ("HOMESCREEN", "HOMESCREEN"),
        ("PANTALLA PRINCIPAL", "HOMESCREEN"),
        ("PANTALLA PRINCIPAL COMERCIAL", "HOMESCREEN"),
        ("CATALOGSCREEN", "CATALOGSCREEN"),
        ("CATÁLOGO GENERAL", "CATALOGSCREEN"),
        ("CATALOGO GENERAL", "CATALOGSCREEN"),
        ("COURSEDETAILSCREEN", "COURSEDETAILSCREEN"),
        ("FICHA TÉCNICA DETALLADA", "COURSEDETAILSCREEN"),
        ("FICHA TECNICA DETALLADA", "COURSEDETAILSCREEN"),
        ("CARTSCREEN", "CARTSCREEN"),
        ("CARRITO DE COMPRAS", "CARTSCREEN"),
        ("MOTOR DE CUPONES", "CUPONES"),
        ("CUPON", "CUPONES"),
        ("DESDE LA BASE DE DATOS", "CONSUMO DATOS REALES"),
        ("DATOS REALES", "CONSUMO DATOS REALES"),
        ("SUPABASE", "CONSUMO DATOS REALES"),
        ("PRUEBA 2", "PRUEBA 2"),
        ("SIN CONEXIÓN", "PRUEBA 2"),
        ("SIN CONEXION", "PRUEBA 2"),
        ("PRUEBA 3", "PRUEBA 3"),
        ("URL MODIFICADA", "PRUEBA 3"),
        ("RECURSO INEXISTENTE", "PRUEBA 3"),
        ("CONSULTA WEB", "CONSULTA WEB"),
        ("CONSULTAWEB", "CONSULTA WEB"),
    ]
    for needle, key in keys_match_order:
        if needle in t:
            return key, PANTALLAS_DB[key]
    return None, None


def insertar_spec_en_tabla(cell, label, valor, color_label=COLOR_EMERALD):
    """Inserta un nuevo renglón (párrafo) de specs dentro de la celda de captura,
    justo después del bloque de specs existente (antes de la primera imagen/pie).
    """
    # Crear nuevo párrafo con el mismo estilo que los specs originales
    p_new = cell.add_paragraph()
    p_new.paragraph_format.space_before = Pt(1)
    p_new.paragraph_format.space_after = Pt(2)

    r_lbl = p_new.add_run(label)
    r_lbl.bold = True
    r_lbl.font.name = "Calibri"
    r_lbl.font.size = Pt(9.5)
    r_lbl.font.color.rgb = color_label

    r_val = p_new.add_run(" " + valor)
    r_val.font.name = "Calibri"
    r_val.font.size = Pt(9.5)
    r_val.font.color.rgb = COLOR_TEXT


def agregar_pie_navegacion(cell, texto_navegacion_img):
    """Inserta UN NUEVO PIE DE FOTO debajo de la(s) imagen(es) o de su pie actual,
    indicando el TIPO DE NAVEGACIÓN que usa la pantalla de la captura."""
    p_cap = cell.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_before = Pt(2)
    p_cap.paragraph_format.space_after = Pt(6)

    r_icon = p_cap.add_run("🧭 ")
    r_icon.font.name = "Calibri"
    r_icon.font.size = Pt(8.5)
    r_icon.font.color.rgb = COLOR_CYAN

    r_lbl = p_cap.add_run("Navegación en pantalla: ")
    r_lbl.bold = True
    r_lbl.font.name = "Calibri"
    r_lbl.font.size = Pt(8.5)
    r_lbl.font.color.rgb = COLOR_NAVY

    r_val = p_cap.add_run(texto_navegacion_img)
    r_val.italic = True
    r_val.font.name = "Calibri"
    r_val.font.size = Pt(8.5)
    r_val.font.color.rgb = COLOR_MUTED


def count_drawing_elements(cell):
    """Cuenta las imágenes (dibujos) en una celda de tabla."""
    cell_xml = cell._tc.xml
    return cell_xml.count("<w:drawing>") + cell_xml.count("<w:pict>")


def find_insert_index(cell):
    """Retorna el índice del párrafo dentro de la celda, DESPUÉS del bloque
    de specs (líneas 📱 🔍 ⚙️ 💡) y ANTES de la primera imagen/pie de foto.
    """
    # Itera los párrafos de la celda y detén cuando encuentres un párrafo con
    # imagen (drawing) o cuyo primer run sea una imagen.
    para_specs_done_idx = None
    for idx, p in enumerate(cell.paragraphs):
        has_img = any(
            (len(run._element.findall(qn("w:drawing"))) > 0 or
             len(run._element.findall(qn("w:pict"))) > 0)
            for run in p.runs
        )
        # Si este párrafo tiene una imagen: inserta ANTES de este párrafo
        if has_img:
            return idx  # insertar_specs se agrega con add_paragraph → irá al final.
        # Si es un párrafo de "Figura:" (pie), insertar antes también.
        if any(
            (run.text or "").strip().startswith(("Figura:", "Fig. ", "Fig ", "🔲"))
            for run in p.runs
        ):
            return idx

    # Si no tiene imágenes/pies, agregar al final
    return len(cell.paragraphs)


def enriquecer_capturas(doc):
    """Punto principal: recorre todas las tablas del doc y enriquece aquellas
    que son bloques de captura/pantalla."""
    procesadas = 0
    saltadas = []

    for ti, tbl in enumerate(doc.tables):
        # Solo 1x1 (capturas son siempre tabla de 1 celda)
        if len(tbl.rows) != 1 or len(tbl.columns) != 1:
            continue
        cell = tbl.rows[0].cells[0]
        texto = cell.text

        # Saltar tablas que NO son captura/pantalla
        if not any(x in texto for x in ["📷", "📸", "CAPTURA", "EVIDENCIA"]):
            continue

        key, data = buscar_entrada_db(texto)
        if data is None:
            saltadas.append((ti, texto.strip().splitlines()[0][:120]))
            continue

        num_imgs = count_drawing_elements(cell)
        if num_imgs == 0:
            # Bloque de captura SIN imagen (placeholder). Agregar specs + nota
            insertar_spec_en_tabla(
                cell,
                "📐 Tipo de Acomodado:",
                data["acomodado"],
                color_label=COLOR_EMERALD,
            )
            insertar_spec_en_tabla(
                cell,
                "🧭 Tipo de Navegación:",
                data["navegacion"],
                color_label=COLOR_CYAN,
            )
            procesadas += 1
            continue

        # ============ BLOQUE CON IMÁGENES ============
        # Encontrar el párrafo ANTES del primero con imagen/pie para insertar specs.
        insert_before_idx = find_insert_index(cell)

        # Sin embargo python-docx no permite insertar párrafo en medio fácilmente,
        # así que reubicamos: reordenamos moviendo los párrafos tras imágenes al final.
        # Para mantener simplicidad y NO romper las imágenes, usamos estrategia segura:
        #    1) Extraer todos los párrafos POSTERIORES (incl. imágenes y pies)
        #       como XML.
        #    2) Insertar los dos specs al final de la celda.
        #    3) Volver a agregar los párrafos extraídos (con imágenes y pies).
        #    4) Agregar el pie de navegación extra al final.
        # Para no romper dibujos, en su lugar usamos una heurística más simple:
        #   → Identificamos el punto exacto, y copiamos el XML.
        #
        # En la práctica observamos que en add_screenshot_section del generador
        # los specs vienen PRIMERO y las imágenes DESPUÉS. Podemos confiar en
        # que insert_before_idx es el primer párrafo de imagen.
        #
        # Estrategia final: mover todos los párrafos desde insert_before_idx
        # a una lista, limpiarlos de la celda, agregar los 2 specs, y
        # volver a insertarlos.

        # Sacar todos los elementos p (párrafos) del XML de la celda en orden
        tc = cell._tc
        tcPr = tc.find(qn("w:tcPr"))
        # Recolectar todos los nodos <w:p> en orden
        p_nodes = list(tc.findall(qn("w:p")))

        before = p_nodes[:insert_before_idx]
        move_away = p_nodes[insert_before_idx:]

        # Remover todos los <w:p> de la celda
        for p in p_nodes:
            tc.remove(p)

        # Volver a agregar los párrafos de specs originales (before)
        for p in before:
            tc.append(p)

        # Crear los 2 PÁRRAFOS NUEVOS (specs) en python-docx y traer sus <w:p>
        # Para lograrlo usamos un párrafo temporal:
        specs_xml = []

        def make_spec_p_xml(label, value, color_label):
            """Construye el <w:p> de un spec usando python-docx sobre un documento dummy."""
            tmp_doc = docx.Document()
            p = tmp_doc.add_paragraph()
            p.paragraph_format.space_before = Pt(1)
            p.paragraph_format.space_after = Pt(2)
            r_lbl = p.add_run(label)
            r_lbl.bold = True
            r_lbl.font.name = "Calibri"
            r_lbl.font.size = Pt(9.5)
            r_lbl.font.color.rgb = color_label
            r_val = p.add_run(" " + value)
            r_val.font.name = "Calibri"
            r_val.font.size = Pt(9.5)
            r_val.font.color.rgb = COLOR_TEXT
            return p._p

        specs_xml.append(make_spec_p_xml(
            "📐 Tipo de Acomodado:", data["acomodado"], COLOR_EMERALD
        ))
        specs_xml.append(make_spec_p_xml(
            "🧭 Tipo de Navegación:", data["navegacion"], COLOR_CYAN
        ))

        for p_xml in specs_xml:
            tc.append(p_xml)

        # Volver a agregar los párrafos con imágenes/pies
        for p in move_away:
            tc.append(p)

        # Finalmente, agregar el PIE DE FOTO EXTRA (tipo de navegación en imagen)
        # Lo agregamos como un nuevo párrafo al final de la celda.
        # Para construirlo con python-docx y tomar su XML:
        pie_xml = make_spec_p_img(data["img_navegacion"])
        tc.append(pie_xml)

        procesadas += 1

    return procesadas, saltadas


def make_spec_p_img(texto):
    """Construye el <w:p> del pie de foto extra de navegación."""
    tmp_doc = docx.Document()
    p = tmp_doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(6)

    r1 = p.add_run("🧭 ")
    r1.font.name = "Calibri"
    r1.font.size = Pt(8.5)
    r1.font.color.rgb = COLOR_CYAN

    r2 = p.add_run("Navegación en pantalla: ")
    r2.bold = True
    r2.font.name = "Calibri"
    r2.font.size = Pt(8.5)
    r2.font.color.rgb = COLOR_NAVY

    r3 = p.add_run(texto)
    r3.italic = True
    r3.font.name = "Calibri"
    r3.font.size = Pt(8.5)
    r3.font.color.rgb = COLOR_MUTED
    return p._p


def safe_save(doc, target_path):
    """Guarda con fallback _Actualizado si el archivo está bloqueado por Word."""
    try:
        doc.save(target_path)
        return target_path, None
    except PermissionError:
        alt = target_path.replace(".docx", "_Actualizado.docx")
        doc.save(alt)
        return alt, target_path


def main():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

    targets = [
        # Entregable principal (destino del alumno)
        os.path.join(base_dir, "entregables", "SkillAcademy_Mobile_Native_Entregables.docx"),
        # Backup (por si el principal está abierto)
        os.path.join(base_dir, "entregables", "SkillAcademy_Mobile_Native_Entregables_ACTUALIZADO.docx"),
        # Copia en documentacion
        os.path.join(base_dir, "documentacion", "SkillAcademy_Mobile_Native_Entregables.docx"),
    ]

    backup_path = os.path.join(base_dir, "entregables", "SkillAcademy_Mobile_Native_Entregables_BACKUP.docx")
    ok = False
    for src in targets:
        if os.path.exists(src):
            WORKER_SRC = src
            ok = True
            break
    if not ok and os.path.exists(backup_path):
        WORKER_SRC = backup_path
        ok = True
    if not ok:
        print("❌ NO SE ENCONTRÓ NINGÚN DOCX FUENTE.")
        sys.exit(1)

    print(f"📂 Cargando documento fuente: {WORKER_SRC}")
    doc = docx.Document(WORKER_SRC)

    procesadas, saltadas = enriquecer_capturas(doc)

    print(f"✅ Bloques de captura procesados: {procesadas}")
    if saltadas:
        print(f"ℹ️  Tablas sin match (se mantienen iguales):")
        for ti, head in saltadas:
            print(f"    · Tabla #{ti}: {head!r}")

    # Guardar en todos los destinos
    all_out = [
        os.path.join(base_dir, "entregables", "SkillAcademy_Mobile_Native_Entregables.docx"),
        os.path.join(base_dir, "entregables", "SkillAcademy_Mobile_Native_Entregables_ACTUALIZADO.docx"),
        os.path.join(base_dir, "documentacion", "SkillAcademy_Mobile_Native_Entregables.docx"),
    ]

    for out in all_out:
        final, locked = safe_save(doc, out)
        if locked:
            print(f"⚠️  '{locked}' estaba abierto → guardado como: {final}")
        else:
            print(f"💾 Documento guardado: {final}")

    print("\n🎉 PROCESO TERMINADO.")
    print("   Campos agregados:")
    print("     · 📐 Tipo de Acomodado (Scrolling / Estándar Americano / Scaffold + Sticky / etc.)")
    print("     · 🧭 Tipo de Navegación (BottomNav / Back Jerárquico / TabRow / Modal / etc.)")
    print("     · 🧭 Pie de foto extra debajo de cada imagen incrustada indicando navegación.")


if __name__ == "__main__":
    main()
