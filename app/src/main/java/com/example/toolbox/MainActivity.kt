package com.example.toolbox

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.toolbox.music.MusicPlayerViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.example.toolbox.function.yunhu.yhbotmaker.BotManagerScreen
import com.example.toolbox.function.mouse.MouseSimulatorScreen
import com.example.toolbox.functionPage.HomeScreen
import com.example.toolbox.guide.GuideActivity
import com.example.toolbox.liFangCommunity.AuthManager
import com.example.toolbox.liFangCommunity.CubeNetworkManager
import com.example.toolbox.liFangCommunity.ProfileScreen_LF
import com.example.toolbox.message.MessageDetailActivity
import com.example.toolbox.message.MessageScreen
import com.example.toolbox.mine.ProfileScreen
import com.example.toolbox.mine.UserBottomSheet
import com.example.toolbox.music.MusicPlayerScreen
import com.example.toolbox.resourceLib.ResourceLibScreen
import com.example.toolbox.settings.UpdateDialog
import com.example.toolbox.ui.theme.ToolBoxTheme
import com.example.toolbox.utils.UpdateInfo
import com.example.toolbox.utils.checkForUpdateWithDetails
import com.example.toolbox.utils.getAppVersionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = this.getSharedPreferences("app_preferences", MODE_PRIVATE)
        val isFinishGuide = prefs.getBoolean("guideFinished", false)
        if (!isFinishGuide) {
            val intent = Intent(this, GuideActivity::class.java)
            this.startActivity(intent)
            finish()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DraftManager.init(applicationContext)
        setContent {
            ToolBoxTheme {
                MyApplicationApp()
            }
        }
    }
}

