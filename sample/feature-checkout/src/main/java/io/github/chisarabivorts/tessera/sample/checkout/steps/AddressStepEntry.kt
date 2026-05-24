package io.github.chisarabivorts.tessera.sample.checkout.steps

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import io.github.chisarabivorts.tessera.sample.checkout.ui.AddressScreen
import javax.inject.Inject

internal class AddressStepEntry @Inject constructor() : FeatureEntry {

    override val route: String = ROUTE

    @Composable
    override fun Content(
        navBackStackEntry: NavBackStackEntry,
        navigator: Navigator,
        resultNavigator: ResultNavigator,
    ) {
        AddressScreen(navigator = navigator)
    }

    companion object {
        const val ROUTE: String = "checkout/address"
    }
}
