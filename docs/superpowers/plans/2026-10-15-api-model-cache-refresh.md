# API Model Cache and Per-Site Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Make the API quick-model popup open immediately from persisted model caches and let users refresh one API site at a time from the popup or settings page.

**Architecture:** Add a small pure `ApiModelCache` component that builds and safely applies successful cache updates. Keep network access in existing IntelliJ background tasks, persist the cache and last-success time on `ApiProviderConfig`, and let each UI surface reuse the same update rules. Remove the current all-sites refresh from `QuickModelAction` and replace its global lock with per-site refresh tracking.

**Tech Stack:** Kotlin 1.8, IntelliJ Platform 2022.2 Swing/action system, JDK 17 `HttpClient`, JUnit 4, Gradle IntelliJ Plugin.

**Repository rule:** Do not run `git add`, `git commit`, `git push`, or `git merge` unless the user separately authorizes that exact operation. The checkpoints below end with tests rather than Git writes.

---

## File map

- Create `src/main/kotlin/com/fs/aicommit/ApiModelCache.kt`: pure cache normalization, identity checking, and update application.
- Create `src/test/kotlin/com/fs/aicommit/ApiModelCacheTest.kt`: cache success, empty response, stale-result, and timestamp tests.
- Modify `src/main/kotlin/com/fs/aicommit/ApiProviderConfig.kt`: persist `modelsFetchedAt` and preserve it through deep copies.
- Modify `src/test/kotlin/com/fs/aicommit/ApiProviderConfigTest.kt`: old-state and deep-copy regression coverage.
- Modify `src/main/kotlin/com/fs/aicommit/ApiConfigDialog.kt`: apply successful fetches with replacement semantics and record success time.
- Modify `src/test/kotlin/com/fs/aicommit/ApiConfigDialogModelTest.kt`: replace the old permanent-merge expectation with current-model-plus-latest-fetch behavior.
- Modify `src/main/kotlin/com/fs/aicommit/ApiConfigurationModel.kt`: apply a cache update to the settings working copy.
- Modify `src/test/kotlin/com/fs/aicommit/ApiConfigurationModelTest.kt`: verify one-site updates and stale updates.
- Modify `src/main/kotlin/com/fs/aicommit/QuickModelAction.kt`: remove automatic all-site loading and add per-site refresh actions/status.
- Create `src/main/kotlin/com/fs/aicommit/QuickModelPopupSupport.kt`: keep the nested action popup open and replace its visible site actions in place.
- Modify `src/test/kotlin/com/fs/aicommit/QuickModelActionTest.kt`: verify cached-only choices and per-site refresh gating.
- Modify `src/main/kotlin/com/fs/aicommit/ApiModePanel.kt`: add “refresh selected site” UI and background task.
- Modify `src/main/resources/messages/SubMsgBundle_zh.properties`: Chinese cache and refresh copy.
- Modify `src/main/resources/messages/SubMsgBundle.properties`: English cache and refresh copy.
- Modify `src/test/kotlin/com/fs/i18n/SubMsgBundleTest.kt`: keep resource-key parity enforced.

### Task 1: Persisted cache data and pure update rules

**Files:**
- Create: `src/main/kotlin/com/fs/aicommit/ApiModelCache.kt`
- Create: `src/test/kotlin/com/fs/aicommit/ApiModelCacheTest.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/ApiProviderConfig.kt`
- Modify: `src/test/kotlin/com/fs/aicommit/ApiProviderConfigTest.kt`

- [x] **Step 1: Write failing cache-rule tests**

Create tests with fixed timestamps so they do not depend on the system clock:

