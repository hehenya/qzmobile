package com.example.toolbox.message

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.TokenManager
import com.example.toolbox.community.UserInfoActivity
import com.example.toolbox.community.uploadImage
import com.example.toolbox.data.GroupInfo
import com.example.toolbox.settings.SettingsGroup
import com.example.toolbox.settings.SettingsItemCell
import com.example.toolbox.settings.SettingsCustomItem
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- 新增数据类 ----------


// ---------- CapsuleTabBar（参考项目样式） ----------
@Composable
fun CapsuleTabBar(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return

    val selectedIndex = selectedTabIndex.coerceIn(tabs.indices)
    val horizontalInset = 6.dp
    val selectionMotion = tween<Dp>(
        durationMillis = 260,
        easing = FastOutSlowInEasing
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = horizontalInset, vertical = 6.dp)
    ) {
        val tabWidth = maxWidth / tabs.size
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = selectionMotion,
            label = "capsule tab indicator offset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
        )

        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "capsule tab text color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onTabSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ---------- 群信息页面 ----------
class GroupInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (TokenManager.get(this) == null) { Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show(); finish(); return }
        enableEdgeToEdge()
        val shareKey = intent.data?.getQueryParameter("key")
        val groupId = if (shareKey != null) -1 else intent.getIntExtra("group_id", -1)
        val initialGroupInfo = if (intent.hasExtra("group_name")) GroupInfo(id = groupId, name = intent.getStringExtra("group_name") ?: "", avatarUrl = intent.getStringExtra("group_avatar") ?: "", description = intent.getStringExtra("group_description") ?: "", isPrivate = intent.getBooleanExtra("group_is_private", false), membersCount = intent.getIntExtra("group_members_count", 0), createdAt = intent.getStringExtra("group_created_at") ?: "", creator = null) else null
        setContent { ToolBoxTheme { val token = TokenManager.get(this); val viewModel: GroupInfoViewModel = viewModel(factory = token?.let { GroupInfoViewModelFactory(it, groupId, initialGroupInfo, shareKey) }); GroupInfoScreen(viewModel = viewModel, onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(viewModel: GroupInfoViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var isUploadingBg by remember { mutableStateOf(false) }
    var bgProgress by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPickingBackground by remember { mutableStateOf(false) }
    var previewImageUri by remember { mutableStateOf<Uri?>(null) }
    var previewBgUrl by remember { mutableStateOf<String?>(null) }

    // 新增状态：标签栏选择与标签管理弹窗
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("信息", "成员", "媒体")
    var showTagManageDialog by remember { mutableStateOf(false) }

    // 媒体筛选类型（由 ViewModel 提供，这里假设存在）
    val mediaType by viewModel.mediaType.collectAsState()
    val mediaList by viewModel.mediaList.collectAsState()
    val isLoadingMedia by viewModel.isLoadingMedia.collectAsState()
    val mediaPage by viewModel.mediaPage.collectAsState()
    val mediaTotalPages by viewModel.mediaTotalPages.collectAsState()

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (isPickingBackground) {
                // --- 背景上传（带进度） ---
                previewImageUri = uri
                isUploadingBg = true
                scope.launch {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                        val tempFile = java.io.File(context.cacheDir, "bg_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }
                        val token = TokenManager.get(context) ?: ""
                        val imageUrl = uploadImage(tempFile.absolutePath, token, 3) { progress ->
                            bgProgress = progress / 100f
                        }
                        isUploadingBg = false
                        if (imageUrl != null) {
                            previewBgUrl = imageUrl
                        } else {
                            Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show()
                        }
                        tempFile.delete()
                    } catch (e: Exception) {
                        isUploadingBg = false
                        Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                scope.launch {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                        val tempFile = java.io.File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }
                        val token = TokenManager.get(context) ?: ""
                        val imageUrl = uploadImage(tempFile.absolutePath, token, 3) {}
                        if (imageUrl != null) {
                            viewModel.updateEditingAvatarUrl(imageUrl)
                            Toast.makeText(context, "头像上传成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show()
                        }
                        tempFile.delete()
                    } catch (e: Exception) {
                        Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel) { viewModel.toastMessage.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }
    LaunchedEffect(viewModel) { viewModel.joinSuccess.collect { onBack() } }

    // 原有各种弹窗
    if (uiState.showLeaveDialog) {
        AlertDialog(onDismissRequest = { viewModel.hideLeaveDialog() }, title = { Text("退出群聊") }, text = { Text("确定要退出该群聊吗？") },
            confirmButton = { Button(onClick = { viewModel.leaveGroup(onBack) }, enabled = !uiState.isLeaving) { if (uiState.isLeaving) CircularProgressIndicator(Modifier.size(16.dp)) else Text("确定") } },
            dismissButton = { TextButton(onClick = { viewModel.hideLeaveDialog() }) { Text("取消") } })
    }
    if (uiState.showDissolveDialog) {
        AlertDialog(onDismissRequest = { viewModel.hideDissolveDialog() }, title = { Text("解散群聊") }, text = { Text("确定要解散该群聊吗？此操作不可撤销！") },
            confirmButton = { Button(onClick = { viewModel.dissolveGroup(onBack) }, enabled = !uiState.isDissolving, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { if (uiState.isDissolving) CircularProgressIndicator(Modifier.size(16.dp)) else Text("解散") } },
            dismissButton = { TextButton(onClick = { viewModel.hideDissolveDialog() }) { Text("取消") } })
    }
    // 创建/编辑标签对话框（仍保留在 ViewModel 控制）
    if (uiState.showTagDialog) {
        AlertDialog(onDismissRequest = { viewModel.hideTagDialog() }, title = { Text(if (uiState.editingTag != null) "编辑标签" else "创建标签") }, text = {
            Column {
                OutlinedTextField(value = uiState.newTagName, onValueChange = { viewModel.updateNewTagName(it) }, label = { Text("标签名称") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = uiState.newTagColor, onValueChange = { viewModel.updateNewTagColor(it) }, label = { Text("颜色 (如 #FF6B6B)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp)); Text("预览:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(4.dp), color = try { Color(uiState.newTagColor.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.primary }) { Text(uiState.newTagName.ifEmpty { "标签" }, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, color = Color.White) }
            }
        }, confirmButton = { Button(onClick = { val t = uiState.editingTag; if (t != null) viewModel.editTag(t.id, uiState.newTagName, uiState.newTagColor) else viewModel.createTag(uiState.newTagName, uiState.newTagColor) }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { viewModel.hideTagDialog() }) { Text("取消") } })
    }

    // 群标签管理弹窗（新增）
    if (showTagManageDialog) {
        AlertDialog(
            onDismissRequest = { showTagManageDialog = false },
            title = { Text("群标签管理") },
            text = {
                if (uiState.isLoadingTags) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn {
                        items(uiState.tags) { tag ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = try { Color(tag.color.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.primary },
                                        modifier = Modifier.size(12.dp)
                                    ) {}
                                    Spacer(Modifier.width(12.dp))
                                    Text(tag.name)
                                }
                                Row {
                                    IconButton(onClick = {
                                        showTagManageDialog = false
                                        viewModel.showTagDialog(tag)
                                    }) { Icon(Icons.Default.Edit, "编辑") }
                                    IconButton(onClick = { viewModel.deleteTag(tag.id) }) { Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showTagManageDialog = false
                    viewModel.showTagDialog()
                }) { Text("添加标签") }
            },
            dismissButton = { TextButton(onClick = { showTagManageDialog = false }) { Text("关闭") } }
        )
    }

    if (uiState.showEditDialog) {
        AlertDialog(onDismissRequest = { viewModel.hideEditDialog() }, title = { Text("编辑群信息") }, text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isPickingBackground = false; imagePickerLauncher.launch("image/*") }) {
                        if (uiState.editingAvatarUrl.isNotEmpty()) AsyncImage(model = if (uiState.editingAvatarUrl.startsWith("http")) uiState.editingAvatarUrl else "${ApiAddress}uploads/${uiState.editingAvatarUrl}", contentDescription = "群头像预览", contentScale = ContentScale.Crop, modifier = Modifier.size(60.dp).clip(CircleShape))
                        else Icon(Icons.Default.Add, contentDescription = "选择头像")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = uiState.editingName, onValueChange = { viewModel.updateEditingName(it) }, label = { Text("群名称") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = uiState.editingDescription, onValueChange = { viewModel.updateEditingDescription(it) }, label = { Text("群简介") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().padding(16.dp).clickable { viewModel.updateEditingJoinVerification(!uiState.editingJoinVerification) }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, "进群审核", Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) { Text("进群审核", style = MaterialTheme.typography.titleMedium); Text("新成员加入需要管理员审核", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = uiState.editingJoinVerification, onCheckedChange = null, thumbContent = { Icon(if (uiState.editingJoinVerification) Icons.Default.Check else Icons.Default.Close, null, Modifier.size(SwitchDefaults.IconSize), tint = if (uiState.editingJoinVerification) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest) })
                }
                // 允许分享
                Row(Modifier.fillMaxWidth().padding(16.dp).clickable { viewModel.updateEditingShareEnabled(!uiState.editingShareEnabled) }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, "允许分享", Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) { Text("允许分享", style = MaterialTheme.typography.titleMedium); Text("允许成员生成分享链接邀请他人加入", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = uiState.editingShareEnabled, onCheckedChange = null, thumbContent = { Icon(if (uiState.editingShareEnabled) Icons.Default.Check else Icons.Default.Close, null, Modifier.size(SwitchDefaults.IconSize), tint = if (uiState.editingShareEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest) })
                }
                // 私有/公开
                Row(Modifier.fillMaxWidth().padding(16.dp).clickable { viewModel.updateEditingIsPrivate(!uiState.editingIsPrivate) }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (uiState.editingIsPrivate) Icons.Default.Lock else Icons.Default.Public, "群类型", Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) { Text("私有群", style = MaterialTheme.typography.titleMedium); Text(if (uiState.editingIsPrivate) "仅群成员可查看和搜索" else "所有人可搜索和加入", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = uiState.editingIsPrivate, onCheckedChange = null, thumbContent = { Icon(if (uiState.editingIsPrivate) Icons.Default.Check else Icons.Default.Close, null, Modifier.size(SwitchDefaults.IconSize), tint = if (uiState.editingIsPrivate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest) })
                }
            }
        }, confirmButton = { Button(onClick = { viewModel.editGroupInfo(uiState.editingName, uiState.editingDescription, uiState.editingAvatarUrl, uiState.editingJoinVerification, uiState.editingShareEnabled, uiState.editingIsPrivate); viewModel.hideEditDialog() }, enabled = !uiState.isEditing) { if (uiState.isEditing) CircularProgressIndicator(Modifier.size(16.dp)) else Text("保存") } },
            dismissButton = { TextButton(onClick = { viewModel.hideEditDialog() }) { Text("取消") } })
    }

    // 背景预览弹窗
    if (previewImageUri != null) {
        val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
        val now = sdf.format(Date())
        AlertDialog(
            onDismissRequest = { previewImageUri = null; previewBgUrl = null; isPickingBackground = false },
            title = { Text("预览聊天背景") },
            text = {
                Column {
                    if (isUploadingBg) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(progress = { bgProgress })
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("上传中...")
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(350.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(model = previewImageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Bottom) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)) {
                                        Text("你知道轻昼可以调节聊天背景吗", modifier = Modifier.padding(8.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)) {
                                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(8.dp)) {
                                            Text("选择图片后就可以设置，试试吧", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text(now, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)) {
                                        Text("效果预览", modifier = Modifier.padding(8.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("以上为背景效果预览", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bgUrl = previewBgUrl
                        previewImageUri = null; previewBgUrl = null
                        if (bgUrl != null && uiState.group != null) {
                            scope.launch {
                                val token = TokenManager.get(context) ?: ""
                                setChatBackground(token, 2, uiState.group!!.id, bgUrl) { success ->
                                    Toast.makeText(context, if (success) "背景设置成功" else "背景设置失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        isPickingBackground = false
                    },
                    enabled = previewBgUrl != null
                ) { Text("确认设置") }
            },
            dismissButton = { TextButton(onClick = { previewImageUri = null; previewBgUrl = null; isPickingBackground = false }) { Text("取消") } }
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(title = { Text("群聊信息") }, navigationIcon = { FilledTonalIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            actions = {
                if (uiState.isJoined && uiState.myRole > 0 && uiState.group != null) {
                    IconButton(onClick = { val intent = Intent(context, JoinRequestsActivity::class.java); intent.putExtra("group_id", uiState.group!!.id); intent.putExtra("group_name", "${uiState.group!!.name} - 入群申请"); context.startActivity(intent) }) { Icon(Icons.Default.Group, contentDescription = "入群申请") }
                }
                var showMenu by remember { mutableStateOf(false) }
                var showShareDialog by remember { mutableStateOf(false) }
                if (uiState.isJoined) {
                    // 三点菜单包含所有管理功能
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (uiState.myRole > 0) {
                                DropdownMenuItem(text = { Text("编辑群信息") }, onClick = { showMenu = false; viewModel.showEditDialog() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                            }
                            if (uiState.group?.shareEnabled != false) {
                                DropdownMenuItem(text = { Text("分享群聊") }, onClick = { showMenu = false; showShareDialog = true }, leadingIcon = { Icon(Icons.Default.Share, null) })
                            }
                            DropdownMenuItem(text = { Text("聊天背景") }, onClick = { showMenu = false; isPickingBackground = true; imagePickerLauncher.launch("image/*") }, leadingIcon = { Icon(Icons.Default.Image, null) })
                            if (uiState.myRole > 0) {
                                DropdownMenuItem(text = { Text("群标签管理") }, onClick = { showMenu = false; showTagManageDialog = true }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                            }
                            if (uiState.myRole == 2) {
                                DropdownMenuItem(text = { Text("解散群聊") }, onClick = { showMenu = false; viewModel.showDissolveDialog() }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                            } else {
                                DropdownMenuItem(text = { Text("退出群聊") }, onClick = { showMenu = false; viewModel.showLeaveDialog() }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) })
                            }
                        }
                    }
                }
                if (showShareDialog && uiState.group != null) {
                    LaunchedEffect(Unit) { viewModel.createShareLink(expireHours = 0) {} }
                    AlertDialog(onDismissRequest = { showShareDialog = false; viewModel.clearShareLinks() }, title = { Text("分享群聊") }, text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("外链", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(value = uiState.shareUrl, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { val cm = context.getSystemService(android.content.ClipboardManager::class.java); cm?.setPrimaryClip(android.content.ClipData.newPlainText("share_url", uiState.shareUrl)); Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.ContentCopy, contentDescription = "复制外链") } }, singleLine = true)
                            Text("内链", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val internalUrl = "qz://group?key=${uiState.shareKey}"
                            OutlinedTextField(value = internalUrl, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { val cm = context.getSystemService(android.content.ClipboardManager::class.java); cm?.setPrimaryClip(android.content.ClipData.newPlainText("share_url", internalUrl)); Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.ContentCopy, contentDescription = "复制内链") } }, singleLine = true)
                            if (uiState.isGeneratingShare) LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }, confirmButton = {}, dismissButton = { TextButton(onClick = { showShareDialog = false; viewModel.clearShareLinks() }) { Text("关闭") } })
                }
            })
    }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.isLoading && uiState.group == null) { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }
            else if (uiState.group != null) {
                Column(Modifier.fillMaxSize()) {
                    CapsuleTabBar(
                        tabs = tabs,
                        selectedTabIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    when (selectedTab) {
                        0 -> InfoTab(uiState, viewModel, context, innerPadding)
                        1 -> MembersTab(uiState, context, innerPadding)
                        2 -> MediaTab(mediaType, mediaList, isLoadingMedia, mediaPage, mediaTotalPages, viewModel, uiState, innerPadding)
                    }
                }
            } else if (uiState.error != null) { Text("错误: ${uiState.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center)) }
        }
    }
}

@Composable
private fun InfoTab(uiState: GroupInfoUiState, viewModel: GroupInfoViewModel, context: android.content.Context, innerPadding: PaddingValues) {
    PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = { viewModel.refresh() }, modifier = Modifier.fillMaxSize()) {
        val group = uiState.group!!
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 16.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(model = if (group.avatarUrl.startsWith("http")) group.avatarUrl else "${ApiAddress}uploads/${group.avatarUrl}", contentDescription = "群头像", contentScale = ContentScale.Crop, modifier = Modifier.size(100.dp).clip(CircleShape))
                    Spacer(Modifier.height(16.dp)); Text(group.name, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("群号: ${group.id}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }
            item { SettingsGroup(title = "群聊信息", items = listOf(
                { SettingsItemCell(icon = Icons.Default.Person, title = "成员数", subtitle = "${group.membersCount} 名成员", onClick = { if (uiState.isJoined) context.startActivity(Intent(context, GroupMembersActivity::class.java).apply { putExtra("group_id", group.id) }) }) },
                { SettingsItemCell(icon = Icons.Default.DateRange, title = "创建时间", subtitle = formatGroupTime(group.createdAt), onClick = {}) },
                { if (group.isPrivate) SettingsItemCell(icon = Icons.Default.Lock, title = "群类型", subtitle = "私有群", onClick = {}, isDestructive = true) else SettingsItemCell(icon = Icons.Default.Public, title = "群类型", subtitle = "公开群", onClick = {}) }
            )) }
            // 聊天背景和群标签已移至菜单，不再直接显示
            if (group.description.isNotBlank()) { item { SettingsGroup(title = "群聊简介", items = listOf({ SettingsCustomItem { Text(group.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(16.dp)) } })) } }
            group.creator?.let { creator ->
                item { SettingsGroup(title = "群主", items = listOf({ SettingsCustomItem { Row(Modifier.fillMaxWidth().clickable { val intent = Intent(context, UserInfoActivity::class.java); intent.putExtra("userId", creator.id); context.startActivity(intent) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { AsyncImage(model = if (creator.avatarUrl.startsWith("http")) creator.avatarUrl else "${ApiAddress}uploads/${creator.avatarUrl}", contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(CircleShape)); Spacer(Modifier.width(12.dp)); Text(creator.username, fontWeight = FontWeight.Bold) } } })) }
            }
            item { Spacer(Modifier.height(16.dp)); if (!uiState.isJoined) Button(onClick = { viewModel.joinGroup() }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), enabled = !uiState.isJoining) { if (uiState.isJoining) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp) else Text("加入群聊") } }
            item { Spacer(Modifier.height(innerPadding.calculateBottomPadding())) }
        }
    }
}

@Composable
private fun MembersTab(uiState: GroupInfoUiState, context: android.content.Context, innerPadding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(innerPadding)) {
        if (!uiState.isJoined) {
            Text("请先加入群聊", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("成员数: ${uiState.group?.membersCount ?: 0}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    uiState.group?.let { group ->
                        context.startActivity(Intent(context, GroupMembersActivity::class.java).apply { putExtra("group_id", group.id) })
                    }
                }) { Text("查看全部成员") }
            }
        }
    }
}

@Composable
private fun MediaTab(
    mediaType: String,
    mediaList: List<MediaItem>,
    isLoadingMedia: Boolean,
    mediaPage: Int,
    mediaTotalPages: Int,
    viewModel: GroupInfoViewModel,
    uiState: GroupInfoUiState,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // 自动加载更多
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= mediaList.size - 2 && !isLoadingMedia && mediaPage < mediaTotalPages
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) viewModel.loadMoreMedia()
    }

    Column(Modifier.fillMaxSize().padding(innerPadding)) {
        // 类型筛选
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "全部", "image" to "图片", "file" to "链接").forEach { (type, label) ->
                FilterChip(selected = mediaType == type, onClick = { viewModel.changeMediaType(type) }, label = { Text(label) })
            }
        }
        if (isLoadingMedia && mediaList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (mediaList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无媒体", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(mediaList) { item ->
                    MediaListItem(item = item)
                    Spacer(Modifier.height(12.dp))
                }
                if (isLoadingMedia) {
                    item { Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } }
                }
            }
        }
    }
}

@Composable
private fun MediaListItem(item: MediaItem) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable {
            if (item.type == "image" || item.type == "sticker") {
                // 可打开大图查看，此处略
            } else if (item.type == "file") {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                context.startActivity(intent)
            }
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.type == "image" || item.type == "sticker") {
            AsyncImage(
                model = item.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Link, contentDescription = "链接", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.senderUsername, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)   // 改这里
            Spacer(Modifier.height(2.dp))
            Text(item.sendTimeDisplay, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // 改这里
        }
        // 发送者头像
        AsyncImage(
            model = if (item.senderAvatar.startsWith("http")) item.senderAvatar else "${ApiAddress}uploads/${item.senderAvatar}",   // 改这里
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(36.dp).clip(CircleShape)
        )
    }
}

fun formatGroupTime(timeStr: String): String = try { val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); val date = sdf.parse(timeStr) ?: return timeStr; SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date) } catch (_: Exception) { timeStr }

private suspend fun setChatBackground(token: String, chatType: Int, targetId: Int, backgroundUrl: String, onResult: (Boolean) -> Unit) {
    try {
        val client = okhttp3.OkHttpClient()
        val json = org.json.JSONObject().apply { put("chat_type", chatType); put("target_id", targetId); put("background_url", backgroundUrl) }.toString()
        val request = okhttp3.Request.Builder().url("${ApiAddress}chat/set_background").header("x-access-token", token).post(okhttp3.RequestBody.create("application/json".toMediaType(), json)).build()
        withContext(Dispatchers.IO) { client.newCall(request).execute().use { r -> val b = r.body?.string() ?: ""; val res = org.json.JSONObject(b); withContext(Dispatchers.Main) { onResult(res.optBoolean("success")) } } }
    } catch (_: Exception) { withContext(Dispatchers.Main) { onResult(false) } }
}