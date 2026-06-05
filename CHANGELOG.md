# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.8.0] - 2026-06-05

### Added

- Support using an existing Halo server for OpenAPI generation via `useExistingServer` option in `OpenApiExtension`. When enabled, the `haloGenerateOpenApiDocs` task skips Docker container setup and teardown, directly generating API documentation from the configured Halo server URL. (#50)

### Changed

- The `haloGenerateOpenApiDocs` task no longer unconditionally depends on `pullHaloImage`. The dependency is only added when `useExistingServer` is `false` (the default).

[0.8.0]: https://github.com/halo-sigs/halo-gradle-plugin/compare/v0.7.0...v0.8.0