```kotlin
class ApiModelCacheTest {
    @Test
    fun `successful refresh replaces stale cache but preserves current model`() {
        val config = ApiProviderConfig(
            id = "one", provider = "openai", baseUrl = "https://one.example/v1",
            model = "manual", models = mutableListOf("manual", "stale"), modelsFetchedAt = 10L
        )

        val update = ApiModelCache.createUpdate(config, listOf(" remote-a ", "manual", "remote-b"), 20L)!!
        assertTrue(ApiModelCache.apply(listOf(config), update))
        assertEquals(listOf("manual", "remote-a", "remote-b"), config.models)
        assertEquals(20L, config.modelsFetchedAt)
    }

    @Test
    fun `empty refresh produces no update and keeps prior cache`() {
        val config = ApiProviderConfig(model = "current", models = mutableListOf("current", "cached"), modelsFetchedAt = 10L)
        assertNull(ApiModelCache.createUpdate(config, listOf(" ", ""), 20L))
        assertEquals(listOf("current", "cached"), config.models)
        assertEquals(10L, config.modelsFetchedAt)
    }

    @Test
    fun `stale refresh is discarded when endpoint identity changes`() {
        val requested = ApiProviderConfig(id = "one", provider = "openai", baseUrl = "https://old.example/v1", model = "m1")
        val update = ApiModelCache.createUpdate(requested, listOf("m2"), 20L)!!
        val current = requested.deepCopy().copy(baseUrl = "https://new.example/v1")
        assertFalse(ApiModelCache.apply(listOf(current), update))
        assertEquals(listOf("m1"), current.availableModels())
        assertEquals(0L, current.modelsFetchedAt)
    }
}
```

Extend `ApiProviderConfigTest` to assert that `modelsFetchedAt` defaults to `0`, is preserved by `deepCopy`, and does not share the mutable `models` list.

- [x] **Step 2: Run the focused tests and verify RED**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
& 'D:\gradle-7.4.2\bin\gradle.bat' test --tests com.fs.aicommit.ApiModelCacheTest --tests com.fs.aicommit.ApiProviderConfigTest --rerun-tasks --console=plain
```

Expected: compilation fails because `ApiModelCache` and `modelsFetchedAt` do not exist.

- [x] **Step 3: Add the persisted field and minimal pure cache component**

Add this property after `models`:

```kotlin
var modelsFetchedAt: Long = 0L,
```

Implement focused cache types:

```kotlin
internal data class ApiModelCacheIdentity(
    val id: String,
    val provider: String,
    val baseUrl: String,
)

internal data class ApiModelCacheUpdate(
    val identity: ApiModelCacheIdentity,
    val remoteModels: List<String>,
    val fetchedAt: Long,
)

internal object ApiModelCache {
    fun createUpdate(config: ApiProviderConfig, fetched: List<String>, fetchedAt: Long): ApiModelCacheUpdate? {
        val remote = fetched.asSequence().map(String::trim).filter(String::isNotBlank).distinct().toList()
        if (remote.isEmpty()) return null
        return ApiModelCacheUpdate(identityOf(config), remote, fetchedAt)
    }

    fun apply(configs: List<ApiProviderConfig>, update: ApiModelCacheUpdate): Boolean {
        val target = configs.firstOrNull { identityOf(it) == update.identity } ?: return false
        target.models = sequenceOf(target.model.trim())
            .plus(update.remoteModels.asSequence())
            .filter(String::isNotBlank)
            .distinct()
            .toMutableList()
        target.modelsFetchedAt = update.fetchedAt
        return true
    }

    private fun identityOf(config: ApiProviderConfig) = ApiModelCacheIdentity(
        config.id, config.provider.trim(), config.baseUrl.trim()
    )
}
```

- [x] **Step 4: Run the focused tests and verify GREEN**

Run the command from Step 2. Expected: both test classes pass.

### Task 2: Reuse replacement semantics in dialogs, working copies, and transfer

**Files:**
- Modify: `src/main/kotlin/com/fs/aicommit/ApiConfigDialog.kt`
- Modify: `src/test/kotlin/com/fs/aicommit/ApiConfigDialogModelTest.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/ApiConfigurationModel.kt`
- Modify: `src/test/kotlin/com/fs/aicommit/ApiConfigurationModelTest.kt`
- Modify: `src/test/kotlin/com/fs/aicommit/ApiSiteTransferTest.kt`

- [x] **Step 1: Write failing integration tests around the pure cache update**

Replace the old “fetched and saved values accumulate forever” dialog test with:

```kotlin
@Test
fun `successful fetched models replace stale saved models while preserving current`() {
    assertEquals(
        listOf("current", "remote-a"),
        ApiConfigDialog.modelsAfterSuccessfulFetch("current", listOf("remote-a", "current"))
    )
}
```

Add working-copy coverage:

```kotlin
@Test
fun `model cache update changes only its matching working configuration`() {
    val model = ApiConfigurationModel(MemoryCredentials())
    model.put(ApiProviderConfig(id = "one", name = "One", baseUrl = "https://one.example/v1", model = "m1"))
    model.put(ApiProviderConfig(id = "two", name = "Two", baseUrl = "https://two.example/v1", model = "x1"))
    val update = ApiModelCache.createUpdate(model.configs.first(), listOf("m2"), 20L)!!
    assertTrue(model.applyModelCache(update))
    assertEquals(listOf("m1", "m2"), model.configs.first().models)
    assertEquals(listOf("x1"), model.configs.last().availableModels())
}
```

Extend `ApiSiteTransferTest` to assert exported JSON has no `modelsFetchedAt` property and decoded configs have `modelsFetchedAt == 0L`.

- [x] **Step 2: Run focused tests and verify RED**

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test --tests com.fs.aicommit.ApiConfigDialogModelTest --tests com.fs.aicommit.ApiConfigurationModelTest --tests com.fs.aicommit.ApiSiteTransferTest --rerun-tasks --console=plain
```

