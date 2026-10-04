# SkillAcademy Mobile (Android Nativo - Kotlin & Jetpack Compose)

Aplicación móvil nativa del ecosistema **SkillAcademy**, enfocada al 100% en el canal de ventas, catálogo interactivo, carrito de compras con cupones, checkout y aula móvil para estudiantes.

---

## 📱 Entregable 1: Diseño UI/UX, Design System y Pantallas de Venta

Este entregable implementa la maquetación visual completa con Material 3 y Jetpack Compose, replicando fielmente la identidad gráfica de la plataforma web:

### 1. Sistema de Diseño Corporativo (`ui/theme/`)
- **`Color.kt`**: Navy oscuro (`#0F2942`), Esmeralda para compras/badges (`#0D9488`), Cyan (`#0284C7`), Slate suave (`#F8FAFC`) y Estrellas doradas (`#F59E0B`).
- **`Theme.kt`**: `SkillAcademyTheme` con soporte de modo claro y oscuro con Material 3.
- **`Type.kt`**: Jerarquía tipográfica ajustada para legibilidad en smartphones.

### 2. Componentes UI Atómicos (`ui/components/`)
- **`SkillTopBar`**: Logotipo de SkillAcademy, avatar y badge numérico animado del carrito en tiempo real.
- **`SkillBottomBar`**: Barra de navegación fija con 4 pestañas: *Inicio*, *Catálogo*, *Mis Cursos* y *Perfil*.
- **`SkillCourseCard`**: Tarjeta comercial idéntica a la web con portada temático-gradiente, badge de descuento porcentual (`-35%`), rating con estrellas, precio tachado y botón rápido para añadir al carrito.
- **`PromoBannerCard`**: Banners de promociones con degradados y botón "Ver Oferta".
- **`CategoryFilterChip`**: Chips interactivos de filtro rápido (Excel, Programación, Diseño, Productividad).
- **`StickyBuyBar`**: Barra inferior fija en la pantalla de detalle que mantiene a la vista el precio final y los botones "Comprar Ahora" y "Añadir al Carrito".

### 3. Pantallas Implementadas (`ui/screens/`)
- **`HomeScreen`**: Carrusel de banners publicitarios, categorías temáticas, lista de cursos más vendidos y modal de cupón de bienvenida (`SKILL20`).
- **`CatalogScreen`**: Buscador interactivo en vivo, chips de categorías, selector de nivel y grid de cursos.
- **`CourseDetailScreen`**: Cabecera con video teaser 16:9 y botón Play, métricas (alumnos, horas, rating), acordeón interactivo de lecciones del temario y barra inferior fija de compra.
- **`CartScreen`**: Carrito de compras con eliminación de ítems, campo para aplicar cupón dinámico (valida `SKILL20` con 20% OFF en vivo), desglose de Subtotal/Descuento/Total y botón de checkout.
- **`MyCoursesScreen`**: Pestaña de estudiante con cursos adquiridos, barra de avance porcentual y botón "Continuar Lección".
- **`ProfileScreen`**: Datos del estudiante, rol y accesos a historial de compras.
- **`MainAppScreen`**: Orquestador principal que conecta todas las pantallas con navegación fluida y estado del carrito compartido.

---

## 🌐 Práctica 3: Consumo de una URL preasignada (Extensión ConsultaWeb)

Esta extensión implementa los requerimientos funcionales para el consumo de recursos HTTP en red y procesamiento de datos JSON:

### 1. Requerimientos Funcionales Implementados
- **RF01**: Título de la pantalla: *"Consulta de información"*.
- **RF02**: Botón interactivo: *"CONSULTAR"*.
- **RF03**: Petición HTTP (`HttpURLConnection` en hilo `Dispatchers.IO`) a la URL preasignada.
- **RF04**: Visualización y formato estructurado del JSON devuelto.
- **RF05**: Estado de carga dinámico con spinner y texto: *"Consultando..."*.
- **RF06**: Manejo de excepciones y estado de fallo mostrando: *"No fue posible obtener la información."*.

### 2. Acceso dentro de la App
- **Desde la barra superior (TopBar)**: Ícono de nube/HTTP situado a la izquierda del carrito.
- **Desde la pantalla de Inicio (Home)**: Tarjeta destacada *"Práctica 3: ConsultaWeb"*.
- **Desde la pestaña Perfil**: Opción de menú *"ConsultaWeb (Práctica 3 URL Preasignada)"*.

### 3. Protocolo de Pruebas para Evaluación
1. **Prueba 1 — Con conexión**: Mantén la conexión a internet activa y pulsa **CONSULTAR**. Observarás el estado *"Consultando..."* seguido del JSON devuelto con código HTTP 200 OK.
2. **Prueba 2 — Sin conexión**: Activa el **Modo Avión** en tu dispositivo/emulador y pulsa **CONSULTAR**. Se captura la excepción de red y se presenta el mensaje de error de RF06: *"No fue posible obtener la información."*.
3. **Prueba 3 — Modificación de la URL**: Edita la URL en el campo de texto (o presiona el chip *"URL Inválida"*). Al presionar **CONSULTAR**, se evidencia que la lógica no está fija y maneja códigos de error como 404 o servidores inexistentes.

---

## 🚀 Cómo Abrir y Ejecutar el Proyecto

1. Abre **Android Studio**.
2. Selecciona **Open** y navega a esta carpeta:
   ```text
   SkillAcademy_Practicas_5_6_Endpoint/mobile-android-skillacademy
   ```
3. Espera a que Gradle sincronice las dependencias de Compose y Material 3.
4. Selecciona un dispositivo o emulador Android (API 26+) y presiona **Run 'app'** (`Shift + F10`).
