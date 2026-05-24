package io.github.chisarabivorts.tessera.sample.detail

import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import javax.inject.Inject

internal class DetailFeatureEntry @Inject constructor() : FeatureEntry {

    override val route: String = "detail/{$ARG_ID}"

    override val arguments: List<NamedNavArgument> = listOf(
        navArgument(ARG_ID) { type = NavType.StringType },
    )

    override val deepLinks: List<NavDeepLink> = listOf(
        navDeepLink { uriPattern = "tessera://sample/detail/{$ARG_ID}" },
    )

    @Composable
    override fun Content(
        navBackStackEntry: NavBackStackEntry,
        navigator: Navigator,
        resultNavigator: ResultNavigator,
    ) {
        val id = navBackStackEntry.arguments?.getString(ARG_ID).orEmpty()
        DetailScreen(id = id, navigator = navigator, resultNavigator = resultNavigator)
    }

    companion object {
        const val ARG_ID: String = "id"

        // Must match the key Home subscribes to. In production apps this would
        // live in a shared :sample:contract module to avoid duplication; kept
        // inline here to keep the demo focused on the navigation contract.
        const val RESULT_KEY_SELECTED_ID: String = "selected_id"
    }
}
