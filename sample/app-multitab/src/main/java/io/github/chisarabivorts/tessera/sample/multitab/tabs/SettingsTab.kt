package io.github.chisarabivorts.tessera.sample.multitab.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.TabFeatureEntry

internal class SettingsTab(
    override val children: Set<FeatureEntry>,
) : TabFeatureEntry {
    override val route: String = "settings_tab"
    override val startDestination: String = "settings"
    override val title: String = "Настройки"
    override val icon: ImageVector = Icons.Default.Settings
    override val order: Int = 1
}
