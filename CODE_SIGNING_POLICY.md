# Code signing policy

Free code signing provided by SignPath.io, certificate by SignPath Foundation.

See [SignPath](https://signpath.io/) and the
[SignPath Foundation](https://signpath.org/) for details.

## Scope

The release-signing policy applies only to Frostguard Windows artifacts built
from this repository by the trusted GitHub Actions release workflow. It covers
the Frostguard desktop and watcher launchers and the MSI that contains them.
Bundled third-party Open Source binaries are not signed as Frostguard binaries.

Release signing is restricted to reviewed source on `main`. The signing request
must contain verifiable build origin information for the repository, commit,
branch, and GitHub Actions job. Every release-signing request requires manual
approval before the signed artifact can be published.

## Team roles

- Committers and reviewers: [Shederator](https://github.com/Shederator),
  [CodeLtDave](https://github.com/CodeLtDave), and
  [bizulk](https://github.com/bizulk)
- Signing approver: [CodeLtDave](https://github.com/CodeLtDave)

Repository contributors certify their changes under the Developer Certificate
of Origin. Changes from people without commit access are reviewed before merge,
including changes to build scripts and GitHub Actions workflows.

## Privacy

Frostguard's [Privacy Policy](PRIVACY.md) describes all optional telemetry and
affected third-party services. Mixpanel telemetry is disabled by default and is
sent only after the user explicitly enables the named option in Frostguard.
