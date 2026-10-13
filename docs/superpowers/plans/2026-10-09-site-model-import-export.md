# Site Model Switching and Import/Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add per-site model lists and nested quick switching with live site model refresh and active-site marking, plus safe JSON import/export of API site configurations with optional API keys.

**Architecture:** Keep `ApiProviderConfig.model` as the selected model and add a persisted `models` list for candidates. Put JSON validation and merge decisions in a pure `ApiSiteTransfer` component, leaving IntelliJ file dialogs and messages in `ApiModePanel`. Build API quick switching as nested `DefaultActionGroup` instances, while retaining the existing flat CLI behavior.

**Tech Stack:** Kotlin 1.8, IntelliJ Platform 2022.2 APIs, Swing, Gson bundled with IntelliJ, JUnit 4, Gradle IntelliJ Plugin 1.13.3, Java 17.

---

The repository instructions prohibit `git add`, `git commit`, `git push`, and `git merge` without separate explicit authorization. This plan therefore contains no Git write steps.

## File map

- Modify `src/main/kotlin/com/fs/aicommit/ApiProviderConfig.kt`: persist and normalize per-site model lists; provide deep copies.
- Modify `src/main/kotlin/com/fs/aicommit/AiCommitSettings.kt`: migrate old single-model configs and deep-copy generation snapshots.
- Modify `src/main/kotlin/com/fs/aicommit/ApiConfigDialog.kt`: retain fetched model candidates when saving a site.
- Create `src/main/kotlin/com/fs/aicommit/ApiSiteTransfer.kt`: versioned JSON encoding, strict decoding, validation, and atomic merge planning.
- Modify `src/main/kotlin/com/fs/aicommit/ApiConfigurationModel.kt`: apply validated imports to the settings working copy without early PasswordSafe writes.
- Modify `src/main/kotlin/com/fs/aicommit/ApiModePanel.kt`: add import/export controls, native file choosers, export options, warnings, and result messages.
- Modify `src/main/kotlin/com/fs/aicommit/QuickModelAction.kt`: create nested site/model groups and switch both site and model.
- Modify `src/main/resources/messages/SubMsgBundle.properties`: add English UI and error strings.
- Modify `src/main/resources/messages/SubMsgBundle_zh.properties`: add matching Chinese UI and error strings.
- Create `src/test/kotlin/com/fs/aicommit/ApiProviderConfigTest.kt`: model normalization and deep-copy regression tests.
- Create `src/test/kotlin/com/fs/aicommit/ApiSiteTransferTest.kt`: export, parse, validation, and merge tests.
- Modify `src/test/kotlin/com/fs/aicommit/AiCommitSettingsTest.kt`: migration and snapshot isolation tests.
- Modify `src/test/kotlin/com/fs/aicommit/ApiConfigurationModelTest.kt`: import staging and credential behavior tests.
- Modify `src/test/kotlin/com/fs/aicommit/QuickModelActionTest.kt`: nested site choices and site/model selection tests.
- Modify `README.md`, `README_CN.md`, `FEATURE_SUMMARY.md`, and `FINAL_SUMMARY.md`: document the delivered behavior and update validation counts.

### Task 1: Persist normalized model lists without breaking old settings

**Files:**
- Modify: `src/main/kotlin/com/fs/aicommit/ApiProviderConfig.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/AiCommitSettings.kt`
- Create: `src/test/kotlin/com/fs/aicommit/ApiProviderConfigTest.kt`
- Modify: `src/test/kotlin/com/fs/aicommit/AiCommitSettingsTest.kt`

- [ ] **Step 1: Write failing normalization and deep-copy tests**

Add tests equivalent to:

```kotlin
@Test
fun `normalization keeps current model first and removes blanks and duplicates`() {
    val config = ApiProviderConfig(
        model = "model-b",
        models = mutableListOf("model-a", "", "model-b", "model-a", "  ")
    )

    config.normalizeModels()

    assertEquals(listOf("model-b", "model-a"), config.models)
}

@Test
fun `deep copy isolates mutable model list`() {
    val source = ApiProviderConfig(model = "one", models = mutableListOf("one", "two"))
    val copy = source.deepCopy()

    source.models.add("three")

    assertEquals(listOf("one", "two"), copy.models)
}
```

