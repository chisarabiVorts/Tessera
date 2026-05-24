package io.github.chisarabivorts.tessera.sample.detail

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.chisarabivorts.tessera.FeatureEntry

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DetailFeatureModule {

    @Binds
    @IntoSet
    abstract fun bindDetailFeatureEntry(impl: DetailFeatureEntry): FeatureEntry
}
