package io.github.chisarabivorts.tessera

import androidx.navigation.NavController

/**
 * Apply a [NavigationIntent] to this [NavController].
 *
 * Recommended consumer-side translator from Tessera intents to the underlying
 * Jetpack Navigation calls. The helper swallows `IllegalArgumentException`
 * from `NavController.navigate(route, ...)` and `NavController.popBackStack(route, ...)`
 * - both throw when the requested route is not in the controller's graph or
 * back stack. Letting that exception escape the intent collector would kill
 * the single-consumer navigation flow until the host re-composes; silent
 * no-op matches the documented contract on [Navigator.popBackStackTo] and is
 * extended here to [Navigator.navigate] for symmetry - important in
 * multi-NavHost setups where a feature in tab A may legitimately emit an
 * intent that only resolves inside tab B.
 *
 * Cost of this safety net: a typo in a route is silently dropped instead of
 * crashing. The IDE/compile-time checks of type-safe routes (planned for
 * 0.3) will close that gap.
 */
public fun NavController.applyNavigationIntent(intent: NavigationIntent) {
    tryApplyNavigationIntent(intent)
}

/**
 * Like [applyNavigationIntent], but returns whether the intent actually
 * matched a destination on this controller (`true`) or was a no-op because
 * the route/deep link was not in this controller's graph (`false`).
 *
 * Internal: used by [MultitabState]'s smart routing - it tries the active
 * tab's controller first and falls back to searching other tabs only when
 * this returns `false`. This leverages Jetpack Navigation's own recursive
 * matching across nested `NavGraph`s instead of us re-implementing it.
 */
internal fun NavController.tryApplyNavigationIntent(intent: NavigationIntent): Boolean {
    return when (intent) {
        is NavigationIntent.NavigateTo -> {
            try {
                navigate(intent.route) {
                    intent.popUpToRoute?.let { popRoute ->
                        popUpTo(popRoute) {
                            inclusive = intent.inclusive
                            saveState = intent.saveState
                        }
                    }
                    launchSingleTop = intent.isSingleTop
                    restoreState = intent.restoreState
                }
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }

        is NavigationIntent.NavigateToDeepLink -> {
            try {
                navigate(intent.request, intent.navOptions)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }

        NavigationIntent.PopBackStack -> {
            popBackStack()
        }

        is NavigationIntent.PopBackStackTo -> {
            try {
                popBackStack(route = intent.route, inclusive = intent.inclusive)
            } catch (_: IllegalArgumentException) {
                false
            }
        }

        is NavigationIntent.SwitchTab -> {
            // Multi-NavHost-only intent: routed to the root controller by
            // MultitabState. On a single NavController there is no root to
            // switch, so the helper silently ignores it.
            false
        }
    }
}
