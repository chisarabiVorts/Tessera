package io.github.chisarabivorts.tessera

import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavOptions
import kotlinx.coroutines.flow.Flow

/**
 * Single-writer entry point that feature modules use to request navigation.
 *
 * Implementations emit [NavigationIntent]s into a [Flow] consumed by the host
 * Activity / [androidx.navigation.NavHostController]. Features never touch a
 * `NavController` directly - they only push intents through this interface.
 *
 * Combine with [ResultNavigator] when a feature also needs to publish/observe
 * inter-screen results.
 */
public interface Navigator {

    /**
     * Stream of navigation intents to be applied to the host's NavController.
     *
     * **Single-consumer.** This flow is backed by a [kotlinx.coroutines.channels.Channel]
     * (`receiveAsFlow`). Collecting it from more than one place at the same
     * time partitions intents round-robin across collectors - each intent goes
     * to exactly one of them, not to all - which silently breaks navigation.
     *
     * Subscribe in exactly one place: the host that owns the `NavController`
     * (typically a single `LaunchedEffect` in your top-level Composable).
     * `MultitabState` does this for you in the multi-NavHost case.
     */
    public val navigationActions: Flow<NavigationIntent>

    public fun navigate(
        route: String,
        popUpToRoute: String? = null,
        inclusive: Boolean = false,
        isSingleTop: Boolean = true,
        restoreState: Boolean = false,
        saveState: Boolean = false,
    )

    public fun navigateRootDeepLink(
        request: NavDeepLinkRequest,
        navOptions: NavOptions? = null,
    )

    public fun popBackStack()

    /**
     * Pop the back stack until [route] is reached.
     *
     * Use this when you need to "go back several screens at once" - e.g. after
     * a multi-step flow finishes, return all the way to a known anchor screen
     * without pushing a new destination on top.
     *
     * Equivalent to `NavController.popBackStack(route, inclusive)` from
     * Jetpack Navigation. If [route] is not present in the back stack, the
     * controller does not change.
     *
     * @param route the target destination route to pop to.
     * @param inclusive if `true`, [route] itself is also popped. Defaults to `false`,
     *   meaning [route] becomes the current destination.
     */
    public fun popBackStackTo(route: String, inclusive: Boolean = false)

    /**
     * Switch the visible tab in a multi-NavHost layout.
     *
     * Used by features inside one tab to navigate to a different tab without
     * dragging in the URI-based [TabDeeplinkNavigator] API. Emits
     * [NavigationIntent.SwitchTab]; the intent is interpreted by
     * [io.github.chisarabivorts.tessera.MultitabState] (which applies it to
     * the root controller with pop-to-start + save/restore semantics).
     *
     * In a **single-NavHost** layout this is a no-op - there is no root
     * controller to switch.
     *
     * The default implementation is a silent no-op so custom [Navigator]
     * implementations that don't care about multi-NavHost layouts don't have
     * to override it; the standard [Tessera.createNavigator] implementation
     * overrides it to emit the intent.
     *
     * @param route the root-level route of the target tab (matches
     *   [TabFeatureEntry.route]).
     */
    public fun switchToTab(route: String) {
        // Default no-op for backwards source compatibility.
    }
}
