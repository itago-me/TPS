package com.tps.ui.product

/**
 * 文件说明：商品模块状态管理，负责商品列表、详情、发布与状态流转的数据编排。
 */

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.ProductRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class PublishUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val successMessage: String? = null,
    val selectedImages: List<Uri> = emptyList()
)

@HiltViewModel
class PublishProductViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState: StateFlow<PublishUiState> = _uiState

    fun addImages(uris: List<Uri>) {
        val current = _uiState.value.selectedImages.toMutableList()
        current.addAll(uris.take(9 - current.size))
        _uiState.value = _uiState.value.copy(selectedImages = current)
    }

    fun removeImage(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            selectedImages = _uiState.value.selectedImages.filter { it != uri }
        )
    }

    fun publish(title: String, description: String, price: Double, category: String, condition: String, location: String) {
        val validationError = validate(title, price, category, condition)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(error = validationError)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val imageUrls = _uiState.value.selectedImages.mapNotNull { uri ->
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@mapNotNull null
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", resolveFileName(uri), body)
                    val response = apiService.uploadImage(part)
                    response.data?.url
                }
                val req = ProductRequest(title, description, price, category, condition, location, imageUrls)
                val response = apiService.createProduct(req)
                if (response.code == 200 && response.data != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        successMessage = "商品已发布，可在“我发布的”中管理上下架"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = response.message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun validate(title: String, price: Double, category: String, condition: String): String? {
        if (title.isBlank()) return "请填写商品标题"
        if (title.length > 100) return "商品标题最多100个字"
        if (price <= 0.0) return "请输入大于0的价格"
        if (category.isBlank()) return "请选择商品分类"
        if (condition.isBlank()) return "请选择商品成色"
        return null
    }

    private fun resolveFileName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                val name = cursor.getString(nameIndex)
                if (!name.isNullOrBlank()) return name
            }
        }
        return "image.jpg"
    }
}
