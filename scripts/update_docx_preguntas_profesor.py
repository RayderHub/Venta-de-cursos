# -*- coding: utf-8 -*-
"""
update_docx_preguntas_profesor.py
=================================
Agrega a la Sección 3 (Preguntas de Fundamentación Técnica de Arquitectura Móvil)
del documento maestro SkillAcademy_Mobile_Native_Entregables.docx:

  1) Pregunta 6: Nombre de la interfaz a utilizar en el proyecto y tipo de navegación
     - Clasificación de Interfaz: Scrolling (LazyColumn/LazyRow), Estándar Americano (Fixed/Compact Box) y Scaffold base.
     - Clasificación de Navegación: Estándar Americano (Bottom Navigation Bar ergonómica), Navegación Jerárquica (Stack/Backhandler) y Modal (Bottom Sheets).
     - Análisis técnico de por qué el inicio de sesión y registro aislados/obligatorios son considerados "pointless" en UX moderna y cómo se resuelve con Auth Unificado contextual (Guest Browsing + Tabbed Auth en Perfil).
  2) Pregunta 7: Justificación Arquitectónica de Exclusión de la Práctica 6 (Interacción P2P entre Dispositivos Móviles - MobileConnect)
     - Modelo de negocio E-learning B2C vs Mensajería P2P.
     - Arquitectura centralizada en la nube (PostgreSQL/Supabase + RLS) vs P2P ad-hoc.
     - Seguridad y Privacidad bajo ISO/IEC 27001.
     - Principio YAGNI y optimización de batería/permisos de hardware.
     - Canal formal de interacción multi-dispositivo adoptado (sincronización en la nube y foros asíncronos).
"""

import os
import shutil
import docx
from docx.shared import Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH

DOC_PATH = r"c:\Users\Rayder\Desktop\SkillAcademy_Practicas_5_6_Endpoint\documentacion\SkillAcademy_Mobile_Native_Entregables.docx"
ENTREGABLES_PATH = r"c:\Users\Rayder\Desktop\SkillAcademy_Practicas_5_6_Endpoint\entregables\SkillAcademy_Mobile_Native_Entregables.docx"

COLOR_PRIMARY = RGBColor(0x0F, 0x29, 0x42)   # Navy corporativo
COLOR_SECONDARY = RGBColor(0x02, 0x84, 0xC7) # Azul secundario
COLOR_TEXT = RGBColor(0x33, 0x41, 0x55)      # Slate texto
COLOR_MUTED = RGBColor(0x64, 0x74, 0x8B)     # Gris secundario

def add_p_before(target_p, style='Normal', space_before=4, space_after=4):
    p = target_p.insert_paragraph_before()
    p.style = style
    p.paragraph_format.space_before = Pt(space_before)
    p.paragraph_format.space_after = Pt(space_after)
    return p

def add_bullet_before(target_p, bold_prefix, text_content):
    p = add_p_before(target_p, style='List Bullet', space_before=2, space_after=3)
    
    r_bullet = p.add_run("• ")
    r_bullet.bold = True
    r_bullet.font.name = "Calibri"
    r_bullet.font.size = Pt(9.5)
    r_bullet.font.color.rgb = COLOR_PRIMARY
    
    if bold_prefix:
        r_pre = p.add_run(bold_prefix + ": ")
        r_pre.bold = True
        r_pre.font.name = "Calibri"
        r_pre.font.size = Pt(9.5)
        r_pre.font.color.rgb = COLOR_PRIMARY
        
    r_txt = p.add_run(text_content)
    r_txt.font.name = "Calibri"
    r_txt.font.size = Pt(9.5)
    r_txt.font.color.rgb = COLOR_TEXT
    return p

def add_question_title_before(target_p, num_str, title_str):
    p = add_p_before(target_p, space_before=12, space_after=4)
    r_num = p.add_run(num_str + " ")
    r_num.bold = True
    r_num.font.name = "Calibri"
    r_num.font.size = Pt(10)
    r_num.font.color.rgb = COLOR_PRIMARY
    
    r_t = p.add_run(title_str)
    r_t.bold = True
    r_t.font.name = "Calibri"
    r_t.font.size = Pt(10)
    r_t.font.color.rgb = COLOR_PRIMARY
    return p

def add_normal_before(target_p, text, bold=False, italic=False):
    p = add_p_before(target_p, space_before=3, space_after=4)
    r = p.add_run(text)
    r.bold = bold
    r.italic = italic
    r.font.name = "Calibri"
    r.font.size = Pt(9.5)
    r.font.color.rgb = COLOR_TEXT
    return p

