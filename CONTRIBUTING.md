# Contributing

This is mainly a portfolio project rather than something with an active roadmap, but if you spot a bug or want to clean something up, PRs are welcome.

## Filing an issue

- Check existing issues first so we're not duplicating.
- Say which Android version and device/emulator you're on, and how to reproduce the problem.
- Don't post passwords, Firebase credentials, real student data, or anything about live hardware access — even by accident.

## Sending a PR

1. Keep it focused on one thing, and explain what problem it solves.
2. Stick to the existing Java/XML style already in the codebase.
3. Run `./gradlew test assembleDebug` locally if you can before opening the PR.
4. Update the README/screenshots if you changed anything user-facing.
5. No real Firebase config, signing keys, production credentials, or private datasets — ever.

## One line on scope

If a change makes the door-control side more "real," it needs to explain why it's safe, and it can't imply the current demo protocol is production-grade — because it isn't. See [SECURITY.md](SECURITY.md).
