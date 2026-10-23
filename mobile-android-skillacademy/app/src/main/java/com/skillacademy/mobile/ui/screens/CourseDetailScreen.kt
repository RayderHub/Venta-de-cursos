@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.skillacademy.mobile.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.data.model.LessonUiModel
import com.skillacademy.mobile.data.model.ReviewUiModel
import com.skillacademy.mobile.ui.components.CourseImage
import com.skillacademy.mobile.ui.theme.*

/**
 * CourseDetailScreen — Entregable 2
 *
 *  ✔ Carrusel de imágenes reales de Supabase (galería de fotos del curso)
 *  ✔ Datos completos del instructor desde profiles
 *  ✔ 3 Tabs: Descripción | Lecciones | Requisitos
 *  ✔ Reseñas reales con nombre del reviewer (curso_reviews_public)
 *  ✔ Cero datos hardcodeados; todo viene del ViewModel
 */
@Composable
fun CourseDetailScreen(
    courseId: Long,
    courseProvider: (Long) -> CourseUiModel?,
    isLoadingDetail: Boolean = false,
    selectedCourse: CourseUiModel? = null,
    onBackClick: () -> Unit,
    onAddToCart: (CourseUiModel) -> Unit,
    onBuyNow: (CourseUiModel) -> Unit
) {
    // Prioridad: selectedCourse (dato completo del ViewModel) → courseProvider (catálogo rápido)
    val curso = selectedCourse ?: courseProvider(courseId)

    if (curso == null) {
        Box(Modifier.fillMaxSize().background(SkillBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SkillEmerald)
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("DESCRIPCIÓN", "LECCIONES", "REQUISITOS")

    // Imágenes del carrusel: galería de Supabase si hay, sino una imagen placeholder temática
    val imagenes = remember(curso) {
        curso.todasImagenes.takeIf { it.isNotEmpty() }
            ?: listOf("https://images.unsplash.com/photo-1516321497487-e288fb19713f?w=800")
    }
    val pagerState = rememberPagerState(pageCount = { imagenes.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(curso.titulo, maxLines = 1, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SkillNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Barra de acción fija inferior
            Surface(shadowElevation = 8.dp, color = SkillSurface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAddToCart(curso) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, SkillEmerald),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SkillEmerald)
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Al Carrito", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onBuyNow(curso) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkillEmerald)
                    ) {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Comprar Ya", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SkillBackground)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ────────────────────────────────────────────────────────
            // 1. CARRUSEL DE IMÁGENES
            // ────────────────────────────────────────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        CourseImage(
                            imageSource = imagenes[page],
                            contentDescription = "Imagen ${page + 1} de ${curso.titulo}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Gradiente inferior del carrusel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, SkillBackground)
                                )
                            )
                    )

                    // Indicadores de página
                    if (imagenes.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(imagenes.size) { index ->
                                val selected = pagerState.currentPage == index
                                val color by animateColorAsState(
                                    targetValue = if (selected) SkillEmerald else Color.White.copy(alpha = 0.5f),
                                    animationSpec = tween(200), label = "dot"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(if (selected) 10.dp else 7.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }
                    }

                    // Badge de descuento
                    curso.descuentoPorcentaje?.let { pct ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SkillDiscountRed)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("-$pct%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }

                    // Cargando detalles
                    if (isLoadingDetail) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SkillEmerald, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            // ────────────────────────────────────────────────────────
            // 2. ENCABEZADO: Título, Nivel, Categoría, Precio
            // ────────────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    // Chips de nivel y categoría
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CourseChip(text = curso.nivel, color = SkillEmerald)
                        CourseChip(text = curso.categoria, color = SkillCyan)
                        if (curso.isBestSeller) CourseChip(text = "Más vendido", color = Color(0xFFB45309))
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = curso.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary,
                        lineHeight = 28.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    // Rating + Reviews
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { i ->
                            Icon(
                                imageVector = if (i < curso.rating.toInt()) Icons.Default.Star
                                              else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${curso.rating} (${curso.reviews} reseñas)",
                            color = SkillTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Precio
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$${curso.precio} USD",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = SkillEmerald
                        )
                        curso.oldPrecio?.let {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "$$it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkillTextMuted,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        }
                    }
                }
            }

            // ────────────────────────────────────────────────────────
            // 3. INSTRUCTOR
            // ────────────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = SkillSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar del instructor
                        val avatarUrl = curso.instructorPerfil?.avatarUrl
                        if (avatarUrl != null && avatarUrl.startsWith("http")) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar instructor",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(SkillNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = curso.instructor.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Instructor",
                                fontSize = 11.sp,
                                color = SkillTextMuted
                            )
                            Text(
                                text = curso.instructor,
                                fontWeight = FontWeight.Bold,
                                color = SkillTextPrimary
                            )
                            curso.instructorPerfil?.email?.let { email ->
                                if (email.isNotBlank()) {
                                    Text(text = email, fontSize = 11.sp, color = SkillCyan)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ────────────────────────────────────────────────────────
            // 4. TABS: Descripción | Lecciones | Requisitos
            // ────────────────────────────────────────────────────────
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SkillSurface,
                    contentColor = SkillEmerald,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }
            }

            // ────────────────────────────────────────────────────────
            // 5. CONTENIDO DE LA TAB SELECCIONADA
            // ────────────────────────────────────────────────────────
            when (selectedTab) {
                // ── TAB 0: DESCRIPCIÓN ──────────────────────────────
                0 -> {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            TabSectionTitle("Descripción del Curso")
                            Spacer(Modifier.height(8.dp))
                            if (curso.descripcion.isNotBlank()) {
                                Text(
                                    text = curso.descripcion,
                                    color = SkillTextSecondary,
                                    lineHeight = 22.sp,
                                    fontSize = 14.sp
                                )
                            } else {
                                Text("Sin descripción disponible.", color = SkillTextMuted)
                            }

                            // Lo que aprenderás
                            if (curso.queAprenderas.isNotBlank()) {
                                Spacer(Modifier.height(20.dp))
                                TabSectionTitle("¿Qué aprenderás?")
                                Spacer(Modifier.height(8.dp))
                                curso.queAprenderas
                                    .split(".")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .forEach { punto ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                null,
                                                tint = SkillEmerald,
                                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = punto,
                                                color = SkillTextSecondary,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                            }
                        }
                    }

                    // Reseñas de alumnos
                    if (curso.resenas.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Column(modifier = Modifier.padding(16.dp)) {
                                TabSectionTitle("Reseñas de Alumnos (${curso.resenas.size})")
                            }
                        }
                        items(curso.resenas.take(5)) { review ->
                            ReviewCard(review = review)
                        }
                    } else if (isLoadingDetail) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                                Text("Cargando reseñas...", color = SkillTextMuted, fontSize = 12.sp)
                            }
                        }
                    } else {
                        item {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                HorizontalDivider()
                                Spacer(Modifier.height(16.dp))
                                Text("Aún no hay reseñas para este curso.", color = SkillTextMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // ── TAB 1: LECCIONES ────────────────────────────────
                1 -> {
                    if (curso.lecciones.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isLoadingDetail) {
                                    CircularProgressIndicator(color = SkillEmerald, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("Cargando lecciones...", color = SkillTextMuted)
                                } else {
                                    Icon(
                                        Icons.Default.VideoLibrary,
                                        null,
                                        tint = SkillTextMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "El instructor aún no ha publicado lecciones.",
                                        color = SkillTextMuted,
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                TabSectionTitle("Contenido del Curso")
                                Text(
                                    text = "${curso.lecciones.size} lecciones",
                                    color = SkillTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        items(curso.lecciones) { leccion ->
                            LeccionItem(leccion = leccion)
                        }
                    }
                }

                // ── TAB 2: REQUISITOS ───────────────────────────────
                2 -> {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            TabSectionTitle("Requisitos del Curso")
                            Spacer(Modifier.height(12.dp))

                            if (curso.requisitos.isNotBlank()) {
                                curso.requisitos
                                    .split(".")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .forEach { req ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                Icons.Default.FiberManualRecord,
                                                null,
                                                tint = SkillEmerald,
                                                modifier = Modifier.size(10.dp).padding(top = 4.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = req,
                                                color = SkillTextSecondary,
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            null,
                                            tint = SkillEmerald,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "No se requieren conocimientos previos para este curso. ¡Es apto para todos los niveles!",
                                            color = Color(0xFF065F46),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Info del curso
                            Spacer(Modifier.height(24.dp))
                            TabSectionTitle("Información del Curso")
                            Spacer(Modifier.height(12.dp))

                            InfoRow(icon = Icons.Default.BarChart, label = "Nivel", value = curso.nivel)
                            InfoRow(icon = Icons.Default.Category, label = "Categoría", value = curso.categoria)
                            if (curso.lecciones.isNotEmpty()) {
                                InfoRow(
                                    icon = Icons.Default.PlayLesson,
                                    label = "Lecciones",
                                    value = "${curso.lecciones.size} lecciones"
                                )
                            }
                            InfoRow(icon = Icons.Default.Star, label = "Calificación", value = "${curso.rating} / 5.0")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes internos
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CourseChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun TabSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = SkillTextPrimary
    )
}

@Composable
private fun LeccionItem(leccion: LessonUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SkillNavy.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${leccion.orden}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SkillNavy
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = leccion.titulo,
                fontWeight = FontWeight.SemiBold,
                color = SkillTextPrimary,
                fontSize = 14.sp
            )
            if (leccion.descripcion.isNotBlank()) {
                Text(
                    text = leccion.descripcion,
                    fontSize = 12.sp,
                    color = SkillTextMuted,
                    maxLines = 2
                )
            }
            if (leccion.duracionMinutos > 0) {
                Text(
                    text = "${leccion.duracionMinutos} min",
                    fontSize = 11.sp,
                    color = SkillCyan
                )
            }
        }
        if (leccion.esGratis) {
            Text(
                text = "GRATIS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SkillEmerald,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFCCFBF1))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = SkillBorder.copy(alpha = 0.5f)
    )
}

@Composable
private fun ReviewCard(review: ReviewUiModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SkillSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                val avatarUrl = review.reviewerAvatar
                if (avatarUrl != null && avatarUrl.startsWith("http")) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SkillCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = review.reviewerName.take(2).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = SkillCyan,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.reviewerName,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary,
                        fontSize = 13.sp
                    )
                    // Estrellas
                    Row {
                        repeat(5) { i ->
                            Icon(
                                imageVector = if (i < review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
                // Fecha
                val fecha = review.createdAt.take(10)
                Text(text = fecha, fontSize = 10.sp, color = SkillTextMuted)
            }
            review.comment?.let { comentario ->
                if (comentario.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "\"$comentario\"",
                        color = SkillTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SkillCyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = SkillTextSecondary, fontSize = 13.sp, modifier = Modifier.width(100.dp))
        Text(value, color = SkillTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
    HorizontalDivider(color = SkillBorder.copy(alpha = 0.4f))
}
