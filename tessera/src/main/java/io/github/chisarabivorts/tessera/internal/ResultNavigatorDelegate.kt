package io.github.chisarabivorts.tessera.internal

import io.github.chisarabivorts.tessera.ResultNavigator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Default [ResultNavigator] implementation, used as the delegate inside
 * [NavigatorImpl] (`Navigator by ...`) so the same instance exposes both APIs.
 *
 * Backed by a [Channel] per result key. This gives one-shot delivery semantics:
 *  - A value published before the subscriber is ready waits in the channel and
 *    is delivered when the subscriber attaches.
 *  - Once a value has been consumed, it is gone - re-subscribing on a fresh
 *    screen entry does not redeliver the old result.
 *  - Each key has its own channel, so publishing under one key never erases a
 *    pending result waiting under another key.
 *
 * Channels have capacity 1 with `DROP_OLDEST`, so if multiple values are
 * published before consumption, only the most recent one is kept.
 *
 * Note: the underlying channels are single-consumer. Concurrent collectors on
 * the same key partition values round-robin rather than receiving them in
 * broadcast - this matches the typical "one screen awaits the result"
 * navigation pattern.
 */
internal class ResultNavigatorDelegate : ResultNavigator {

    private val channels = ConcurrentHashMap<String, Channel<Any>>()

    private fun channelFor(key: String): Channel<Any> =
        channels.computeIfAbsent(key) {
            Channel(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        }

    override fun <T> publishResult(key: String, result: T) {
        if (result != null) {
            channelFor(key).trySend(result)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> resultFlow(key: String): Flow<T> =
        channelFor(key).receiveAsFlow() as Flow<T>
}
