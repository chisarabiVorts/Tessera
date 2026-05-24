package io.github.chisarabivorts.tessera.sample.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.FeatureEntryWithBottomBar

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsFeatureModule {

    @Binds
    @IntoSet
    abstract fun bindSettingsFeatureEntry(impl: SettingsFeatureEntry): FeatureEntry

    // Also a tab in the bottom bar
    @Binds
    @IntoSet
    abstract fun bindSettingsTab(impl: SettingsFeatureEntry): FeatureEntryWithBottomBar

    @Binds
    @IntoSet
    abstract fun bindDemoDialogFeatureEntry(impl: DemoDialogFeatureEntry): FeatureEntry
}
