package com.skillacademy.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.data.remote.AuthRepository
import com.skillacademy.mobile.data.remote.SupabaseProfile
import com.skillacademy.mobile.data.remote.SupabaseSession
import com.skillacademy.mobile.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Entregable 2 — Pantalla de Inicio de Sesión
 *
 * Réplica funcional del login web (auth-login.html / auth-login.ts):
 *  ✔ Campo Email con validación de formato
 *  ✔ Campo Contraseña con validación mínima 6 caracteres
 *  ✔ Show/hide contraseña
 *  ✔ Recordarme (checkbox)
 *  ✔ ¿Olvidaste tu contraseña? (enlace)
 *  ✔ Spinner de carga durante el login
 *  ✔ Mensaje de error en tiempo real
 *  ✔ Redireccionamiento por rol (student / teacher / admin)
 *
 * Práctica 4 — también accesible después del login desde el perfil.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (session: SupabaseSession, profile: SupabaseProfile) -> Unit
) {
    val coroutineScope  = rememberCoroutineScope()
    val focusManager    = LocalFocusManager.current
    val passwordFocus   = remember { FocusRequester() }

    // ── Estado del formulario ─────────────────────────────────────────────────
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var rememberMe  by remember { mutableStateOf(false) }

    var emailTouched    by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }

    var showPassword by remember { mutableStateOf(false) }
    var isLoading    by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf("") }

    // ── Validaciones (idénticas al frontend web) ──────────────────────────────
    val emailError = when {
        emailTouched && email.isBlank()                    -> "Email es requerido"
        emailTouched && !email.contains("@")               -> "Email inválido"
        emailTouched && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email inválido"
        else -> ""
    }
    val passwordError = when {
        passwordTouched && password.isBlank()   -> "Contraseña es requerida"
        passwordTouched && password.length < 6  -> "Mínimo 6 caracteres"
        else -> ""
    }
    val formValid = emailError.isEmpty() && passwordError.isEmpty()
            && email.isNotBlank() && password.isNotBlank()

    // ── Fondo degradado esmeralda igual al web ────────────────────────────────
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
        // Orbes decorativas (equivalente a ::before / ::after del CSS)
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

        // ── Tarjeta de Login ──────────────────────────────────────────────────
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo / Marca
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SkillEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text       = "Iniciar sesión",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Black,
                    color      = SkillTextPrimary
                )
                Text(
                    text       = "Accede a tu cuenta SkillAcademy",
                    fontSize   = 13.sp,
                    color      = SkillTextMuted,
                    modifier   = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // ── Error global (igual que el div.error-message del HTML) ──
                if (errorMsg.isNotBlank()) {
                    Surface(
                        color  = Color(0xFFFEF2F2),
                        shape  = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text     = errorMsg,
                            color    = Color(0xFF8A2F21),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── Campo Email ───────────────────────────────────────────────
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Email", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkillTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it; emailTouched = true; errorMsg = "" },
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = { Text("correo@ejemplo.com", color = SkillTextMuted) },
                        leadingIcon   = { Icon(Icons.Default.Email, null, tint = SkillTextMuted) },
                        isError       = emailError.isNotEmpty(),
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                        shape  = RoundedCornerShape(10.dp),
                        colors = loginFieldColors()
                    )
                    if (emailError.isNotEmpty()) {
                        Text(emailError, color = Color(0xFFC0392B), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 3.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Campo Contraseña ──────────────────────────────────────────
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Contraseña", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkillTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value         = password,
                        onValueChange = { password = it; passwordTouched = true; errorMsg = "" },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocus),
                        placeholder   = { Text("Mínimo 6 caracteres", color = SkillTextMuted) },
                        leadingIcon   = { Icon(Icons.Default.Lock, null, tint = SkillTextMuted) },
                        trailingIcon  = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Ocultar" else "Mostrar",
                                    tint = SkillTextMuted
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        isError = passwordError.isNotEmpty(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape  = RoundedCornerShape(10.dp),
                        colors = loginFieldColors()
                    )
                    if (passwordError.isNotEmpty()) {
                        Text(passwordError, color = Color(0xFFC0392B), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 3.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Recordarme + ¿Olvidaste tu contraseña? ───────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked  = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors   = CheckboxDefaults.colors(checkedColor = SkillEmerald)
                    )
                    Text("Recordarme", fontSize = 13.sp, color = SkillTextSecondary,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {}) {
                        Text("¿Olvidaste tu contraseña?", color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── Botón Iniciar Sesión ───────────────────────────────────────
                Button(
                    onClick = {
                        emailTouched    = true
                        passwordTouched = true
                        if (!formValid || isLoading) return@Button
                        focusManager.clearFocus()
                        isLoading = true
                        errorMsg  = ""

                        coroutineScope.launch {
                            // 1) Login vía Supabase Auth (igual que signIn en auth.service.ts)
                            val authResult = AuthRepository.signIn(email.trim(), password)
                            if (!authResult.isSuccess) {
                                errorMsg  = mapAuthError(authResult.error ?: "")
                                isLoading = false
                                return@launch
                            }
                            val session = authResult.data!!

                            // 2) Cargar perfil con role_id (igual que loadProfile)
                            val profileResult = AuthRepository.loadProfile(session.userId, session.accessToken)
                            if (!profileResult.isSuccess) {
                                errorMsg  = "No se pudo cargar el perfil. Intenta de nuevo."
                                isLoading = false
                                return@launch
                            }
                            val profile = profileResult.data!!

                            // 3) Verificar cuenta activa
                            if (!profile.isActive) {
                                errorMsg  = "Tu cuenta está inactiva. Contacta al administrador."
                                isLoading = false
                                return@launch
                            }

                            isLoading = false
                            onLoginSuccess(session, profile)
                        }
                    },
                    enabled  = !isLoading,
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = SkillEmerald,
                        disabledContainerColor = SkillEmerald.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color  = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Entrando...", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text("Iniciar Sesión", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // ── Divisor ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SkillBorder)
                    Text("  o  ", color = SkillTextMuted, fontSize = 13.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SkillBorder)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Botón Google (deshabilitado, igual que en el web) ─────────
                OutlinedButton(
                    onClick  = {},
                    enabled  = false,
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    border   = ButtonDefaults.outlinedButtonBorder
                ) {
                    Text("G  Continuar con Google", fontWeight = FontWeight.Bold, color = SkillTextMuted)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── ¿No tienes cuenta? ────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("¿No tienes cuenta? ", color = SkillTextMuted, fontSize = 13.sp)
                    Text("Registrarse", color = SkillEmerald, fontWeight = FontWeight.Bold,
                        fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Convierte errores de Supabase Auth al mismo mensaje que usa el frontend web. */
private fun mapAuthError(msg: String): String = when {
    msg.contains("Invalid login credentials") || msg.contains("invalid_credentials") ->
        "Email o contraseña incorrectos"
    msg.contains("Email not confirmed") ->
        "Confirma tu email antes de entrar"
    msg.contains("account_disabled") || msg.contains("inactive") ->
        "Tu cuenta está inactiva. Contacta al administrador."
    else ->
        "Error al iniciar sesión. Intenta de nuevo."
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = SkillEmerald,
    unfocusedBorderColor = SkillBorder,
    errorBorderColor     = Color(0xFFC0392B),
    focusedLabelColor    = SkillEmerald,
    cursorColor          = SkillEmerald
)
