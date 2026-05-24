package io.github.chisarabivorts.tessera

import android.net.Uri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavGraph
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
class NestedFeatureEntryTest {

    private fun newController(): TestNavHostController =
        TestNavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            navigatorProvider.addNavigator(DialogNavigator())
        }

    @Test
    fun `nested entry registers a NavGraph node with the same route`() {
        val nested = object : NestedFeatureEntry() {
            override val route: String = "nested"
            override val startRoute: String = "child-a"
            override val children: List<FeatureEntry> = listOf(
                FakeFeatureEntry(route = "child-a"),
                FakeFeatureEntry(route = "child-b"),
            )
        }
        val navigator = Tessera.createNavigator()
        val resultNavigator = navigator as ResultNavigator

        val graph = newController().createGraph(startDestination = "nested") {
            nested.registerGraph(this, navigator, resultNavigator)
        }

        val node = graph.findNode("nested")
        assertNotNull("nested route should be present in graph", node)
        assertTrue("nested entry should produce a NavGraph node", node is NavGraph)
    }

    @Test
    fun `nested entry registers all children inside the nested graph`() {
        val nested = object : NestedFeatureEntry() {
            override val route: String = "nested"
            override val startRoute: String = "child-a"
            override val children: List<FeatureEntry> = listOf(
                FakeFeatureEntry(route = "child-a"),
                FakeFeatureEntry(route = "child-b"),
                FakeFeatureEntry(route = "child-c"),
            )
        }
        val navigator = Tessera.createNavigator()
        val resultNavigator = navigator as ResultNavigator

        val graph = newController().createGraph(startDestination = "nested") {
            nested.registerGraph(this, navigator, resultNavigator)
        }

        val nestedGraph = graph.findNode("nested") as NavGraph
        assertEquals("child-a", nestedGraph.startDestinationRoute)
        assertNotNull(nestedGraph.findNode("child-a"))
        assertNotNull(nestedGraph.findNode("child-b"))
        assertNotNull(nestedGraph.findNode("child-c"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `nested entry rejects a startRoute that does not match any child`() {
        // Regression guard: declaring a startRoute that no child entry exposes
        // used to register an "empty" nested graph that crashed later at the
        // first navigation attempt with an opaque "destination not found"
        // error. registerGraph now fails fast with a helpful message.
        val nested = object : NestedFeatureEntry() {
            override val route: String = "broken"
            override val startRoute: String = "ghost"
            override val children: List<FeatureEntry> = emptyList()
        }
        val navigator = Tessera.createNavigator()
        val resultNavigator = navigator as ResultNavigator

        newController().createGraph(startDestination = "broken") {
            nested.registerGraph(this, navigator, resultNavigator)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `nested entry rejects duplicate child routes`() {
        // Two children declaring the same route would crash later in Jetpack
        // Navigation graph construction with a less informative error.
        val nested = object : NestedFeatureEntry() {
            override val route: String = "graph"
            override val startRoute: String = "shared"
            override val children: List<FeatureEntry> = listOf(
                FakeFeatureEntry(route = "shared"),
                FakeFeatureEntry(route = "shared"),
            )
        }
        val navigator = Tessera.createNavigator()
        val resultNavigator = navigator as ResultNavigator

        newController().createGraph(startDestination = "graph") {
            nested.registerGraph(this, navigator, resultNavigator)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `nested entry rejects a startRoute not present in non-empty children`() {
        // Same check, but children is non-empty - common typo scenario.
        val nested = object : NestedFeatureEntry() {
            override val route: String = "checkout"
            override val startRoute: String = "addresss" // typo
            override val children: List<FeatureEntry> = listOf(
                FakeFeatureEntry(route = "address"),
                FakeFeatureEntry(route = "confirm"),
            )
        }
        val navigator = Tessera.createNavigator()
        val resultNavigator = navigator as ResultNavigator

        newController().createGraph(startDestination = "checkout") {
            nested.registerGraph(this, navigator, resultNavigator)
        }
    }

    @Test
    fun `nested entry forwards arguments and deepLinks to the nested graph`() {
        val nested = object : NestedFeatureEntry() {
            override val route: String = "checkout/{section}"
            override val startRoute: String = "address"
            override val children: List<FeatureEntry> = listOf(FakeFeatureEntry("address"))
            override val arguments = listOf(
                navArgument("section") { type = NavType.StringType },
            )
            override val deepLinks = listOf(
                navDeepLink { uriPattern = "tessera://checkout/{section}" },
            )
        }
        val navigator = Tessera.createNavigator()
        val resultNavigator = navigator as ResultNavigator

        val graph = newController().createGraph(startDestination = "checkout/{section}") {
            nested.registerGraph(this, navigator, resultNavigator)
        }

        val nestedGraph = graph.findNode("checkout/{section}") as NavGraph
        assertNotNull(
            "argument 'section' must be registered on the nested graph",
            nestedGraph.arguments["section"],
        )
        val matching = NavDeepLinkRequest.Builder
            .fromUri(Uri.parse("tessera://checkout/payment"))
            .build()
        assertTrue(
            "registered deep link must match a request against its uriPattern",
            nestedGraph.hasDeepLink(matching),
        )
    }

    private class FakeFeatureEntry(override val route: String) : FeatureEntry
}
