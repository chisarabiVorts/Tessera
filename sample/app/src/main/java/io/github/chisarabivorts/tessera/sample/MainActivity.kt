package io.github.chisarabivorts.tessera.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.github.chisarabivorts.tessera.FeatureEntry
import io.github.chisarabivorts.tessera.FeatureEntryWithBottomBar
import io.github.chisarabivorts.tessera.Navigator
import io.github.chisarabivorts.tessera.ResultNavigator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var featureEntries: Set<@JvmSuppressWildcards FeatureEntry>

    @Inject lateinit var bottomBarTabs: Set<@JvmSuppressWildcards FeatureEntryWithBottomBar>

    @Inject lateinit var navigator: Navigator

    @Inject lateinit var resultNavigator: ResultNavigator

    private val newIntents = MutableSharedFlow<Intent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        featureEntries = featureEntries,
                        bottomBarTabs = bottomBarTabs,
                        navigator = navigator,
                        resultNavigator = resultNavigator,
                        newIntents = newIntents.asSharedFlow(),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        newIntents.tryEmit(intent)
    }
}
