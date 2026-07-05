# YAES &rarr; kyo-compat binding: GAP analysis

What YAES is missing to be added as a new `kyo-compat` backend binding.

- **Carrier:** direct-style / Loom
- **Reference binding:** `ox`
- **Target:** JVM-only

## Frame

`kyo-compat` is a multi-backend layer (bindings: `ce`, `future`, `kyo`, `ox`, `twitter-future`, `zio`). A binding implements **13 opaque `C*` types** over one carrier:

`CIO`, `CFiber`, `CPromise`, `CChannel`, `CLatch`, `CMeter`, `CLocal`, `CStream`, `CAtomicInt/Long/Boolean/Ref`, `CChunk` (+ `FromCompletionStage` on JVM).

YAES is direct-style on Loom (context functions, eager, `StructuredTaskScope`). The existing **`ox` binding is the exact template** (also Loom direct-style). Natural carrier: `CIO[+A] = (…, Async) ?=> A`, errors as `throw` / `catch NonFatal` (same as `ox`, no need to route through `Raise`).

> **Decisive fact.** The `ox` binding backs Channel/Promise/Latch/Meter/Atomics with plain `java.util.concurrent`. On Loom, blocking is cheap, so **most `C*` types are pure plumbing, not effect-system primitives.** The gap collapses to one capability.

## The gap (confirmed)

> **YAES has only eager-propagating structured forks. No fork whose failure is _contained_ until explicit join = no unsupervised fork.**
>
> **Evidence.** `Async.fork` registers into a per-fiber nested `StructuredTaskScope`; `Async.run` joins all before returning. Fiber error propagates eagerly up, cancels siblings + scope (async-specs §2.1/2.2). `grep unsupervised|detach|daemon` over `yaes-core` &rarr; nothing.

This one missing capability breaks four compat operations:

| Compat op | Needs | Why YAES fails today |
|---|---|---|
| `CFiber.get` error round-trip | error surfaces at `.get`, scope survives | `FiberTest:29` "CFiber.get re-fails on typed error" forks a failing CIO, expects `Failure(TestError)` at `fib.get.liftToTry`. YAES fork failure tears down the enclosing scope before `.get` runs. **Named failing test.** |
| stack-safe `flatMap` bounce | fork fresh vthread stack, join, rethrow cleanly | `ox` does `forkUnsupervised(body).join()`. YAES `fork` would eagerly cancel on body failure, and needs `Async` at every hop. |
| `cede` | cheap yield via fork+join | same; `ox` uses `forkUnsupervised(()).join()`. |
| `CFiber.onComplete` observer | run effectful callback after completion, **outside** the structured scope | `ox` uses `oxThreadFactory.newThread` (detached daemon vthread) + fresh `supervised`. YAES has no scope-escape; `Fiber.onComplete` exists but requires ambient `Async` and cannot outlive `run`. |

> **Recommendation.** Add to YAES core an `Async.forkUnsupervised[A](block): Fiber[A]` whose failure is captured in the fiber (surfaced at `value`/join via `Raise`/`Try`) and does **not** cancel siblings. Plus a detached/daemon spawn for observers. That is the headline deliverable.

## Secondary real gaps

Smaller; some workaroundable in the binding.

| Gap | Detail | Workaround |
|---|---|---|
| **race semantics** | YAES `race` = first-to-**complete** (success _or_ failure), loser cancelled. Compat needs first-**success** (`ox.raceSuccess`): `RaceZipTest:93` "fast success + slow failure &rarr; success wins"; `:49` failure only if **all** fail. Discriminating case (fast-fail + slow-success): YAES returns the failure; compat wants the slow success. | Buildable in binding on exposed `racePair` (loop until success or all-fail). Primitive itself is wrong-shaped. |
| **bounded parallelism** | `foreach/filter/collectAll` take a `concurrency: Int` cap. `Async.parTraverse` is **unbounded** (one fiber per item). No `parLimit`. | Gate with a `Semaphore` (Meter), or add `parTraverseLimit` to YAES. |
| **Async.never** | compat `CIO.never` / `ox.never`. None in YAES. | Block interruptibly forever (`Async.delay(MAX)` or latch await). Trivial. |
| **run blocks** | `unsafeRun: => Future[A]` = `Future { Async.run { … } }`. Boundary must install all handlers (`Async/Clock/Resource/Raise`). | Non-gap; binding assembly. |

## Non-gaps

Build in the binding, no YAES change, exactly like `ox`.

| Type | Backing | Note |
|---|---|---|
| `CPromise` | `CompletableFuture` | JUC |
| `CLatch` | `CountDownLatch` | JUC |
| `CMeter` | `Semaphore` | JUC |
| `CAtomic*` | `java.util.concurrent.atomic.*` | JUC |
| `CChunk` | `kyo.Chunk` / `Vector` | data |
| `CChannel` | YAES `Channel` (yaes-data) or `LinkedBlockingQueue` | bounded/unbounded/rendezvous, `send`/`receive`/`close`. `poll` (non-blocking) needs a `tryReceive` &mdash; confirm yaes-data Channel exposes one. |
| `CLocal` | java `ScopedValue` &mdash; **not** `Reader` | `Reader[R]` is **type-keyed**; `LocalTest:16-17` allocates two `CLocal.init[Int]` (same type, distinct instances) &rarr; Reader collides. `ScopedValue` is instance-keyed and inherited by `StructuredTaskScope` subtasks, so cross-fork propagation works for free. |
| Clock / timeout | YAES `Clock`, `Async.timeout` | now/monotonic, sleep, `timeout`/`timeoutWithError` all mappable. |
| acquire/ensure | `try/finally` + `addSuppressed` | Resource exists but not required. |
| error channel | `throw` / `catch NonFatal` | See nuance below. |

> **One nuance to verify.** A raw-thrown error inside a forked `CFiber` must still trigger Async sibling-cancellation the way `Raise.raise` does, or be routed through `Raise`. `NonFatal` already excludes `InterruptedException` (cancellation is interrupt-based), so interrupts are safe; the open question is the failure&rarr;cancel integration.

## CStream (partial)

YAES `Flow` exists in **yaes-data** (pull/push-collector: `map/filter/take/drop/fold/unfold/merge/zipWith`). But:

- `map/filter/transform` take **pure** functions; compat needs **effectful** (`A => CIO[B]`). Trivial in direct-style (run effect inline in the collector).
- **No `flatMap`** (stream-of-streams), **no `concat`**, no `collectPure` / effectful `tap`. Build in binding.

Moderate binding work, **not** a core gap blocker. (`ox` uses its own `ox.flow.Flow`; YAES `Flow` is the analog.)

## Bottom line

Add **one** thing to YAES core: an **unsupervised / failure-contained fork** (+ detached daemon spawn for observers, + `Async.never`). Everything else is binding plumbing (JUC + `ScopedValue` + throw/catch), with `race` (via `racePair`), bounded `parTraverse`, and `Flow` `flatMap`/`concat` as binding-side fills. The `ox` binding is the reference throughout. Binding is JVM-only (Loom).

Open question: does yaes-data `Channel` expose a non-blocking `tryReceive` (for `CChannel.poll`)? Not seen in the surface scan.

---

_Sources: `kyo-compat/bindings/ox` (template), `kyo-compat/bindings/kyo` (contract), `kyo-compat/test` (acceptance), `yaes-core/Async.scala` + `async-specs.md`, `yaes-data/{Channel,Flow}.scala`._
