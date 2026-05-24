package io.github.chisarabivorts.tessera

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog

/**
 * A feature entry rendered as a dialog (or bottom sheet) instead of a full-screen
 * composable.
 *
 * Registered via [NavGraphBuilder.dialog]; transitions are managed by the dialog
 * window, so the `enter/exit/pop*Transition` fields from [FeatureEntry] are ignored.
 *
 * Example:
 * ```
 * class ConfirmLogoutDialogEntry @Inject constructor() : DialogFeatureEntry {
 *     override val route: String = "auth/confirm-logout"
 *
 *     @Composable
 *     override fun Content(
 *         navBackStackEntry: NavBackStackEntry,
 *         navigator: Navigator,
 *         resultNavigator: ResultNavigator,
 *     ) {
 *         AlertDialog(
 *             onDismissRequest = { navigator.popBackStack() },
 *             confirmButton = {
 *                 TextButton(onClick = {
 *                     resultNavigator.publishResult("logout_confirmed", true)
 *                     navigator.popBackStack()
 *                 }) { Text("Log out") }
 *             },
 *             dismissButton = {
 *                 TextButton(onClick = { navigator.popBackStack() }) { Text("Cancel") }
 *             },
 *             title = { Text("Log out?") },
 *         )
 *     }
 * }
 * ```
 */
public interface DialogFeatureEntry : FeatureEntry {

    /** Properties controlling dialog behaviour (dismiss policy, width, etc.). */
    public val dialogProperties: DialogProperties
        get() = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        resultNavigator: ResultNavigator,
    ) {
        navGraphBuilder.dialog(
            route = route,
            arguments = arguments,
            deepLinks = deepLinks,
            dialogProperties = dialogProperties,
        ) { backStackEntry ->
            Content(
                navBackStackEntry = backStackEntry,
                navigator = navigator,
                resultNavigator = resultNavigator,
            )
        }
    }
}
