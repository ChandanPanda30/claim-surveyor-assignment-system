# Branch Policy

## Long-lived branches

| Branch | Purpose |
|--------|---------|
| `main` | Always release-ready. Only receives merges from `develop` and tags. |
| `develop` | Integration branch. All feature branches merge here through a pull request. |

## Short-lived branches

Branch off `develop` and use these prefixes:

| Pattern | Use | Example |
|---------|-----|---------|
| `feature/<short-name>` | New functionality | `feature/claim-submission` |
| `bugfix/<short-name>` | Fix found during development | `bugfix/date-validation` |
| `hotfix/<short-name>` | Urgent fix branched from `main` | `hotfix/health-check` |
| `chore/<short-name>` | Build, CI, docs, tooling | `chore/jenkinsfile` |

Use lowercase and hyphens only.

## Commit messages

Format: `<type>: <what changed>` in the imperative mood.

Types: `feat`, `fix`, `docs`, `test`, `build`, `ci`, `refactor`, `chore`.

Examples:
- `feat: add claim submission form with validation`
- `test: cover claim service number generation`
- `ci: add Jenkinsfile with build and package stages`

## Pull requests

1. Open the PR into `develop`, linking the issue (`Closes #n`).
2. At least one review comment or approval before merge.
3. Build and tests must pass (`mvn verify`).
4. Merge with a merge commit so the feature history stays visible.

## Releases

Merge `develop` into `main`, then tag: `git tag -a v0.1.0 -m "MVP baseline"`.
