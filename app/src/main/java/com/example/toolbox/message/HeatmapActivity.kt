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
        setContent {
            ToolBoxTheme {
                HeatmapScreen(token, chatType, chatId, parseYearMonth(dateString)) { finish() }
            }
        }
    }

    private fun parseYearMonth(s: String): YearMonth {
        if (s.isBlank()) return YearMonth.now()
        return try {
            val cleaned = s.replace("年", "-").replace("月", "-").replace("日", "")
            val parts = cleaned.split("-").filter { it.isNotBlank() }
            if (parts.size == 2) YearMonth.of(YearMonth.now().year, parts[0].toInt())
            else if (parts.size >= 3) YearMonth.of(parts[0].toInt(), parts[1].toInt())
            else YearMonth.now()
        } catch (_: Exception) { YearMonth.now() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(
    token: String, chatType: Int, chatId: Int,
    initialYearMonth: YearMonth, onBack: () -> Unit
) {
    val viewModel: MessageDetailViewModel = viewModel(
        factory = MessageDetailViewModelFactory(token, chatType, chatId)
    )
    val today = LocalDate.now()
    val todayYM = YearMonth.from(today)

    val allMonths = remember {
        val list = mutableListOf<YearMonth>()
        var ym = YearMonth.of(2020, 1)
        while (!ym.isAfter(todayYM)) {
            list.add(ym)
            ym = ym.plusMonths(1)
        }
        list
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = allMonths.indexOf(initialYearMonth).coerceAtLeast(0)
    )

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
                        currentVisibleMonth.format(DateTimeFormatter.ofPattern("yyyy年M月")),
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
            itemsIndexed(allMonths) { _, ym ->
                MonthItem(
                    yearMonth = ym,
                    viewModel = viewModel,
                    today = today,
                    todayYM = todayYM
                )
            }
        }
    }
}

@Composable
private fun MonthItem(
    yearMonth: YearMonth,
    viewModel: MessageDetailViewModel,
    today: LocalDate,
    todayYM: YearMonth
) {
    // ✅ 一次性加载，不重复请求
    LaunchedEffect(Unit) {
        viewModel.loadActiveDays(yearMonth)
    }

    val activeDays by viewModel.activeDays.collectAsState()
    val loading by viewModel.isLoadingActiveDays.collectAsState()

    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDow = yearMonth.atDay(1).dayOfWeek.value
    val map = remember(activeDays) { activeDays.associate { it.date to it.msgCount } }
    val max = remember(activeDays) { activeDays.maxOfOrNull { it.msgCount } ?: 1 }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            yearMonth.format(DateTimeFormatter.ofPattern("yyyy年M月")),
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
            val total = firstDow - 1 + daysInMonth
            val rows = (total + 6) / 7
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0..6) {
                            val d = r * 7 + c - firstDow + 2
                            Box(Modifier.weight(1f)) {
                                if (d in 1..daysInMonth) {
                                    val date = yearMonth.atDay(d)
                                    val key = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                    val cnt = map[key] ?: 0
                                    val isToday = date == today
                                    val isFuture = date.isAfter(today) && yearMonth == todayYM

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