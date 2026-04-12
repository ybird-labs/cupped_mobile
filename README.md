This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

### Font Awesome Scripts

Install the Python dependencies for the Font Awesome tooling:

```shell
python3 -m pip install -r scripts/fontawesome/requirements.txt
```

The thin-script contract is:
- Font Awesome kit SVGs are the source of truth.
- `scripts/fontawesome/icons.json` only controls app naming and accessibility labels.
- iOS copies SVGs into `xcassets` without rewriting geometry.
- Android preserves the SVG viewport and scales only the intrinsic `dp` size to fit a 24dp max side.

Regenerate the checked-in iOS and Android icon assets from `scripts/fontawesome/icons.json`:

```shell
python3 scripts/fontawesome/import_mobile_icons.py
```

Run the font tooling tests:

```shell
python3 -m unittest scripts.fontawesome.test_import_mobile_icons
```

Validate that the manifest, Swift icon mappings, and Android icon mappings stay in sync:

```shell
python3 scripts/fontawesome/validate_icon_mappings.py
```
