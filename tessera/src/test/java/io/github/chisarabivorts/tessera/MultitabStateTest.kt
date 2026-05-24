package io.github.chisarabivorts.tessera

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDeepLink
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.createGraph
import androidx.navigation.navDeepLink
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MultitabStateTest {

    private val fakeIcon: ImageVector = ImageVector.Builder(
        name = "fake",
        defaultWidth = 1.dp,
        defaultHeight = 1.dp,
        viewportWidth = 1f,
        viewportHeight = 1f,
    ).build()

    private fun controller(routes: List<String>): TestNavHostController {
        val c = TestNavHostController(RuntimeEnvironment.getApplication())
        c.navigatorProvider.addNavigator(ComposeNavigator())
        c.setGraph(
            c.createGraph(startDestination = routes.first()) {
                routes.forEach { r -> composable(r) {} }
            },
            null,
        )
        return c
    }

    /**
     * Builds a nested controller that mirrors a tab's [TabFeatureEntry.children] -
     * each child becomes a composable destination with its declared deep links.
     */
    private fun nestedControllerFor(children: Set<FeatureEntry>): TestNavHostController {
        require(children.isNotEmpty()) { "tab must have at least one child for nested controller setup" }
        val c = TestNavHostController(RuntimeEnvironment.getApplication())
        c.navigatorProvider.addNavigator(ComposeNavigator())
        val ordered = children.toList()
        val graph = c.createGraph(startDestination = ordered.first().route) {
            ordered.forEach { entry ->
                composable(route = entry.route, deepLinks = entry.deepLinks) {}
            }
        }
        c.setGraph(graph, null)
        return c
    }

    private fun stateFor(
        tabs: List<TabFeatureEntry>,
        currentTab: String? = tabs.first().route,
        resetActiveTabOnReselect: Boolean = false,
    ): MultitabState {
        val rootController = controller(tabs.map { it.route })
        if (currentTab != null && currentTab != tabs.first().route) {
            rootController.navigate(currentTab)
        }
        val state = MultitabState(
            tabs = tabs,
            rootNavController = rootController,
            selectedTab = mutableStateOf(currentTab),
            resetActiveTabOnReselect = resetActiveTabOnReselect,
        )
        // Tests pre-attach every tab's nested controller. In production, this
        // is done by each tab's composable via DisposableEffect when it enters
        // composition; the test simulates "all tabs attached" so route
        // assertions are easy.
        //
        // For tap-to-top tests the helper needs the controller graph to know
        // all of the tab's child routes (so popBackStack to startDestination
        // works once we've navigated deeper). When children are provided the
        // graph contains them; otherwise the controller has just the start
        // destination.
        tabs.forEach { tab ->
            val routes = if (tab.children.isEmpty()) {
                listOf(tab.startDestination)
            } else {
                buildList {
                    add(tab.startDestination)
                    tab.children.forEach { child ->
                        if (child.route != tab.startDestination) add(child.route)
                    }
                }
            }
            state.attachNestedController(tab.route, controller(routes))
        }
        return state
    }

    /**
     * Like [stateFor], but each tab's nested controller knows about all the
     * tab's children (including their deep links). Used by bridge tests that
     * need real nested navigation / deep-link matching.
     */
    private fun stateWithChildren(
        tabs: List<TabFeatureEntry>,
        currentTab: String? = tabs.first().route,
    ): MultitabState {
        val rootController = controller(tabs.map { it.route })
        if (currentTab != null && currentTab != tabs.first().route) {
            rootController.navigate(currentTab)
        }
        val state = MultitabState(
            tabs = tabs,
            rootNavController = rootController,
            selectedTab = mutableStateOf(currentTab),
        )
        tabs.forEach { tab ->
            state.attachNestedController(tab.route, nestedControllerFor(tab.children))
        }
        return state
    }

    private fun MultitabState.requireNested(route: String): NavHostController =
        nestedController(route) ?: error("test setup: tab '$route' has no attached controller")

    // --- nestedController ---

    @Test
    fun `nestedController returns the controller for a registered tab`() {
        val state = stateFor(
            listOf(
                FakeTab("home_tab", "home"),
                FakeTab("settings_tab", "settings"),
            ),
        )
        assertNotNull(state.nestedController("home_tab"))
        assertNotNull(state.nestedController("settings_tab"))
    }

    @Test
    fun `nestedController returns null for an unknown tab`() {
        // After the lifecycle refactor, nestedController is nullable. Unknown
        // (or not-currently-attached) tabs return null instead of throwing -
        // callers that consume intents (handleNavigatorIntent /
        // handleTabDeeplinkAction) handle null by no-op or queueing.
        val state = stateFor(listOf(FakeTab("home_tab", "home")))
        assertNull(state.nestedController("unknown"))
    }

    @Test
    fun `detachNestedController removes the controller from the state`() {
        val state = stateFor(listOf(FakeTab("home_tab", "home")))
        assertNotNull(state.nestedController("home_tab"))
        state.detachNestedController("home_tab")
        assertNull(state.nestedController("home_tab"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `attachNestedController rejects an unknown tab route`() {
        val state = stateFor(listOf(FakeTab("home_tab", "home")))
        state.attachNestedController("unknown_tab", controller(listOf("anywhere")))
    }

    @Test
    fun `pending deep link is dispatched when the target tab attaches its controller`() {
        // Bridge needs to handle the race where the root NavController switches
        // tab BEFORE the new tab's composable enters composition (so no nested
        // controller is attached yet at the time we want to apply the deep link).
        val homeEntry = FakeEntry(
            route = "home",
            deepLinks = listOf(navDeepLink { uriPattern = "tessera://app/home" }),
        )
        val settingsEntry = FakeEntry(
            route = "settings",
            deepLinks = listOf(navDeepLink { uriPattern = "tessera://app/settings" }),
        )
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(homeEntry)),
            FakeTab("settings_tab", "settings", children = setOf(settingsEntry)),
        )
        val rootController = controller(tabs.map { it.route })
        val state = MultitabState(
            tabs = tabs,
            rootNavController = rootController,
            selectedTab = mutableStateOf("home_tab"),
        )
        // Only home_tab is attached at the time the deep link arrives.
        state.attachNestedController("home_tab", nestedControllerFor(setOf(homeEntry)))

        state.handleTabDeeplinkAction(
            TabNavigationAction.DeepLinkToTab(uri = Uri.parse("tessera://app/settings")),
        )

        // settings_tab not attached yet - deep link is queued.
        // Now its composable enters composition and attaches.
        val settingsNested = nestedControllerFor(setOf(settingsEntry))
        state.attachNestedController("settings_tab", settingsNested)

        // Queued deep link must have been applied on attach.
        assertEquals("settings", settingsNested.currentDestination?.route)
    }

    // --- selectTab ---

    @Test
    fun `selectTab navigates the root controller to the requested tab`() {
        val tabs = listOf(
            FakeTab("home_tab", "home"),
            FakeTab("settings_tab", "settings"),
        )
        val state = stateFor(tabs)

        state.selectTab("settings_tab")

        assertEquals("settings_tab", state.rootNavController.currentDestination?.route)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `selectTab throws for an unknown tab`() {
        val state = stateFor(listOf(FakeTab("home_tab", "home")))
        state.selectTab("unknown")
    }

    @Test
    fun `selectTab is a no-op when already on the requested tab`() {
        val tabs = listOf(FakeTab("home_tab", "home"))
        val state = stateFor(tabs)
        // already on home_tab - should not throw, should not change anything
        state.selectTab("home_tab")
        assertEquals("home_tab", state.rootNavController.currentDestination?.route)
    }

    // --- resetActiveTabOnReselect (tap-to-top) ---

    @Test
    fun `selectTab on active tab is no-op when resetActiveTabOnReselect is false`() {
        // Defaults to false. Push deeper inside the active tab; re-tapping the
        // tab should leave the deep state intact.
        val tabs = listOf(
            FakeTab(
                "home_tab",
                "home",
                children = setOf(FakeEntry("home"), FakeEntry("detail")),
            ),
        )
        val state = stateFor(tabs, resetActiveTabOnReselect = false)
        val home = state.requireNested("home_tab")
        home.navigate("detail")
        assertEquals("detail", home.currentDestination?.route)

        state.selectTab("home_tab") // re-tap the active tab

        assertEquals(
            "tap on active tab must NOT pop the nested stack when flag is false",
            "detail",
            home.currentDestination?.route,
        )
    }

    @Test
    fun `selectTab on active tab pops nested back to start when resetActiveTabOnReselect is true`() {
        // Main feature: Instagram-style "tap-to-top". User is deep inside the
        // active tab; re-tapping the tab pops the nested stack down to the
        // tab's startDestination.
        val tabs = listOf(
            FakeTab(
                "home_tab",
                "home",
                children = setOf(FakeEntry("home"), FakeEntry("detail")),
            ),
        )
        val state = stateFor(tabs, resetActiveTabOnReselect = true)
        val home = state.requireNested("home_tab")
        home.navigate("detail")
        assertEquals("detail", home.currentDestination?.route)

        state.selectTab("home_tab") // re-tap → must reset

        assertEquals(
            "tap on active tab MUST pop the nested stack to start when flag is true",
            "home",
            home.currentDestination?.route,
        )
    }

    @Test
    fun `selectTab on active tab is a safe no-op when already at start and flag is true`() {
        // Edge case: flag is on, but the nested stack already sits at its
        // startDestination - nothing to pop. Must not throw (the IAE from
        // popBackStack(route) on an absent route is caught internally).
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"))),
        )
        val state = stateFor(tabs, resetActiveTabOnReselect = true)
        assertEquals("home", state.requireNested("home_tab").currentDestination?.route)

        state.selectTab("home_tab") // re-tap on already-at-start

        // Still on start, no exception thrown.
        assertEquals("home", state.requireNested("home_tab").currentDestination?.route)
    }

    @Test
    fun `selectTab on a different tab still switches normally when flag is true`() {
        // The flag must only affect the "reselect active" path. Switching to
        // a different tab must work the same way regardless of the flag.
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"))),
            FakeTab("settings_tab", "settings", children = setOf(FakeEntry("settings"))),
        )
        val state = stateFor(tabs, resetActiveTabOnReselect = true)

        state.selectTab("settings_tab")

        assertEquals("settings_tab", state.rootNavController.currentDestination?.route)
    }

    // --- tabRouteForUri (internal - driven by TabDeeplinkNavigator bridge) ---

    @Test
    fun `tabRouteForUri matches the tab whose child has a matching deep link`() {
        val homeEntry = FakeEntry(
            route = "home",
            deepLinks = listOf(navDeepLink { uriPattern = "tessera://test/home" }),
        )
        val detailEntry = FakeEntry(
            route = "detail/{id}",
            deepLinks = listOf(navDeepLink { uriPattern = "tessera://test/detail/{id}" }),
        )
        val settingsEntry = FakeEntry(
            route = "settings",
            deepLinks = listOf(navDeepLink { uriPattern = "tessera://test/settings" }),
        )

        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(homeEntry, detailEntry)),
            FakeTab("settings_tab", "settings", children = setOf(settingsEntry)),
        )
        val state = stateFor(tabs)

        assertEquals("home_tab", state.tabRouteForUri(Uri.parse("tessera://test/home")))
        assertEquals("home_tab", state.tabRouteForUri(Uri.parse("tessera://test/detail/42")))
        assertEquals("settings_tab", state.tabRouteForUri(Uri.parse("tessera://test/settings")))
    }

    @Test
    fun `tabRouteForUri returns null when no tab declares a matching deep link`() {
        val state = stateFor(
            listOf(
                FakeTab(
                    "home_tab",
                    "home",
                    children = setOf(
                        FakeEntry(
                            route = "home",
                            deepLinks = listOf(navDeepLink { uriPattern = "tessera://test/home" }),
                        ),
                    ),
                ),
            ),
        )
        assertNull(state.tabRouteForUri(Uri.parse("tessera://nowhere")))
    }

    @Test
    fun `tabRouteForUri ignores tabs whose children declare no deep links`() {
        val state = stateFor(
            listOf(
                FakeTab(
                    "home_tab",
                    "home",
                    children = setOf(FakeEntry(route = "home")),
                ),
            ),
        )
        assertNull(state.tabRouteForUri(Uri.parse("tessera://anything")))
    }

    // --- requireDistinctTabRoutes (validates rememberMultitabState input) ---

    @Test
    fun `requireDistinctTabRoutes passes when all routes are unique`() {
        val tabs = listOf(
            FakeTab("home_tab", "home"),
            FakeTab("settings_tab", "settings"),
        )
        requireDistinctTabRoutes(tabs) // must not throw
    }

    @Test(expected = IllegalArgumentException::class)
    fun `requireDistinctTabRoutes throws when two tabs share the same route`() {
        val tabs = listOf(
            FakeTab("home_tab", "home"),
            FakeTab("home_tab", "other"), // same route, different start destination
        )
        requireDistinctTabRoutes(tabs)
    }

    // --- handleNavigatorIntent (bridge: Navigator → active tab's nested) ---

    @Test
    fun `handleNavigatorIntent SwitchTab switches the root controller to the target tab`() {
        val tabs = listOf(
            FakeTab("home_tab", "home"),
            FakeTab("settings_tab", "settings"),
        )
        val state = stateFor(tabs)

        state.handleNavigatorIntent(NavigationIntent.SwitchTab(route = "settings_tab"))

        assertEquals("settings_tab", state.rootNavController.currentDestination?.route)
    }

    @Test
    fun `handleNavigatorIntent NavigateTo applies to the active tab nested controller`() {
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"), FakeEntry("detail"))),
            FakeTab("settings_tab", "settings", children = setOf(FakeEntry("settings"))),
        )
        val state = stateWithChildren(tabs)

        state.handleNavigatorIntent(NavigationIntent.NavigateTo(route = "detail"))

        assertEquals("detail", state.requireNested("home_tab").currentDestination?.route)
        assertEquals(
            "settings nested controller must be untouched",
            "settings",
            state.requireNested("settings_tab").currentDestination?.route,
        )
    }

    @Test
    fun `handleNavigatorIntent PopBackStack applies to the active tab nested controller`() {
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"), FakeEntry("detail"))),
        )
        val state = stateWithChildren(tabs)
        // Push detail onto the home tab's nested stack.
        state.requireNested("home_tab").navigate("detail")

        state.handleNavigatorIntent(NavigationIntent.PopBackStack)

        assertEquals("home", state.requireNested("home_tab").currentDestination?.route)
    }

    @Test
    fun `handleNavigatorIntent PopBackStackTo applies to the active tab nested controller`() {
        val tabs = listOf(
            FakeTab(
                "home_tab",
                "home",
                children = setOf(FakeEntry("home"), FakeEntry("detail"), FakeEntry("more")),
            ),
        )
        val state = stateWithChildren(tabs)
        val homeNested = state.requireNested("home_tab")
        homeNested.navigate("detail")
        homeNested.navigate("more")

        state.handleNavigatorIntent(NavigationIntent.PopBackStackTo(route = "home"))

        assertEquals("home", homeNested.currentDestination?.route)
    }

    @Test
    fun `handleNavigatorIntent PopBackStack with null selectedTab is silently dropped`() {
        // PopBackStack has no route info, so it cannot be smart-routed to
        // any specific tab. With no active tab, drop silently.
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"), FakeEntry("detail"))),
        )
        val state = stateWithChildren(tabs, currentTab = null)
        state.requireNested("home_tab").navigate("detail")  // push for visibility

        state.handleNavigatorIntent(NavigationIntent.PopBackStack)

        // Nested controller untouched - no active tab to apply to.
        assertEquals("detail", state.requireNested("home_tab").currentDestination?.route)
    }

    // --- Smart routing across tabs ---

    @Test
    fun `NavigateTo to a route in another tab switches to that tab and applies there`() {
        // The scenario from the multitab sample: pressing "Open Settings" on
        // the Home tab fires navigator.navigate("settings"). "settings" is a
        // child of settings_tab, not home_tab. Without smart routing the intent
        // is silently dropped by applyNavigationIntent's IAE catch.
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"))),
            FakeTab("settings_tab", "settings", children = setOf(FakeEntry("settings"))),
        )
        val state = stateWithChildren(tabs)  // currentTab = home_tab

        state.handleNavigatorIntent(NavigationIntent.NavigateTo(route = "settings"))

        assertEquals("settings_tab", state.rootNavController.currentDestination?.route)
        assertEquals("settings", state.requireNested("settings_tab").currentDestination?.route)
    }

    @Test
    fun `PopBackStackTo to a route in another tab switches and pops there`() {
        // Scenario from sample ConfirmScreen: popBackStackTo("home") emitted
        // while on the checkout tab. "home" is owned by home_tab.
        // checkout_tab is first so it is the root start destination - this
        // sidesteps a TestNavHostController quirk where navigating to a tab
        // that IS the start destination after popUpTo(start) leaves
        // currentDestination unchanged.
        val tabs = listOf(
            FakeTab("checkout_tab", "checkout", children = setOf(FakeEntry("checkout"))),
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"), FakeEntry("detail"))),
        )
        val state = stateWithChildren(tabs, currentTab = "checkout_tab")
        // Push a non-start entry on home_tab so the pop has somewhere to pop from.
        state.requireNested("home_tab").navigate("detail")

        state.handleNavigatorIntent(NavigationIntent.PopBackStackTo(route = "home"))

        assertEquals("home_tab", state.rootNavController.currentDestination?.route)
        assertEquals("home", state.requireNested("home_tab").currentDestination?.route)
    }

    @Test
    fun `NavigateTo with route owned by no tab is a silent no-op`() {
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"))),
            FakeTab("settings_tab", "settings", children = setOf(FakeEntry("settings"))),
        )
        val state = stateWithChildren(tabs)  // currentTab = home_tab

        state.handleNavigatorIntent(NavigationIntent.NavigateTo(route = "ghost"))

        // No switch, no nested changes.
        assertEquals("home_tab", state.rootNavController.currentDestination?.route)
        assertEquals("home", state.requireNested("home_tab").currentDestination?.route)
        assertEquals("settings", state.requireNested("settings_tab").currentDestination?.route)
    }

    @Test
    fun `NavigateTo to active tab's own route does not trigger a tab switch`() {
        // Regression guard: smart routing must prefer the active tab when it
        // owns the route, not "fall through" to children search.
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"), FakeEntry("detail"))),
            FakeTab("settings_tab", "settings", children = setOf(FakeEntry("settings"))),
        )
        val state = stateWithChildren(tabs)

        state.handleNavigatorIntent(NavigationIntent.NavigateTo(route = "detail"))

        // Still on home_tab - no switch.
        assertEquals("home_tab", state.rootNavController.currentDestination?.route)
        assertEquals("detail", state.requireNested("home_tab").currentDestination?.route)
        assertEquals("settings", state.requireNested("settings_tab").currentDestination?.route)
    }

    @Test
    fun `intent for target tab is queued and dispatched when that tab attaches`() {
        // Mirror the deep-link queueing test for regular NavigationIntents:
        // route owned by a tab whose composable isn't in composition yet.
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"))),
            FakeTab("settings_tab", "settings", children = setOf(FakeEntry("settings"))),
        )
        val rootController = controller(tabs.map { it.route })
        val state = MultitabState(
            tabs = tabs,
            rootNavController = rootController,
            selectedTab = mutableStateOf("home_tab"),
        )
        state.attachNestedController("home_tab", nestedControllerFor(setOf(FakeEntry("home"))))
        // settings_tab intentionally NOT attached yet.

        state.handleNavigatorIntent(NavigationIntent.NavigateTo(route = "settings"))

        // Root switched, intent queued.
        assertEquals("settings_tab", rootController.currentDestination?.route)

        // Now settings_tab's composable enters composition and attaches:
        val settingsNested = nestedControllerFor(setOf(FakeEntry("settings")))
        state.attachNestedController("settings_tab", settingsNested)

        // Queued intent was applied on attach.
        assertEquals("settings", settingsNested.currentDestination?.route)
    }

    // --- handleTabDeeplinkAction (bridge: TabDeeplinkNavigator → switch + forward) ---

    @Test
    fun `handleTabDeeplinkAction switches to the matching tab and navigates its nested controller`() {
        val homeEntry = FakeEntry(
            route = "home",
            deepLinks = listOf(navDeepLink { uriPattern = "tessera://app/home" }),
        )
        val settingsEntry = FakeEntry(
            route = "settings",
            deepLinks = listOf(navDeepLink { uriPattern = "tessera://app/settings" }),
        )
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(homeEntry)),
            FakeTab("settings_tab", "settings", children = setOf(settingsEntry)),
        )
        val state = stateWithChildren(tabs)  // currentTab = home_tab

        state.handleTabDeeplinkAction(
            TabNavigationAction.DeepLinkToTab(uri = Uri.parse("tessera://app/settings")),
        )

        assertEquals("settings_tab", state.rootNavController.currentDestination?.route)
        assertEquals(
            "settings",
            state.requireNested("settings_tab").currentDestination?.route,
        )
    }

    @Test
    fun `handleTabDeeplinkAction with an unknown URI is a no-op`() {
        val tabs = listOf(
            FakeTab(
                "home_tab",
                "home",
                children = setOf(
                    FakeEntry("home", deepLinks = listOf(navDeepLink { uriPattern = "tessera://app/home" })),
                ),
            ),
            FakeTab(
                "settings_tab",
                "settings",
                children = setOf(FakeEntry("settings")),
            ),
        )
        val state = stateWithChildren(tabs)

        state.handleTabDeeplinkAction(
            TabNavigationAction.DeepLinkToTab(uri = Uri.parse("tessera://nowhere")),
        )

        // No switch, no navigate.
        assertEquals("home_tab", state.rootNavController.currentDestination?.route)
        assertEquals("home", state.requireNested("home_tab").currentDestination?.route)
        assertEquals("settings", state.requireNested("settings_tab").currentDestination?.route)
    }

    @Test
    fun `handleTabDeeplinkAction picks the first tab when multiple tabs declare matching deep links`() {
        // Both tabs claim the same URI pattern. The iteration order of `tabs`
        // (already sorted by [TabFeatureEntry.order]) decides who wins.
        val pattern = "tessera://app/shared"
        val tabs = listOf(
            FakeTab(
                "first_tab",
                "first",
                children = setOf(
                    FakeEntry("first", deepLinks = listOf(navDeepLink { uriPattern = pattern })),
                ),
            ),
            FakeTab(
                "second_tab",
                "second",
                children = setOf(
                    FakeEntry("second", deepLinks = listOf(navDeepLink { uriPattern = pattern })),
                ),
            ),
        )
        val state = stateWithChildren(tabs)

        state.handleTabDeeplinkAction(
            TabNavigationAction.DeepLinkToTab(uri = Uri.parse(pattern)),
        )

        assertEquals("first_tab", state.rootNavController.currentDestination?.route)
    }

    // --- NavigateToDeepLink via Navigator (parallel to TabDeeplinkNavigator path) ---

    @Test
    fun `NavigateToDeepLink via Navigator with cross-tab URI switches and applies`() {
        // Same outcome as handleTabDeeplinkAction, but the deep link arrives
        // via Navigator.navigateRootDeepLink (NavigationIntent.NavigateToDeepLink)
        // instead of TabDeeplinkNavigator.deepLinkToTab.
        val settingsEntry = FakeEntry(
            route = "settings",
            deepLinks = listOf(navDeepLink { uriPattern = "tessera://app/settings" }),
        )
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"))),
            FakeTab("settings_tab", "settings", children = setOf(settingsEntry)),
        )
        val state = stateWithChildren(tabs)  // currentTab = home_tab

        val request = androidx.navigation.NavDeepLinkRequest.Builder
            .fromUri(Uri.parse("tessera://app/settings"))
            .build()
        state.handleNavigatorIntent(NavigationIntent.NavigateToDeepLink(request))

        assertEquals("settings_tab", state.rootNavController.currentDestination?.route)
        assertEquals("settings", state.requireNested("settings_tab").currentDestination?.route)
    }

    @Test
    fun `NavigateToDeepLink via Navigator with active-tab URI applies without switching`() {
        val homeEntry = FakeEntry(
            route = "home",
            deepLinks = listOf(navDeepLink { uriPattern = "tessera://app/home" }),
        )
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(homeEntry)),
            FakeTab("settings_tab", "settings", children = setOf(FakeEntry("settings"))),
        )
        val state = stateWithChildren(tabs)

        val request = androidx.navigation.NavDeepLinkRequest.Builder
            .fromUri(Uri.parse("tessera://app/home"))
            .build()
        state.handleNavigatorIntent(NavigationIntent.NavigateToDeepLink(request))

        // No switch (already on home_tab); home was already current - single-top
        // means no new entry, but at least no crash and no cross-tab behaviour.
        assertEquals("home_tab", state.rootNavController.currentDestination?.route)
        assertEquals("home", state.requireNested("home_tab").currentDestination?.route)
        assertEquals("settings", state.requireNested("settings_tab").currentDestination?.route)
    }

    @Test
    fun `NavigateToDeepLink via Navigator with no matching URI is a silent no-op`() {
        val tabs = listOf(
            FakeTab(
                "home_tab",
                "home",
                children = setOf(
                    FakeEntry(
                        "home",
                        deepLinks = listOf(navDeepLink { uriPattern = "tessera://app/home" }),
                    ),
                ),
            ),
        )
        val state = stateWithChildren(tabs)

        val request = androidx.navigation.NavDeepLinkRequest.Builder
            .fromUri(Uri.parse("tessera://unknown/x"))
            .build()
        state.handleNavigatorIntent(NavigationIntent.NavigateToDeepLink(request))

        assertEquals("home_tab", state.rootNavController.currentDestination?.route)
        assertEquals("home", state.requireNested("home_tab").currentDestination?.route)
    }

    // --- PopBackStackTo with unknown route (smart-routing symmetry with NavigateTo) ---

    @Test
    fun `PopBackStackTo with route owned by no tab is a silent no-op`() {
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"), FakeEntry("detail"))),
        )
        val state = stateWithChildren(tabs)
        state.requireNested("home_tab").navigate("detail")

        state.handleNavigatorIntent(NavigationIntent.PopBackStackTo(route = "ghost"))

        // No switch, no pop - nested still at detail.
        assertEquals("home_tab", state.rootNavController.currentDestination?.route)
        assertEquals("detail", state.requireNested("home_tab").currentDestination?.route)
    }

    // --- Queue mechanics ---

    @Test
    fun `multiple queued intents for one tab are dispatched in FIFO order when it attaches`() {
        // pendingTabIntents holds a List per tab; this test pins the FIFO
        // contract so a future refactor to e.g. a Set wouldn't silently
        // shuffle intent ordering on multi-step deferred dispatch.
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"))),
            FakeTab(
                "settings_tab",
                "settings",
                children = setOf(FakeEntry("settings"), FakeEntry("about")),
            ),
        )
        val rootController = controller(tabs.map { it.route })
        val state = MultitabState(
            tabs = tabs,
            rootNavController = rootController,
            selectedTab = mutableStateOf("home_tab"),
        )
        state.attachNestedController("home_tab", nestedControllerFor(setOf(FakeEntry("home"))))
        // settings_tab not yet attached.

        // Queue two intents in order: navigate to settings, then to about.
        state.handleNavigatorIntent(NavigationIntent.NavigateTo(route = "settings"))
        state.handleNavigatorIntent(NavigationIntent.NavigateTo(route = "about"))

        val settingsNested = nestedControllerFor(
            setOf(FakeEntry("settings"), FakeEntry("about")),
        )
        state.attachNestedController("settings_tab", settingsNested)

        // Last applied wins as current destination; FIFO means "settings" was
        // applied before "about", so popping returns to "settings".
        assertEquals("about", settingsNested.currentDestination?.route)
        settingsNested.popBackStack()
        assertEquals("settings", settingsNested.currentDestination?.route)
    }

    // --- Detach + reattach lifecycle ---

    @Test
    fun `detach then attach with a new controller routes intents to the new instance`() {
        // Mirrors what happens when a tab leaves composition and re-enters:
        // rememberNavController produces a fresh NavController instance backed
        // by the previous one's saved state. The state holder must route to
        // the new instance - never the old (which has DESTROYED entries).
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"), FakeEntry("detail"))),
        )
        val state = stateWithChildren(tabs)
        val originalController = state.requireNested("home_tab")

        state.detachNestedController("home_tab")
        val newController = nestedControllerFor(setOf(FakeEntry("home"), FakeEntry("detail")))
        state.attachNestedController("home_tab", newController)

        state.handleNavigatorIntent(NavigationIntent.NavigateTo(route = "detail"))

        assertEquals(
            "intent must apply to the freshly-attached controller, not the detached one",
            "detail",
            newController.currentDestination?.route,
        )
        assertEquals(
            "old controller must not have received the intent",
            "home",
            originalController.currentDestination?.route,
        )
    }

    // --- Smart routing with NestedFeatureEntry (regression: checkout/confirm) ---

    @Test
    fun `NavigateTo to a route inside a NestedFeatureEntry in active tab applies locally`() {
        // The reported sample bug: on the Checkout tab, AddressScreen fires
        // navigator.navigate("checkout/confirm"). The route lives inside a
        // NestedFeatureEntry inside checkout_tab.children. Previously smart
        // routing's pre-check used NavGraph.findNode (non-recursive) and
        // missed nested destinations; smart routing then searched children
        // via direct equality (also non-recursive) and found nothing → no-op.
        val addressEntry = FakeFeatureEntry("checkout/address")
        val confirmEntry = FakeFeatureEntry("checkout/confirm")
        val checkoutNested = object : NestedFeatureEntry() {
            override val route: String = "checkout"
            override val startRoute: String = "checkout/address"
            override val children: List<FeatureEntry> = listOf(addressEntry, confirmEntry)
        }
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"))),
            FakeTab("checkout_tab", "checkout", children = setOf(checkoutNested)),
        )

        // Build a checkout-tab controller whose graph includes the nested
        // graph the way Tessera's NestedFeatureEntry.registerGraph would.
        val rootController = controller(tabs.map { it.route })
        rootController.navigate("checkout_tab")
        val checkoutController = TestNavHostController(RuntimeEnvironment.getApplication())
        checkoutController.navigatorProvider.addNavigator(ComposeNavigator())
        val graph = checkoutController.createGraph(startDestination = "checkout") {
            // Nested NavGraph node mirroring registerGraph of NestedFeatureEntry.
            navigation(
                route = "checkout",
                startDestination = "checkout/address",
            ) {
                composable("checkout/address") {}
                composable("checkout/confirm") {}
            }
        }
        checkoutController.setGraph(graph, null)

        val state = MultitabState(
            tabs = tabs,
            rootNavController = rootController,
            selectedTab = mutableStateOf("checkout_tab"),
        )
        state.attachNestedController("home_tab", controller(listOf("home")))
        state.attachNestedController("checkout_tab", checkoutController)

        state.handleNavigatorIntent(NavigationIntent.NavigateTo(route = "checkout/confirm"))

        // Active tab handled it → no switch + checkout_tab navigated to confirm.
        assertEquals("checkout_tab", rootController.currentDestination?.route)
        assertEquals("checkout/confirm", checkoutController.currentDestination?.route)
    }

    @Test
    fun `containsRoute recurses through nested feature entries when finding owner tab`() {
        // Cross-tab variant: route is inside a NestedFeatureEntry in another
        // tab. Smart routing must walk children recursively, not just the
        // first level, to attribute the route to the correct tab.
        val confirmEntry = FakeFeatureEntry("checkout/confirm")
        val checkoutNested = object : NestedFeatureEntry() {
            override val route: String = "checkout"
            override val startRoute: String = "checkout/address"
            override val children: List<FeatureEntry> = listOf(
                FakeFeatureEntry("checkout/address"),
                confirmEntry,
            )
        }
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"))),
            FakeTab("checkout_tab", "checkout", children = setOf(checkoutNested)),
        )
        val rootController = controller(tabs.map { it.route })
        val state = MultitabState(
            tabs = tabs,
            rootNavController = rootController,
            selectedTab = mutableStateOf("home_tab"),
        )
        // home_tab attached, checkout_tab NOT yet attached.
        state.attachNestedController("home_tab", controller(listOf("home")))

        // Emit from home tab while standing on home_tab.
        state.handleNavigatorIntent(NavigationIntent.NavigateTo(route = "checkout/confirm"))

        // Smart routing must have:
        //  1. failed tryApply on home_tab (route not there),
        //  2. walked checkout_tab.children → checkoutNested.children → found "checkout/confirm",
        //  3. switched root to checkout_tab,
        //  4. queued the intent (checkout_tab controller not yet attached).
        assertEquals("checkout_tab", rootController.currentDestination?.route)

        // Now checkout_tab's composable enters composition; attach its
        // controller - queued intent must dispatch.
        val checkoutController = TestNavHostController(RuntimeEnvironment.getApplication())
        checkoutController.navigatorProvider.addNavigator(ComposeNavigator())
        val graph = checkoutController.createGraph(startDestination = "checkout") {
            navigation(
                route = "checkout",
                startDestination = "checkout/address",
            ) {
                composable("checkout/address") {}
                composable("checkout/confirm") {}
            }
        }
        checkoutController.setGraph(graph, null)
        state.attachNestedController("checkout_tab", checkoutController)

        assertEquals("checkout/confirm", checkoutController.currentDestination?.route)
    }

    private class FakeFeatureEntry(override val route: String) : FeatureEntry

    private class FakeFeatureEntryWithDeepLink(
        override val route: String,
        override val deepLinks: List<NavDeepLink>,
    ) : FeatureEntry

    // --- Deep nested target via TabDeeplinkNavigator path (cold/warm deep links) ---

    @Test
    fun `handleTabDeeplinkAction navigates into a deeply nested destination in target tab`() {
        // Real scenario: a push notification delivers the URI
        // "myapp://settings/two-factor". The target destination lives 2 levels
        // deep (settings → privacy → two-factor) inside settings_tab.
        // Tessera must: find the URI's owner, switch the root to that tab,
        // forward the deep link to its nested controller, where Jetpack
        // Navigation's recursive matchDeepLink reaches the deep destination.
        val twoFactor = FakeFeatureEntryWithDeepLink(
            route = "settings/privacy/two-factor",
            deepLinks = listOf(navDeepLink { uriPattern = "myapp://settings/two-factor" }),
        )
        val privacyMain = FakeFeatureEntry("settings/privacy/main")
        val privacyNested = object : NestedFeatureEntry() {
            override val route = "settings/privacy"
            override val startRoute = "settings/privacy/main"
            override val children = listOf(privacyMain, twoFactor)
        }
        val settingsNested = object : NestedFeatureEntry() {
            override val route = "settings"
            override val startRoute = "settings/main"
            override val children = listOf(FakeFeatureEntry("settings/main"), privacyNested)
        }
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"))),
            FakeTab("settings_tab", "settings", children = setOf(settingsNested)),
        )

        val rootController = controller(tabs.map { it.route })
        val homeController = TestNavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setGraph(createGraph(startDestination = "home") { composable("home") {} }, null)
        }
        // The settings_tab graph includes a deep link on the deep destination -
        // mirrors what NestedFeatureEntry.registerGraph would assemble.
        val settingsController = TestNavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setGraph(
                createGraph(startDestination = "settings") {
                    navigation(route = "settings", startDestination = "settings/main") {
                        composable("settings/main") {}
                        navigation(
                            route = "settings/privacy",
                            startDestination = "settings/privacy/main",
                        ) {
                            composable("settings/privacy/main") {}
                            composable(
                                route = "settings/privacy/two-factor",
                                deepLinks = listOf(
                                    navDeepLink { uriPattern = "myapp://settings/two-factor" },
                                ),
                            ) {}
                        }
                    }
                },
                null,
            )
        }

        val state = MultitabState(
            tabs = tabs,
            rootNavController = rootController,
            selectedTab = mutableStateOf("home_tab"),
        )
        state.attachNestedController("home_tab", homeController)
        state.attachNestedController("settings_tab", settingsController)

        state.handleTabDeeplinkAction(
            TabNavigationAction.DeepLinkToTab(uri = Uri.parse("myapp://settings/two-factor")),
        )

        assertEquals("settings_tab", rootController.currentDestination?.route)
        assertEquals(
            "settings/privacy/two-factor",
            settingsController.currentDestination?.route,
        )
    }

    @Test
    fun `cold-start deep link queues until target tab attaches and lands on deep destination`() {
        // Multi-NavHost cold start: MultitabActivity catches an Intent and
        // immediately calls tabDeeplinkNavigator.deepLinkToTab(uri) BEFORE
        // the target tab's composable has mounted and attached its controller.
        // handleTabDeeplinkAction must queue the deep link and dispatch it
        // on attachNestedController.
        val deepEntry = FakeFeatureEntryWithDeepLink(
            route = "settings/privacy/two-factor",
            deepLinks = listOf(navDeepLink { uriPattern = "myapp://settings/two-factor" }),
        )
        val privacyNested = object : NestedFeatureEntry() {
            override val route = "settings/privacy"
            override val startRoute = "settings/privacy/main"
            override val children = listOf(
                FakeFeatureEntry("settings/privacy/main"),
                deepEntry,
            )
        }
        val settingsNested = object : NestedFeatureEntry() {
            override val route = "settings"
            override val startRoute = "settings/main"
            override val children = listOf(FakeFeatureEntry("settings/main"), privacyNested)
        }
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"))),
            FakeTab("settings_tab", "settings", children = setOf(settingsNested)),
        )

        val rootController = controller(tabs.map { it.route })
        val state = MultitabState(
            tabs = tabs,
            rootNavController = rootController,
            selectedTab = mutableStateOf("home_tab"),
        )
        // IMPORTANT: target tab is NOT yet attached - simulating cold start,
        // where tabDeeplinkNavigator is called before the UI mounts.
        state.attachNestedController(
            "home_tab",
            controller(listOf("home")),
        )

        state.handleTabDeeplinkAction(
            TabNavigationAction.DeepLinkToTab(uri = Uri.parse("myapp://settings/two-factor")),
        )

        // Root switched even without an attached target controller.
        assertEquals("settings_tab", rootController.currentDestination?.route)

        // Now the UI mounts, settings_tab composable enters composition,
        // attaches its controller. pendingDeepLinks must immediately dispatch
        // the URI to this fresh controller.
        val settingsController = TestNavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setGraph(
                createGraph(startDestination = "settings") {
                    navigation(route = "settings", startDestination = "settings/main") {
                        composable("settings/main") {}
                        navigation(
                            route = "settings/privacy",
                            startDestination = "settings/privacy/main",
                        ) {
                            composable("settings/privacy/main") {}
                            composable(
                                route = "settings/privacy/two-factor",
                                deepLinks = listOf(
                                    navDeepLink { uriPattern = "myapp://settings/two-factor" },
                                ),
                            ) {}
                        }
                    }
                },
                null,
            )
        }
        state.attachNestedController("settings_tab", settingsController)

        assertEquals(
            "settings/privacy/two-factor",
            settingsController.currentDestination?.route,
        )
    }

    // --- FeatureEntry reuse across tabs (smart routing prefers active tab) ---

    @Test
    fun `same FeatureEntry shared between two tabs - smart routing prefers the active tab`() {
        // A FeatureEntry with route "shared/profile" appears in the children
        // of both tabs. When the user is on settings_tab and emits
        // navigate("shared/profile"), smart routing must prefer the ACTIVE
        // tab rather than falling back to the first one by order. Critical
        // for reusing shared screens across tabs without unexpected switches.
        val sharedEntry = FakeEntry("shared/profile")
        val tabs = listOf(
            FakeTab("home_tab", "home", children = setOf(FakeEntry("home"), sharedEntry)),
            FakeTab(
                "settings_tab",
                "settings",
                children = setOf(FakeEntry("settings"), sharedEntry),
            ),
        )

        val rootController = controller(tabs.map { it.route })
        rootController.navigate("settings_tab") // active tab = settings

        val homeController = TestNavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setGraph(
                createGraph(startDestination = "home") {
                    composable("home") {}
                    composable("shared/profile") {}
                },
                null,
            )
        }
        val settingsController = TestNavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setGraph(
                createGraph(startDestination = "settings") {
                    composable("settings") {}
                    composable("shared/profile") {}
                },
                null,
            )
        }

        val state = MultitabState(
            tabs = tabs,
            rootNavController = rootController,
            selectedTab = mutableStateOf("settings_tab"),
        )
        state.attachNestedController("home_tab", homeController)
        state.attachNestedController("settings_tab", settingsController)

        state.handleNavigatorIntent(NavigationIntent.NavigateTo(route = "shared/profile"))

        // Active tab (settings_tab) handled it locally - no root switch.
        assertEquals("settings_tab", rootController.currentDestination?.route)
        assertEquals("shared/profile", settingsController.currentDestination?.route)
        // home_tab's instance of "shared/profile" - untouched.
        assertEquals("home", homeController.currentDestination?.route)
    }

    // --- Deep source stack + deep nested target (real-world cross-tab scenario) ---

    @Test
    fun `cross-tab navigation from deep home stack to deep settings nested destination`() {
        // Real-world scenario from the guide:
        // User is on Home → Detail (deep inside home_tab).
        // Taps something that triggers navigator.navigate("settings/privacy/two-factor").
        // "two-factor" lives inside a NESTED NestedFeatureEntry (settings → privacy)
        // in settings_tab.
        //
        // Expected: root switches to settings_tab + the settings_tab nested
        // controller lands on the deep destination, with the back stack
        // auto-built through all intermediate NavGraph starts.

        // --- target tab structure: settings_tab with 2 levels of nesting ---
        val settingsMain = FakeFeatureEntry("settings/main")
        val privacyMain = FakeFeatureEntry("settings/privacy/main")
        val twoFactor = FakeFeatureEntry("settings/privacy/two-factor")
        val privacyNested = object : NestedFeatureEntry() {
            override val route = "settings/privacy"
            override val startRoute = "settings/privacy/main"
            override val children = listOf(privacyMain, twoFactor)
        }
        val settingsNested = object : NestedFeatureEntry() {
            override val route = "settings"
            override val startRoute = "settings/main"
            override val children = listOf(settingsMain, privacyNested)
        }

        val tabs = listOf(
            FakeTab(
                "home_tab",
                "home",
                children = setOf(FakeEntry("home"), FakeEntry("detail")),
            ),
            FakeTab("settings_tab", "settings", children = setOf(settingsNested)),
        )

        // --- root and home_tab controllers ---
        val rootController = controller(tabs.map { it.route })
        val homeController = TestNavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setGraph(
                createGraph(startDestination = "home") {
                    composable("home") {}
                    composable("detail") {}
                },
                null,
            )
        }
        // Deepen home_tab - user is on Detail.
        homeController.navigate("detail")

        // --- settings_tab controller with full 2-level nesting ---
        // We do NOT invoke NestedFeatureEntry.registerGraph (it has ComposeContent);
        // instead the same structure is reproduced manually via navigation { }
        // blocks - equivalent to what registerGraph builds under the hood.
        val settingsController = TestNavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setGraph(
                createGraph(startDestination = "settings") {
                    navigation(route = "settings", startDestination = "settings/main") {
                        composable("settings/main") {}
                        navigation(
                            route = "settings/privacy",
                            startDestination = "settings/privacy/main",
                        ) {
                            composable("settings/privacy/main") {}
                            composable("settings/privacy/two-factor") {}
                        }
                    }
                },
                null,
            )
        }

        val state = MultitabState(
            tabs = tabs,
            rootNavController = rootController,
            selectedTab = mutableStateOf("home_tab"),
        )
        state.attachNestedController("home_tab", homeController)
        state.attachNestedController("settings_tab", settingsController)

        // --- trigger: from home_tab deep into settings_tab ---
        state.handleNavigatorIntent(
            NavigationIntent.NavigateTo(route = "settings/privacy/two-factor"),
        )

        // 1. Root switched to settings_tab.
        assertEquals("settings_tab", rootController.currentDestination?.route)

        // 2. settings_tab nested controller landed on the deep destination.
        //    Jetpack Navigation resolved the route via recursive matchDeepLink.
        assertEquals(
            "settings/privacy/two-factor",
            settingsController.currentDestination?.route,
        )

        // 3. home_tab back stack preserved - controller stayed attached in the
        //    test, its currentDestination = "detail". In production this works
        //    thanks to saveState/restoreState on root + rememberSaveable inside
        //    rememberNavController.
        assertEquals("detail", homeController.currentDestination?.route)
    }

    // --- helpers ---

    private inner class FakeTab(
        override val route: String,
        override val startDestination: String,
        override val children: Set<FeatureEntry> = emptySet(),
    ) : TabFeatureEntry {
        override val title: String = route
        override val icon: ImageVector = fakeIcon
        override val order: Int = 0
    }

    private class FakeEntry(
        override val route: String,
        override val deepLinks: List<NavDeepLink> = emptyList(),
    ) : FeatureEntry
}
