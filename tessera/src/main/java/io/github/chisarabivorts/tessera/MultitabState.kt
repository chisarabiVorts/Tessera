package io.github.chisarabivorts.tessera

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavDeepLink
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * State holder coordinating a multi-NavHost layout for Tessera.
 *
 * Tessera ships this as a state holder (not a `Host` composable) so callers
 * keep full control over UI - bottom bar, navigation rail, drawer, or any
 * custom layout. The state holder owns:
 *
 *  - One root [NavHostController] that switches between tabs.
 *  - A live map of currently-attached nested [NavHostController]s, one per
 *    visible tab destination. Bridges intents from [Navigator] and
 *    [TabDeeplinkNavigator] flows to the active or target nested controller.
 *
 * **Important - nested controller lifecycle.** Each tab's `NavHostController`
 * must be created **inside** that tab's `composable(...)` block via
 * [rememberNavController], not at the top level. The state holder does not
 * keep a permanent reference: the per-tab composable registers its controller
 * via [attachNestedController] when it enters composition and unregisters via
 * [detachNestedController] in `onDispose`. This ties the controller's lifecycle
 * to its owning [androidx.navigation.NavBackStackEntry] - reusing one controller
 * across destroy/restore cycles corrupts internal `NavBackStackEntry` state and
 * crashes with `IllegalStateException: You cannot access the NavBackStackEntry's
 * ViewModels after the NavBackStackEntry is destroyed.`
 *
 * Typical usage in `MainActivity`:
 *
 * ```
 * val state = rememberMultitabState(
 *     tabs = tabs,
 *     navigator = navigator,
 *     resultNavigator = resultNavigator,
 *     tabDeeplinkNavigator = tabDeeplinkNavigator,
 * )
 * val selectedTab by state.selectedTab
 *
 * Scaffold(
 *     bottomBar = {
 *         NavigationBar {
 *             state.tabs.forEach { tab ->
 *                 NavigationBarItem(
 *                     selected = tab.route == selectedTab,
 *                     onClick = { state.selectTab(tab.route) },
 *                     icon = { Icon(tab.icon, contentDescription = tab.title) },
 *                     label = { Text(tab.title) },
 *                 )
 *             }
 *         }
 *     },
 * ) { padding ->
 *     NavHost(
 *         navController = state.rootNavController,
 *         startDestination = state.tabs.first().route,
 *         modifier = Modifier.padding(padding),
 *     ) {
 *         state.tabs.forEach { tab ->
 *             composable(tab.route) {
 *                 // IMPORTANT: rememberNavController() inside the composable
 *                 // so its lifecycle is tied to this back stack entry.
 *                 val nestedController = rememberNavController()
 *                 DisposableEffect(nestedController) {
 *                     state.attachNestedController(tab.route, nestedController)
 *                     onDispose { state.detachNestedController(tab.route) }
 *                 }
 *                 NavHost(
 *                     navController = nestedController,
 *                     startDestination = tab.startDestination,
 *                 ) {
 *                     tab.children.forEach { child ->
 *                         child.registerGraph(this, navigator, resultNavigator)
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 */
public class MultitabState internal constructor(
    /** Tabs in display order (sorted by [TabFeatureEntry.order]). */
    public val tabs: List<TabFeatureEntry>,
    /** Root controller - switches between tabs. */
    public val rootNavController: NavHostController,
    /**
     * The currently active tab's route. Derived from [rootNavController]'s
     * current destination; `null` while the root graph is still initialising.
     */
    public val selectedTab: State<String?>,
    /**
     * Behaviour for [selectTab] when called with the route of the **already
     * active** tab.
     *
     *  - `false` (default) - pure no-op. Matches the simplest Material 3 spec.
     *  - `true` - "tap-to-top": pop the active tab's nested back stack down
     *    to its [TabFeatureEntry.startDestination], so re-tapping the active
     *    tab returns the user to the top of that tab (Instagram / Twitter UX).
     *
     * Configured via the `resetActiveTabOnReselect` parameter of
     * [rememberMultitabState].
     */
    private val resetActiveTabOnReselect: Boolean = false,
) {

    private val tabNavControllers: MutableMap<String, NavHostController> = mutableMapOf()
    private val pendingDeepLinks: MutableMap<String, NavDeepLinkRequest> = mutableMapOf()
    private val pendingTabIntents: MutableMap<String, MutableList<NavigationIntent>> = mutableMapOf()

    /**
     * Register [controller] as the nested controller for [tabRoute]. Call from
     * a `DisposableEffect` inside that tab's `composable(...)` block; pair with
     * [detachNestedController] in `onDispose`.
     *
     * If a deep link was queued for this tab while its composable was offscreen
     * (see [handleTabDeeplinkAction]), it is dispatched here.
     *
     * @throws IllegalArgumentException if [tabRoute] is not one of [tabs].
     */
    public fun attachNestedController(tabRoute: String, controller: NavHostController) {
        require(tabs.any { it.route == tabRoute }) {
            "attachNestedController: '$tabRoute' is not one of the registered tabs " +
                "(${tabs.map { it.route }})"
        }
        tabNavControllers[tabRoute] = controller
        pendingDeepLinks.remove(tabRoute)?.let { request ->
            controller.navigate(request)
        }
        pendingTabIntents.remove(tabRoute)?.forEach { intent ->
            controller.applyNavigationIntent(intent)
        }
    }

    /**
     * Unregister the nested controller for [tabRoute]. Call from `onDispose`
     * of the same `DisposableEffect` that called [attachNestedController].
     */
    public fun detachNestedController(tabRoute: String) {
        tabNavControllers.remove(tabRoute)
    }

    /**
     * Returns the currently-attached nested controller for [tabRoute], or
     * `null` if no composable for that tab is currently in composition. In
     * the typical multi-NavHost layout this is non-null exactly for the
     * active tab.
     */
    public fun nestedController(tabRoute: String): NavHostController? =
        tabNavControllers[tabRoute]

    /**
     * Switch the visible tab.
     *
     * Uses the canonical Material 3 tab navigation pattern: pop to the root
     * start destination, save the previous tab's state, restore the target
     * tab's state.
     *
     * **Re-selecting the active tab** is governed by the
     * `resetActiveTabOnReselect` flag passed to [rememberMultitabState]:
     *  - `false` (default) - no-op.
     *  - `true` - pop the active tab's nested back stack down to its
     *    [TabFeatureEntry.startDestination] (tap-to-top).
     *
     * @throws IllegalArgumentException if [tabRoute] is not one of [tabs].
     */
    public fun selectTab(tabRoute: String) {
        require(tabs.any { it.route == tabRoute }) {
            "Unknown tab '$tabRoute'. Known tabs: ${tabs.map { it.route }}"
        }
        if (selectedTab.value == tabRoute) {
            if (resetActiveTabOnReselect) resetActiveTabToStart(tabRoute)
            return
        }
        rootNavController.navigate(tabRoute) {
            popUpTo(rootNavController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * Pop the nested controller of [tabRoute] down to its
     * [TabFeatureEntry.startDestination]. No-op if the controller is not
     * attached or the start destination is already on top of the stack.
     */
    private fun resetActiveTabToStart(tabRoute: String) {
        val controller = tabNavControllers[tabRoute] ?: return
        val tab = tabs.first { it.route == tabRoute }
        try {
            controller.popBackStack(tab.startDestination, inclusive = false)
        } catch (_: IllegalArgumentException) {
            // Start destination not on stack (e.g. controller just attached
            // and is already at start) - no-op.
        }
    }

    /**
     * Resolve which tab owns a destination matching [uri], by recursively
     * walking each tab's `FeatureEntry` tree (including [NestedFeatureEntry]
     * children) for a matching `NavDeepLink`. Returns `null` if no tab
     * declares a deep link for this URI.
     *
     * Recursion mirrors how Jetpack Navigation resolves deep links through
     * nested `NavGraph`s - a deep link declared on a step destination inside
     * a `NestedFeatureEntry` must attribute to its owning tab.
     */
    internal fun tabRouteForUri(uri: Uri): String? {
        val request = NavDeepLinkRequest.Builder.fromUri(uri).build()
        return tabs.firstOrNull { tab ->
            tab.children.any { child -> child.matchesDeepLink(request) }
        }?.route
    }

    /**
     * True if any `NavDeepLink` declared on this entry - or, recursively, on
     * any descendant entry inside a [NestedFeatureEntry] - matches [request].
     */
    private fun FeatureEntry.matchesDeepLink(request: NavDeepLinkRequest): Boolean {
        if (anyLinkMatches(deepLinks, request)) return true
        if (this is NestedFeatureEntry) {
            return children.any { it.matchesDeepLink(request) }
        }
        return false
    }

    /**
     * Apply a [NavigationIntent] from [Navigator] to the right controller.
     *
     * Smart routing - features don't need to be aware of which tab they live in:
     *
     *  - [NavigationIntent.SwitchTab] → root controller via [selectTab].
     *  - [NavigationIntent.PopBackStack] → active tab only (no route info to
     *    cross-route on).
     *  - [NavigationIntent.NavigateTo] / [NavigationIntent.PopBackStackTo] →
     *    try the active tab first; if it doesn't own the route, look at every
     *    [TabFeatureEntry.children] entry to find an owning tab; if found,
     *    [selectTab] to that tab and forward the intent (queue if that tab's
     *    nested controller is not yet attached).
     *  - [NavigationIntent.NavigateToDeepLink] → resolve owner via
     *    [tabRouteForUri], then forward (queue on not-attached).
     *
     * Routes are matched by **literal string** against `child.route`. Templated
     * routes (`"detail/{id}"`) are recognised only by their literal pattern -
     * emit them from within the owning tab so the active controller can handle
     * argument substitution.
     *
     * If no tab claims the requested route, the intent is a silent no-op. The
     * same applies if all routing targets are unattached and queueing won't
     * help.
     */
    internal fun handleNavigatorIntent(intent: NavigationIntent) {
        when (intent) {
            is NavigationIntent.SwitchTab -> selectTab(intent.route)
            NavigationIntent.PopBackStack -> {
                // No route - only the active tab is a sensible target.
                val currentRoute = selectedTab.value ?: return
                tabNavControllers[currentRoute]?.applyNavigationIntent(intent)
            }
            is NavigationIntent.NavigateTo -> routeByRoute(intent, intent.route)
            is NavigationIntent.PopBackStackTo -> routeByRoute(intent, intent.route)
            is NavigationIntent.NavigateToDeepLink -> {
                val uri = intent.request.uri
                if (uri == null) {
                    // No URI to match - try the active tab and rely on the
                    // controller's own IAE-catching no-op.
                    val currentRoute = selectedTab.value ?: return
                    tabNavControllers[currentRoute]?.applyNavigationIntent(intent)
                    return
                }
                val ownerTabRoute = tabRouteForUri(uri) ?: return
                forwardToTab(intent, ownerTabRoute)
            }
        }
    }

    /**
     * Route an intent that carries an explicit destination [route]. Tries the
     * active tab's controller first (leveraging Jetpack Navigation's own
     * recursive destination matching across nested graphs); falls back to a
     * recursive search through every [TabFeatureEntry.children] tree.
     */
    private fun routeByRoute(intent: NavigationIntent, route: String) {
        val currentRoute = selectedTab.value
        val currentController = currentRoute?.let { tabNavControllers[it] }

        // Active tab handles the intent - including nested NavGraph destinations,
        // because tryApplyNavigationIntent uses NavController.navigate which does
        // recursive matchDeepLink across the tab's entire graph hierarchy.
        if (currentController != null && currentController.tryApplyNavigationIntent(intent)) {
            return
        }

        // Find the tab whose feature-entry tree claims this route. Walks into
        // NestedFeatureEntry.children so a step destination like
        // "checkout/confirm" inside a CheckoutFeatureEntry: NestedFeatureEntry
        // is correctly attributed to its owning tab.
        val ownerTabRoute = tabs.firstOrNull { tab ->
            tab.children.any { it.containsRoute(route) }
        }?.route ?: return // no tab - silent no-op

        if (ownerTabRoute == currentRoute) {
            // Active tab's children tree contains the route but tryApply didn't
            // match (controller not yet attached, or composition not finalised).
            // Apply directly if attached, else queue.
            val controller = tabNavControllers[ownerTabRoute]
            if (controller != null) {
                controller.applyNavigationIntent(intent)
            } else {
                pendingTabIntents.getOrPut(ownerTabRoute) { mutableListOf() }.add(intent)
            }
            return
        }

        forwardToTab(intent, ownerTabRoute)
    }

    /**
     * Walks this entry's route tree to check whether any destination matches
     * [route]. Direct match on [FeatureEntry.route]; for [NestedFeatureEntry]
     * also recurses into [NestedFeatureEntry.children]. Mirrors how Jetpack
     * Navigation resolves routes through nested `NavGraph`s.
     */
    private fun FeatureEntry.containsRoute(route: String): Boolean {
        if (this.route == route) return true
        if (this is NestedFeatureEntry) {
            return children.any { it.containsRoute(route) }
        }
        return false
    }

    /**
     * Switch the root controller to [targetTabRoute] and forward [intent] into
     * its nested controller. If that controller is not yet attached (the tab's
     * composable hasn't entered composition since the root navigated), the
     * intent is queued and dispatched by [attachNestedController].
     */
    private fun forwardToTab(intent: NavigationIntent, targetTabRoute: String) {
        selectTab(targetTabRoute)
        val target = tabNavControllers[targetTabRoute]
        if (target != null) {
            target.applyNavigationIntent(intent)
        } else {
            pendingTabIntents.getOrPut(targetTabRoute) { mutableListOf() }.add(intent)
        }
    }

    /**
     * Apply a [TabNavigationAction] from [TabDeeplinkNavigator]: resolve the
     * target tab from the URI, switch the root controller to it, then forward
     * the deep link into that tab's nested controller. No-op if no tab declares
     * a matching deep link.
     *
     * If the target tab's composable is not yet in composition (e.g. the root
     * controller is mid-transition into that tab), the deep link is queued and
     * dispatched when [attachNestedController] registers the controller.
     */
    internal fun handleTabDeeplinkAction(action: TabNavigationAction) {
        when (action) {
            is TabNavigationAction.DeepLinkToTab -> {
                val targetRoute = tabRouteForUri(action.uri) ?: return
                selectTab(targetRoute)
                val request = NavDeepLinkRequest.Builder.fromUri(action.uri).build()
                val controller = tabNavControllers[targetRoute]
                if (controller != null) {
                    controller.navigate(request)
                } else {
                    pendingDeepLinks[targetRoute] = request
                }
            }
        }
    }

    /**
     * Uses [NavDestination.hasDeepLink] (public) on a throwaway destination
     * to test whether any deep link from [deepLinks] matches [request].
     * `NavDeepLink.matches(...)` itself is internal in Jetpack Navigation.
     */
    private fun anyLinkMatches(deepLinks: List<NavDeepLink>, request: NavDeepLinkRequest): Boolean {
        if (deepLinks.isEmpty()) return false
        val probe = NavDestination("__tessera-tab-resolver-probe__")
        deepLinks.forEach { probe.addDeepLink(it) }
        return probe.hasDeepLink(request)
    }
}

/**
 * Creates and remembers a [MultitabState] for [tabs].
 *
 * Subscribes for the lifetime of the host composition:
 *  - intents from [Navigator] are applied to the currently active tab's
 *    nested controller via [applyNavigationIntent];
 *  - cross-tab deep links from [TabDeeplinkNavigator] switch the active tab
 *    (by resolving which tab owns a destination for the link's URI), then
 *    dispatch the link into the target tab's nested controller (or queue it
 *    until that tab's composable attaches its controller).
 *
 * Each per-tab nested `NavHostController` must be created **inside** that
 * tab's `composable(...)` block via [rememberNavController] and registered
 * via [MultitabState.attachNestedController] inside a `DisposableEffect`.
 * See [MultitabState] KDoc for the canonical recipe.
 *
 * The [resultNavigator] parameter is accepted for API symmetry with the
 * single-NavHost setup - callers typically pass it down to feature entries
 * via `child.registerGraph(navGraphBuilder, navigator, resultNavigator)`.
 *
 * @param resetActiveTabOnReselect controls what happens when the user taps
 *   the **already-active** tab. `false` (default) - no-op (the simplest
 *   Material 3 spec). `true` - pop that tab's nested back stack down to its
 *   [TabFeatureEntry.startDestination] (Instagram / Twitter style "tap-to-top").
 *
 * @throws IllegalArgumentException if [tabs] is empty or contains duplicate routes.
 */
@Composable
public fun rememberMultitabState(
    tabs: Set<TabFeatureEntry>,
    navigator: Navigator,
    @Suppress("UNUSED_PARAMETER") resultNavigator: ResultNavigator,
    tabDeeplinkNavigator: TabDeeplinkNavigator,
    resetActiveTabOnReselect: Boolean = false,
): MultitabState {
    require(tabs.isNotEmpty()) { "rememberMultitabState requires at least one tab" }
    requireDistinctTabRoutes(tabs)

    val sortedTabs = remember(tabs) { tabs.sortedBy { it.order }.toList() }
    val rootNavController = rememberNavController()

    val currentBackStackEntryState = rootNavController.currentBackStackEntryAsState()
    val selectedTab: State<String?> = remember(currentBackStackEntryState) {
        derivedStateOf { currentBackStackEntryState.value?.destination?.route }
    }

    val state = remember(sortedTabs, rootNavController, selectedTab, resetActiveTabOnReselect) {
        MultitabState(
            tabs = sortedTabs,
            rootNavController = rootNavController,
            selectedTab = selectedTab,
            resetActiveTabOnReselect = resetActiveTabOnReselect,
        )
    }

    LaunchedEffect(state, navigator) {
        navigator.navigationActions.collect { state.handleNavigatorIntent(it) }
    }
    LaunchedEffect(state, tabDeeplinkNavigator) {
        tabDeeplinkNavigator.tabNavigationFlow.collect { state.handleTabDeeplinkAction(it) }
    }

    return state
}

/**
 * Fail-fast check that no two [TabFeatureEntry] in [tabs] share the same
 * [TabFeatureEntry.route]. Duplicate routes would otherwise cause the bottom
 * bar to render both tabs while only one nested controller exists for the
 * shared route - clicks on either tab would land on the same destination
 * without any warning.
 *
 * Extracted as an internal top-level function so it can be unit-tested
 * without entering a Compose composition.
 */
internal fun requireDistinctTabRoutes(tabs: Collection<TabFeatureEntry>) {
    val duplicates = tabs.groupBy { it.route }.filterValues { it.size > 1 }.keys
    require(duplicates.isEmpty()) {
        "MultitabState requires distinct tab routes. Duplicates: $duplicates"
    }
}
