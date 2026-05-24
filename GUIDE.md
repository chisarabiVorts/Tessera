# Tessera - руководство разработчика

Практический гайд: как установить Tessera и пользоваться ей.
Зачем нужна и что меняет - см. [`README.md`](README.md) / [`README.ru.md`](README.ru.md).
История изменений - в [`CHANGELOG.md`](CHANGELOG.md).

---

## Содержание

1. [Установка и первый запуск](#1-установка-и-первый-запуск)
2. [Быстрый старт за 5 минут](#2-быстрый-старт-за-5-минут)
3. [Основные концепции](#3-основные-концепции)
4. [Структура проекта](#4-структура-проекта-на-tessera)
5. [Сценарии (рецепты)](#5-сценарии-рецепты)
6. [Анимации переходов](#6-анимации-переходов)
7. [Single-NavHost vs Multi-NavHost](#7-single-navhost-vs-multi-navhost)
8. [Паттерн BottomBar `selectTab`](#8-паттерн-bottombar-selecttab)
9. [Sample-приложения](#9-sample-приложения)
10. [Что есть и чего нет в 0.1.0](#10-что-есть-и-чего-нет-в-010)
11. [Сравнение с другими подходами](#11-сравнение-с-другими-подходами)
12. [Какие приложения можно строить](#12-какие-приложения-можно-строить)
13. [Что НЕ покрыто библиотекой](#13-что-не-покрыто-библиотекой)
14. [Дорожная карта](#14-дорожная-карта)
15. [Требования](#15-требования)
16. [Лицензия](#16-лицензия)

---

## 1. Установка и первый запуск

### Gradle

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")  // ← Tessera публикуется здесь
    }
}
```

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

dependencies {
    // Координаты JitPack: com.github.{user}.{repo}:{module}:{version}
    implementation("com.github.chisarabiVorts.tessera:tessera:0.1.0")
    implementation("com.github.chisarabiVorts.tessera:tessera-hilt:0.1.0")  // готовые @Provides

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.navigation.compose)
    // Compose BOM, material3, activity-compose - стандартно
}
```

### Application + Activity

```kotlin
@HiltAndroidApp
class SampleApplication : Application()
```

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var featureEntries: Set<@JvmSuppressWildcards FeatureEntry>
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var resultNavigator: ResultNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(featureEntries, navigator, resultNavigator)
            }
        }
    }
}
```

### MainScreen - собирает граф из всех фич

```kotlin
@Composable
fun MainScreen(
    featureEntries: Set<FeatureEntry>,
    navigator: Navigator,
    resultNavigator: ResultNavigator,
) {
    val navController = rememberNavController()

    LaunchedEffect(navController) {
        navigator.navigationActions.collect { intent ->
            navController.applyNavigationIntent(intent)
        }
    }

    NavHost(navController, startDestination = "home") {
        featureEntries.forEach { entry ->
            entry.registerGraph(this, navigator, resultNavigator)
        }
    }
}
```

Всё. После этого добавление новой фичи = добавить feature-модуль в зависимости. Никаких правок в `MainScreen`.

---

## 2. Быстрый старт за 5 минут

Минимальный пример «новая фича за три шага», без обвязки Application/Activity (это уже сделано в разделе 1).

### Шаг 1 - реализовать `FeatureEntry` в feature-модуле

```kotlin
// :feature-chat/.../ChatFeatureEntry.kt
internal class ChatFeatureEntry @Inject constructor() : FeatureEntry {

    override val route: String = "chat/{$ARG_DIALOG_ID}"

    override val arguments = listOf(
        navArgument(ARG_DIALOG_ID) { type = NavType.StringType },
    )

    override val deepLinks = listOf(
        navDeepLink { uriPattern = "myapp://chat/{$ARG_DIALOG_ID}" },
    )

    @Composable
    override fun Content(
        navBackStackEntry: NavBackStackEntry,
        navigator: Navigator,
        resultNavigator: ResultNavigator,
    ) {
        val dialogId = navBackStackEntry.arguments?.getString(ARG_DIALOG_ID).orEmpty()
        ChatScreen(
            dialogId = dialogId,
            onBack = { navigator.popBackStack() },
            onOpenProfile = { userId -> navigator.navigate("profile/$userId") },
        )
    }

    companion object {
        const val ARG_DIALOG_ID = "dialog_id"
    }
}
```

### Шаг 2 - добавить вклад в multibinding (Hilt)

```kotlin
// :feature-chat/.../ChatFeatureModule.kt
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ChatFeatureModule {

    @Binds @IntoSet
    abstract fun bindChatFeatureEntry(impl: ChatFeatureEntry): FeatureEntry
}
```

### Шаг 3 - `MainScreen` уже умеет

Если ты пришёл из раздела 1, `MainScreen` уже собирает граф из всех `FeatureEntry`
через multibinding. Новая фича подхватится автоматически - без правок в `:app`.

Полный рабочий пример - в [`sample/app/`](sample/app/).

---

## 3. Основные концепции

### `FeatureEntry`

Базовый интерфейс - описание одного экрана. Содержит `route`, опциональные `arguments` / `deepLinks` / transitions, и `@Composable Content(...)` с самим экраном.

```kotlin
internal class HomeFeatureEntry @Inject constructor() : FeatureEntry {
    override val route = "home"

    @Composable
    override fun Content(
        navBackStackEntry: NavBackStackEntry,
        navigator: Navigator,
        resultNavigator: ResultNavigator,
    ) {
        HomeScreen(navigator, resultNavigator)
    }
}
```

### `Navigator`

Шина команд навигации. Фича вызывает `navigator.navigate("detail/42")` - это становится `NavigationIntent`, который коллектор в `MainScreen` применяет к `NavController`.

Фича **не видит** `NavController`. Зависимость идёт в одну сторону.

### `ResultNavigator`

Шина для передачи значений между экранами:

```kotlin
// Экран B (например, перед popBackStack):
resultNavigator.publishResult("selected_id", id)

// Экран A:
val selectedId by resultNavigator
    .resultFlow<String>("selected_id")
    .collectAsState(initial = null)
```

Каждый publish доставляется ровно один раз - повторное возвращение на экран A не получает stale-значение.

### `NestedFeatureEntry`

Фича - это **граф из нескольких экранов**. Используется для многошаговых потоков (checkout, onboarding).

```kotlin
class CheckoutFeatureEntry @Inject constructor(
    private val address: AddressStepEntry,
    private val confirm: ConfirmStepEntry,
) : NestedFeatureEntry() {
    override val route = "checkout"
    override val startRoute = "checkout/address"
    override val children = listOf(address, confirm)
}
```

Внешний код видит только `"checkout"`. Внутри - нормальный вложенный NavGraph.

### `DialogFeatureEntry`

Экран регистрируется как **диалог** (через `NavGraphBuilder.dialog`):

```kotlin
class ConfirmLogoutDialog @Inject constructor() : DialogFeatureEntry {
    override val route = "auth/confirm-logout"

    @Composable
    override fun Content(...) { AlertDialog(...) }
}
```

Открывается тем же `navigator.navigate(route)` - Tessera сама поймёт, что это диалог.

### `FeatureEntryWithBottomBar`

Маркер: фича должна появиться **табом** в bottom bar.

```kotlin
internal class HomeFeatureEntry @Inject constructor() : FeatureEntryWithBottomBar {
    override val route = "home"
    override val title = "Главная"
    override val icon = Icons.Default.Home
    override val order = 0

    @Composable
    override fun Content(...) { HomeScreen(...) }
}
```

UI bottom bar пишет твой `:app` - Tessera не диктует layout, только даёт информацию.

### `TabFeatureEntry` + `MultitabState` (multi-NavHost)

Для приложений с **независимыми back stack на таб** (Instagram, Telegram, банковские). Каждый таб - свой `NavHost` со своим `NavController`. State holder `MultitabState` координирует переключения, intent'ы и deep link'и между табами.

См. раздел [Single-NavHost vs Multi-NavHost](#7-single-navhost-vs-multi-navhost) - выбор зависит от того, нужны ли тебе настоящие independent stacks.

### `TabDeeplinkNavigator`

Отдельная шина для cross-tab deep link'ов (например, push-уведомление открывает экран **внутри конкретного таба**). Tessera резолвит, какому табу принадлежит URI, переключает таб и пробрасывает navigate внутри.

### `Tessera.createNavigator()` / `createTabNavigator()`

Фабрики для проектов **без DI**:

```kotlin
val navigator: Navigator = Tessera.createNavigator()
val resultNavigator: ResultNavigator = navigator as ResultNavigator
val tabDeeplinkNavigator: TabDeeplinkNavigator = Tessera.createTabNavigator()
```

С Hilt не нужны - `tessera-hilt` автоматически их предоставляет.

---

## 4. Структура проекта на Tessera

Типичная многомодульная раскладка:

```
my-app/
├── app/                          ← :app
│   ├── SampleApplication.kt
│   ├── MainActivity.kt
│   └── MainScreen.kt
├── feature-home/                 ← :feature-home
│   ├── HomeFeatureEntry.kt
│   ├── HomeFeatureModule.kt      (Hilt: @Binds @IntoSet → FeatureEntry)
│   └── HomeScreen.kt
├── feature-detail/               ← :feature-detail
│   ├── DetailFeatureEntry.kt
│   ├── DetailFeatureModule.kt
│   └── DetailScreen.kt
├── feature-checkout/             ← :feature-checkout (NestedFeatureEntry)
│   ├── CheckoutFeatureEntry.kt
│   ├── steps/AddressStepEntry.kt
│   ├── steps/ConfirmStepEntry.kt
│   ├── ui/AddressScreen.kt
│   └── ui/ConfirmScreen.kt
└── settings.gradle.kts
```

### Принципы

- **Код `:app` не упоминает feature-модули** ни по типу, ни по route. Видит их только как `Set<FeatureEntry>`, заинжекченный через Hilt. Build-граф остаётся: `build.gradle.kts` `:app`-модуля всё равно содержит `implementation(project(":feature-..."))` для каждой фичи - это и нужно, чтобы Hilt их увидел.
- **Feature-модули не знают друг о друге**. Если Home хочет открыть Detail - она вызывает `navigator.navigate("detail/42")`. Это просто строка, без import.
- **Feature-модуль НЕ зависит от** `:app`. Зависит только от `:tessera` (и `:tessera-hilt`).
- **Маршруты - строки**. В 0.1.x. Type-safe `@Serializable` маршруты - в roadmap 0.2.0.

---

## 5. Сценарии (рецепты)

### 5.1 Простая навигация

```kotlin
// Внутри какого-то экрана
Button(onClick = { navigator.navigate("settings") }) { Text("Открыть Настройки") }
```

### 5.2 Навигация с аргументами

```kotlin
// DetailFeatureEntry
override val route = "detail/{id}"
override val arguments = listOf(
    navArgument("id") { type = NavType.StringType }
)

@Composable
override fun Content(navBackStackEntry: NavBackStackEntry, ...) {
    val id = navBackStackEntry.arguments?.getString("id").orEmpty()
    DetailScreen(id = id, ...)
}
```

```kotlin
// Где-то в Home:
Button(onClick = { navigator.navigate("detail/42") }) { Text("Open #42") }
```

### 5.3 Возврат на предыдущий экран

```kotlin
IconButton(onClick = { navigator.popBackStack() }) { Text("←") }
```

### 5.4 Передача результата между экранами

```kotlin
// Detail: вернуть выбранный id
Button(onClick = {
    resultNavigator.publishResult("selected_id", "42")
    navigator.popBackStack()
}) { Text("Подтвердить") }
```

```kotlin
// Home: подписаться
val selectedId by resultNavigator
    .resultFlow<String>("selected_id")
    .collectAsState(initial = null)

selectedId?.let { Text("Выбран: $it") }
```

Результат **one-shot per publish** - повторный заход на Home без нового publish не получит старое значение.

### 5.5 Диалог как destination

```kotlin
class DemoDialog @Inject constructor() : DialogFeatureEntry {
    override val route = "demo-dialog"

    @Composable
    override fun Content(...) {
        Surface(...) { Text("Я диалог"); Button(onClick = { navigator.popBackStack() }) {...} }
    }
}
```

Открыть: `navigator.navigate("demo-dialog")`.

### 5.6 Многошаговый поток (NestedFeatureEntry)

```kotlin
class CheckoutFeatureEntry @Inject constructor(
    private val address: AddressStepEntry,
    private val confirm: ConfirmStepEntry,
) : NestedFeatureEntry() {
    override val route = "checkout"
    override val startRoute = address.route          // "checkout/address"
    override val children = listOf(address, confirm)
}
```

Открыть весь flow: `navigator.navigate("checkout")` - попадаешь на `checkout/address`. Из него `navigator.navigate("checkout/confirm")` - на следующий шаг. По окончании `navigator.popBackStackTo("home")` - выходишь из всего флоу разом, минуя промежуточные экраны.

### 5.7 Deep links (cold + warm start)

В FeatureEntry - объявляешь шаблоны:

```kotlin
override val deepLinks = listOf(
    navDeepLink { uriPattern = "myapp://detail/{id}" }
)
```

В `AndroidManifest.xml` Activity:
```xml
<activity android:name=".MainActivity" android:launchMode="singleTask">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="myapp" />
    </intent-filter>
</activity>
```

Cold start работает автоматически. Warm start (deep link когда app уже запущен):

```kotlin
class MainActivity : ComponentActivity() {
    private val newIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)

    override fun onCreate(...) {
        setContent { MainScreen(..., newIntents = newIntents.asSharedFlow()) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        newIntents.tryEmit(intent)
    }
}

// MainScreen
LaunchedEffect(navController) {
    newIntents.collect { navController.handleDeepLink(it) }
}
```

### 5.8 Bottom-bar табы (single-NavHost)

Feature помечается как таб:

```kotlin
class HomeFeatureEntry @Inject constructor() : FeatureEntryWithBottomBar {
    override val route = "home"
    override val title = "Главная"
    override val icon = Icons.Default.Home
    override val order = 0
    @Composable override fun Content(...) { HomeScreen(...) }
}
```

В `:app` собираешь Set:
```kotlin
@Inject lateinit var bottomBarTabs: Set<@JvmSuppressWildcards FeatureEntryWithBottomBar>

// В UI
NavigationBar {
    bottomBarTabs.sortedBy { it.order }.forEach { tab ->
        NavigationBarItem(
            selected = tab.route == currentRoute,
            onClick = { /* selectTab pattern из README */ },
            icon = { Icon(tab.icon, contentDescription = tab.title) },
            label = { Text(tab.title) },
        )
    }
}
```

### 5.9 Multi-NavHost (Instagram-style независимые стеки)

Группируешь фичи в табы:

```kotlin
class HomeTab(override val children: Set<FeatureEntry>) : TabFeatureEntry {
    override val route = "home_tab"
    override val startDestination = "home"
    override val title = "Главная"
    override val icon = Icons.Default.Home
    override val order = 0
}

@Module @InstallIn(SingletonComponent::class)
object MultitabModule {
    @Provides @IntoSet
    fun provideHomeTab(features: Set<@JvmSuppressWildcards FeatureEntry>): TabFeatureEntry {
        val byRoute = features.associateBy { it.route }
        return HomeTab(children = setOf(byRoute.getValue("home"), byRoute.getValue("detail/{id}")))
    }
}
```

В MainScreen используешь `rememberMultitabState`:

```kotlin
val state = rememberMultitabState(
    tabs = tabs,
    navigator = navigator,
    resultNavigator = resultNavigator,
    tabDeeplinkNavigator = tabDeeplinkNavigator,
)

Scaffold(bottomBar = { /* NavigationBar используя state.selectedTab */ }) {
    NavHost(state.rootNavController, startDestination = state.tabs.first().route) {
        state.tabs.forEach { tab ->
            composable(tab.route) {
                val nestedController = rememberNavController()
                DisposableEffect(nestedController) {
                    state.attachNestedController(tab.route, nestedController)
                    onDispose { state.detachNestedController(tab.route) }
                }
                NavHost(nestedController, startDestination = tab.startDestination) {
                    tab.children.forEach { it.registerGraph(this, navigator, resultNavigator) }
                }
            }
        }
    }
}
```

Полный пример - в [`sample/app-multitab/`](sample/app-multitab/).

### 5.10 Cross-tab навигация изнутри фичи

`navigator.navigate("settings")` из фичи в табе Home - **умный** routing: если "settings" принадлежит другому табу, Tessera **сама** переключит таб и навигирует.

Если хочешь явно переключить таб без перехода внутрь - `navigator.switchToTab("settings_tab")`.

### 5.11 Cross-tab deep link (push-уведомление, BroadcastReceiver)

```kotlin
// Из BroadcastReceiver, Service, WorkManager job, notification handler
tabDeeplinkNavigator.deepLinkToTab(
    deepLinkUri = Uri.parse("myapp://settings/about")
)
```

`MultitabState` сам найдёт таб (через рекурсивный обход `TabFeatureEntry.children` и их вложенных `NestedFeatureEntry`), переключит root на него и применит deep link к nested controller'у. Если в момент вызова target-таб ещё не в composition - intent queue'ится и dispatch'ится при `attachNestedController`.

### 5.12 Cold-start deep link в multi-NavHost

Когда приложение запускается **впервые** по deep link'у (тап на push, deep link из браузера и т.п.), Activity.onCreate получает Intent с URI. В multi-NavHost подключается через `TabDeeplinkNavigator`:

```kotlin
@AndroidEntryPoint
class MultitabActivity : ComponentActivity() {
    @Inject lateinit var tabDeeplinkNavigator: TabDeeplinkNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { /* MultitabMainScreen(...) */ }
        intent?.data?.let { tabDeeplinkNavigator.deepLinkToTab(it) }
    }
}
```

```xml
<!-- AndroidManifest.xml - добавь intent-filter под используемый scheme -->
<activity android:name=".MultitabActivity" android:launchMode="singleTask">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="myapp" />
    </intent-filter>
</activity>
```

Что происходит:
1. Activity создаётся, deep link отправляется в `TabDeeplinkNavigator` **до** того как composition отрисуется
2. Channel буферизует intent
3. `MultitabMainScreen` → `rememberMultitabState` → `LaunchedEffect` запускает collector
4. Collector подхватывает intent → `handleTabDeeplinkAction`
5. Резолвер находит владеющий таб (рекурсивно по children), `selectTab` переключает root
6. Target tab's composable входит в composition → attach nested controller → queue для этого таба drain'ится → navigate

Глубокий nested destination (например, `myapp://settings/privacy/two-factor` внутри 2-уровневого NestedFeatureEntry) попадает в правильное место с автоматически построенным back stack'ом.

### 5.13 Warm-start deep link

Когда приложение **уже запущено** и приходит новый Intent (push tap в running app):

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    intent.data?.let { tabDeeplinkNavigator.deepLinkToTab(it) }
}
```

`launchMode="singleTask"` гарантирует, что один и тот же Activity instance получает Intent через `onNewIntent`. Та же шина `TabDeeplinkNavigator` обрабатывает его.

См. реальную реализацию в [`sample/app-multitab/MultitabActivity.kt`](sample/app-multitab/src/main/java/io/github/chisarabivorts/tessera/sample/multitab/MultitabActivity.kt).

### 5.14 Tap-to-top (Instagram-style повторный тап по табу)

По дефолту повторный тап по активному табу - no-op (`Material 3` спецификация). Если хочешь поведение Instagram/Twitter - "тап по активному табу возвращает на верх стека таба" - передай флаг при создании state holder'а:

```kotlin
val state = rememberMultitabState(
    tabs = tabs,
    navigator = navigator,
    resultNavigator = resultNavigator,
    tabDeeplinkNavigator = tabDeeplinkNavigator,
    resetActiveTabOnReselect = true,   // ← добавлено
)
```

Тогда при клике по уже активному табу его nested back stack **схлопывается до `tab.startDestination`**. Сценарий:
- Пользователь на Главная → Detail #42 (stack `[home, detail/42]`)
- Тап по табу "Главная" → стек становится `[home]`
- Ещё тап по "Главная" → уже на старте, no-op

`MultitabState.selectTab(tab.route)` внутри сам делает `popBackStack(tab.startDestination, inclusive = false)` для активного таба - IAE если стек уже на старте проглатывается, поведение остаётся safe.

### 5.15 Переиспользование `FeatureEntry` в нескольких табах

**Технически возможно**, но осторожно. Положив одну и ту же `FeatureEntry` в `children` двух разных табов, ты получаешь:

✅ **Что работает:**
- Два независимых destination'а в двух разных nested NavController графах
- Независимые back stack'и и ViewModel scope'ы - пользователь может быть на shared/profile одновременно в двух табах
- Smart routing **предпочитает активный таб** - если ты на settings_tab и эмитишь `navigator.navigate("shared/profile")`, остаёшься в settings_tab локально (не переключается на home_tab где тоже есть этот route)

⚠️ **Подводные камни:**
- `tabRouteForUri` (резолвер для cross-tab deep link'ов) находит **первый** таб с матчем по `TabFeatureEntry.order` - если push-deep link на `shared/profile` придёт извне, попадёт всегда в первый таб
- Если активный таб НЕ содержит этот route - smart routing уйдёт в первый по order, не туда куда ожидает пользователь

**Рекомендация:** в большинстве случаев лучше **извлечь общий UI в `@Composable`-функцию**, а не делить одну `FeatureEntry`:

```kotlin
// :feature-shared/SharedProfileScreen.kt
@Composable
fun SharedProfileScreen(...) { /* общая реализация */ }

// :feature-home/HomeProfileEntry.kt
class HomeProfileEntry @Inject constructor() : FeatureEntry {
    override val route = "home/profile"  // ← разный route!
    @Composable override fun Content(...) { SharedProfileScreen(...) }
}

// :feature-settings/SettingsProfileEntry.kt
class SettingsProfileEntry @Inject constructor() : FeatureEntry {
    override val route = "settings/profile"  // ← разный route!
    @Composable override fun Content(...) { SharedProfileScreen(...) }
}
```

Каждый таб имеет свой entry с локальным route. Smart routing работает прозрачно. Общий UI-код один.

**Прямое переиспользование** `FeatureEntry` имеет смысл когда:
- Это глобальный overlay-экран (типа `HelpDialogEntry`) - реально один и тот же из любого таба
- Auth-экран, доступный из любого места: `navigator.navigate("auth/login")`

---

## 6. Анимации переходов

Каждый `FeatureEntry` может переопределить 4 transition-свойства:

```kotlin
override val enterTransition = {
    slideIntoContainer(SlideDirection.Left, tween(300))
}
override val exitTransition = {
    slideOutOfContainer(SlideDirection.Left, tween(300))
}
override val popEnterTransition = {
    slideIntoContainer(SlideDirection.Right, tween(300))
}
override val popExitTransition = {
    slideOutOfContainer(SlideDirection.Right, tween(300))
}
```

| Поле | Когда применяется |
|---|---|
| `enterTransition` | A→B, как **B появляется** |
| `exitTransition` | A→B, как **A уходит** |
| `popEnterTransition` | B→A (system back), как **A возвращается** |
| `popExitTransition` | B→A, как **B уходит** |

**Дефолт** - мгновенный fade (длительность 0), чтобы не было нежелательных flash. Каждая фича сама решает свою анимацию.

**Доступные эффекты** из `androidx.compose.animation`: `fadeIn/fadeOut`, `slideIn/slideOut`, `scaleIn/scaleOut`, `expandIn/shrinkOut`. Комбинируешь через `+`.

**Особенности:**
- `DialogFeatureEntry` игнорирует transitions FeatureEntry - диалоги используют свои Window-анимации
- Глобальный `TransitionConfig` (один набор на всё приложение) - в roadmap 0.2.0+

---

## 7. Single-NavHost vs Multi-NavHost

Два валидных паттерна. Выбор зависит от UX-требований.

| Аспект | Single-NavHost | Multi-NavHost |
|---|---|---|
| Один `NavController` на app | ✅ | ❌ (root + N nested) |
| Глубина back stack между табами | Сохраняется только **текущий destination** таба | **Весь стек** сохраняется |
| Сложность кода в `:app` | Минимум | Средне (`rememberMultitabState` + per-tab `rememberNavController`) |
| Cross-tab навигация | "Просто работает" | Работает + smart routing внутри либы |
| Подходит для | Простых приложений с табами на уровне разделов | Instagram/Telegram-style, где история **в каждом табе** должна выживать переключение |

### Когда single

- Новостной ридер: Главная / Поиск / Сохранённое
- Простой банковский UI
- Большинство business-приложений

### Когда multi

- Соцсеть: ходишь по постам в Feed → переключился на Profile → вернулся в Feed → **должен быть на том же посте**
- Messenger: чат → подпереход в Profile собеседника → перешёл на Calls → вернулся в Chats → должен быть на том же месте
- Любое приложение, где пользователь ожидает что таб помнит "где я был"

См. оба варианта работающими: [`sample/app/`](sample/app/) и [`sample/app-multitab/`](sample/app-multitab/).

---

## 8. Паттерн BottomBar `selectTab`

У классического паттерна multi-tab с сохранением состояния между табами есть
тонкий edge case в Compose Navigation: если переходить на маршрут, который
одновременно является `popUpTo`-целью и уже лежит в back stack, операция
может silent-fail (визуально ничего не меняется). Рецепт ниже обходит это:
для start destination используется `popBackStackTo`, для остальных - обычный
`navigate(...)`.

```kotlin
// В твоём :app - функция рядом с MainScreen.
private fun selectTab(
    navigator: Navigator,
    currentRoute: String?,
    tabRoute: String,
    startDestinationRoute: String,
) {
    when {
        // Уже на этом табе - ничего не делаем.
        tabRoute == currentRoute -> Unit

        // Возврат на start destination - снимаем всё до него.
        // popBackStackTo обходит edge case "navigate-to-current with popUpTo".
        tabRoute == startDestinationRoute -> {
            navigator.popBackStackTo(route = startDestinationRoute)
        }

        // Переход на любой другой таб - стандартный navigate с
        // сохранением состояния, чтобы потом восстановить scroll / форму.
        else -> {
            navigator.navigate(
                route = tabRoute,
                popUpToRoute = startDestinationRoute,
                saveState = true,
                restoreState = true,
                isSingleTop = true,
            )
        }
    }
}

// И в Composable с BottomBar:
NavigationBar {
    bottomBarTabs.sortedBy { it.order }.forEach { tab ->
        NavigationBarItem(
            selected = tab.route == currentRoute,
            onClick = { selectTab(navigator, currentRoute, tab.route, START_DESTINATION) },
            icon = { Icon(tab.icon, contentDescription = tab.title) },
            label = { Text(tab.title) },
        )
    }
}
```

Что делает каждая ветка:

| Случай | Поведение |
|---|---|
| Клик по текущему табу | No-op (не дёргает back stack без надобности). |
| Клик по start-destination табу откуда угодно | `popBackStackTo(startRoute)` - снимает все экраны выше него одним вызовом. |
| Клик по любому другому табу | Обычный `navigate` с `saveState = true` + `restoreState = true` - сохраняет/восстанавливает scroll, форму и т.д. |

Встроенный `Navigator.selectTab(...)` хелпер, возможно, появится в 0.2.0 по
фидбэку. В 0.1.0 этот паттерн живёт в твоём `:app`.

Полный рабочий пример - в
[`sample/app/MainScreen.kt`](sample/app/src/main/java/io/github/chisarabivorts/tessera/sample/MainScreen.kt).

---

## 9. Sample-приложения

Два параллельных sample-апа покрывают оба основных паттерна навигации.

### `sample/app` - single-NavHost

Один `NavController` на всё приложение, табы делят общий back stack
(через `saveState` / `restoreState`). Демонстрирует:

- Базовый `FeatureEntry` (Home), `FeatureEntry` с аргументом (`detail/{id}`)
- Возврат результатов между экранами (`ResultNavigator`)
- `DialogFeatureEntry` (Settings → демо-диалог)
- `NestedFeatureEntry` (многошаговый Checkout flow)
- Deep link с обработкой cold-start и warm-start
- Подключение через Hilt multibinding

```bash
./gradlew :sample:app:installDebug
adb shell am start -n io.github.chisarabivorts.tessera.sample/.MainActivity

# Cold-start deep link:
adb shell am start -W -a android.intent.action.VIEW -d "tessera://sample/detail/42"
```

### `sample/app-multitab` - multi-NavHost

Root `NavController` над таб-маршрутами, в каждом табе свой nested
`NavController` со **своим** независимым back stack. Демонстрирует:

- `TabFeatureEntry` + `rememberMultitabState` - канонический рецепт
- Три таба (Главная, Настройки, Оформление), сохраняющие per-tab состояние при
  переключении
- Сборку табов через Hilt из глобального `Set<FeatureEntry>` multibinding'а
- Автоматические мосты от `Navigator` и `TabDeeplinkNavigator` к нужному
  контроллеру через state holder

```bash
./gradlew :sample:app-multitab:installDebug
adb shell am start -n io.github.chisarabivorts.tessera.sample.multitab/.MultitabActivity
```

---

## 10. Что есть и чего нет в 0.1.0

| Функционал | Статус |
|---|---|
| Модульная регистрация через `FeatureEntry` | ✅ |
| Вложенные графы (`NestedFeatureEntry`) | ✅ |
| Модальные диалоги (`DialogFeatureEntry`) | ✅ |
| Аргументы, deep links, анимации на экран | ✅ |
| `Navigator` + `ResultNavigator` для inter-screen коммуникации | ✅ |
| `TabDeeplinkNavigator` для multi-tab приложений | ✅ (см. `sample/app-multitab`) |
| Multi-NavHost через `MultitabState` | ✅ (см. `sample/app-multitab`) |
| Кросс-таб навигация через `Navigator.switchToTab(route)` | ✅ |
| Hilt-интеграция (`tessera-hilt`) | ✅ |
| No-DI-фабрика (`Tessera.createNavigator()`) | ✅ |
| Cold-start и warm-start deep link | ✅ (см. sample) |
| Type-safe маршруты через `@Serializable` | ❌ Запланировано на 0.2.0 |
| Koin-интеграция (`tessera-koin`) | ❌ Запланировано на 0.2.0 |
| Kotlin Multiplatform | ❌ Не планируется |
| Codegen (`@FeatureEntry` annotation processor) | ❌ Не планируется |

---

## 11. Сравнение с другими подходами

Честное сравнение - не с другими навигационными движками, а с паттерном
extension-функций на `NavGraphBuilder` из Now in Android, которым реально
пользуется большинство проектов.

|   | Plain `NavGraphBuilder` extensions | Tessera | Compose Destinations | Decompose | Voyager |
|---|---|---|---|---|---|
| Базируется на Jetpack Navigation Compose | ✅ | ✅ | ✅ | ❌ свой движок | ❌ свой движок |
| Без внешних зависимостей | ✅ | ❌ (эта библиотека) | ❌ KSP-проход | ❌ | ❌ |
| Host не вызывает фичи по имени | ❌ host пишет `homeScreen()` и т.д. | ✅ multibinding итерация | ❌ требует KSP в host | ✅ | ⚠️ через ScreenRegistry |
| Стандартизированная передача результатов без бага redelivery `savedStateHandle` | ❌ роли свой | ✅ `ResultNavigator` | ❌ | ✅ через свою state-модель | ❌ |
| Готовый рецепт multi-NavHost / per-tab back stack | ❌ роли свой | ✅ `MultitabState` | ❌ | ✅ через свою state-модель | ⚠️ |
| Type-safe маршруты | ⚠️ через NavType | ❌ план 0.2 | ✅ | ✅ | ✅ |
| Диалоги как полноценные destinations | ✅ | ✅ | ✅ | вручную | ✅ |
| KMP | ❌ | ❌ | ❌ | ✅ | ✅ |
| Требует codegen | ❌ | ❌ | ✅ | ❌ | ❌ |

Ниша Tессеры: ты **уже** используешь Jetpack Navigation Compose, **уже** хочешь
модульность, не готов переходить на другой движок или жить с обязательной
кодогенерацией, **и** уже ощутил боль стандартизации передачи результатов между
фичами или ручной сборки multi-`NavHost` layout. Если эти две боли тебя не
коснулись - обычные extension-функции `NavGraphBuilder` правильнее в качестве
старта.

---

## 12. Какие приложения можно строить

Чтобы было понятно, на что Tessera ложится естественно.

### 📰 Новостной ридер

```
:app
:feature-feed       - лента статей (FeatureEntryWithBottomBar)
:feature-article    - экран статьи (route "article/{id}", deepLinks)
:feature-saved      - сохранённые (FeatureEntryWithBottomBar)
:feature-search     - поиск (FeatureEntryWithBottomBar)
:feature-settings   - настройки
:feature-paywall    - DialogFeatureEntry для подписки
```
Паттерн: **single-NavHost**, простая навигация по статьям, диалоги для модалок, deep link'и из push-уведомлений на конкретные статьи.

### 🛒 E-commerce

```
:feature-catalog       - каталог + категории
:feature-product       - детали товара (с аргументом id)
:feature-cart          - корзина
:feature-checkout      - NestedFeatureEntry: address → payment → confirm
:feature-profile       - профиль (FeatureEntryWithBottomBar)
:feature-orders        - история заказов
```
Паттерн: **single** или **multi**. Checkout - отличный кейс для `NestedFeatureEntry` (`popBackStackTo("home")` в конце выходит из всего флоу). Возврат результата из address → confirm через `ResultNavigator`.

### 💬 Messenger

```
:feature-chats         - список чатов (FeatureEntryWithBottomBar)
:feature-chat          - экран чата (route "chat/{id}")
:feature-calls         - звонки (FeatureEntryWithBottomBar)
:feature-contacts      - контакты (FeatureEntryWithBottomBar)
:feature-profile       - настройки (FeatureEntryWithBottomBar)
:feature-attachment    - вложения (Dialog)
```
Паттерн: **multi-NavHost** обязательно. История переходов внутри табов Chats и Contacts должна выживать. Cross-tab deep link от push-уведомления: `tabDeeplinkNavigator.deepLinkToTab(myapp://chat/42)` - открывает конкретный чат внутри таба Chats.

### 🏦 Банк-клиент

```
:feature-accounts          - счета и карты (FeatureEntryWithBottomBar)
:feature-account-detail    - детали счёта + транзакции
:feature-transfer          - NestedFeatureEntry: пайплайн перевода
                             (recipient → amount → confirm → success)
:feature-cards             - управление картами (FeatureEntryWithBottomBar)
:feature-history           - история (FeatureEntryWithBottomBar)
:feature-settings          - настройки
:feature-2fa-dialog        - DialogFeatureEntry для подтверждения
```
Паттерн: **single** обычно достаточно (банковский UX не требует Instagram-style stacks). Многошаговые переводы - идеальный `NestedFeatureEntry`. 2FA-подтверждение - `DialogFeatureEntry`.

### 🎵 Музыкальный плеер

```
:feature-library       - библиотека (FeatureEntryWithBottomBar)
:feature-search        - поиск (FeatureEntryWithBottomBar)
:feature-player        - экран плеера (либо bottom sheet, либо отдельный экран)
:feature-playlist      - детали плейлиста (route "playlist/{id}")
:feature-artist        - артист (route "artist/{id}")
:feature-album         - альбом (route "album/{id}")
```
Паттерн: **multi-NavHost**. Пользователь привык: листал альбомы → переключился на поиск → вернулся → на том же альбоме.

### 📱 Onboarding

```
:feature-onboarding    - NestedFeatureEntry:
                          welcome → permissions → profile → done
:app                   - открывает onboarding при первом запуске
                         navigator.navigate("onboarding", popUpTo="home", inclusive=true)
                         в конце: navigator.popBackStackTo("home", inclusive=true) + navigate("home")
```
Один `NestedFeatureEntry` на весь onboarding flow.

### 🧪 Settings-heavy приложение

```
:feature-settings-root         - главное меню настроек
:feature-settings-account
:feature-settings-privacy
:feature-settings-notifications
:feature-settings-language     - Dialog (выбор из списка)
:feature-settings-theme        - Dialog
:feature-settings-about        - обычный экран
```
Паттерн: **single-NavHost**, много `DialogFeatureEntry` для модальных выборов.

---

## 13. Что НЕ покрыто библиотекой

Чтобы избежать неправильных ожиданий - что Tessera **не** делает:

| Не покрыто | Альтернатива |
|---|---|
| **Type-safe `@Serializable` маршруты** | Строки в 0.1.x. Запланировано в 0.2.0. Пока - string routes как в обычной Jetpack Navigation |
| **Кодогенерация / KSP** | Не в этой библиотеке. Используй [Compose Destinations](https://github.com/raamcosta/compose-destinations) если KSP нужен |
| **KMP (multiplatform)** | Только Android. Под KMP - [Decompose](https://github.com/arkivanov/Decompose) |
| **DI-фреймворк** | Tessera агностична. Hilt - рекомендуется (`tessera-hilt`). Koin - в roadmap (`tessera-koin`). Без DI - `Tessera.createNavigator()` |
| **UI для bottom bar / nav rail / drawer** | Маркер `FeatureEntryWithBottomBar` даёт **данные** (title, icon, order). Рисуешь сам Compose |
| **Глобальный `TransitionConfig`** | Roadmap 0.2.0+. Сейчас - каждая фича сама |
| **Shared element transitions** | В alpha в Jetpack Navigation; пока не интегрировано |
| **ViewModel scope-routing** | Используешь стандартный `hilt.navigation.compose` `hiltViewModel()` внутри `Content(...)` |
| **Сохранение состояния form/scroll при гибели процесса** | Это ответственность экрана через `rememberSaveable` / `ViewModel`+`SavedStateHandle`. Tessera не вмешивается |

---

## 14. Дорожная карта

**0.2.0** (запланировано)

- Type-safe маршруты через `@Serializable` data class (фича Navigation Compose 2.8+)
- Артефакт `tessera-koin` для пользователей Koin
- Опциональный `TransitionConfig` для app-wide дефолтных анимаций

**1.0.0** (когда-то)

- Стабильный публичный API (никаких breaking changes между 0.5+ и 1.0)
- Публикация в Maven Central
- 80%+ покрытие тестами + JaCoCo-отчёт

---

## 15. Требования

- Kotlin 2.0+
- AGP 8.6+
- minSdk 24, compileSdk 35+
- Подключённый Compose Compiler-плагин

---

## 16. Лицензия

```
Copyright 2026 Sergey Strekalov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

Полный текст - в [LICENSE](LICENSE).

---

## Что дальше

- **Сэмплы** - [`sample/app/`](sample/app/) (single-NavHost, 4 фичи) и [`sample/app-multitab/`](sample/app-multitab/) (multi-NavHost, 3 таба)
- **README** - [`README.md`](README.md) / [`README.ru.md`](README.ru.md) - короткий обзор «зачем нужна» и ссылка сюда
- **Issues** - [GitHub Issues](https://github.com/chisarabiVorts/tessera/issues) для багов и feature requests

---

Если что-то осталось непонятно - открой issue или сделай PR с улучшением этого гайда. Tessera ещё в 0.1.x и live-документ.
