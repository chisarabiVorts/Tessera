package io.github.chisarabivorts.tessera.internal

import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavOptions
import io.github.chisarabivorts.tessera.NavigationIntent
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Default [Navigator] implementation backed by an unlimited [Channel].
 *
 * Also implements [ResultNavigator] via Kotlin delegation, so a single
 * `Navigator` reference exposes both APIs to feature code - this matches the
 * pattern used in the source project.
 *
 * The channel uses [Channel.UNLIMITED] so [trySend] never fails due to buffer
 * overflow: navigation intents are small and user-rate-limited, and losing one
 * silently would corrupt the user-visible navigation state.
 */
internal class NavigatorImpl(
    private val resultDelegate: ResultNavigator,
) : Navigator,
    ResultNavigator by resultDelegate {

    private val _navigationActions = Channel<NavigationIntent>(Channel.UNLIMITED)

    override val navigationActions: Flow<NavigationIntent>
        get() = _navigationActions.receiveAsFlow()

    override fun navigate(
        route: String,
        popUpToRoute: String?,
        inclusive: Boolean,
        isSingleTop: Boolean,
        restoreState: Boolean,
        saveState: Boolean,
    ) {
        _navigationActions.trySend(
            NavigationIntent.NavigateTo(
                route = route,
                popUpToRoute = popUpToRoute,
                inclusive = inclusive,
                isSingleTop = isSingleTop,
                restoreState = restoreState,
                saveState = saveState,
            ),
        )
    }

    override fun navigateRootDeepLink(
        request: NavDeepLinkRequest,
        navOptions: NavOptions?,
    ) {
        _navigationActions.trySend(
            NavigationIntent.NavigateToDeepLink(request = request, navOptions = navOptions),
        )
    }

    override fun popBackStack() {
        _navigationActions.trySend(NavigationIntent.PopBackStack)
    }

    override fun popBackStackTo(route: String, inclusive: Boolean) {
        _navigationActions.trySend(
            NavigationIntent.PopBackStackTo(route = route, inclusive = inclusive),
        )
    }

    override fun switchToTab(route: String) {
        _navigationActions.trySend(NavigationIntent.SwitchTab(route = route))
    }
}
