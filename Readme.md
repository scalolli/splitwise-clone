# Splitwise Clone

[![Kotlin CI](https://github.com/scalolli/splitwise-clone/actions/workflows/kotlin.yml/badge.svg)](https://github.com/scalolli/splitwise-clone/actions/workflows/kotlin.yml)

An expense sharing application inspired by Splitwise, implemented in Kotlin with http4k.

## Current Phase

- Active work follows the implementation plan in `docs/http4k-rewrite/`
- The current next slice is tracked in `docs/http4k-rewrite/07-handoff.md`
- The functionality target is captured in `docs/http4k-rewrite/08-functionality-checklist.md`

## Running Locally

Prerequisite: Java 21

```bash
./gradlew run
```

The app starts on port `8080` by default.

## Running Tests

```bash
./gradlew test
```

## Project Structure

```text
.
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   ├── main/kotlin/
│   ├── main/resources/
│   └── test/kotlin/
└── docs/http4k-rewrite/
```

## Rewrite Docs

- `docs/http4k-rewrite/README.md` - documentation index
- `docs/http4k-rewrite/02-behavior-spec.md` - application behavior contract
- `docs/http4k-rewrite/04-iteration-backlog.md` - slice-by-slice delivery order
- `docs/http4k-rewrite/07-handoff.md` - current state and next action
- `docs/http4k-rewrite/08-functionality-checklist.md` - consolidated feature checklist

## Development Workflow

This project follows Test-Driven Development:

1. Write a failing test
2. Implement the smallest change to make it pass
3. Refactor while keeping tests green
4. Commit the green slice

## License

This project is developed for educational purposes.
