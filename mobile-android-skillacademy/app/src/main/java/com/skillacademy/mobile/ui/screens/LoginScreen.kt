package com.skillacademy.mobile.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.data.local.SessionManager
import com.skillacademy.mobile.data.remote.AuthRepository
import com.skillacademy.mobile.data.remote.SupabaseProfile
import com.skillacademy.mobile.data.remote.SupabaseSession
import com.skillacademy.mobile.ui.theme.*
import android.util.Log
import kotlinx.coroutines.launch

// ─── Constantes de Código Seguro (Práctica 5 - AccesoSeguro) ──────────────────
private const val MAX_USER_LENGTH = 50
private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_LENGTH = 32

/**
 * Entregable 2 — Pantalla de Autenticación Unificada (Login y Registro)
 *
 * Réplica funcional de la autenticación web de SkillAcademy:
 *  ✔ Inicio de sesión con Email y Contraseña
 *  ✔ Registro de nuevos alumnos (trigger on_auth_user_created con rol student)
 *  ✔ Persistencia de sesión con Recordarme
 *  ✔ Validación reactiva de campos
 *  ✔ Retorno inmediato a la compra/carrito tras autenticarse
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (session: SupabaseSession, profile: SupabaseProfile) -> Unit,
    onSkipLogin: (() -> Unit)? = null
) {
    // Interceptar botón atrás físico para regresar con seguridad al home/catálogo
    BackHandler {
        onSkipLogin?.invoke()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val sessionManager = remember { SessionManager(context) }

    val passwordFocus = remember { FocusRequester() }
    val confirmPasswordFocus = remember { FocusRequester() }

    // 0 = Iniciar Sesión, 1 = Registrarse
    var authMode by remember { mutableIntStateOf(0) }

    // ── Estado del formulario ─────────────────────────────────────────────────
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    var fullNameTouched by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var confirmPasswordTouched by remember { mutableStateOf(false) }

    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    // Control antifuerza bruta bajo ISO 27001 A.8.5 (Entregable 5)
    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }

    // ── Validaciones de Seguridad (Práctica 5: AccesoSeguro) ───────────────────
    val fullNameError = when {
        authMode == 1 && fullNameTouched && fullName.isBlank() -> "Nombre es requerido"
        authMode == 1 && fullNameTouched && fullName.trim().length < 3 -> "Mínimo 3 letras"
        else -> ""
    }

    // Regla 2 y 3: Validar información, rechazar sólo espacios y longitud máxima
    val emailError = when {
        emailTouched && email.trim().isEmpty() -> "El usuario no puede estar vacío ni contener solo espacios"
        emailTouched && email.length > MAX_USER_LENGTH -> "El usuario no puede superar los $MAX_USER_LENGTH caracteres"
        authMode == 1 && emailTouched && (!email.contains("@") || !email.contains(".")) -> "Email inválido"
        else -> ""
    }

    // Regla 3 y adicional: Mínimo 8 caracteres, máximo 32, no vacío
    val passwordError = when {
        passwordTouched && password.isEmpty() -> "Contraseña es requerida"
        passwordTouched && password.length < MIN_PASSWORD_LENGTH -> "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres"
        passwordTouched && password.length > MAX_PASSWORD_LENGTH -> "La contraseña no puede superar los $MAX_PASSWORD_LENGTH caracteres"
        else -> ""
    }

    val confirmPasswordError = when {
        authMode == 1 && confirmPasswordTouched && confirmPassword != password -> "Las contraseñas no coinciden"
        else -> ""
    }

    val isFormValid = if (authMode == 0) {
        emailError.isEmpty() && passwordError.isEmpty() && email.trim().isNotEmpty() && password.isNotEmpty()
    } else {
        fullNameError.isEmpty() && emailError.isEmpty() && passwordError.isEmpty() &&
                confirmPasswordError.isEmpty() && fullName.isNotBlank() && email.trim().isNotEmpty() &&
                password.isNotEmpty() && confirmPassword.isNotEmpty()
    }

    // ── Fondo degradado esmeralda corporativo ─────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0D4A3A), Color(0xFF0F2942)),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Orbes de ambientación
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-160).dp)
                .alpha(0.25f)
                .background(
                    brush = Brush.radialGradient(listOf(SkillEmerald, Color.Transparent)),
                    shape = RoundedCornerShape(50)
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = 120.dp, y = 200.dp)
                .alpha(0.18f)
                .background(
                    brush = Brush.radialGradient(listOf(SkillEmeraldLight, Color.Transparent)),
                    shape = RoundedCornerShape(50)
                )
        )

        // ── Tarjeta de Autenticación ──────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barra superior de navegación / volver
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onSkipLogin != null) {
                        TextButton(
                            onClick = onSkipLogin,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = SkillNavy,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Volver a la tienda",
                                color = SkillNavy,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Insignia SkillAcademy
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFCCFBF1)
                    ) {
                        Text(
                            text = "SkillAcademy",
                            color = SkillEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de pestañas: Iniciar Sesión / Registrarse
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (authMode == 0) Color.White else Color.Transparent)
                                .clickable {
                                    authMode = 0
                                    errorMsg = ""
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Iniciar Sesión",
                                fontWeight = if (authMode == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (authMode == 0) SkillNavy else SkillTextMuted,
                                fontSize = 14.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (authMode == 1) Color.White else Color.Transparent)
                                .clickable {
                                    authMode = 1
                                    errorMsg = ""
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Crear Cuenta",
                                fontWeight = if (authMode == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (authMode == 1) SkillNavy else SkillTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (authMode == 0) "¡Bienvenido de nuevo!" else "Únete a SkillAcademy",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = SkillTextPrimary
                )
                Text(
                    text = if (authMode == 0)
                        "Inicia sesión para comprar y acceder a tus cursos."
                    else
                        "Crea tu cuenta de estudiante en un instante.",
                    fontSize = 12.sp,
                    color = SkillTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                // ── Mensaje de Error Global ──────────────────────────────────
                if (errorMsg.isNotBlank()) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg,
                            color = Color(0xFF8A2F21),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Campo Nombre Completo (solo en registro) ──────────────────
                if (authMode == 1) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Nombre Completo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SkillTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it; fullNameTouched = true; errorMsg = "" },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ej: Laura Gómez", color = SkillTextMuted, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = SkillTextMuted) },
                            isError = fullNameError.isNotEmpty(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(10.dp),
                            colors = loginFieldColors()
                        )
                        if (fullNameError.isNotEmpty()) {
                            Text(fullNameError, color = Color(0xFFC0392B), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Campo Email / Usuario (Reglas 1, 2 y 3) ──────────────────────
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (authMode == 0) "Usuario o Email" else "Email",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SkillTextPrimary
                        )
                        Text(
                            text = "${email.length}/$MAX_USER_LENGTH",
                            fontSize = 11.sp,
                            color = if (email.length >= MAX_USER_LENGTH) Color(0xFFC0392B) else SkillTextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            if (it.length <= MAX_USER_LENGTH) {
                                email = it
                                emailTouched = true
                                errorMsg = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("usuario o correo@ejemplo.com", color = SkillTextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = SkillTextMuted) },
                        isError = emailError.isNotEmpty(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                        shape = RoundedCornerShape(10.dp),
                        colors = loginFieldColors()
                    )
                    if (emailError.isNotEmpty()) {
                        Text(emailError, color = Color(0xFFC0392B), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Campo Contraseña (Reglas 1, 3 y 4) ──────────────────────────
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Contraseña", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SkillTextPrimary)
                        Text(
                            text = "${password.length}/$MAX_PASSWORD_LENGTH",
                            fontSize = 11.sp,
                            color = if (password.length >= MAX_PASSWORD_LENGTH) Color(0xFFC0392B) else SkillTextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            if (it.length <= MAX_PASSWORD_LENGTH) {
                                password = it
                                passwordTouched = true
                                errorMsg = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocus),
                        placeholder = { Text("Mínimo $MIN_PASSWORD_LENGTH caracteres", color = SkillTextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = SkillTextMuted) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Ocultar" else "Mostrar",
                                    tint = SkillTextMuted
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = passwordError.isNotEmpty(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (authMode == 1) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { confirmPasswordFocus.requestFocus() },
                            onDone = { focusManager.clearFocus() }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = loginFieldColors()
                    )
                    if (passwordError.isNotEmpty()) {
                        Text(passwordError, color = Color(0xFFC0392B), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                }

                // ── Campo Confirmar Contraseña (solo en registro) ────────────
                if (authMode == 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Confirmar Contraseña", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SkillTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; confirmPasswordTouched = true; errorMsg = "" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(confirmPasswordFocus),
                            placeholder = { Text("Repite tu contraseña", color = SkillTextMuted, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = SkillTextMuted) },
                            visualTransformation = PasswordVisualTransformation(),
                            isError = confirmPasswordError.isNotEmpty(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            shape = RoundedCornerShape(10.dp),
                            colors = loginFieldColors()
                        )
                        if (confirmPasswordError.isNotEmpty()) {
                            Text(confirmPasswordError, color = Color(0xFFC0392B), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Recordarme (en login) ────────────────────────────────────
                if (authMode == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = SkillEmerald)
                        )
                        Text("Recordarme", fontSize = 13.sp, color = SkillTextSecondary,
                            modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Botón Principal de Acción ────────────────────────────────
                Button(
                    onClick = {
                        emailTouched = true
                        passwordTouched = true
                        if (authMode == 1) {
                            fullNameTouched = true
                            confirmPasswordTouched = true
                        }

                        // Requisito 6: Evitar imprimir contraseñas u otros datos confidenciales en Logcat
                        Log.i("AccesoSeguro", "=== Evento de Autenticación ===")
                        Log.i("AccesoSeguro", "Validando usuario (longitud: ${email.trim().length} car.)")
                        Log.i("AccesoSeguro", "MEDIDA DE SEGURIDAD: La contraseña se procesa en memoria y NUNCA se registra en Logcat.")

                        if (!isFormValid || isLoading) {
                            Log.w("AccesoSeguro", "Validación local rechazada: Entradas no cumplen con los requisitos de seguridad.")
                            return@Button
                        }
                        Log.i("AccesoSeguro", "Validación local exitosa. Iniciando comprobación de credenciales...")

                        focusManager.clearFocus()
                        isLoading = true
                        errorMsg = ""

                        coroutineScope.launch {
                            if (authMode == 0) {
                                // Requisito 7: Mostrar mensaje de acceso simulado cuando los datos de prueba sean válidos
                                val isSimulatedUser = email.trim().equals("admin@seguro.com", ignoreCase = true) ||
                                        email.trim().equals("usuario_prueba", ignoreCase = true) ||
                                        email.trim().equals("estudiante@seguro.com", ignoreCase = true)
                                if (isSimulatedUser && (password == "Seguro1234#" || password == "12345678" || password == "Password123#")) {
                                    Log.i("AccesoSeguro", "Acceso simulado concedido para usuario de prueba. Credenciales de prueba válidas.")
                                    val simSession = SupabaseSession(
                                        accessToken = "simulated_secure_token_${System.currentTimeMillis()}",
                                        userId = "simulated-test-user-001",
                                        email = email.trim()
                                    )
                                    val simProfile = SupabaseProfile(
                                        id = "simulated-test-user-001",
                                        email = email.trim(),
                                        fullName = "Usuario de Prueba Seguro",
                                        roleId = "student",
                                        isActive = true,
                                        avatarUrl = null
                                    )
                                    sessionManager.saveSession(simSession, simProfile, rememberMe)
                                    isLoading = false
                                    Toast.makeText(context, "¡Acceso simulado concedido! Bienvenido al sistema seguro.", Toast.LENGTH_LONG).show()
                                    onLoginSuccess(simSession, simProfile)
                                    return@launch
                                }

                                // 1) Login real contra Supabase
                                val authResult = AuthRepository.signIn(email.trim(), password)
                                if (!authResult.isSuccess) {
                                    failedAttempts++
                                    Log.w("AccesoSeguro", "Fallo de autenticación (intento $failedAttempts).")
                                    if (failedAttempts >= 5) {
                                        lockoutSeconds = 30
                                        errorMsg = "Demasiados intentos fallidos. Intenta nuevamente en 30 segundos."
                                        coroutineScope.launch {
                                            while (lockoutSeconds > 0) {
                                                kotlinx.coroutines.delay(1000)
                                                lockoutSeconds--
                                                if (lockoutSeconds > 0) {
                                                    errorMsg = "Demasiados intentos fallidos. Intenta nuevamente en $lockoutSeconds segundos."
                                                } else {
                                                    errorMsg = ""
                                                    failedAttempts = 0
                                                }
                                            }
                                        }
                                    } else {
                                        errorMsg = mapAuthError(authResult.error ?: "")
                                    }
                                    isLoading = false
                                    return@launch
                                }
                                failedAttempts = 0
                                val session = authResult.data!!
                                Log.i("AccesoSeguro", "Autenticación exitosa. Credenciales verificadas sin fugas en logs.")

                                val profileResult = AuthRepository.loadProfile(session.userId, session.accessToken)
                                val profile = profileResult.data ?: SupabaseProfile(
                                    id = session.userId,
                                    email = session.email,
                                    fullName = email.substringBefore("@"),
                                    roleId = "student",
                                    isActive = true,
                                    avatarUrl = null
                                )

                                if (!profile.isActive) {
                                    errorMsg = "Acceso denegado. Comuníquese con el administrador."
                                    isLoading = false
                                    return@launch
                                }

                                sessionManager.saveSession(session, profile, rememberMe)
                                isLoading = false
                                Toast.makeText(context, "¡Acceso concedido! Bienvenido al sistema seguro.", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(session, profile)
                            } else {
                                // 2) Registro
                                val regResult = AuthRepository.signUp(email.trim(), password, fullName.trim())
                                if (!regResult.isSuccess) {
                                    errorMsg = mapAuthError(regResult.error ?: "")
                                    isLoading = false
                                    return@launch
                                }
                                val session = regResult.data!!

                                val profileResult = AuthRepository.loadProfile(session.userId, session.accessToken)
                                val profile = profileResult.data ?: SupabaseProfile(
                                    id = session.userId,
                                    email = session.email,
                                    fullName = fullName.trim(),
                                    roleId = "student",
                                    isActive = true,
                                    avatarUrl = null
                                )

                                sessionManager.saveSession(session, profile, true)
                                isLoading = false
                                Toast.makeText(context, "¡Cuenta creada exitosamente!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(session, profile)
                            }
                        }
                    },
                    enabled = !isLoading && lockoutSeconds == 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkillEmerald,
                        disabledContainerColor = SkillEmerald.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (authMode == 0) "Iniciando sesión..." else "Registrando...", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text(
                            text = if (authMode == 0) "Iniciar Sesión" else "Crear Cuenta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Alternar entre Login y Registro al pie
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (authMode == 0) "¿No tienes cuenta? " else "¿Ya tienes cuenta? ",
                        color = SkillTextMuted,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (authMode == 0) "Regístrate aquí" else "Inicia sesión",
                        color = SkillEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            authMode = if (authMode == 0) 1 else 0
                            errorMsg = ""
                        }
                    )
                }
            }
        }
    }
}

// Requisito 5: Mostrar mensajes de error genéricos, sin revelar datos sensibles
private fun mapAuthError(msg: String): String = when {
    msg.contains("Invalid login credentials") || msg.contains("invalid_credentials") ->
        "Usuario o contraseña incorrectos. Por favor, verifique sus datos."
    msg.contains("User already registered") || msg.contains("user_already_exists") ->
        "No se pudo completar el registro. Verifique sus datos o intente iniciar sesión."
    msg.contains("Network") || msg.contains("timeout") || msg.contains("Unable to resolve")
        || msg.contains("failed to connect") || msg.contains("Connection refused")
        || msg.contains("SSL") || msg.contains("SSLHandshake") || msg.contains("CertPath")
        || msg.contains("ECONNREFUSED") || msg.contains("SocketException") ->
        "Error de conexión con el servidor. Intente nuevamente más tarde."
    msg.contains("HTTP 4") ->
        "Usuario o contraseña incorrectos. Por favor, verifique sus datos."
    else ->
        "Error inesperado. Intente de nuevo."
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SkillEmerald,
    unfocusedBorderColor = SkillBorder,
    errorBorderColor = Color(0xFFC0392B),
    focusedLabelColor = SkillEmerald,
    cursorColor = SkillEmerald
)
