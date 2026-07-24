package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoViewModel = viewModel()
) {
    val url by viewModel.urlState.collectAsStateWithLifecycle()
    val videoInfoState by viewModel.videoInfoState.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgressState.collectAsStateWithLifecycle()
    val selectedLanguages by viewModel.selectedSubLanguages.collectAsStateWithLifecycle()
    val embedSubtitles by viewModel.embedSubtitles.collectAsStateWithLifecycle()

    var showQualitySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { viewModel.updateUrl(it) },
                label = { Text("رابط الفيديو (YouTube URL)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.fetchVideoInfo(url) },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() && videoInfoState !is VideoInfoState.Loading
            ) {
                Text("جلب معلومات الفيديو")
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = videoInfoState) {
                is VideoInfoState.Idle -> {
                    Text(
                        text = "أدخل رابط فيديو يوتيوب وانقر على جلب المعلومات",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is VideoInfoState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("جاري تحضير معلومات الفيديو والترجمة...")
                }
                is VideoInfoState.Success -> {
                    val details = state.videoDetails
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (details.thumbnailUrl.isNotBlank()) {
                                AsyncImage(
                                    model = details.thumbnailUrl,
                                    contentDescription = "Video Thumbnail",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Text(
                                text = details.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { showQualitySheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !downloadProgress.isDownloading
                            ) {
                                Text("اختر الجودة والترجمة للتحميل")
                            }
                        }
                    }

                    if (showQualitySheet) {
                        QualitySheet(
                            formats = details.formats,
                            subtitles = details.subtitles,
                            selectedLanguages = selectedLanguages,
                            embedSubtitles = embedSubtitles,
                            onLanguageToggled = { viewModel.toggleSubtitleLanguage(it) },
                            onEmbedToggled = { viewModel.setEmbedSubtitles(it) },
                            sheetState = sheetState,
                            onDismissRequest = { showQualitySheet = false },
                            onFormatSelected = { formatId ->
                                viewModel.startDownload(formatId)
                            }
                        )
                    }
                }
                is VideoInfoState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (downloadProgress.isDownloading || downloadProgress.isCompleted || downloadProgress.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (downloadProgress.isDownloading) {
                            Text(
                                text = "جاري التحميل... ${String.format(Locale.getDefault(), "%.1f", downloadProgress.progress)}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress.progress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (downloadProgress.etaInSeconds > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "الوقت المتبقي: ${downloadProgress.etaInSeconds} ثانية",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.cancelDownload() }
                            ) {
                                Text("إلغاء التحميل")
                            }
                        } else if (downloadProgress.isCompleted) {
                            Text(
                                text = "تم التحميل والدمج بنجاح! 🚀",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تمت إضافة الفيديو إلى مجلد Downloads",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (downloadProgress.error != null) {
                            Text(
                                text = downloadProgress.error ?: "حدث خطأ أثناء التحميل",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
