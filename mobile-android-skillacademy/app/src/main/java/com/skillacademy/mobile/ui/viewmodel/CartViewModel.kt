package com.skillacademy.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillacademy.mobile.data.model.CouponUiState
import com.skillacademy.mobile.data.model.CourseUiModel
import com.skillacademy.mobile.data.remote.CouponRepository
import com.skillacademy.mobile.data.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Entregable 2 — CartViewModel (MVVM)
 * Administra el estado reactivo del carrito, persistencia local con SQLite
 * y validación reactiva de cupones con Supabase en tiempo real.
 */
class CartViewModel(private val cartRepo: CartRepository) : ViewModel() {

    val cartItems: StateFlow<List<CourseUiModel>> = cartRepo.cartItems

    private val _couponState = MutableStateFlow<CouponUiState>(CouponUiState.Idle)
    val couponState: StateFlow<CouponUiState> = _couponState.asStateFlow()

    // Recálculo financiero reactivo automático (Subtotal, Descuento, Total)
    val financialSummary = combine(cartItems, couponState) { items, coupon ->
        val subtotal = items.sumOf { it.precio }
        val discountPercent = when (coupon) {
            is CouponUiState.Applied -> coupon.porcentaje
            else -> 0
        }
        val discountAmount = subtotal * (discountPercent / 100.0)
        val total = (subtotal - discountAmount).coerceAtLeast(0.0)
        FinancialSummary(
            subtotal = subtotal,
            discountPercent = discountPercent,
            discountAmount = discountAmount,
            total = total
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialSummary()
    )

    fun applyCoupon(codigo: String) {
        val trimmed = codigo.trim()
        if (trimmed.isEmpty()) {
            _couponState.value = CouponUiState.Error("Ingresa un código de cupón")
            return
        }
        viewModelScope.launch {
            _couponState.value = CouponUiState.Validating
            val result = CouponRepository.validateCoupon(trimmed)
            _couponState.value = result
        }
    }

    fun removeCoupon() {
        _couponState.value = CouponUiState.Idle
    }

    fun addToCart(course: CourseUiModel): Boolean {
        return cartRepo.addToCart(course)
    }

    fun removeItem(courseId: Long) {
        cartRepo.removeFromCart(courseId)
    }

    fun clearCart() {
        cartRepo.clearCart()
        _couponState.value = CouponUiState.Idle
    }
}

data class FinancialSummary(
    val subtotal: Double = 0.0,
    val discountPercent: Int = 0,
    val discountAmount: Double = 0.0,
    val total: Double = 0.0
)
