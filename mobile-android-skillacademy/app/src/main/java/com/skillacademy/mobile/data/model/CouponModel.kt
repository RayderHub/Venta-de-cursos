package com.skillacademy.mobile.data.model

sealed interface CouponUiState {
    object Idle : CouponUiState
    object Validating : CouponUiState
    data class Applied(val codigo: String, val porcentaje: Int) : CouponUiState
    data class Error(val message: String) : CouponUiState
}

data class CuponDto(
    val id: Long,
    val codigo: String,
    val descuento: Int,
    val activo: Boolean
)