def main():
    if not os.path.exists(DOC_PATH):
        print(f"Error: No existe el archivo {DOC_PATH}")
        return

    doc = docx.Document(DOC_PATH)
    
    # Buscar el párrafo de corte: "ENTREGABLE 1: DISEÑO UI/UX"
    target_idx = None
    for i, p in enumerate(doc.paragraphs):
        if "ENTREGABLE 1:" in p.text.upper():
            target_idx = i
            break
            
    if target_idx is None:
        print("Error: No se encontró el encabezado de ENTREGABLE 1")
        return

    target_p = doc.paragraphs[target_idx]
    print(f"Insertando preguntas antes del párrafo [{target_idx}]: {target_p.text[:60]}")

    # =========================================================================
    # PREGUNTA 6
    # =========================================================================
    add_question_title_before(
        target_p,
        "6.",
        "¿Cuál es el nombre de la interfaz que vas a utilizar en tu proyecto y tipo de navegación?"
    )

    add_normal_before(
        target_p,
        "En el desarrollo de SkillAcademy Mobile se determinaron formalmente las especificaciones de interfaz "
        "y patrones de navegación, alineadas a las directrices de diseño oficiales de Android (Material Design 3) "
        "y a los estándares ergonómicos de interacción móvil:"
    )

    # Sub-punto 1: Tipo de Interfaz / Acomodado
    add_bullet_before(
        target_p,
        "Nombre y Tipo de Interfaz (Acomodado en Pantalla)",
        "SkillAcademy implementa una Interfaz Híbrida basada en Scaffold que combina los patrones de Scrolling (Desplazamiento Dinámico) "
        "y Estándar Americano (Fixed/Compact Box). "
        "a) Patrón Scrolling (Vertical Feed): Gobierna las pantallas comerciales principales (HomeScreen, CatalogScreen, CourseDetailScreen y CartScreen). "
        "Utiliza listas reactivas reciclables en Jetpack Compose (LazyColumn para listados verticales de cursos y LazyRow para carruseles de banners publicitarios y filtros de categorías). "
        "Permite desplegar catálogos extensos con renderizado bajo demanda sin saturar la memoria RAM del teléfono. "
        "b) Patrón Estándar Americano (Standard / Fixed Layout): Se aplica en vistas de contenido concentrado, estados vacíos del carrito (Empty State), formularios compactos y pantallas de carga/confirmación. "
        "La información se organiza en un contenedor centrado (Box / Column) que aprovecha el alto visible del dispositivo sin exigir scroll forzado innecesario. "
        "c) Estructura Base Scaffold: Todas las vistas se estructuran mediante un Scaffold nativo que hospeda de forma fija y no invasiva la barra superior (TopAppBar) y la barra inferior de navegación."
    )

    # Sub-punto 2: Tipo de Navegación
    add_bullet_before(
        target_p,
        "Tipo de Navegación Primaria y Secundaria",
        "La arquitectura de navegación combina tres esquemas complementarios: "
        "1. Navegación Estándar Americano con Barra Inferior (Bottom Navigation Bar): Es el esquema de navegación global primario. "
        "Fija una barra en la zona inferior de la pantalla accesible ergonómicamente con el pulgar (Thumb Zone), con 4 destinos cardinales de igual jerarquía: "
        "Inicio (Home), Catálogo, Mis Cursos y Perfil. Facilita la alternancia instantánea entre secciones sin destruir el estado de cada pantalla. "
        "2. Navegación Jerárquica de Pila (Stack Navigation con BackHandler): Permite el avance y retroceso entre pantallas de maestro-detalle "
        "(Catálogo → Ficha Detallada del Curso → Aula Móvil) mediante la flecha superior ← en la TopAppBar y el botón nativo Atrás de Android. "
        "3. Navegación Modal Transaccional (Modal Bottom Sheets): Utilizada para la pasarela de pagos (PaymentGatewayBottomSheet) y aplicación de cupones, "
        "permitiendo que el usuario complete la transacción en una hoja emergente deslizable sin abandonar el contexto de compra."
    )

    # Sub-punto 3: Análisis técnico de login/registro "pointless"
    add_bullet_before(
        target_p,
        "Tratamiento de Inicio de Sesión y Registro (Análisis de Flujo 'Pointless' y Solución Unificada)",
        "En la arquitectura de UX contemporánea, obligar al usuario a interactuar con pantallas aisladas de Login o Registro como condición "
        "previa para usar la aplicación es una práctica ineficiente ('Pointless Auth Wall') que eleva el abandono en más del 60%. "
        "SkillAcademy resuelve esto mediante: "
        "a) Navegación Abierta (Guest Browsing): El usuario puede descubrir libremente cursos, explorar el catálogo, revisar precios y cargar el carrito local SQLite sin necesidad de crear cuenta ni autenticarse previamente. "
        "b) Interfaz de Autenticación Unificada (Single-Screen Tabbed Auth): Se elimina la separación tosca de dos pantallas independientes ('Pantalla Login' vs 'Pantalla Registro'). "
        "En su lugar, dentro de la pestaña 'Perfil' (y de forma modal al momento de confirmar una compra), se despliega una interfaz unificada con dos pestañas fluidas ('Iniciar Sesión' y 'Registrarse'). "
        "Ambas comparten el mismo formulario base optimizado, evitando saltos de pantalla redundantes, navegación circular y pantallas vacías. "
        "c) Autenticación Just-in-Time (JIT): La sesión se exige únicamente cuando es estrictamente indispensable (al formalizar el pago o acceder al aula 'Mis Cursos' sincronizada en Supabase)."
    )

    # =========================================================================
    # PREGUNTA 7: JUSTIFICACIÓN DE EXCLUSIÓN PRÁCTICA 6
    # =========================================================================
    add_question_title_before(
        target_p,
        "7.",
        "Justificación Arquitectónica: Exclusión de la Práctica 6 (Interacción P2P entre Dispositivos Móviles - MobileConnect)"
    )

    add_normal_before(
        target_p,
        "La Práctica 6 académica propone la construcción de un canal de intercambio de mensajes cortos directamente entre dos dispositivos móviles "
        "(Dispositivo A y Dispositivo B) para simular conectividad inalámbrica. "
        "En el diseño empresarial de SkillAcademy Mobile, se determinó formalmente la NO implementación de dicho módulo de chat P2P local, "
        "sustentada en los siguientes criterios técnicos y de gobernanza:"
    )

    add_bullet_before(
        target_p,
        "1. Incompatibilidad con el Modelo de Negocio (E-Learning B2C)",
        "SkillAcademy es una plataforma comercial de formación tecnológica y venta de cursos digitales. No es una red social ni un servicio de mensajería instantánea abierta. "
        "Implementar un chat genérico punto a punto (P2P) entre dispositivos de estudiantes aleatorios carece de valor funcional para el negocio y desvía la atención del objetivo pedagógico de la aplicación."
    )

    add_bullet_before(
        target_p,
        "2. Discordancia Arquitectónica (Centralización Cloud vs. Red Descentralizada P2P)",
        "La arquitectura del sistema descansa sobre un backend centralizado en PostgreSQL / Supabase bajo estrictas políticas de Row Level Security (RLS) y autenticación Bearer JWT. "
        "Un canal P2P ad-hoc (mediante Sockets locales, Wi-Fi Direct o Bluetooth Classic) no permite auditoría institucional, no garantiza persistencia histórica en la nube "
        "y expone direcciones IP y huellas de hardware de los dispositivos de los estudiantes, contraviniendo directamente los controles de seguridad ISO/IEC 27001 implementados en la app (Entregables 4, 5 y 6)."
    )

    add_bullet_before(
        target_p,
        "3. Principio YAGNI (You Aren't Gonna Need It) y Eficiencia Energética",
        "En desarrollo móvil nativo, agregar servicios en segundo plano para escaneo y emparejamiento inalámbrico (Bluetooth LE / Wi-Fi Aware) genera un drenado constante de batería "
        "y exige permisos intrusivos en tiempo de ejecución (BLUETOOTH_SCAN, NEARBY_WIFI_DEVICES, ACCESS_FINE_LOCATION). "
        "Incorporar esta sobrecarga técnica para una función que no forma parte del alcance operativo de la academia representa una mala práctica de ingeniería de software."
    )

    add_bullet_before(
        target_p,
        "4. Canal de Interacción Formal Adoptado por SkillAcademy",
        "La interacción entre dispositivos en SkillAcademy se canaliza de forma profesional y escalable a través de dos mecanismos legítimos: "
        "a) Dudas y Foros Asíncronos en el Aula Virtual: El estudiante envía preguntas estructuradas directamente asociadas a cada lección, dirigidas al instructor y persistidas en base de datos. "
        "b) Sincronización Multi-Dispositivo en Tiempo Real: La interacción real ocurre en la nube; un alumno puede adquirir un curso en su teléfono inteligente y continuar la reproducción de inmediato en su tablet o laptop gracias a la sincronización centralizada en Supabase."
    )

    # Espacio separador antes de Entregable 1
    add_normal_before(target_p, "")

    # Guardar documento maestro
    doc.save(DOC_PATH)
    print(f"✅ Documento maestro actualizado con éxito en: {DOC_PATH}")

    # Copiar a entregables/
    try:
        shutil.copy2(DOC_PATH, ENTREGABLES_PATH)
        print(f"✅ Copia sincronizada en entregables: {ENTREGABLES_PATH}")
    except Exception as e:
        print(f"Aviso al copiar a entregables: {e}")

if __name__ == "__main__":
    main()
