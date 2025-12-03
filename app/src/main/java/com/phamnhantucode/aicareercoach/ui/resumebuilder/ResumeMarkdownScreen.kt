package com.phamnhantucode.aicareercoach.ui.resumebuilder

import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeMarkdownScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ResumeMarkdownViewModel = viewModel { ResumeMarkdownViewModel(context) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val uiState by viewModel.uiState.collectAsState()
    val markdown by viewModel.markdown.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    // View mode: true = rendered preview, false = raw text
    var isPreviewMode by remember { mutableStateOf(true) }

    // Calculate WindowInsets once for stable references
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val topPadding = systemBarsPadding.calculateTopPadding()
    val bottomPadding = systemBarsPadding.calculateBottomPadding()

    // Handle export events
    LaunchedEffect(viewModel) {
        viewModel.exportEvents.collect { event ->
            when (event) {
                is ResumeExportResult.Success -> {
                    val message = "${event.format.displayName} saved to Downloads as ${event.fileName}"
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                is ResumeExportResult.Error -> {
                    val error = event.throwable.localizedMessage ?: "Unknown error"
                    val message = "Failed to export ${event.format.displayName}: $error"
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Handle sync events
    LaunchedEffect(viewModel) {
        viewModel.syncEvents.collect { event ->
            Toast.makeText(context, event, Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp + topPadding,
                            bottom = 12.dp
                        )
                ) {
                    // First Row: Back button and Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Resume Markdown",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (uiState) {
                                    is MarkdownUiState.Loading -> "Loading..."
                                    is MarkdownUiState.Syncing -> "Syncing to cloud..."
                                    is MarkdownUiState.Success -> "Synced with cloud"
                                    is MarkdownUiState.Error -> "Error syncing"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when (uiState) {
                                    is MarkdownUiState.Error -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    // Second Row: Export Buttons
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copy to Clipboard
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(markdown))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(24.dp),
                            enabled = markdown.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy")
                        }

                        // Export as Markdown
                        Button(
                            onClick = { viewModel.exportMarkdown() },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            enabled = !isExporting && markdown.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = "Export Markdown",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Export .md",
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Export as PDF
                        Button(
                            onClick = { viewModel.exportPdf() },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !isExporting && markdown.isNotBlank()
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = "Export PDF",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export PDF")
                        }
                    }
                }
            }

            // Markdown Content
            when (val state = uiState) {
                is MarkdownUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading resume...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is MarkdownUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = { viewModel.retry() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is MarkdownUiState.Success, is MarkdownUiState.Syncing -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Always render WebView to keep it alive (use graphicsLayer to hide/show)
                        MarkdownWebView(
                            markdown = markdown,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = bottomPadding)
                                .graphicsLayer {
                                    alpha = if (isPreviewMode) 1f else 0f
                                }
                        )

                        // Raw text view (always rendered but hidden when in preview mode)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                                .graphicsLayer {
                                    alpha = if (isPreviewMode) 0f else 1f
                                }
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = markdown,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    ),
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(bottomPadding + 80.dp)
                            )
                        }

                        // FAB to toggle view mode
                        FloatingActionButton(
                            onClick = { isPreviewMode = !isPreviewMode },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .padding(bottom = bottomPadding),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(
                                imageVector = if (isPreviewMode) Icons.Filled.Code else Icons.Filled.Visibility,
                                contentDescription = if (isPreviewMode) "Show raw text" else "Show preview"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownWebView(
    markdown: String,
    modifier: Modifier = Modifier
) {
    // Get theme colors
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val codeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()

    // Convert colors to hex - memoized to prevent recalculation
    val bgHex = remember(backgroundColor) {
        String.format("#%06X", 0xFFFFFF and backgroundColor)
    }
    val textHex = remember(textColor) {
        String.format("#%06X", 0xFFFFFF and textColor)
    }
    val linkHex = remember(linkColor) {
        String.format("#%06X", 0xFFFFFF and linkColor)
    }
    val codeBgHex = remember(codeBackgroundColor) {
        String.format("#%06X", 0xFFFFFF and codeBackgroundColor)
    }

    val htmlContent = remember(markdown, bgHex, textHex, linkHex, codeBgHex) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
            <style>
                * {
                    box-sizing: border-box;
                }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                    line-height: 1.6;
                    padding: 16px;
                    margin: 0;
                    background-color: $bgHex;
                    color: $textHex;
                    font-size: 15px;
                }
                h1, h2, h3, h4, h5, h6 {
                    margin-top: 24px;
                    margin-bottom: 16px;
                    font-weight: 600;
                    line-height: 1.25;
                }
                h1 { font-size: 1.8em; border-bottom: 1px solid #ddd; padding-bottom: 8px; }
                h2 { font-size: 1.5em; border-bottom: 1px solid #eee; padding-bottom: 6px; }
                h3 { font-size: 1.25em; }
                p { margin: 0 0 16px 0; }
                ul, ol {
                    padding-left: 24px;
                    margin: 0 0 16px 0;
                }
                li { margin: 4px 0; }
                a {
                    color: $linkHex;
                    text-decoration: none;
                }
                a:hover { text-decoration: underline; }
                code {
                    background-color: $codeBgHex;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-family: 'SF Mono', Monaco, 'Courier New', monospace;
                    font-size: 0.9em;
                }
                pre {
                    background-color: $codeBgHex;
                    padding: 12px;
                    border-radius: 8px;
                    overflow-x: auto;
                    margin: 16px 0;
                }
                pre code {
                    background: none;
                    padding: 0;
                }
                blockquote {
                    border-left: 4px solid $linkHex;
                    margin: 16px 0;
                    padding: 8px 16px;
                    background-color: $codeBgHex;
                    border-radius: 0 8px 8px 0;
                }
                hr {
                    border: none;
                    border-top: 1px solid #ddd;
                    margin: 24px 0;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 16px 0;
                }
                th, td {
                    border: 1px solid #ddd;
                    padding: 8px 12px;
                    text-align: left;
                }
                th {
                    background-color: $codeBgHex;
                }
                strong { font-weight: 600; }
                em { font-style: italic; }
                div[align="center"] {
                    text-align: center;
                }
            </style>
        </head>
        <body>
            <div id="content"></div>
            <script>
                const markdown = ${escapeJsString(markdown)};
                document.getElementById('content').innerHTML = marked.parse(markdown);
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    // Track last loaded content to prevent unnecessary reloads
    var lastLoadedContent by remember { mutableStateOf("") }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(backgroundColor)
            }
        },
        update = { webView ->
            // Only reload if content actually changed
            if (htmlContent != lastLoadedContent) {
                webView.loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
                lastLoadedContent = htmlContent
            }
        },
        modifier = modifier
    )
}

private fun escapeJsString(str: String): String {
    return buildString {
        append('`')
        str.forEach { char ->
            when (char) {
                '`' -> append("\\`")
                '\\' -> append("\\\\")
                '$' -> append("\\$")
                else -> append(char)
            }
        }
        append('`')
    }
}

/**
 * Resume Markdown Content - for embedding in tabs
 * Shows the markdown preview/editor without the scaffold/app bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeMarkdownContentInternal() {
    val context = LocalContext.current
    val viewModel: ResumeMarkdownViewModel = viewModel { ResumeMarkdownViewModel(context) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val uiState by viewModel.uiState.collectAsState()
    val markdown by viewModel.markdown.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    var isPreviewMode by remember { mutableStateOf(true) }

    // Handle export events
    LaunchedEffect(viewModel) {
        viewModel.exportEvents.collect { event ->
            when (event) {
                is ResumeExportResult.Success -> {
                    val message = "${event.format.displayName} saved to Downloads as ${event.fileName}"
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                is ResumeExportResult.Error -> {
                    val error = event.throwable.localizedMessage ?: "Unknown error"
                    val message = "Failed to export ${event.format.displayName}: $error"
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Handle sync events
    LaunchedEffect(viewModel) {
        viewModel.syncEvents.collect { event ->
            Toast.makeText(context, event, Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Copy button
            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(markdown))
                    Toast.makeText(context, "Markdown copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy")
            }

            // Export to PDF
            Button(
                onClick = {
                    viewModel.exportPdf()
                },
                enabled = !isExporting && uiState is MarkdownUiState.Success,
                shape = RoundedCornerShape(24.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export PDF")
            }

            // Sync button (reloads from form data)
            OutlinedButton(
                onClick = { viewModel.retry() },
                enabled = uiState !is MarkdownUiState.Loading,
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Sync from Form")
            }
        }

        // Markdown Content
        when (val state = uiState) {
            is MarkdownUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading resume...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is MarkdownUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { viewModel.retry() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            is MarkdownUiState.Success, is MarkdownUiState.Syncing -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    // WebView preview
                    MarkdownWebView(
                        markdown = markdown,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = if (isPreviewMode) 1f else 0f
                            }
                    )

                    // Raw text view
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                            .graphicsLayer {
                                alpha = if (isPreviewMode) 0f else 1f
                            }
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = markdown,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                ),
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }

                    // FAB to toggle view mode
                    FloatingActionButton(
                        onClick = { isPreviewMode = !isPreviewMode },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Filled.Code else Icons.Filled.Visibility,
                            contentDescription = if (isPreviewMode) "Show raw text" else "Show preview"
                        )
                    }
                }
            }
        }
    }
}
