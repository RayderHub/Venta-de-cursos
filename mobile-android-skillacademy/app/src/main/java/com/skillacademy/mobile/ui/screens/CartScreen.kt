package com.skillacademy.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.ui.theme.*

@Composable
fun CartScreen(
    cartItems: List<CourseUiModel>,
    onRemoveItem: (Long) -> Unit,
    onCheckoutClick: (Double) -> Unit,
    onExploreCourses: () -> Unit
) {
    var couponInput by remember { mutableStateOf("") }
    var appliedDiscountPercent by remember { mutableStateOf(0) }
    var couponMessage by remember { mutableStateOf<String?>(null) }
    var isCouponSuccess by remember { mutableStateOf(false) }

    val subtotal = remember(cartItems) { cartItems.sumOf { it.precio } }
    val discountAmount = remember(subtotal, appliedDiscountPercent) {
        subtotal * (appliedDiscountPercent / 100.0)
    }
    val total = remember(subtotal, discountAmount) {
        (subtotal - discountAmount).coerceAtLeast(0.0)
    }

    if (cartItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SkillBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = SkillNavy,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tu carrito está vacío",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SkillTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Explora nuestro catálogo y descubre cursos especializados para impulsar tu carrera.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkillTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onExploreCourses,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkillEmerald)
                ) {
                    Text("Explorar Catálogo", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SkillBackground)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Mi Carrito (${cartItems.size} cursos)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )
                }

                // Lista de Cursos en el Carrito
                items(cartItems) { curso ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SkillSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Miniatura
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SkillNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = curso.categoria.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = curso.titulo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SkillTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Instructor: ${curso.instructor}",
                                    fontSize = 11.sp,
                                    color = SkillTextMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$${curso.precio}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = SkillEmerald
                                )
                            }
                            // Botón Eliminar
                            IconButton(onClick = { onRemoveItem(curso.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Eliminar",
                                    tint = SkillDiscountRed
                                )
                            }
                        }
                    }
                }

                // Sección de Cupón de Descuento
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SkillSurface),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "¿Tienes un cupón de descuento?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SkillTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = couponInput,
                                    onValueChange = { couponInput = it.uppercase() },
                                    placeholder = { Text("Ej: SKILL20", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SkillSurface,
                                        unfocusedContainerColor = SkillSurface
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (couponInput.trim().equals("SKILL20", ignoreCase = true)) {
                                            appliedDiscountPercent = 20
                                            couponMessage = "¡Cupón SKILL20 aplicado con éxito (20% OFF)!"
                                            isCouponSuccess = true
                                        } else if (couponInput.trim().isNotEmpty()) {
                                            appliedDiscountPercent = 0
                                            couponMessage = "Cupón inválido o inactivo."
                                            isCouponSuccess = false
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SkillNavy)
                                ) {
                                    Text("Aplicar", fontWeight = FontWeight.Bold)
                                }
                            }
                            couponMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isCouponSuccess) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SkillEmerald,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = msg,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isCouponSuccess) SkillEmerald else SkillDiscountRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Resumen de Compra y Botón Checkout
            Surface(
                color = SkillSurface,
                tonalElevation = 12.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = SkillTextSecondary, fontSize = 13.sp)
                        Text("$${String.format("%.2f", subtotal)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    if (appliedDiscountPercent > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Descuento ($appliedDiscountPercent%)", color = SkillEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("-$${String.format("%.2f", discountAmount)}", color = SkillEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SkillBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total a Pagar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SkillTextPrimary
                        )
                        Text(
                            text = "$${String.format("%.2f", total)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = SkillEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { onCheckoutClick(total) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkillEmerald)
                    ) {
                        Text(
                            text = "Proceder al Checkout",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
