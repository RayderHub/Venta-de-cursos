package com.skillacademy.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.skillacademy.mobile.ui.screens.MainAppScreen
import com.skillacademy.mobile.ui.theme.SkillAcademyTheme

/**
 * =========================================================================================
 * PRÁCTICA 5: Prácticas de código seguro en aplicaciones móviles (AccesoSeguro)
 * =========================================================================================
 * 
 * OBJETIVO:
 * Implementar y comprobar medidas básicas de seguridad en la aplicación móvil Android,
 * mediante la validación estricta de entradas, manejo seguro de credenciales en memoria,
 * prevención de fuga de información confidencial en Logcat y mensajes de error genéricos.
 * 
 * REGLAS Y VALIDACIONES DE SEGURIDAD IMPLEMENTADAS EN LA APLICACIÓN:
 * 
 * 1. Campos de Entrada (Usuario y Contraseña):
 *    - Presentación de campos dedicados con controles de foco e interfaz reactiva (Jetpack Compose).
 * 
 * 2. Validación de Contenido Obligatorio y Prevención de Bypass de Espacios:
 *    - Se valida que ningún campo esté vacío.
 *    - Se rechazan entradas compuestas únicamente por espacios en blanco mediante trim().isEmpty().
 * 
 * 3. Restricción y Rechazo por Longitud Definida (Defensa en Profundidad):
 *    - Campo Usuario / Email: Límite máximo definido de 50 caracteres (MAX_USER_LENGTH = 50).
 *      Evita desbordamientos lógicos, inyecciones y saturación de búferes o memoria.
 *    - Campo Contraseña: Longitud mínima obligatoria de 8 caracteres (MIN_PASSWORD_LENGTH = 8)
 *      y longitud máxima de 32 caracteres (MAX_PASSWORD_LENGTH = 32).
 *      Se previene el uso de contraseñas cortas o triviales vulnerables a fuerza bruta.
 * 
 * 4. Ocultamiento Visual de Credenciales:
 *    - Uso de PasswordVisualTransformation() para enmascarar los caracteres en pantalla,
 *      mitigando riesgos de hombros curiosos (shoulder surfing).
 * 
 * 5. Mensajes de Error Genéricos (Prevención de Enumeración):
 *    - En caso de credenciales inválidas, el sistema emite respuestas neutras como
 *      "Usuario o contraseña incorrectos", evitando revelar si el usuario existe o no
 *      en la base de datos (OWASP A07: Identification and Authentication Failures).
 * 
 * 6. Prevención de Fuga de Credenciales en Registros (Logcat):
 *    - Se audita el evento con el tag "AccesoSeguro" indicando longitudes o identificadores
 *      parcialmente anonimizados, garantizando que contraseñas en texto plano y tokens
 *      NUNCA se impriman en Logcat (OWASP MASVS-STORAGE / Sensitive Data Exposure).
 * 
 * 7. Acceso Simulado y Verificación de Credenciales de Prueba:
 *    - Reconocimiento seguro de cuentas de prueba (ej. usuario_prueba / admin@seguro.com)
 *      con retroalimentación visual de "¡Acceso simulado concedido!" ante credenciales válidas.
 * =========================================================================================
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SkillAcademyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen()
                }
            }
        }
    }
}

