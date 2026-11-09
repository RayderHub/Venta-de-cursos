package com.skillacademy.mobile.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Entregable 3 — Pasarela de Pago Transaccional y Checkout Móvil
 *
 * Cumple con los requerimientos de la Sección 3.1:
 *  ✔ Selección de Medios de Pago (Tarjeta, Google Pay, Saldo Educativo)
 *  ✔ Resumen financiero con cálculo de cupones
 *  ✔ Simulación de tokenización segura
 *  ✔ Generación de Número de Orden (ORD-XXXXXX)
 *  ✔ Transacción conectada al alta de inscripciones en Supabase
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentGatewayBottomSheet(
    totalAmount: Double,
    subtotalAmount: Double,
    discountAmount: Double,
    courses: List<CourseUiModel>,
    userEmail: String,
    onDismissRequest: () -> Unit,
    onPaymentConfirmed: (orderNumber: String, paymentMethod: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // ISO 27001 A.8.11 / A.8.12: Blindaje de pasarela con FLAG_SECURE
    // Bloquea capturas de pantalla y grabadores mientras el bottom sheet esté visible
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    var selectedMethod by remember { mutableStateOf("card") } // "card" | "gpay" | "instant"
    var cardNumber by remember { mutableStateOf("4242 4242 4242 4242") }
    var cardExpiry by remember { mutableStateOf("12/28") }
    var cardCvv by remember { mutableStateOf("789") }
    var cardHolder by remember { mutableStateOf("Estudiante SkillAcademy") }
    var secureTokenize by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = SkillSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pasarela de Pago Segura",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )
                    Text(
                        text = "SkillAcademy Pay • Encriptación 256-bit SSL",
                        fontSize = 11.sp,
                        color = SkillTextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SkillEmerald.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SkillEmerald, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PCI DSS", color = SkillEmerald, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }

            // Resumen de la Orden
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SkillBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal (${courses.size} curso/s)", fontSize = 12.sp, color = SkillTextSecondary)
                        Text("$${String.format("%.2f", subtotalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SkillTextPrimary)
                    }
                    if (discountAmount > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Descuento cupón", fontSize = 12.sp, color = SkillEmerald)
                            Text("-$${String.format("%.2f", discountAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkillEmerald)
                        }
                    }
                    Divider(color = SkillDivider, thickness = 0.8.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total a pagar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SkillTextPrimary)
                        Text("$${String.format("%.2f", totalAmount)} USD", fontSize = 16.sp, fontWeight = FontWeight.Black, color = SkillEmerald)
                    }
                }
            }

            // Selector de Medios de Pago
            Text(
                text = "Selecciona un medio de pago:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SkillTextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Opción 1: Tarjeta
                PaymentMethodChip(
                    title = "Tarjeta",
                    icon = Icons.Default.CreditCard,
                    isSelected = selectedMethod == "card",
                    onClick = { selectedMethod = "card" },
                    modifier = Modifier.weight(1f)
                )

                // Opción 2: Google Pay
                PaymentMethodChip(
                    title = "Google Pay",
                    icon = Icons.Default.AccountBalanceWallet,
                    isSelected = selectedMethod == "gpay",
                    onClick = { selectedMethod = "gpay" },
                    modifier = Modifier.weight(1f)
                )

                // Opción 3: Saldo / Pago Instantáneo
                PaymentMethodChip(
                    title = "Instant Pay",
                    icon = Icons.Default.Bolt,
                    isSelected = selectedMethod == "instant",
                    onClick = { selectedMethod = "instant" },
                    modifier = Modifier.weight(1f)
                )
            }

            // Formulario según método seleccionado
            AnimatedVisibility(visible = selectedMethod == "card") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text("Número de Tarjeta") },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = SkillCyan) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkillCyan,
                            focusedTextColor = SkillTextPrimary,
                            unfocusedTextColor = SkillTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = cardExpiry,
                            onValueChange = { cardExpiry = it },
                            label = { Text("MM/AA") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkillCyan,
                                focusedTextColor = SkillTextPrimary,
                                unfocusedTextColor = SkillTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cardCvv,
                            onValueChange = { cardCvv = it },
                            label = { Text("CVV") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkillCyan,
                                focusedTextColor = SkillTextPrimary,
                                unfocusedTextColor = SkillTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = cardHolder,
                        onValueChange = { cardHolder = it },
                        label = { Text("Titular de la Tarjeta") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkillCyan,
                            focusedTextColor = SkillTextPrimary,
                            unfocusedTextColor = SkillTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AnimatedVisibility(visible = selectedMethod == "gpay") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1F2937),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Google Pay Vinculado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Cuenta: $userEmail", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedMethod == "instant") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SkillEmerald.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, SkillEmerald.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SkillEmerald, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Aprobación Educativa Inmediata", color = SkillEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Habilitación automática de lecciones en tu cuenta.", color = SkillTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Switch tokenización segura
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SkillCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tokenizar y confirmar compra", fontSize = 12.sp, color = SkillTextSecondary)
                }
                Switch(
                    checked = secureTokenize,
                    onCheckedChange = { secureTokenize = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SkillEmerald)
                )
            }

            // Botón de Confirmación y Pago
            Button(
                onClick = {
                    isProcessing = true
                    coroutineScope.launch {
                        delay(1200) // Simulación de contacto seguro con pasarela
                        val orderCode = "ORD-${Random.nextInt(100000, 999999)}"
                        val methodLabel = when (selectedMethod) {
                            "card" -> "Tarjeta •••• ${cardNumber.takeLast(4).ifBlank { "4242" }}"
                            "gpay" -> "Google Pay"
                            else -> "SkillPay Instantáneo"
                        }
                        isProcessing = false
                        onPaymentConfirmed(orderCode, methodLabel)
                    }
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = SkillEmerald),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Procesando pago seguro...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar y Pagar $${String.format("%.2f", totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) SkillCyan.copy(alpha = 0.12f) else SkillBackground,
        border = if (isSelected) BorderStroke(1.5.dp, SkillCyan) else BorderStroke(1.dp, SkillDivider),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) SkillCyan else SkillTextMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) SkillCyan else SkillTextSecondary
            )
        }
    }
}
