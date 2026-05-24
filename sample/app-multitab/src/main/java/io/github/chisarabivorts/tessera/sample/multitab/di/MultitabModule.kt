package io.github.chisarabivorts.tessera.sample.multitab.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.TabFeatureEntry
import io.github.chisarabivorts.tessera.sample.multitab.tabs.CheckoutTab
import io.github.chisarabivorts.tessera.sample.multitab.tabs.HomeTab
import io.github.chisarabivorts.tessera.sample.multitab.tabs.SettingsTab

/**
 * Assembles [TabFeatureEntry]s out of the global `Set<FeatureEntry>` multibinding
 * that feature modules contribute to.
 *
 * Feature entries are addressed by their route string. Routes are duplicated
 * here as constants - in a real app, a shared `:contract` module would expose
 * them so both feature modules and tab modules can refer to the same names.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object MultitabModule {

    private const val ROUTE_HOME = "home"
    private const val ROUTE_DETAIL = "detail/{id}"
    private const val ROUTE_SETTINGS = "settings"
    private const val ROUTE_SETTINGS_DIALOG = "settings/demo_dialog"
    private const val ROUTE_CHECKOUT = "checkout"

    @Provides
    @IntoSet
    fun provideHomeTab(
        featureEntries: Set<@JvmSuppressWildcards FeatureEntry>,
    ): TabFeatureEntry {
        val byRoute = featureEntries.associateBy { it.route }
        return HomeTab(
            children = setOf(
                byRoute.getValue(ROUTE_HOME),
                byRoute.getValue(ROUTE_DETAIL),
            ),
        )
    }

    @Provides
    @IntoSet
    fun provideSettingsTab(
        featureEntries: Set<@JvmSuppressWildcards FeatureEntry>,
    ): TabFeatureEntry {
        val byRoute = featureEntries.associateBy { it.route }
        return SettingsTab(
            children = setOf(
                byRoute.getValue(ROUTE_SETTINGS),
                byRoute.getValue(ROUTE_SETTINGS_DIALOG),
            ),
        )
    }

    @Provides
    @IntoSet
    fun provideCheckoutTab(
        featureEntries: Set<@JvmSuppressWildcards FeatureEntry>,
    ): TabFeatureEntry {
        val byRoute = featureEntries.associateBy { it.route }
        return CheckoutTab(
            children = setOf(byRoute.getValue(ROUTE_CHECKOUT)),
        )
    }
}
