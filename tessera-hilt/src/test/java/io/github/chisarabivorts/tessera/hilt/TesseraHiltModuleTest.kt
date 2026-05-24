package io.github.chisarabivorts.tessera.hilt

import io.github.chisarabivorts.tessera.ResultNavigator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TesseraHiltModuleTest {

    @Test
    fun `provideNavigator returns a non-null Navigator instance`() {
        val navigator = TesseraHiltModule.provideNavigator()
        assertNotNull(navigator)
    }

    @Test
    fun `provideNavigator returns an instance that also implements ResultNavigator`() {
        // Critical: provideResultNavigator casts the input as ResultNavigator.
        // The Navigator we provide must satisfy that cast - otherwise Hilt
        // graph construction crashes at runtime in consuming apps.
        val navigator = TesseraHiltModule.provideNavigator()
        assertTrue(
            "Navigator from TesseraHiltModule must also implement ResultNavigator",
            navigator is ResultNavigator,
        )
    }

    @Test
    fun `provideResultNavigator returns the same instance as the input Navigator`() {
        // The contract: Tessera's Navigator IS its ResultNavigator - both
        // interfaces are served by one object. This test guards against
        // anyone accidentally replacing this with a new instance.
        val navigator = TesseraHiltModule.provideNavigator()
        val resultNavigator = TesseraHiltModule.provideResultNavigator(navigator)
        assertSame(navigator, resultNavigator)
    }

    @Test
    fun `provideTabDeeplinkNavigator returns a non-null TabDeeplinkNavigator`() {
        val tabNavigator = TesseraHiltModule.provideTabDeeplinkNavigator()
        assertNotNull(tabNavigator)
    }

    @Test
    fun `each @Provides call returns a fresh instance (singleton scope is enforced by Hilt)`() {
        // The @Provides functions themselves do not cache - @Singleton is
        // applied by Hilt's component scope. Verify that calling the function
        // directly twice gives two instances, so the singleton behaviour
        // genuinely comes from Hilt, not from the module.
        val first = TesseraHiltModule.provideNavigator()
        val second = TesseraHiltModule.provideNavigator()
        assertNotSame(first, second)
    }
}
