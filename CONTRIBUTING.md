# Contributing to DevPulse

## Branching strategy

The project follows a simple and stable branching model:

- main: production-ready code
- develop: integration branch for active development
- feature/*: new features or major additions
- fix/*: corrective changes and bug fixes

## Commit conventions

Use clear, conventional commit messages.

Examples:

- feat: add JWT authentication flow
- fix: correct refresh token expiration handling
- docs: update architecture documentation
- test: add backend controller integration tests
- ci: add security scan stage
- chore: initialize repository structure

## Development workflow

1. create a feature or fix branch from develop
2. implement the smallest relevant change
3. add or update tests
4. update documentation when behavior changes
5. run local validation before opening a PR
6. request review before merging into develop

## Quality requirements

Every change should:

- respect the project architecture
- include relevant tests
- avoid unnecessary abstraction
- avoid exposing secrets
- maintain documentation consistency

## Code review checklist

- business logic is in the correct layer
- security requirements are preserved
- errors are handled explicitly
- public APIs remain documented
- no test is skipped to force a green build
