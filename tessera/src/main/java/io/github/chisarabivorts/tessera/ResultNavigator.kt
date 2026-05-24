package io.github.chisarabivorts.tessera

import kotlinx.coroutines.flow.Flow

/**
 * Shared bus for delivering results between screens.
 *
 * Typical usage - screen B returns a value to screen A:
 *
 * 1. On screen B (e.g. before `popBackStack()`):
 *    `resultNavigator.publishResult(SELECTED_FILTER_KEY, filter)`
 *
 * 2. On screen A (e.g. inside `LaunchedEffect` or a ViewModel):
 *    `resultNavigator.resultFlow<Filter>(SELECTED_FILTER_KEY).collect { ... }`
 *
 * Each published value is delivered exactly once. If screen A subscribes
 * before screen B publishes, the value arrives on the active subscription. If
 * screen B publishes first, the value waits until screen A subscribes, then
 * is consumed. After consumption, returning to screen A on a fresh navigation
 * does not redeliver the old value - a result is one-shot per
 * [publishResult] call.
 *
 * Results are partitioned per [key], so publishing under one key never
 * affects buffered results for another key.
 */
public interface ResultNavigator {

    /**
     * Publishes a [result] under [key] for the next subscriber listening to
     * that key.
     *
     * `null` results are silently ignored.
     */
    public fun <T> publishResult(key: String, result: T)

    /**
     * Returns a [Flow] of values published under [key].
     *
     * Subscribers receive any value previously published under this key that
     * has not yet been consumed, then each subsequent publication.
     */
    public fun <T> resultFlow(key: String): Flow<T>
}
