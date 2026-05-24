package io.github.chisarabivorts.tessera.sample.multitab.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.TabFeatureEntry

internal class HomeTab(
    override val children: Set<FeatureEntry>,
) : TabFeatureEntry {
    override val route: String = "home_tab"
    override val startDestination: String = "home"
    override val title: String = "Главная"
    override val icon: ImageVector = Icons.Default.Home
    override val order: Int = 0
}
