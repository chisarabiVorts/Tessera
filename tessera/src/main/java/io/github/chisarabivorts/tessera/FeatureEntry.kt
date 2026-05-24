package io.github.chisarabivorts.tessera

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * A single navigable destination provided by a feature module.
 *
 * Each feature module implements this interface to register its screens
 * into the Jetpack Compose navigation graph without depending on the `:app` module.
 *
 * The `:app` module collects all [FeatureEntry] instances (typically via DI) and
 * calls [registerGraph] on each of them when building the [NavGraphBuilder].
 *
 * Example:
 * ```
 * class HomeFeatureEntry @Inject constructor() : FeatureEntry {
 *     override val route: String = "home"
 *
 *     @Composable
 *     override fun Content(
 *         navBackStackEntry: NavBackStackEntry,
 *         navigator: Navigator,
 *         resultNavigator: ResultNavigator
 *     ) {
 *         HomeScreen(onOpenDetail = { navigator.navigate("detail/42") })
 *     }
 * }
 * ```
 */
public interface FeatureEntry {

    /** Unique navigation route, e.g. `"home"` or `"details/{id}"`. */
    public val route: String

    /** Navigation arguments expected on this destination. */
    public val arguments: List<NamedNavArgument>
        get() = emptyList()

    /** Deep links that should resolve to this destination. */
    public val deepLinks: List<NavDeepLink>
        get() = emptyList()

    public val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)?
        get() = { fadeIn(tween(durationMillis = 0)) }

    public val exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)?
        get() = { fadeOut(tween(durationMillis = 0)) }

    public val popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)?
        get() = { fadeIn(tween(durationMillis = 0)) }

    public val popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)?
        get() = { fadeOut(tween(durationMillis = 0)) }

    /**
     * Self-registration in the navigation graph.
     *
     * Default implementation registers this entry as a `composable` destination.
     * Override in [NestedFeatureEntry] / [DialogFeatureEntry] for different node types.
     */
    public fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        resultNavigator: ResultNavigator,
    ) {
        navGraphBuilder.composable(
            route = route,
            arguments = arguments,
            deepLinks = deepLinks,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition,
        ) { backStackEntry ->
            Content(
                navBackStackEntry = backStackEntry,
                navigator = navigator,
                resultNavigator = resultNavigator,
            )
        }
    }

    /**
     * The Composable content rendered for this destination.
     *
     * Feature modules implement this to compose their UI. Pass [navigator] callbacks
     * down into screens so they can request navigation.
     */
    @Composable
    public fun Content(
        navBackStackEntry: NavBackStackEntry,
        navigator: Navigator,
        resultNavigator: ResultNavigator,
    ) {
    }
}
