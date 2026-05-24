package io.github.chisarabivorts.tessera.sample.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import io.github.chisarabivorts.tessera.FeatureEntryWithBottomBar
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import javax.inject.Inject

internal class HomeFeatureEntry @Inject constructor() : FeatureEntryWithBottomBar {

    override val route: String = ROUTE

    override val title: String = "Главная"
    override val icon: ImageVector = Icons.Default.Home
    override val order: Int = 1

    @Composable
    override fun Content(
        navBackStackEntry: NavBackStackEntry,
        navigator: Navigator,
        resultNavigator: ResultNavigator,
    ) {
        HomeScreen(navigator = navigator, resultNavigator = resultNavigator)
    }

    companion object {
        const val ROUTE: String = "home"
        const val RESULT_KEY_SELECTED_ID: String = "selected_id"
    }
}
