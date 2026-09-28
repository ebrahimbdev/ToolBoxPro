package com.toolbox.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.pro.core.localization.LocalStrings
import com.toolbox.pro.core.net.ConnIssue
import kotlinx.coroutines.launch

private val ActiveColor = Color(0xFFE53935)
private val IdleColor = Color(0xFF9E9E9E)

/**
 * Full-screen blocking page shown when the app cannot talk to the server
 * (409), could not register with the admin backend (410) or cannot reach the
 * ad network (411). The active code is highlighted; retry re-runs the check.
 */
@Composable
fun ConnectionErrorScreen(
    issue: ConnIssue,
    detail: String = "",
    onRetry: suspend () -> Unit
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = ActiveColor,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = s.connTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = titleFor(issue, s.code409, s.code410),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            CodeRow("409", s.code409, issue == ConnIssue.Server)
            CodeRow("410", s.code410, issue == ConnIssue.NotRegistered)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (checking) return@Button
                    checking = true
                    scope.launch {
                        onRetry()
                        checking = false
                    }
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                if (checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(if (checking) s.checking else s.retry)
            }

            if (detail.isNotBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CodeRow(code: String, label: String, active: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) ActiveColor.copy(alpha = 0.12f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = code,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = if (active) ActiveColor else IdleColor
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (active) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ActiveColor)
            )
        }
    }
}

private fun titleFor(issue: ConnIssue, c409: String, c410: String): String =
    when (issue) {
        ConnIssue.Server -> c409
        ConnIssue.NotRegistered -> c410
    }
