package com.example.toolbox.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hrm.markdown.renderer.Markdown
import com.hrm.markdown.renderer.MarkdownTheme
import com.hrm.markdown.renderer.components.CodeBlockState

@Composable
fun CopyCodeBlock(
    state: CodeBlockState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier.padding(8.dp)) {
        // 语言标签 + 复制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.language != null) {
                Text(
                    text = state.language,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("code", state.code))
                Toast.makeText(context, "代码已复制", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, "复制", Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("复制", style = MaterialTheme.typography.labelSmall)
            }
        }
        // 渲染代码高亮（不递归传入 components，避免死循环）
        Markdown(
            markdown = "```${state.language.orEmpty()}\n${state.code}\n```",
            theme = MarkdownTheme.auto()
        )
    }
}