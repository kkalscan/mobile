package ru.kkalscan.app.ui.features

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.kkalscan.app.components.KkalErrorBanner
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.domain.model.FeatureSearchItem
import ru.kkalscan.presentation.features.FeatureSearchUiState
import ru.kkalscan.presentation.features.IFeatureSearchViewModel

@Composable
fun FeatureSearchBar(
    viewModel: IFeatureSearchViewModel,
    onOpenDeepLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    // After submit/clear, block focus so IME / dialog restore cannot put caret back.
    var acceptFocus by remember { mutableStateOf(true) }
    val dismissKeyboard: () -> Unit = {
        acceptFocus = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    LaunchedEffect(acceptFocus) {
        if (!acceptFocus) {
            delay(800)
            acceptFocus = true
        }
    }
    // First focusable on screen is the search field — don't steal focus on open.
    LaunchedEffect(Unit) {
        acceptFocus = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    // ViewModel clears query on food-intent — also drop search focus.
    LaunchedEffect(state.query) {
        if (state.query.isEmpty()) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }
    val showResults = state.query.isNotBlank() &&
        (state.isSearching || state.results.isNotEmpty() || state.errorMessage != null)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("feature-search-bar"),
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("feature-search-input")
                .focusProperties { canFocus = acceptFocus }
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && !acceptFocus) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                },
            placeholder = {
                Text(
                    text = "Поиск: дневник, профиль, скан…",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                when {
                    state.isSearching -> {
                        CircularProgressIndicator(
                            color = KkalScanColors.Primary,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    state.query.isNotBlank() -> {
                        IconButton(onClick = {
                            viewModel.onQueryChange("")
                            dismissKeyboard()
                        }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Очистить")
                        }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    dismissKeyboard()
                    viewModel.onSubmit()
                },
            ),
        )

        if (showResults) {
            Spacer(Modifier.height(8.dp))
            FeatureSearchResultsPanel(
                state = state,
                onRetry = { viewModel.onSubmit() },
                onSelect = { item ->
                    dismissKeyboard()
                    viewModel.onQueryChange("")
                    onOpenDeepLink(item.deeplink)
                },
            )
        }
    }
}

@Composable
private fun FeatureSearchResultsPanel(
    state: FeatureSearchUiState,
    onRetry: () -> Unit,
    onSelect: (FeatureSearchItem) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KkalScanDimens.cardRadius),
        color = KkalScanColors.Surface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, KkalScanColors.Outline.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            state.errorMessage?.let { message ->
                KkalErrorBanner(message = message, onRetry = onRetry)
                Spacer(Modifier.height(8.dp))
            }

            when {
                state.isSearching -> {
                    Row(
                        Modifier.fillMaxWidth().height(80.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(color = KkalScanColors.Primary, modifier = Modifier.size(28.dp))
                    }
                }

                state.showPopular -> {
                    Text(
                        "Ничего не найдено",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KkalScanColors.OnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(
                        "Популярное",
                        style = MaterialTheme.typography.labelLarge,
                        color = KkalScanColors.OnSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(state.results, key = { it.id }) { item ->
                            FeatureSearchResultRow(item = item, onClick = { onSelect(item) })
                        }
                    }
                }

                state.results.isEmpty() && state.query.isNotBlank() && !state.isSearching -> {
                    Text(
                        "Ничего не найдено",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KkalScanColors.OnSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }

                else -> {
                    Text(
                        "Результаты",
                        style = MaterialTheme.typography.labelLarge,
                        color = KkalScanColors.OnSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(state.results, key = { it.id }) { item ->
                            FeatureSearchResultRow(item = item, onClick = { onSelect(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureSearchResultRow(
    item: FeatureSearchItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("feature-search-result-${item.id}"),
        shape = RoundedCornerShape(12.dp),
        color = KkalScanColors.Background,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = KkalScanColors.PrimaryContainer,
            ) {
                Icon(
                    iconForFeature(item.icon),
                    contentDescription = null,
                    tint = KkalScanColors.Primary,
                    modifier = Modifier.padding(8.dp),
                )
            }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                item.subtitle?.let { subtitle ->
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = KkalScanColors.OnSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = KkalScanColors.OnSurfaceVariant,
            )
        }
    }
}

private fun iconForFeature(key: String): ImageVector = when (key) {
    "today" -> Icons.Outlined.Today
    "scan" -> Icons.Outlined.CameraAlt
    "edit" -> Icons.Outlined.Edit
    "search" -> Icons.Outlined.Search
    "journal", "macros" -> Icons.Outlined.CalendarMonth
    "fiber" -> Icons.Outlined.Eco
    "profile" -> Icons.Outlined.Person
    "pro" -> Icons.Outlined.Star
    "gift" -> Icons.Outlined.CardGiftcard
    "dietitian" -> Icons.Outlined.TipsAndUpdates
    else -> Icons.Outlined.PieChart
}
