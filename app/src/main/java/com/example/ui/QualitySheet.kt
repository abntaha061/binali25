package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SubtitleItem
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySheet(
    formats: List<FormatItem>,
    subtitles: List<SubtitleItem>,
    selectedLanguages: Set<String>,
    embedSubtitles: Boolean,
    onLanguageToggled: (String) -> Unit,
    onEmbedToggled: (Boolean) -> Unit,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onFormatSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "اختر جودة التحميل والترجمة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Subtitle Selection Section
            SubtitleSelector(
                availableSubtitles = subtitles,
                selectedLanguages = selectedLanguages,
                embedSubtitles = embedSubtitles,
                onLanguageToggled = onLanguageToggled,
                onEmbedToggled = onEmbedToggled
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "جودة الفيديو المتاحة:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (formats.isEmpty()) {
                Text(
                    text = "لا توجد صيغ متاحة للتحميل",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(formats) { format ->
                        FormatCard(
                            format = format,
                            onDownloadClick = {
                                onFormatSelected(format.formatId)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormatCard(
    format: FormatItem,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "الدقة: ${format.resolution}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                val sizeText = if (format.fileSizeApprox > 0) {
                    val mb = format.fileSizeApprox / (1024f * 1024f)
                    String.format(Locale.getDefault(), "%.1f MB", mb)
                } else {
                    "حجم غير معروف"
                }

                Text(
                    text = "الامتداد: ${format.ext.uppercase(Locale.getDefault())} • $sizeText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onDownloadClick,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("تحميل")
            }
        }
    }
}
