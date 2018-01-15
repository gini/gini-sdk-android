# Release Process

This document describes the release process for a new version of the Gini SDK for Android.

1. Add new features only in separate `feature` branches
2. Merge `feature` branches into `master`
3. Update `baseVersion` in `ginisdk/gradle.properties`
4. Update the version in the snippets in `Readme.md` and `ginisdk/src/doc/source/guides/getting-started.rst`
4. Add entry to changelog with version and date
6. Tag `master` branch with the same version used in 3
7. Push all branches to remote including tags
8. Wait for Jenkins to pass and confirm the release
9. Done!
