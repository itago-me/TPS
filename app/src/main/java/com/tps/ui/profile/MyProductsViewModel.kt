package com.tps.ui.profile

/**
 * 文件说明：个人中心状态管理，负责资料、收藏、历史、反馈等数据编排。
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.ProductDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyProductsUiState(
    val products: List<ProductDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val operatingProductId: Long? = null
)

@HiltViewModel
class MyProductsViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProductsUiState())
    val uiState: StateFlow<MyProductsUiState> = _uiState

    fun loadMyProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val resp = apiService.getMyProducts()
                _uiState.value = _uiState.value.copy(
                    products = resp.data ?: emptyList(),
                    isLoading = false,
                    operatingProductId = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun updateStatus(id: Long, status: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operatingProductId = id, error = null, successMessage = null)
            try {
                apiService.updateProductStatus(id, status)
                _uiState.value = _uiState.value.copy(
                    successMessage = if (status == "ON_SALE") "商品已重新上架" else "商品已下架"
                )
                loadMyProducts() // Reload to get updated data
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, operatingProductId = null)
            }
        }
    }

    fun bumpProduct(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operatingProductId = id, error = null, successMessage = null)
            try {
                apiService.bumpProduct(id)
                _uiState.value = _uiState.value.copy(successMessage = "商品已擦亮")
                loadMyProducts()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, operatingProductId = null)
            }
        }
    }

    fun consumeMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
