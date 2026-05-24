package io.github.chisarabivorts.tessera

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation

/**
 * A feature entry that owns a nested navigation graph of its own children.
 *
 * Useful when a feature consists of multiple screens that should share a graph
 * (and a back stack scope). The implementation contributes a nested graph at
 * [route] containing all entries from [children], starting at [startRoute].
 *
 * Example:
 * ```
 * class CheckoutFeatureEntry @Inject constructor(
 *     private val cart: CartFeatureEntry,
 *     private val payment: PaymentFeatureEntry,
 * ) : NestedFeatureEntry() {
 *     override val route: String = "checkout"
 *     override val startRoute: String = cart.route
 *     override val children: List<FeatureEntry> = listOf(cart, payment)
 * }
 * ```
 */
public abstract class NestedFeatureEntry : FeatureEntry {

    /** Route of the child entry that should open first when entering this graph. */
    public abstract val startRoute: String

    /** Child entries contained in this nested graph. */
    public open val children: List<FeatureEntry> = emptyList()

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        resultNavigator: ResultNavigator,
    ) {
        require(children.any { it.route == startRoute }) {
            "NestedFeatureEntry '$route' declares startRoute = '$startRoute', " +
                "but no child entry has that route. Known children: " +
                children.map { it.route }
        }
        val duplicateChildRoutes = children
            .groupBy { it.route }
            .filterValues { it.size > 1 }
            .keys
        require(duplicateChildRoutes.isEmpty()) {
            "NestedFeatureEntry '$route' has duplicate child routes: " +
                "$duplicateChildRoutes. Each child must have a unique route, " +
                "otherwise Jetpack Navigation throws IllegalStateException " +
                "later during graph construction."
        }
        navGraphBuilder.navigation(
            route = route,
            startDestination = startRoute,
            arguments = arguments,
            deepLinks = deepLinks,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition,
        ) {
            children.forEach { child ->
                child.registerGraph(
                    navGraphBuilder = this,
                    navigator = navigator,
                    resultNavigator = resultNavigator,
                )
            }
        }
    }
}
