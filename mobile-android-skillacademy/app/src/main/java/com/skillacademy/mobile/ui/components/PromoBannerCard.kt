package com.skillacademy.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.data.model.BannerUiModel
import com.skillacademy.mobile.ui.theme.SkillEmerald

@Composable
fun PromoBannerCard(
    banner: BannerUiModel,
    onBannerClick: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = when (banner.imagenFondo) {
        "emerald_banner" -> Brush.horizontalGradient(listOf(Color(0xFF0F2942), Color(0xFF0D9488)))
        "navy_banner" -> Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF0F2942)))
        "purple_banner" -> Brush.horizontalGradient(listOf(Color(0xFF6B21A8), Color(0xFF3B82F6)))
        else -> Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF0D9488)))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onBannerClick(banner.enlaceCursoId) },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.72f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Badge descuento
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SkillEmerald)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = banner.descuentoTexto,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = banner.titulo,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = banner.subtitulo,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                }

                // Botón CTA
                Button(
                    onClick = { onBannerClick(banner.enlaceCursoId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F2942)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "Ver Oferta",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Decoración visual
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "%",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
