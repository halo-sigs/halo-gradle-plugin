# AGENTS.md

## Project Overview

- This repository contains `halo-gradle-plugin`, a Java-based Gradle plugin for developing and building Halo plugins.
- The published Gradle plugin ID is `run.halo.plugin.devtools`.
- Main source code lives under `src/main/java/run/halo/gradle`.
- Tests live under `src/test/java/run/halo/gradle`.
- The project uses Gradle Wrapper with Gradle 8.14 and Java 17 toolchains.

## Repository Layout

- `build.gradle`: Gradle plugin metadata, dependencies, publishing, signing, and test configuration.
- `settings.gradle`: root project name.
- `gradle.properties`: project version.
- `README.md` and `README_zh.md`: user-facing documentation.
- `src/main/java/run/halo/gradle/docker`: Docker remote API support used by development tasks.
- `src/main/java/run/halo/gradle/openapi`: OpenAPI docs and API client generation tasks.
- `src/main/java/run/halo/gradle/role`: role template generation.
- `src/main/java/run/halo/gradle/watch`: file watching and automatic reload support.
- `src/main/java/run/halo/gradle/utils`: shared utilities.

## Setup Commands

- Use the checked-in wrapper for all Gradle commands: `./gradlew`.
- Compile and run tests: `./gradlew test`.
- Full local verification: `./gradlew clean build`.
- Publish to the local internal build repo when needed: `./gradlew publish`.

## Testing Instructions

- Run `./gradlew test` for normal code changes.
- Run `./gradlew clean build` before release-oriented changes, Gradle metadata changes, dependency changes, or broad refactors.
- Add or update focused JUnit 5 tests for behavior changes.
- Prefer tests near the package being changed, mirroring the `src/main/java` package structure.
- Some plugin tasks interact with Docker or a Halo server; do not assume Docker-dependent behavior is covered by unit tests unless the test explicitly sets it up.

## Code Style

- Java source targets Java 17. Do not introduce APIs that require a newer Java runtime.
- Follow `.editorconfig`: UTF-8, LF line endings, spaces, 4-space indentation, and 100-character line width for Java.
- Keep changes small and local to the relevant package.
- Prefer existing project patterns over new abstractions.
- Use clear task and extension names that match Gradle conventions.
- Avoid adding comments that merely restate the code; add comments only for non-obvious behavior or external constraints.

## Gradle Plugin Notes

- Plugin implementation starts at `run.halo.gradle.HaloDevtoolsPlugin`.
- The plugin provides development tasks such as `haloServer`, `watch`, and API client generation.
- `haloServer` and `watch` require a working Docker environment.
- Preserve backwards-compatible Gradle DSL behavior unless the change is explicitly breaking.
- Be careful with task inputs, outputs, and lazy Gradle APIs so configuration cache and incremental behavior do not regress.

## Documentation Rules

- When changing user-facing behavior, update `README.md` and `README_zh.md` together when practical.
- Keep command examples using `./gradlew`.
- Keep Halo plugin configuration examples in Groovy DSL unless the surrounding documentation uses another format.

## Context7 Documentation Lookup

Use the `ctx7` CLI to fetch current documentation whenever a task asks about a library, framework, SDK, API, CLI tool, or cloud service. This includes API syntax, configuration, version migration, library-specific debugging, setup instructions, and CLI usage.

Do not use Context7 for refactoring, writing scripts from scratch, debugging this project's business logic, code review, or general programming concepts.

Steps:

1. Resolve the library first:

   ```shell
   npx ctx7@latest library <name> "<user's question>"
   ```

2. Pick the best `/org/project` match using exact name match, description relevance, snippet count, source reputation, and benchmark score.
3. Fetch docs:

   ```shell
   npx ctx7@latest docs <libraryId> "<user's question>"
   ```

4. If the result is not satisfactory, rerun the docs command with `--research`.

Do not run more than 3 Context7 commands per question. Do not include secrets in Context7 queries. If Context7 fails with a quota error, tell the user and suggest `npx ctx7@latest login` or setting `CONTEXT7_API_KEY`.

## Pull Request Guidance

- Summarize behavior changes and verification commands in the PR description.
- Mention any Docker, Halo server, publishing, or signing assumptions.
- Do not commit generated build output from `build/`.
- Keep dependency upgrades intentional and call them out explicitly.

## Security Considerations

- Never commit credentials, signing keys, Maven credentials, Docker credentials, or local Halo admin passwords.
- Treat values such as `OSSR_USERNAME`, `OSSR_PASSWORD`, `MAVEN_USERNAME`, and `MAVEN_PASSWORD` as secrets.
- Avoid logging authorization headers, Basic Auth strings, or tokens in task output.
