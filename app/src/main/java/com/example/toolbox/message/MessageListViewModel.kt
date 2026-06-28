// ui/chat/components/MultiImageViewer.kt
package com.example.toolbox.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun MultiImageViewer(
    images: List<String>,
    initialPage: Int = 0,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVisible || images.isEmpty()) return
    
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { images.size }
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = images[page],
                    contentDescription = "图片 ${page + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        // 页码指示器
        if (images.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1} / ${images.size}",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
        
        // 关闭按钮
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }
    }
}