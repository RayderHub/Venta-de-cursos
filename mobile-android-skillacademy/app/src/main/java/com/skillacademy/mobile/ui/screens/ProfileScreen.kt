package com.skillacademy.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.data.remote.SupabaseProfile
import com.skillacademy.mobile.ui.theme.*

@Composable
fun ProfileScreen(
    profile: SupabaseProfile? = null,
    onLogoutClick: () -> Unit,
    onConsultaWebClick: (() -> Unit)? = null,
    onPractica4Click: (() -> Unit)? = null
) {
    val displayName  = profile?.fullName?.ifBlank { profile.email } ?: "Estudiante SkillAcademy"
    val displayEmail = profile?.email ?: "estudiante@skillacademy.com"
    val rolLabel     = profile?.rolLabel ?: "Estudiante"
    val roleColor    = profile?.let { Color(it.roleColor) } ?: SkillEmerald
    val iniciales    = displayName
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .uppercase()
        .ifBlank { displayEmail.take(2).uppercase() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkillBackground)
            .padding(16.dp)
    ) {
        // ── Tarjeta de Perfil ──────────────────────────────────────────────────
        Card(
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = SkillSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier  = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(roleColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iniciales, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text       = displayName,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = SkillTextPrimary
                    )
                    Text(
                        text  = displayEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = SkillTextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(roleColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text       = "Rol: $rolLabel",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = roleColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Opciones del menú ──────────────────────────────────────────────────
        Card(
            shape    = RoundedCornerShape(16.dp),
            colors   = CardDefaults.cardColors(containerColor = SkillSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {

                // Práctica 3 — ConsultaWeb
                if (onConsultaWebClick != null) {
                    ProfileMenuItem(
                        icon    = Icons.Default.CloudDownload,
                        title   = "ConsultaWeb (Práctica 3 URL Preasignada)",
                        onClick = onConsultaWebClick
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))
                }

                // Práctica 4 — Usuarios Supabase (CRUD)
                if (onPractica4Click != null) {
                    ProfileMenuItem(
                        icon    = Icons.Default.Storage,
                        title   = "Práctica 4: Usuarios Supabase (CRUD)",
                        badge   = "CRUD",
                        badgeColor = SkillCyan,
                        onClick = onPractica4Click
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))
                }

                ProfileMenuItem(icon = Icons.AutoMirrored.Filled.ReceiptLong, title = "Historial de Compras")
                ProfileMenuItem(icon = Icons.Default.School,                   title = "Mis Certificados")
                ProfileMenuItem(icon = Icons.Default.Notifications,            title = "Notificaciones de Ofertas")
                ProfileMenuItem(icon = Icons.Default.Security,                 title = "Seguridad y Contraseña")
                ProfileMenuItem(icon = Icons.AutoMirrored.Filled.HelpOutline,  title = "Centro de Ayuda")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Botón Cerrar Sesión ────────────────────────────────────────────────
        OutlinedButton(
            onClick  = onLogoutClick,
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = SkillDiscountRed),
            border   = androidx.compose.foundation.BorderStroke(1.dp, SkillDiscountRed.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = SkillDiscountRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    badge: String? = null,
    badgeColor: Color = SkillEmerald,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = SkillCyan, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text      = title,
            style     = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color     = SkillTextPrimary,
            modifier  = Modifier.weight(1f)
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SkillTextMuted)
    }
}
