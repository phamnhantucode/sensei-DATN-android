package com.phamnhantucode.aicareercoach.ui.coverletter.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CoverLetterEditorScreen(
    coverLetterId: String = "",
    jobTitle: String,
    companyName: String,
    jobDescription: String,
    initialGeneratedContent: String = "",
    onBack: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val editorFocusRequester = remember { FocusRequester() }
    val repository = remember { com.phamnhantucode.aicareercoach.data.coverletter.CoverLetterRepository() }
    var isSaving by remember { mutableStateOf(false) }

    val initialContent = remember(initialGeneratedContent, jobTitle, companyName, jobDescription) {
        initialGeneratedContent.ifBlank {
            buildInitialEmail(jobTitle, companyName, jobDescription)
        }
    }

    var editorState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialContent))
    }

    val screenTitle = remember(jobTitle, companyName) {
        when {
            jobTitle.isNotBlank() && companyName.isNotBlank() -> "${jobTitle.trim()} at ${companyName.trim()}"
            jobTitle.isNotBlank() -> jobTitle.trim()
            companyName.isNotBlank() -> "Role at ${companyName.trim()}"
            else -> "Cover Letter"
        }
    }

    var isEditorFocused by remember { mutableStateOf(false) }
    val showFormattingToolbar = isEditorFocused

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Start
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (coverLetterId.isNotBlank()) {
                        TextButton(
                            onClick = {
                                if (isSaving) return@TextButton
                                isSaving = true
                                coroutineScope.launch {
                                    try {
                                        repository.updateCoverLetter(
                                            id = coverLetterId,
                                            content = editorState.text
                                        )
                                        snackbarHostState.showSnackbar("Saved successfully")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Save")
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(editorState.text))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Copied to clipboard")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy email body"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = if (showFormattingToolbar) 120.dp else 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Customize the email before you send it. Select any text to format it or drop in boilerplate snippets to speed things up.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BoilerplateSection(
                    onInsertSnippet = { snippet ->
                        editorState = editorState.insertSnippet(snippet)
                        editorFocusRequester.requestFocus()
                    }
                )

                OutlinedTextField(
                    value = editorState,
                    onValueChange = { editorState = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(editorFocusRequester)
                        .onFocusChanged { focusState -> isEditorFocused = focusState.isFocused },
                    minLines = 14,
                    label = { Text("Email body") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions.Default,
                    keyboardActions = KeyboardActions.Default
                )
            }

            if (showFormattingToolbar) {
                FormattingToolbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding(),
                    onBold = {
                        editorState = editorState.wrapSelectionWith("**")
                        editorFocusRequester.requestFocus()
                    },
                    onItalic = {
                        editorState = editorState.wrapSelectionWith("_")
                        editorFocusRequester.requestFocus()
                    },
                    onBullet = {
                        editorState = editorState.toggleBullet()
                        editorFocusRequester.requestFocus()
                    },
                    onReset = {
                        editorState = TextFieldValue(initialContent)
                        editorFocusRequester.requestFocus()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Template restored")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoilerplateSection(
    onInsertSnippet: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Boilerplate snippets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            boilerplateSnippets.forEach { snippet ->
                AssistChip(
                    onClick = { onInsertSnippet(snippet.content) },
                    label = { Text(snippet.label) }
                )
            }
        }

        TextButton(
            onClick = { onInsertSnippet(customClosingSnippet) },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
        ) {
            Text("Insert polished closing")
        }
    }
}

@Composable
private fun FormattingToolbar(
    modifier: Modifier = Modifier,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onBullet: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBold) {
                Icon(
                    imageVector = Icons.Filled.FormatBold,
                    contentDescription = "Bold selection"
                )
            }
            IconButton(onClick = onItalic) {
                Icon(
                    imageVector = Icons.Filled.FormatItalic,
                    contentDescription = "Italic selection"
                )
            }
            IconButton(onClick = onBullet) {
                Icon(
                    imageVector = Icons.Filled.FormatListBulleted,
                    contentDescription = "Toggle bullet"
                )
            }
            IconButton(onClick = onReset) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Reset template"
                )
            }
        }
    }
}

