package io.github.chisarabivorts.tessera.sample

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.FeatureEntryWithBottomBar
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import io.github.chisarabivorts.tessera.applyNavigationIntent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val START_DESTINATION = "home"

@Composable
fun MainScreen(
    featureEntries: Set<FeatureEntry>,
    bottomBarTabs: Set<FeatureEntryWithBottomBar>,
    navigator: Navigator,
    resultNavigator: ResultNavigator,
    newIntents: Flow<Intent> = emptyFlow(),
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val sortedTabs = bottomBarTabs.sortedBy { it.order }
    val tabRoutes = sortedTabs.map { it.route }.toSet()
    val showBottomBar = currentRoute in tabRoutes

    // Translate intents from Tessera's Navigator into NavController calls.
    LaunchedEffect(navController) {
        navigator.navigationActions.collect { intent ->
            navController.applyNavigationIntent(intent)
        }
    }

    // Forward warm-start deep links into the controller.
    LaunchedEffect(navController) {
        newIntents.collect { intent ->
            navController.handleDeepLink(intent)
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    sortedTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = tab.route == currentRoute,
                            onClick = {
                                when {
                                    // Already on this tab - do nothing.
                                    tab.route == currentRoute -> Unit

                                    // Going back to the start destination - use popBackStackTo.
                                    // Avoids the "navigate-to-current with popUpTo" edge case in
                                    // Compose Navigation, and showcases the new pop API.
                                    tab.route == START_DESTINATION -> {
                                        navigator.popBackStackTo(
                                            route = START_DESTINATION,
                                            inclusive = false,
                                        )
                                    }

                                    // Going to a non-start tab - standard tab navigation
                                    // pattern with state preservation between tabs.
                                    else -> {
                                        navigator.navigate(
                                            route = tab.route,
                                            popUpToRoute = START_DESTINATION,
                                            saveState = true,
                                            restoreState = true,
                                            isSingleTop = true,
                                        )
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = START_DESTINATION,
            modifier = Modifier.padding(innerPadding),
        ) {
            featureEntries.forEach { entry ->
                entry.registerGraph(
                    navGraphBuilder = this,
                    navigator = navigator,
                    resultNavigator = resultNavigator,
                )
            }
        }
    }
}
