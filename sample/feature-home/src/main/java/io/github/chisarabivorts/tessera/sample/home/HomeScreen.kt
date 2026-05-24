package io.github.chisarabivorts.tessera.sample.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    navigator: Navigator,
    resultNavigator: ResultNavigator,
) {
    val selectedId by resultNavigator
        .resultFlow<String>(HomeFeatureEntry.RESULT_KEY_SELECTED_ID)
        .collectAsState(initial = null)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Главная") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Tessera Sample",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Этот экран - в :sample:feature-home, зарегистрирован через FeatureEntry.",
                style = MaterialTheme.typography.bodyMedium,
            )

            selectedId?.let { id ->
                Spacer(modifier = Modifier.height(8.dp))
                Card {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "Последний результат из Detail: id = $id",
                    )
                }
            }

            Spacer(modifier = Modifier.fillMaxWidth().height(24.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navigator.navigate("detail/42") },
            ) {
                Text("Открыть Detail (id = 42)")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navigator.navigate("settings") },
            ) {
                Text("Открыть Настройки")
            }
        }
    }
}
