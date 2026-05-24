package io.github.chisarabivorts.tessera.sample.checkout.steps

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import io.github.chisarabivorts.tessera.sample.checkout.ui.ConfirmScreen
import javax.inject.Inject

internal class ConfirmStepEntry @Inject constructor() : FeatureEntry {

    override val route: String = ROUTE

    @Composable
    override fun Content(
        navBackStackEntry: NavBackStackEntry,
        navigator: Navigator,
        resultNavigator: ResultNavigator,
    ) {
        ConfirmScreen(navigator = navigator)
    }

    companion object {
        const val ROUTE: String = "checkout/confirm"
    }
}
