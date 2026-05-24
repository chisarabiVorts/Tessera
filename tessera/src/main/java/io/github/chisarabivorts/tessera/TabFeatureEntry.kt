package io.github.chisarabivorts.tessera

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A top-level tab in a multi-NavHost layout.
 *
 * Each tab owns its own nested navigation graph and back stack, so navigation
 * inside one tab is independent of navigation inside another. A root
 * `NavController` switches between tabs; each tab's nested `NavController`
 * handles intra-tab navigation.
 *
 * Use with [rememberMultitabState], which wires up the lifecycle of the root
 * and per-tab `NavController`s and routes intents from [Navigator] /
 * [TabDeeplinkNavigator] to the appropriate controller.
 *
 * Tessera does **not** render tab UI itself - implementations only declare
 * what a tab is and what features it contains. The `:app` module decides how
 * tabs look (bottom bar, nav rail, drawer, etc.).
 *
 * Example:
 * ```
 * class HomeTab @Inject constructor(
 *     private val home: HomeFeatureEntry,
 *     private val detail: DetailFeatureEntry,
 * ) : TabFeatureEntry {
 *     override val route = "home_tab"
 *     override val startDestination = "home"
 *     override val children = setOf(home, detail)
 *     override val title = "Home"
 *     override val icon = Icons.Default.Home
 *     override val order = 0
 * }
 * ```
 */
public interface TabFeatureEntry {

    /** Route owned by this tab in the root `NavController`. */
    public val route: String

    /** Route inside the nested graph that opens first when entering this tab. */
    public val startDestination: String

    /** Feature entries available inside this tab's nested `NavHost`. */
    public val children: Set<FeatureEntry>

    /** Human-readable title for the tab UI. */
    public val title: String

    /** Icon for the tab UI. */
    public val icon: ImageVector

    /** Display order; lower values appear first in the tab strip. */
    public val order: Int
}
