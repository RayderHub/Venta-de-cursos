package com.skillacademy.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillacademy.mobile.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Práctica 3: Consumo de una URL preasignada desde una aplicación móvil
 * Extensión ConsultaWeb integrada en SkillAcademy Mobile
 *
 * Cumple con los requerimientos funcionales:
 * RF01: Al iniciar muestra "Consulta de información"
 * RF02: Botón "CONSULTAR"
 * RF03: Petición HTTP a la URL preasignada
 * RF04: Visualización de información en pantalla
 * RF05: Indicador de carga "Consultando..."
 * RF06: Error de conexión "No fue posible obtener la información."
 */

// URL preasignada por defecto (Recurso JSON público de SkillAcademy / JSONPlaceholder)
const val URL_PREASIGNADA_DEFAULT = "https://raw.githubusercontent.com/RayderHub/Venta-de-cursos/main/backend-fastify/examples/respuesta_widget_ejemplo.json"
const val URL_PREASIGNADA_FALLBACK = "https://jsonplaceholder.typicode.com/posts/1"

sealed class ConsultaUiState {
    object Initial : ConsultaUiState()
    object Loading : ConsultaUiState()
    data class Success(
        val rawResponse: String,
        val formattedJson: String,
        val statusCode: Int
    ) : ConsultaUiState()
    data class Error(val errorMessage: String) : ConsultaUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultaWebScreen(
    onBackClick: () -> Unit
) {
    var urlInput by remember { mutableStateOf(URL_PREASIGNADA_DEFAULT) }
    var uiState by remember { mutableStateOf<ConsultaUiState>(ConsultaUiState.Initial) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ConsultaWeb",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Extensión Práctica 3 — URL Preasignada",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SkillNavy
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SkillBackground)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // RF01: Al iniciar la aplicación deberá mostrar: "Consulta de información"
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SkillSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Http,
                            contentDescription = "HTTP",
                            tint = SkillCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Consulta de información",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Consume datos JSON mediante peticiones HTTP a una URL preasignada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SkillTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Campo de URL Preasignada (Permite la Prueba 3: Modificación de la URL)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SkillSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "URL preasignada del recurso:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkillCyan,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Atajos rápidos para evaluación:",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkillTextMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { urlInput = URL_PREASIGNADA_DEFAULT },
                            label = { Text("SkillAcademy JSON", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        AssistChip(
                            onClick = { urlInput = URL_PREASIGNADA_FALLBACK },
                            label = { Text("JSONPlaceholder", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { urlInput = "https://servidor-invalido-o-inexistente.com/404" },
                            label = { Text("URL Inválida (Prueba 3/Error)", fontSize = 11.sp, color = SkillDiscountRed) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // RF02: Botón "CONSULTAR"
            Button(
                onClick = {
                    uiState = ConsultaUiState.Loading
                    coroutineScope.launch {
                        uiState = ejecutarPeticionHttp(urlInput)
                    }
                },
                enabled = uiState !is ConsultaUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = SkillEmerald),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CONSULTAR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contenedor de Respuestas y Estados
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SkillSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (val state = uiState) {
                        is ConsultaUiState.Initial -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = SkillTextMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Presione el botón CONSULTAR para iniciar la petición HTTP.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SkillTextMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        // RF05: Mientras se realiza la petición deberá mostrarse: Consultando...
                        is ConsultaUiState.Loading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = SkillCyan,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Consultando...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SkillNavy
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Estableciendo conexión HTTP...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SkillTextMuted
                                )
                            }
                        }

                        // RF04: La información obtenida deberá mostrarse en pantalla
                        is ConsultaUiState.Success -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Éxito",
                                        tint = SkillEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Respuesta Recibida (HTTP ${state.statusCode} OK)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SkillEmerald
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = state.formattedJson,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Color(0xFF38BDF8),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        // RF06: Si existe un error de conexión deberá mostrarse: No fue posible obtener la información.
                        is ConsultaUiState.Error -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = SkillDiscountRed,
                                    modifier = Modifier.size(46.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No fue posible obtener la información.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SkillDiscountRed,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SkillTextMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta informativa de la Actividad Principal y sus 3 Pruebas
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📋 Guía de Pruebas de la Práctica 3",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = SkillNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Prueba 1 (Con conexión): Presiona CONSULTAR con Wi-Fi/Datos activos para recibir el JSON.\n" +
                                "• Prueba 2 (Sin conexión): Activa Modo Avión y presiona CONSULTAR para comprobar el mensaje RF06.\n" +
                                "• Prueba 3 (Modificación): Modifica la URL manualmente o usa los botones de atajo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SkillTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Función suspendida que ejecuta la solicitud HTTP y procesa el JSON recibido en un hilo de E/S.
 */
private suspend fun ejecutarPeticionHttp(urlObjetivo: String): ConsultaUiState = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val url = URL(urlObjetivo.trim())
        connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SkillAcademy-Mobile/1.0")
        }

        val statusCode = connection.responseCode
        if (statusCode in 200..299) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val rawResponse = reader.use { it.readText() }

            // Procesamiento de JSON
            val formatted = try {
                val tokener = JSONTokener(rawResponse).nextValue()
                when (tokener) {
                    is JSONObject -> tokener.toString(4)
                    is JSONArray -> tokener.toString(4)
                    else -> rawResponse
                }
            } catch (e: Exception) {
                rawResponse
            }

            ConsultaUiState.Success(
                rawResponse = rawResponse,
                formattedJson = formatted,
                statusCode = statusCode
            )
        } else {
            // RF06
            ConsultaUiState.Error("Código HTTP: $statusCode (Recurso no encontrado o no disponible)")
        }
    } catch (e: Exception) {
        // RF06: Si existe un error de conexión
        ConsultaUiState.Error("Comprueba tu conexión de red o la disponibilidad del servidor.")
    } finally {
        connection?.disconnect()
    }
}
