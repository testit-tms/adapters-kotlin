# Test run tags and links

Specification for attaching **run-level** tags and links to a Test IT test run from Kotlin adapters.

These are **not** the same as per-autotest tags/labels or result links (`TestItContext.links` / `@Link`).

## Configuration

| Description | File property | Environment variable | System property |
|-------------|---------------|----------------------|-----------------|
| Test run tags | `testRunTags` | `TMS_TEST_RUN_TAGS` | `tmsTestRunTags` |
| Test run links (JSON) | `testRunLinks` | `TMS_TEST_RUN_LINKS` | `tmsTestRunLinks` |

Empty / omitted values mean “do not change tags/links”.

### Tags format

Comma-separated:

```text
smoke,nightly
```

or JSON array:

```json
["smoke", "nightly"]
```

### Links format

JSON array. `url` is required; `title`, `description`, `type` are optional.

```json
[
  {
    "url": "https://gitlab.example.com/group/project/-/jobs/12345",
    "title": "CI Job",
    "type": "Related"
  }
]
```

### Link types (test run)

| Type | Value |
|------|-------|
| Related (default) | `Related` |
| Blocked by | `BlockedBy` |
| Defect | `Defect` |
| Issue | `Issue` |
| Requirement | `Requirement` |
| Repository | `Repository` |

If `type` is omitted or unknown, the adapter uses `Related`.

## When data is sent

| Scenario | Behaviour |
|----------|-----------|
| Adapter **creates** the test run (`adapterMode=2`, or Sync Storage path without `testRunId`) | `tags` and `links` are passed in `CreateEmptyTestRunApiModel` **before** / at start of the run |
| Adapter uses an **existing** `testRunId` (`adapterMode=1`) | Early `updateTestRun` at startup merges configured tags/links into the run (**before** long test execution) |

**Hard requirement from product:** a CI job URL from config must be visible on the run while status is still **In progress**.

## Merge semantics (existing run)

- Existing UI / API tags and links are **kept**
- Configured items are **added**
- Duplicates skipped: same tag string; same link `url`

## CI example

```bash
export TMS_TEST_RUN_TAGS=smoke,ci
export TMS_TEST_RUN_LINKS='[{"url":"'"$CI_JOB_URL"'","title":"GitLab Job","type":"Related"}]'
```

Typical flow:

1. Create test run (CLI/API) **with** job link + optional tags, or let adapter create it in mode 2 with the same env vars.
2. Pass `TMS_TEST_RUN_ID` into the adapter when reusing a run.
3. Adapter merges any extra tags/links from its config at startup.

## Implementation reference

| Component | Role |
|-----------|------|
| `AppProperties.TEST_RUN_TAGS` / `TEST_RUN_LINKS` | Config keys + env/cli aliases |
| `TestRunMetadataParser` | Parse + merge helpers |
| `ClientConfiguration.testRunTags` / `testRunLinks` | Typed config |
| `TmsApiClient.createTestRun()` | Pass tags/links on create |
| `TmsApiClient.updateTestRun()` | Early merge on existing run |
| `AdapterManager.startTests()` | Calls update/create at launch start |
