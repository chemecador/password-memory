[CmdletBinding()]
param(
    [switch]$VerifyOnly,
    [string]$BundlePath,
    [string]$ExpectedSignerSha256
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if (Test-Path variable:PSNativeCommandUseErrorActionPreference) {
    $PSNativeCommandUseErrorActionPreference = $false
}

function Resolve-Java {
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $javaHomeExecutable = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (-not (Test-Path -LiteralPath $javaHomeExecutable -PathType Leaf)) {
            throw "JAVA_HOME does not contain bin\java.exe: $env:JAVA_HOME"
        }
        return [System.IO.Path]::GetFullPath($javaHomeExecutable)
    }

    $command = Get-Command 'java.exe' -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw "Required tool 'java.exe' was not found on PATH."
    }
    return $command.Source
}

function Resolve-NativeTool {
    param(
        [Parameter(Mandatory)]
        [string]$Name
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw "Required tool '$Name' was not found on PATH."
    }
    return $command.Source
}

function Invoke-NativeTool {
    param(
        [Parameter(Mandatory)]
        [string]$FilePath,
        [Parameter(Mandatory)]
        [string[]]$Arguments,
        [Parameter(Mandatory)]
        [string]$Description
    )

    & $FilePath @Arguments
    $nativeExitCode = $LASTEXITCODE
    if ($nativeExitCode -ne 0) {
        throw "$Description failed with exit code $nativeExitCode."
    }
}

function Assert-SigningPropertiesProtection {
    param(
        [Parameter(Mandatory)]
        [string]$RepositoryRoot
    )

    $propertiesPath = Join-Path $RepositoryRoot 'keystore.properties'
    if (-not (Test-Path -LiteralPath $propertiesPath -PathType Leaf)) {
        throw ("Missing keystore.properties. Copy keystore.properties.example, " +
            "use the protected upload-key details, and never commit that file.")
    }

    $git = Resolve-NativeTool -Name 'git.exe'
    & $git -C $RepositoryRoot ls-files --error-unmatch -- keystore.properties *> $null
    $trackedExitCode = $LASTEXITCODE
    if ($trackedExitCode -eq 0) {
        throw 'keystore.properties is tracked by Git. Remove it from the index before continuing.'
    }
    & $git -C $RepositoryRoot check-ignore --quiet -- keystore.properties
    $ignoredExitCode = $LASTEXITCODE
    if ($ignoredExitCode -ne 0) {
        throw 'keystore.properties is not ignored by Git.'
    }

    return (Resolve-Path -LiteralPath $propertiesPath).Path
}

function Resolve-Bundle {
    param(
        [Parameter(Mandatory)]
        [string]$RepositoryRoot,
        [string]$RequestedPath
    )

    $candidate = if ([string]::IsNullOrWhiteSpace($RequestedPath)) {
        Join-Path $RepositoryRoot 'app\build\outputs\bundle\release\app-release.aab'
    } elseif ([System.IO.Path]::IsPathRooted($RequestedPath)) {
        $RequestedPath
    } else {
        Join-Path $RepositoryRoot $RequestedPath
    }

    if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) {
        throw "Release bundle does not exist: $candidate"
    }
    return (Resolve-Path -LiteralPath $candidate).Path
}

try {
    $repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
    $java = Resolve-Java
    $verifier = Join-Path $PSScriptRoot 'tools\VerifySignedBundle.java'
    $gradle = Join-Path $repositoryRoot 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $verifier -PathType Leaf)) {
        throw "Bundle verifier does not exist: $verifier"
    }
    if (-not (Test-Path -LiteralPath $gradle -PathType Leaf)) {
        throw "Gradle wrapper does not exist: $gradle"
    }

    if (-not $VerifyOnly -and -not [string]::IsNullOrWhiteSpace($BundlePath)) {
        throw '-BundlePath is only allowed with -VerifyOnly.'
    }
    if (-not $VerifyOnly -and -not [string]::IsNullOrWhiteSpace($ExpectedSignerSha256)) {
        throw '-ExpectedSignerSha256 is only allowed with -VerifyOnly.'
    }

    $signingProperties = $null
    $verificationArguments = $null

    if ($VerifyOnly -and -not [string]::IsNullOrWhiteSpace($ExpectedSignerSha256)) {
        $verificationArguments = @(
            '--verify',
            '',
            '--expected-sha256',
            $ExpectedSignerSha256
        )
    } else {
        $signingProperties = Assert-SigningPropertiesProtection `
            -RepositoryRoot $repositoryRoot
        Invoke-NativeTool `
            -FilePath $java `
            -Arguments @(
                $verifier,
                '--check-keystore',
                $signingProperties,
                $repositoryRoot
            ) `
            -Description 'Upload keystore verification'
        $verificationArguments = @(
            '--verify',
            '',
            '--properties',
            $signingProperties,
            $repositoryRoot
        )
    }

    $buildStartedAtUtc = $null
    if (-not $VerifyOnly) {
        $canonicalBundle = [System.IO.Path]::GetFullPath(
            (Join-Path $repositoryRoot 'app\build\outputs\bundle\release\app-release.aab')
        )
        $buildStartedAtUtc = [DateTime]::UtcNow
        Push-Location $repositoryRoot
        try {
            Invoke-NativeTool `
                -FilePath $gradle `
                -Arguments @(
                    '--no-configuration-cache',
                    '--no-daemon',
                    '--console=plain',
                    "-PpasswordMemory.bundleToValidate=$canonicalBundle",
                    'clean',
                    ':app:testDebugUnitTest',
                    ':app:lintRelease',
                    ':app:bundleRelease',
                    ':app:validatePlayBundle'
                ) `
                -Description 'Release build gates'
        } finally {
            Pop-Location
        }
    }

    $bundle = Resolve-Bundle `
        -RepositoryRoot $repositoryRoot `
        -RequestedPath $BundlePath
    if ($null -ne $buildStartedAtUtc) {
        $bundleTimestamp = (Get-Item -LiteralPath $bundle).LastWriteTimeUtc
        if ($bundleTimestamp -lt $buildStartedAtUtc.AddSeconds(-2)) {
            throw 'The release AAB was not regenerated by this verification run.'
        }
    }

    if ($VerifyOnly) {
        Push-Location $repositoryRoot
        try {
            Invoke-NativeTool `
                -FilePath $gradle `
                -Arguments @(
                    '--no-configuration-cache',
                    '--no-daemon',
                    '--console=plain',
                    "-PpasswordMemory.bundleToValidate=$bundle",
                    ':app:validatePlayBundle'
                ) `
                -Description 'bundletool semantic validation'
        } finally {
            Pop-Location
        }
    }

    $verificationArguments[1] = $bundle
    Invoke-NativeTool `
        -FilePath $java `
        -Arguments (@($verifier) + $verificationArguments) `
        -Description 'AAB signature verification'

    $bundleFile = Get-Item -LiteralPath $bundle
    $bundleHash = Get-FileHash -LiteralPath $bundle -Algorithm SHA256
    Write-Output 'Release verification passed.'
    Write-Output "AAB: $($bundleFile.FullName)"
    Write-Output "Size: $($bundleFile.Length) bytes"
    Write-Output "SHA-256: $($bundleHash.Hash)"
} catch {
    [Console]::Error.WriteLine("Release verification failed: $($_.Exception.Message)")
    exit 1
}
