# Capture REAL MainActivity framebuffer pixels on one authorized physical
# Android device. This is an alternative to unreliable GitHub-hosted emulators.
# Requires the explicitly installed SEPARATE debug preview package. It never
# installs an APK, writes finance data, changes owner signing, or touches the
# production app. PNGs stay on this Windows PC (never upload to GitHub).
#
# From the repository root with Android SDK platform-tools and Python 3:
#   pwsh -File scripts/capture_owner_android_device.ps1
#   # or Windows PowerShell 5.1:
#   powershell -ExecutionPolicy Bypass -File scripts/capture_owner_android_device.ps1
[CmdletBinding()]
param(
    [string]$Adb = "adb",
    [string]$OutputRoot = ""
)
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$package = "app.wensarafet.tracker.recovered.debug"
$receiver = "app.wensarafet.tracker.OwnerThemeCaptureReceiver"
$activity = "app.wensarafet.tracker.MainActivity"
$validator = Join-Path $PSScriptRoot "validate_native_capture.py"
if (!(Test-Path -LiteralPath $validator)) {
    throw "Required strict PNG validator missing: $validator"
}

if (!(Get-Command $Adb -ErrorAction SilentlyContinue)) {
    $maybe = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path -LiteralPath $maybe) { $Adb = $maybe }
    else { throw "Android platform-tools ADB not found. Install and put adb on PATH." }
}
function Invoke-Adb {
    param([string[]]$Parts)
    $lines = & $Adb @Parts 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw ("ADB failed (" + ($Parts -join " ") + "): " + ($lines | Out-String))
    }
    return ($lines | Out-String).Trim()
}
function Check-Png {
    param([string]$Path)
    if (Get-Command py -ErrorAction SilentlyContinue) {
        & py -3 $validator $Path
    } elseif (Get-Command python -ErrorAction SilentlyContinue) {
        & python $validator $Path
    } elseif (Get-Command python3 -ErrorAction SilentlyContinue) {
        & python3 $validator $Path
    } else {
        throw "Python 3 is required to verify PNG CRC and framebuffer contents."
    }
    if ($LASTEXITCODE -ne 0) {
        throw "PNG rejected as incomplete/corrupt: $Path"
    }
}
$raw = Invoke-Adb -Parts @("devices")
$ready = @($raw -split "\r?\n" | Where-Object { $_ -match "^\S+\s+device$" })
if ($ready.Count -ne 1) {
    throw "Connect and authorize exactly ONE Android device (USB debugging). ADB returned: $raw"
}
$serial = ($ready[0] -split "\s+")[0]
$installed = Invoke-Adb -Parts @("-s", $serial, "shell", "pm", "path", $package)
if ($installed -notmatch "package:") {
    throw "Install the separate owner visual DEBUG preview package first. Production app is untouched."
}
if (-not $OutputRoot) {
    $OutputRoot = Join-Path (Get-Location).Path "build\owner-physical-qa"
}
$runName = "real-android-" + (Get-Date -Format "yyyyMMdd-HHmmss")
$outDir = Join-Path $OutputRoot $runName
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$deviceModel = (Invoke-Adb -Parts @("-s", $serial, "shell", "getprop", "ro.product.model")).Trim()
$api = (Invoke-Adb -Parts @("-s", $serial, "shell", "getprop", "ro.build.version.sdk")).Trim()
$report = @(
    "REAL physical Android MainActivity screenshot audit"
    "Device model: $deviceModel"
    "Android SDK: $api"
    "Package: $package (isolated debug only)"
    "Captured: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
    "No simulated images; real ADB framebuffer; original account logos."
)
try {
    foreach ($theme in @(25,26,27)) {
        Write-Host "Capturing real gold theme $theme..." -ForegroundColor Cyan
        Invoke-Adb -Parts @("-s",$serial,"logcat","-c") | Out-Null
        Invoke-Adb -Parts @("-s",$serial,"shell","am","force-stop",$package) | Out-Null
        $broadcast = Invoke-Adb -Parts @("-s",$serial,"shell","am","broadcast","-n",
                "$package/$receiver","--ei","theme","$theme")
        if ($broadcast -notmatch "Broadcast completed") {
            throw "Debug theme receiver failed: $broadcast"
        }
        Invoke-Adb -Parts @("-s",$serial,"shell","am","start","-W","-n",
                "$package/$activity") | Out-Null
        $isReady = $false
        for ($i=0; $i -lt 25; $i++) {
            Start-Sleep -Milliseconds 400
            $optics = Invoke-Adb -Parts @("-s",$serial,"logcat","-d","-s",
                    "WenGlassProbe:I","WenRealPreview:I")
            if ($optics.Contains("MIT_VIEW_GLASS_BACKDROP_BOUND source=gold-scene")) {
                $isReady = $true
                break
            }
        }
        if (-not $isReady) {
            throw "Theme $theme did not render authentic gold-scene; refusing SplashScreen screenshot."
        }
        Start-Sleep -Milliseconds 500
        $remote = "/sdcard/Download/wen-owner-physical-theme-$theme.png"
        $local = Join-Path $outDir "main-theme-$theme-home.png"
        Invoke-Adb -Parts @("-s",$serial,"shell","screencap","-p",$remote) | Out-Null
        Invoke-Adb -Parts @("-s",$serial,"pull",$remote,$local) | Out-Null
        Check-Png -Path $local
        Invoke-Adb -Parts @("-s",$serial,"shell","rm","-f",$remote) | Out-Null
        $sha = (Get-FileHash -LiteralPath $local -Algorithm SHA256).Hash
        $report += "Theme $theme | $((Get-Item $local).Length) bytes | SHA256=$sha"
        Write-Host "PASS: $local" -ForegroundColor Green
    }
    $report += "RESULT: 3/3 original-pixel Android framebuffer screenshots verified."
    $report | Set-Content -LiteralPath (Join-Path $outDir "capture-proof.txt") -Encoding UTF8
    $zip = "$outDir.zip"
    Compress-Archive -LiteralPath $outDir -DestinationPath $zip -Force
    Write-Host "VERIFIED physical screenshots ZIP: $zip" -ForegroundColor Green
} catch {
    $report += "FAILED: $($_.Exception.Message)"
    $report | Set-Content -LiteralPath (Join-Path $outDir "capture-proof.txt") -Encoding UTF8
    Write-Warning "No success is claimed. Diagnostics in $outDir"
    throw
}
