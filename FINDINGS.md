# Migration Findings (issue #67)

## Completed
- #288: build.sbt + CI slug done. organization=io.yaes, homepage=github.com/yaes-io/yaes, codecov slug=yaes-io/yaes.
- #290: yaes-core migrated. All 39 files moved from in/rcard/yaes → io/yaes. 322 tests pass.
- #292: yaes-data migrated. 3 source + 14 test files moved to io/yaes. 295 tests pass.
- #291: yaes-cats migrated. 13 source + 6 test files moved to io/yaes. Root package object converted to top-level definitions. 70 tests pass.
- #293: yaes-slf4j + yaes-test migrated. 2 slf4j sources + 1 test, 4 core-scalatest sources + 4 tests, 2 http-scalatest sources + 1 test moved to io/yaes. 38 tests pass.

## Task order (dependency chain)
1. #288 build.sbt + CI — DONE
2. #290 yaes-core — DONE
3. #292 yaes-data — DONE
4. #291 yaes-cats — DONE
5. #293 yaes-slf4j + yaes-test — DONE
6. #294 yaes-http (server, client, jsoniter, circe)
7. #289 README + docs

## Migration pattern per module
- Move files: `src/main/scala/in/rcard/yaes/` → `src/main/scala/io/yaes/`
- Move files: `src/test/scala/in/rcard/yaes/` → `src/test/scala/io/yaes/`
- Sed replace `package in.rcard.yaes` → `package io.yaes` in all .scala files
- Sed replace `import in.rcard.yaes` → `import io.yaes` in all .scala files
- Delete old directory tree after move

## Cross-module imports
Some modules import from each other — after moving yaes-core, check that
dependent modules' `import in.rcard.yaes.*` statements also get updated when
their turn comes.

## io.yaes namespace collision pitfalls
- In files under `io.yaes.*`, writing `cats.data.X` resolves to `io.yaes.cats` sub-package, not `_root_.cats`. Use `import _root_.cats.data.X` explicitly.
- In extension methods where the receiver parameter is named `io`, writing `io.yaes.Sync` fails. Use the import alias (e.g., `YaesSync`) or `_root_.io.yaes.Sync`.
