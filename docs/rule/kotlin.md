# Kotlin Guidelines For K-ToME

## Scope

- This document applies to all Kotlin code in `core`, `game`, `client`, and `tools`
- Read this file before writing Kotlin code
- `AGENTS.md`, phase documents, and checklist gates remain authoritative; when rules overlap, follow the stricter one
- These rules optimize for maintainability, determinism, typed contracts, and long-term evolution, not mechanical brevity

## Core Principles

- Prefer `val` over `var`; mutable state must have a clear owner
- Prefer specific types over generic placeholders
- Explicitly declare return types for public APIs, cross-module contracts, save/schema DTO mappers, and complex functions
- Keep business semantics in types instead of encoding them in raw strings, maps, or booleans
- Prefer small, reviewable changes over speculative abstraction
- Do not optimize prematurely; performance changes should be justified by measurement

## Project Boundaries

- `core` defines rules, deterministic algorithms, save semantics, and typed runtime models
- `game` assembles content, schema, registries, and official gameplay sessions
- `client` handles rendering, input, audio, locale bundle consumption, and presentation orchestration
- `tools` owns lint, smoke, harness, batch, and release validation
- Do not move rule logic into `client` for convenience
- Do not make `core` return localized strings, raw asset paths, or UI-only state
- Do not create a second source of truth across modules

## Naming

- Classes, interfaces, and objects: `PascalCase`
- Functions, properties, and local variables: `camelCase`
- Constants and enum entries: `UPPER_SNAKE_CASE`
- Packages: lowercase and dot-separated
- Boolean names should read as predicates: `is`, `has`, `can`, `should`
- Use names that carry domain meaning; avoid vague names such as `Utils`, `Helper`, `Manager` unless the type is truly infrastructural
- Abbreviations are acceptable only when they are standard and improve readability, such as `API`, `URL`, `HTTP`, `JSON`, `id`

## Function Design

- A function should have one clear responsibility
- Prefer early returns over deep nesting
- Use expression bodies when they improve clarity
- Function names should describe an action or a computed result
- Use named arguments when they materially improve readability, especially in tests, builders, and same-module call sites
- Do not use named arguments as a blanket rule on public APIs where parameter renaming would become a compatibility hazard
- Use default parameters when they simplify the API; do not replace clear overloads with confusing parameter matrices
- Do not split a coherent function only to satisfy an arbitrary line limit

```kotlin
fun resolveActorRole(actor: Actor?): String {
    actor ?: return "Unknown"
    if (!actor.isAlive) return "Dead"
    if (!actor.isHostile) return "Neutral"
    return "Hostile"
}
```

## Types And Contracts

- Public business APIs must not expose bare `Any`
- Generic infrastructure may use `T : Any` or internal `Any` storage when it is encapsulated and type-safe at the boundary
- Prefer `data class`, `sealed interface`, `sealed class`, and `enum class` to express finite domain models
- Prefer value objects or `@JvmInline value class` when wrapping primitives adds real semantic safety
- Replace stringly-typed result channels with typed result objects, enums, or sealed results
- Stable contracts should be explicit: schema IDs, event IDs, save versions, manifest versions, and typed operation codes belong in named models

```kotlin
sealed interface TalentUseResult {
    data class Success(val talentId: String) : TalentUseResult

    data class Failure(
        val code: TalentFailureCode,
        val talentName: String? = null,
    ) : TalentUseResult
}
```

## Null Safety

- Prefer nullable types, safe calls, Elvis, `requireNotNull`, and `checkNotNull`
- Do not use `!!` as a routine control-flow tool
- If `!!` is truly unavoidable, document the invariant immediately next to it
- Do not stack repeated `if (x != null)` checks when a guard clause or local narrowing is clearer
- Prefer failing fast at invalid boundaries instead of letting impossible nulls leak deeper into the system

```kotlin
val player = requireNotNull(world.get<Player>(playerId)) { "Missing Player for $playerId" }
```

## Collections And State

- Default to read-only collection types at public boundaries
- Do not leak mutable collections that callers can mutate behind your back
- Use mutable collections internally only when mutation is intentional and ownership is clear
- Use `asSequence()` only when it measurably helps or clearly improves a long transformation pipeline
- Prefer a simple loop when it is easier to read than a chain of collection operators
- Use scope functions sparingly; if `let`/`also`/`apply`/`run` hides intent, write the control flow out directly

## Constants And Literals

- Extract constants when a value has domain meaning, is reused, or is part of a stable contract
- Do not create named constants for trivial local literals such as `0`, `1`, or one-off loop bounds unless the name adds meaning
- IDs, schema versions, action costs, floor thresholds, and visual/audio contract keys should never be magic literals scattered across call sites

## Class Design

- Prefer composition over inheritance
- Use `object` for real singletons or stateless policy containers, not as a dumping ground for unrelated helpers
- Keep classes cohesive; if a type mixes unrelated responsibilities, split by responsibility rather than by arbitrary size
- Large orchestrators or mappers are acceptable when they remain the natural ownership boundary, but extract repeated sub-logic and unstable detail
- Avoid classes whose only role is to mirror data without adding semantics unless they are explicit DTOs or schema models

## Control Flow And Errors

- Validate assumptions at boundaries with `require`, `check`, and explicit failures
- Error messages should explain the violated invariant, not narrate obvious code flow
- Prefer typed failure codes for behavior that callers need to branch on
- Use exceptions for broken invariants and impossible states, not as a substitute for ordinary domain branching
- Avoid boolean parameter combinations that hide behavior; prefer named option types or configuration models

## Concurrency And Determinism

- Determinism is mandatory for rule logic, save/load, replay, and harness paths
- Do not read system time, hidden global randomness, thread-local state, or unstable iteration order from rule code
- Randomness must be injected, seeded, or isolated behind explicit boundaries
- Do not introduce coroutine or Flow architecture by default; this repository currently does not use it as a baseline
- If a future task explicitly introduces coroutines, use structured concurrency and document ownership, cancellation, and threading semantics
- Use `async` only for truly independent work that benefits from parallelism; sequential code is correct when the dependency is sequential

## Visibility

- Use the least visibility that keeps the API usable
- Default to `private` for implementation detail and `internal` for module-local contracts
- Do not make APIs `public` unless there is a real cross-file, cross-module, or external consumer
- Keep extension functions close to the type or use case they belong to; avoid global extension clutter

## Tests And Reviewability

- Non-trivial rule changes should come with tests in the same change
- Tests should use fixed seeds and stable assertions where determinism matters
- Prefer assertions on behavior and contract, not incidental implementation detail
- Keep test names descriptive enough to explain the invariant being protected
- When a design choice is non-obvious, add a short comment explaining why, not what

## Heuristics For Review

- If a reviewer cannot tell which module owns a piece of logic, the boundary is probably wrong
- If behavior depends on parsing UI text or localized strings, the contract is probably wrong
- If a stable key exists only as a repeated string literal, the contract is probably under-modeled
- If extracting a helper makes the call graph harder to follow without reducing complexity, keep the logic inline
- If a refactor lowers local line count but increases semantic indirection, it is probably not an improvement
