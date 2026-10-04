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
import androidx.compose.material.icons.filled.Http
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.skillacademy.mobile.data.mock.MockData
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.ui.components.CategoryFilterChip
import com.skillacademy.mobile.ui.components.PromoBannerCard
import com.skillacademy.mobile.ui.components.SkillCourseCard
import com.skillacademy.mobile.ui.theme.*

@Composable
fun HomeScreen(
    onCourseClick: (Long) -> Unit,
    onAddToCart: (CourseUiModel) -> Unit,
    onNavigateToCatalog: (String?) -> Unit,
    onConsultaWebClick: (() -> Unit)? = null
) {
    var showWelcomeDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Todos") }

    val courses = MockData.courses
    val banners = MockData.banners

    val filteredCourses = remember(selectedCategory) {
        if (selectedCategory == "Todos") courses
        else courses.filter { it.categoria.equals(selectedCategory, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SkillBackground),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Carrusel de Banners Promocionales
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
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

        // 2. Banner de Cupón Flash (Popup trigger)
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
                            text = "¡Cupón de Bienvenida Disponible!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Toca aquí para ver tu código de 20% de descuento.",
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

        // 2.1 Banner Directo Práctica 3: ConsultaWeb (URL Preasignada)
        if (onConsultaWebClick != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onConsultaWebClick() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SkillCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Http,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Práctica 3: ConsultaWeb",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SkillNavy
                            )
                            Text(
                                text = "Consumo HTTP con URL preasignada y parseo JSON.",
                                fontSize = 11.sp,
                                color = Color(0xFF0369A1)
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

        // 3. Chips de Categorías Rápidas
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Explorar por Área",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SkillTextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
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
                    items(MockData.categories) { cat ->
                        CategoryFilterChip(
                            nombre = cat.nombre,
                            isSelected = selectedCategory == cat.nombre,
                            onSelected = { selectedCategory = cat.nombre }
                        )
                    }
                }
            }
        }

        // 4. Sección "Cursos Más Vendidos"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cursos Más Vendidos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SkillTextPrimary
                )
                Text(
                    text = "Ver todos",
                    color = SkillCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToCatalog(null) }
                )
            }
        }

        // 5. Grid vertical de Cursos Filtrados
        items(filteredCourses) { curso ->
            SkillCourseCard(
                curso = curso,
                onCourseClick = onCourseClick,
                onAddToCart = onAddToCart,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    // Modal de Cupón de Bienvenida
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
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
                        text = "¡Descuento Exclusivo!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Usa el siguiente cupón en tu carrito de compras para obtener un 20% de descuento:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkillTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "SKILL20",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = SkillEmerald,
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { showWelcomeDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SkillNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("¡Entendido!", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
