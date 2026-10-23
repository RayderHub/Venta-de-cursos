package com.skillacademy.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillacademy.mobile.data.model.*
import com.skillacademy.mobile.data.remote.CoursesRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class CoursesViewModel : ViewModel() {

    // ── Catálogo ──────────────────────────────────────────────────────────────
    private val _courses  = MutableStateFlow<List<CourseUiModel>>(emptyList())
    val courses: StateFlow<List<CourseUiModel>> = _courses.asStateFlow()

    private val _banners  = MutableStateFlow<List<BannerUiModel>>(emptyList())
    val banners: StateFlow<List<BannerUiModel>> = _banners.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryUiModel>>(emptyList())
    val categories: StateFlow<List<CategoryUiModel>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _networkError = MutableStateFlow<String?>(null)
    val networkError: StateFlow<String?> = _networkError.asStateFlow()

    // ── Detalle de curso (cargado bajo demanda) ───────────────────────────────
    private val _selectedCourse = MutableStateFlow<CourseUiModel?>(null)
    val selectedCourse: StateFlow<CourseUiModel?> = _selectedCourse.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    // ── Búsqueda predictiva ───────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadInitialData()
        // Debounce de 300ms en la búsqueda
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        _isLoading.value = true
                        _courses.value = CoursesRepository.searchCourses(query)
                        _isLoading.value = false
                    } else if (query.isBlank() && _courses.value.isEmpty()) {
                        loadInitialData()
                    }
                }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            _networkError.value = null
            try {
                val fetched = CoursesRepository.getCourses()
                if (CoursesRepository.isLastFetchSuccess) {
                    _courses.value    = fetched
                    _banners.value    = CoursesRepository.getBanners()
                    _categories.value = CoursesRepository.getCategories()
                    _networkError.value = null
                } else {
                    _courses.value = emptyList()
                    _networkError.value = "No fue posible obtener la información."
                }
            } catch (e: Exception) {
                _courses.value = emptyList()
                _networkError.value = "No fue posible obtener la información."
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Carga el detalle COMPLETO de un curso (lecciones, reseñas, instructor).
     * Primero pone el objeto base del catálogo como placeholder para que la UI
     * muestre algo inmediatamente, luego lo reemplaza con los datos completos.
     */
    fun loadCourseDetail(cursoId: Long) {
        viewModelScope.launch {
            // Placeholder instantáneo desde el catálogo ya cargado
            _selectedCourse.value = _courses.value.find { it.id == cursoId }
            _isLoadingDetail.value = true
            try {
                val detail = CoursesRepository.getCourseDetail(cursoId)
                if (detail != null) _selectedCourse.value = detail
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }

    fun searchCourses(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            viewModelScope.launch {
                _isLoading.value = true
                _courses.value = CoursesRepository.getCourses()
                _isLoading.value = false
            }
        }
    }

    /** Busca un curso en la lista local ya cargada (para CourseDetailScreen rápido) */
    fun getCourseById(id: Long): CourseUiModel? = _courses.value.find { it.id == id }
}
