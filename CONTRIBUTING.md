# Contributing to CloudFlow

## Development Setup

1. Fork the repo and clone locally
2. Follow the [Quick Start](README.md#quick-start) guide
3. Create a feature branch: `git checkout -b feat/your-feature`

## Code Style

- Java: standard Spring Boot conventions, Lombok for boilerplate
- React: functional components only, hooks for state
- Commits: conventional commits (`feat:`, `fix:`, `ci:`, `docs:`)

## Pull Request Process

1. Ensure unit tests pass: `./mvnw test -Dtest="CronValidatorTest,AuthServiceTest,JobServiceTest"`
2. Add tests for new functionality
3. Update README if adding new env vars or endpoints
4. PR title follows conventional commits format

## Cron Expression Format

CloudFlow uses 6-field Quartz cron (not 5-field Unix cron):
```
Seconds Minutes Hours DayOfMonth Month DayOfWeek
0       *       *     *          *     ?
```