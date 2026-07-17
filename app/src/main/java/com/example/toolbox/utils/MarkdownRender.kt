package com.example.toolbox.utils

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hrm.markdown.parser.MarkdownParser
import com.hrm.markdown.renderer.Markdown
import com.hrm.markdown.renderer.MarkdownTheme
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
        val parser = remember { MarkdownParser() }
        val document = remember(content) { parser.parse(content) }

        Markdown(
            document = document,
            modifier = modifier,
            enableScroll = false,
            theme = MarkdownTheme.material3(),
            onLinkClick = { url: String ->
                if (onLinkClick != null) {
                    onLinkClick(url)
                } else {
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
            // 图片点击暂不使用 imageContent，如需支持可后续实现
        )
    }

    private fun openWithSystemBrowser(context: android.content.Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) { }
    }
}