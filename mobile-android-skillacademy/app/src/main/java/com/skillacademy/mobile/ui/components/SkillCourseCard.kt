package com.skillacademy.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.ui.theme.*

@Composable
fun SkillCourseCard(
    curso: CourseUiModel,
    onCourseClick: (Long) -> Unit,
    onAddToCart: (CourseUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCourseClick(curso.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SkillSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                // Miniatura real agregada por el profesor (Base64, URL o Tema)
                CourseImage(
                    imageSource = curso.imagen,
                    contentDescription = curso.titulo,
                    modifier = Modifier.fillMaxSize()
                )

                // Sombra suave en los bordes para resaltar badges
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f)
                                )
                            )
                        )
                )

                // Badge de descuento (-XX%)
                curso.descuentoPorcentaje?.let { desc ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SkillEmerald)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "-$desc%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Badge Nivel
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = curso.nivel,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            // Información del Curso
            Column(modifier = Modifier.padding(14.dp)) {
                // Categoría
                Text(
                    text = curso.categoria.uppercase(),
                    color = SkillCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Título
                Text(
                    text = curso.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SkillTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Instructor
                Text(
                    text = "Por ${curso.instructor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SkillTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Calificación con estrellas
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = SkillStarGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = curso.rating.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SkillTextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${curso.reviews} reseñas)",
                        fontSize = 11.sp,
                        color = SkillTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Precios y Botón de Añadir al Carrito
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$${curso.precio}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SkillEmerald
                            )
                            curso.oldPrecio?.let { old ->
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$$old",
                                    fontSize = 13.sp,
                                    textDecoration = TextDecoration.LineThrough,
                                    color = SkillTextMuted
                                )
                            }
                        }
                    }

                    // Botón de acción rápida Carrito
                    FilledIconButton(
                        onClick = { onAddToCart(curso) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = SkillEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "Añadir al carrito",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
