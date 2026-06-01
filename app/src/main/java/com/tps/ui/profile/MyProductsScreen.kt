package com.tps.ui.profile

/**
 * 文件说明：个人中心界面，负责资料、收藏、历史、反馈等页面展示。
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tps.data.remote.dto.ProductDto
import com.tps.ui.product.ProductCard
import com.tps.ui.theme.MarketBackground
import com.tps.ui.theme.MarketEmptyState
import com.tps.ui.theme.MarketOrange

private enum class ProductStatusFilter(val label: String, val status: String?) {
    ALL("全部", null),
    ON_SALE("在售", "ON_SALE"),
    OFF("已下架", "OFF"),
    SOLD("已售出", "SOLD")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProductsScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: MyProductsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedFilter by remember { mutableStateOf(ProductStatusFilter.ALL) }
    var pendingTakedown by remember { mutableStateOf<ProductDto?>(null) }

    val filteredProducts = remember(uiState.products, selectedFilter) {
        selectedFilter.status?.let { status ->
            uiState.products.filter { it.status == status }
        } ?: uiState.products
    }

    LaunchedEffect(Unit) {
        viewModel.loadMyProducts()
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        val message = uiState.error ?: uiState.successMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessages()
        }
    }

    pendingTakedown?.let { product ->
        AlertDialog(
            onDismissRequest = { pendingTakedown = null },
            title = { Text("下架商品") },
            text = { Text("确定下架“${product.title}”吗？下架后买家将无法在首页看到它，你可以稍后重新上架。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateStatus(product.id, "OFF")
                        pendingTakedown = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认下架")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTakedown = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("我发布的", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F7F7))
            )
        }
    ) { padding ->
        MarketBackground {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ProductStatusFilter.entries) { filter ->
                        val count = if (filter.status == null) {
                            uiState.products.size
                        } else {
                            uiState.products.count { it.status == filter.status }
                        }
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text("${filter.label} $count") }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading && uiState.products.isEmpty() -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        filteredProducts.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                MarketEmptyState("暂无${selectedFilter.label}商品", "发布后可以在这里管理擦亮、下架和重新上架")
                            }
                        }
                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredProducts, key = { it.id }) { product ->
                                    MyProductItem(
                                        product = product,
                                        operating = uiState.operatingProductId == product.id,
                                        onNavigateToDetail = onNavigateToDetail,
                                        onBump = { viewModel.bumpProduct(product.id) },
                                        onTakedown = { pendingTakedown = product },
                                        onRelist = { viewModel.updateStatus(product.id, "ON_SALE") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyProductItem(
    product: ProductDto,
    operating: Boolean,
    onNavigateToDetail: (Long) -> Unit,
    onBump: () -> Unit,
    onTakedown: () -> Unit,
    onRelist: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProductCard(product = product, onClick = { onNavigateToDetail(product.id) })

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(product.statusLabel()) }
                )
                Text(
                    text = product.statusHint(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (product.status == "OFF" && !product.takedownReason.isNullOrBlank()) {
                Text(
                    text = "平台下架原因：${product.takedownReason}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF0F0), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (operating) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp), strokeWidth = 2.dp)
                }
                when (product.status) {
                    "ON_SALE", "AVAILABLE" -> {
                        TextButton(onClick = onBump, enabled = !operating) { Text("擦亮") }
                        TextButton(
                            onClick = onTakedown,
                            enabled = !operating,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("下架")
                        }
                    }
                    "OFF" -> {
                        OutlinedButton(onClick = onRelist, enabled = !operating) { Text("重新上架") }
                    }
                    "SOLD" -> {
                        Text(
                            text = "已成交，不能重新上架",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(Color(0xFFF1F1F1), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun ProductDto.statusLabel(): String = when (status) {
    "ON_SALE", "AVAILABLE" -> "在售"
    "OFF" -> "已下架"
    "SOLD" -> "已售出"
    else -> status
}

private fun ProductDto.statusHint(): String = when (status) {
    "ON_SALE", "AVAILABLE" -> "买家可浏览、收藏和下单"
    "OFF" -> if (takedownReason.isNullOrBlank()) "首页不可见，可重新上架" else "平台已下架，请查看原因"
    "SOLD" -> "订单完成后锁定状态"
    else -> "状态未知"
}
