# Branching rules - Tessera

Правила работы с ветками и история коммитов в этом репозитории.
Цель - линейная, читаемая история и предсказуемый процесс релизов.

---

## TL;DR

- `master` - только релизные коммиты + тэги. Никаких прямых пушей.
- `develop` - интеграционная ветка, сюда вливаются все PR'ы.
- Работа идёт в коротких ветках `feat/...`, `fix/...`, `docs/...` от `develop`.
- Слияние в `develop` - только через PR и только **squash merge** (1 PR = 1 коммит).
- Релиз - PR `develop → master`, затем тэг `v0.x.y` на `master`.

---

## Долгоживущие ветки

| Ветка | Назначение | Кто пишет | Force-push |
|---|---|---|---|
| `master` | Стабильные релизы. Каждый коммит здесь = реальный релиз с тэгом. CI badge привязан сюда. | Только через PR из `develop` или `hotfix/*`. | Запрещён. |
| `develop` | Интеграция фичей. Может быть нестабильным между релизами, но CI должен быть зелёным. | Только через PR из feature-веток. | Запрещён. |

Любая работа начинается с **актуального** `develop`:

```bash
git checkout develop
git pull --ff-only
git checkout -b feat/...
```

---

## Именование feature-веток

Формат: `<type>/<short-kebab-case-description>`.
Описание - на английском, 2-5 слов через дефис.

| Префикс | Когда использовать | Пример |
|---|---|---|
| `feat/` | Новая функциональность | `feat/serializable-routes` |
| `fix/` | Исправление бага | `fix/popbackstack-iae` |
| `docs/` | Только документация | `docs/guide-section-on-deeplinks` |
| `refactor/` | Рефакторинг без изменения поведения | `refactor/extract-multitab-state-holder` |
| `test/` | Только тесты | `test/result-navigator-regression` |
| `chore/` | Build, deps, CI, gradle | `chore/bump-agp-to-8.7` |
| `perf/` | Оптимизация без новой функциональности | `perf/avoid-extra-recomposition` |
| `hotfix/` | Срочный фикс с `master`, минуя `develop` | `hotfix/release-yml-broken-tag` |
| `release/` | Подготовка релиза (опционально) | `release/0.2.0` |

**Запрещены:** ветки без префикса, ветки с пробелами или CamelCase, длинные ветки (`feat/my-very-long-branch-name-explaining-everything`).

> Можно прописать regex в GitHub Rulesets, чтобы запрет был автоматическим:
> ```
> ^(master|develop|(feat|fix|docs|refactor|test|chore|perf|hotfix|release)/[a-z0-9][a-z0-9-]*)$
> ```

---

## Workflow: добавить фичу или фикс

```bash
# 1. Обновить develop
git checkout develop
git pull --ff-only

# 2. Создать ветку
git checkout -b feat/serializable-routes

# 3. Работать; коммиты могут быть любого качества - squash их объединит.
git add ...
git commit -m "wip: parse @Serializable route"
git commit -m "wip: tests"
git commit -m "wip: docs"

# 4. Пушнуть и открыть PR в develop
git push -u origin feat/serializable-routes
# GitHub: New pull request, base = develop, compare = feat/serializable-routes

# 5. Дождаться зелёного CI (Test & Build).

# 6. Squash merge через GitHub UI.
#    Заголовок PR станет единственным коммитом в develop -
#    форматируй его по Conventional Commits (см. ниже).

# 7. Удалить ветку (GitHub предложит кнопку "Delete branch").
git checkout develop
git pull --ff-only
git branch -d feat/serializable-routes
```

---

## Формат заголовка PR (= squash commit message)

