package io.github.chisarabivorts.tessera.sample.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import io.github.chisarabivorts.tessera.DialogFeatureEntry
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import javax.inject.Inject

internal class DemoDialogFeatureEntry @Inject constructor() : DialogFeatureEntry {

    override val route: String = ROUTE

    @Composable
    override fun Content(
        navBackStackEntry: NavBackStackEntry,
        navigator: Navigator,
        resultNavigator: ResultNavigator,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "DialogFeatureEntry",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Tessera зарегистрировала этот destination через NavGraphBuilder.dialog(...).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navigator.popBackStack() },
                ) {
                    Text("Закрыть")
                }
            }
        }
    }

    companion object {
        const val ROUTE: String = "settings/demo_dialog"
    }
}
