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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.data.mock.MockData
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.ui.components.CategoryFilterChip
import com.skillacademy.mobile.ui.components.SkillCourseCard
import com.skillacademy.mobile.ui.theme.*

@Composable
fun CatalogScreen(
    initialCategory: String? = null,
    onCourseClick: (Long) -> Unit,
    onAddToCart: (CourseUiModel) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory ?: "Todos") }
    var selectedLevel by remember { mutableStateOf("Todos") }

    val allCourses = MockData.courses
    val levels = listOf("Todos", "Principiante", "Intermedio", "Avanzado")

    val filteredCourses = remember(searchQuery, selectedCategory, selectedLevel) {
        allCourses.filter { curso ->
            val matchQuery = searchQuery.isBlank() ||
                    curso.titulo.contains(searchQuery, ignoreCase = true) ||
                    curso.instructor.contains(searchQuery, ignoreCase = true) ||
                    curso.categoria.contains(searchQuery, ignoreCase = true)

            val matchCategory = selectedCategory == "Todos" ||
                    curso.categoria.equals(selectedCategory, ignoreCase = true)

            val matchLevel = selectedLevel == "Todos" ||
                    curso.nivel.equals(selectedLevel, ignoreCase = true)

            matchQuery && matchCategory && matchLevel
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkillBackground)
    ) {
        // Barra de Búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar cursos, temas o instructores...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = SkillCyan)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
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

        // Filtro de Categorías
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
            items(MockData.categories) { cat ->
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
                    label = { Text("Nivel: $lvl", fontSize = 11.sp) },
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
            text = "${filteredCourses.size} cursos disponibles",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SkillTextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Listado de Cursos
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredCourses.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No se encontraron cursos con estos filtros",
                            color = SkillTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Intenta buscar con otros términos o categorías.",
                            color = SkillTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
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
