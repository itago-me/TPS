package com.tps.ui.admin

/**
 * 文件说明：管理员页面状态管理，负责后台管理数据加载与操作编排。
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.AdminStats
import com.tps.data.remote.dto.OrderDto
import com.tps.data.remote.dto.ProductDto
import com.tps.data.remote.dto.ReportDto
import com.tps.data.remote.dto.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val users: List<UserProfile> = emptyList(),
    val listedProducts: List<ProductDto> = emptyList(),
    val reportedProducts: List<ReportDto> = emptyList(),
    val orders: List<OrderDto> = emptyList(),
    val stats: AdminStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val operatingProductId: Long? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    init {
        loadUsers()
        loadListedProducts()
        loadReportedProducts()
        loadOrders()
        loadStats()
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                val resp = apiService.adminGetUsers()
                _uiState.value = _uiState.value.copy(users = resp.data?.content ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadListedProducts() {
        viewModelScope.launch {
            try {
                val resp = apiService.adminGetProducts(status = "ON_SALE")
                _uiState.value = _uiState.value.copy(listedProducts = resp.data?.content ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadReportedProducts() {
        viewModelScope.launch {
            try {
                val resp = apiService.adminGetReports()
                _uiState.value = _uiState.value.copy(reportedProducts = resp.data?.content ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            try {
                val resp = apiService.adminGetOrders()
                _uiState.value = _uiState.value.copy(orders = resp.data?.content ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                val resp = apiService.adminGetStats()
                _uiState.value = _uiState.value.copy(stats = resp.data)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun banUser(userId: Long, isBanned: Boolean) {
        viewModelScope.launch {
            try {
                if (isBanned) apiService.adminUnbanUser(userId)
                else apiService.adminBanUser(userId)
                loadUsers()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun handleReport(reportId: Long, takedown: Boolean, reason: String? = null) {
        if (takedown && reason.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = "请填写下架原因")
            return
        }
        viewModelScope.launch {
            try {
                apiService.adminHandleReport(reportId, takedown, reason?.trim())
                _uiState.value = _uiState.value.copy(successMessage = if (takedown) "举报已处理，商品已下架" else "举报已处理")
                loadReportedProducts()
                loadListedProducts()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun takedownProduct(productId: Long, reason: String) {
        if (reason.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请填写下架原因")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                operatingProductId = productId,
                error = null,
                successMessage = null
            )
            try {
                apiService.adminTakedownProduct(productId, reason.trim())
                _uiState.value = _uiState.value.copy(
                    successMessage = "商品已强制下架",
                    operatingProductId = null
                )
                loadListedProducts()
                loadStats()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, operatingProductId = null)
            }
        }
    }

    fun consumeMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
