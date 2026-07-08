## Project Overview

λÆS (Yet Another Effect System) is an experimental effect system for Scala 3 inspired by Algebraic Effects. It uses context parameters and context functions for modular, composable effect management with deferred execution.

**Scala Version:** 3.8.1 | **Java Requirement:** Java 25+ (Virtual Threads, Structured Concurrency)

## Build and Test

Refer to the [TESTS.md](../TESTS.md) file for testing requirements and guidelines.

## Error Handling Philosophy

As an effect system, λÆS must never throw untracked exceptions (e.g., `require`, `IllegalArgumentException`). Invalid inputs must be handled without escaping the effect system. Two acceptable strategies:

1. **Default values**: Clamp or coerce invalid inputs to a sensible default (e.g., negative delay becomes `Duration.Zero`, `NaN` factor becomes `0.0`).
2. **Make wrong state unrepresentable**: Use validated opaque types so invalid values cannot be constructed at all (e.g., an opaque `PositiveDuration` type that only admits valid values).

Never use `require`, `assert`, or throw exceptions in public API surfaces.

## References

- Architecture and module structure: `ARCHITECTURE.md`
- Code style and documentation standards: `CONVENTIONS.md`
- Build and test guidelines: `TESTS.md`
- Module-specific guidance: `<module>/CLAUDE.md`

## Agent skills

### Issue tracker

Issues live in GitHub Issues (`github.com/yaes-io/yaes`). See `docs/agents/issue-tracker.md`.

### Triage labels

Using default label vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Multi-context layout (`CONTEXT-MAP.md` at root, per-context `CONTEXT.md` files under `src/<context>/`). See `docs/agents/domain.md`.
