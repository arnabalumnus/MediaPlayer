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

## Structure

- `MainActivity` — dashboard with **Audio** / **Video** tabs (swipeable via
  `HorizontalPager`, kept in sync with `TabRow`), each with a list/grid toggle.
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
  - **Cast to TV** — a Cast icon in the top bar opens a device picker; picking
    a DLNA-capable renderer on the same Wi-Fi hands the currently playing video
    off to it. See "Casting" below for how this works and what it needs.
  - Custom Compose transport controls: play/pause, ±10s seek — these route to
    the remote device instead of the local player while casting.
  - Left-half vertical drag = screen brightness, right-half vertical drag =
    media volume, tap = show/hide controls (these still control the phone,
    not the TV, while casting).

## Casting

Casting went through two implementations. Google Cast (`play-services-cast-framework`
+ `MediaRouteButton`) was tried first, but live testing showed the target TV
never answers `_googlecast._tcp` — it has no Chromecast support at all. `dlna/`
replaces it with a hand-rolled DLNA/UPnP sender, which most smart TVs (Samsung's
Tizen sets included) support natively without any Chromecast hardware:

- `SsdpDiscovery` sends SSDP `M-SEARCH` (UDP multicast) to find UPnP devices,
  fetches each candidate's device-description XML, and keeps the ones that
  expose an `AVTransport` service — that's the actual capability needed
  (checking for it directly is more reliable than trusting a vendor's root
  `deviceType` string, which varies and can bury the renderer as a sub-device).
  Search is re-sent every ~2s across a 6s window since SSDP-over-UDP is lossy.
- `AvTransportClient` sends the UPnP SOAP actions (`SetAVTransportURI`, `Play`,
  `Pause`, `Seek`, `GetPositionInfo`, `GetTransportInfo`) to the renderer's
  control URL.
- `LocalHttpMediaServer` (NanoHTTPD) serves the currently playing video's bytes
  over plain HTTP on the phone's LAN address with `Range` support — a renderer
  can't resolve our `content://` URI, it needs an HTTP URL it can fetch itself.
- `DlnaController` orchestrates discovery/connect and forwards play/pause/seek
  to the renderer instead of the local `ExoPlayer` once casting starts.
- `DlnaDeviceDialog` is a plain Compose `AlertDialog` device picker (no
  `MediaRouteButton`/AppCompat dependency needed for this approach).

**Note on cleartext HTTP**: both the SSDP description fetch and the SOAP calls
use plain `http://` to an IP discovered at runtime, which Android blocks by
default since API 28. The manifest sets `android:usesCleartextTraffic="true"` —
there's no way to scope a Network Security Config tighter here since the
renderer's IP isn't known ahead of time.

**Verified against a real TV, not just compiled**: this was tested live against
the user's actual Samsung TV on their real network via an attached device. SSDP
discovery reliably found the TV, but its only advertised service turned out to
be `urn:dial-multiscreen-org:device:dialreceiver:1` (DIAL — what YouTube/Netflix
use to launch their own TV app) with no `AVTransport` service, so it can't
receive an arbitrary video URL from a third-party app. That's most likely what
"other app casting" was actually using. If your TV has a "Smart View" / "Screen
Mirroring" / "Device Connect Manager" toggle that's off, enabling it may expose
a proper renderer service and make it discoverable; otherwise a Chromecast or
Android TV/Google TV streaming device (most of which speak DLNA too) will work
immediately with this same code.

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
