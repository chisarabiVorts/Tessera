package io.github.chisarabivorts.tessera.sample.home

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.FeatureEntryWithBottomBar

@Module
@InstallIn(SingletonComponent::class)
internal abstract class HomeFeatureModule {

    @Binds
    @IntoSet
    abstract fun bindHomeFeatureEntry(impl: HomeFeatureEntry): FeatureEntry

    // Also a tab in the bottom bar
    @Binds
    @IntoSet
    abstract fun bindHomeTab(impl: HomeFeatureEntry): FeatureEntryWithBottomBar
}
