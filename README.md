# AURA Orchestrator

**Aura Orchestrator** is a production-oriented, modular AI orchestration platform for Android — an AI
reasoning + planning + agents + tools + persistent workspace, rather than a conventional chatbot.

> "Build the system as an actual functioning orchestrator, not a visual mockup."

This repository tracks the **v0.1.0** vertical slice. Everything below is real: Room persistence,
deterministic tools, WorkManager downloads, a permission engine, dynamic capability registry,
a credential broker (Android Keystore), an OpenAI-compatible model gateway, task recovery after
process death, and a premium Material 3 / Compose UI.

## Building the APK

The only environment with the network access Gradle needs is GitHub Actions, so building is driven
by CI (this sandbox's egress blocks `dl.google.com` / Maven Central / Gradle). Push to the branch
and CI compiles, runs the JVM tests and uploads **signed debug and release APKs** as workflow
artifacts.

> ✅ **Build status: green.** The latest run
> [34122024066](https://github.com/mrmusic7654-wq/Auror/actions/runs/34122024066) compiles `core` +
> `app`, passes the JVM unit tests, and produces a **signed `aura-release-apk`** (~8.8 MB) plus a
> debug APK. Download them from the run's **Artifacts** panel (Actions → "AURA Build & Test" → latest
> run → Artifacts). CI signs the release with a CI-provisioned self-signed key so no private key
> lives in the repository.

```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:assembleRelease        # signed release APK
./gradlew :app:testDebugUnitTest      # JVM unit tests
./gradlew :core:test                  # core module tests
```

Locally you only need the Android SDK (AGP auto-downloads missing packages when online).

## Architecture (spec-faithful)

Two Gradle modules; strict boundaries, independently testable.

```
core   pure Kotlin JVM (domain, protocol SDK, DAG/task-graph, routers,
       permission engine, memory, model-router, artifacts, budgets) — unit-tested
app    Android (Room, DataStore, Hilt, WorkManager, OkHttp, Compose/Material3,
       dragon-core UI, local bound service, download worker)
```

- Request → `OrchestratorEngine.submit` → complexity router
  - **SIMPLE** → deterministic offline tools (calculator, list files, download, note)
  - **MEDIUM / COMPLEX** → model path via the OpenAI-compatible gateway (only when a model is
    configured; otherwise the task honestly reports "model not configured" and lists offline actions)
- Every task/state is persisted in Room → survives process death; interrupted tasks are recovered
  on restart.
- Capabilities resolve dynamically: `capability → provider → transport`; no provider is hardcoded
  into the UI.
- Secrets live in the encrypted credential broker; the model only ever sees an "authenticated"
  signal, never a raw key.
- Agents are bounded executor roles with budgets; no uncontrolled agent loops; no chain-of-thought
  is exposed (only safe activity summaries).

## Status

Milestone 1 (foundation + working vertical slice) is implemented. Later milestones (remote
Python/Linux sandbox, companion-app AIDL discovery, browser automation, GitHub, MCP servers,
mini-apps, PDF) are scaffolded as registry/honest "not configured" states and are progressively
being filled in. Run Diagnostics in-app to see each subsystem's real state.
