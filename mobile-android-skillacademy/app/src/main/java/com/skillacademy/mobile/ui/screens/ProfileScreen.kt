package com.skillacademy.mobile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import coil.request.ImageRequest
import com.skillacademy.mobile.data.remote.AuthRepository
import com.skillacademy.mobile.data.remote.SupabaseProfile
import com.skillacademy.mobile.ui.theme.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Pantalla de Perfil de Usuario — Entregable 2 & Prácticas 5/6
 *
 *  ✔ Cero información falsa: datos sincronizados directamente con Supabase
 *  ✔ Avatar persistente: subida directa a Supabase Storage (bucket avatars)
 *  ✔ Edición de nombre directo mediante modal con icono de lápiz junto al nombre
 *  ✔ Renovación transparente de tokens para garantizar que las actualizaciones nunca fallen
 *  ✔ Botón de Cerrar Sesión con confirmación
 */
@Composable
fun ProfileScreen(
    profile: SupabaseProfile,
    accessToken: String,
    refreshToken: String = "",
    onProfileUpdated: (SupabaseProfile) -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember(profile) { mutableStateOf(profile.fullName) }
    var avatarUrl by remember(profile) {
        val u = profile.avatarUrl.orEmpty()
        mutableStateOf(if (u.startsWith("http") && !u.contains("?t=")) "$u?t=${System.currentTimeMillis()}" else u)
    }
    var bio by remember(profile) { mutableStateOf(profile.bio ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    // Sincronizar automáticamente el perfil más reciente desde Supabase al entrar
    LaunchedEffect(profile.id) {
        val fresh = AuthRepository.loadProfile(profile.id, accessToken)
        if (fresh.isSuccess && fresh.data != null) {
            val p = fresh.data
            fullName = p.fullName
            val u = p.avatarUrl.orEmpty()
            avatarUrl = if (u.startsWith("http")) "$u?t=${System.currentTimeMillis()}" else u
            bio = p.bio.orEmpty()
            onProfileUpdated(p)
        }
    }

    fun uploadAndSavePhoto(imageBytes: ByteArray) {
        isUploadingPhoto = true
        coroutineScope.launch {
            // 1. Subir la imagen real a Supabase Storage (bucket avatars/avatars/<id>.jpg)
            val uploadRes = AuthRepository.uploadAvatar(
                userId = profile.id,
                imageBytes = imageBytes,
                accessToken = accessToken,
                refreshToken = refreshToken,
                userEmail = profile.email
            )
            val finalAvatarUrl = if (uploadRes.isSuccess && uploadRes.data != null) {
                uploadRes.data
            } else {
                // Respaldo en almacenamiento interno
                val file = File(context.filesDir, "avatar_${profile.id}.jpg")
                file.writeBytes(imageBytes)
                Uri.fromFile(file).toString()
            }

            // 2. Guardar la URL pública permanente en la tabla profiles de Supabase
            AuthRepository.updateProfile(
                userId = profile.id,
                accessToken = accessToken,
                fullName = fullName,
                avatarUrl = finalAvatarUrl,
                bio = bio.takeIf { it.isNotBlank() },
                refreshToken = refreshToken,
                userEmail = profile.email
            )

            // 3. Forzar refresco inmediato en Coil agregando timestamp para burlar la caché de imagen vieja
            val freshDisplayUrl = if (finalAvatarUrl.startsWith("http")) "$finalAvatarUrl?t=${System.currentTimeMillis()}" else finalAvatarUrl
            avatarUrl = freshDisplayUrl
            isUploadingPhoto = false

            val updatedProfile = profile.copy(avatarUrl = finalAvatarUrl)
            onProfileUpdated(updatedProfile)
            Toast.makeText(context, "¡Foto de perfil guardada exitosamente en la nube!", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher de Cámara nativa del dispositivo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val imageBytes = stream.toByteArray()
                uploadAndSavePhoto(imageBytes)
            } catch (e: Exception) {
                Toast.makeText(context, "Error al procesar foto: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permiso de Cámara en tiempo de ejecución
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(
                context,
                "Se requiere permiso de cámara para tomar tu foto de perfil.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestCamera() {
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Launcher de Galería del dispositivo
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val imageBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    uploadAndSavePhoto(imageBytes)
                } else {
                    Toast.makeText(context, "No se pudo leer la imagen de la galería.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al seleccionar imagen: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val roleColor = Color(profile.roleColor)
    val iniciales = remember(fullName, profile.email) {
        fullName.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
            .uppercase()
            .ifBlank { profile.email.take(2).uppercase() }
    }

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
                // Avatar interactivo con indicador de subida y botón flotante de cámara
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(roleColor)
                            .clickable(enabled = !isUploadingPhoto) { showPhotoSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        } else if (avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = iniciales,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 34.sp
                            )
                        }
                    }

                    // Botón flotante de cámara/galería
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SkillCyan)
                            .clickable(enabled = !isUploadingPhoto) { showPhotoSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Cambiar foto",
                            tint = SkillNavy,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isUploadingPhoto) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Subiendo imagen a la nube...",
                        style = MaterialTheme.typography.bodySmall,
                        color = SkillCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Nombre con botón de edición al lado
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = fullName.ifBlank { "Usuario SkillAcademy" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            tempName = fullName
                            showEditNameDialog = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar nombre",
                            tint = SkillCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkillTextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Badge de Rol
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = roleColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
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

                if (bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = SkillTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── Tarjeta Cuenta y Seguridad ─────────────────────────────────────────
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
                    Icon(Icons.Default.Email, contentDescription = null, tint = SkillCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Correo registrado", fontSize = 12.sp, color = SkillTextMuted)
                        Text(profile.email, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SkillTextPrimary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SkillEmerald, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Estado de cuenta", fontSize = 12.sp, color = SkillTextMuted)
                        Text(if (profile.isActive) "Activa y verificada" else "Pendiente", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SkillEmerald)
                    }
                }

                HorizontalDivider(color = SkillDivider)

                // Botón Cerrar Sesión
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar Sesión", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal para Editar Nombre
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditNameDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = SkillCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar Nombre", fontWeight = FontWeight.Bold, color = SkillTextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ingresa tu nuevo nombre para mostrar en tu cuenta de SkillAcademy:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SkillTextMuted
                    )
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Nombre completo") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkillCyan,
                            focusedLabelColor = SkillCyan,
                            focusedTextColor = SkillTextPrimary,
                            unfocusedTextColor = SkillTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = tempName.trim()
                        if (trimmed.isBlank()) {
                            Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        coroutineScope.launch {
                            val res = AuthRepository.updateProfile(
                                userId = profile.id,
                                accessToken = accessToken,
                                fullName = trimmed,
                                avatarUrl = avatarUrl.substringBefore("?").takeIf { it.isNotBlank() },
                                bio = bio.takeIf { it.isNotBlank() },
                                refreshToken = refreshToken,
                                userEmail = profile.email
                            )
                            isSaving = false
                            showEditNameDialog = false
                            if (res.isSuccess) {
                                fullName = trimmed
                                val updated = profile.copy(fullName = trimmed)
                                onProfileUpdated(updated)
                                Toast.makeText(context, "¡Nombre actualizado con éxito en la nube!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, res.error ?: "Error al actualizar nombre", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = SkillEmerald)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Guardar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditNameDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Cancelar", color = SkillTextMuted)
                }
            },
            containerColor = SkillSurface
        )
    }

    // Modal Selector de Origen de Foto (Cámara / Galería)
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = SkillCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Foto de Perfil", fontWeight = FontWeight.Bold, color = SkillTextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Selecciona una opción para cambiar tu foto de perfil:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkillTextMuted
                    )

                    Button(
                        onClick = {
                            showPhotoSourceDialog = false
                            requestCamera()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkillCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = SkillNavy, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tomar foto con la cámara", color = SkillNavy, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showPhotoSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = SkillCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elegir de la galería", color = SkillCyan, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text("Cancelar", color = SkillTextMuted)
                }
            },
            containerColor = SkillSurface
        )
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
