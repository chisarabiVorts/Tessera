package io.github.chisarabivorts.tessera

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Separate navigation channel for cross-tab deep links.
 *
 * Many multi-tab apps wire each tab to its own `NavHost` while still needing
 * to dispatch a deep link "into a specific tab" (e.g. a notification opening
 * a product card inside the Catalog tab). [TabDeeplinkNavigator] is a parallel
 * channel to [Navigator], dedicated to those cross-tab jumps so they don't
 * fight with regular intra-tab navigation.
 *
 * Listening to [tabNavigationFlow] is normally handled for you by
 * [MultitabState] - it resolves the target tab from the URI, switches the
 * root `NavController`, and forwards the deep link into the matching nested
 * controller. See `rememberMultitabState` for the recipe.
 */
public interface TabDeeplinkNavigator {

    /**
     * Stream of cross-tab deep-link actions.
     *
     * **Single-consumer**, same as [Navigator.navigationActions]: the underlying
     * channel partitions emissions round-robin across collectors. Subscribe
     * from exactly one place. In the standard setup, [MultitabState] owns
     * the subscription.
     */
    public val tabNavigationFlow: Flow<TabNavigationAction>

    public fun deepLinkToTab(
        deepLinkUri: Uri,
        popUpToRoute: String? = null,
        inclusive: Boolean = false,
        isSingleTop: Boolean = true,
        restoreState: Boolean = false,
        saveState: Boolean = false,
    )
}
