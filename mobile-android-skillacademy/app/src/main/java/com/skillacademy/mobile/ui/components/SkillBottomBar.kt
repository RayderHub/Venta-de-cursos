package com.skillacademy.mobile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.skillacademy.mobile.ui.theme.SkillCyan
import com.skillacademy.mobile.ui.theme.SkillNavy
import com.skillacademy.mobile.ui.theme.SkillTextMuted

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Inicio", Icons.Default.Home)
    object Catalog : BottomNavItem("catalog", "Catálogo", Icons.Default.Search)
    object MyCourses : BottomNavItem("my_courses", "Mis Cursos", Icons.Default.PlayArrow)
    object Profile : BottomNavItem("profile", "Perfil", Icons.Default.Person)
}

@Composable
fun SkillBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Catalog,
        BottomNavItem.MyCourses,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = SkillNavy,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (isSelected) SkillCyan else SkillTextMuted
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        color = if (isSelected) SkillCyan else SkillTextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF1E3A5F)
                )
            )
        }
    }
}
