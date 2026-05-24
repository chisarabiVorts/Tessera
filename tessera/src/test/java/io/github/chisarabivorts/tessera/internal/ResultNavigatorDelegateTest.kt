package io.github.chisarabivorts.tessera.internal

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultNavigatorDelegateTest {

    @Test
    fun `publishResult emits to resultFlow under matching key`() = runTest {
        val delegate = ResultNavigatorDelegate()
        delegate.resultFlow<String>("k").test {
            delegate.publishResult("k", "value")
            assertEquals("value", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resultFlow filters out results published to other keys`() = runTest {
        val delegate = ResultNavigatorDelegate()
        delegate.resultFlow<String>("a").test {
            delegate.publishResult("b", "wrong-key")
            delegate.publishResult("a", "right-key")
            assertEquals("right-key", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `null result is silently dropped`() = runTest {
        val delegate = ResultNavigatorDelegate()
        delegate.publishResult<String?>("k", null)
        delegate.resultFlow<String>("k").test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `late subscriber receives previously published value`() = runTest {
        val delegate = ResultNavigatorDelegate()
        delegate.publishResult("k", "v1")
        delegate.resultFlow<String>("k").test {
            assertEquals("v1", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `latest publish wins when multiple values arrive before consumption`() = runTest {
        val delegate = ResultNavigatorDelegate()
        delegate.publishResult("k", "v1")
        delegate.publishResult("k", "v2")
        delegate.resultFlow<String>("k").test {
            assertEquals("v2", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Regression tests for bugs #2 and #3 from the review ---

    @Test
    fun `result is consumed once and not redelivered to a fresh subscriber`() = runTest {
        // Bug #2: re-entering a screen after fully popping it off the stack
        // must not redeliver an already-consumed result to the subscriber.
        val delegate = ResultNavigatorDelegate()
        delegate.publishResult("k", "v1")

        // First subscription - picks up the value.
        delegate.resultFlow<String>("k").test {
            assertEquals("v1", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Second subscription (simulates re-entering the screen) must see
        // nothing: no new publishResult was made.
        delegate.resultFlow<String>("k").test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `publishing under one key does not erase a pending result for another key`() = runTest {
        // Bug #3: the previous single replay buffer made a publish under one
        // key erase a pending value under another key.
        val delegate = ResultNavigatorDelegate()
        delegate.publishResult("a", "value-a")
        delegate.publishResult("b", "value-b")

        // Both values must be delivered independently to their subscribers.
        delegate.resultFlow<String>("a").test {
            assertEquals("value-a", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        delegate.resultFlow<String>("b").test {
            assertEquals("value-b", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
