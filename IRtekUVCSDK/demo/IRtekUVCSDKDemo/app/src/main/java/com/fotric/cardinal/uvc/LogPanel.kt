package com.fotric.cardinal.uvc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * 日志面板（主 Tab 4）。
 * 一键测试从「设备控制」并入此页；日志区占满剩余高度，
 * 等宽字体并支持长按选择复制。
 */
@Composable
internal fun LogPanel(
    state: UvcUiState,
    strings: AppStrings,
    onRunBatchTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PanelCard {
            PanelTitle(strings.apiTest)
            Button(
                onClick = onRunBatchTest,
                enabled = state.currentDevice != null && !state.isBatchTesting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (state.isBatchTesting) strings.testing else strings.startBatchTest,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        PanelCard(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PanelTitle(strings.runResult)
                TextButton(onClick = { state.apiResultText = "" }) {
                    Text(strings.clear, style = MaterialTheme.typography.labelMedium)
                }
            }
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.shapes.small
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = state.apiResultText.ifEmpty { strings.resultPlaceholder },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }
    }
}
