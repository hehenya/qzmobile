package com.example.toolbox.message

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HeatmapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val chatType = intent.getIntExtra("chat_type", 1)
        val chatId = intent.getIntExtra("chat_id", 0)
        val token = TokenManager.get(this) ?: ""
        val dateString = intent.getStringExtra("date_string") ?: ""
        setContent {
            ToolBoxTheme {
                HeatmapScreen(token, chatType, chatId, parseYearMonth(dateString)) { finish() }
            }
        }
    }

    // 将日期字符串解析为 Calendar（保留年月，日设为1）
    private fun parseYearMonth(s: String): Calendar {
        val cal = Calendar.getInstance()
        if (s.isBlank()) return cal

        return try {
            val cleaned = s.replace("年", "-").replace("月", "-").replace("日", "")
            val parts = cleaned.split("-").filter { it.isNotBlank() }
            when {
                parts.size == 2 -> {
                    val month = parts[0].toIntOrNull() ?: return cal
                    cal.set(Calendar.MONTH, month - 1)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal
                }
                parts.size >= 3 -> {
                    val year = parts[0].toIntOrNull() ?: return cal
                    val month = parts[1].toIntOrNull() ?: return cal
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month - 1)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal
                }
                else -> cal
            }
        } catch (_: Exception) {
            cal
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(
    token: String, chatType: Int, chatId: Int,
    initialYearMonth: Calendar, onBack: () -> Unit
) {
    val viewModel: MessageDetailViewModel = viewModel(
        factory = MessageDetailViewModelFactory(token, chatType, chatId)
    )
    val todayCal = Calendar.getInstance()

    // 生成所有月份的 Calendar 列表（从2020年1月到当前月）
    val allMonths = remember {
        val list = mutableListOf<Calendar>()
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2020)
            set(Calendar.MONTH, 0) // 1月
            set(Calendar.DAY_OF_MONTH, 1)
        }
        while (startCal.before(todayCal) || startCal == todayCal) {
            list.add(startCal.clone() as Calendar)
            startCal.add(Calendar.MONTH, 1)
        }
        list
    }

    // 计算初始滚动位置
    val initialIndex = remember(initialYearMonth) {
        allMonths.indexOfFirst { cal ->
            cal.get(Calendar.YEAR) == initialYearMonth.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == initialYearMonth.get(Calendar.MONTH)
        }.coerceAtLeast(0)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    var currentVisibleMonth by remember { mutableStateOf(initialYearMonth) }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val index = listState.firstVisibleItemIndex
        if (index in allMonths.indices) {
            currentVisibleMonth = allMonths[index]
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        formatCalendar(currentVisibleMonth),
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pd ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(pd)
        ) {
            itemsIndexed(allMonths) { _, calendar ->
                MonthItem(
                    yearMonthCalendar = calendar,
                    viewModel = viewModel,
                    todayCal = todayCal
                )
            }
        }
    }
}

// 格式化 Calendar 为 "yyyy年M月"
private fun formatCalendar(cal: Calendar): String {
    val sdf = SimpleDateFormat("yyyy年M月", Locale.getDefault())
    return sdf.format(cal.time)
}

@Composable
private fun MonthItem(
    yearMonthCalendar: Calendar,
    viewModel: MessageDetailViewModel,
    todayCal: Calendar
) {
    // 调用 ViewModel 加载数据
    LaunchedEffect(Unit) {
        viewModel.loadActiveDays(yearMonthCalendar)
    }

    val activeDays by viewModel.activeDays.collectAsState()
    val loading by viewModel.isLoadingActiveDays.collectAsState()

    // 获取该月的天数和第一天是周几（周一=1,...周日=7）
    val maxDay = yearMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = with(yearMonthCalendar.clone() as Calendar) {
        set(Calendar.DAY_OF_MONTH, 1)
        get(Calendar.DAY_OF_WEEK) // 周日=1, 周一=2, ... 周六=7
    }
    // 转换为周一=1 ... 周日=7
    val firstDow = if (firstDayOfWeek == Calendar.SUNDAY) 7 else firstDayOfWeek - 1

    // 建立日期字符串到消息数的映射（格式 "yyyy-MM-dd"）
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val map = remember(activeDays) {
        val m = mutableMapOf<String, Int>()
        activeDays.forEach { day ->
            m[day.date] = day.msgCount
        }
        m
    }
    val max = remember(activeDays) { activeDays.maxOfOrNull { it.msgCount } ?: 1 }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            formatCalendar(yearMonthCalendar),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
        Spacer(Modifier.height(10.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        } else {
            val total = firstDow - 1 + maxDay
            val rows = (total + 6) / 7
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0..6) {
                            val d = r * 7 + c - firstDow + 2
                            Box(Modifier.weight(1f)) {
                                if (d in 1..maxDay) {
                                    val calClone = yearMonthCalendar.clone() as Calendar
                                    calClone.set(Calendar.DAY_OF_MONTH, d)
                                    val dateKey = dateFormat.format(calClone.time)
                                    val cnt = map[dateKey] ?: 0
                                    val isToday = isSameDay(calClone, todayCal)
                                    val isFuture = calClone.after(todayCal) && calClone.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH) && calClone.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)

                                    Box(
                                        Modifier
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(
                                                if (cnt > 0) Color(0xFF4DA6A6).copy(alpha = (0.2f + cnt.toFloat() / max * 0.8f).coerceIn(0.2f, 1f))
                                                else Color.Transparent
                                            )
                                            .then(if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$d", fontSize = 13.sp,
                                            color = when {
                                                isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                                cnt > 0 && cnt >= max * 0.5f -> Color.White
                                                isToday -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
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

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}