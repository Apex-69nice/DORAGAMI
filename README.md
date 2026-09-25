# DORAGAMI

A warm, paper-like screen tint for Android — your own version of Windows
Night Light / the Daylight DC-1 look, toggled on and off from a simple app.


A system-wide warm screen tint for Android, toggled from a simple app —
your own version of Windows Night Light / the Daylight DC-1 warm-paper look.

## How it works

A foreground `Service` draws a full-screen, semi-transparent, warm-colored
window (`TYPE_APPLICATION_OVERLAY`) on top of everything else on the device.
It doesn't intercept touches or typing, so it's purely visual. The `SeekBar`
in the app controls how strong (opaque/orange) that tint is, from a faint
amber wash up to a deep orange. This is the same technique apps like
Twilight or Night Owl use — Android doesn't let a normal app change the
display's actual color temperature/gamma the way desktop Windows can,
so a color overlay is the closest software-only equivalent.

## Set up the project (one-time)

1. Install Android Studio if you don't have it.
2. **File → New → New Project → Empty Views Activity**
   - Name: `WarmLight`
   - Package name: `com.nikhil.warmlight`
   - Language: **Kotlin**
   - Minimum SDK: **API 26 (Android 8.0)**
   - Finish, let it sync once.
3. Copy the files from this `app/` folder into your new project's `app/`
   folder, overwriting the generated `build.gradle.kts`,
   `AndroidManifest.xml`, `MainActivity.kt`, `activity_main.xml`, and
   `strings.xml`. Delete the generated `MainActivity.kt` first if Android
   Studio complains about a duplicate class.
4. Add the new file `OverlayService.kt` into the same package folder as
   `MainActivity.kt` (`app/src/main/java/com/nikhil/warmlight/`).
5. Sync Gradle (the elephant/sync icon, or it'll prompt automatically).

## Run it on the Tab S7

1. On the tablet: **Settings → About tablet → tap "Build number" 7 times**
   to unlock Developer Options, then **Settings → Developer options → USB
   debugging** (or set up Wireless debugging).
2. Connect it to Android Studio and hit Run.
3. First time you flip the switch on, Android will send you to a
   **"Display over other apps"** permission screen — allow it for WarmLight,
   then come back (the app resumes the toggle automatically).
4. On Android 13+, you'll also get a one-time notification-permission
   prompt (needed for the small "overlay is on" notification).
5. Drag the Warmth slider — the overlay updates live, on every screen,
   even outside the app. Turn it off from the switch or straight from the
   notification.

## Ideas to extend it later
- A schedule (auto-on at sunset, auto-off at sunrise) using `AlarmManager`.
- Auto-restore state on reboot with a `BOOT_COMPLETED` receiver — note
  Android 15+ added restrictions on which foreground services a boot
  receiver is allowed to start, so this needs a bit of testing on your
  exact tablet's Android version.
- A quick-settings tile so you can toggle it from the notification shade
  without opening the app.
