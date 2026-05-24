# Tessera

> Modular Compose Navigation for Android.
> Register screens from feature modules without coupling to `:app`.

**Languages:** **English** · [Русский](README.ru.md)

[![CI](https://github.com/chisarabiVorts/tessera/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/chisarabiVorts/tessera/actions/workflows/ci.yml)
[![Version](https://img.shields.io/badge/version-0.1.0-blue)](https://github.com/chisarabiVorts/tessera/releases)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-purple)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/compose-BOM%202024.09-orange)](https://developer.android.com/jetpack/compose)

---

> **A note before you read on.**
> Tessera started as a personal project to understand Jetpack Compose Navigation
> from the inside - how nested graphs, deep links, multi-`NavHost` lifecycles and
> result passing really fit together once you try to assemble them yourself.
> It's published openly because the result turned out useful, not because it
> claims to be a battle-tested framework. The code is covered by unit tests and
> two sample apps, but it has not yet been validated by a third-party project in
> production. Feedback, issues and PRs are very welcome; please be gentle -
> this is a learning piece as much as a library.

---

## Why

I was building a multi-module Android app on Jetpack Compose Navigation
and spent a lot of time wiring up the navigation layer - nested graphs,
deep links, multi-`NavHost` lifecycles, passing results between screens.
Each of those is documented in isolation, but assembling them into a
coherent stack the way a real app needs is a different exercise.

Tessera is what came out of that. I extracted my own conventions into a
small library, partly to keep them out of every feature module's import
list, and partly because writing it as a library was the best way I had
to actually *understand* how Compose Navigation works under the hood.
The result turned out useful enough to share - not as a "framework", but
as a documented recipe plus a bit of code you can drop in.

### What it actually changes

If you read Google's [Now in Android](https://github.com/android/nowinandroid)
sample, you'll see the de-facto convention for modular navigation: each
feature module exposes a `NavGraphBuilder.featureScreen()` extension
function, and the host calls them in order. That works, and it's where
I'd start in a new project.

Tessera adds three things on top of that convention:

1. **A typed `FeatureEntry` contract instead of an extension function.**
   The host iterates a `Set<FeatureEntry>` injected via Hilt multibinding
   rather than calling `homeScreen(); detailScreen(); ...` by name.
   Adding a new feature stops touching the host module entirely - only
   the feature module changes.

2. **A result-passing channel that doesn't redeliver stale values.**
   The standard `savedStateHandle.getStateFlow(...)` from
   `previousBackStackEntry` re-emits the last value every time the
   receiver screen re-enters composition - a classic source of "the
   dialog confirmation fires twice" bugs. `ResultNavigator` uses a
   per-key `Channel(capacity=1, DROP_OLDEST)` with one-shot semantics:
   once consumed, the value is gone.

3. **A working multi-NavHost recipe** (`MultitabState` +
   `TabDeeplinkNavigator`) for apps where each tab keeps its own back
   stack. There's no official Google sample for this and the obvious
   approaches have lifecycle pitfalls (you'll hit `IllegalStateException:
   NavBackStackEntry's ViewModels after DESTROYED` quickly). The
   implementation here is covered by ~1,200 lines of tests, including
   the deep-link cold-start case.

### What it doesn't change

- The host module still depends on every feature module in
  `build.gradle.kts` - Hilt has to see them to wire the multibinding.
  The "no `:app` knowledge" is about *code*, not *build graph*.
- Tessera takes an opinionated position that features shouldn't touch
  `NavController` directly - they go through `Navigator` +
  `NavigationIntent`. If you're fine passing `NavController` around,
  this is overhead rather than a benefit.
- If your app has one `NavHost`, no result passing, and only a handful
  of features, plain `NavGraphBuilder` extension functions get you 90%
  of the benefit with zero dependencies.

---

## Install

TL;DR: Tessera is published via JitPack, two artifacts:

```kotlin
implementation("com.github.chisarabiVorts.tessera:tessera:0.1.0")
implementation("com.github.chisarabiVorts.tessera:tessera-hilt:0.1.0")  // optional, Hilt bindings
```

Full installation walkthrough (repositories, Application/Activity setup,
MainScreen wiring), all usage recipes, concept reference, sample apps,
comparison with other navigation libraries, roadmap, requirements and license
live in the developer guide:

**→ [GUIDE.md](GUIDE.md)** (currently Russian; sections are heavily code-driven
and read with auto-translation if needed).

---

## Where to go next

- **[GUIDE.md](GUIDE.md)** - installation, full usage walkthrough, recipes
- **[sample/app/](sample/app/)** - single-NavHost sample
- **[sample/app-multitab/](sample/app-multitab/)** - multi-NavHost sample with per-tab back stacks
- **[LICENSE](LICENSE)** - Apache 2.0
