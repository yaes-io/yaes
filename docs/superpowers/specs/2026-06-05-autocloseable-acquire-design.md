# Design: Widen `Resource.acquire` to `AutoCloseable`

**Date:** 2026-06-05
**Status:** Approved

## Problem

`Resource.acquire` currently accepts only `java.io.Closeable`. Many JVM resource types (JDBC `Connection`, `Statement`, `ResultSet`; NIO channels; etc.) implement `java.lang.AutoCloseable` but not `java.io.Closeable`. Users of those types must fall back to `Resource.install` with an explicit release function, which is more verbose than necessary.

## Decision

Widen the type bound of `Resource.acquire` from `Closeable` to `AutoCloseable`.

`java.io.Closeable` extends `java.lang.AutoCloseable`, so the change is fully backward-compatible. All existing callers passing a `Closeable` continue to work unchanged.

## Changes

### `Resource.scala`

- Change `acquire[A <: Closeable]` to `acquire[A <: AutoCloseable]`.
- Remove `import java.io.Closeable` (no longer referenced).
- `java.lang.AutoCloseable` requires no import.
- Update Scaladoc: replace `Closeable` references with `AutoCloseable`.

### `ResourceSpec.scala`

- Existing test helpers (`TestResource`, `FailingOnAcquireResource`) extend `Closeable` and continue to compile.
- Add one new test resource that extends only `AutoCloseable` (not `Closeable`) to exercise the widened bound.
- Add test cases covering: normal acquisition/release, error during usage, error during acquisition.

## Scope

Two files, approximately five lines changed.

## Non-goals

- No new method names or overloads.
- No changes to `install`, `ensuring`, or `Resource.run`.
- No changes to any other effect.
