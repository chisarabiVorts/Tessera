package io.github.chisarabivorts.tessera.sample.multitab

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import io.github.chisarabivorts.tessera.TabDeeplinkNavigator
import io.github.chisarabivorts.tessera.TabFeatureEntry
import io.github.chisarabivorts.tessera.rememberMultitabState

/**
 * Multi-NavHost host driven by Tessera's [rememberMultitabState].
 *
 * Tessera itself ships no UI composable - this file shows the canonical
 * recipe: root [NavHost] over tab routes, each tab destination hosts a
 * nested [NavHost] whose `NavHostController` is **created inside** the tab's
 * composable (via [rememberNavController]) and registered with the state
 * holder through `DisposableEffect`.
 *
 * Each tab keeps its own independent back stack across tab switches. The
 * controller's state survives recompositions and process death via the
 * `NavBackStackEntry`'s `SavedStateRegistry` - but reusing a single
 * `NavHostController` instance across destroy/restore cycles is unsafe (it
 * leaks DESTROYED `NavBackStackEntry`s back into composition and crashes).
 */
@Composable
fun MultitabMainScreen(
    tabs: Set<TabFeatureEntry>,
    navigator: Navigator,
    resultNavigator: ResultNavigator,
    tabDeeplinkNavigator: TabDeeplinkNavigator,
) {
    val state = rememberMultitabState(
        tabs = tabs,
        navigator = navigator,
        resultNavigator = resultNavigator,
        tabDeeplinkNavigator = tabDeeplinkNavigator,
    )
    val selectedTab by state.selectedTab

    Scaffold(
        bottomBar = {
            NavigationBar {
                state.tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = tab.route == selectedTab,
                        onClick = { state.selectTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = state.rootNavController,
            startDestination = state.tabs.first().route,
            modifier = Modifier.padding(padding),
        ) {
            state.tabs.forEach { tab ->
                composable(tab.route) {
                    // Per-tab NavController: created in this composable's scope
                    // so its lifecycle is tied to the back stack entry that
                    // owns it. State (back stack + saved instance state) is
                    // preserved across tab switches by rememberNavController's
                    // internal Saver - we get a fresh NavController instance
                    // each time but with the previous back stack restored.
                    val nestedController = rememberNavController()
                    DisposableEffect(nestedController) {
                        state.attachNestedController(tab.route, nestedController)
                        onDispose { state.detachNestedController(tab.route) }
                    }
                    NavHost(
                        navController = nestedController,
                        startDestination = tab.startDestination,
                    ) {
                        tab.children.forEach { child ->
                            child.registerGraph(
                                navGraphBuilder = this,
                                navigator = navigator,
                                resultNavigator = resultNavigator,
                            )
                        }
                    }
                }
            }
        }
    }
}
