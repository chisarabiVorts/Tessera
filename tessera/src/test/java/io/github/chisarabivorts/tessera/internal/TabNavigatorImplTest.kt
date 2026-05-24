package io.github.chisarabivorts.tessera.internal

import android.net.Uri
import app.cash.turbine.test
import io.github.chisarabivorts.tessera.TabNavigationAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TabNavigatorImplTest {

    @Test
    fun `deepLinkToTab emits DeepLinkToTab with uri and default flags`() = runTest {
        val tab = TabNavigatorImpl()
        val uri = Uri.parse("tessera://tab/home")
        tab.tabNavigationFlow.test {
            tab.deepLinkToTab(deepLinkUri = uri)
            val action = awaitItem() as TabNavigationAction.DeepLinkToTab
            assertEquals(uri, action.uri)
            assertNull(action.popUpToRoute)
            assertFalse(action.inclusive)
            assertTrue(action.isSingleTop)
            assertFalse(action.restoreState)
            assertFalse(action.saveState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deepLinkToTab forwards all flags`() = runTest {
        val tab = TabNavigatorImpl()
        val uri = Uri.parse("tessera://tab/catalog")
        tab.tabNavigationFlow.test {
            tab.deepLinkToTab(
                deepLinkUri = uri,
                popUpToRoute = "home",
                inclusive = true,
                isSingleTop = false,
                restoreState = true,
                saveState = true,
            )
            val action = awaitItem() as TabNavigationAction.DeepLinkToTab
            assertEquals(uri, action.uri)
            assertEquals("home", action.popUpToRoute)
            assertTrue(action.inclusive)
            assertFalse(action.isSingleTop)
            assertTrue(action.restoreState)
            assertTrue(action.saveState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tab and root navigator have independent channels`() = runTest {
        val tab = TabNavigatorImpl()
        val root = NavigatorImpl(ResultNavigatorDelegate())

        tab.tabNavigationFlow.test {
            // Emitting on the root navigator must NOT surface on the tab channel.
            root.navigate("x")
            expectNoEvents()

            // But emitting on the tab navigator does.
            tab.deepLinkToTab(deepLinkUri = Uri.parse("tessera://tab/x"))
            awaitItem()

            cancelAndIgnoreRemainingEvents()
        }
    }
}
