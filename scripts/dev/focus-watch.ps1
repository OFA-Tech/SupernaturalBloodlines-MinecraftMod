<#
.SYNOPSIS
  Tripwire for screenshot runs: fails if the foreground window or the cursor changes, or a java window becomes visible.

.DESCRIPTION
  Polls GetForegroundWindow, GetCursorPos and IsWindowVisible (for the windows of the java/javaw processes) every -IntervalMs.
  On the first change it prints what changed, stops the guarded run and the java processes it started (its process tree, never anyone else's), and exits 2.
  Exit codes: 0 = nothing changed (or the guarded command's own exit code), 2 = tripped, 1 = bad use or failed self test.
  Read-only toward the desktop: it never moves the cursor, focuses or sends keys.

  -Run '<command>'  start the command (for example '.\gradlew.bat runSafeShots') and guard it until it ends.
  -JavaOnly         trip only when the foreground window is a java window or a java window becomes visible. Use this while someone
                    plays: their own alt-tabbing and mouse are then ignored. Without it any foreground or cursor change trips
                    (the AFK proof).
  -BaselineFg <h>   pretend window <h> was in front at the start: the watcher must trip on the first poll (proves it can fail).
  -SelfTest         check the decision rules on made-up snapshots (no desktop access) and exit.
#>
param(
    [string]$Run,
    [string[]]$Process = @('javaw', 'java'),
    [int]$IntervalMs = 100,
    [int]$MaxSec = 3600,
    [switch]$JavaOnly,
    [switch]$SelfTest,
    [long]$BaselineFg = 0
)

# A snapshot: Fg (hwnd), FgPid, Cursor ("x,y"), Visible (hwnd -> pid of the visible windows owned by the watched processes).
# Returns the first change as text, or $null.
function Get-Trip($base, $now, [bool]$javaOnly, $javaPids) {
    foreach ($h in $now.Visible.Keys) {
        if (-not $base.Visible.ContainsKey($h)) { return "java window became visible: hwnd $h (pid $($now.Visible[$h]))" }
    }
    if ($now.Fg -ne $base.Fg) {
        if (-not $javaOnly -or $javaPids -contains $now.FgPid) { return "foreground changed: hwnd $($base.Fg) -> $($now.Fg) (pid $($now.FgPid) $($now.FgName))" }
    }
    if (-not $javaOnly -and $now.Cursor -ne $base.Cursor) { return "cursor moved: $($base.Cursor) -> $($now.Cursor)" }
    return $null
}

# The ids of a process and of everything it started: rows are objects with ProcessId and ParentProcessId. Pure.
function Get-TreeIds($rows, [int]$rootId) {
    $ids = [System.Collections.Generic.HashSet[int]]::new()
    [void]$ids.Add($rootId)
    do {
        $grew = $false
        foreach ($r in $rows) {
            if ($ids.Contains([int]$r.ParentProcessId) -and $ids.Add([int]$r.ProcessId)) { $grew = $true }
        }
    } while ($grew)
    @($ids)
}

# The java processes to stop on a trip: those that are both a watched java process and inside the guarded run's tree. A java window that is not ours is reported, never returned. Pure.
function Get-StopSet($newPids, [int]$fgPid, $javaPids, $tree) {
    @($newPids + $fgPid | Where-Object { $javaPids -contains $_ -and $tree -contains $_ } | Select-Object -Unique)
}

if ($SelfTest) {
    $rows = @(
        [pscustomobject]@{ ProcessId = 2; ParentProcessId = 1 }, [pscustomobject]@{ ProcessId = 3; ParentProcessId = 2 },
        [pscustomobject]@{ ProcessId = 10; ParentProcessId = 9 }, [pscustomobject]@{ ProcessId = 11; ParentProcessId = 1 })
    $tree = Get-TreeIds $rows 1
    $failTree = @()
    if (($tree | Sort-Object) -join "," -ne "1,2,3,11") { $failTree += "the tree of 1 was $($tree -join ",")" }
    if ($tree -contains 10 -or $tree -contains 9) { $failTree += "a process outside the guarded run was in its tree" }
    $b = @{ Fg = 10; FgPid = 1; FgName = 'notepad'; Cursor = '5,5'; Visible = @{} }
    function CloneSnap($o) { @{ Fg = $o.Fg; FgPid = $o.FgPid; FgName = $o.FgName; Cursor = $o.Cursor; Visible = @{} + $o.Visible } }
    $fail = @()
    if (Get-Trip $b (CloneSnap $b) $false @(99)) { $fail += 'unchanged must not trip' }
    $n = CloneSnap $b; $n.Cursor = '0,0'
    if (-not (Get-Trip $b $n $false @(99))) { $fail += 'cursor must trip' }
    if (Get-Trip $b $n $true @(99)) { $fail += 'cursor must not trip with -JavaOnly' }
    $n = CloneSnap $b; $n.Fg = 11; $n.FgPid = 2
    if (-not (Get-Trip $b $n $false @(99))) { $fail += 'foreground must trip' }
    if (Get-Trip $b $n $true @(99)) { $fail += 'other foreground must not trip with -JavaOnly' }
    $n.FgPid = 99
    if (-not (Get-Trip $b $n $true @(99))) { $fail += 'java foreground must trip with -JavaOnly' }
    $n = CloneSnap $b; $n.Visible[77] = 99
    if (-not (Get-Trip $b $n $true @(99))) { $fail += 'new visible java window must trip' }
    $b2 = CloneSnap $b; $b2.Visible[77] = 99
    if (Get-Trip $b2 (CloneSnap $b2) $false @(99)) { $fail += 'a java window visible from the start is the baseline' }
    # the player's own java (pid 50, in front and visible) is not in the guarded run's tree: it must never be stopped
    $stop = Get-StopSet @(50, 3) 50 @(3, 50) @(1, 2, 3)
    if ($stop -contains 50) { $fail += 'a java pid outside the guarded tree was in the stop set' }
    if (($stop -join ',') -ne '3') { $fail += "the stop set was $($stop -join ',') not 3" }
    if (@(Get-StopSet @() 99 @(99) @()).Count -ne 0) { $fail += 'an empty tree must stop nothing' }
    $fail += $failTree
    if ($fail) { $fail | ForEach-Object { Write-Host "SELFTEST FAIL: $_" }; exit 1 }
    Write-Host 'SELFTEST ok'
    exit 0
}

if (-not ('FocusWatchNative' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
public static class FocusWatchNative {
    [StructLayout(LayoutKind.Sequential)] public struct POINT { public int X, Y; }
    delegate bool EnumProc(IntPtr h, IntPtr l);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool GetCursorPos(out POINT p);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
    [DllImport("user32.dll")] static extern bool EnumWindows(EnumProc cb, IntPtr l);
    public static uint PidOf(IntPtr h) { uint pid; GetWindowThreadProcessId(h, out pid); return pid; }
    // hwnd and pid of every visible top-level window
    public static List<long[]> VisibleWindows() {
        var list = new List<long[]>();
        EnumWindows((h, l) => { if (IsWindowVisible(h)) list.Add(new long[] { h.ToInt64(), PidOf(h) }); return true; }, IntPtr.Zero);
        return list;
    }
}
'@
}

function Get-JavaPids { @(Get-Process -Name $Process -ErrorAction SilentlyContinue | ForEach-Object { $_.Id }) }

function Get-Snapshot {
    $javaPids = Get-JavaPids
    $fg = [FocusWatchNative]::GetForegroundWindow()
    $fgPid = [int][FocusWatchNative]::PidOf($fg)
    $pt = New-Object FocusWatchNative+POINT
    [void][FocusWatchNative]::GetCursorPos([ref]$pt)
    $visible = @{}
    foreach ($w in [FocusWatchNative]::VisibleWindows()) { if ($javaPids -contains [int]$w[1]) { $visible[$w[0]] = [int]$w[1] } }
    $name = (Get-Process -Id $fgPid -ErrorAction SilentlyContinue).ProcessName
    @{ Fg = $fg.ToInt64(); FgPid = $fgPid; FgName = $name; Cursor = "$($pt.X),$($pt.Y)"; Visible = $visible }
}

$base = Get-Snapshot
if ($BaselineFg) { $base.Fg = $BaselineFg } # tripwire check without touching the desktop: pretend a different window was in front
$child = $null
if ($Run) { $child = Start-Process -FilePath pwsh -ArgumentList '-NoProfile', '-Command', $Run -PassThru -NoNewWindow }
Write-Host "focus-watch: baseline foreground pid $($base.FgPid) ($($base.FgName)), cursor $($base.Cursor), watching $($Process -join '/')$(if ($JavaOnly) { ' (-JavaOnly)' })"

$deadline = (Get-Date).AddSeconds($MaxSec)
while ((Get-Date) -lt $deadline -and -not ($child -and $child.HasExited)) {
    Start-Sleep -Milliseconds $IntervalMs
    $now = Get-Snapshot
    $trip = Get-Trip $base $now $JavaOnly (Get-JavaPids)
    if ($trip) {
        Write-Host "focus-watch TRIP at $(Get-Date -Format HH:mm:ss.fff): $trip"
        $new = @($now.Visible.Keys | Where-Object { -not $base.Visible.ContainsKey($_) } | ForEach-Object { $now.Visible[$_] })
        # only what the guarded run started is stopped: a java window that is not ours (the player's own game, an IDE) is reported, never killed
        $tree = if ($child) { Get-TreeIds (Get-CimInstance Win32_Process | Select-Object ProcessId, ParentProcessId) $child.Id } else { @() }
        foreach ($p in (Get-StopSet $new $now.FgPid (Get-JavaPids) $tree)) { Stop-Process -Id $p -Force -ErrorAction SilentlyContinue }
        if ($child -and -not $child.HasExited) { & taskkill /PID $child.Id /T /F | Out-Null }
        exit 2
    }
}
if ($child) {
    if (-not $child.HasExited) { & taskkill /PID $child.Id /T /F | Out-Null; Write-Host 'focus-watch: MaxSec reached, run stopped'; exit 1 }
    Write-Host "focus-watch: run ended, no change (exit code $($child.ExitCode))"
    exit $child.ExitCode
}
Write-Host 'focus-watch: no change'
exit 0
