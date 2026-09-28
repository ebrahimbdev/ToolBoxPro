package com.toolbox.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.pro.monetization.UsageGate
import com.toolbox.pro.monetization.UsageGateEntryPoint
import dagger.hilt.android.EntryPointAccessors

private val HeaderGradient = listOf(Color(0xFF6C63FF), Color(0xFF9C27B0))

/**
 * Shared header for tool screens: fixed-size neumorphic surface band with
 * gradient icon chip, title/subtitle, optional back button, actions slot and
 * an optional segmented tab row for pages that grow sections later on.
 *
 * Pass [tabs] empty (default) and the tab row is not rendered at all.
 */
@Composable
fun ToolHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onBack: (() -> Unit)? = null,
    tabs: List<String> = emptyList(),
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = title)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(HeaderGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val usageGate = rememberUsageGate()
                if (usageGate != null) {
                    UsageCircle(usageGate)
                }

                if (actions != null) {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }

            if (tabs.isNotEmpty()) {
                TabRow(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun rememberUsageGate(): UsageGate? {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                UsageGateEntryPoint::class.java
            ).usageGate()
        }.getOrNull()
    }
}

/**
 * Free-quota ring in the header: runs 100 -> 0 as the free turns are used and
 * turns red at 0 (the rewarded ad that refills it fires automatically).
 */
@Composable
private fun UsageCircle(gate: UsageGate) {
    val left by gate.quotaLeft.collectAsState()
    val total by gate.quotaTotal.collectAsState()
    val premium by gate.premium.collectAsState()

    if (premium || total <= 0) return

    val progress = (left.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val percent = (left * 100 / total).coerceIn(0, 100)
    val color = when {
        left <= 0 -> Color(0xFFE53935)
        progress < 0.34f -> Color(0xFFFFA000)
        else -> Color(0xFF43A047)
    }

    Box(
        modifier = Modifier
            .padding(start = 10.dp)
            .size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = 4.dp,
            trackColor = color.copy(alpha = 0.18f)
        )
        Text(
            text = "$percent",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TabRow(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    // Up to 3 tabs share the width equally; beyond that the row scrolls sideways
    // so tabs stay readable instead of squeezing into each other.
    val fixedWidth = tabs.size <= 3

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (fixedWidth) Modifier else Modifier.horizontalScroll(scrollState)),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedTab
            Box(
                modifier = Modifier
                    .then(if (fixedWidth) Modifier.weight(1f) else Modifier)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
