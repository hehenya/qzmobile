package com.example.toolbox.utils

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hrm.markdown.renderer.Markdown
import com.hrm.markdown.renderer.MarkdownTheme
import com.hrm.markdown.renderer.model.MarkdownImageData
import com.example.toolbox.webview.WebViewActivity

object MarkdownRenderer {

    @Composable
    fun Render(
        modifier: Modifier = Modifier,
        content: String,
        onLinkClick: ((String) -> Unit)? = null,
        onImageClick: ((String, String) -> Unit)? = null  // (imageUrl, altText)
    ) {
        val context = LocalContext.current

        Markdown(
            document = content,   // 新版库的参数名
            modifier = modifier,
            enableScroll = false,
            theme = MarkdownTheme.material3(),
            // 自定义图片渲染（如果调用方需要图片点击，则传入回调）
            imageContent = if (onImageClick != null) {
                { imageData: MarkdownImageData, imageModifier: Modifier ->
                    Box(
                        modifier = imageModifier
                            .clickable {
                                onImageClick(imageData.url, imageData.altText ?: "")
                            }
                    ) {
                        AsyncImage(
                            model = imageData.url,
                            contentDescription = imageData.altText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            } else null,
            onLinkClick = { url: String ->
                // 优先使用调用方传入的自定义回调
                if (onLinkClick != null) {
                    onLinkClick(url)
                } else {
                    // 默认行为：HTTP/HTTPS 用内置浏览器，其他用系统浏览器
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        try {
                            val intent = Intent(context, WebViewActivity::class.java).apply {
                                putExtra(WebViewActivity.EXTRA_URL, url)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
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
        } catch (_: Exception) { }
    }
}