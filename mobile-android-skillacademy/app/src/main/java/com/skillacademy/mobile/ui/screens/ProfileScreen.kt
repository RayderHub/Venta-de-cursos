package com.skillacademy.mobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.skillacademy.mobile.data.remote.AuthRepository
import com.skillacademy.mobile.data.remote.SupabaseProfile
import com.skillacademy.mobile.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Pantalla de Perfil de Usuario — Entregable 2 & Prácticas 5/6
 *
 *  ✔ Cero información falsa: requiere sesión real activa
 *  ✔ Desglose completo de datos: nombre, email, rol, biografía y foto
 *  ✔ Edición de Nombre y Foto de Perfil (con selector rápido de avatares)
 *  ✔ Guarda los cambios directamente en Supabase (tabla profiles)
 *  ✔ Botón de Cerrar Sesión con confirmación
 */
@Composable
fun ProfileScreen(
    profile: SupabaseProfile,
    accessToken: String,
    onProfileUpdated: (SupabaseProfile) -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember(profile) { mutableStateOf(profile.fullName) }
    var avatarUrl by remember(profile) { mutableStateOf(profile.avatarUrl ?: "") }
    var bio by remember(profile) { mutableStateOf(profile.bio ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val roleColor = Color(profile.roleColor)
    val iniciales = remember(fullName, profile.email) {
        fullName.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
            .uppercase()
            .ifBlank { profile.email.take(2).uppercase() }
    }

    // Avatares rápidos preconfigurados tipo DiceBear
    val presetAvatars = listOf(
        "https://api.dicebear.com/7.x/bottts/png?seed=${profile.id}&backgroundColor=0f172a",
        "https://api.dicebear.com/7.x/avataaars/png?seed=${profile.email}&backgroundColor=1e293b",
        "https://api.dicebear.com/7.x/lorelei/png?seed=${profile.fullName.ifBlank { "skill" }}&backgroundColor=0d9488",
        "https://api.dicebear.com/7.x/notionists/png?seed=${profile.id}&backgroundColor=2563eb",
        "https://api.dicebear.com/7.x/personas/png?seed=${profile.email}&backgroundColor=8b5cf6"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkillBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Tarjeta Cabecera de Perfil ─────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SkillSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar grande
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(roleColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = iniciales,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = fullName.ifBlank { "Usuario SkillAcademy" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SkillTextPrimary
                )

                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkillTextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Badge de Rol
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = roleColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(roleColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = profile.rolLabel,
                            color = roleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // ── Formulario de Edición de Perfil ────────────────────────────────────
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SkillSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = SkillCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Editar Información",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )
                }

                Text(
                    text = "Personaliza tu nombre y tu avatar visible en el catálogo, reseñas y foro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SkillTextMuted
                )

                // Nombre completo
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nombre Completo") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SkillCyan) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkillCyan,
                        focusedLabelColor = SkillCyan,
                        focusedTextColor = SkillTextPrimary,
                        unfocusedTextColor = SkillTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // URL de Foto / Avatar
                OutlinedTextField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    label = { Text("URL de Foto de Perfil") },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = SkillCyan) },
                    placeholder = { Text("https://ejemplo.com/foto.jpg") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkillCyan,
                        focusedLabelColor = SkillCyan,
                        focusedTextColor = SkillTextPrimary,
                        unfocusedTextColor = SkillTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector de avatares rápidos
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "O elige un avatar temático:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SkillTextMuted,
                        fontSize = 12.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetAvatars) { preset ->
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(SkillNavy)
                                    .clickable { avatarUrl = preset }
                                    .then(
                                        if (avatarUrl == preset) {
                                            Modifier.background(SkillCyan.copy(alpha = 0.3f))
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = preset,
                                    contentDescription = "Avatar preset",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Biografía (solo si es profesor o disponible para todos)
                if (profile.roleId == "teacher") {
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Biografía del Instructor") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = SkillCyan) },
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkillCyan,
                            focusedLabelColor = SkillCyan,
                            focusedTextColor = SkillTextPrimary,
                            unfocusedTextColor = SkillTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Botón Guardar Cambios
                Button(
                    onClick = {
                        if (fullName.isBlank()) {
                            Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        coroutineScope.launch {
                            val result = AuthRepository.updateProfile(
                                userId = profile.id,
                                accessToken = accessToken,
                                fullName = fullName.trim(),
                                avatarUrl = avatarUrl.trim().takeIf { it.isNotBlank() },
                                bio = bio.trim().takeIf { it.isNotBlank() }
                            )
                            isSaving = false
                            if (result.isSuccess) {
                                val updated = profile.copy(
                                    fullName = fullName.trim(),
                                    avatarUrl = avatarUrl.trim().takeIf { it.isNotBlank() },
                                    bio = bio.trim().takeIf { it.isNotBlank() }
                                )
                                onProfileUpdated(updated)
                                Toast.makeText(context, "¡Perfil actualizado con éxito!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, result.error ?: "Error al guardar perfil", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = SkillEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardando en Supabase...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Tarjeta Cuenta / Seguridad ─────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SkillSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cuenta y Sesión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SkillTextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SkillTextMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Correo registrado", fontSize = 12.sp, color = SkillTextMuted)
                        Text(profile.email, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SkillTextPrimary)
                    }
                }

                Divider(color = SkillDivider)

                // Botón Cerrar Sesión
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Cerrar Sesión", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal de Confirmación de Logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("¿Cerrar Sesión?", fontWeight = FontWeight.Bold) },
            text = { Text("Tendrás que ingresar tus credenciales nuevamente para acceder a tus cursos y perfil.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Sí, cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
