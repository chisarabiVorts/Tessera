package io.github.chisarabivorts.tessera.internal

import android.net.Uri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavOptions
import app.cash.turbine.test
import io.github.chisarabivorts.tessera.NavigationIntent
import io.github.chisarabivorts.tessera.Navigator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigatorImplTest {

    @Test
    fun `navigate emits NavigateTo intent with default flags`() = runTest {
        val navigator = NavigatorImpl(ResultNavigatorDelegate())
        navigator.navigationActions.test {
            navigator.navigate(route = "home")
            val item = awaitItem() as NavigationIntent.NavigateTo
            assertEquals("home", item.route)
            assertNull(item.popUpToRoute)
            assertFalse(item.inclusive)
            assertTrue(item.isSingleTop)
            assertFalse(item.restoreState)
            assertFalse(item.saveState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `navigate forwards all popUpTo, save and restore flags verbatim`() = runTest {
        val navigator = NavigatorImpl(ResultNavigatorDelegate())
        navigator.navigationActions.test {
            navigator.navigate(
                route = "details/1",
                popUpToRoute = "home",
                inclusive = true,
                isSingleTop = false,
                restoreState = true,
                saveState = true,
            )
            val item = awaitItem() as NavigationIntent.NavigateTo
            assertEquals("details/1", item.route)
            assertEquals("home", item.popUpToRoute)
            assertTrue(item.inclusive)
            assertFalse(item.isSingleTop)
            assertTrue(item.restoreState)
            assertTrue(item.saveState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `popBackStack emits PopBackStack singleton intent`() = runTest {
        val navigator = NavigatorImpl(ResultNavigatorDelegate())
        navigator.navigationActions.test {
            navigator.popBackStack()
            assertSame(NavigationIntent.PopBackStack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `popBackStackTo emits PopBackStackTo with default exclusive flag`() = runTest {
        val navigator = NavigatorImpl(ResultNavigatorDelegate())
        navigator.navigationActions.test {
            navigator.popBackStackTo(route = "home")
            val item = awaitItem() as NavigationIntent.PopBackStackTo
            assertEquals("home", item.route)
            assertFalse(item.inclusive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `popBackStackTo forwards inclusive flag`() = runTest {
        val navigator = NavigatorImpl(ResultNavigatorDelegate())
        navigator.navigationActions.test {
            navigator.popBackStackTo(route = "checkout", inclusive = true)
            val item = awaitItem() as NavigationIntent.PopBackStackTo
            assertEquals("checkout", item.route)
            assertTrue(item.inclusive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `navigateRootDeepLink forwards request and options`() = runTest {
        val navigator = NavigatorImpl(ResultNavigatorDelegate())
        val request = NavDeepLinkRequest.Builder
            .fromUri(Uri.parse("tessera://test/x"))
            .build()
        navigator.navigationActions.test {
            navigator.navigateRootDeepLink(request)
            val item = awaitItem() as NavigationIntent.NavigateToDeepLink
            assertSame(request, item.request)
            assertNull(item.navOptions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple sequential intents preserve order`() = runTest {
        val navigator = NavigatorImpl(ResultNavigatorDelegate())
        navigator.navigationActions.test {
            navigator.navigate("a")
            navigator.navigate("b")
            navigator.popBackStack()
            assertEquals("a", (awaitItem() as NavigationIntent.NavigateTo).route)
            assertEquals("b", (awaitItem() as NavigationIntent.NavigateTo).route)
            assertSame(NavigationIntent.PopBackStack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Navigator also implements ResultNavigator via delegate`() = runTest {
        val delegate = ResultNavigatorDelegate()
        val navigator = NavigatorImpl(delegate)
        navigator.resultFlow<String>("k").test {
            navigator.publishResult("k", "value")
            assertEquals("value", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switchToTab emits SwitchTab intent with the tab route`() = runTest {
        val navigator = NavigatorImpl(ResultNavigatorDelegate())
        navigator.navigationActions.test {
            navigator.switchToTab(route = "settings_tab")
            val item = awaitItem() as NavigationIntent.SwitchTab
            assertEquals("settings_tab", item.route)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `default Navigator switchToTab implementation is a silent no-op`() {
        // Tessera ships Navigator.switchToTab with a default empty body so
        // custom Navigator implementations stay source-compatible across
        // 0.1 → 0.2. This test pins that contract: an implementation that
        // overrides everything else but NOT switchToTab must not throw.
        val customNavigator = object : Navigator {
            override val navigationActions: Flow<NavigationIntent> = emptyFlow()
            override fun navigate(
                route: String,
                popUpToRoute: String?,
                inclusive: Boolean,
                isSingleTop: Boolean,
                restoreState: Boolean,
                saveState: Boolean,
            ) = Unit
            override fun navigateRootDeepLink(
                request: NavDeepLinkRequest,
                navOptions: NavOptions?,
            ) = Unit
            override fun popBackStack() = Unit
            override fun popBackStackTo(route: String, inclusive: Boolean) = Unit
            // switchToTab intentionally NOT overridden - relies on default impl.
        }
        customNavigator.switchToTab("any_tab_route") // must not throw
    }

    @Test
    fun `intents are not dropped when emitted faster than collected`() = runTest {
        // Regression for bug #4: with Channel.BUFFERED (cap 64), intents
        // emitted before the collector attached were dropped after the 64th.
        // With UNLIMITED, every intent must arrive.
        val navigator = NavigatorImpl(ResultNavigatorDelegate())
        val total = 200

        repeat(total) { i -> navigator.navigate("route-$i") }

        navigator.navigationActions.test {
            repeat(total) { i ->
                val item = awaitItem() as NavigationIntent.NavigateTo
                assertEquals("route-$i", item.route)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
