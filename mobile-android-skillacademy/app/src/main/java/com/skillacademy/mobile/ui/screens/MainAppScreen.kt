package com.skillacademy.mobile.ui.screens

import android.widget.Toast
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
import com.skillacademy.mobile.data.mock.MockData
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.data.remote.SupabaseProfile
import com.skillacademy.mobile.data.remote.SupabaseSession
import com.skillacademy.mobile.ui.components.SkillBottomBar
import com.skillacademy.mobile.ui.components.SkillTopBar
import com.skillacademy.mobile.ui.theme.*

@Composable
fun MainAppScreen() {
    val context = LocalContext.current

    // ── Sesión del usuario (null = no ha iniciado sesión) ─────────────────────
    var session  by remember { mutableStateOf<SupabaseSession?>(null) }
    var profile  by remember { mutableStateOf<SupabaseProfile?>(null) }

    // ── Navegación ────────────────────────────────────────────────────────────
    var currentScreen by remember { mutableStateOf("login") }
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var selectedCatalogCategory by remember { mutableStateOf<String?>(null) }
    var showCheckoutSuccessDialog by remember { mutableStateOf(false) }
    var lastPaidAmount by remember { mutableDoubleStateOf(0.0) }

    val cartItems = remember { mutableStateListOf<CourseUiModel>() }

    LaunchedEffect(Unit) {
        if (cartItems.isEmpty()) {
            cartItems.add(MockData.courses[0])
        }
    }

    // ── Pantalla de Login (no usa Scaffold para ocupar toda la pantalla) ──────
    if (currentScreen == "login") {
        LoginScreen(
            onLoginSuccess = { sess, prof ->
                session       = sess
                profile       = prof
                // Enrutamiento por rol (igual que getHomeRouteForRole en auth.service.ts)
                currentScreen = when (prof.roleId) {
                    "admin"   -> "home"   // Los admin también pueden usar la app
                    "teacher" -> "home"
                    else      -> "home"   // student
                }
                Toast.makeText(
                    context,
                    "¡Bienvenido ${prof.fullName.ifBlank { prof.email }}! (${prof.rolLabel})",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
        return
    }

    // ── Práctica 4 — pantalla independiente ───────────────────────────────────
    if (currentScreen == "practica4") {
        Practica4UsuariosScreen(
            accessToken = session?.accessToken ?: "",
            onBackClick = { currentScreen = "profile" }
        )
        return
    }

    // ── App principal (requiere sesión) ───────────────────────────────────────
    Scaffold(
        topBar = {
            if (currentScreen != "course_detail" && currentScreen != "consulta_web") {
                SkillTopBar(
                    cartCount          = cartItems.size,
                    onCartClick        = { currentScreen = "cart" },
                    onConsultaWebClick = { currentScreen = "consulta_web" }
                )
            }
        },
        bottomBar = {
            if (currentScreen in listOf("home", "catalog", "my_courses", "profile")) {
                SkillBottomBar(
                    currentRoute = currentScreen,
                    onNavigate   = { route ->
                        currentScreen    = route
                        selectedCourseId = null
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
                    onCourseClick = { cursoId ->
                        selectedCourseId = cursoId
                        currentScreen    = "course_detail"
                    },
                    onAddToCart = { curso ->
                        if (!cartItems.any { it.id == curso.id }) {
                            cartItems.add(curso)
                            Toast.makeText(context, "¡${curso.titulo} añadido al carrito!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Este curso ya está en tu carrito", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onNavigateToCatalog = { cat ->
                        selectedCatalogCategory = cat
                        currentScreen           = "catalog"
                    },
                    onConsultaWebClick = { currentScreen = "consulta_web" }
                )

                "catalog" -> CatalogScreen(
                    initialCategory = selectedCatalogCategory,
                    onCourseClick   = { cursoId ->
                        selectedCourseId = cursoId
                        currentScreen    = "course_detail"
                    },
                    onAddToCart = { curso ->
                        if (!cartItems.any { it.id == curso.id }) {
                            cartItems.add(curso)
                            Toast.makeText(context, "¡Añadido al carrito!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                "course_detail" -> {
                    selectedCourseId?.let { id ->
                        CourseDetailScreen(
                            courseId    = id,
                            onBackClick = { currentScreen = "home" },
                            onAddToCart = { curso ->
                                if (!cartItems.any { it.id == curso.id }) {
                                    cartItems.add(curso)
                                    Toast.makeText(context, "¡Añadido al carrito!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBuyNow = { curso ->
                                if (!cartItems.any { it.id == curso.id }) {
                                    cartItems.add(curso)
                                }
                                currentScreen = "cart"
                            }
                        )
                    }
                }

                "cart" -> CartScreen(
                    cartItems       = cartItems,
                    onRemoveItem    = { id -> cartItems.removeAll { it.id == id } },
                    onCheckoutClick = { total ->
                        lastPaidAmount            = total
                        showCheckoutSuccessDialog = true
                    },
                    onExploreCourses = { currentScreen = "catalog" }
                )

                "my_courses" -> MyCoursesScreen(
                    onCourseLearnClick = {
                        Toast.makeText(context, "Iniciando lección del curso...", Toast.LENGTH_SHORT).show()
                    }
                )

                "profile" -> ProfileScreen(
                    profile = profile,
                    onLogoutClick = {
                        session       = null
                        profile       = null
                        cartItems.clear()
                        currentScreen = "login"
                        Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                    },
                    onConsultaWebClick  = { currentScreen = "consulta_web" },
                    onPractica4Click    = { currentScreen = "practica4" }
                )

                "consulta_web" -> ConsultaWebScreen(
                    onBackClick = { currentScreen = "home" }
                )
            }
        }
    }

    // ── Modal Compra Exitosa ───────────────────────────────────────────────────
    if (showCheckoutSuccessDialog) {
        Dialog(onDismissRequest = {
            showCheckoutSuccessDialog = false
            cartItems.clear()
            currentScreen = "my_courses"
        }) {
            Card(
                shape  = RoundedCornerShape(20.dp),
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
                            .background(Color(0xFFCCFBF1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Éxito",
                            tint = SkillEmerald,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("¡Compra Exitosa!", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = SkillTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Total abonado: $${String.format("%.2f", lastPaidAmount)} USD",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SkillEmerald)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Tus cursos han sido activados en tu cuenta. Ya puedes acceder al aula virtual y comenzar a aprender.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SkillTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            showCheckoutSuccessDialog = false
                            cartItems.clear()
                            currentScreen = "my_courses"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkillEmerald),
                        shape  = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Ir a Mis Cursos Ahora", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
