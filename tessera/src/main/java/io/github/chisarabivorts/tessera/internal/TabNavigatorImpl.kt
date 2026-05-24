package io.github.chisarabivorts.tessera.internal

import android.net.Uri
import io.github.chisarabivorts.tessera.TabDeeplinkNavigator
import io.github.chisarabivorts.tessera.TabNavigationAction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Default [TabDeeplinkNavigator] implementation backed by an unlimited [Channel].
 *
 * Kept as a separate channel from [NavigatorImpl] so cross-tab deep links
 * never interleave with intra-tab navigation events. Uses [Channel.UNLIMITED]
 * so [trySend] never silently drops an intent under buffer pressure.
 */
internal class TabNavigatorImpl : TabDeeplinkNavigator {

    private val _tabNavigationActions = Channel<TabNavigationAction>(Channel.UNLIMITED)

    override val tabNavigationFlow: Flow<TabNavigationAction> =
        _tabNavigationActions.receiveAsFlow()

    override fun deepLinkToTab(
        deepLinkUri: Uri,
        popUpToRoute: String?,
        inclusive: Boolean,
        isSingleTop: Boolean,
        restoreState: Boolean,
        saveState: Boolean,
    ) {
        _tabNavigationActions.trySend(
            TabNavigationAction.DeepLinkToTab(
                uri = deepLinkUri,
                popUpToRoute = popUpToRoute,
                inclusive = inclusive,
                isSingleTop = isSingleTop,
                restoreState = restoreState,
                saveState = saveState,
            ),
        )
    }
}
