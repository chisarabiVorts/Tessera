package io.github.chisarabivorts.tessera

import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavOptions

/**
 * A serialisable navigation command emitted by [Navigator] and consumed by the
 * host's `NavController`.
 *
 * Modelled as a sealed hierarchy so the consumer can `when`-exhaustively
 * apply each intent to the controller.
 */
public sealed interface NavigationIntent {

    public data class NavigateTo(
        val route: String,
        val popUpToRoute: String? = null,
        val inclusive: Boolean = false,
        val isSingleTop: Boolean = true,
        val restoreState: Boolean = false,
        val saveState: Boolean = false,
    ) : NavigationIntent

    public data class NavigateToDeepLink(
        val request: NavDeepLinkRequest,
        val navOptions: NavOptions? = null,
    ) : NavigationIntent

    public data object PopBackStack : NavigationIntent

    /**
     * Pop the back stack until [route] is reached.
     *
     * @param route the target destination route to pop to.
     * @param inclusive if `true`, [route] itself is also popped (the entry is
     *   removed from the back stack). If `false`, [route] becomes the current
     *   destination.
     */
    public data class PopBackStackTo(
        val route: String,
        val inclusive: Boolean = false,
    ) : NavigationIntent

    /**
     * Switch the visible tab in a multi-NavHost layout.
     *
     * Consumed by [io.github.chisarabivorts.tessera.MultitabState] - applies
     * to the root controller, with pop-to-start + save/restore semantics
     * (the canonical Material 3 tab navigation pattern).
     *
     * In a single-NavHost layout this intent is a no-op: there is no root
     * controller to switch.
     */
    public data class SwitchTab(val route: String) : NavigationIntent
}
