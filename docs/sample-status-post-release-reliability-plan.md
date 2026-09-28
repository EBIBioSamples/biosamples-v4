# SampleStatus Tracking + Post-Release Reliability Plan

## Guardrail Decision
- Keep `release` date as the source of truth for public/private behavior in access/search/cache paths.
- Use `SampleStatus` as explicit lifecycle tracking and workflow state (`DRAFT`, `PRIVATE`, `PUBLIC`, `CANCELLED`, `SUPPRESSED`, `KILLED`).
- Do not switch existing visibility logic in services such as `WebinAuthenticationService` and `SolrFilterService` to status-only behavior.

## Scope 1: Validate Status Model Usage (Tracking, Not Visibility)
- Review status creation/update touchpoints and ensure they are consistent with release-based visibility:
  - `core/src/main/java/uk/ac/ebi/biosamples/core/model/Sample.java`
  - `webapps/core/src/main/java/uk/ac/ebi/biosamples/service/SampleService.java`
  - `webapps/core-v2/src/main/java/uk/ac/ebi/biosamples/service/SampleService.java`
  - `pipelines/sample-release/src/main/java/uk/ac/ebi/biosamples/samplerelease/SampleReleaseCallable.java`
  - `pipelines/sample-post-release-action/src/main/java/uk/ac/ebi/biosamples/postrelease/SamplePostReleaseActionCallable.java`
  - `pipelines/chain/src/main/java/uk/ac/ebi/biosamples/helpdesk/services/SampleStatusUpdater.java`
- Define allowed status transitions and ownership (submission vs pipeline vs helpdesk/manual).
- Identify mismatches where status is stale/incorrect but release-driven behavior is still correct.

## Scope 2: Root-Cause Pipeline Missed Samples
- Investigate release-window generation behavior in:
  - `pipelines/common/src/main/java/uk/ac/ebi/biosamples/utils/PipelineUtils.java`
- Confirm that `release` filtering supports intended `--from` and `--until` semantics.
- Verify runtime arguments and operational trigger cadence for `sample-post-release-action`.
- Audit completion behavior in runner:
  - future handling/draining
  - shutdown wait behavior
  - failed queue capture and reporting

## Scope 3: Reliability Design for `sample-post-release-action`
- Keep pipeline function: update status from `PRIVATE -> PUBLIC` for samples released in target window.
- Improve capture reliability without changing release-based visibility semantics:
  - Define explicit window contract (`from..until` range, inclusive day handling).
  - Add overlap strategy for reruns to avoid boundary misses.
  - Add optional watermark/checkpoint mode for continuous operation.
  - Add observability:
    - input/effective window
    - candidate count
    - success/failure count
    - failed accession output
- Keep transition idempotent and skip non-target statuses safely.

## Scope 4: Non-Visibility Status Handling
- Specify handling rules for non-visibility statuses:
  - `DRAFT`
  - `CANCELLED`
  - `SUPPRESSED`
  - `KILLED`
- Ensure status transitions do not accidentally alter release-driven visibility unless explicitly required.
- Document interoperability with existing fields/attributes (for example `INSDC status`).

## Scope 5: Test Strategy (No Behavior Flip for Visibility)
- Add/expand tests around:
  - release window boundaries (`from`, `until`, UTC day edges)
  - pipeline rerun overlap and missed-sample prevention
  - idempotent transitions (`PRIVATE -> PUBLIC`) and no-op on other statuses
  - status tracking consistency while preserving release-based access behavior
- Add focused tests for runner completion to ensure all scheduled tasks are awaited.

## Deliverables
- Findings report containing:
  - confirmed root cause(s) for missed samples
  - recommended minimal code changes
  - rollout order and rollback-safe steps
- Follow-up implementation breakdown into small commits:
  - window fix
  - observability
  - reliability hardening
  - tests
  - optional watermark mode
