package com.skillacademy.mobile.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillacademy.mobile.data.local.SessionManager
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.data.remote.EnrolledCourse
import com.skillacademy.mobile.data.remote.InscripcionesRepository
import com.skillacademy.mobile.data.remote.SupabaseProfile
import com.skillacademy.mobile.data.remote.SupabaseSession
import com.skillacademy.mobile.data.repository.CartRepository
import com.skillacademy.mobile.ui.components.PaymentGatewayBottomSheet
import com.skillacademy.mobile.ui.components.SkillBottomBar
import com.skillacademy.mobile.ui.components.SkillTopBar
import com.skillacademy.mobile.ui.theme.*
import com.skillacademy.mobile.ui.viewmodel.CartViewModel
import com.skillacademy.mobile.ui.viewmodel.CartViewModelFactory
import com.skillacademy.mobile.ui.viewmodel.CoursesViewModel
import com.skillacademy.mobile.util.SkillNotificationHelper
import kotlinx.coroutines.launch

/**
 * MainAppScreen — Entregable 2 & Prácticas 5/6
 *
 * Orquesta toda la aplicación móvil de SkillAcademy:
 *  ✔ Catálogo y Detalle con portadas reales subidas por profesores (Base64 / URL / Tema)
 *  ✔ Protección de rutas ("Mis cursos" y "Perfil"): login obligatorio sin fugas
 *  ✔ Al cancelar login (atrás), regresa con seguridad a Inicio/Catálogo sin mostrar datos falsos
 *  ✔ "Mis Cursos" 100% real desde Supabase (`inscripciones`)
 *  ✔ Aula virtual interactiva con lecciones y tareas reales
 *  ✔ Perfil de usuario editable (nombre, foto, bio) sincronizado con Supabase
 *  ✔ Carrito persistente SQLite y checkout conectado a inscripciones reales
 */
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Inicializar repositorios locales
    val sessionManager = remember { SessionManager(context) }
    val cartRepository = remember { CartRepository(context) }

    // ViewModels
    val coursesViewModel: CoursesViewModel = viewModel()
    val cartViewModel: CartViewModel = viewModel(factory = CartViewModelFactory(cartRepository))

    // ── Estado de sesión (restauración inmediata de persistencia) ───────────
    val initialSaved = remember { sessionManager.getSession() }
    var session by remember { mutableStateOf(initialSaved?.first) }
    var profile by remember { mutableStateOf(initialSaved?.second) }

    // ── Estado de cursos inscritos (inicializado desde caché local) ─────────
    var enrolledCourses by remember { mutableStateOf(sessionManager.getCachedCourses()) }
    var isLoadingEnrolled by remember { mutableStateOf(false) }

    // Función para recargar cursos inscritos desde Supabase
    fun reloadEnrolledCourses() {
        val sess = session ?: return
        isLoadingEnrolled = true
        coroutineScope.launch {
            enrolledCourses = InscripcionesRepository.fetchMisCursos(
                userId = sess.userId,
                accessToken = sess.accessToken,
                refreshToken = sess.refreshToken,
                sessionManager = sessionManager
            )
            isLoadingEnrolled = false
        }
    }

    // Recargar inscripciones cada vez que haya una sesión activa
    LaunchedEffect(session) {
        if (session != null) {
            reloadEnrolledCourses()
        } else {
            enrolledCourses = emptyList()
        }
    }

    // ── Navegación ───────────────────────────────────────────────────────────
    var currentScreen by remember { mutableStateOf("home") }
    var previousScreen by remember { mutableStateOf("home") }
    var authRedirectTarget by remember { mutableStateOf<String?>(null) }

    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var selectedCatalogCategory by remember { mutableStateOf<String?>(null) }

    var learningCourse by remember { mutableStateOf<CourseUiModel?>(null) }
    var learningCourseProgress by remember { mutableIntStateOf(0) }

    var showCheckoutSuccessDialog by remember { mutableStateOf(false) }
    var showPaymentGatewaySheet by remember { mutableStateOf(false) }
    var lastPaidAmount by remember { mutableDoubleStateOf(0.0) }
    var lastOrderNumber by remember { mutableStateOf("") }
    var lastPaymentMethod by remember { mutableStateOf("") }

    // ── Datos del ViewModel (live desde Supabase) ────────────────────────────
    val courses by coursesViewModel.courses.collectAsState()
    val banners by coursesViewModel.banners.collectAsState()
    val categories by coursesViewModel.categories.collectAsState()
    val isLoading by coursesViewModel.isLoading.collectAsState()
    val networkError by coursesViewModel.networkError.collectAsState()
    val cartItems by cartViewModel.cartItems.collectAsState()
    val financialSummary by cartViewModel.financialSummary.collectAsState()
    val selectedCourse by coursesViewModel.selectedCourse.collectAsState()
    val isLoadingDetail by coursesViewModel.isLoadingDetail.collectAsState()

    // ── Manejo global del botón atrás del dispositivo ───────────────────────
    BackHandler(enabled = currentScreen != "home") {
        when (currentScreen) {
            "course_learn" -> {
                learningCourse = null
                reloadEnrolledCourses()
                currentScreen = "my_courses"
            }
            "course_detail" -> {
                selectedCourseId = null
                currentScreen = if (previousScreen in listOf("home", "catalog")) previousScreen else "home"
            }
            "login" -> {
                authRedirectTarget = null
                currentScreen = if (previousScreen in listOf("home", "catalog")) previousScreen else "home"
            }
            "consulta_web" -> {
                currentScreen = if (previousScreen in listOf("home", "catalog", "cart", "profile")) previousScreen else "home"
            }
            "cart", "my_courses", "profile", "catalog" -> {
                currentScreen = "home"
            }
            else -> {
                currentScreen = "home"
            }
        }
    }

    // ── Pantalla de Login/Registro (Auth Guard) ──────────────────────────────
    if (currentScreen == "login") {
        LoginScreen(
            onLoginSuccess = { sess, prof ->
                session = sess
                profile = prof
                reloadEnrolledCourses()

                val target = authRedirectTarget
                authRedirectTarget = null
                currentScreen = if (!target.isNullOrBlank()) target else "home"

                Toast.makeText(
                    context,
                    "¡Bienvenido ${prof.fullName.ifBlank { prof.email }}! (${prof.rolLabel})",
                    Toast.LENGTH_LONG
                ).show()
            },
            onSkipLogin = {
                // Al dar atrás o cancelar el login, NUNCA permite entrar a secciones protegidas.
                // Redirige siempre a la pantalla pública previa (home o catálogo).
                authRedirectTarget = null
                currentScreen = if (previousScreen in listOf("home", "catalog")) previousScreen else "home"
            }
        )
        return
    }

    // ── App principal ────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            if (currentScreen !in listOf("course_detail", "course_learn", "consulta_web")) {
                SkillTopBar(
                    cartCount = cartItems.size,
                    onCartClick = {
                        previousScreen = currentScreen
                        currentScreen = "cart"
                    },
                    onNetworkTestClick = {
                        previousScreen = currentScreen
                        currentScreen = "consulta_web"
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreen in listOf("home", "catalog", "cart", "my_courses", "profile")) {
                SkillBottomBar(
                    currentRoute = currentScreen,
                    onNavigate = { route ->
                        if (route in listOf("my_courses", "profile") && session == null) {
                            // Ruta protegida: solicitar login primero
                            previousScreen = if (currentScreen in listOf("home", "catalog")) currentScreen else "home"
                            authRedirectTarget = route
                            currentScreen = "login"
                        } else {
                            authRedirectTarget = null
                            previousScreen = currentScreen
                            currentScreen = route
                            selectedCourseId = null
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                "home" -> HomeScreen(
                    courses = courses,
                    banners = banners,
                    categories = categories,
                    isLoading = isLoading,
                    networkError = networkError,
                    onCourseClick = { cursoId ->
                        selectedCourseId = cursoId
                        coursesViewModel.loadCourseDetail(cursoId)
                        previousScreen = "home"
                        currentScreen = "course_detail"
                    },
                    onNavigateToCatalog = { catName ->
                        selectedCatalogCategory = catName
                        previousScreen = "home"
                        currentScreen = "catalog"
                    },
                    onAddToCart = { curso ->
                        cartViewModel.addToCart(curso)
                        Toast.makeText(context, "Agregado al carrito: ${curso.titulo}", Toast.LENGTH_SHORT).show()
                    },
                    onRefresh = {
                        coursesViewModel.loadInitialData()
                    },
                    onOpenConsultaWeb = {
                        previousScreen = "home"
                        currentScreen = "consulta_web"
                    }
                )

                "consulta_web" -> ConsultaWebScreen(
                    onBackClick = {
                        currentScreen = if (previousScreen in listOf("home", "catalog", "cart", "profile")) previousScreen else "home"
                    }
                )

                "catalog" -> CatalogScreen(
                    courses = courses,
                    categories = categories,
                    isLoading = isLoading,
                    initialCategory = selectedCatalogCategory,
                    onCourseClick = { cursoId ->
                        selectedCourseId = cursoId
                        coursesViewModel.loadCourseDetail(cursoId)
                        previousScreen = "catalog"
                        currentScreen = "course_detail"
                    },
                    onAddToCart = { curso ->
                        cartViewModel.addToCart(curso)
                        Toast.makeText(context, "Agregado al carrito: ${curso.titulo}", Toast.LENGTH_SHORT).show()
                    },
                    onSearchQueryChanged = { query ->
                        // Delega la búsqueda al ViewModel, que llama a Elasticsearch con debounce
                        coursesViewModel.searchCourses(query)
                    }
                )

                "course_detail" -> {
                    val id = selectedCourseId ?: 1L
                    CourseDetailScreen(
                        courseId = id,
                        courseProvider = { coursesViewModel.getCourseById(it) },
                        selectedCourse = selectedCourse,
                        isLoadingDetail = isLoadingDetail,
                        onBackClick = {
                            selectedCourseId = null
                            currentScreen = if (previousScreen in listOf("home", "catalog")) previousScreen else "home"
                        },
                        onAddToCart = { curso ->
                            cartViewModel.addToCart(curso)
                            Toast.makeText(context, "Agregado al carrito: ${curso.titulo}", Toast.LENGTH_SHORT).show()
                        },
                        onBuyNow = { curso ->
                            if (session == null) {
                                previousScreen = "course_detail"
                                authRedirectTarget = "my_courses"
                                currentScreen = "login"
                            } else {
                                val sess = session!!
                                coroutineScope.launch {
                                    val res = InscripcionesRepository.inscribir(sess.userId, curso.id, sess.accessToken)
                                    if (res.isSuccess) {
                                        reloadEnrolledCourses()
                                        Toast.makeText(context, "¡Inscripción exitosa a ${curso.titulo}!", Toast.LENGTH_SHORT).show()
                                        currentScreen = "my_courses"
                                    } else {
                                        Toast.makeText(context, res.error ?: "Error al inscribirse", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }

                "cart" -> CartScreen(
                    cartItems = cartItems,
                    isLoggedIn = session != null,
                    userDisplayName = profile?.fullName?.ifBlank { profile?.email },
                    onRemoveItem = { id ->
                        cartViewModel.removeItem(id)
                    },
                    onCheckoutClick = { total ->
                        if (session == null) {
                            previousScreen = "cart"
                            authRedirectTarget = "cart"
                            currentScreen = "login"
                        } else {
                            lastPaidAmount = total
                            showPaymentGatewaySheet = true
                        }
                    },
                    onExploreCourses = {
                        previousScreen = "cart"
                        currentScreen = "catalog"
                    }
                )

                "my_courses" -> {
                    // Doble protección: si no hay sesión, regresa inmediatamente a home
                    if (session == null) {
                        currentScreen = "home"
                    } else {
                        MyCoursesScreen(
                            enrolledCourses = enrolledCourses,
                            isLoading = isLoadingEnrolled,
                            onCourseLearnClick = { curso, progreso ->
                                learningCourse = curso
                                learningCourseProgress = progreso
                                previousScreen = "my_courses"
                                currentScreen = "course_learn"
                            },
                            onExploreClick = {
                                previousScreen = "my_courses"
                                currentScreen = "catalog"
                            }
                        )
                    }
                }

                "course_learn" -> {
                    val curso = learningCourse
                    val sess = session
                    if (sess == null || curso == null) {
                        currentScreen = "home"
                    } else {
                        CourseLearnScreen(
                            curso = curso,
                            userId = sess.userId,
                            accessToken = sess.accessToken,
                            initialProgress = learningCourseProgress,
                            onBackClick = {
                                learningCourse = null
                                reloadEnrolledCourses()
                                currentScreen = "my_courses"
                            }
                        )
                    }
                }

                "profile" -> {
                    // Doble protección: si no hay sesión, regresa inmediatamente a home
                    val sess = session
                    val prof = profile
                    if (sess == null || prof == null) {
                        currentScreen = "home"
                    } else {
                        ProfileScreen(
                            profile = prof,
                            accessToken = sess.accessToken,
                            onProfileUpdated = { updated ->
                                profile = updated
                                sessionManager.saveProfile(updated)
                            },
                            onLogoutClick = {
                                sessionManager.clearSession()
                                session = null
                                profile = null
                                enrolledCourses = emptyList()
                                cartViewModel.clearCart()
                                currentScreen = "home"
                                Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Pasarela de Pago Transaccional (Sección 3.1) ──────────────────────────
    if (showPaymentGatewaySheet && session != null) {
        val sess = session!!
        PaymentGatewayBottomSheet(
            totalAmount = financialSummary.total,
            subtotalAmount = financialSummary.subtotal,
            discountAmount = financialSummary.discountAmount,
            courses = cartItems,
            userEmail = sess.email,
            onDismissRequest = { showPaymentGatewaySheet = false },
            onPaymentConfirmed = { orderCode, method ->
                showPaymentGatewaySheet = false
                lastOrderNumber = orderCode
                lastPaymentMethod = method
                lastPaidAmount = financialSummary.total
                coroutineScope.launch {
                    // 1. Inscribir cada curso en la tabla inscripciones de Supabase
                    cartItems.forEach { item ->
                        InscripcionesRepository.inscribir(sess.userId, item.id, sess.accessToken)
                    }
                    // 2. Refrescar cursos de la cuenta
                    reloadEnrolledCourses()
                    // 3. Notificación nativa de compra exitosa
                    SkillNotificationHelper.showPromoNotification(
                        context,
                        "¡Compra Confirmada! (Orden $orderCode)",
                        "Tus ${cartItems.size} curso(s) ya están habilitados en Mis Cursos. ¡Comienza a aprender!"
                    )
                    // 4. Mostrar confirmación visual de la orden
                    showCheckoutSuccessDialog = true
                }
            }
        )
    }

    // ── Modal Compra Exitosa con Número de Orden ──────────────────────────────
    if (showCheckoutSuccessDialog) {
        Dialog(onDismissRequest = {
            showCheckoutSuccessDialog = false
            cartViewModel.clearCart()
            currentScreen = "my_courses"
        }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SkillSurface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SkillEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Pago exitoso",
                            tint = SkillEmerald,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "¡Pago Confirmado!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SkillTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SkillCyan.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (lastOrderNumber.isNotBlank()) "ORDEN: $lastOrderNumber" else "ORDEN: ORD-849201",
                            color = SkillCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Medio: ${lastPaymentMethod.ifBlank { "Tarjeta de Crédito" }}\nTotal pagado: $${String.format("%.2f", lastPaidAmount)} USD",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SkillTextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Tu inscripción ha sido registrada de forma segura en Supabase y el contenido ya se encuentra disponible en tu aula virtual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SkillTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            showCheckoutSuccessDialog = false
                            cartViewModel.clearCart()
                            currentScreen = "my_courses"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkillEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Ir a Mis Cursos y Aprender", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
