package com.skillacademy.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.data.mock.MockData
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.ui.components.StickyBuyBar
import com.skillacademy.mobile.ui.theme.*

@Composable
fun CourseDetailScreen(
    courseId: Long,
    onBackClick: () -> Unit,
    onAddToCart: (CourseUiModel) -> Unit,
    onBuyNow: (CourseUiModel) -> Unit
) {
    val curso = remember(courseId) {
        MockData.courses.find { it.id == courseId } ?: MockData.courses.first()
    }

    var isVideoPlaying by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(SkillBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. Cabecera Multimedia de Video Preview
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color(0xFF0F172A))
                ) {
                    if (isVideoPlaying) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "▶ Reproduciendo vista previa gratuita...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        // Imagen de portada + botón Play
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(SkillEmerald)
                                        .clickable { isVideoPlaying = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir preview",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Vista previa gratuita del curso",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Botón flotante para regresar
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White
                        )
                    }
                }
            }

            // 2. Información General del Curso
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge Categoría
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SkillCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = curso.categoria.uppercase(),
                                color = SkillCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        // Badge Nivel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = curso.nivel,
                                color = SkillTextSecondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = curso.titulo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Instructor: ${curso.instructor}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkillTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fila de Métricas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SkillSurface)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricItem(icon = Icons.Default.Star, value = "${curso.rating}", label = "Rating")
                        MetricItem(icon = Icons.Default.Group, value = "${curso.estudiantes}", label = "Alumnos")
                        MetricItem(icon = Icons.Default.AccessTime, value = "${curso.horas}h", label = "Duración")
                        MetricItem(icon = Icons.Default.Verified, value = "Sí", label = "Certificado")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Acerca de este curso",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = curso.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkillTextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // 3. Temario de Lecciones (Acordeón de contenidos)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Temario y Lecciones (${curso.lecciones.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            items(curso.lecciones) { leccion ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SkillSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (leccion.esGratis) SkillEmerald.copy(alpha = 0.15f) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (leccion.esGratis) Icons.Default.PlayArrow else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (leccion.esGratis) SkillEmerald else SkillTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${leccion.orden}. ${leccion.titulo}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = SkillTextPrimary
                            )
                            Text(
                                text = "${leccion.duracionMinutos} min" + if (leccion.esGratis) " • Clase de muestra gratis" else "",
                                fontSize = 11.sp,
                                color = if (leccion.esGratis) SkillEmerald else SkillTextMuted
                            )
                        }
                    }
                }
            }
        }

        // 4. Sticky Buy Bar en la parte inferior
        StickyBuyBar(
            precio = curso.precio,
            oldPrecio = curso.oldPrecio,
            onAddToCart = { onAddToCart(curso) },
            onBuyNow = { onBuyNow(curso) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MetricItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = SkillCyan, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SkillTextPrimary)
        Text(label, fontSize = 10.sp, color = SkillTextMuted)
    }
}