Expected: failure because `modelsAfterSuccessfulFetch` and `applyModelCache` are absent.

- [x] **Step 3: Implement dialog and working-copy integration**

In `ApiConfigurationModel` add:

```kotlin
internal fun applyModelCache(update: ApiModelCacheUpdate): Boolean =
    ApiModelCache.apply(configs, update)
```

In `ApiConfigDialog`, replace permanent merging with a local cached list and success timestamp:

```kotlin
private var cachedModels = original.availableModels()
private var modelsFetchedAt = original.modelsFetchedAt

internal fun modelsAfterSuccessfulFetch(current: String, fetched: List<String>): List<String> =
    sequenceOf(current).plus(fetched.asSequence())
        .map(String::trim).filter(String::isNotBlank).distinct().toList()
```

On a non-empty successful fetch, update `cachedModels`, set `modelsFetchedAt = System.currentTimeMillis()`, and rebuild the combo box. In `config()`, persist the current editable model plus `cachedModels`, and persist `modelsFetchedAt`. If the response is empty, keep the old list/time and show `message.model.retained`.

- [x] **Step 4: Run focused tests and verify GREEN**

Run the command from Step 2. Expected: all three classes pass.

### Task 3: Make the quick popup cached-first and refresh one site

**Files:**
- Modify: `src/main/kotlin/com/fs/aicommit/QuickModelAction.kt`
- Modify: `src/test/kotlin/com/fs/aicommit/QuickModelActionTest.kt`
- Create: `src/test/kotlin/com/fs/aicommit/QuickModelPopupIntegrationTest.kt`

- [x] **Step 1: Write failing tests for cached choices and refresh coordination**

Remove the test that expects every site to be requested. Add tests for the new contract:

```kotlin
@Test
fun `API quick choices use saved cache without a refresh step`() {
    val settings = AiCommitSettings().apply {
        mode = AiCommitSettings.MODE_API
        apiConfigs = mutableListOf(ApiProviderConfig(
            id = "one", name = "One", model = "m1", models = mutableListOf("m1", "cached")
        ))
        activeApiConfigId = "one"
    }
    assertEquals(listOf("m1", "cached"), QuickModelAction.apiSites(settings).single().choices.map { it.model })
}

@Test
fun `refresh gate blocks only duplicate requests for the same site`() {
    assertTrue(QuickModelAction.beginApiRefresh("one"))
    assertFalse(QuickModelAction.beginApiRefresh("one"))
    assertTrue(QuickModelAction.beginApiRefresh("two"))
    QuickModelAction.endApiRefresh("one")
    QuickModelAction.endApiRefresh("two")
}
```

Add a test that a successful update for site `one` leaves site `two` unchanged; reuse `ApiModelCache.createUpdate` instead of an all-site loader.

