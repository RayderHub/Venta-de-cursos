package com.skillacademy.mobile.data.repository

import android.content.Context
import com.skillacademy.mobile.data.local.CartDatabaseHelper
import com.skillacademy.mobile.data.model.CourseUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositorio del Carrito con Persistencia Local (SQLite/Room).
 * Cumple la sección 2.4 y 2.6 del Entregable 2:
 * "Los cursos agregados al carrito se mantienen intactos al reiniciar la aplicación."
 */
class CartRepository(context: Context) {

    private val dbHelper = CartDatabaseHelper(context)
    private val _cartItems = MutableStateFlow<List<CourseUiModel>>(emptyList())
    val cartItems: StateFlow<List<CourseUiModel>> = _cartItems.asStateFlow()

    init {
        // Cargar inmediatamente los cursos guardados en la base de datos local
        loadCart()
    }

    fun loadCart() {
        val items = dbHelper.getAllItems()
        _cartItems.value = items
    }

    fun addToCart(course: CourseUiModel): Boolean {
        if (dbHelper.hasItem(course.id)) return false
        val success = dbHelper.insertItem(course)
        if (success) {
            loadCart()
        }
        return success
    }

    fun removeFromCart(courseId: Long): Boolean {
        val success = dbHelper.deleteItem(courseId)
        if (success) {
            loadCart()
        }
        return success
    }

    fun clearCart() {
        dbHelper.clearCart()
        _cartItems.value = emptyList()
    }

    fun isInCart(courseId: Long): Boolean {
        return dbHelper.hasItem(courseId)
    }

    val count: Int
        get() = _cartItems.value.size

    val subtotal: Double
        get() = _cartItems.value.sumOf { it.precio }
}