Используем [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>
```

- **type** - совпадает с префиксом ветки: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, `revert`.
- **scope** (опционально, но желательно) - затронутая область: `navigator`, `multitab`, `hilt`, `samples`, `ci`, `gradle`, `result-navigator`, `nested-entry`.
- **subject** - императив, lowercase, без точки в конце, <70 символов.

Примеры:

```
feat(routes): support @Serializable type-safe routes
fix(navigator): swallow IAE on missing popUpTo route
fix(multitab): preserve back stack across DESTROYED lifecycle
docs(guide): clarify cold-start deep link recipe
refactor(internal): extract NavController bridge into helper
test(result-navigator): regression for concurrent publish under one key
chore(deps): bump kotlin to 2.0.21
ci: cover :tessera-hilt in test workflow
```

Тело сообщения (необязательно) - пиши в **описании PR**, при squash merge GitHub его склеит.

---

## Релиз `develop → master`

Когда `develop` накопил достаточно для релиза:

1. Открыть PR `develop → master`. Title:
   ```
   release: v0.2.0
   ```
2. В описание PR - копия раздела CHANGELOG для этой версии.
3. Дождаться зелёного CI.
4. **Squash merge.**
5. Локально получить новый master + тэгнуть:
   ```bash
   git checkout master
   git pull --ff-only
   git tag -a v0.2.0 -m "Tessera 0.2.0"
   git push origin v0.2.0
   ```
6. `release.yml` сам соберёт AAR и создаст GitHub Release.

После релиза синхронизировать `develop` с `master` (быстро вернуть squash-коммит):

```bash
git checkout develop
git pull --ff-only
git merge --ff-only master   # или новый PR master → develop, как удобнее
git push
```

---

## Hotfix-процесс

Когда нужно срочно починить уже выпущенную версию, минуя весь `develop`:

```bash
# Ветка от master, не от develop
git checkout master
git pull --ff-only
git checkout -b hotfix/release-yml-broken-tag

# Работать, пушнуть, открыть PR в master
# После merge в master - обязательно мерж master → develop, чтобы фикс не потерялся
```

Тэг сразу после merge: `v0.2.1`.

---

## GitHub-side правила (Settings → Branches / Rulesets)

Эти правила воспроизводят соглашения автоматически.

### Branch protection - `master`

- ✅ Require a pull request before merging
- ✅ Require approvals: `0` для solo, `1` если появятся внешние контрибьюторы
- ✅ Require status checks to pass - добавить job `Test & Build` из `ci.yml`
- ✅ Require branches to be up to date before merging
- ✅ Require linear history (запретит обычные merge commits)
- ✅ Do not allow bypassing the above settings
- ❌ Allow force pushes - выключить
- ❌ Allow deletions - выключить

### Branch protection - `develop`

То же, что `master`, кроме:

- Approvals можно не требовать (solo)
- Allow force pushes - выключить
- Require linear history - желательно

### Repository merge settings

Settings → General → Pull Requests:

- ❌ Allow merge commits
- ✅ Allow squash merging - **дефолт**
- ❌ Allow rebase merging

Это уберёт две из трёх кнопок в UI PR'а - останется только Squash.

Default commit message для squash → `Pull request title and description`.

### Branch naming ruleset (опционально, но рекомендую)

Settings → Rules → New ruleset → Branch ruleset:

- Target: All branches except `master` and `develop`
- Restrict creations: ✅
- Branch name pattern (regex):
  ```
  ^(feat|fix|docs|refactor|test|chore|perf|hotfix|release)/[a-z0-9][a-z0-9-]*$
  ```

Любая ветка не по схеме не создастся.

---

## Быстрая шпаргалка

```bash
# Завести фичу
git checkout develop && git pull --ff-only
git checkout -b feat/some-thing

# Закончить - push и PR в develop
git push -u origin feat/some-thing

# После merge - почистить
git checkout develop && git pull --ff-only
git branch -d feat/some-thing

# Свежий локальный master после релиза
git checkout master && git pull --ff-only
git tag -a v0.2.0 -m "Tessera 0.2.0"
git push origin v0.2.0

# Hotfix
git checkout master && git pull --ff-only
git checkout -b hotfix/short-desc
# ...PR в master, потом merge master → develop
```

---

## История коммитов в `master` - как должна выглядеть

После полугода работы:

```
v0.3.0  release: v0.3.0
v0.2.1  fix(navigator): swallow IAE on missing popUpTo route
v0.2.0  release: v0.2.0
v0.1.0  Initial release: Tessera 0.1.0
```

Каждый коммит = либо релиз (`release: v...`), либо hotfix. Между ними промежуточных коммитов нет. История читается за 5 секунд.

История `develop` будет плотнее - по одному squash-коммиту на PR - но тоже линейна, без merge-bubbles.
