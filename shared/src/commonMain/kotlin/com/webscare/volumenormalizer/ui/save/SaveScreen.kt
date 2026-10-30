
package com.webscare.volumenormalizer.ui.save

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.webscare.volumenormalizer.theme.AppColors
import com.webscare.volumenormalizer.ui.components.AppCard
import com.webscare.volumenormalizer.ui.components.PrimaryButton
import com.webscare.volumenormalizer.ui.components.ToolTopBar

@Composable
fun SaveScreen(
    fileName: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit,
    isSaving: Boolean = false,
    message: String? = null,
    isError: Boolean = false
) {
    var exportName by remember(fileName) {
        mutableStateOf(
            fileName.substringBeforeLast('.', fileName) + "_normalized"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        ToolTopBar(
            title = "Save Audio",
            onBack = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Export your normalized audio",
            style = MaterialTheme.typography.headlineSmall,
            color = AppColors.TextPrimary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Choose a file name and save your processed audio to your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        AppCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "File name",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = exportName,
                onValueChange = { exportName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text("Audio name")
                },
                placeholder = {
                    Text("Normalized Audio")
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None
                )
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Export format",
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "WAV audio (.wav)",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }

        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            text = if (isSaving) "Saving..." else "Save to Device",
            onClick = {
                onSave(exportName.trim())
            },
            enabled = !isSaving && exportName.isNotBlank()
        )

        if (message != null) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    AppColors.Primary
                }
            )
        }
    }
}
