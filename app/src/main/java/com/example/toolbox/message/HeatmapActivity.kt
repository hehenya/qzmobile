package com.example.toolbox.message

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

class HeatmapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val chatType = intent.getIntExtra("chat_type", 1)
        val chatId = intent.getIntExtra("chat_id", 0)
        val token = TokenManager.get(this) ?: ""
        val dateString = intent.getStringExtra("date_string") ?: ""

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
        if (dateString.isBlank()) return YearMonth.now()
        return try {
            val cleaned = dateString
                .replace("年", "-")
                .replace("月", "-")
                .replace("日", "")
            val parts = cleaned.split("-").filter { it.isNotBlank() }
            if (parts.size == 2) {
                val month = parts[0].toIntOrNull() ?: return YearMonth.now()
                YearMonth.of(YearMonth.now().year, month)
            } else if (parts.size >= 3) {
                val year = parts[0].toIntOrNull() ?: return YearMonth.now()
                val month = parts[1].toIntOrNull() ?: return YearMonth.now()
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
    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value
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
                            text = currentYearMonth.format(
                                DateTimeFormatter.ofPattern("yyyy年M月")
                            ),
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 日历网格
                    val totalCells = firstDayOfWeek - 1 + daysInMonth
                    val rows = (totalCells + 6) / 7

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0..6) {
                                    val cellIndex = row * 7 + col
                                    val dayOfMonth = cellIndex - (firstDayOfWeek - 1) + 1

                                    Box(modifier = Modifier.weight(1f)) {
                                        if (dayOfMonth in 1..daysInMonth) {
                                            val date = currentYearMonth.atDay(dayOfMonth)
                                            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                            val msgCount = activeDaysMap[dateStr] ?: 0
                                            val isFuture = date.isAfter(today) && currentYearMonth == YearMonth.from(today)
                                            val isToday = date == today

                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                            msgCount == 0 -> Color.Transparent
                                                            else -> getHeatColor(msgCount, maxMsgCount)
                                                        }
                                                    )
                                                    .then(
                                                        if (isToday && msgCount == 0) {
                                                            Modifier.border(
                                                                1.5.dp,
                                                                MaterialTheme.colorScheme.primary,
                                                                CircleShape
                                                            )
                                                        } else Modifier
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = dayOfMonth.toString(),
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                        msgCount > 0 -> Color.White
                                                        isToday -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
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
}

private fun getHeatColor(msgCount: Int, maxCount: Int): Color {
    if (maxCount == 0) return Color(0xFF4CAF50).copy(alpha = 0.3f)
    val ratio = (msgCount.toFloat() / maxCount.toFloat()).coerceIn(0.2f, 1f)
    val green = 0xFF4CAF50
    return Color(green).copy(alpha = 0.25f + ratio * 0.75f)
}