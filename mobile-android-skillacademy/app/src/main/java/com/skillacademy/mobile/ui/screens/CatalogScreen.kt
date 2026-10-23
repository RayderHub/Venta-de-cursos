package com.skillacademy.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.data.model.CategoryUiModel
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.ui.components.CategoryFilterChip
import com.skillacademy.mobile.ui.components.SkillCourseCard
import com.skillacademy.mobile.ui.theme.*

/**
 * Entregable 2 — CatalogScreen actualizado
 * Búsqueda predictiva en tiempo real con Supabase y fallback local.
 */
@Composable
fun CatalogScreen(
    courses: List<CourseUiModel>,
    categories: List<CategoryUiModel>,
    isLoading: Boolean = false,
    initialCategory: String? = null,
    onCourseClick: (Long) -> Unit,
    onAddToCart: (CourseUiModel) -> Unit,
    onSearchQueryChanged: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory ?: "Todos") }
    var selectedLevel by remember { mutableStateOf("Todos") }
    val levels = listOf("Todos", "Principiante", "Intermedio", "Avanzado")

    /**
     * Los cursos ya llegan filtrados por Elasticsearch desde el ViewModel.
     * Aquí solo se aplican los chips de Categoría y Nivel como refinamiento secundario.
     */
    val filteredCourses = remember(selectedCategory, selectedLevel, courses) {
        courses.filter { curso ->
            val matchCategory = selectedCategory == "Todos" ||
                    curso.categoria.equals(selectedCategory, ignoreCase = true)

            val matchLevel = selectedLevel == "Todos" ||
                    curso.nivel.equals(selectedLevel, ignoreCase = true)

            matchCategory && matchLevel
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkillBackground)
    ) {
        // Barra de Búsqueda Predictiva (conectada a CoursesViewModel para búsqueda en Supabase)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onSearchQueryChanged(it)
            },
            placeholder = { Text("Buscar cursos, temas o instructores...", fontSize = 13.sp) },
            leadingIcon = {
                if (isLoading && searchQuery.isNotEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SkillCyan, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = SkillCyan)
                }
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        onSearchQueryChanged("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SkillSurface,
                unfocusedContainerColor = SkillSurface,
                focusedBorderColor = SkillCyan,
                unfocusedBorderColor = SkillBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Filtro de Categorías (desde Supabase)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.padding(bottom = 6.dp)
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

        // Filtro de Niveles
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(levels) { lvl ->
                FilterChip(
                    selected = selectedLevel == lvl,
                    onClick = { selectedLevel = lvl },
                    label = { Text("$lvl", fontSize = 11.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFCCFBF1),
                        selectedLabelColor = SkillEmerald,
                        containerColor = SkillSurface,
                        labelColor = SkillTextSecondary
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        // Contador de Resultados
        Text(
            text = when {
                isLoading && searchQuery.isNotBlank() -> "Buscando en Elasticsearch..."
                searchQuery.isNotBlank() -> "${filteredCourses.size} resultado(s) de Elasticsearch"
                else -> "${filteredCourses.size} cursos disponibles"
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) SkillCyan else SkillTextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Listado de Cursos
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isLoading && courses.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = SkillEmerald)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    if (searchQuery.isNotBlank())
                                        "Buscando en Elasticsearch..."
                                    else
                                        "Cargando catálogo desde Supabase...",
                                    color = SkillTextMuted, fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                filteredCourses.isEmpty() -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No se encontraron cursos",
                                color = SkillTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Intenta con otros términos de búsqueda."
                                else
                                    "Intenta con otra categoría o nivel.",
                                color = SkillTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                else -> {
                    items(filteredCourses) { curso ->
                        SkillCourseCard(
                            curso = curso,
                            onCourseClick = onCourseClick,
                            onAddToCart = onAddToCart
                        )
                    }
                }
            }
        }
    }
}
