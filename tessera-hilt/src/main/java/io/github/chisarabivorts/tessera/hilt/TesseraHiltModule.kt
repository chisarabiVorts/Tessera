package io.github.chisarabivorts.tessera.hilt

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import io.github.chisarabivorts.tessera.TabDeeplinkNavigator
import io.github.chisarabivorts.tessera.Tessera
import javax.inject.Singleton

/**
 * Hilt bindings for the Tessera core API.
 *
 * Apps that use Hilt get a ready-made [Navigator], [ResultNavigator] and
 * [TabDeeplinkNavigator] in [SingletonComponent] simply by depending on
 * `tessera-hilt`. No further `@Module` is required on the app side.
 *
 * Implementation note: Tessera's internal classes are deliberately `internal`,
 * so this module wires them through the public [Tessera] factory rather than
 * binding `NavigatorImpl` directly via `@Binds`. The factory returns a single
 * [Navigator] instance that also implements [ResultNavigator], so we expose
 * the same object for both interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
public object TesseraHiltModule {

    @Provides
    @Singleton
    public fun provideNavigator(): Navigator = Tessera.createNavigator()

    @Provides
    @Singleton
    public fun provideResultNavigator(navigator: Navigator): ResultNavigator =
        navigator as ResultNavigator

    @Provides
    @Singleton
    public fun provideTabDeeplinkNavigator(): TabDeeplinkNavigator =
        Tessera.createTabNavigator()
}
