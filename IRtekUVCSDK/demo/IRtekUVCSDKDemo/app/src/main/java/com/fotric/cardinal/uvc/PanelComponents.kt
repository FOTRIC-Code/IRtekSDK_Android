package com.fotric.cardinal.uvc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 统一的功能卡片容器：所有功能分区共用同一种底色、圆角与内边距。 */
@Composable
internal fun PanelCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/** 卡片分区标题。 */
@Composable
internal fun PanelTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

/** 全宽下拉选择按钮，用于在多个选项中单选。 */
@Composable
internal fun <T> PanelDropdown(
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.firstOrNull { it.first == selectedValue }?.second
        ?: selectedValue.toString()
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text(selectedText, style = MaterialTheme.typography.labelMedium) }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label, style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** 「获取 / 选择 / 设置」三合一的设备设置行。 */
@Composable
internal fun <T> DeviceSettingRow(
    title: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelected: (T) -> Unit,
    onGet: () -> Unit,
    onSet: () -> Unit,
    getLabel: String,
    setLabel: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onGet,
                modifier = Modifier.weight(0.75f)
            ) { Text(getLabel, style = MaterialTheme.typography.labelMedium) }

            PanelDropdown(
                options = options,
                selectedValue = selectedValue,
                onSelected = onSelected,
                modifier = Modifier.weight(1.5f)
            )

            Button(
                onClick = onSet,
                modifier = Modifier.weight(0.75f)
            ) { Text(setLabel, style = MaterialTheme.typography.labelMedium) }
        }
    }
}
