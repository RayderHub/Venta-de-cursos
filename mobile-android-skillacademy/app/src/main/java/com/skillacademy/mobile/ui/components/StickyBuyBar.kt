package com.skillacademy.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.ui.theme.*

@Composable
fun StickyBuyBar(
    precio: Double,
    oldPrecio: Double?,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SkillSurface,
        tonalElevation = 16.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sección de Precios
            Column {
                Text(
                    text = "Precio final",
                    style = MaterialTheme.typography.labelSmall,
                    color = SkillTextMuted
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$$precio",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = SkillEmerald
                    )
                    oldPrecio?.let { old ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$$old",
                            fontSize = 14.sp,
                            textDecoration = TextDecoration.LineThrough,
                            color = SkillTextMuted
                        )
                    }
                }
            }

            // Botones de Acción
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Añadir al Carrito
                OutlinedIconButton(
                    onClick = onAddToCart,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        contentColor = SkillNavy
                    ),
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "Añadir al carrito",
                        tint = SkillNavy
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Comprar Ahora
                Button(
                    onClick = onBuyNow,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkillEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Comprar Ahora",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
