package com.skillacademy.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.skillacademy.mobile.data.model.CouponUiState
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.data.remote.CouponRepository
import com.skillacademy.mobile.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Entregable 2 — Pantalla de Carrito de Compras (CartScreen)
 *
 * Características del Entregable 2:
 *  ✔ Persistencia local: Ítems preservados en SQLite incluso al reiniciar la app.
 *  ✔ Validación reactiva de cupones contra Supabase (/rest/v1/cupones en tiempo real).
 *  ✔ Recálculo financiero automático (Subtotal, Descuento, Total).
 *  ✔ Control de autenticación: Si el usuario es invitado, solicita inicio de sesión
 *    o registro antes de pagar, manteniendo los ítems intactos en caché.
 */
@Composable
fun CartScreen(
    cartItems: List<CourseUiModel>,
    isLoggedIn: Boolean = false,
    userDisplayName: String? = null,
    onRemoveItem: (Long) -> Unit,
    onCheckoutClick: (Double) -> Unit,
    onExploreCourses: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var couponInput by remember { mutableStateOf("") }
    var couponState by remember { mutableStateOf<CouponUiState>(CouponUiState.Idle) }

    val subtotal = remember(cartItems) { cartItems.sumOf { it.precio } }

    val discountPercent = when (val state = couponState) {
        is CouponUiState.Applied -> state.porcentaje
        else -> 0
    }

    val discountAmount = remember(subtotal, discountPercent) {
        subtotal * (discountPercent / 100.0)
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
                    text = "Explora nuestro catálogo y descubre cursos especializados para impulsar tu carrera profesional.",
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
                        text = "Mi Carrito (${cartItems.size} ${if (cartItems.size == 1) "curso" else "cursos"})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )
                }

                // Lista de Cursos en el Carrito (Persistidos en SQLite)
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
                            // Miniatura de color corporativo
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$${curso.precio}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = SkillEmerald
                                    )
                                    curso.oldPrecio?.let { old ->
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$$old",
                                            fontSize = 11.sp,
                                            color = SkillTextMuted,
                                            style = androidx.compose.ui.text.TextStyle(
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                            )
                                        )
                                    }
                                }
                            }
                            // Botón Eliminar del Carrito
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

                // Sección 2.5: Motor de Cupones en Tiempo Real con Supabase
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SkillSurface),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "¿Tienes un cupón de descuento?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SkillTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (couponState is CouponUiState.Applied) {
                                val applied = couponState as CouponUiState.Applied
                                Surface(
                                    color = Color(0xFFECFDF5),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SkillEmerald.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = SkillEmerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "CUPÓN: ${applied.codigo}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = SkillEmerald
                                                )
                                                Text(
                                                    text = "¡${applied.porcentaje}% de descuento aplicado con éxito!",
                                                    fontSize = 11.sp,
                                                    color = SkillTextSecondary
                                                )
                                            }
                                        }
                                        IconButton(onClick = {
                                            couponState = CouponUiState.Idle
                                            couponInput = ""
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Quitar cupón",
                                                tint = SkillTextMuted
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = couponInput,
                                        onValueChange = {
                                            couponInput = it.uppercase()
                                            if (couponState is CouponUiState.Error) {
                                                couponState = CouponUiState.Idle
                                            }
                                        },
                                        placeholder = { Text("Ej: SKILL20 o BIENVENIDA", fontSize = 12.sp) },
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
                                            coroutineScope.launch {
                                                couponState = CouponUiState.Validating
                                                val res = CouponRepository.validateCoupon(couponInput)
                                                couponState = res
                                            }
                                        },
                                        enabled = couponState !is CouponUiState.Validating && couponInput.isNotBlank(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SkillNavy)
                                    ) {
                                        if (couponState is CouponUiState.Validating) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Aplicar", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (couponState is CouponUiState.Error) {
                                    val err = couponState as CouponUiState.Error
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = err.message,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SkillDiscountRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Resumen Financiero y Botón Checkout
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

                    if (discountPercent > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Descuento ($discountPercent%)", color = SkillEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                            text = "$${String.format("%.2f", total)} USD",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = SkillEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Información o aviso de sesión
                    if (!isLoggedIn) {
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💡", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Inicia sesión o crea tu cuenta para formalizar la compra de tus cursos.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    } else if (userDisplayName != null) {
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👤", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Comprando como: $userDisplayName",
                                    fontSize = 11.sp,
                                    color = SkillEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = { onCheckoutClick(total) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLoggedIn) SkillEmerald else SkillNavy
                        )
                    ) {
                        Text(
                            text = if (isLoggedIn) "Proceder al Checkout ($${String.format("%.2f", total)})"
                            else "Iniciar Sesión o Registrarse para Comprar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
