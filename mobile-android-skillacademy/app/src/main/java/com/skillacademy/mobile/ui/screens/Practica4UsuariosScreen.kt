package com.skillacademy.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.skillacademy.mobile.data.remote.ProfilesRepository
import com.skillacademy.mobile.data.remote.SupabaseProfile
import com.skillacademy.mobile.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Práctica 4 — Almacenamiento estructurado en aplicación móvil
 *
 * Utiliza la base de datos Supabase del repositorio del proyecto (tabla `profiles`).
 *
 * Casos de evaluación:
 *  - Caso 1 — Insertar:   Registrar tres estudiantes (botón "+ Insertar Estudiante")
 *  - Caso 2 — Consultar:  dao.obtenerTodos() → botón "VER usuarios"
 *  - Caso 3 — Actualizar: Modificar la carrera o rol de un estudiante
 *  - Caso 4 — Campo correo/bio: Soporta correo y biografía/carrera + eliminación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Practica4UsuariosScreen(
    accessToken: String,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // ── Estado de la lista de usuarios ────────────────────────────────────────
    var usuarios    by remember { mutableStateOf<List<SupabaseProfile>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }
    var successMsg  by remember { mutableStateOf("") }

    // ── Estado del diálogo de Inserción (Caso 1 & 4) ───────────────────────────
    var showInsertDialog by remember { mutableStateOf(false) }
    var insertNombre     by remember { mutableStateOf("") }
    var insertCarrera    by remember { mutableStateOf("") }
    var insertEdad       by remember { mutableStateOf("") }
    var insertEmail      by remember { mutableStateOf("") }
    var isInserting      by remember { mutableStateOf(false) }
    var insertDialogError by remember { mutableStateOf("") }

    // ── Estado del diálogo de edición (Caso 3 & 4) ────────────────────────────
    var editTarget   by remember { mutableStateOf<SupabaseProfile?>(null) }
    var editNombre   by remember { mutableStateOf("") }
    var editRol      by remember { mutableStateOf("student") }
    var editBio      by remember { mutableStateOf("") }
    var showEdit     by remember { mutableStateOf(false) }

    // ── Diálogo de confirmación de eliminación (Caso 4) ───────────────────────
    var deleteTarget by remember { mutableStateOf<SupabaseProfile?>(null) }
    var showDelete   by remember { mutableStateOf(false) }

    val roles = listOf(
        "student"  to "Estudiante",
        "teacher"  to "Profesor",
        "admin"    to "Administrador"
    )

    // ── Función: dao.obtenerTodos() ───────────────────────────────────────────
    fun cargarUsuarios() {
        isLoading = true
        errorMsg  = ""
        coroutineScope.launch {
            val r = ProfilesRepository.obtenerTodos(accessToken)
            isLoading = false
            if (r.isSuccess) {
                usuarios = r.data ?: emptyList()
            } else {
                errorMsg = r.error ?: "Error al consultar usuarios"
            }
        }
    }

    // Carga inicial automática al abrir la pantalla
    LaunchedEffect(Unit) { cargarUsuarios() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Práctica 4 — Usuarios Supabase",
                            fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Text("Tabla: profiles | Base de datos del proyecto",
                            fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    // Botón directo para Insertar Estudiante (Caso 1)
                    FilledTonalButton(
                        onClick = {
                            insertNombre = ""
                            insertCarrera = ""
                            insertEdad = ""
                            insertEmail = ""
                            insertDialogError = ""
                            showInsertDialog = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SkillEmerald,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Insertar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Botón VER usuarios → dao.obtenerTodos() (Caso 2)
                    TextButton(onClick = { cargarUsuarios() }) {
                        Text("VER usuarios", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SkillNavy)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    insertNombre = ""
                    insertCarrera = ""
                    insertEdad = ""
                    insertEmail = ""
                    insertDialogError = ""
                    showInsertDialog = true
                },
                containerColor = SkillEmerald,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Insertar") },
                text = { Text("Insertar Estudiante", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SkillBackground)
                .padding(padding)
                .padding(16.dp)
        ) {

            // ── Banner informativo de la práctica ─────────────────────────────
            Card(
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📋 Práctica 4 — Operaciones CRUD",
                            fontWeight = FontWeight.Black, color = SkillEmerald, fontSize = 13.sp)
                        Surface(
                            color = SkillEmerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Supabase DB", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = SkillEmerald, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    listOf(
                        "👉 Caso 1 — Insertar: pulsa \"+ Insertar\" para registrar estudiantes",
                        "👉 Caso 2 — Consultar: pulsa \"VER usuarios\" (dao.obtenerTodos)",
                        "👉 Caso 3 — Actualizar: pulsa ✏️ para modificar carrera o rol",
                        "👉 Caso 4 — Campo Correo/Bio: incluye correo y carrera + eliminar con 🗑️"
                    ).forEach { line ->
                        Text(line, fontSize = 11.sp, color = Color(0xFF065F46),
                            modifier = Modifier.padding(top = 2.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botón de acción rápida para insertar
                    Button(
                        onClick = {
                            insertNombre = ""
                            insertCarrera = ""
                            insertEdad = ""
                            insertEmail = ""
                            insertDialogError = ""
                            showInsertDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkillEmerald),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Registrar Nuevo Estudiante (Caso 1)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Mensajes de feedback ──────────────────────────────────────────
            if (successMsg.isNotBlank()) {
                Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = SkillEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(successMsg, color = Color(0xFF065F46), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (errorMsg.isNotBlank()) {
                Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = Color(0xFF8A2F21), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMsg, color = Color(0xFF8A2F21), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Lista de usuarios ─────────────────────────────────────────────
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SkillEmerald)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Consultando usuarios (dao.obtenerTodos)...", color = SkillTextMuted, fontSize = 14.sp)
                    }
                }
            } else if (usuarios.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PeopleOutline, null, tint = SkillTextMuted,
                            modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Sin usuarios cargados.",
                            color = SkillTextMuted, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { cargarUsuarios() },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillNavy)
                        ) {
                            Text("Ejecutar dao.obtenerTodos()")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${usuarios.size} estudiantes / usuarios registrados",
                        fontWeight = FontWeight.Bold, color = SkillTextPrimary, fontSize = 14.sp)
                    TextButton(onClick = { cargarUsuarios() }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = SkillEmerald)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refrescar", fontSize = 12.sp, color = SkillEmerald, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(usuarios, key = { it.id }) { user ->
                        UsuarioCard(
                            usuario = user,
                            onEdit = {
                                editTarget  = user
                                editNombre  = user.fullName
                                editRol     = user.roleId
                                editBio     = user.bio ?: ""
                                showEdit    = true
                            },
                            onDelete = {
                                deleteTarget = user
                                showDelete   = true
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Diálogo INSERTAR nuevo estudiante (Caso 1 & 4) ─────────────────────────
    if (showInsertDialog) {
        Dialog(onDismissRequest = { if (!isInserting) showInsertDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SkillEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PersonAdd, null, tint = SkillEmerald, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Registrar Estudiante", fontWeight = FontWeight.Black, fontSize = 17.sp, color = SkillTextPrimary)
                            Text("Caso 1 — Insertar en Supabase", fontSize = 11.sp, color = SkillEmerald, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (insertDialogError.isNotBlank()) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Text(insertDialogError, color = Color(0xFF8A2F21), fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Campo: Nombre del estudiante
                    OutlinedTextField(
                        value         = insertNombre,
                        onValueChange = { insertNombre = it },
                        label         = { Text("Nombre del estudiante *") },
                        placeholder   = { Text("Ej. Juan Pérez") },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Campo: Carrera / Profesión (según la tabla de la práctica)
                    OutlinedTextField(
                        value         = insertCarrera,
                        onValueChange = { insertCarrera = it },
                        label         = { Text("Carrera / Profesión *") },
                        placeholder   = { Text("Ej. Ing. en Sistemas Computacionales") },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Campo: Edad (según la tabla de la práctica)
                    OutlinedTextField(
                        value         = insertEdad,
                        onValueChange = { insertEdad = it },
                        label         = { Text("Edad (opcional)") },
                        placeholder   = { Text("Ej. 21") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Campo: Correo electrónico (Caso 4)
                    OutlinedTextField(
                        value         = insertEmail,
                        onValueChange = { insertEmail = it },
                        label         = { Text("Correo electrónico (Caso 4)") },
                        placeholder   = { Text("Dejar vacío para autogenerar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick  = { showInsertDialog = false },
                            enabled  = !isInserting,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp)
                        ) { Text("Cancelar") }

                        Button(
                            onClick = {
                                if (insertNombre.isBlank()) {
                                    insertDialogError = "Por favor ingresa el nombre del estudiante"
                                    return@Button
                                }
                                if (insertCarrera.isBlank()) {
                                    insertDialogError = "Por favor ingresa la carrera o profesión"
                                    return@Button
                                }

                                isInserting = true
                                insertDialogError = ""
                                coroutineScope.launch {
                                    val r = ProfilesRepository.insertarEstudiante(
                                        nombre = insertNombre,
                                        correo = insertEmail,
                                        carrera = insertCarrera,
                                        edad = insertEdad,
                                        accessToken = accessToken
                                    )
                                    isInserting = false
                                    if (r.isSuccess) {
                                        showInsertDialog = false
                                        successMsg = "✅ Estudiante '${insertNombre.trim()}' registrado correctamente en Supabase (Caso 1)"
                                        cargarUsuarios()
                                    } else {
                                        insertDialogError = r.error ?: "Error al registrar estudiante"
                                    }
                                }
                            },
                            enabled  = !isInserting,
                            colors   = ButtonDefaults.buttonColors(containerColor = SkillEmerald),
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp)
                        ) {
                            if (isInserting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardando...")
                            } else {
                                Text("Insertar", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Diálogo Editar usuario (Casos 1, 3 y 4) ───────────────────────────────
    if (showEdit && editTarget != null) {
        val target = editTarget!!
        Dialog(onDismissRequest = { showEdit = false }) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Editar Estudiante / Usuario", fontWeight = FontWeight.Black, fontSize = 18.sp, color = SkillTextPrimary)
                    Text(target.email, fontSize = 12.sp, color = SkillTextMuted, modifier = Modifier.padding(bottom = 16.dp))

                    // Nombre
                    OutlinedTextField(
                        value         = editNombre,
                        onValueChange = { editNombre = it },
                        label         = { Text("Nombre completo") },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Carrera / Bio / Edad (Caso 3 y 4)
                    OutlinedTextField(
                        value         = editBio,
                        onValueChange = { editBio = it },
                        label         = { Text("Carrera / Profesión (Caso 3)") },
                        placeholder   = { Text("Ej. Ing. en Software") },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Rol
                    Text("Rol", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SkillTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    roles.forEach { (id, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = editRol == id,
                                onClick  = { editRol = id },
                                colors   = RadioButtonDefaults.colors(selectedColor = SkillEmerald)
                            )
                            Text(label, color = SkillTextPrimary, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick  = { showEdit = false },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp)
                        ) { Text("Cancelar") }

                        Button(
                            onClick = {
                                showEdit   = false
                                isLoading  = true
                                successMsg = ""
                                errorMsg   = ""
                                coroutineScope.launch {
                                    // Actualiza nombre + rol (Casos 1 y 3)
                                    val r1 = ProfilesRepository.actualizarNombreYRol(
                                        target.id, editNombre, editRol, accessToken)
                                    // Actualiza carrera/bio (Caso 3 y 4)
                                    val r2 = ProfilesRepository.actualizarBio(
                                        target.id, editBio, accessToken)
                                    isLoading = false
                                    if (r1.isSuccess && r2.isSuccess) {
                                        successMsg = "✅ Carrera/Perfil actualizado correctamente (Caso 3)"
                                        cargarUsuarios()
                                    } else {
                                        errorMsg = r1.error ?: r2.error ?: "Error al actualizar"
                                    }
                                }
                            },
                            colors   = ButtonDefaults.buttonColors(containerColor = SkillEmerald),
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp)
                        ) { Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    // ── Diálogo Confirmar eliminación (Caso 4) ────────────────────────────────
    if (showDelete && deleteTarget != null) {
        val target = deleteTarget!!
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title   = { Text("Eliminar usuario", fontWeight = FontWeight.Bold) },
            text    = { Text("¿Eliminar a ${target.fullName.ifBlank { target.email }}?\nEsta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete  = false
                    isLoading   = true
                    successMsg  = ""
                    errorMsg    = ""
                    coroutineScope.launch {
                        val r = ProfilesRepository.eliminar(target.id, accessToken)
                        isLoading = false
                        if (r.isSuccess) {
                            successMsg = "🗑️ Usuario eliminado correctamente"
                            cargarUsuarios()
                        } else {
                            errorMsg = r.error ?: "Error al eliminar"
                        }
                    }
                }) {
                    Text("Eliminar", color = SkillDiscountRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

// ── Tarjeta de usuario individual ─────────────────────────────────────────────

@Composable
private fun UsuarioCard(
    usuario: SupabaseProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val iniciales = usuario.fullName
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .uppercase()
        .ifBlank { usuario.email.take(2).uppercase() }

    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SkillSurface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con iniciales
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(usuario.roleColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(iniciales, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = usuario.fullName.ifBlank { "(Sin nombre)" },
                    fontWeight = FontWeight.Bold,
                    color = SkillTextPrimary,
                    fontSize = 14.sp
                )
                Text(usuario.email, fontSize = 12.sp, color = SkillTextMuted)

                // Carrera / Profesión (almacenado en `bio`)
                if (!usuario.bio.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "🎓 ${usuario.bio}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SkillEmerald
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Rol badge
                    Surface(
                        color  = Color(usuario.roleColor).copy(alpha = 0.12f),
                        shape  = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = usuario.rolLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(usuario.roleColor),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Estado activo
                    Text(
                        text = if (usuario.isActive) "🟢 Activo" else "🔴 Inactivo",
                        fontSize = 11.sp, color = SkillTextMuted
                    )
                }
            }

            // Acciones: Editar (Caso 3) y Eliminar (Caso 4)
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Editar", tint = SkillCyan)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Eliminar", tint = SkillDiscountRed)
                }
            }
        }
    }
}
