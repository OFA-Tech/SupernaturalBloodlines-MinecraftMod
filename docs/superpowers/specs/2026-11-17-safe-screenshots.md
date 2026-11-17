# Safe dev screenshots (no focus, no cursor, no visible window)

Research only; nothing here was run. Items marked (memory) are from knowledge of the 1.21.1 / GLFW 3.4 source and must be confirmed in the proof run below.

## Machine
`[Screen]::AllScreens`: DISPLAY1 primary 2560x1440 at (0,0); DISPLAY2 1920x1080 at (2560,0). Monitor 2 is exactly 1080p, the size DevShots already asks for.

## What DevShots does today (`parts/supernatural/client/hub/DevShots.java`)
- Line 130: `glfwSetWindowSize(window, 1920, 1080)` on the first tick. This resizes a visible window.
- Lines 167, 171, 181: `glfwSetCursorPos`. GLFW ignores this call unless the window is focused (`input.c`: `if (!windowFocused(window)) return;`) (memory), so it only moves the real cursor if our window stole focus.
- No Win32 calls anywhere in the repo (grep for SetCursorPos, SetWindowPos, SetForegroundWindow, SendInput: nothing). The harm is in the window being created, shown and focused.
- `run/options.txt` already has `fullscreen:false` and `pauseOnLostFocus:false`, so an unfocused window does not pause the game or open the pause menu.
- `run/config/fml.toml`: `earlyWindowControl=true`, provider `fmlearlywindow`. NeoForge creates the GLFW window early, visible, for the loading screen, and vanilla `Window` reuses it. Any hint set in `Window.<init>` is too late for that window.

## Options considered
| Option | Verdict |
|---|---|
| `options.txt` `fullscreenResolution` | A video-mode string for the monitor, not a monitor choice. 1.21.1 `Window.<init>` opens on the primary monitor. `setMode()` (F11) uses `ScreenManager.findBestMonitor(window)`, which is the monitor with the largest overlap with the window (memory). So fullscreen on monitor 2 means moving the window to x>=2560 first. That needs a visible window. |
| `GLFW_FOCUS_ON_SHOW=false`, `GLFW_FOCUSED=false` | These work but only on a window we create. They are not in the vanilla `Window` and are not settable by an LWJGL property. They need a mixin and `earlyWindowControl=false`. A visible window on monitor 2 can still cause Windows to flash or reorder things around a fullscreen game on monitor 1. |
| `glfwSetWindowMonitor` on monitor 2 | `ChangeDisplaySettingsEx` and topmost fullscreen on the second display. It is the likeliest to disturb Valorant (display change, a window that Windows may activate). Reject. |
| Start minimized / `SW_SHOWNOACTIVATE` / foreground lock | Cannot be controlled from outside: Java creates the window itself and GLFW calls `ShowWindow(SW_SHOW)` + `SetForegroundWindow`. A minimized GLFW window has a 0x0 framebuffer and breaks rendering. The foreground-lock timeout is only a partial protection (flash, not guaranteed). Reject. |
| **Hidden window (`GLFW_VISIBLE=false`) + FBO capture** | **Recommended.** |

## Recommendation: invisible GLFW window, capture from the render target
Minecraft draws the world into `mc.getMainRenderTarget()` (an FBO) and `Screenshot.grab` reads that FBO. It never reads the screen, so a hidden window gives identical pixels. A hidden window gets no focus, no cursor, no taskbar entry and no monitor. GLFW supports a GL context on an invisible window. Requirement 1 (fullscreen on monitor 2) is replaced by an invisible 1920x1080 window: monitor 2 is not needed because nothing is shown. If the owner insists on seeing it, fall back to the visible variant in "Fallback".

