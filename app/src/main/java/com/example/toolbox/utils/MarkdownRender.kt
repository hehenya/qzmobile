package com.example.toolbox.utils

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hrm.markdown.renderer.Markdown
import com.hrm.markdown.renderer.MarkdownTheme
import com.hrm.markdown.renderer.components.MarkdownComponents
import com.hrm.markdown.renderer.components.ImageState
import com.example.toolbox.webview.WebViewActivity

object MarkdownRenderer {

    @Composable
    fun Render(
        modifier: Modifier = Modifier,
        content: String,
        onLinkClick: ((String) -> Unit)? = null,
        onImageClick: ((String, String) -> Unit)? = null
    ) {
        val context = LocalContext.current

        // 准备图片组件（若将来需要）
        val components = remember(onImageClick) {
            if (onImageClick != null) {
                MarkdownComponents(
                    image = { state ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onImageClick.invoke(state.url, state.altText.orEmpty())
                                }
                        ) {
                            AsyncImage(
                                model = state.url,
                                contentDescription = state.altText.orEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                )
            } else null
        }

        Markdown(
            markdown = content,
            modifier = modifier,
            enableScroll = false,
            theme = MarkdownTheme.material3(),
            components = components,
            onLinkClick = { url: String ->
                // 优先使用调用方传入的自定义回调
                if (onLinkClick != null) {
                    onLinkClick(url)
                } else {
                    // 默认行为：HTTP 链接使用内置浏览器，否则使用系统浏览器
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        try {
                            val intent = Intent(context, WebViewActivity::class.java).apply {
                                putExtra(WebViewActivity.EXTRA_URL, url)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // 内置浏览器不可用时回退到系统浏览器
                            openWithSystemBrowser(context, url)
                        }
                    } else {
                        openWithSystemBrowser(context, url)
                    }
                }
            }
        )
    }

    private fun openWithSystemBrowser(context: android.content.Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            // 无应用可打开，静默忽略
        }
    }
}