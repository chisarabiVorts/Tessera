package io.github.chisarabivorts.tessera.sample.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import io.github.chisarabivorts.tessera.FeatureEntryWithBottomBar
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import javax.inject.Inject

internal class SettingsFeatureEntry @Inject constructor() : FeatureEntryWithBottomBar {

    override val route: String = ROUTE

    override val title: String = "Настройки"
    override val icon: ImageVector = Icons.Default.Settings
    override val order: Int = 3

    @Composable
    override fun Content(
        navBackStackEntry: NavBackStackEntry,
        navigator: Navigator,
        resultNavigator: ResultNavigator,
    ) {
        SettingsScreen(navigator = navigator)
    }

    companion object {
        const val ROUTE: String = "settings"
    }
}
