[CmdletBinding()]
param(
    [string]$DeviceId,
    [switch]$SkipBuild,
    [ValidateSet("en-US", "es-ES")]
    [string[]]$Locale = @("en-US", "es-ES")
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $repositoryRoot "gradlew.bat"
$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if ($null -eq $adbCommand) {
    throw "adb is not available on PATH."
}
$adb = $adbCommand.Source

$applicationId = "com.chemecador.passwordmemory"
$testClass = "$applicationId.PlayStoreScreenshotTest"
$remoteDirectory = "/sdcard/Android/data/$applicationId/files/play-store"
$outputRoot = Join-Path $PSScriptRoot "assets\screenshots"
$expectedFiles = @(
    "01-vault-overview.jpg",
    "02-unlock-animation.jpg",
    "03-password-generator.jpg",
    "04-encrypted-detail.jpg",
    "05-hashed-password-guess.jpg",
    "06-autofill-security.jpg",
    "07-security-settings.jpg",
    "08-privacy-policy.jpg"
)

function Invoke-Adb {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    $output = & $adb -s $DeviceId @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: adb -s $DeviceId $($Arguments -join ' ')`n$($output -join [Environment]::NewLine)"
    }
    return @($output)
}

function Validate-PlayScreenshot {
    param(
        [Parameter(Mandatory)]
        [string]$Path
    )

    $file = Get-Item -LiteralPath $Path
    if ($file.Length -le 0 -or $file.Length -gt 8MB) {
        throw "Screenshot size is outside Google Play limits: $Path ($($file.Length) bytes)."
    }

    $header = [System.IO.File]::ReadAllBytes($file.FullName)
    if ($header.Length -lt 2 -or $header[0] -ne 0xFF -or $header[1] -ne 0xD8) {
        throw "Screenshot is not a JPEG: $Path"
    }

    Add-Type -AssemblyName System.Drawing
    $image = [System.Drawing.Image]::FromFile($file.FullName)
    try {
        if ($image.Width -ne 1080 -or $image.Height -ne 1920) {
            throw "Screenshot must be exactly 1080x1920: $Path is $($image.Width)x$($image.Height)."
        }
        $longSide = [Math]::Max($image.Width, $image.Height)
        $shortSide = [Math]::Min($image.Width, $image.Height)
        if (($longSide / $shortSide) -gt 2) {
            throw "Screenshot exceeds Google Play's 2:1 aspect-ratio limit: $Path"
        }
        if ($image.PixelFormat.ToString().Contains("Alpha")) {
            throw "Screenshot contains an alpha channel: $Path"
        }
    } finally {
        $image.Dispose()
    }
}

$connectedDevices = @(
    & $adb devices |
        Select-Object -Skip 1 |
        Where-Object { $_ -match "^\S+\s+device(?:\s|$)" } |
        ForEach-Object { ($_ -split "\s+")[0] }
)
if ($LASTEXITCODE -ne 0) {
    throw "Could not list Android devices."
}
if ([string]::IsNullOrWhiteSpace($DeviceId)) {
    if ($connectedDevices.Count -ne 1) {
        throw "Pass -DeviceId when there is not exactly one connected Android device."
    }
    $DeviceId = $connectedDevices[0]
} elseif ($DeviceId -notin $connectedDevices) {
    throw "Android device '$DeviceId' is not connected and ready."
}

$originalSizeOutput = Invoke-Adb -Arguments @("shell", "wm", "size")
$overrideMatch = [regex]::Match(
    ($originalSizeOutput -join "`n"),
    "Override size:\s*(\d+x\d+)"
)
$originalOverrideSize = if ($overrideMatch.Success) { $overrideMatch.Groups[1].Value } else { $null }

Push-Location $repositoryRoot
try {
    if (-not $SkipBuild) {
        Write-Output "Building and installing screenshot fixtures on $DeviceId..."
        & $gradle `
            --no-configuration-cache `
            --no-daemon `
            --console=plain `
            :app:installDebug `
            :app:installDebugAndroidTest
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle could not install the app and instrumented test APK."
        }
    }

    $instrumentationLine = Invoke-Adb -Arguments @(
        "shell", "pm", "list", "instrumentation"
    ) | Where-Object { $_ -like "*target=$applicationId*" } | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($instrumentationLine)) {
        throw "Could not locate the AndroidJUnitRunner for $applicationId."
    }
    $runner = (($instrumentationLine -replace "^instrumentation:", "") -split "\s+")[0]

    Invoke-Adb -Arguments @("shell", "wm", "size", "1080x1920") | Out-Null

    foreach ($localeTag in $Locale) {
        Write-Output "Capturing $localeTag..."
        Invoke-Adb -Arguments @(
            "shell", "cmd", "locale", "set-app-locales",
            $applicationId, "--locales", $localeTag
        ) | Out-Null
        Invoke-Adb -Arguments @("shell", "am", "force-stop", $applicationId) | Out-Null

        foreach ($fileName in $expectedFiles) {
            Invoke-Adb -Arguments @(
                "shell", "rm", "-f", "$remoteDirectory/$fileName"
            ) | Out-Null
        }

        $instrumentationOutput = Invoke-Adb -Arguments @(
            "shell", "am", "instrument", "-w",
            "-e", "playStoreCapture", "true",
            "-e", "class", $testClass,
            $runner
        )
        if (($instrumentationOutput -join "`n") -notmatch "\bOK \(\d+ tests?\)") {
            throw "Screenshot tests failed for $localeTag.`n$($instrumentationOutput -join [Environment]::NewLine)"
        }

        $localeDirectory = Join-Path $outputRoot $localeTag
        New-Item -ItemType Directory -Force -Path $localeDirectory | Out-Null
        foreach ($fileName in $expectedFiles) {
            $localPath = Join-Path $localeDirectory $fileName
            if (Test-Path -LiteralPath $localPath) {
                Remove-Item -LiteralPath $localPath
            }
            Invoke-Adb -Arguments @(
                "pull", "$remoteDirectory/$fileName", $localPath
            ) | Out-Null
            Validate-PlayScreenshot -Path $localPath
        }

        $unexpectedScreenshots = @(
            Get-ChildItem -LiteralPath $localeDirectory -File -Filter "*.jpg" |
                Where-Object { $_.Name -notin $expectedFiles }
        )
        if ($unexpectedScreenshots.Count -gt 0) {
            throw "Unexpected JPEG files found in ${localeTag}: $($unexpectedScreenshots.Name -join ', ')"
        }
    }

    Write-Output "Created and validated $($expectedFiles.Count * $Locale.Count) screenshots."
    Write-Output "Output: $outputRoot"
} finally {
    Invoke-Adb -Arguments @(
        "shell", "cmd", "locale", "set-app-locales", $applicationId
    ) | Out-Null
    if ($null -ne $originalOverrideSize) {
        Invoke-Adb -Arguments @("shell", "wm", "size", $originalOverrideSize) | Out-Null
    } else {
        Invoke-Adb -Arguments @("shell", "wm", "size", "reset") | Out-Null
    }
    Pop-Location
}
