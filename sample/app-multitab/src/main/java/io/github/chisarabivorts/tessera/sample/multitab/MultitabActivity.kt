package io.github.chisarabivorts.tessera.sample.multitab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import io.github.chisarabivorts.tessera.TabDeeplinkNavigator
import io.github.chisarabivorts.tessera.TabFeatureEntry
import javax.inject.Inject

@AndroidEntryPoint
class MultitabActivity : ComponentActivity() {

    @Inject lateinit var tabs: Set<@JvmSuppressWildcards TabFeatureEntry>

    @Inject lateinit var navigator: Navigator

    @Inject lateinit var resultNavigator: ResultNavigator

    @Inject lateinit var tabDeeplinkNavigator: TabDeeplinkNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MultitabMainScreen(
                        tabs = tabs,
                        navigator = navigator,
                        resultNavigator = resultNavigator,
                        tabDeeplinkNavigator = tabDeeplinkNavigator,
                    )
                }
            }
        }
        // Cold-start deep link: if launched with a URI, push it through
        // TabDeeplinkNavigator. The bridge inside MultitabState resolves the
        // owning tab, switches root to it, and dispatches the link into the
        // nested controller (queueing if its composable is not yet attached).
        intent?.let { dispatchDeepLink(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Warm-start deep link: same path through TabDeeplinkNavigator.
        // singleTask launchMode + intent filter means we land here for any
        // tessera:// URI delivered while the activity is already alive.
        setIntent(intent)
        dispatchDeepLink(intent)
    }

    private fun dispatchDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        tabDeeplinkNavigator.deepLinkToTab(deepLinkUri = uri)
    }
}
