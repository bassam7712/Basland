# Basland

A minimal Android project scaffold demonstrating how to create a managed
(work profile) sandbox using `DevicePolicyManager`, and how to freeze/hide
apps inside it — the same underlying mechanism apps like Island use.

## What's included
- `BaslandDeviceAdminReceiver.kt` — the required `DeviceAdminReceiver` subclass. Once the managed profile is provisioned, Android promotes this app to **Profile Owner** inside that new profile.
- `MainActivity.kt` — a screen with buttons to:
  - **Create Space**: launches Android's built-in `ACTION_PROVISION_MANAGED_PROFILE` wizard, which creates the sandboxed work profile and installs a copy of Basland inside it automatically.
  - **Destroy Space**: points the user to system settings to remove the work profile (Android does not allow silently deleting a profile from outside it).
  - **Freeze / Unfreeze Demo App**: calls `setPackagesSuspended()` to block/unblock a target package.
  - **Hide / Unhide Demo App**: calls `setApplicationHidden()` to show/hide a target package's launcher icon.
- `device_admin_receiver.xml` — declares which device-admin policies Basland requests (password limits, lock, wipe, etc.).

## Important things to understand before running this

1. **The freeze/hide buttons only work from *inside* the managed profile.**
   After you tap "Create Space," Android automatically installs a second
   copy of Basland inside the new work profile (you'll see a second
   Basland icon on your launcher, usually with a briefcase badge). You
   must open *that* copy to freeze/hide/suspend apps — the copy running
   in your personal profile is not the profile owner and those calls will
   fail with a `SecurityException`.

2. **`demoTargetPackage` is a placeholder.** Edit the value in
   `MainActivity.kt` to the package name of an app actually installed
   inside the work profile before testing freeze/hide (e.g. install a
   test app inside the profile first, via the Play Store icon that
   appears there).

3. **Cloning apps (running two instances of the same app) is not covered
   by DevicePolicyManager alone.** Android's managed profile already
   gives you "cloning" for free in a sense — install the same app inside
   both the personal profile and the work profile, and you get two
   independent instances with separate data/login. True on-the-fly app
   cloning without a second profile requires more advanced techniques
   (virtualization/app-sandboxing libraries) that go beyond the
   `DevicePolicyManager` APIs covered here.

4. **Removing the profile.** From Android 8+, only the profile owner
   (Basland running inside the work profile) can fully wipe itself via
   `dpm.wipeData(0)`, or the user can remove it manually via
   **Settings → Accounts → Remove work profile**. Add a "Destroy Space"
   button *inside* the profile-owner copy that calls `wipeData()` if you
   want a one-tap teardown from within the app.

## Requirements
- Android Studio (Koala/2024.1 or newer recommended)
- JDK 17 (bundled with recent Android Studio)
- A physical device or emulator running Android 7.0 (API 24) or higher.
  Work profile provisioning is unreliable on some emulator images —
  a real device is recommended for testing.

## Setting up the project in Android Studio
1. Open Android Studio → **File → Open** → select the `Basland` project folder.
2. Let Gradle sync (it will download the Android Gradle Plugin and Kotlin plugin versions declared in the root `build.gradle.kts`).
3. Connect a physical device with USB debugging enabled, or start an emulator.
4. Click **Run ▶** to install and launch the debug build.

## Building an APK from Android Studio (GUI)
1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
2. When it finishes, click the "locate" link in the notification, or find it at:
   `app/build/outputs/apk/debug/app-debug.apk`
3. To build a signed **release** APK instead:
   - **Build → Generate Signed Bundle / APK…**
   - Choose **APK**, then **Next**.
   - Click **Create new…** to generate a keystore (or select an existing one), fill in the key details, and continue.
   - Choose the **release** build variant and finish.
   - The signed APK will appear under `app/build/outputs/apk/release/`.

## Building an APK from the command line
From the project root (after Android Studio has generated the Gradle
wrapper — open the project once in Studio first so `gradlew`/`gradlew.bat`
are created):

```bash
# Debug APK (unsigned-for-testing, installable directly via adb)
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK (requires signing config for a distributable build)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

Install directly to a connected device for testing:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Continuous Integration (GitHub Actions)

`.github/workflows/build.yml` automatically builds a debug APK on every push
or pull request to `main` (and can also be triggered manually via the
**Actions** tab → **Run workflow**).

- It installs JDK 17 and Gradle 8.7 directly (via `gradle/actions/setup-gradle`), so it works even before you've generated the Gradle wrapper (`gradlew`) locally.
- It runs `gradle assembleDebug`.
- The resulting APK is uploaded as a workflow artifact named **basland-debug-apk** — download it from the workflow run's summary page under "Artifacts."

**To use it:** push this project to a GitHub repository (including the
`.github/workflows/build.yml` file) and the workflow will run automatically.

**Optional — switch to the Gradle wrapper:** once you've opened the project
in Android Studio at least once, it will generate `gradlew`, `gradlew.bat`,
and `gradle/wrapper/gradle-wrapper.jar`. Commit those, then change the
"Build debug APK" step in the workflow from `gradle assembleDebug` to
`./gradlew assembleDebug` — this pins the exact Gradle version for
reproducible builds instead of relying on a version installed by the action.

## Next steps to build this into the full Basland app
- Replace the single hard-coded `demoTargetPackage` with a `RecyclerView`
  listing all apps installed in the current profile (`PackageManager.getInstalledApplications()`),
  with per-app toggle switches for freeze/hide.
- Add a VPN configuration screen using `DevicePolicyManager.setAlwaysOnVpnPackage()` to scope a VPN to just the managed profile.
- Add an in-app "Destroy Space" action (profile-owner side) calling `dpm.wipeData(0)`.
- Design and add a custom app icon/launcher (`mipmap` resources) — none are included in this scaffold, so Android Studio will use a default icon until you add your own.