Extend `AiCommitSettingsTest` so loading a legacy config with only `model = "legacy"` produces `models == ["legacy"]`, and mutating a source config's `models` after `generationSnapshot()` does not change the snapshot.

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test --tests 'com.fs.aicommit.ApiProviderConfigTest' --tests 'com.fs.aicommit.AiCommitSettingsTest' --no-daemon --console=plain
```

Expected: compilation fails because `models`, `normalizeModels`, or `deepCopy` does not exist.

- [ ] **Step 3: Implement the persisted list and normalization helpers**

Add to `ApiProviderConfig`:

```kotlin
var models: MutableList<String> = mutableListOf()

fun normalizeModels() {
    val current = model.trim()
    val normalized = sequenceOf(current)
        .plus(models.asSequence().map(String::trim))
        .filter(String::isNotBlank)
        .distinct()
        .toMutableList()
    model = current
    models = normalized
}

fun availableModels(): List<String> {
    val copy = deepCopy()
    copy.normalizeModels()
    return copy.models.toList()
}

fun deepCopy(): ApiProviderConfig = copy(models = models.toMutableList())
```

Place `models` after `model` in the data class constructor so IntelliJ XML serialization persists it naturally.

In `AiCommitSettings.loadState`, normalize every config after `XmlSerializerUtil.copyBean`. In `generationSnapshot`, replace `it.copy()` with `it.deepCopy()`.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run the command from Step 2.

Expected: both test classes pass with zero failures.

### Task 2: Retain fetched models in the site editor

**Files:**
- Modify: `src/main/kotlin/com/fs/aicommit/ApiConfigDialog.kt`
- Create: `src/test/kotlin/com/fs/aicommit/ApiConfigDialogModelTest.kt`

- [ ] **Step 1: Extract and test pure model merging**

Create tests for an internal companion function:

```kotlin
@Test
fun `merged models retain current fetched and saved values in stable order`() {
    assertEquals(
        listOf("current", "fetched-a", "saved-a"),
        ApiConfigDialog.mergeModels(
            current = "current",
            fetched = listOf("fetched-a", "current", ""),
            saved = listOf("saved-a", "fetched-a")
        )
    )
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test --tests 'com.fs.aicommit.ApiConfigDialogModelTest' --no-daemon --console=plain
```

Expected: compilation fails because `mergeModels` does not exist.

- [ ] **Step 3: Save fetched and manually entered models**

Add dialog state:

```kotlin
private var fetchedModels: List<String> = emptyList()
```

Build the returned config with:

```kotlin
val currentModel = modelBox.editor.item?.toString().orEmpty().trim()
return original.deepCopy().copy(
    name = nameField.text.trim(),
    provider = providerIds[providerBox.selectedIndex],
    baseUrl = urlField.text.trim(),
    apiKey = String(keyField.password).trim(),
    model = currentModel,
    models = mergeModels(currentModel, fetchedModels, original.models).toMutableList(),
    reasoningCompatible = reasoningBox.isSelected
)
```

Implement `mergeModels` using trim, nonblank filtering, and `distinct()`. On fetch success, assign the returned list to `fetchedModels`, then repopulate the combo while preserving the current editor value.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the command from Step 2.

Expected: the test passes with zero failures.

### Task 3: Add strict versioned JSON transfer logic

**Files:**
- Create: `src/main/kotlin/com/fs/aicommit/ApiSiteTransfer.kt`
- Create: `src/test/kotlin/com/fs/aicommit/ApiSiteTransferTest.kt`

- [ ] **Step 1: Write failing export tests**

Cover both secret modes:

```kotlin
@Test
fun `export omits apiKey when secrets are disabled`() {
    val json = ApiSiteTransfer.encode(
        listOf(ApiProviderConfig(id = "one", name = "One", apiKey = "secret", model = "m", models = mutableListOf("m"))),
        activeId = "one",
        includeApiKeys = false
    )
    assertFalse(JsonParser.parseString(json).asJsonObject.getAsJsonArray("configs")[0]
        .asJsonObject.has("apiKey"))
}

@Test
fun `export includes apiKey only when requested`() {
    val json = ApiSiteTransfer.encode(
        listOf(ApiProviderConfig(id = "one", name = "One", apiKey = "secret", model = "m")),
        activeId = "one",
        includeApiKeys = true
    )
    assertEquals("secret", JsonParser.parseString(json).asJsonObject.getAsJsonArray("configs")[0]
        .asJsonObject.get("apiKey").asString)
}
```

- [ ] **Step 2: Write failing validation and atomic merge tests**

Test these exact cases:

- `format` differs from `generate-commit-message.api-sites`.
- `version` is not `1`.
- `configs` is not an array.
- duplicate nonblank IDs occur in one file.
- URL is invalid.
- a field has the wrong JSON type.
- same ID overwrites nonsecret fields.
- missing `apiKey` preserves the existing key.
- nonblank imported `apiKey` replaces the existing key.
- imported active ID wins when valid; otherwise existing active ID wins; otherwise the first config wins.

The parse method should return a complete immutable document or throw `ApiSiteTransferException`; no existing list is passed to the parser, which prevents partial mutation during validation.

- [ ] **Step 3: Run the focused tests and verify RED**

Run:

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test --tests 'com.fs.aicommit.ApiSiteTransferTest' --no-daemon --console=plain
```

Expected: compilation fails because `ApiSiteTransfer` does not exist.

- [ ] **Step 4: Implement the transfer types and encoder**

Use focused immutable types:

```kotlin
internal data class ApiSiteImportDocument(
    val activeConfigId: String,
    val configs: List<ApiProviderConfig>,
    val importedKeys: Map<String, String>
)

internal data class ApiSiteMergeResult(
    val configs: List<ApiProviderConfig>,
    val activeConfigId: String,
    val added: Int,
    val overwritten: Int,
    val importedKeys: Int,
    val preservedKeys: Int
)

internal class ApiSiteTransferException(message: String) : IllegalArgumentException(message)
```

`ApiSiteTransfer.encode` must create a `JsonObject`, set `format`, `version`, `activeConfigId`, `includeApiKeys`, and a normalized `configs` array. Add `apiKey` only when `includeApiKeys` is true and the key is nonblank. Return pretty JSON using `GsonBuilder().setPrettyPrinting().create()`.

- [ ] **Step 5: Implement strict parsing and merge planning**

Implement helpers that require expected JSON types instead of calling permissive Gson accessors directly. Parse all configs, reject duplicates, construct `ApiProviderConfig`, call `normalizeModels`, and reuse `validationError(requireModel = true)` after supplying a harmless placeholder key only for structural validation of remote sites. Do not weaken runtime key validation.

Implement:

```kotlin
fun merge(
    existing: List<ApiProviderConfig>,
    existingActiveId: String,
    document: ApiSiteImportDocument
): ApiSiteMergeResult
```

Build a new list from deep copies, preserving existing keys when the import has no nonblank key, replacing fields for matching IDs, appending new IDs, and choosing the active ID according to the design. Return the complete result without mutating inputs.

- [ ] **Step 6: Run the focused tests and verify GREEN**

Run the command from Step 3.

Expected: all `ApiSiteTransferTest` cases pass with zero failures.

### Task 4: Stage imports in the settings model and preserve PasswordSafe semantics

**Files:**
- Modify: `src/main/kotlin/com/fs/aicommit/ApiConfigurationModel.kt`
- Modify: `src/test/kotlin/com/fs/aicommit/ApiConfigurationModelTest.kt`

- [ ] **Step 1: Write failing staging tests**

Add tests proving:

```kotlin
@Test
fun `import changes the working copy but does not write credentials before apply`() {
    val credentials = MemoryCredentials().apply { keys["one"] = "local" }
    val settings = AiCommitSettings().apply {
        apiConfigs.add(ApiProviderConfig(id = "one", name = "Old", model = "old"))
        activeApiConfigId = "one"
    }
    val model = ApiConfigurationModel(credentials)
    model.reset(settings)
    val document = ApiSiteTransfer.decode(validImportJson(name = "New", apiKey = "imported"))

    model.importSites(document)

    assertEquals("New", model.configs.single().name)
    assertEquals("local", credentials.read("one"))
    model.apply(settings)
    assertEquals("imported", credentials.read("one"))
}
```

Also test that an import without an API key keeps the loaded local key after `apply`, and that a decode exception leaves the working list unchanged because `importSites` is never called.

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test --tests 'com.fs.aicommit.ApiConfigurationModelTest' --no-daemon --console=plain
```

Expected: compilation fails because `importSites` does not exist.

- [ ] **Step 3: Implement deep-copy-safe model operations**

Replace shallow `copy()` calls in `put`, `reset`, `apply`, and `baseline` with `deepCopy()`.

Add:

```kotlin
fun importSites(document: ApiSiteImportDocument): ApiSiteMergeResult {
    val result = ApiSiteTransfer.merge(configs, activeId, document)
    configs.clear()
    configs.addAll(result.configs.map(ApiProviderConfig::deepCopy))
    activeId = result.activeConfigId
    return result
}

fun exportJson(includeApiKeys: Boolean): String =
    ApiSiteTransfer.encode(configs, activeId, includeApiKeys)
```

Do not write to `ApiCredentialStore` in either method. Credential writes remain exclusively in `apply`.

- [ ] **Step 4: Run the focused tests and verify GREEN**

Run the command from Step 2.

Expected: all `ApiConfigurationModelTest` cases pass with zero failures.

### Task 5: Build nested API site/model quick switching

**Files:**
- Modify: `src/main/kotlin/com/fs/aicommit/QuickModelAction.kt`
- Modify: `src/test/kotlin/com/fs/aicommit/QuickModelActionTest.kt`

- [ ] **Step 1: Write failing site grouping tests**

Replace the old flat API expectation with:

```kotlin
@Test
fun `API sites expose normalized model choices and selecting one switches site and model`() {
    val settings = AiCommitSettings().apply {
        mode = AiCommitSettings.MODE_API
        apiConfigs = mutableListOf(
            ApiProviderConfig(id = "one", name = "One", model = "m1", models = mutableListOf("m1", "m2")),
            ApiProviderConfig(id = "two", name = "Two", model = "x1", models = mutableListOf("x1", "x2"))
        )
        activeApiConfigId = "one"
    }

    val sites = QuickModelAction.apiSites(settings)

    assertEquals(listOf("One", "Two"), sites.map { it.label })
    assertEquals(listOf("x1", "x2"), sites[1].choices.map { it.model })
    assertTrue(QuickModelAction.select(settings, sites[1].choices[1]))
    assertEquals("two", settings.activeApiConfigId)
    assertEquals("x2", settings.apiConfigs[1].model)
}
```

Add stale-choice coverage: deleting the config or removing the model before selection returns false and changes nothing.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test --tests 'com.fs.aicommit.QuickModelActionTest' --no-daemon --console=plain
```

Expected: compilation fails because `apiSites` and grouped choices do not exist.

- [ ] **Step 3: Add pure site grouping and robust selection**

Introduce:

```kotlin
internal data class ApiSiteChoices(
    val configId: String,
    val label: String,
    val choices: List<Choice>
)
```

`apiSites(settings)` returns one item per config and uses `config.availableModels()`. Each child `Choice.active` is true only when both the config ID and model match the current selection.

Update `select` so API selection finds the live config by ID, verifies that the selected model still exists in `availableModels`, sets `activeApiConfigId`, then sets `config.model`.

- [ ] **Step 4: Render nested action groups**

In API mode, build a popup subgroup per site:

```kotlin
val siteGroup = DefaultActionGroup(site.label, true)
site.choices.forEach { choice -> siteGroup.add(modelToggle(settings, project, choice)) }
group.add(siteGroup)
```

Keep CLI mode flat by reusing the existing toggle construction. Keep the management separator, settings action, failure title, speed search, ad text, anchoring, and running-task guard unchanged.

- [ ] **Step 5: Run the focused test and verify GREEN**

Run the command from Step 2.

Expected: all quick model tests pass with zero failures.

### Task 6: Add import/export controls and Windows file handling

**Files:**
- Modify: `src/main/kotlin/com/fs/aicommit/ApiModePanel.kt`
- Create: `src/main/kotlin/com/fs/aicommit/ApiSiteExportDialog.kt`
- Modify: `src/main/resources/messages/SubMsgBundle.properties`
- Modify: `src/main/resources/messages/SubMsgBundle_zh.properties`
- Modify: `src/test/kotlin/com/fs/i18n/SubMsgBundleTest.kt`

- [ ] **Step 1: Add failing resource parity and required-key assertions**

Extend `SubMsgBundleTest` to assert both bundles have identical keys and include strings for:

```text
aicommit.api.import
aicommit.api.export
aicommit.api.export.keys
aicommit.api.export.keys.warning
aicommit.api.export.keys.confirm
aicommit.api.export.title
aicommit.api.import.title
aicommit.api.import.success
aicommit.api.export.success
aicommit.api.transfer.error
```

- [ ] **Step 2: Run the resource test and verify RED**

Run:

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test --tests 'com.fs.i18n.SubMsgBundleTest' --no-daemon --console=plain
```

Expected: required keys are missing.

- [ ] **Step 3: Add bilingual strings and the export options dialog**

Create `ApiSiteExportDialog` as a small `DialogWrapper` with one `JBCheckBox`. Expose:

```kotlin
val includeApiKeys: Boolean
    get() = includeKeysBox.isSelected
```

The checkbox defaults to false. The dialog text must state that selected keys are written as plaintext. The dialog itself does not read files or settings.

- [ ] **Step 4: Add toolbar buttons in `ApiModePanel`**

After the existing test button, add a `JSeparator(SwingConstants.VERTICAL)`, then Import and Export buttons with translated labels and IntelliJ icons when suitable. Keep the existing add/edit/remove/test controls unchanged.

- [ ] **Step 5: Implement export UI flow**

The export handler performs these steps in order:

1. Show `ApiSiteExportDialog`; return on cancel.
2. If keys are included, call `Messages.showYesNoDialog` with the plaintext warning; return unless YES.
3. Open `FileSaverDescriptor` with the `json` extension through `FileChooserFactory`.
4. Return when the chooser is canceled.
5. Call `model.exportJson(includeApiKeys)` and write UTF-8 to the selected `VirtualFileWrapper.file` using `Files.writeString`.
6. Show a success message with the absolute path.
7. Catch `IOException` and `RuntimeException`, show `aicommit.api.transfer.error`, and leave the working model unchanged.

Use `Path`/`File` returned by the IntelliJ chooser; never build paths with `"/"` or `"\\"`.

- [ ] **Step 6: Implement atomic import UI flow**

The import handler performs these steps in order:

1. Open a single-file JSON `FileChooserDescriptor` using the IntelliJ platform chooser.
2. Return when canceled.
3. Read UTF-8 from the chosen local file.
4. Call `ApiSiteTransfer.decode(text)` before touching `ApiConfigurationModel`.
5. Call `model.importSites(document)` only after successful decoding.
6. Refresh the table and active combo.
7. Show added, overwritten, imported-key, and preserved-key counts.
8. Catch transfer, JSON, and I/O errors, display the exact safe message, and do not call `refresh` after a failed decode.

- [ ] **Step 7: Run resource and focused model tests and verify GREEN**

Run:

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test --tests 'com.fs.i18n.SubMsgBundleTest' --tests 'com.fs.aicommit.ApiSiteTransferTest' --tests 'com.fs.aicommit.ApiConfigurationModelTest' --no-daemon --console=plain
```

Expected: all selected tests pass with zero failures.

### Task 7: Update documentation and perform full verification

**Files:**
- Modify: `README.md`
- Modify: `README_CN.md`
- Modify: `FEATURE_SUMMARY.md`
- Modify: `FINAL_SUMMARY.md`
- Modify: `docs/superpowers/plans/2026-10-09-site-model-import-export.md`

- [ ] **Step 1: Update user documentation**

Document:

- API sites can retain multiple fetched or manually entered models.
- The commit toolbar menu uses `site > model` nesting.
- Selecting a model also activates its site.
- Import/export lives beside API site management buttons.
- JSON export omits API keys by default.
- Enabling key export writes plaintext secrets and requires confirmation.
- Same-ID import overwrites site fields; imports without keys preserve PasswordSafe values.

- [ ] **Step 2: Run all tests and build the plugin**

Run:

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test buildPlugin --no-daemon --console=plain
```

Expected: `BUILD SUCCESSFUL`, zero test failures, and a plugin ZIP at `build/distributions/generateCommitMessage-1.0.0.zip`.

- [ ] **Step 3: Inspect test totals and ZIP integrity**

Run:

```powershell
$files = Get-ChildItem -LiteralPath 'D:\subMsg\build\test-results\test' -Filter 'TEST-*.xml' -File
$tests = $failures = $errors = $skipped = 0
foreach ($file in $files) {
    [xml]$xml = Get-Content -LiteralPath $file.FullName
    $tests += [int]$xml.testsuite.tests
    $failures += [int]$xml.testsuite.failures
    $errors += [int]$xml.testsuite.errors
    $skipped += [int]$xml.testsuite.skipped
}
[pscustomobject]@{ Tests=$tests; Failures=$failures; Errors=$errors; Skipped=$skipped }
Get-Item -LiteralPath 'D:\subMsg\build\distributions\generateCommitMessage-1.0.0.zip'
Get-FileHash -LiteralPath 'D:\subMsg\build\distributions\generateCommitMessage-1.0.0.zip' -Algorithm SHA256
```

Expected: failures and errors are zero; the ZIP exists and has a nonzero size and SHA256.

- [ ] **Step 4: Perform static repository checks**

Run:

```powershell
git diff --check
git status --short
```

Expected: `git diff --check` produces no whitespace errors. `git status --short` lists the intended files and any pre-existing workspace changes; do not stage or discard them.

- [ ] **Step 5: Record final evidence**

Update `FINAL_SUMMARY.md` with the actual test count, build result, ZIP size, SHA256, key behavior, and Windows compatibility result. Mark every completed checkbox in this plan only after its command has produced the stated evidence.
