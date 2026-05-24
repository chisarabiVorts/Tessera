package io.github.chisarabivorts.tessera

import android.net.Uri
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TesseraFactoryTest {

    @Test
    fun `createNavigator returns instance that also implements ResultNavigator`() {
        val navigator = Tessera.createNavigator()
        assertTrue(
            "Navigator instance must also implement ResultNavigator",
            navigator is ResultNavigator,
        )
    }

    @Test
    fun `createNavigator returns a fresh instance each call`() {
        val first = Tessera.createNavigator()
        val second = Tessera.createNavigator()
        assertNotSame("each call must produce a new Navigator instance", first, second)
    }

    @Test
    fun `createTabNavigator returns a working TabDeeplinkNavigator`() = runTest {
        val tab = Tessera.createTabNavigator()
        val uri = Uri.parse("tessera://tab/x")
        tab.tabNavigationFlow.test {
            tab.deepLinkToTab(deepLinkUri = uri)
            val item = awaitItem() as TabNavigationAction.DeepLinkToTab
            assertEquals(uri, item.uri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createTabNavigator returns a fresh instance each call`() {
        val first = Tessera.createTabNavigator()
        val second = Tessera.createTabNavigator()
        assertNotSame("each call must produce a new TabDeeplinkNavigator", first, second)
    }
}
