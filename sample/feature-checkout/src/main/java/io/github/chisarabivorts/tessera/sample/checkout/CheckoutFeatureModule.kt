package io.github.chisarabivorts.tessera.sample.checkout

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.FeatureEntryWithBottomBar

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CheckoutFeatureModule {

    // The umbrella entry - the only one external code sees.
    @Binds
    @IntoSet
    abstract fun bindCheckoutFeatureEntry(impl: CheckoutFeatureEntry): FeatureEntry

    // Also tab - appears in BottomBar.
    @Binds
    @IntoSet
    abstract fun bindCheckoutTab(impl: CheckoutFeatureEntry): FeatureEntryWithBottomBar

    // Internal step entries are NOT contributed to FeatureEntry set -
    // they register themselves through CheckoutFeatureEntry.children
    // inside the NestedFeatureEntry.registerGraph block.
}
