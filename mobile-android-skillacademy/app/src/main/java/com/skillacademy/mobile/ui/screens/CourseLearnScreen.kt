package com.skillacademy.mobile.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.data.remote.*
import com.skillacademy.mobile.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseLearnScreen(
    curso: CourseUiModel,
    userId: String,
    accessToken: String,
    initialProgress: Int,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf("lecciones") }

    // ── Estado de Lecciones ───────────────────────────────────────────────────
    var lecciones by remember { mutableStateOf<List<com.skillacademy.mobile.data.model.LessonUiModel>>(emptyList()) }
    var isLoadingLecciones by remember { mutableStateOf(true) }
    var selectedLessonIndex by remember { mutableStateOf(0) }

    var contenidoActivo by remember { mutableStateOf<List<ContenidoLeccionUiModel>>(emptyList()) }
    var isLoadingContenido by remember { mutableStateOf(false) }

    // ── Estado de Tareas ──────────────────────────────────────────────────────
    var tareas by remember { mutableStateOf<List<TareaUiModel>>(emptyList()) }
    var isLoadingTareas by remember { mutableStateOf(true) }
    var selectedTareaIndex by remember { mutableStateOf(0) }

    var contenidoTareaActiva by remember { mutableStateOf<List<TareaContenidoUiModel>>(emptyList()) }
    var isLoadingTareaContenido by remember { mutableStateOf(false) }
    var resultadoTareaActiva by remember { mutableStateOf<TareaResultadoUiModel?>(null) }

    // Respuestas en curso para la tarea activa
    val respuestasOpcion = remember { mutableStateMapOf<Long, String>() }
    val respuestasTexto = remember { mutableStateMapOf<Long, String>() }
    var notaEntregaLibre by remember { mutableStateOf("") }
    var isSubmittingTarea by remember { mutableStateOf(false) }
    var showResultadoModal by remember { mutableStateOf(false) }
    var ultimoResultadoCalificacion by remember { mutableIntStateOf(0) }
    var ultimoResultadoAprobada by remember { mutableStateOf(false) }

    // ── Seguimiento de Avance Real ────────────────────────────────────────────
    val completedLessons = remember { mutableStateListOf<Long>() }
    val completedTareas = remember { mutableStateListOf<Long>() }
    var currentProgress by remember { mutableStateOf(initialProgress) }
    val flippedCards = remember { mutableStateListOf<Long>() }

    // 1. Cargar lecciones del curso y lecciones vistas del usuario desde Supabase
    LaunchedEffect(curso.id) {
        isLoadingLecciones = true
        val fetchedLessons = InscripcionesRepository.fetchLecciones(curso.id)
        lecciones = fetchedLessons
        isLoadingLecciones = false

        if (userId.isNotBlank() && accessToken.isNotBlank()) {
            val vistas = InscripcionesRepository.fetchLeccionesVistas(userId, accessToken)
            completedLessons.clear()
            fetchedLessons.forEach { l ->
                if (vistas.contains(l.id)) {
                    completedLessons.add(l.id)
                }
            }
        }

        if (fetchedLessons.isNotEmpty()) {
            isLoadingContenido = true
            contenidoActivo = InscripcionesRepository.fetchContenidoLeccion(fetchedLessons[0].id)
            isLoadingContenido = false
        }
    }

    // 2. Cargar tareas reales asignadas al curso y tareas ya aprobadas/entregadas
    LaunchedEffect(curso.id) {
        isLoadingTareas = true
        val fetchedTareas = InscripcionesRepository.fetchTareas(curso.id, curso.titulo, accessToken)
        tareas = fetchedTareas
        isLoadingTareas = false

        if (userId.isNotBlank() && accessToken.isNotBlank()) {
            val aprobadas = InscripcionesRepository.fetchTareasAprobadas(userId, accessToken)
            completedTareas.clear()
            fetchedTareas.forEach { t ->
                if (aprobadas.contains(t.id)) {
                    completedTareas.add(t.id)
                }
            }
        }
    }

    // Cargar contenido interactivo de la tarea seleccionada
    LaunchedEffect(selectedTareaIndex, tareas) {
        val activeTarea = tareas.getOrNull(selectedTareaIndex)
        if (activeTarea != null) {
            isLoadingTareaContenido = true
            respuestasOpcion.clear()
            respuestasTexto.clear()
            showResultadoModal = false
            notaEntregaLibre = ""

            val contenidos = InscripcionesRepository.fetchTareaContenido(activeTarea.id)
            contenidoTareaActiva = contenidos

            if (userId.isNotBlank() && accessToken.isNotBlank()) {
                val resultado = InscripcionesRepository.fetchTareaResultado(userId, activeTarea.id, accessToken)
                resultadoTareaActiva = resultado
                if (resultado != null && resultado.aprobada && !completedTareas.contains(activeTarea.id)) {
                    completedTareas.add(activeTarea.id)
                }
            }
            isLoadingTareaContenido = false
        }
    }

    // 3. Recalcular progreso automático
    LaunchedEffect(completedLessons.size, completedTareas.size, lecciones.size, tareas.size) {
        val totalItems = lecciones.size + tareas.size
        if (totalItems > 0) {
            val completados = completedLessons.size + completedTareas.size
            val calc = ((completados.toFloat() / totalItems.toFloat()) * 100).toInt().coerceIn(0, 100)
            currentProgress = maxOf(currentProgress, calc)
        }
    }

    fun recalcularProgreso() {
        val totalItems = lecciones.size + tareas.size
        if (totalItems == 0) return
        val completados = completedLessons.size + completedTareas.size
        val nuevo = ((completados.toFloat() / totalItems.toFloat()) * 100).toInt().coerceIn(0, 100)
        currentProgress = nuevo
        coroutineScope.launch {
            InscripcionesRepository.actualizarProgreso(userId, curso.id, nuevo, accessToken)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = curso.titulo,
                            maxLines = 1,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aula Virtual • $currentProgress% completado",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SkillNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SkillBackground)
                .padding(paddingValues)
        ) {
            // Barra de progreso general
            Surface(
                color = SkillSurface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tu progreso en el curso",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SkillTextPrimary
                        )
                        Text(
                            text = "$currentProgress%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SkillEmerald
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentProgress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SkillEmerald,
                        trackColor = SkillEmerald.copy(alpha = 0.15f)
                    )
                }
            }

            // Pestañas de navegación interna
            TabRow(
                selectedTabIndex = if (activeTab == "lecciones") 0 else 1,
                containerColor = SkillSurface,
                contentColor = SkillCyan
            ) {
                Tab(
                    selected = activeTab == "lecciones",
                    onClick = { activeTab = "lecciones" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LECCIONES (${lecciones.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = activeTab == "tareas",
                    onClick = { activeTab = "tareas" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TAREAS (${tareas.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )
            }

            // ═════════════════════════════════════════════════════════════════════
            // PESTAÑA 1: LECCIONES Y CONTENIDO INTERACTIVO
            // ═════════════════════════════════════════════════════════════════════
            if (activeTab == "lecciones") {
                when {
                    isLoadingLecciones -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SkillCyan)
                    }
                    lecciones.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = SkillTextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Este curso aún no tiene lecciones publicadas.", color = SkillTextMuted, textAlign = TextAlign.Center)
                        }
                    }
                    else -> {
                        val activeLesson = lecciones.getOrNull(selectedLessonIndex) ?: lecciones.first()
                        val isCompleted = completedLessons.contains(activeLesson.id)

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Visor principal de la lección seleccionada
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SkillSurface),
                                    elevation = CardDefaults.cardElevation(3.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = SkillCyan.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    "LECCIÓN ${activeLesson.orden}",
                                                    color = SkillCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            if (activeLesson.duracionMinutos > 0) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = SkillTextMuted, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("${activeLesson.duracionMinutos} min", fontSize = 12.sp, color = SkillTextMuted)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = activeLesson.titulo,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SkillTextPrimary
                                        )

                                        if (activeLesson.descripcion.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = activeLesson.descripcion,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SkillTextMuted
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(color = SkillTextMuted.copy(alpha = 0.15f))
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Renderizado de bloques de contenido reales
                                        if (isLoadingContenido) {
                                            Box(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(color = SkillCyan, modifier = Modifier.size(32.dp))
                                            }
                                        } else if (contenidoActivo.isEmpty()) {
                                            Text(
                                                "Esta lección aún no tiene contenido publicado.",
                                                color = SkillTextMuted,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                            )
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                                contenidoActivo.forEach { bloque ->
                                                    when (bloque.tipo) {
                                                        "titulo" -> Text(
                                                            text = bloque.contenido,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SkillTextPrimary
                                                        )
                                                        "texto" -> Text(
                                                            text = bloque.contenido,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = SkillTextSecondary,
                                                            lineHeight = 22.sp
                                                        )
                                                        "imagen" -> {
                                                            val resolvedImage = resolveImageModel(bloque.contenido)
                                                            if (resolvedImage != null) {
                                                                Card(
                                                                    shape = RoundedCornerShape(12.dp),
                                                                    border = BorderStroke(1.dp, SkillCyan.copy(alpha = 0.2f)),
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    AsyncImage(
                                                                        model = resolvedImage,
                                                                        contentDescription = "Imagen de la lección",
                                                                        contentScale = ContentScale.Fit,
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .heightIn(min = 180.dp, max = 320.dp)
                                                                            .background(Color.Black.copy(alpha = 0.05f))
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        "tarjeta" -> {
                                                            val isFlipped = flippedCards.contains(bloque.id)
                                                            val tarjetaData = parseTarjetaData(bloque.contenido)
                                                            val resolvedTarjetaImg = resolveImageModel(tarjetaData.imagen)

                                                            Card(
                                                                shape = RoundedCornerShape(14.dp),
                                                                colors = CardDefaults.cardColors(
                                                                    containerColor = if (isFlipped) SkillNavy else SkillCyan.copy(alpha = 0.08f)
                                                                ),
                                                                border = BorderStroke(
                                                                    1.5.dp,
                                                                    if (isFlipped) SkillEmerald else SkillCyan.copy(alpha = 0.5f)
                                                                ),
                                                                elevation = CardDefaults.cardElevation(if (isFlipped) 4.dp else 2.dp),
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .animateContentSize()
                                                                    .clickable {
                                                                        if (isFlipped) flippedCards.remove(bloque.id)
                                                                        else flippedCards.add(bloque.id)
                                                                    }
                                                            ) {
                                                                Column(modifier = Modifier.padding(16.dp)) {
                                                                    if (!isFlipped) {
                                                                        // Cara Frontal de la Tarjeta
                                                                        Row(
                                                                            modifier = Modifier.fillMaxWidth(),
                                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Surface(
                                                                                shape = RoundedCornerShape(6.dp),
                                                                                color = SkillCyan.copy(alpha = 0.2f)
                                                                            ) {
                                                                                Text(
                                                                                    "TARJETA INTERACTIVA",
                                                                                    color = SkillCyan,
                                                                                    fontWeight = FontWeight.Bold,
                                                                                    fontSize = 10.sp,
                                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                                                )
                                                                            }
                                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                                Icon(
                                                                                    Icons.Default.FlipToBack,
                                                                                    contentDescription = null,
                                                                                    tint = SkillCyan,
                                                                                    modifier = Modifier.size(16.dp)
                                                                                )
                                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                                Text("Toca para voltear", fontSize = 11.sp, color = SkillCyan)
                                                                            }
                                                                        }

                                                                        if (resolvedTarjetaImg != null) {
                                                                            Spacer(modifier = Modifier.height(10.dp))
                                                                            AsyncImage(
                                                                                model = resolvedTarjetaImg,
                                                                                contentDescription = null,
                                                                                contentScale = ContentScale.Crop,
                                                                                modifier = Modifier
                                                                                    .fillMaxWidth()
                                                                                    .height(160.dp)
                                                                                    .clip(RoundedCornerShape(8.dp))
                                                                            )
                                                                        }

                                                                        Spacer(modifier = Modifier.height(10.dp))
                                                                        Text(
                                                                            text = tarjetaData.titulo.ifBlank { "Concepto Clave" },
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = SkillTextPrimary,
                                                                            fontSize = 15.sp
                                                                        )
                                                                    } else {
                                                                        // Cara Trasera de la Tarjeta
                                                                        Row(
                                                                            modifier = Modifier.fillMaxWidth(),
                                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Surface(
                                                                                shape = RoundedCornerShape(6.dp),
                                                                                color = SkillEmerald.copy(alpha = 0.2f)
                                                                            ) {
                                                                                Text(
                                                                                    "RESPUESTA / EXPLICACIÓN",
                                                                                    color = SkillEmerald,
                                                                                    fontWeight = FontWeight.Bold,
                                                                                    fontSize = 10.sp,
                                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                                                )
                                                                            }
                                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                                Icon(
                                                                                    Icons.Default.FlipToFront,
                                                                                    contentDescription = null,
                                                                                    tint = Color.White.copy(alpha = 0.8f),
                                                                                    modifier = Modifier.size(16.dp)
                                                                                )
                                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                                Text("Toca para volver", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                                                            }
                                                                        }

                                                                        Spacer(modifier = Modifier.height(12.dp))
                                                                        Text(
                                                                            text = tarjetaData.descripcion.ifBlank { bloque.contenido },
                                                                            style = MaterialTheme.typography.bodyMedium,
                                                                            color = Color.White,
                                                                            lineHeight = 22.sp
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        else -> if (bloque.contenido.isNotBlank()) {
                                                            Text(text = bloque.contenido, style = MaterialTheme.typography.bodyMedium, color = SkillTextSecondary)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                val willComplete = !isCompleted
                                                if (willComplete) {
                                                    completedLessons.add(activeLesson.id)
                                                    Toast.makeText(context, "¡Lección completada!", Toast.LENGTH_SHORT).show()
                                                    coroutineScope.launch {
                                                        InscripcionesRepository.marcarLeccionVista(userId, activeLesson.id, accessToken)
                                                    }
                                                } else {
                                                    completedLessons.remove(activeLesson.id)
                                                    Toast.makeText(context, "Lección desmarcada", Toast.LENGTH_SHORT).show()
                                                    coroutineScope.launch {
                                                        InscripcionesRepository.desmarcarLeccionVista(userId, activeLesson.id, accessToken)
                                                    }
                                                }
                                                recalcularProgreso()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isCompleted) SkillEmerald else SkillNavy
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isCompleted) "Completada" else "Marcar como completada",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    "Temario del Curso",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SkillTextPrimary
                                )
                            }

                            itemsIndexed(lecciones) { index, leccion ->
                                val isItemCompleted = completedLessons.contains(leccion.id)
                                val isSelected = index == selectedLessonIndex
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) SkillCyan.copy(alpha = 0.08f) else SkillSurface,
                                    border = if (isSelected) BorderStroke(1.5.dp, SkillCyan) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLessonIndex = index
                                            coroutineScope.launch {
                                                isLoadingContenido = true
                                                contenidoActivo = InscripcionesRepository.fetchContenidoLeccion(leccion.id)
                                                isLoadingContenido = false
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isItemCompleted) SkillEmerald.copy(alpha = 0.2f)
                                                    else SkillNavy.copy(alpha = 0.1f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isItemCompleted) {
                                                Icon(Icons.Default.Check, contentDescription = "Completada", tint = SkillEmerald, modifier = Modifier.size(18.dp))
                                            } else {
                                                Text("${leccion.orden}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SkillNavy)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = leccion.titulo, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp, color = SkillTextPrimary)
                                            if (leccion.duracionMinutos > 0) {
                                                Text("${leccion.duracionMinutos} min", fontSize = 11.sp, color = SkillTextMuted)
                                            }
                                        }
                                        if (leccion.esGratis) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = SkillEmerald.copy(alpha = 0.15f)
                                            ) {
                                                Text("GRATIS", color = SkillEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = SkillCyan, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ═════════════════════════════════════════════════════════════════
                // PESTAÑA 2: TAREAS INTERACTIVAS (REALIZACIÓN Y CALIFICACIÓN REAL)
                // ═════════════════════════════════════════════════════════════════
                when {
                    isLoadingTareas -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SkillCyan)
                    }
                    tareas.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Default.AssignmentLate, contentDescription = null, tint = SkillTextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No hay tareas asignadas para este curso.", color = SkillTextMuted, textAlign = TextAlign.Center)
                        }
                    }
                    else -> {
                        val activeTarea = tareas.getOrNull(selectedTareaIndex) ?: tareas.first()
                        val isTareaDone = completedTareas.contains(activeTarea.id)

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Tarjeta principal interactiva para resolver la tarea
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SkillSurface),
                                    elevation = CardDefaults.cardElevation(3.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        // Encabezado de la Tarea
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    "MIN. APROBATORIO: ${activeTarea.ponderacion}%",
                                                    color = Color(0xFF8B5CF6),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isTareaDone) SkillEmerald.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
                                            ) {
                                                Text(
                                                    if (isTareaDone) "APROBADA / ENTREGADA" else "PENDIENTE",
                                                    color = if (isTareaDone) SkillEmerald else SkillTextMuted,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = activeTarea.titulo,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SkillTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = activeTarea.descripcion,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SkillTextMuted,
                                            lineHeight = 20.sp
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider(color = SkillTextMuted.copy(alpha = 0.15f))
                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Banner de calificación previa si ya se había resuelto
                                        if (resultadoTareaActiva != null && isTareaDone) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = SkillEmerald.copy(alpha = 0.12f),
                                                border = BorderStroke(1.dp, SkillEmerald.copy(alpha = 0.3f)),
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SkillEmerald)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text("¡Ya completaste esta tarea!", fontWeight = FontWeight.Bold, color = SkillEmerald, fontSize = 13.sp)
                                                        Text(
                                                            "Calificación obtenida: ${resultadoTareaActiva?.calificacion}% (Mínimo requerido: ${activeTarea.ponderacion}%)",
                                                            fontSize = 12.sp,
                                                            color = SkillTextPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Formulario dinámico con las preguntas reales de tarea_contenido
                                        if (isLoadingTareaContenido) {
                                            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = Color(0xFF8B5CF6), modifier = Modifier.size(30.dp))
                                            }
                                        } else if (contenidoTareaActiva.isNotEmpty()) {
                                            Text(
                                                "Preguntas y Ejercicios de la Tarea:",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SkillTextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                contenidoTareaActiva.forEachIndexed { qIdx, itemContenido ->
                                                    when (itemContenido.tipo) {
                                                        "titulo" -> {
                                                            Text(
                                                                text = itemContenido.contenido,
                                                                fontWeight = FontWeight.Bold,
                                                                color = SkillTextPrimary,
                                                                fontSize = 15.sp
                                                            )
                                                        }
                                                        "texto" -> {
                                                            Text(
                                                                text = itemContenido.contenido,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = SkillTextSecondary
                                                            )
                                                        }
                                                        "opcion_multiple" -> {
                                                            val qData = parseOpcionMultiple(itemContenido.contenido)
                                                            val selectedOption = respuestasOpcion[itemContenido.id]

                                                            Column(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                                                                    .padding(12.dp)
                                                            ) {
                                                                Text(
                                                                    text = "${qIdx + 1}. ${qData.titulo}",
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    fontSize = 14.sp,
                                                                    color = SkillTextPrimary
                                                                )
                                                                Spacer(modifier = Modifier.height(8.dp))

                                                                qData.opciones.forEach { opcion ->
                                                                    val isSelected = selectedOption == opcion
                                                                    Surface(
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.15f) else SkillSurface,
                                                                        border = BorderStroke(
                                                                            1.dp,
                                                                            if (isSelected) Color(0xFF8B5CF6) else SkillTextMuted.copy(alpha = 0.2f)
                                                                        ),
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .padding(vertical = 4.dp)
                                                                            .clickable {
                                                                                respuestasOpcion[itemContenido.id] = opcion
                                                                            }
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(10.dp),
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            RadioButton(
                                                                                selected = isSelected,
                                                                                onClick = { respuestasOpcion[itemContenido.id] = opcion },
                                                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF8B5CF6))
                                                                            )
                                                                            Spacer(modifier = Modifier.width(8.dp))
                                                                            Text(text = opcion, fontSize = 13.sp, color = SkillTextPrimary)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        "respuesta_texto" -> {
                                                            val currentTxt = respuestasTexto[itemContenido.id] ?: ""
                                                            Column(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                                                                    .padding(12.dp)
                                                            ) {
                                                                Text(
                                                                    text = "${qIdx + 1}. ${itemContenido.contenido}",
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    fontSize = 14.sp,
                                                                    color = SkillTextPrimary
                                                                )
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                OutlinedTextField(
                                                                    value = currentTxt,
                                                                    onValueChange = { respuestasTexto[itemContenido.id] = it },
                                                                    placeholder = { Text("Escribe tu respuesta aquí...", fontSize = 13.sp) },
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    minLines = 3,
                                                                    shape = RoundedCornerShape(8.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Botón para calificar y enviar la tarea interactiva
                                            Button(
                                                onClick = {
                                                    isSubmittingTarea = true
                                                    coroutineScope.launch {
                                                        // Evaluar respuestas
                                                        val preguntas = contenidoTareaActiva.filter {
                                                            it.tipo == "opcion_multiple" || it.tipo == "respuesta_texto"
                                                        }
                                                        var aciertos = 0
                                                        preguntas.forEach { p ->
                                                            if (p.tipo == "respuesta_texto") {
                                                                val txt = respuestasTexto[p.id]?.trim() ?: ""
                                                                if (txt.isNotEmpty()) aciertos++
                                                            } else {
                                                                val qData = parseOpcionMultiple(p.contenido)
                                                                val selected = respuestasOpcion[p.id]
                                                                if (selected != null && qData.respuestaCorrecta.isNotBlank() &&
                                                                    selected.trim().equals(qData.respuestaCorrecta.trim(), ignoreCase = true)
                                                                ) {
                                                                    aciertos++
                                                                }
                                                            }
                                                        }

                                                        val calificacion = if (preguntas.isNotEmpty()) {
                                                            ((aciertos.toFloat() / preguntas.size.toFloat()) * 100).toInt()
                                                        } else 100

                                                        val aprobada = calificacion >= activeTarea.ponderacion
                                                        ultimoResultadoCalificacion = calificacion
                                                        ultimoResultadoAprobada = aprobada

                                                        val respuestasJson = JSONObject().apply {
                                                            val opcionesJson = JSONObject()
                                                            respuestasOpcion.forEach { (k, v) -> opcionesJson.put(k.toString(), v) }
                                                            put("opciones", opcionesJson)
                                                            val textosJson = JSONObject()
                                                            respuestasTexto.forEach { (k, v) -> textosJson.put(k.toString(), v) }
                                                            put("textos", textosJson)
                                                        }

                                                        InscripcionesRepository.guardarTareaResultado(
                                                            userId = userId,
                                                            tareaId = activeTarea.id,
                                                            aprobada = aprobada,
                                                            calificacion = calificacion,
                                                            respuestas = respuestasJson,
                                                            accessToken = accessToken
                                                        )

                                                        if (aprobada) {
                                                            if (!completedTareas.contains(activeTarea.id)) {
                                                                completedTareas.add(activeTarea.id)
                                                            }
                                                        } else {
                                                            completedTareas.remove(activeTarea.id)
                                                        }

                                                        resultadoTareaActiva = TareaResultadoUiModel(
                                                            id = 0,
                                                            usuarioId = userId,
                                                            tareaId = activeTarea.id,
                                                            calificacion = calificacion,
                                                            aprobada = aprobada
                                                        )

                                                        recalcularProgreso()
                                                        isSubmittingTarea = false
                                                        showResultadoModal = true
                                                    }
                                                },
                                                enabled = !isSubmittingTarea,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth().height(48.dp)
                                            ) {
                                                if (isSubmittingTarea) {
                                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Calificando respuestas...")
                                                } else {
                                                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Enviar y Calificar Tarea", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        } else {
                                            // Tarea sin preguntas interactivas (entrega de proyecto o lectura)
                                            Text(
                                                "Esta tarea no cuenta con cuestionario interactivo. Puedes ingresar tus notas de entrega o marcarla como completada:",
                                                fontSize = 13.sp,
                                                color = SkillTextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedTextField(
                                                value = notaEntregaLibre,
                                                onValueChange = { notaEntregaLibre = it },
                                                placeholder = { Text("Notas de tu entrega o enlace de proyecto...") },
                                                modifier = Modifier.fillMaxWidth(),
                                                minLines = 3,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Button(
                                                onClick = {
                                                    val willDone = !isTareaDone
                                                    if (willDone) {
                                                        completedTareas.add(activeTarea.id)
                                                        Toast.makeText(context, "¡Tarea entregada exitosamente!", Toast.LENGTH_SHORT).show()
                                                        coroutineScope.launch {
                                                            val respObj = JSONObject().apply { put("nota", notaEntregaLibre) }
                                                            InscripcionesRepository.guardarTareaResultado(
                                                                userId, activeTarea.id, true, 100, respObj, accessToken
                                                            )
                                                        }
                                                    } else {
                                                        completedTareas.remove(activeTarea.id)
                                                        Toast.makeText(context, "Tarea desmarcada", Toast.LENGTH_SHORT).show()
                                                        coroutineScope.launch {
                                                            InscripcionesRepository.eliminarTareaResultado(userId, activeTarea.id, accessToken)
                                                        }
                                                    }
                                                    recalcularProgreso()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isTareaDone) SkillEmerald else Color(0xFF8B5CF6)
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth().height(44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isTareaDone) Icons.Default.CheckCircle else Icons.Default.Upload,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if (isTareaDone) "Tarea Entregada" else "Entregar Tarea", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    "Lista de Tareas y Evaluaciones",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SkillTextPrimary
                                )
                            }

                            itemsIndexed(tareas) { index, tarea ->
                                val isDone = completedTareas.contains(tarea.id)
                                val isSelected = index == selectedTareaIndex
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.08f) else SkillSurface,
                                    border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF8B5CF6)) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedTareaIndex = index }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isDone) SkillEmerald.copy(alpha = 0.2f)
                                                    else Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isDone) Icons.Default.Check else Icons.AutoMirrored.Filled.Assignment,
                                                contentDescription = null,
                                                tint = if (isDone) SkillEmerald else Color(0xFF8B5CF6),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tarea.titulo,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = SkillTextPrimary
                                            )
                                            Text(
                                                "${tarea.ponderacion}% para aprobar",
                                                fontSize = 11.sp,
                                                color = SkillTextMuted
                                            )
                                        }
                                        if (isDone) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = SkillEmerald.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    "LISTO",
                                                    color = SkillEmerald,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Calificación al Terminar la Tarea
    if (showResultadoModal) {
        val activeTarea = tareas.getOrNull(selectedTareaIndex)
        AlertDialog(
            onDismissRequest = { showResultadoModal = false },
            icon = {
                Icon(
                    imageVector = if (ultimoResultadoAprobada) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (ultimoResultadoAprobada) SkillEmerald else Color(0xFFEF4444),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = if (ultimoResultadoAprobada) "¡Tarea Aprobada! 🎉" else "Tarea No Aprobada",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Tu calificación fue de $ultimoResultadoCalificacion%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ultimoResultadoAprobada) SkillEmerald else Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (ultimoResultadoAprobada) {
                            "Cumpliste con el mínimo requerido de ${activeTarea?.ponderacion ?: 60}%. Tu progreso en el curso ha aumentado."
                        } else {
                            "El mínimo requerido para aprobar es de ${activeTarea?.ponderacion ?: 60}%. Puedes revisar el material e intentarlo de nuevo."
                        },
                        fontSize = 13.sp,
                        color = SkillTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showResultadoModal = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (ultimoResultadoAprobada) SkillEmerald else SkillNavy
                    )
                ) {
                    Text(if (ultimoResultadoAprobada) "Continuar" else "Reintentar")
                }
            }
        )
    }
}

private data class TarjetaData(val titulo: String, val descripcion: String, val imagen: String)

private fun parseTarjetaData(contenido: String): TarjetaData {
    return try {
        val obj = JSONObject(contenido)
        TarjetaData(
            titulo = obj.optString("titulo", ""),
            descripcion = obj.optString("descripcion", ""),
            imagen = obj.optString("imagen", obj.optString("url", ""))
        )
    } catch (_: Exception) {
        TarjetaData(titulo = "", descripcion = contenido, imagen = "")
    }
}

private data class OpcionMultipleData(val titulo: String, val opciones: List<String>, val respuestaCorrecta: String)

private fun parseOpcionMultiple(contenido: String): OpcionMultipleData {
    return try {
        val obj = JSONObject(contenido)
        val arr = obj.optJSONArray("opciones")
        val opciones = mutableListOf<String>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val op = arr.optString(i, "")
                if (op.isNotBlank()) opciones.add(op)
            }
        }
        OpcionMultipleData(
            titulo = obj.optString("titulo", "Pregunta de opción múltiple"),
            opciones = opciones,
            respuestaCorrecta = obj.optString("respuesta_correcta", "")
        )
    } catch (_: Exception) {
        OpcionMultipleData(titulo = contenido, opciones = emptyList(), respuestaCorrecta = "")
    }
}
