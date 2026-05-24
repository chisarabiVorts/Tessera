package io.github.chisarabivorts.tessera.sample.checkout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.sample.checkout.steps.ConfirmStepEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddressScreen(navigator: Navigator) {
    var address by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Оформление · Шаг 1 из 2 · Адрес") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Этот экран - в :sample:feature-checkout. Маршрут 'checkout' - это " +
                    "NestedFeatureEntry, и сейчас вы в начале его вложенного графа.",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Адрес доставки") },
                singleLine = true,
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navigator.navigate(ConfirmStepEntry.ROUTE) },
                enabled = address.isNotBlank(),
            ) {
                Text("Продолжить")
            }
        }
    }
}