- [x] **Step 2: Run the quick-action tests and verify RED**

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test --tests com.fs.aicommit.QuickModelActionTest --rerun-tasks --console=plain
```

Expected: failure because per-site gate methods do not exist and old refresh behavior still exists.

- [x] **Step 3: Remove automatic all-site loading**

Delete `ApiRefreshResult`, `refreshApiModels`, `loadApiModels`, `applyApiModels`, the `refreshApi` argument, and the initial background-refresh branch in `showPopup`. Keep `apiSites(settings)` as the only source for API menu choices.

Replace the global `AtomicBoolean` with a concurrent site-ID set:

```kotlin
private val apiRefreshRunning = ConcurrentHashMap.newKeySet<String>()

internal fun beginApiRefresh(configId: String): Boolean = apiRefreshRunning.add(configId)
internal fun endApiRefresh(configId: String) { apiRefreshRunning.remove(configId) }
internal fun isApiRefreshRunning(configId: String): Boolean = configId in apiRefreshRunning
```

- [x] **Step 4: Add a refresh action inside every site submenu**

For each `ApiSiteChoices`, add an `AnAction` before the model choices:

```kotlin
siteGroup.add(object : AnAction(
    if (isApiRefreshRunning(site.configId)) tr("action.quick.model.refreshing") else tr("action.quick.model.refresh.site"),
    null,
    AllIcons.Actions.Refresh
), DumbAware {
    override fun actionPerformed(event: AnActionEvent) {
        refreshApiSite(project, anchor, site.configId, afterFailure)
    }
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = !isApiRefreshRunning(site.configId)
    }
})
siteGroup.addSeparator()
```

Implement `refreshApiSite` as one `Task.Backgroundable`: resolve credentials from a deep copy, fetch only that configuration, create an `ApiModelCacheUpdate`, apply it only if identity still matches, record a short per-site success/retained/failure message, and release the gate in every completion path.

Append a disabled cache-status item after the models. Format known times as `MM-dd HH:mm`; unknown times use the localized “time unknown” message.

- [x] **Step 5: Run the quick-action tests and verify GREEN**

Run the command from Step 2. Expected: `QuickModelActionTest` passes without making network calls.

### Task 4: Add selected-site refresh to API settings and finish UI copy

**Files:**
- Modify: `src/main/kotlin/com/fs/aicommit/ApiModePanel.kt`
- Modify: `src/main/resources/messages/SubMsgBundle_zh.properties`
- Modify: `src/main/resources/messages/SubMsgBundle.properties`
- Modify: `src/test/kotlin/com/fs/i18n/SubMsgBundleTest.kt`

- [x] **Step 1: Add the Chinese keys first so the parity test detects the missing English side**

The existing parity test reads both bundles. Add UI code references only after defining matching Chinese and English keys:

```properties
# Chinese
aicommit.api.refresh.models=刷新模型
action.quick.model.refresh.site=重新获取此站点模型
action.quick.model.refreshing=正在获取模型…
action.quick.model.cache=缓存 {0} 个 · 上次成功：{1}
action.quick.model.cache.unknown=缓存 {0} 个 · 获取时间未知
action.quick.model.cache.empty=暂无缓存模型
message.model.refreshing.site=正在获取 {0} 的模型列表…
message.model.refreshed.site={0}：已缓存 {1} 个模型
message.model.failed.retained=获取失败，已保留上次缓存：{0}
```

- [x] **Step 2: Run the bundle test and verify RED, then add the complete English set**

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test --tests com.fs.i18n.SubMsgBundleTest --rerun-tasks --console=plain
```

Expected after adding only the Chinese keys: key-parity failure.

Then add the exact English keys:

```properties
aicommit.api.refresh.models=Refresh Models
action.quick.model.refresh.site=Refresh Models for This Site
action.quick.model.refreshing=Refreshing models…
action.quick.model.cache={0} cached · Last success: {1}
action.quick.model.cache.unknown={0} cached · Fetch time unknown
action.quick.model.cache.empty=No cached models
message.model.refreshing.site=Fetching models for {0}…
message.model.refreshed.site={0}: cached {1} models
message.model.failed.retained=Fetch failed; previous cache retained: {0}
```

Run the same test again. Expected: pass.

- [x] **Step 3: Add the settings toolbar button and background refresh**

Add a text/icon button after “Test” and before the import/export separator:

```kotlin
private val refreshModelsButton = JButton(tr("aicommit.api.refresh.models"), AllIcons.Actions.Refresh)
```

