package com.skillacademy.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.data.remote.EnrolledCourse
import com.skillacademy.mobile.ui.components.CourseImage
import com.skillacademy.mobile.ui.theme.*

/**
 * Pantalla "Mis Cursos" — Entregable 2 & Prácticas 5/6
 *
 *  ✔ Cero cursos falsos de relleno: muestra exclusivamente las inscripciones reales del usuario en Supabase
 *  ✔ Barra de progreso real (0-100%) por cada curso inscrito
 *  ✔ Entrada directa al aula virtual (CourseLearnScreen) para ver lecciones y tareas
 *  ✔ Estado vacío elegante con botón de llamada a la acción para explorar el catálogo
 */
@Composable
fun MyCoursesScreen(
    enrolledCourses: List<EnrolledCourse>,
    isLoading: Boolean = false,
    onCourseLearnClick: (CourseUiModel, Int) -> Unit,
    onExploreClick: () -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SkillBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = SkillEmerald)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Cargando tus cursos de Supabase...", color = SkillTextMuted, fontSize = 13.sp)
            }
        }
        return
    }

    if (enrolledCourses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SkillBackground)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SkillNavy.copy(alpha = 0.1f),
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = SkillCyan,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Aún no te has inscrito en ningún curso",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SkillTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Explora nuestro catálogo con cientos de cursos de programación, diseño, finanzas y más para empezar a aprender hoy mismo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkillTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onExploreClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SkillEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Explorar Catálogo", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SkillBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Mis Cursos Inscritos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SkillTextPrimary
                )
                Text(
                    text = "Tienes ${enrolledCourses.size} curso(s) en progreso. ¡Sigue aprendiendo!",
                    style = MaterialTheme.typography.bodySmall,
                    color = SkillTextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(enrolledCourses, key = { it.inscripcionId }) { enrolled ->
            val curso = enrolled.curso
            val progreso = enrolled.progreso

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SkillSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCourseLearnClick(curso, progreso) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Miniatura real del curso
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            CourseImage(
                                imageSource = curso.imagen,
                                contentDescription = curso.titulo,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = curso.categoria.uppercase(),
                                color = SkillCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = curso.titulo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SkillTextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Prof. ${curso.instructor}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SkillTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Barra de Progreso
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progreso del curso",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SkillTextMuted
                        )
                        Text(
                            text = "$progreso%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (progreso >= 100) SkillEmerald else SkillNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (progreso / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (progreso >= 100) SkillEmerald else SkillCyan,
                        trackColor = SkillDivider
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botón para acceder al aula
                    Button(
                        onClick = { onCourseLearnClick(curso, progreso) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (progreso >= 100) SkillEmerald else SkillNavy
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Continuar aprendiendo",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (progreso >= 100) "Repasar Curso" else "Continuar Aprendiendo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
