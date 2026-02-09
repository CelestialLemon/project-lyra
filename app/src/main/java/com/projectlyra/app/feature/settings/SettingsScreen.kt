package com.projectlyra.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projectlyra.app.data.settings.TmdbApiKeyValidator

@Composable
fun SettingsRoute(
    factory: SettingsViewModelFactory,
    viewModel: SettingsViewModel = viewModel(factory = factory),
) {
    val settings by viewModel.settings.collectAsState()
    val apiKeyError by viewModel.apiKeyError.collectAsState()
    var apiKeyDraft by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                    )
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "Your TMDB API key is encrypted with Android Keystore before it is stored on device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = apiKeyDraft,
            onValueChange = {
                apiKeyDraft = it
                viewModel.onApiKeyDraftChanged(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("TMDB API Key") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            singleLine = true,
            isError = apiKeyError != null,
            supportingText = {
                Text(apiKeyError ?: TmdbApiKeyValidator.HELPER_TEXT)
            },
        )

        Button(
            onClick = { viewModel.updateApiKey(apiKeyDraft) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save API Key")
        }

        SettingToggleRow(
            label = "Notify for Watching + On Hold",
            checked = settings.reminderEnabled,
            onCheckedChange = viewModel::updateReminder,
        )

        SettingToggleRow(
            label = "Include API key in JSON backup",
            checked = settings.includeApiKeyInBackup,
            onCheckedChange = viewModel::updateIncludeApiKeyInBackup,
        )
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