Enable it only when a row is selected and no settings-page refresh is running. Its handler must:

1. Deep-copy the selected config.
2. Call `ApiClient().fetchModels(snapshot)` in `Task.Backgroundable` or the existing cancellable task style.
3. Create an update with `ApiModelCache.createUpdate`.
4. Apply through `model.applyModelCache(update)`.
5. Refresh the table and show site-specific success status.
6. On empty, cancellation, or failure, retain cache/time and show the localized retained status.

Add a table selection listener so button enabled state follows the selected row.

- [x] **Step 4: Run focused tests and compile the Swing code**

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test --tests com.fs.aicommit.ApiConfigurationModelTest --tests com.fs.i18n.SubMsgBundleTest compileKotlin --rerun-tasks --console=plain
```

Expected: tests pass and Kotlin compilation succeeds against IntelliJ 2022.2.5 APIs.

### Task 5: Full regression and Windows plugin build

**Files:**
- Verify all changed production and test files.
- Update task checkboxes in this plan as work completes.

- [x] **Step 1: Run the complete automated suite**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
& 'D:\gradle-7.4.2\bin\gradle.bat' test --rerun-tasks --console=plain
```

Expected: all tests pass with zero failures and zero errors.

- [x] **Step 2: Build the distributable plugin**

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' buildPlugin --rerun-tasks --console=plain
```

Expected: `BUILD SUCCESSFUL` and an updated ZIP under `build/distributions/`.

- [x] **Step 3: Inspect the final diff for scope and secrets**

```powershell
git status --short
git diff -- src/main src/test docs/superpowers/specs docs/superpowers/plans
```

Confirm the diff contains the cache/refresh feature, tests, localization, specification, and plan only. Confirm it contains no API keys or local exported site JSON.

- [x] **Step 4: Report the usable artifact and remaining Git action**

Report the built ZIP path, test result, and the visible interaction changes. Do not stage, commit, or push until the user explicitly authorizes each Git operation.

### Task 6: Keep the open site submenu and replace models in place

**Files:**
- Create: `src/main/kotlin/com/fs/aicommit/QuickModelPopupSupport.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/QuickModelAction.kt`
- Modify: `src/test/kotlin/com/fs/aicommit/QuickModelActionTest.kt`
- Modify: `docs/superpowers/specs/2026-10-15-api-model-cache-refresh-design.md`

- [x] **Step 1: Add failing popup-behavior tests**

Add regression coverage that replacing visible actions removes stale entries, installs the latest list, and synchronizes the popup model. Add an IntelliJ light-platform integration test that executes the refresh item in the nested popup, verifies the popup is not disposed, and then replaces the visible model actions. Also verify the refresh action does not link the removed `KeepingPopupOpenAction` internal interface.

- [x] **Step 2: Verify RED**

Run the focused `QuickModelActionTest`. Expected: compilation fails because the keep-open refresh action and popup item replacement support do not exist.

- [x] **Step 3: Add the native popup adaptation**

Keep the existing IntelliJ Action Popup appearance. Use a dynamic site `ActionGroup` and a small `ListPopupImpl` adaptation for nested menus. The popup identifies `ApiSiteRefreshAction` directly, executes it through the stable Action System callback path, and skips the normal selection path so the current window remains open. Do not implement the removed `KeepingPopupOpenAction` internal interface or call the old `ActionPopupStep.performAction` method. On task completion, rebuild the current site's actions from the latest cache, replace the existing `ActionPopupStep` values, synchronize `ListPopupModel`, and resize the same child popup. Do not call `showPopup` after refresh.

- [x] **Step 4: Verify focused tests**

Run `QuickModelActionTest` and `QuickModelPopupIntegrationTest`, and confirm all tests pass.

- [x] **Step 5: Run full regression, build the plugin, and exercise the popup in the IntelliJ test platform**

Run the complete test suite and `buildPlugin`. Use `QuickModelPopupIntegrationTest` to execute the nested refresh item in an IntelliJ light-platform fixture, verify the child popup is not disposed, and replace the visible model actions in that same popup. The cache tests continue to verify that failures retain the old list and successful refreshes replace it.
