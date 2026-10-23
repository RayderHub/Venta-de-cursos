package com.skillacademy.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.skillacademy.mobile.data.model.BannerUiModel
import com.skillacademy.mobile.data.model.CategoryUiModel
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.ui.components.CategoryFilterChip
import com.skillacademy.mobile.ui.components.PromoBannerCard
import com.skillacademy.mobile.ui.components.SkillCourseCard
import com.skillacademy.mobile.ui.theme.*

/**
 * Entregable 2 — HomeScreen actualizado
 * Consume datos reales desde Supabase vía CoursesViewModel.
 * Muestra el error 'No fue posible obtener la información' ante fallos de conexión.
 */
@Composable
fun HomeScreen(
    courses: List<CourseUiModel>,
    banners: List<BannerUiModel>,
    categories: List<CategoryUiModel>,
    isLoading: Boolean = false,
    networkError: String? = null,
    onCourseClick: (Long) -> Unit,
    onAddToCart: (CourseUiModel) -> Unit,
    onNavigateToCatalog: (String?) -> Unit,
    onRefresh: () -> Unit = {},
    onOpenConsultaWeb: (() -> Unit)? = null
) {
    var showWelcomeDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Todos") }

    val filteredCourses = remember(selectedCategory, courses) {
        if (selectedCategory == "Todos") courses
        else courses.filter { it.categoria.equals(selectedCategory, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SkillBackground),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Banner de Error de Red (RF06: No fue posible obtener la información)
        if (networkError != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkillDiscountRed.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error de red",
                            tint = SkillDiscountRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No fue posible obtener la información.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SkillDiscountRed,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Error de conexión con el servidor. Comprueba tu conexión a Internet o activa el Wi-Fi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SkillTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = SkillDiscountRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REINTENTAR CONSULTA", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Acceso directo a Práctica 3: ConsultaWeb
        if (onOpenConsultaWeb != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenConsultaWeb() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkillCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Práctica 3",
                            tint = SkillCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🌐 Práctica 3: ConsultaWeb (URL Preasignada)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SkillNavy
                            )
                            Text(
                                text = "Pantalla de pruebas HTTP (Con/Sin conexión y URL modificada)",
                                fontSize = 11.sp,
                                color = SkillTextSecondary
                            )
                        }
                        Text(
                            text = "Abrir",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SkillCyan
                        )
                    }
                }
            }
        }
        // 1. Carrusel de Banners Promocionales (Supabase live)
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                if (isLoading && banners.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SkillSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SkillEmerald)
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(banners) { banner ->
                            PromoBannerCard(
                                banner = banner,
                                onBannerClick = { cursoId ->
                                    if (cursoId != null) onCourseClick(cursoId)
                                    else onNavigateToCatalog(null)
                                },
                                modifier = Modifier.fillParentMaxWidth(0.92f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Banner de Cupón Flash
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showWelcomeDialog = true },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Cupón",
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¡Cupones de Descuento Disponibles!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Toca para ver tu código SKILL20 o BIENVENIDA.",
                            fontSize = 11.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                    Text(
                        text = "Ver",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }

        // 3. Chips de Categorías (Supabase live)
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Explorar por Área",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )
                    if (!isLoading) {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualizar",
                                tint = SkillCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    item {
                        CategoryFilterChip(
                            nombre = "Todos",
                            isSelected = selectedCategory == "Todos",
                            onSelected = { selectedCategory = "Todos" }
                        )
                    }
                    items(categories) { cat ->
                        CategoryFilterChip(
                            nombre = cat.nombre,
                            isSelected = selectedCategory == cat.nombre,
                            onSelected = { selectedCategory = cat.nombre }
                        )
                    }
                }
            }
        }

        // 4. Sección "Cursos Más Vendidos" con datos reales
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cursos Más Vendidos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )
                    if (isLoading) {
                        Text(text = "Actualizando...", color = SkillTextMuted, fontSize = 11.sp)
                    }
                }
                Text(
                    text = "Ver todos",
                    color = SkillCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToCatalog(null) }
                )
            }
        }

        // 5. Grid de cursos filtrados
        if (isLoading && courses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SkillEmerald)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cargando cursos desde Supabase...", color = SkillTextMuted, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(filteredCourses) { curso ->
                SkillCourseCard(
                    curso = curso,
                    onCourseClick = onCourseClick,
                    onAddToCart = onAddToCart,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (filteredCourses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay cursos en esta categoría.",
                            color = SkillTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Modal de Cupones de Bienvenida
    if (showWelcomeDialog) {
        Dialog(onDismissRequest = { showWelcomeDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SkillSurface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { showWelcomeDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFCCFBF1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = SkillEmerald,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "¡Cupones Exclusivos!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Usa uno de estos cupones en el carrito de compras:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkillTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Cupón SKILL20
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("SKILL20", fontSize = 18.sp, fontWeight = FontWeight.Black, color = SkillEmerald, letterSpacing = 2.sp)
                        }
                        Text("20% OFF", fontWeight = FontWeight.Bold, color = SkillEmerald, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Cupón BIENVENIDA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("BIENVENIDA", fontSize = 15.sp, fontWeight = FontWeight.Black, color = SkillNavy, letterSpacing = 1.sp)
                        }
                        Text("10% OFF", fontWeight = FontWeight.Bold, color = SkillNavy, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { showWelcomeDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SkillEmerald),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("¡Entendido, voy al carrito!", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
