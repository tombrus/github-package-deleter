# github-package-deleter

Handwritten Java Swing app to browse and delete packages/versions from the GitHub
package registry. No build tool: IntelliJ compiles it, `makeMacApp.sh` bundles it.

## Layout
- `src/com/tombrus/githubPackageDeleter/` - all sources, one package
- `GithubPackageDeleter.java` - main class, UI wiring; `.form` is the IntelliJ GUI designer file
- `Details.java` + `*Details.java` - tree nodes
- `*Lister.java`, `GraphQL.java`, `Paginator.java` - GitHub GraphQL access
- `lib/` - mvg-json jar

## Dependencies
- `lib/mvg-json-1.6.3.jar` - vendored, not on Maven Central
- `~/.m2/repository/com/formdev/flatlaf/3.7.2/flatlaf-3.7.2.jar`
- `~/.m2/repository/com/intellij/forms_rt/7.0.3/forms_rt-7.0.3.jar` - only for
  command-line compiling; IntelliJ itself uses the `forms_rt.jar` it ships

Versions are pinned in three places that must stay in sync: `makeMacApp.sh`,
`.idea/libraries/*.xml` and `.idea/misc.xml` (language level + project JDK).

## Build / run
Java 25 (current LTS). `makeMacApp.sh` needs `out/artifacts/github-package-deleter-jar/`
built by IntelliJ first, then jlinks a JDK 25 runtime, jpackages the app and copies it
to the Desktop.

## Gotchas
- The popup menu on the tree must be driven by `MouseEvent.isPopupTrigger()` from
  `mousePressed`/`mouseReleased`, never by `mouseClicked`. AWT only synthesizes
  MOUSE_CLICKED when press and release land on the same pixel, so a physical mouse
  that jitters never delivers it, while a trackpad click does.
- FlatLaf loads a native library for the macOS window decorations. Without
  `--enable-native-access=ALL-UNNAMED` JDK 25 prints a JEP 472 warning and a future
  JDK will block it, so `makeMacApp.sh` passes it as a `--java-options`.
- jlink wants `--compress=zip-6`; the old numeric `--compress=2` is deprecated.
- The bundled runtime is jlinked with `--strip-native-commands`, so the app has no
  `bin/java`; use a separately installed JDK 25 to reproduce its behaviour.
