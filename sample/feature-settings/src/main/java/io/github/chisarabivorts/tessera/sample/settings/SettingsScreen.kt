package io.github.chisarabivorts.tessera.sample.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.chisarabivorts.tessera.Navigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(navigator: Navigator) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Этот экран - в :sample:feature-settings. " +
                    "Демонстрирует DialogFeatureEntry - нажмите ниже, чтобы открыть диалог.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navigator.navigate(DemoDialogFeatureEntry.ROUTE) },
            ) {
                Text("Открыть диалог")
            }
        }
    }
}
