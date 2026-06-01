package com.tps.data.remote.dto

/**
 * 文件说明：Android 侧数据传输对象集合，负责承接接口请求与响应字段。
 */

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    @SerializedName(value = "number", alternate = ["page"])
    val number: Int
)

// Auth
data class RegisterRequest(
    val phone: String,
    val password: String,
    val code: String,
    val studentId: String,
    val nickname: String
)

data class LoginRequest(
    val phone: String,
    val password: String? = null,
    val code: String? = null,
    val loginType: String = "PASSWORD"
)

data class LoginResponse(
    val token: String,
    val refreshToken: String,
    val userId: Long,
    val nickname: String,
    val avatarUrl: String?,
    val role: String
)

// User
data class UserProfile(
    val id: Long,
    val phone: String?,
    val studentId: String?,
    val nickname: String,
    val avatarUrl: String?,
    val creditScore: Int,
    val bio: String?,
    val location: String?,
    val shippingAddress: String?,
    val role: String,
    val status: String,
    val productCount: Int
)

data class UpdateProfileRequest(
    val nickname: String?,
    val bio: String?,
    val location: String?,
    val shippingAddress: String?,
    val avatarUrl: String? = null
)

data class UploadResponse(
    val url: String,
    val path: String
)

// Product
data class ProductDto(
    val id: Long,
    val userId: Long,
    val sellerNickname: String,
    val sellerAvatar: String?,
    val title: String,
    val description: String?,
    val price: Double,
    val category: String?,
    val condition: String?,
    val status: String,
    val location: String?,
    val viewCount: Int,
    val favoriteCount: Int = 0,
    val bumpedAt: String? = null,
    val takedownReason: String? = null,
    val takedownBy: Long? = null,
    val takedownAt: String? = null,
    val imageUrls: List<String>,
    val favorited: Boolean,
    val createdAt: String?
)

data class ProductRequest(
    val title: String,
    val description: String?,
    val price: Double,
    val category: String?,
    val condition: String?,
    val location: String?,
    val imageUrls: List<String>
)

// Order
data class OrderDto(
    val id: Long,
    val productId: Long,
    val productTitle: String,
    val productCover: String? = null,
    val buyerId: Long,
    val buyerNickname: String? = null,
    val sellerId: Long,
    val sellerNickname: String? = null,
    val price: Double,
    val status: String,
    val remark: String? = null,
    val trackingNumber: String? = null,
    val createdAt: String?
)

// Message
data class ConversationDto(
    val id: Long,
    val conversationId: Long? = null,
    val buyerId: Long? = null,
    val sellerId: Long? = null,
    val productId: Long,
    val productTitle: String? = null,
    val productPrice: Double? = null,
    val productImageUrl: String? = null,
    val productCover: String? = null,
    val targetUserId: Long? = null,
    val targetNickname: String? = null,
    val targetAvatarUrl: String? = null,
    val targetAvatar: String? = null,
    val lastMessage: String?,
    val unreadCount: Int? = null,
    val unreadBuyer: Int? = null,
    val unreadSeller: Int? = null,
    val updatedAt: String?
)

data class MessageDto(
    val id: Long,
    val conversationId: Long? = null,
    val senderId: Long,
    val content: String,
    val type: String,
    val createdAt: String,
    val isRead: Boolean? = null
)

// Notification
data class NotificationDto(
    val id: Long,
    val title: String,
    val content: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: String
)

// Report
data class ReportDto(
    val id: Long,
    val reporterId: Long,
    val productId: Long,
    val productTitle: String? = null,
    val productImageUrl: String? = null,
    val reason: String?,
    val status: String,
    val createdAt: String?
)

// Feedback
data class FeedbackRequest(
    val type: String,
    val content: String,
    val contact: String?
)

data class FeedbackDto(
    val id: Long,
    val userId: Long,
    val userNickname: String? = null,
    val type: String,
    val content: String,
    val contact: String?,
    val status: String,
    val reply: String?,
    val createdAt: String?,
    val updatedAt: String? = null
)

// Admin
data class AdminStats(
    val totalUsers: Long,
    val totalProducts: Long,
    val totalOrders: Long,
    val totalAmount: Double
)

typealias AdminStatsDto = AdminStats

data class AdminUserDto(
    val id: Long,
    val phone: String,
    val nickname: String,
    val avatarUrl: String?,
    val creditScore: Int,
    val bio: String?,
    val location: String?,
    val role: String,
    val status: String,
    val createdAt: String?
)
