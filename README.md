# MediaPlayer

A Jetpack Compose Android app that scans local storage for audio (`Music/`) and
video (`Movies/`) and plays them back.

## Opening the project

1. Open this folder in Android Studio (Koala/Ladybug or newer) and let it sync.
2. `local.properties` points at an SDK install path — if Android Studio doesn't
   already know your SDK location, update `sdk.dir` there.
3. Run the `app` configuration on a device/emulator running API 26+.

The project was verified to build from the command line with
`./gradlew assembleDebug` (Gradle 8.13, AGP 8.5.2, JDK 17/21, Android SDK 34).

## Design

The UI runs on a custom indigo/coral brand theme (`ui/theme/Color.kt`,
`Shapes.kt` — dynamic-color is intentionally disabled so the look stays
consistent across devices) with generously rounded corners everywhere and a
"liquid glass" frosted look on floating surfaces, built on the
[Haze](https://github.com/chrisbanes/haze) library (real backdrop blur, not a
fake translucent-gradient approximation):

- `ui/components/Glass.kt` — `Modifier.glassSource(hazeState)` marks content
  to be blurred; `Modifier.glassPanel(hazeState, shape, tint)` is the frosted
  panel itself (wraps Haze's `HazeMaterials.thin` preset).
- Used on: the dashboard's floating pill-shaped bottom tab bar (`Audio`/`Video`
  icons, no text — swipe on the pager still works, the pill just shows/reflects
  the current page) over the scrolling list; the video player's top bar,
  bottom seek bar, and center play button over the video itself.
- List/grid tiles (`ui/components/MediaItemViews.kt`) use a lighter translucent
  `Card` treatment rather than live blur (they're not floating over moving
  content, so a static frosted look reads the same without the performance
  cost of blurring dozens of list items).
- Edge-to-edge is enabled on every screen (`enableEdgeToEdge()` / manual
  `WindowCompat` insets) so the gradient backgrounds and glass panels run
  under the status/navigation bars instead of behind a solid bar.
- The launcher icon (`res/drawable/ic_launcher_*.xml`) is a gradient adaptive
  icon with a play-glyph foreground, plus a `<monochrome>` layer for Android
  13+ themed icons.

## Structure

- `MainActivity` — dashboard with **Audio** / **Video** tabs (swipeable via
  `HorizontalPager`, selected via a floating glass icon-only tab bar pinned to
  the bottom), each with a list/grid toggle.
- `data/MediaRepository` — queries `MediaStore` for audio restricted to the
  `Music` folder (`audio/mpeg`) and video restricted to the `Movies` folder
  (`video/mp4`, `video/mpeg`, `video/3gpp`, etc).
- `playback/AudioPlaybackService` — a Media3 `MediaSessionService` hosting the
  audio `ExoPlayer`. Media3 automatically raises the foreground
  play/pause/skip notification while a session is active; `AudioPlayerActivity`
  talks to it through a `MediaController`, so audio keeps playing if you leave
  the player screen or the app.
- `ui/audio/AudioPlayerActivity` — Now Playing screen (art, seek bar,
  previous/play-pause/next), backed by the session above.
- `ui/video/VideoPlayerActivity` — dedicated video player with:
  - **Fullscreen mode** — hides system bars and forces landscape.
  - **Keep-screen-on** — toggles `FLAG_KEEP_SCREEN_ON` for the window.
  - **Aspect-ratio ("custom screen") toggle** — the icon+label control in the
    top bar (shows "Fit"/"Fill"/"Crop") cycles the `PlayerView`'s resize mode;
    a transient "Screen: …" banner confirms the change.
  - **Picture-in-picture** — a dedicated top-bar button enters PiP on demand,
    and pressing Home/Recents while a video is playing auto-enters PiP too.
    The system's floating PiP window gets a working play/pause action (wired
    through a `BroadcastReceiver`); the in-app overlay controls/gestures are
    hidden while in PiP since the window is too small for them.
  - **Cast to TV** — a `MediaRouteButton` in the top bar opens the standard
    Google Cast device picker; picking a Chromecast-built-in Android TV on the
    same Wi-Fi hands the currently playing video off to it. See "Casting" below
    for how this actually works and what's needed to test it.
  - Custom Compose transport controls: play/pause, ±10s seek — these route to
    the remote device instead of the local player while casting.
  - Left-half vertical drag = screen brightness, right-half vertical drag =
    media volume, tap = show/hide controls (these still control the phone,
    not the TV, while casting).

## Casting

`cast/` implements "cast to TV" using the Google Cast SDK:

- `CastOptionsProviderImpl` registers the default media receiver with the Cast
  framework (declared in the manifest's `OPTIONS_PROVIDER_CLASS_NAME` meta-data).
- `LocalHttpMediaServer` (built on NanoHTTPD) serves the currently playing
  video's bytes over plain HTTP on the phone's LAN address — a Cast receiver
  can't resolve our `content://` URI, it needs an HTTP URL it can fetch itself.
  It supports `Range` requests so the TV can seek.
- `CastController` owns the `CastSession`/`SessionManager` lifecycle: once a
  session connects it loads the HTTP URL onto the receiver (resuming from
  wherever local playback was), and forwards play/pause/seek to the remote
  `RemoteMediaClient` instead of the local `ExoPlayer`.
- `CastButton` wraps the framework's `MediaRouteButton`; it renders the device
  picker and swaps its own connected/disconnected icon automatically.

**To actually test this** you need: a phone and an Android TV (or any
Chromecast-built-in device) on the *same* Wi-Fi network — casting relies on
mDNS discovery over the LAN, so it won't find anything across mobile data,
a guest network, or client-isolated Wi-Fi. On API 33+ the app requests
`NEARBY_WIFI_DEVICES` at launch (declined permission just means no devices
show up in the picker). I couldn't verify this end-to-end here — no physical
TV/Cast receiver in this environment — so I've confirmed it *compiles and
wires up correctly*, but real-device testing is worth doing before you rely
on it. If the cast icon does nothing at all, check `adb logcat` for the
`CastController`/`VideoPlayerActivity` warnings it logs (outdated Play
Services, LAN IP not resolvable, port already in use, etc).

## Notes / interpretive calls

The brief used a couple of ambiguous terms I resolved as follows — flag if you
meant something different:

- **"ScreenOnscreen feature"** → keep-screen-on toggle during video playback.
- **"Cust screen feature"** → originally implemented as an aspect-ratio/scale
  toggle (Fit/Fill/Crop); per a later request this was actually meant as
  **"Cast screen"** and is now the Cast-to-TV feature described above. The
  aspect-ratio toggle is still there too (in the top bar, next to the title).
- Video "backward"/"forward" controls → ±10 second seek buttons (there's no
  video queue/playlist — only the audio tab has next/previous track).

## Permissions

Runtime permissions (`READ_MEDIA_AUDIO`/`READ_MEDIA_VIDEO` on API 33+, else
`READ_EXTERNAL_STORAGE`, plus `POST_NOTIFICATIONS` on API 33+) are requested
from the dashboard before it scans storage.