@Composable
fun MyApplicationApp() {
    val mainViewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    
    val musicPlayerViewModel: MusicPlayerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MusicPlayerViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )
    
    val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    var lastBackPressedTime by remember { mutableLongStateOf(0L) }
    
    var showAutoUpdateDialog by remember { mutableStateOf(false) }
    var autoUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var currentNotification by remember { mutableStateOf<InAppNotification?>(null) }
    val bannerVisible = currentNotification != null
    var token by remember { mutableStateOf(TokenManager.get(context)) }

    LaunchedEffect(Unit) {
        token?.let { mainViewModel.refreshUserInfo(it) }
    }
    
    LaunchedEffect(Unit) {
        Toast.makeText(context, "通知系统已启动", Toast.LENGTH_SHORT).show()
        
        // 先启动监听
        launch {
            NotificationManager.notifications.collect { notification ->
                Toast.makeText(context, "收到通知: ${notification.title}", Toast.LENGTH_SHORT).show()
                currentNotification = notification
                delay(4000)
                currentNotification = null
            }
        }
        
        // 延迟一下再发送测试通知，确保 collect 已经开始
        delay(1000)
        NotificationManager.show(
            InAppNotification(
                title = "测试通知",
                message = "如果你看到这条消息，横幅组件正常",
                chatId = null,
                chatType = null,
                avatarUrl = ""
            )
        )
    }
    val tokenListener = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "safeToken") {
                token = TokenManager.get(context)
                if (token != null) {
                    mainViewModel.refreshUserInfo(token!!)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        prefs.registerOnSharedPreferenceChangeListener(tokenListener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(tokenListener)
        }
    }

    val showChat = token != null

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    var isBottomBarVisible by remember { mutableStateOf(true) }

    val userInfo by mainViewModel.userInfo.collectAsState()
    val uiStatus by mainViewModel.uiStatus.collectAsState()
    val showDialog = uiStatus.showUserDialog
    val userName = userInfo.name
    val userId = userInfo.id
    val userAvatar = userInfo.avatar

    val showSidebar by mainViewModel.showSidebar.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                val delta = available.y
                if (delta < -15f && isBottomBarVisible) isBottomBarVisible = false
                if (delta > 15f && !isBottomBarVisible) isBottomBarVisible = true
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    val visibleAppDestinations = remember(showChat) {
        if (showChat) AppDestinations.entries.toList()
        else AppDestinations.entries.filter { it != AppDestinations.CHAT }
    }
    
    val isMainPage by remember(currentRoute) {
        derivedStateOf {
            val mainRoutes = visibleAppDestinations.map { it.route }
            currentRoute in mainRoutes
        }
    }
    
    val allDestinations = remember(visibleAppDestinations) {
        visibleAppDestinations.map { it as NavDestination } + 
        TopLevelDestinations.entries.map { it as NavDestination }
    }
    
    val selectedRoute by remember(currentDestination, allDestinations) {
        derivedStateOf {
            allDestinations.find { item ->
                currentDestination?.hierarchy?.any { it.route == item.route } == true
            }?.route
        }
    }
    
    // 全局通知横幅显示状态
    

    
    
    if (showAutoUpdateDialog && autoUpdateInfo != null) {
        UpdateDialog(
            updateInfo = autoUpdateInfo!!,
            currentVersion = context.getAppVersionInfo().versionName,
            onDismiss = { showAutoUpdateDialog = false },
            onConfirm = {
                val intent = Intent(Intent.ACTION_VIEW, autoUpdateInfo?.releaseUrl?.toUri())
                context.startActivity(intent)
                showAutoUpdateDialog = false
            }
        )
    }
    
    if (showSidebar) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.fillMaxWidth(0.75f)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(visibleAppDestinations) { item ->
                            NavigationDrawerItem(
                                label = { Text(item.label) },
                                selected = item.route == selectedRoute,
                                icon = { Icon(item.icon, contentDescription = null) },
                                onClick = {
                                    navController.navigateToTopLevel(item.route)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }

                        items(TopLevelDestinations.entries) { item ->
                            NavigationDrawerItem(
                                label = { Text(item.label) },
                                selected = item.route == selectedRoute,
                                icon = { Icon(item.icon, contentDescription = null) },
                                onClick = {
                                    navController.navigateToTopLevel(item.route)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            },
            gesturesEnabled = true,
            content = {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainContent(
                        nestedScrollConnection = nestedScrollConnection,
                        navController = navController,
                        mainViewModel = mainViewModel,
                        musicPlayerViewModel = musicPlayerViewModel,
                        drawerState = drawerState,
                        scope = scope,
                        isMainPage = isMainPage,
                        isBottomBarVisible = isBottomBarVisible,
                        visibleAppDestinations = visibleAppDestinations,
                        selectedRoute = selectedRoute,
                        showDialog = showDialog,
                        userId = userId,
                        userName = userName,
                        userAvatar = userAvatar,
                        onUserDialogDismiss = { mainViewModel.changeUserDialogStatus(false) }
                    )
                    
                    // 全局通知横幅
                                        // 全局通知横幅
                                        // 全局通知横幅
InAppNotificationBanner(
    notification = currentNotification,
    visible = bannerVisible,
    onDismiss = { currentNotification = null },
    onClick = { notif ->
        currentNotification = null
        if (notif.chatId != null && notif.chatType != null) {
            val intent = Intent(context, MessageDetailActivity::class.java).apply {
                putExtra("chat_type", notif.chatType)
                putExtra("chat_id", notif.chatId)
            }
            context.startActivity(intent)
        }
    },
    modifier = Modifier
        .zIndex(100f)
        .align(Alignment.TopCenter)
        .padding(horizontal = 12.dp, vertical = 8.dp)
)
                }
            },
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            MainContent(
                nestedScrollConnection = nestedScrollConnection,
                navController = navController,
                mainViewModel = mainViewModel,
                musicPlayerViewModel = musicPlayerViewModel,
                drawerState = drawerState,
                scope = scope,
                isMainPage = isMainPage,
                isBottomBarVisible = isBottomBarVisible,
                visibleAppDestinations = visibleAppDestinations,
                selectedRoute = selectedRoute,
                showDialog = showDialog,
                userId = userId,
                userName = userName,
                userAvatar = userAvatar,
                onUserDialogDismiss = { mainViewModel.changeUserDialogStatus(false) }
            )
            
            // 全局通知横幅
                        // 全局通知横幅
                        InAppNotificationBanner(
                            notification = currentNotification,
                            visible = bannerVisible,
                            onDismiss = { currentNotification = null },
                            onClick = { notif ->
                                currentNotification = null
                                if (notif.chatId != null && notif.chatType != null) {
                                    val intent = Intent(context, MessageDetailActivity::class.java).apply {
                                        putExtra("chat_type", notif.chatType)
                                        putExtra("chat_id", notif.chatId)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier
                                .zIndex(100f)
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    
    BackHandler(enabled = showDialog) {
        mainViewModel.changeUserDialogStatus(false)
    }
    
    BackHandler(enabled = !showDialog && !drawerState.isOpen) {
        val currentRoute = currentDestination?.route ?: return@BackHandler
        val defaultStartRoute = getStartDestination(context)
    
        val secondaryRoutes = setOf(AppDestinations.CHAT.route, AppDestinations.RESOURCE.route, AppDestinations.PROFILE.route)
        val isSecondary = currentRoute in secondaryRoutes
    
        if (isSecondary) {
            navController.navigateToTopLevel(AppDestinations.HOME.route)
            return@BackHandler
        }
    
        val topLevelRoutes = setOf(
            AppDestinations.HOME.route,
            TopLevelDestinations.LFCommunity.route,
            TopLevelDestinations.YHBotMaker.route,
            TopLevelDestinations.MusicPlayer.route,
            TopLevelDestinations.MouseSimulator.route
        )
        val isTopLevel = currentRoute in topLevelRoutes

        if (!isTopLevel) {
            if (!navController.popBackStack()) {
                navController.navigateToTopLevel(defaultStartRoute)
            }
            return@BackHandler
        }
    
        if (currentRoute == defaultStartRoute) {
            val exitConfirmationEnabled = prefs.getBoolean("exit_confirmation", false)
            if (!exitConfirmationEnabled) {
                (context as? Activity)?.finish()
            } else {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedTime < 2000) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressedTime = now
                    Toast.makeText(context, "再按一次退出应用", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            navController.navigateToTopLevel(defaultStartRoute)
        }
    }
}

fun androidx.navigation.NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MainContent(
    nestedScrollConnection: NestedScrollConnection,
    navController: androidx.navigation.NavHostController,
    mainViewModel: MainViewModel,
    musicPlayerViewModel: MusicPlayerViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    isMainPage: Boolean,
    isBottomBarVisible: Boolean,
    visibleAppDestinations: List<AppDestinations>,
    selectedRoute: String?,
    showDialog: Boolean,
    userId: Int,
    userName: String,
    userAvatar: String,
    onUserDialogDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(nestedScrollConnection)
    ) {
        MainContentNavHost(
            navController = navController,
            mainViewModel = mainViewModel,
            musicPlayerViewModel = musicPlayerViewModel,
            drawerState = drawerState,
            scope = scope,
            modifier = Modifier.fillMaxSize()
        )

        if (isMainPage) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                        NavigationBar {
                            visibleAppDestinations.forEach { item ->
                                val isSelected = item.route == selectedRoute
                                NavigationBarItem(
                                    icon = {
                                        Crossfade(targetState = isSelected) { selected ->
                                            Icon(
                                                imageVector = if (selected) item.icon else item.iconOutlined,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    label = {
                                        AnimatedVisibility(
                                            visible = isSelected,
                                            enter = fadeIn(animationSpec = tween(200)) +
                                                    scaleIn(initialScale = 0.5f, animationSpec = tween(200)),
                                            exit = fadeOut(animationSpec = tween(150)) +
                                                    scaleOut(targetScale = 0.5f, animationSpec = tween(150))
                                        ) {
                                            Text(item.label)
                                        }
                                    },
                                    selected = isSelected,
                                    onClick = {
                                        navController.navigateToTopLevel(item.route)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        UserBottomSheet(
            show = showDialog,
            userId = userId,
            userName = userName,
            userAvatar = userAvatar,
            onDismiss = onUserDialogDismiss
        )
    }
}

@Composable
fun MainContentNavHost(
    navController: androidx.navigation.NavHostController,
    mainViewModel: MainViewModel,
    musicPlayerViewModel: MusicPlayerViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val onMenuClick: () -> Unit = remember { { scope.launch { drawerState.open() } } }

    LaunchedEffect(Unit) {
        AuthManager.initialize(context.applicationContext)
        CubeNetworkManager.initialize(context.applicationContext)
    }

    NavHost(
        navController = navController,
        startDestination = getStartDestination(context),
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable(AppDestinations.HOME.route) {
            HomeScreen(
                onMenuClick = onMenuClick,
                mainViewModel = mainViewModel
            )
        }
        composable(AppDestinations.CHAT.route) {
            MessageScreen(
                onMenuClick = onMenuClick,
                mainViewModel = mainViewModel
            )
        }
        composable(AppDestinations.RESOURCE.route) {
            ResourceLibScreen(
                onMenuClick = onMenuClick,
                mainViewModel = mainViewModel
            )
        }
        composable(AppDestinations.PROFILE.route) {
            ProfileScreen(
                onMenuClick = onMenuClick,
                mainViewModel = mainViewModel
            )
        }
        composable(TopLevelDestinations.LFCommunity.route) {
            ProfileScreen_LF(
                onMenuClick = onMenuClick,
            )
        }
        composable(TopLevelDestinations.YHBotMaker.route) {
            BotManagerScreen(
                isMain = true,
                onMenuClick = onMenuClick,
            )
        }
        composable(TopLevelDestinations.MusicPlayer.route) {
            MusicPlayerScreen(
                onMenuClick = onMenuClick,
                viewModel = musicPlayerViewModel,
            )
        }
        composable(TopLevelDestinations.MouseSimulator.route) {
            MouseSimulatorScreen(
                isMain = true,
                onMenuClick = onMenuClick,
            )
        }
    }
}

interface NavDestination {
    val route: String
    val label: String
    val icon: ImageVector
    val description: String
}

enum class TopLevelDestinations(
    override val route: String,
    override val label: String,
    override val icon: ImageVector,
    override val description: String
) : NavDestination {
    LFCommunity("lfcommunity", "立方论坛", Icons.Default.ChatBubbleOutline, "主要作为立方论坛客户端"),
    YHBotMaker("yhbotmaker", "YHBotMaker", Icons.Default.Android, "主要作为云湖机器人制作器"),
    MusicPlayer("musicplayer", "音乐", Icons.Default.MusicNote, "本地音乐播放器"),
    MouseSimulator("mousesimulator", "模拟鼠标", Icons.Default.Computer, "屏幕鼠标模拟器")
}

enum class AppDestinations(
    override val route: String,
    override val label: String,
    override val icon: ImageVector,
    val iconOutlined: ImageVector,
    override val description: String
) : NavDestination {
    HOME("home", "主页", Icons.Filled.Home, Icons.Outlined.Home, "主要使用轻昼实用功能"),
    CHAT("chat", "会话", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, ""),
    RESOURCE("resource", "资源库", Icons.Filled.Inbox, Icons.Outlined.Inbox, ""),
    PROFILE("profile", "我", Icons.Filled.Person, Icons.Outlined.Person, "");
}

fun getStartDestination(context: Context): String {
    val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    val defaultStartPageName = prefs.getString("default_start_page", "主页") ?: "主页"
    
    return when (defaultStartPageName) {
        "主页" -> AppDestinations.HOME.route
        "立方论坛" -> TopLevelDestinations.LFCommunity.route
        "YHBotMaker" -> TopLevelDestinations.YHBotMaker.route
        "音乐" -> TopLevelDestinations.MusicPlayer.route
        "模拟鼠标" -> TopLevelDestinations.MouseSimulator.route
        else -> AppDestinations.HOME.route
    }
}

/**
 * 应用内通知横幅组件，仿抖音风格：
 * 左侧圆形头像，右侧上方标题（会话名/发送者），下方消息内容预览。
 */
@Composable
fun InAppNotificationBanner(
    notification: InAppNotification?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onClick: (InAppNotification) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && notification != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        notification?.let { notif ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(notif) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 头像
                    AsyncImage(
                        model = notif.avatarUrl,
                        contentDescription = "头像",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // 文字区域
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = notif.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = notif.message,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 关闭按钮
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}