package io.github.chisarabivorts.tessera

import android.net.Uri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.createGraph
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DialogFeatureEntryTest {

    private fun newController(): TestNavHostController =
        TestNavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            navigatorProvider.addNavigator(DialogNavigator())
        }

    @Test
    fun `dialog entry registers a destination with the dialog navigator`() {
        val dialog = object : DialogFeatureEntry {
            override val route: String = "demo-dialog"
        }
        val navigator = Tessera.createNavigator()
        val resultNavigator = navigator as ResultNavigator

        val graph = newController().createGraph(startDestination = "demo-dialog") {
            dialog.registerGraph(this, navigator, resultNavigator)
        }

        val node = graph.findNode("demo-dialog")
        assertNotNull("dialog destination must be present", node)
        assertEquals(
            "dialog destination must be owned by the dialog navigator",
            "dialog",
            node!!.navigatorName,
        )
    }

    @Test
    fun `plain FeatureEntry is registered with the composable navigator`() {
        val plain = object : FeatureEntry {
            override val route: String = "plain"
        }
        val navigator = Tessera.createNavigator()
        val resultNavigator = navigator as ResultNavigator

        val graph = newController().createGraph(startDestination = "plain") {
            plain.registerGraph(this, navigator, resultNavigator)
        }

        val node = graph.findNode("plain")
        assertNotNull(node)
        assertEquals("composable", node!!.navigatorName)
    }

    @Test
    fun `dialog entry forwards arguments and deepLinks to the destination`() {
        val dialog = object : DialogFeatureEntry {
            override val route: String = "demo-dialog/{id}"
            override val arguments = listOf(
                navArgument("id") { type = NavType.StringType },
            )
            override val deepLinks = listOf(
                navDeepLink { uriPattern = "tessera://demo/{id}" },
            )
        }
        val navigator = Tessera.createNavigator()
        val resultNavigator = navigator as ResultNavigator

        val graph = newController().createGraph(startDestination = "demo-dialog/{id}") {
            dialog.registerGraph(this, navigator, resultNavigator)
        }

        val node = graph.findNode("demo-dialog/{id}")
        assertNotNull(node)
        assertNotNull(
            "argument 'id' must be registered on the dialog destination",
            node!!.arguments["id"],
        )
        val matching = NavDeepLinkRequest.Builder
            .fromUri(Uri.parse("tessera://demo/42"))
            .build()
        assertTrue(
            "registered deep link must match a request against its uriPattern",
            node.hasDeepLink(matching),
        )
    }
}
