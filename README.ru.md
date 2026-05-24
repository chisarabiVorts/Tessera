# Tessera

> Модульная навигация Compose для Android.
> Регистрируйте экраны из feature-модулей без привязки к `:app`.

**Языки:** [English](README.md) · **Русский**

[![CI](https://github.com/chisarabiVorts/tessera/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/chisarabiVorts/tessera/actions/workflows/ci.yml)
[![Version](https://img.shields.io/badge/version-0.1.0-blue)](https://github.com/chisarabiVorts/tessera/releases)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-purple)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/compose-BOM%202024.09-orange)](https://developer.android.com/jetpack/compose)

---

> **Пара слов перед чтением.**
> Tessera задумывалась как личный проект, чтобы изнутри разобраться с Jetpack
> Compose Navigation - как на практике соединяются вложенные графы, deep links,
> жизненный цикл multi-`NavHost` и передача результатов, когда пытаешься собрать
> всё это самостоятельно. Опубликована открыто, потому что результат оказался
> полезным, а не потому, что претендует на статус production-фреймворка. Код
> покрыт юнит-тестами и двумя sample-приложениями, но пока не прошёл проверку
> сторонним проектом в проде. Фидбэк, issue и PR-ы - welcome; пожалуйста, не
> судите строго: это в равной степени и учебная работа, и библиотека.

---

## Зачем

Я делал многомодульное Android-приложение на Jetpack Compose Navigation
и потратил много времени на навигационный слой - вложенные графы, deep
links, жизненный цикл multi-`NavHost`, передачу результатов между
экранами. Каждая из этих тем документирована отдельно, но собрать их в
связную конструкцию, как нужно реальному приложению, - совсем другое
упражнение.

Tessera - то, что получилось в итоге. Я вынес свои конвенции в небольшую
библиотеку: отчасти чтобы не таскать их через import'ы каждого
feature-модуля, отчасти потому что написать всё это в виде библиотеки -
лучший способ *действительно разобраться*, как Compose Navigation
работает под капотом. Результат оказался полезным, и я решил его
опубликовать - не как "фреймворк", а как документированный рецепт плюс
немного кода, который можно подключить.

### Что реально меняется

Если посмотришь Google'овский [Now in Android](https://github.com/android/nowinandroid),
увидишь de-facto конвенцию для модульной навигации: каждый feature-модуль
экспортирует extension-функцию `NavGraphBuilder.featureScreen()`, а host
вызывает их по очереди. Это работает, и в новом проекте я бы начал именно
так.

Tessera добавляет три вещи поверх этой конвенции:

1. **Типизированный контракт `FeatureEntry` вместо extension-функции.**
   Host итерируется по `Set<FeatureEntry>`, заинжекченному через Hilt
   multibinding, а не вызывает `homeScreen(); detailScreen(); ...` по
   имени. Добавление новой фичи перестаёт трогать host-модуль вообще -
   меняется только сама фича.

2. **Канал передачи результатов, который не доставляет старые значения
   повторно.** Стандартный `savedStateHandle.getStateFlow(...)` у
   `previousBackStackEntry` повторно эмитит последнее значение каждый
   раз, когда экран-получатель возвращается в composition - классический
   источник багов "подтверждение диалога стреляет дважды".
   `ResultNavigator` использует per-key `Channel(capacity=1, DROP_OLDEST)`
   с one-shot семантикой: раз потреблённое значение пропадает.

3. **Рабочий рецепт multi-NavHost** (`MultitabState` +
   `TabDeeplinkNavigator`) для приложений, где каждый таб держит свой
   back stack. Официального Google'овского sample на это нет, а
   очевидные подходы имеют ловушки жизненного цикла (быстро поймаешь
   `IllegalStateException: NavBackStackEntry's ViewModels after
   DESTROYED`). Реализация покрыта ~1200 строк тестов, включая
   cold-start deep link.

### Что не меняется

- Host-модуль всё равно зависит от каждого feature-модуля в
  `build.gradle.kts` - Hilt должен их видеть, чтобы собрать multibinding.
  "`:app` ничего не знает о фичах" - это про *код*, не про *build graph*.
- Tessera занимает позицию, что фичи не должны трогать `NavController`
  напрямую - они идут через `Navigator` + `NavigationIntent`. Если ты
  спокойно прокидываешь `NavController` вниз, это overhead, а не
  выигрыш.
- Если у тебя один `NavHost`, без передачи результатов и горстка фич -
  обычные extension-функции `NavGraphBuilder` дают 90% пользы без
  каких-либо зависимостей.

---

## Установка

TL;DR: Tessera публикуется через JitPack, два артефакта:

```kotlin
implementation("com.github.chisarabiVorts.tessera:tessera:0.1.0")
implementation("com.github.chisarabiVorts.tessera:tessera-hilt:0.1.0")  // опционально, Hilt-биндинги
```

Полный walkthrough установки (репозитории, поднятие Application/Activity,
подключение `MainScreen`), все рецепты использования, концепции, sample-приложения,
сравнение с другими навигационными подходами, дорожная карта, требования и лицензия -
в гайде разработчика:

**→ [GUIDE.md](GUIDE.md)**

---

## Куда дальше

- **[GUIDE.md](GUIDE.md)** - установка, полный walkthrough, рецепты
- **[sample/app/](sample/app/)** - single-NavHost sample
- **[sample/app-multitab/](sample/app-multitab/)** - multi-NavHost sample с независимыми back stack на таб
- **[LICENSE](LICENSE)** - Apache 2.0
