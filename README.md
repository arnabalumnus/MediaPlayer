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
  - Custom Compose transport controls: play/pause, ±10s seek.
  - Left-half vertical drag = screen brightness, right-half vertical drag =
    media volume, tap = show/hide controls.

## Notes / interpretive calls

The brief used a couple of ambiguous terms I resolved as follows — flag if you
meant something different:

- **"ScreenOnscreen feature"** → keep-screen-on toggle during video playback.
- **"Cust screen feature"** → a custom aspect-ratio/scale toggle (Fit/Fill/Crop)
  on the video surface.
- Video "backward"/"forward" controls → ±10 second seek buttons (there's no
  video queue/playlist — only the audio tab has next/previous track).

## Permissions

Runtime permissions (`READ_MEDIA_AUDIO`/`READ_MEDIA_VIDEO` on API 33+, else
`READ_EXTERNAL_STORAGE`, plus `POST_NOTIFICATIONS` on API 33+) are requested
from the dashboard before it scans storage.
