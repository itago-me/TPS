package com.tps.ui.product

/**
 * 文件说明：商品模块状态管理，负责商品列表、详情、发布与状态流转的数据编排。
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.ProductDto
import com.tps.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val product: ProductDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val orderCreated: Boolean = false,
    val orderError: String? = null,
    val navigateToChatId: Long? = null,
    val isOwner: Boolean = false,
    val deleted: Boolean = false,
    val actionSuccess: String? = null
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState

    fun load(productId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // 详情接口会顺带返回当前用户是否已收藏，页面据此一次性初始化多个按钮状态。
                val resp = apiService.getProduct(productId)
                val product = resp.data
                val isOwner = product?.userId == tokenManager.getUserId()

                _uiState.value = _uiState.value.copy(
                    product = product,
                    isLoading = false,
                    isFavorite = product?.favorited == true,
                    isOwner = isOwner
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun createOrder(productId: Long) {
        viewModelScope.launch {
            try {
                val price = _uiState.value.product?.price ?: 0.0
                apiService.createOrder(productId = productId, finalPrice = price)
                _uiState.value = _uiState.value.copy(orderCreated = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(orderError = e.message)
            }
        }
    }

    fun startChat(sellerId: Long, productId: Long) {
        viewModelScope.launch {
            try {
                // 联系卖家前先让后端按“商品 + 买家 + 卖家”归一化会话，避免同一商品反复生成新会话。
                val resp = apiService.getOrCreateConversation(targetUserId = sellerId, productId = productId)
                resp.data?.let { _uiState.value = _uiState.value.copy(navigateToChatId = it.id) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "无法创建会话")
            }
        }
    }

    fun consumeNavigateToChat() {
        _uiState.value = _uiState.value.copy(navigateToChatId = null)
    }

    fun toggleFavorite(productId: Long) {
        viewModelScope.launch {
            try {
                val resp = apiService.toggleFavorite(productId)
                _uiState.value = _uiState.value.copy(isFavorite = resp.data == true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "收藏操作失败")
            }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            try {
                apiService.updateProductStatus(productId, "OFF")
                _uiState.value = _uiState.value.copy(deleted = true, actionSuccess = "商品已下架")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun bumpProduct(productId: Long) {
        viewModelScope.launch {
            try {
                val resp = apiService.bumpProduct(productId)
                _uiState.value = _uiState.value.copy(
                    product = resp.data ?: _uiState.value.product,
                    actionSuccess = "商品已擦亮"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateStatus(productId: Long, status: String) {
        viewModelScope.launch {
            try {
                val resp = apiService.updateProductStatus(productId, status)
                _uiState.value = _uiState.value.copy(
                    product = resp.data ?: _uiState.value.product,
                    actionSuccess = if (status == "ON_SALE") "商品已重新上架" else "商品已下架"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun reportProduct(productId: Long, reason: String) {
        if (reason.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请填写举报原因")
            return
        }
        viewModelScope.launch {
            try {
                apiService.reportProduct(productId, reason.trim())
                _uiState.value = _uiState.value.copy(actionSuccess = "举报已提交，平台将进行审核")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "举报失败")
            }
        }
    }

    fun consumeActionSuccess() {
        _uiState.value = _uiState.value.copy(actionSuccess = null)
    }
}
