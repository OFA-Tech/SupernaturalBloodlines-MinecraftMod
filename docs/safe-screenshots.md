# Safe dev screenshots (hidden window)

`.\gradlew.bat runSafeShots` runs the client with a hidden, unfocused, undecorated GLFW window (`-Dsupernaturalbloodlines.devshots.hidden=true`, `--width 1920 --height 1080`). Minecraft renders into its framebuffer and `Screenshot.grab` reads that, so the PNGs are the same as from a visible window, with nothing shown, no focus taken and no cursor moved. Design and reasoning: `docs/superpowers/specs/2026-11-17-safe-screenshots.md`; the mixin is in `docs/mixins.md` ("Dev tooling").

- Own game dir `run-safeshots/` (git-ignored). `prepareSafeShots` writes there `config/fml.toml` (`earlyWindowControl = false`) and sets `enableVsync:false`, `pauseOnLostFocus:false`, `fullscreen:false`, `maxFps:60` in `options.txt`. The normal `run/` is never touched. Copy a world named `New World` into `run-safeshots/saves/` (DevShots opens it).
- Hidden mode makes `DevShots` skip `glfwSetWindowSize` and every `glfwSetCursorPos` (also never in an unfocused window). The rules are in `core/DevShotsGate` (unit test `DevShotsGateTest`).
- The usual DevShots properties go in `JAVA_TOOL_OPTIONS`, for example `-Dsupernaturalbloodlines.devshots=grimoire:bloodline -Dsupernaturalbloodlines.devshots.scales=4 -Dsupernaturalbloodlines.devshots.demo=true -Dsupernaturalbloodlines.devshots.quit=true`. Pictures: `run-safeshots/screenshots/devshots/`.

## Tripwire: `scripts/dev/focus-watch.ps1`

Polls every 100 ms the foreground window, the cursor and the visible windows of `java`/`javaw`. On the first change it prints it, stops the run and the new java windows' processes, and exits 2 (0 = clean, else the run's own exit code). Read-only: it never moves, focuses or types.

    pwsh scripts/dev/focus-watch.ps1 -Run '.\gradlew.bat runSafeShots --no-daemon'              # AFK proof: any foreground or cursor change trips
    pwsh scripts/dev/focus-watch.ps1 -JavaOnly -Run '.\gradlew.bat runSafeShots --no-daemon'    # while someone plays: trips only on a java window
    pwsh scripts/dev/focus-watch.ps1 -SelfTest                                                  # rules on made-up snapshots
    pwsh scripts/dev/focus-watch.ps1 -BaselineFg 1 -MaxSec 3                                    # must exit 2: the watcher can fail

### AFK proof (owner away)
1. Notepad in front. Start the guarded run with the mixin **off** (a run without `-Dsupernaturalbloodlines.devshots.hidden=true`, for example the normal `runClient` with DevShots): the watcher must trip (a visible java window and/or a foreground change). This shows it can fail. It steals focus, so only when nobody plays.
2. Same guarded run with `runSafeShots`: exit 0, Notepad still in front, cursor unmoved (strict mode), PNGs 1920x1080 and not black.

## Proof run (2026-11-17, owner playing Valorant fullscreen; run B only, by the owner's choice)
- **Mutation:** `focus-watch.ps1 -BaselineFg 1 -MaxSec 3` pretended another window was in front: `TRIP ... foreground changed: hwnd 1 -> 6818584`, exit 2. `-SelfTest` ok. The mixin-off run A (which steals focus) was not done; do it when the owner is AFK, with Notepad in front.
- **Run B (mixin ON):** `focus-watch.ps1 -JavaOnly -Run '.\gradlew.bat runSafeShots --no-daemon'` with `devshots=grimoire:bloodline`, scale 4, demo, quit. Log: `DEVSHOTS hidden window mixin applied`, the foreground stayed Valorant (pid 44288), no java window became visible, the watcher ended `no change`, exit 0. `-JavaOnly` was used because the owner was playing, so the cursor was not part of the check: the cursor part stays for the AFK proof (strict mode).
- **Picture:** `docs/superpowers/design/safe-shots/grimoire-bloodline-s4.png`, 1920x1080, a real Grimoire Bloodline render (opened and looked at, not black).
- `runGameTestServer` under the same watcher: green, no java window.

## Item art shots (plan 17a)

`-Dsupernaturalbloodlines.devshots.items=sheet,compass,worn` (with `.tag=before|after`, `.quit=true`) makes the client open the world and photograph every item of ours in the GUI, on the ground and in a hand (`sheet`), the Vampire Compass toward eight targets (`compass`) and the three rings on classic and slim players, standing and sneaking (`worn`). Pictures: `run-safeshots/screenshots/itemshots/<tag>-<kind>.png`. Driver: `parts/artifacts/client/ItemShots` (client only, inert without the property). A before picture needs the same driver on the old commit: a scratch worktree of the old commit with `ItemShots` and `RingPose` copied in and one call to `ItemShots.registerIfRequested()` in `ArtifactsClient.onClientSetup`. Do not pipe the run into `Select -First`: the pipe closing ends the game.
