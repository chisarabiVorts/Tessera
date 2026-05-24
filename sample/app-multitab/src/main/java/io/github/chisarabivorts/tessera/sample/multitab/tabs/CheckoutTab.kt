package io.github.chisarabivorts.tessera.sample.multitab.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.TabFeatureEntry

internal class CheckoutTab(
    override val children: Set<FeatureEntry>,
) : TabFeatureEntry {
    override val route: String = "checkout_tab"
    // The Checkout NestedFeatureEntry's route is "checkout" - that's the start
    // destination inside this tab's nested graph.
    override val startDestination: String = "checkout"
    override val title: String = "Оформление"
    override val icon: ImageVector = Icons.Default.ShoppingCart
    override val order: Int = 2
}
