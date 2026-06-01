package com.tps.ui.product

/**
 * 文件说明：商品模块界面，负责商品浏览、详情或发布流程的 Compose 展示。
 */

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import com.tps.ui.theme.AppAsyncImage
import com.tps.ui.theme.MarketBackground
import com.tps.ui.theme.MarketCard
import com.tps.ui.theme.MarketHeroCard
import com.tps.ui.theme.MarketOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishProductScreen(onBack: () -> Unit, viewModel: PublishProductViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("GOOD") }
    var location by remember { mutableStateOf("") }
    val priceValue = price.toDoubleOrNull()
    val canPublish = title.isNotBlank() && category.isNotBlank() && priceValue != null && priceValue > 0.0
    val categories = listOf("数码", "服装", "书籍", "家居", "运动", "其他")
    val conditions = listOf("NEW" to "全新", "LIKE_NEW" to "几乎全新", "GOOD" to "成色好", "FAIR" to "有使用痕迹")

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.addImages(uris)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onBack()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("发布闲置", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        bottomBar = {
            Surface(color = Color.White, tonalElevation = 8.dp) {
            Box(Modifier.fillMaxWidth().padding(12.dp)) {
                Button(
                    onClick = { viewModel.publish(title.trim(), description.trim(), priceValue ?: 0.0, category, condition, location.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && canPublish,
                    colors = ButtonDefaults.buttonColors(containerColor = MarketOrange)
                ) {
                    if (uiState.isLoading) CircularProgressIndicator(Modifier.size(18.dp))
                    else Text("立即发布，开始曝光")
                }
            }
            }
        }
    ) { padding ->
        MarketBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MarketHeroCard("卖掉闲置", "上传真实图片、写清成色，校内同学更愿意下单。")
            MarketCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("商品图片（最多9张）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.selectedImages) { uri ->
                    Box {
                        AppAsyncImage(url = uri.toString(), contentDescription = null,
                            modifier = Modifier.size(86.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
                        IconButton(onClick = { viewModel.removeImage(uri) },
                            modifier = Modifier.align(Alignment.TopEnd).size(20.dp)) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
                if (uiState.selectedImages.size < 9) {
                    item {
                        Box(Modifier.size(86.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFFFF0E6)).border(1.dp, Color(0xFFFFD1BF), RoundedCornerShape(18.dp)).clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, tint = MarketOrange)
                        }
                    }
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 100) title = it },
                label = { Text("标题，例如 iPad Air 九成新") },
                supportingText = { Text("${title.length}/100") },
                isError = title.length > 100,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            )
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述成色、配件、交易时间") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(18.dp))
            OutlinedTextField(
                value = price,
                onValueChange = { value -> price = value.filter { it.isDigit() || it == '.' }.take(10) },
                label = { Text("价格（元）") },
                supportingText = {
                    if (price.isNotBlank() && (priceValue == null || priceValue <= 0.0)) Text("请输入大于0的数字")
                },
                isError = price.isNotBlank() && (priceValue == null || priceValue <= 0.0),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            )
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("交易地点，如 图书馆门口") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
            }
            }

            MarketCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("分类", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                }
            }

            Text("成色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(conditions) { (value, label) ->
                    FilterChip(selected = condition == value, onClick = { condition = value }, label = { Text(label) })
                }
            }
            }
            }

            if (uiState.error != null) Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            if (!canPublish) {
                Text("填写标题、价格并选择分类后即可发布", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        }
    }
}