### Code needed (dev only, all inert without the property)
1. **Mixin** `client/mixin/DevShotsWindowMixin` on `com.mojang.blaze3d.platform.Window`, registered in a new `supernaturalbloodlines.devshots.mixins.json` (`"client": [...]`, same pattern as the existing `*.mixins.json`) and added as a `[[mixins]]` entry in `src/main/templates/META-INF/neoforge.mods.toml`.
   ```java
   @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
   private long devshots$hidden(int w, int h, CharSequence t, long monitor, long share, Operation<Long> orig) {
       if (Boolean.getBoolean("supernaturalbloodlines.devshots.hidden")) {
           GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
           GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
           GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
           GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE); // client area is exactly w x h
       }
       return orig.call(w, h, t, monitor, share);
   }
   ```
   The hints go right before `glfwCreateWindow` because vanilla calls `glfwDefaultWindowHints()` just before. If the target descriptor does not match in 1.21.1, check it with the remapped `Window` class; this is the only fragile line.
2. **`run/config/fml.toml`**: `earlyWindowControl = false` (or `earlyWindowProvider = "dummy"`) for the screenshot run only, so vanilla `Window` creates the window instead of the early (visible) one. Keep a separate game dir for the shot run (`--gameDir`/a copied `run`) so the normal config is not touched.
3. **Run args**: `--width 1920 --height 1080` (no resize needed) and `options.txt` `enableVsync:false` (a hidden window may not be paced by DWM; `maxFps:60` is enough) plus `pauseOnLostFocus:false` (already set).
4. **`DevShots.java`**: delete the `glfwSetWindowSize` block (line 129-132) when `.hidden=true`, and guard the three `glfwSetCursorPos` calls with `glfwGetWindowAttrib(win, GLFW_FOCUSED) == GLFW_TRUE`. An unfocused window already no-ops them, and `MouseHandler` stays at (0,0), which is what the code wants (no tooltip). Optional `.hidden` read for logging.

### Risks
- Hidden-window GL on this driver is not proven; if the first frame is black, test with `GLFW_VISIBLE` on but `GLFW_DECORATED=false`, window at (2560,0) (the fallback).
- `earlyWindowControl=false` can disable some GL features (comment in `fml.toml`); the hub UI uses none, but check the log for GL errors.
- The mixin descriptor and MixinExtras `@WrapOperation` availability (NeoForge 21.1 ships MixinExtras) need confirming.
- `Screenshot.grab` is async: wait for the file (the driver already logs `DEVSHOTS name`).

### Fallback (visible but not focused, on monitor 2)
Same mixin, but `VISIBLE=true`, `FOCUSED=false`, `FOCUS_ON_SHOW=false`, `DECORATED=false`, then `glfwSetWindowPos(win, 2560, 0)` in the first tick with the 1920x1080 window. Do not call `glfwSetWindowMonitor` or F11. Windows can still raise the new window, so this variant must pass the same proof test and must not be run during the owner's game until it has.

## Proof that it does not steal focus (run only when the owner is AFK, never during Valorant)
A watcher script, started before `gradlew runClient`, polls every 20 ms until the java process exits and writes a CSV:
- `GetForegroundWindow()` handle, its `GetWindowThreadProcessId` pid and title, logged on every change.
- `GetCursorPos()`, logged on every change.
- `EnumWindows` + `IsWindowVisible` for the java pid (and its Gradle child), logged on every change.
Setup: a Notepad window in front on monitor 1 as the stand-in "game".
Pass: (a) foreground stays Notepad for the whole run, no entry for the java pid; (b) cursor trace has no jump to (1,1) or (0,0) from the run; (c) the visible-window list for the java pid stays empty (hidden variant) or never gets the foreground flag (fallback); (d) all `devshots/*.png` exist and are 1920x1080 and not black. Also add `mutation`: run once with the mixin disabled, the watcher must then record a java foreground change or a visible window, which proves the watcher can fail.
Then the shot run can happen during the owner's game, with the same watcher kept on as a tripwire that kills the java process (`Stop-Process`) on the first foreground change to java.
