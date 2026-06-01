package com.tps.ui.admin

/**
 * 文件说明：管理员模块界面，负责后台管理页面的 Compose 展示与交互。
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tps.data.remote.dto.ProductDto
import com.tps.data.remote.dto.ReportDto
import com.tps.ui.theme.AppAsyncImage
import com.tps.ui.theme.MarketCard
import com.tps.ui.theme.MarketHeroCard
import com.tps.ui.theme.MarketOrange
import com.tps.ui.theme.StatusPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("用户管理", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MarketHeroCard("用户治理", "查看信用与账号状态，快速封禁异常用户。") }
            items(uiState.users) { user ->
                MarketCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(user.nickname, fontWeight = FontWeight.Bold)
                            Text("信用分：${user.creditScore}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(user.status, if (user.status == "BANNED") MaterialTheme.colorScheme.error else MarketOrange)
                        IconButton(onClick = { viewModel.banUser(user.id, user.status == "BANNED") }) {
                            Icon(
                                if (user.status == "BANNED") Icons.Default.Block else Icons.Default.Block,
                                contentDescription = null,
                                tint = if (user.status == "BANNED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingTakedown by remember { mutableStateOf<ProductDto?>(null) }
    var takedownReason by remember { mutableStateOf("") }
    var pendingReportTakedown by remember { mutableStateOf<ReportDto?>(null) }
    var reportTakedownReason by remember { mutableStateOf("") }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        val message = uiState.error ?: uiState.successMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessages()
        }
    }

    pendingTakedown?.let { product ->
        AlertDialog(
            onDismissRequest = {
                pendingTakedown = null
                takedownReason = ""
            },
            title = { Text("强制下架商品") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("商品：${product.title}")
                    OutlinedTextField(
                        value = takedownReason,
                        onValueChange = { takedownReason = it.take(255) },
                        label = { Text("下架原因") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${takedownReason.length}/255", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.takedownProduct(product.id, takedownReason)
                        pendingTakedown = null
                        takedownReason = ""
                    },
                    enabled = takedownReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认下架")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingTakedown = null
                    takedownReason = ""
                }) {
                    Text("取消")
                }
            }
        )
    }
    pendingReportTakedown?.let { report ->
        AlertDialog(
            onDismissRequest = {
                pendingReportTakedown = null
                reportTakedownReason = ""
            },
            title = { Text("处理举报并下架") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(report.productTitle ?: "商品ID：${report.productId}")
                    Text("用户举报原因：${report.reason ?: "无"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = reportTakedownReason,
                        onValueChange = { reportTakedownReason = it.take(255) },
                        label = { Text("平台下架原因") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${reportTakedownReason.length}/255", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.handleReport(report.id, true, reportTakedownReason)
                        pendingReportTakedown = null
                        reportTakedownReason = ""
                    },
                    enabled = reportTakedownReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认下架")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingReportTakedown = null
                    reportTakedownReason = ""
                }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("商品审核", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MarketHeroCard("商品治理", "查看上架商品，填写原因后强制下架违规内容。") }

            item {
                SectionHeader("上架中商品", "${uiState.listedProducts.size} 件")
            }

            if (uiState.listedProducts.isEmpty()) {
                item {
                    MarketCard {
                        Text("暂无上架中商品", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.listedProducts, key = { it.id }) { product ->
                    AdminListedProductCard(
                        product = product,
                        operating = uiState.operatingProductId == product.id,
                        onTakedown = {
                            pendingTakedown = product
                            takedownReason = ""
                        }
                    )
                }
            }

            item {
                SectionHeader("待处理举报", "${uiState.reportedProducts.size} 条")
            }

            items(uiState.reportedProducts) { report ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppAsyncImage(
                                url = report.productImageUrl,
                                contentDescription = report.productTitle,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFFFE1D2)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(report.productTitle ?: "商品ID：${report.productId}", style = MaterialTheme.typography.titleMedium)
                                Text("商品ID：${report.productId}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("举报原因：${report.reason ?: "无"}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                pendingReportTakedown = report
                                reportTakedownReason = ""
                            },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                Text("处理并下架")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        StatusPill(count, MarketOrange)
    }
}

@Composable
private fun AdminListedProductCard(
    product: ProductDto,
    operating: Boolean,
    onTakedown: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppAsyncImage(
                    url = product.imageUrls.firstOrNull(),
                    contentDescription = product.title,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFE1D2)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(product.title, fontWeight = FontWeight.Bold)
                    Text("卖家：${product.sellerNickname} | ¥${product.price}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("商品ID：${product.id} | ${product.category ?: "未分类"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill("在售", MarketOrange)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (operating) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp), strokeWidth = 2.dp)
                }
                Button(
                    onClick = onTakedown,
                    enabled = !operating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("强制下架")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("订单管理", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MarketHeroCard("订单看板", "集中查看平台交易状态和退款风险。") }
            items(uiState.orders) { order ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("订单 #${order.id}", style = MaterialTheme.typography.labelSmall)
                        Text("商品ID：${order.productId}", style = MaterialTheme.typography.titleMedium)
                        Text("状态：${order.status} | ¥${order.price}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatsScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("数据统计", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MarketHeroCard("运营数据", "用户、商品、订单和交易额一屏掌握。")
            StatCard("总用户数", uiState.stats?.totalUsers?.toString() ?: "-")
            StatCard("总商品数", uiState.stats?.totalProducts?.toString() ?: "-")
            StatCard("总订单数", uiState.stats?.totalOrders?.toString() ?: "-")
            StatCard("总交易额", uiState.stats?.totalAmount?.let { "¥$it" } ?: "-")
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MarketOrange, fontWeight = FontWeight.ExtraBold)
        }
    }
}
