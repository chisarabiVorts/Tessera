package io.github.chisarabivorts.tessera

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Marker for feature entries that should appear as tabs in a bottom navigation bar.
 *
 * The consuming `:app` module collects all [FeatureEntryWithBottomBar] instances
 * (typically from DI), sorts them by [order], and renders them as bottom-bar items.
 *
 * Tessera does **not** render the bottom bar itself - it only provides the marker
 * so that feature modules can declare their participation in it.
 */
public interface FeatureEntryWithBottomBar : FeatureEntry {

    /** Human-readable title shown under the tab icon. */
    public val title: String

    /** Icon shown for this tab. */
    public val icon: ImageVector

    /** Display order; lower values appear first. */
    public val order: Int
}
