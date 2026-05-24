package io.github.chisarabivorts.tessera

import android.net.Uri

/**
 * Cross-tab navigation action emitted by [TabDeeplinkNavigator].
 *
 * Sealed so the host can exhaustively dispatch each variant to the
 * appropriate per-tab `NavController`.
 */
public sealed interface TabNavigationAction {

    public data class DeepLinkToTab(
        val uri: Uri,
        val popUpToRoute: String? = null,
        val inclusive: Boolean = false,
        val isSingleTop: Boolean = true,
        val restoreState: Boolean = false,
        val saveState: Boolean = false,
    ) : TabNavigationAction
}
