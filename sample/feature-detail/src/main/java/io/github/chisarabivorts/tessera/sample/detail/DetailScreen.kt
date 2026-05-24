package io.github.chisarabivorts.tessera.sample.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import io.github.chisarabivorts.tessera.sample.detail.DetailFeatureEntry.Companion.RESULT_KEY_SELECTED_ID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailScreen(
    id: String,
    navigator: Navigator,
    resultNavigator: ResultNavigator,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail #$id") },
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
                text = "Элемент Detail, id = $id",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Этот экран - в :sample:feature-detail. " +
                    "Доступен по маршруту 'detail/{id}' или по deep link " +
                    "tessera://sample/detail/{id}.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.fillMaxWidth().size(0.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    resultNavigator.publishResult(RESULT_KEY_SELECTED_ID, id)
                    navigator.popBackStack()
                },
            ) {
                Text("Вернуть результат и закрыть")
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navigator.popBackStack() },
            ) {
                Text("Просто закрыть")
            }
        }
    }
}
