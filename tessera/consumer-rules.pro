# ===========================================================================
# Consumer ProGuard / R8 rules for Tessera.
#
# These rules are bundled into the AAR (tessera/build.gradle.kts:
# consumerProguardFiles("consumer-rules.pro")) and applied automatically when
# a consumer app runs R8/ProGuard. Keep this file minimal - only rules that
# would otherwise break consumer code.
# ===========================================================================

# ---------------------------------------------------------------------------
# Public API contract.
#
# Feature modules implement these interfaces / abstract classes and the host
# app reflects on them (Hilt multibindings, Kotlin object dispatch). Keep
# the type names so consumers can still resolve the supertypes after shrinking.
# Implementation method names are referenced through the interface and may
# safely be renamed by R8.
# ---------------------------------------------------------------------------
-keep public interface io.github.chisarabivorts.tessera.FeatureEntry
-keep public interface io.github.chisarabivorts.tessera.DialogFeatureEntry
-keep public interface io.github.chisarabivorts.tessera.FeatureEntryWithBottomBar
-keep public interface io.github.chisarabivorts.tessera.TabFeatureEntry
-keep public interface io.github.chisarabivorts.tessera.Navigator
-keep public interface io.github.chisarabivorts.tessera.ResultNavigator
-keep public interface io.github.chisarabivorts.tessera.TabDeeplinkNavigator
-keep public class io.github.chisarabivorts.tessera.NestedFeatureEntry

# ---------------------------------------------------------------------------
# NavigationIntent - sealed hierarchy used in `when` branches by the consumer
# host. Keep the type names so a consumer's debug logging / crash reports
# stay readable; properties of data classes are referenced from generated
# bytecode and don't need explicit keep rules.
# ---------------------------------------------------------------------------
-keep public class io.github.chisarabivorts.tessera.NavigationIntent
-keep public class io.github.chisarabivorts.tessera.NavigationIntent$* { *; }
-keep public class io.github.chisarabivorts.tessera.TabNavigationAction
-keep public class io.github.chisarabivorts.tessera.TabNavigationAction$* { *; }

# ---------------------------------------------------------------------------
# Factory entry point - referenced by name from no-DI consumer code and from
# the Hilt module in :tessera-hilt.
# ---------------------------------------------------------------------------
-keep public class io.github.chisarabivorts.tessera.Tessera {
    public static *** INSTANCE;
    public *** createNavigator();
    public *** createTabNavigator();
}

# ---------------------------------------------------------------------------
# MultitabState - its public surface is consumed both by app code and via
# remember/Composable callsites; keep public members so the @Composable
# `rememberMultitabState(...)` factory and its helpers stay accessible after
# shrinking.
# ---------------------------------------------------------------------------
-keep public class io.github.chisarabivorts.tessera.MultitabState {
    public <methods>;
    public <fields>;
}
-keep public class io.github.chisarabivorts.tessera.MultitabStateKt {
    public <methods>;
}
-keep public class io.github.chisarabivorts.tessera.NavControllerExtKt {
    public <methods>;
}