private fun buildInitialEmail(
    jobTitle: String,
    companyName: String,
    jobDescription: String
): String {
    val roleLine = when {
        jobTitle.isNotBlank() && companyName.isNotBlank() -> "$jobTitle role at $companyName"
        jobTitle.isNotBlank() -> jobTitle
        companyName.isNotBlank() -> "the opportunity at $companyName"
        else -> "the opportunity"
    }

    val normalizedDescription = jobDescription.trim().takeIf { it.isNotBlank() }
    val descriptionLine = normalizedDescription?.let {
        "From your description — \"$it\" — I see a clear opportunity to contribute immediately."
    }

    return buildString {
        appendLine("Dear Hiring Manager,")
        appendLine()
        appendLine("I'm excited to submit my application for the $roleLine. I have a track record of delivering measurable results when it comes to AI-driven product experiences.")
        appendLine()
        descriptionLine?.let {
            appendLine(it)
            appendLine()
        }
        appendLine("Here are a few highlights that align with the role:")
        appendLine("- Led cross-functional teams to ship AI-powered features that improved engagement by 18%.")
        appendLine("- Built scalable experimentation roadmaps that translated into faster go-to-market cycles.")
        appendLine("- Partnered with stakeholders to translate ambiguous business problems into actionable product bets.")
        appendLine()
        appendLine("I'd love to share more about how I can help ${companyName.ifBlank { "your team" }} move faster on its 2024 goals.")
        appendLine()
        appendLine("Thank you for your consideration,")
        appendLine("[Your Name]")
    }.trimEnd()
}

private fun TextFieldValue.wrapSelectionWith(
    prefix: String,
    suffix: String = prefix
): TextFieldValue {
    val safeStart = min(selection.start, selection.end).coerceIn(0, text.length)
    val safeEnd = max(selection.start, selection.end).coerceIn(0, text.length)

    return if (safeStart == safeEnd) {
        val newText = StringBuilder(text).insert(safeStart, prefix + suffix).toString()
        val cursor = safeStart + prefix.length
        TextFieldValue(
            text = newText,
            selection = TextRange(cursor, cursor)
        )
    } else {
        val selected = text.substring(safeStart, safeEnd)
        val newText = text.substring(0, safeStart) + prefix + selected + suffix + text.substring(safeEnd)
        val newSelectionStart = safeStart + prefix.length
        val newSelectionEnd = newSelectionStart + selected.length
        TextFieldValue(
            text = newText,
            selection = TextRange(newSelectionStart, newSelectionEnd)
        )
    }
}

private fun TextFieldValue.toggleBullet(): TextFieldValue {
    val caret = selection.start.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', caret - 1).let { if (it == -1) 0 else it + 1 }
    val bulletPrefix = "- "
    val hasBullet = text.startsWith(bulletPrefix, lineStart)

    return if (hasBullet) {
        val newText = text.removeRange(lineStart, (lineStart + bulletPrefix.length).coerceAtMost(text.length))
        val delta = -bulletPrefix.length
        TextFieldValue(
            text = newText,
            selection = TextRange(
                (selection.start + delta).coerceAtLeast(lineStart),
                (selection.end + delta).coerceAtLeast(lineStart)
            )
        )
    } else {
        val newText = text.substring(0, lineStart) + bulletPrefix + text.substring(lineStart)
        val delta = bulletPrefix.length
        TextFieldValue(
            text = newText,
            selection = TextRange(
                (selection.start + delta).coerceIn(0, newText.length),
                (selection.end + delta).coerceIn(0, newText.length)
            )
        )
    }
}

private fun TextFieldValue.insertSnippet(snippet: String): TextFieldValue {
    val safeStart = min(selection.start, selection.end).coerceIn(0, text.length)
    val safeEnd = max(selection.start, selection.end).coerceIn(0, text.length)
    val prefix = text.substring(0, safeStart)
    val suffix = text.substring(safeEnd)

    val needsLeadingBreak = prefix.isNotEmpty() && !prefix.endsWith("\n\n")
    val needsTrailingBreak = suffix.isNotEmpty() && !suffix.startsWith("\n\n")

    val insertion = buildString {
        if (needsLeadingBreak) append("\n\n")
        append(snippet.trim())
        if (needsTrailingBreak) append("\n\n")
    }

    val newText = prefix + insertion + suffix
    val cursor = (prefix + insertion).length
    return TextFieldValue(
        text = newText,
        selection = TextRange(cursor, cursor)
    )
}

private data class BoilerplateSnippet(
    val label: String,
    val content: String
)

private val boilerplateSnippets = listOf(
    BoilerplateSnippet(
        label = "Leadership impact",
        content = "In my last role, I guided a cross-functional squad that launched an AI-powered analytics module two quarters ahead of schedule."
    ),
    BoilerplateSnippet(
        label = "Metrics win",
        content = "I focus on measurable outcomes — the latest campaign I owned increased qualified pipeline by 27% within the first 60 days."
    ),
    BoilerplateSnippet(
        label = "Team collaboration",
        content = "I enjoy translating between product, engineering, and GTM teams to keep everyone anchored on clear hypotheses and customer value."
    )
)

private const val customClosingSnippet =
    "Let me know if there's time next week to walk through the roadmap you have planned — I'd love to compare notes and share ideas."

@Preview(showBackground = true)
@Composable
private fun CoverLetterEditorPreview() {
    AppTheme {
        CoverLetterEditorScreen(
            jobTitle = "Senior Product Manager",
            companyName = "Acme Robotics",
            jobDescription = "Drive the strategy for AI-powered customer experience workflows."
        )
    }
}
