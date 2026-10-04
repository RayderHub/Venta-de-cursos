package com.skillacademy.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.ui.theme.SkillBorder
import com.skillacademy.mobile.ui.theme.SkillNavy
import com.skillacademy.mobile.ui.theme.SkillSurface
import com.skillacademy.mobile.ui.theme.SkillTextPrimary

@Composable
fun CategoryFilterChip(
    nombre: String,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onSelected,
        label = {
            Text(
                text = nombre,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SkillNavy,
            selectedLabelColor = Color.White,
            containerColor = SkillSurface,
            labelColor = SkillTextPrimary
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) SkillNavy else SkillBorder
        ),
        modifier = Modifier.padding(end = 8.dp)
    )
}
