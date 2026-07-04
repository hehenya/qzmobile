// com/example/toolbox/message/HeatmapActivity.kt
package com.example.toolbox.message

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.TokenManager
import com.example.toolbox.data.ActiveDay
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class HeatmapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val chatType = intent.getIntExtra("chat_type", 1)
        val chatId = intent.getIntExtra("chat_id", 0)
        val token = TokenManager.get(this) ?: ""
        val dateString = intent.getStringExtra("date_string") ?: ""
        
        // 解析日期获取年月
        val initialYearMonth = parseYearMonth(dateString)
        
        setContent {
            ToolBoxTheme {
                HeatmapScreen(
                    token = token,
                    chatType = chatType,
                    chatId = chatId,
                    initialYearMonth = initialYearMonth,
                    onBack = { finish() }
                )
            }
        }
    }
    
    private fun parseYearMonth(dateString: String): YearMonth {
        return try {
            val cleanedDate = dateString
                .replace("年", "-")
                .replace("月", "-")
                .replace("日", "")
            val parts = cleanedDate.split("-")
            if (parts.size >= 2) {
                val year = parts.getOrNull(0)?.toIntOrNull() ?: YearMonth.now().year
                val month = parts.lastOrNull()?.toIntOrNull() ?: YearMonth.now().monthValue
                YearMonth.of(year, month)
            } else {
                YearMonth.now()
            }
        } catch (e: Exception) {
            YearMonth.now()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(
    token: String,
    chatType: Int,
    chatId: Int,
    initialYearMonth: YearMonth,
    onBack: () -> Unit
) {
    val viewModel: MessageDetailViewModel = viewModel(
        factory = MessageDetailViewModelFactory(token, chatType, chatId)
    )
    
    var currentYearMonth by remember { mutableStateOf(initialYearMonth) }
    val activeDays by viewModel.activeDays.collectAsState()
    val isLoading by viewModel.isLoadingActiveDays.collectAsState()
    
    val today = LocalDate.now()
    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value // 1=周一, 7=周日
    val activeDaysMap = remember(activeDays) { activeDays.associate { it.date to it.msgCount } }
    val maxMsgCount = remember(activeDays) { activeDays.maxOfOrNull { it.msgCount } ?: 1 }
    
    LaunchedEffect(currentYearMonth) {
        viewModel.loadActiveDays(currentYearMonth)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = {
                            currentYearMonth = currentYearMonth.minusMonths(1)
                        }) {
                            Text("◀", fontSize = 20.sp)
                        }
                        
                        Text(
                            text = currentYearMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月")),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        IconButton(
                            onClick = {
                                val nextMonth = currentYearMonth.plusMonths(1)
                                if (!nextMonth.isAfter(YearMonth.from(today))) {
                                    currentYearMonth = nextMonth
                                }
                            },
                            enabled = currentYearMonth.isBefore(YearMonth.from(today))
                        ) {
                            Text("▶", fontSize = 20.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 统计信息卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                label = "活跃天数",
                                value = "${activeDays.size}",
                                modifier = Modifier.weight(1f)
                            )
                            StatItem(
                                label = "总消息",
                                value = "${activeDays.sumOf { it.msgCount }}",
                                modifier = Modifier.weight(1f)
                            )
                            StatItem(
                                label = "日均消息",
                                value = if (activeDays.isNotEmpty()) 
                                    "${activeDays.sumOf { it.msgCount } / activeDays.size}" 
                                else "0",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 星期标题
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 热力图网格
                    val totalCells = firstDayOfWeek - 1 + daysInMonth
                    val rows = (totalCells + 6) / 7
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (col in 0..6) {
                                    val cellIndex = row * 7 + col
                                    val dayOfMonth = cellIndex - (firstDayOfWeek - 1) + 1
                                    
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (dayOfMonth in 1..daysInMonth) {
                                            val date = currentYearMonth.atDay(dayOfMonth)
                                            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                            val msgCount = activeDaysMap[dateStr] ?: 0
                                            val isFuture = date.isAfter(today)
                                            val isToday = date == today
                                            
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            when {
                                                                isFuture -> Color.Gray.copy(alpha = 0.1f)
                                                                msgCount > 0 -> getHeatmapColor(msgCount, maxMsgCount)
                                                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                            }
                                                        )
                                                        .then(
                                                            if (isToday) {
                                                                Modifier.border(
                                                                    2.dp,
                                                                    MaterialTheme.colorScheme.primary,
                                                                    RoundedCornerShape(8.dp)
                                                                )
                                                            } else {
                                                                Modifier
                                                            }
                                                        )
                                                        .clickable {
                                                            // 点击某一天可以查看当天的消息（可选功能）
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = dayOfMonth.toString(),
                                                        fontSize = 14.sp,
                                                        fontWeight = if (msgCount > 0 || isToday) FontWeight.Bold else FontWeight.Normal,
                                                        color = when {
                                                            isFuture -> Color.Gray.copy(alpha = 0.3f)
                                                            msgCount > maxMsgCount * 0.7f -> Color.White
                                                            msgCount > 0 -> Color.White.copy(alpha = 0.9f)
                                                            isToday -> MaterialTheme.colorScheme.primary
                                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                                
                                                if (msgCount > 0) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "$msgCount",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 图例
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("少", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        listOf(0.1f, 0.3f, 0.5f, 0.7f, 1f).forEach { intensity ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Color(
                                            red = 0.1f,
                                            green = 0.6f,
                                            blue = 0.1f,
                                            alpha = intensity
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("多", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 活跃日期列表
                    if (activeDays.isNotEmpty()) {
                        Text(
                            "活跃详情",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(activeDays) { day ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = day.date,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${day.msgCount} 条消息",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
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
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

fun getHeatmapColor(msgCount: Int, maxCount: Int): Color {
    if (maxCount == 0) return Color(0.1f, 0.6f, 0.1f, 0.1f)
    val intensity = (msgCount.toFloat() / maxCount.toFloat()).coerceIn(0.1f, 1.0f)
    return Color(
        red = 0.1f,
        green = 0.6f,
        blue = 0.1f,
        alpha = intensity
    )
}