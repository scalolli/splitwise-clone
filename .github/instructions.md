# Splitwise Clone - Development Instructions

## Overview
This repository contains the active Kotlin/http4k implementation of Splitwise Clone.

## Active Project Direction
- Primary work happens in the root Gradle project
- Start with `docs/http4k-rewrite/README.md` for orientation
- Use `docs/http4k-rewrite/07-handoff.md` to find the exact next slice
- Use `docs/http4k-rewrite/08-functionality-checklist.md` for the consolidated behavior checklist

## Technology Direction
- Kotlin
- http4k
- Gradle
- Test-first implementation per slice

## Development Methodology
- Follow Test-Driven Development (TDD)
- Deliver one green slice at a time
- Keep commits small and atomic
- All code changes must have corresponding tests

## Testing Policy
All new features, bug fixes, or validations must have failing tests first. No code changes
without relevant tests.

## Current Next Step
- Read `docs/http4k-rewrite/07-handoff.md`
- Read the `SLICE-003` entry in `docs/http4k-rewrite/04-iteration-backlog.md`
- Write the failing `Money` tests first
- Implement the minimal value object to go green

## Project Structure
```
src/
├── main/kotlin/     # Application code
├── main/resources/  # Templates and migrations
└── test/kotlin/     # Unit and handler tests

docs/http4k-rewrite/ # Plan, spec, backlog, handoff, checklist
```

## Key Rule
Prefer the rewrite docs over any historical assumptions. The docs define the behavior and architecture.
