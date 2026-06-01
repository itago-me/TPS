package com.tps.ui.product

/**
 * 文件说明：商品模块界面，负责商品浏览、详情或发布流程的 Compose 展示。
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tps.ui.theme.AppAsyncImage
import com.tps.util.resolveMediaUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    onBack: () -> Unit,
    onChat: (Long) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(productId) { viewModel.load(productId) }
    LaunchedEffect(uiState.navigateToChatId) {
        uiState.navigateToChatId?.let { 
            onChat(it)
            viewModel.consumeNavigateToChat()
        }
    }
    LaunchedEffect(uiState.orderCreated) {
        if (uiState.orderCreated) snackbarHostState.showSnackbar("下单成功！")
    }
    LaunchedEffect(uiState.orderError) {
        uiState.orderError?.let { snackbarHostState.showSnackbar("下单失败：$it") }
    }
    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(uiState.actionSuccess) {
        uiState.actionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeActionSuccess()
        }
    }

    Scaffold(
        containerColor = Color(0xFFFFF7F0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            uiState.product?.let { product ->
                var showMenu by remember { mutableStateOf(false) }
                var showDeleteDialog by remember { mutableStateOf(false) }
                var showReportDialog by remember { mutableStateOf(false) }
                var reportReason by remember { mutableStateOf("") }
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("下架商品") },
                        text = { Text("确定要下架该商品吗？下架后可以重新上架。") },
                        confirmButton = {
                            TextButton(onClick = { viewModel.deleteProduct(product.id) }) { Text("下架", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
                        }
                    )
                }
                if (showReportDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showReportDialog = false
                            reportReason = ""
                        },
                        title = { Text("举报商品") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("请说明该商品存在的问题，平台管理员会在后台审核。")
                                OutlinedTextField(
                                    value = reportReason,
                                    onValueChange = { reportReason = it.take(255) },
                                    label = { Text("举报原因") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("${reportReason.length}/255", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.reportProduct(product.id, reportReason)
                                    showReportDialog = false
                                    reportReason = ""
                                },
                                enabled = reportReason.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("提交举报")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showReportDialog = false
                                reportReason = ""
                            }) { Text("取消") }
                        }
                    )
                }

                TopAppBar(
                    title = { Text("商品详情", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                    actions = {
                        IconButton(onClick = { viewModel.toggleFavorite(productId) }) {
                            Icon(
                                if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                null
                            )
                        }
                        if (uiState.isOwner) {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "更多操作")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("下架商品", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; showDeleteDialog = true }
                                )
                            }
                        } else {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "更多操作")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("举报商品", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; showReportDialog = true }
                                )
                            }
                        }
                    }
                )
            } ?: TopAppBar(
                title = { Text("商品详情", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(productId) }) {
                        Icon(
                            if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null
                        )
                    }
                }
            )
        },
        bottomBar = {
            uiState.product?.let { product ->
                var showBuyDialog by remember { mutableStateOf(false) }
                if (showBuyDialog) {
                    AlertDialog(
                        onDismissRequest = { showBuyDialog = false },
                        title = { Text("确认购买") },
                        text = { Text("商品名称：${product.title}\n购买价格：¥${product.price}") },
                        confirmButton = {
                            Button(onClick = {
                                viewModel.createOrder(product.id)
                                showBuyDialog = false
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A1F))) { Text("确认下单") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBuyDialog = false }) { Text("取消") }
                        }
                    )
                }
                Surface(tonalElevation = 8.dp, color = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.isOwner) {
                        when (product.status) {
                            "ON_SALE", "AVAILABLE" -> {
                                OutlinedButton(
                                    onClick = { viewModel.bumpProduct(product.id) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("擦亮商品") }
                                Button(
                                    onClick = { viewModel.updateStatus(product.id, "OFF") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A1F))
                                ) { Text("下架商品") }
                            }
                            "OFF" -> {
                                Button(
                                    onClick = { viewModel.updateStatus(product.id, "ON_SALE") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A1F))
                                ) { Text("重新上架") }
                            }
                            "SOLD" -> {
                                Text(
                                    text = "商品已成交，不能重新上架",
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.startChat(product.userId, product.id) },
                            modifier = Modifier.weight(1f)
                        ) { Text("联系卖家") }
                        Button(
                            onClick = { showBuyDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = product.status == "ON_SALE" || product.status == "AVAILABLE",
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A1F))
                        ) { Text("立即购买") }
                    }
                }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            uiState.product?.let { product ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { product.imageUrls.size })
                        var showFullScreenImage by remember { mutableStateOf<String?>(null) }

                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF7F7F7))
                                    .clickable { showFullScreenImage = product.imageUrls[page] }
                            ) {
                                AppAsyncImage(
                                    url = resolveMediaUrl(product.imageUrls[page]),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        if (product.imageUrls.isNotEmpty()) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${product.imageUrls.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (showFullScreenImage != null) {
                            androidx.compose.ui.window.Dialog(
                                onDismissRequest = { showFullScreenImage = null },
                                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                        .clickable { showFullScreenImage = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    var scale by remember { mutableStateOf(1f) }
                                    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                                    AppAsyncImage(
                                        url = resolveMediaUrl(showFullScreenImage),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .transformable(
                                                state = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, panChange, _ ->
                                                    scale = (scale * zoomChange).coerceIn(1f, 3f)
                                                    offset += panChange
                                                }
                                            )
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = offset.x,
                                                translationY = offset.y
                                            ),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                val priceStr = product.price.toString()
                                val parts = priceStr.split(".")
                                val integerPart = parts[0]
                                val decimalPart = if (parts.size > 1 && parts[1] != "0") ".${parts[1]}" else ""

                                Text("¥", fontSize = 16.sp, color = Color(0xFFE93600), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                                Text(integerPart, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE93600))
                                Text(decimalPart, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE93600), modifier = Modifier.padding(bottom = 4.dp))
                                Spacer(Modifier.weight(1f))
                                Text(
                                    when (product.status) {
                                        "ON_SALE", "AVAILABLE" -> "在售"
                                        "OFF" -> "已下架"
                                        "SOLD" -> "已售出"
                                        else -> product.status
                                    },
                                    color = Color(0xFFFF5A1F),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(CircleShape).background(Color(0xFFFFE1D2)).padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Text(product.title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF241A16), lineHeight = 27.sp)
                            product.description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 21.sp) }
                            if (uiState.isOwner && product.status == "OFF" && !product.takedownReason.isNullOrBlank()) {
                                Text(
                                    text = "平台下架原因：${product.takedownReason}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFFF0F0))
                                        .padding(12.dp)
                                )
                            }
                            HorizontalDivider(color = Color(0xFFFFE1D2))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoChip("${product.category ?: "其他"}")
                                InfoChip("成色 ${product.condition ?: "未标注"}")
                                InfoChip(product.location ?: "校内面交")
                            }
                        }
                    }
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(46.dp).clip(CircleShape).background(
                                    Brush.linearGradient(listOf(Color(0xFFFFB000), Color(0xFFFF5A1F)))
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(product.sellerNickname.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(product.sellerNickname, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("已实名认证 · 校园交易更安心", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.Verified, null, tint = Color(0xFF1F8A70))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = Color(0xFF7B4A37),
        modifier = Modifier.clip(CircleShape).background(Color(0xFFFFF0E6)).padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
