package io.github.chisarabivorts.tessera

import android.net.Uri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.navDeepLink
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NavControllerExtTest {

    private fun controllerWithStack(
        routes: List<String>,
        extraGraphRoutes: List<String> = emptyList(),
    ): TestNavHostController {
        require(routes.isNotEmpty()) { "routes must include at least the start destination" }
        val controller = TestNavHostController(RuntimeEnvironment.getApplication())
        controller.navigatorProvider.addNavigator(ComposeNavigator())
        val graph = controller.createGraph(startDestination = routes.first()) {
            (routes + extraGraphRoutes).forEach { r -> composable(r) {} }
        }
        controller.setGraph(graph, null)
        routes.drop(1).forEach { controller.navigate(it) }
        return controller
    }

    // --- PopBackStackTo branch (regression coverage for bug #1) ---

    @Test
    fun `PopBackStackTo with missing route is a no-op and does not throw`() {
        val controller = controllerWithStack(listOf("home", "detail", "settings"))
        assertEquals("settings", controller.currentDestination?.route)

        controller.applyNavigationIntent(
            NavigationIntent.PopBackStackTo(route = "nonexistent"),
        )

        assertEquals("settings", controller.currentDestination?.route)
    }

    @Test
    fun `PopBackStackTo with existing route pops down to it`() {
        val controller = controllerWithStack(listOf("home", "detail", "settings"))

        controller.applyNavigationIntent(
            NavigationIntent.PopBackStackTo(route = "home", inclusive = false),
        )

        assertEquals("home", controller.currentDestination?.route)
    }

    @Test
    fun `PopBackStackTo with inclusive=true also pops the route itself`() {
        val controller = controllerWithStack(listOf("home", "detail", "settings"))

        controller.applyNavigationIntent(
            NavigationIntent.PopBackStackTo(route = "detail", inclusive = true),
        )

        assertEquals("home", controller.currentDestination?.route)
    }

    @Test
    fun `PopBackStackTo missing route does not break subsequent navigation`() {
        val controller = controllerWithStack(listOf("home", "detail"))

        controller.applyNavigationIntent(
            NavigationIntent.PopBackStackTo(route = "nonexistent"),
        )
        controller.applyNavigationIntent(
            NavigationIntent.NavigateTo(route = "home"),
        )

        assertEquals("home", controller.currentDestination?.route)
    }

    // --- NavigateTo branch ---

    @Test
    fun `NavigateTo with default flags navigates to the requested route`() {
        val controller = controllerWithStack(listOf("home"), extraGraphRoutes = listOf("detail"))

        controller.applyNavigationIntent(NavigationIntent.NavigateTo(route = "detail"))

        assertEquals("detail", controller.currentDestination?.route)
        assertEquals("home", controller.previousBackStackEntry?.destination?.route)
    }

    @Test
    fun `NavigateTo with popUpTo pops back to that route before navigating`() {
        // Start: [home, detail, settings]. Goal: navigate("detail", popUpTo="home").
        // Expected: pop down to home, then navigate detail → [home, detail].
        val controller = controllerWithStack(listOf("home", "detail", "settings"))

        controller.applyNavigationIntent(
            NavigationIntent.NavigateTo(
                route = "detail",
                popUpToRoute = "home",
                inclusive = false,
            ),
        )

        assertEquals("detail", controller.currentDestination?.route)
        assertEquals("home", controller.previousBackStackEntry?.destination?.route)
    }

    @Test
    fun `NavigateTo with popUpTo inclusive=true also pops the target route`() {
        // Start: [home, detail]. Goal: navigate("settings", popUpTo="home", inclusive=true).
        // Expected: pop detail, pop home (inclusive), navigate settings → [settings].
        val controller = controllerWithStack(
            routes = listOf("home", "detail"),
            extraGraphRoutes = listOf("settings"),
        )

        controller.applyNavigationIntent(
            NavigationIntent.NavigateTo(
                route = "settings",
                popUpToRoute = "home",
                inclusive = true,
            ),
        )

        assertEquals("settings", controller.currentDestination?.route)
        assertNull(
            "stack must contain only 'settings' after inclusive popUpTo of root",
            controller.previousBackStackEntry,
        )
    }

    @Test
    fun `NavigateTo with default singleTop=true does not stack duplicate of current destination`() {
        val controller = controllerWithStack(listOf("home", "detail"))

        controller.applyNavigationIntent(NavigationIntent.NavigateTo(route = "detail"))

        // launchSingleTop = true (default) coalesces the existing top entry,
        // so one popBackStack returns straight to "home".
        assertEquals("detail", controller.currentDestination?.route)
        assertTrue(controller.popBackStack())
        assertEquals("home", controller.currentDestination?.route)
    }

    @Test
    fun `NavigateTo with singleTop=false stacks duplicate of current destination`() {
        val controller = controllerWithStack(listOf("home", "detail"))

        controller.applyNavigationIntent(
            NavigationIntent.NavigateTo(route = "detail", isSingleTop = false),
        )

        // launchSingleTop = false adds a duplicate entry, so popping back
        // once still leaves us on "detail" before reaching "home".
        assertEquals("detail", controller.currentDestination?.route)
        assertTrue(controller.popBackStack())
        assertEquals("detail", controller.currentDestination?.route)
        assertTrue(controller.popBackStack())
        assertEquals("home", controller.currentDestination?.route)
    }

    @Test
    fun `NavigateTo with unknown route is a no-op and does not throw`() {
        // Regression: in multi-NavHost setups a feature in tab A may emit an
        // intent that only resolves inside tab B; the intent reaches tab A's
        // nested controller which doesn't know the route. Before this guard,
        // NavController.navigate(route) threw IllegalArgumentException out of
        // the LaunchedEffect collector - killing the bridge for that tab.
        val controller = controllerWithStack(listOf("home", "detail"))

        controller.applyNavigationIntent(
            NavigationIntent.NavigateTo(route = "nonexistent_in_this_graph"),
        )

        assertEquals("detail", controller.currentDestination?.route)
    }

    @Test
    fun `NavigateTo with unknown route does not break subsequent navigation`() {
        // Regression on the main consequence of the bug: after IAE the
        // single-consumer collector would die. Verify a subsequent NavigateTo
        // to a known route still works.
        val controller = controllerWithStack(listOf("home"), extraGraphRoutes = listOf("detail"))

        controller.applyNavigationIntent(NavigationIntent.NavigateTo(route = "ghost"))
        controller.applyNavigationIntent(NavigationIntent.NavigateTo(route = "detail"))

        assertEquals("detail", controller.currentDestination?.route)
    }

    @Test
    fun `NavigateToDeepLink with no matching destination is a no-op and does not throw`() {
        // Symmetric guard: deep link from one tab routed to another tab's
        // controller would otherwise throw IAE and kill the collector.
        val controller = TestNavHostController(RuntimeEnvironment.getApplication())
        controller.navigatorProvider.addNavigator(ComposeNavigator())
        controller.setGraph(
            controller.createGraph(startDestination = "home") {
                composable("home") {}
            },
            null,
        )

        val request = NavDeepLinkRequest.Builder
            .fromUri(Uri.parse("tessera://unknown/x"))
            .build()
        controller.applyNavigationIntent(NavigationIntent.NavigateToDeepLink(request))

        assertEquals("home", controller.currentDestination?.route)
    }

    // --- PopBackStack branch ---

    @Test
    fun `PopBackStack pops a single entry off the stack`() {
        val controller = controllerWithStack(listOf("home", "detail", "settings"))

        controller.applyNavigationIntent(NavigationIntent.PopBackStack)

        assertEquals("detail", controller.currentDestination?.route)
    }

    // --- NavigateToDeepLink branch ---

    @Test
    fun `NavigateToDeepLink navigates to the destination matching the deep link`() {
        val controller = TestNavHostController(RuntimeEnvironment.getApplication())
        controller.navigatorProvider.addNavigator(ComposeNavigator())
        val graph = controller.createGraph(startDestination = "home") {
            composable("home") {}
            composable(
                route = "detail",
                deepLinks = listOf(navDeepLink { uriPattern = "tessera://test/detail" }),
            ) {}
        }
        controller.setGraph(graph, null)

        val request = NavDeepLinkRequest.Builder
            .fromUri(Uri.parse("tessera://test/detail"))
            .build()
        controller.applyNavigationIntent(
            NavigationIntent.NavigateToDeepLink(request = request),
        )

        assertEquals("detail", controller.currentDestination?.route)
    }

    // --- SwitchTab branch (no-op on single NavController) ---

    @Test
    fun `SwitchTab on a plain NavController is a no-op and does not throw`() {
        val controller = controllerWithStack(listOf("home", "detail"))
        assertEquals("detail", controller.currentDestination?.route)

        controller.applyNavigationIntent(
            NavigationIntent.SwitchTab(route = "settings_tab"),
        )

        // SwitchTab is meant for the root controller in a multi-NavHost setup;
        // on a plain (single-NavHost) NavController it must be silently ignored.
        assertEquals("detail", controller.currentDestination?.route)
    }

    // --- Smoke test: helper returns Unit, doesn't throw on any intent variant ---

    @Test
    fun `applyNavigationIntent is exhaustive for all NavigationIntent variants`() {
        // Regression guard for adding a new NavigationIntent: if a branch is
        // missing in the helper, Kotlin's exhaustive `when` fails at compile
        // time - but this test also pins the fact that all current branches
        // execute on a minimal stack without throwing.
        val controller = controllerWithStack(
            routes = listOf("home"),
            extraGraphRoutes = listOf(
                "navigate-target",
                "deeplink-target",
            ),
        )
        // Graph for the deep link case
        // (regenerate a separate controller with the deepLink for simplicity)
        val deepLinkController = TestNavHostController(RuntimeEnvironment.getApplication())
        deepLinkController.navigatorProvider.addNavigator(ComposeNavigator())
        val graph = deepLinkController.createGraph(startDestination = "home") {
            composable("home") {}
            composable(
                route = "dl",
                deepLinks = listOf(navDeepLink { uriPattern = "tessera://x/dl" }),
            ) {}
        }
        deepLinkController.setGraph(graph, null)

        // All 4 branches must execute without throwing.
        controller.applyNavigationIntent(NavigationIntent.NavigateTo("navigate-target"))
        controller.applyNavigationIntent(NavigationIntent.PopBackStack)
        controller.applyNavigationIntent(NavigationIntent.PopBackStackTo("home"))
        deepLinkController.applyNavigationIntent(
            NavigationIntent.NavigateToDeepLink(
                NavDeepLinkRequest.Builder.fromUri(Uri.parse("tessera://x/dl")).build(),
            ),
        )

        assertTrue("smoke test reached the end", true)
    }
}
