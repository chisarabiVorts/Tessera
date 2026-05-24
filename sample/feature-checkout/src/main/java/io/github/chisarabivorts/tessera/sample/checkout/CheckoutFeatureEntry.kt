package io.github.chisarabivorts.tessera.sample.checkout

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.FeatureEntryWithBottomBar
import io.github.chisarabivorts.tessera.NestedFeatureEntry
import io.github.chisarabivorts.tessera.sample.checkout.steps.AddressStepEntry
import io.github.chisarabivorts.tessera.sample.checkout.steps.ConfirmStepEntry
import javax.inject.Inject

/**
 * The Checkout feature exposes itself as a single tab "checkout" at the BottomBar
 * level, but internally it is a [NestedFeatureEntry] with its own nested graph:
 *
 *     checkout
 *      ├── checkout/address     (start)
 *      └── checkout/confirm
 *
 * External callers never know about the internal steps - they only `navigate("checkout")`
 * and the nested graph takes over from there.
 */
internal class CheckoutFeatureEntry @Inject constructor(
    private val address: AddressStepEntry,
    private val confirm: ConfirmStepEntry,
) : NestedFeatureEntry(), FeatureEntryWithBottomBar {

    override val route: String = ROUTE
    override val startRoute: String = address.route
    override val children: List<FeatureEntry> = listOf(address, confirm)

    override val title: String = "Оформление"
    override val icon: ImageVector = Icons.Default.ShoppingCart
    override val order: Int = 2

    companion object {
        const val ROUTE: String = "checkout"
    }
}
