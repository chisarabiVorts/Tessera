package io.github.chisarabivorts.tessera

import io.github.chisarabivorts.tessera.internal.NavigatorImpl
import io.github.chisarabivorts.tessera.internal.ResultNavigatorDelegate
import io.github.chisarabivorts.tessera.internal.TabNavigatorImpl

/**
 * Public entry point for users who want to wire Tessera without a DI framework.
 *
 * Pick the factory matching your need:
 *
 * - [createNavigator] - a [Navigator] that also exposes [ResultNavigator]
 *   (the returned instance can be cast / used as either).
 * - [createTabNavigator] - a separate [TabDeeplinkNavigator] for cross-tab
 *   deep links.
 *
 * If you use Hilt, depend on `tessera-hilt` instead - it registers ready-made
 * `@Provides` bindings backed by these same factories.
 *
 * Each call creates a fresh instance; treat them as singletons in your app
 * (cache once per process / Activity scope).
 *
 * Example - no-DI wiring inside an Activity:
 * ```
 * class MainActivity : ComponentActivity() {
 *     // Hold the same Navigator instance for the whole Activity lifetime.
 *     private val navigator: Navigator = Tessera.createNavigator()
 *     private val resultNavigator: ResultNavigator = navigator as ResultNavigator
 *
 *     private val featureEntries: Set<FeatureEntry> = setOf(
 *         HomeFeatureEntry(),
 *         DetailFeatureEntry(),
 *         // ... add more features here
 *     )
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         setContent {
 *             MainScreen(featureEntries, navigator, resultNavigator)
 *         }
 *     }
 * }
 * ```
 */
public object Tessera {

    /**
     * Creates a [Navigator] that also implements [ResultNavigator].
     *
     * The returned object is the same instance for both interfaces - feature
     * code that needs both can take a single [Navigator] reference and cast,
     * or you can expose each role separately at the DI boundary.
     */
    public fun createNavigator(): Navigator =
        NavigatorImpl(resultDelegate = ResultNavigatorDelegate())

    /** Creates a stand-alone [TabDeeplinkNavigator] for cross-tab deep links. */
    public fun createTabNavigator(): TabDeeplinkNavigator = TabNavigatorImpl()
}
