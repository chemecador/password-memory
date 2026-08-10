param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "assets")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

function New-ScaledPoint {
    param(
        [double]$X,
        [double]$Y,
        [double]$Scale,
        [double]$OffsetX,
        [double]$OffsetY
    )

    [System.Drawing.PointF]::new(
        [single]($OffsetX + ($X * $Scale)),
        [single]($OffsetY + ($Y * $Scale))
    )
}

function New-ShieldPath {
    param(
        [double]$Scale,
        [double]$OffsetX,
        [double]$OffsetY
    )

    $path = [System.Drawing.Drawing2D.GraphicsPath]::new(
        [System.Drawing.Drawing2D.FillMode]::Alternate
    )

    $path.StartFigure()
    $path.AddLine(
        (New-ScaledPoint 54 24 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 83 33.5 $Scale $OffsetX $OffsetY)
    )
    $path.AddLine(
        (New-ScaledPoint 83 33.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 83 52 $Scale $OffsetX $OffsetY)
    )
    $path.AddBezier(
        (New-ScaledPoint 83 52 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 83 66.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 71 78.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 54 85 $Scale $OffsetX $OffsetY)
    )
    $path.AddBezier(
        (New-ScaledPoint 54 85 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 37 78.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 25 66.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 25 52 $Scale $OffsetX $OffsetY)
    )
    $path.AddLine(
        (New-ScaledPoint 25 52 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 25 33.5 $Scale $OffsetX $OffsetY)
    )
    $path.CloseFigure()

    $path.StartFigure()
    $path.AddBezier(
        (New-ScaledPoint 54 38.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 49.3 38.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 45.5 42.3 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 45.5 47 $Scale $OffsetX $OffsetY)
    )
    $path.AddBezier(
        (New-ScaledPoint 45.5 47 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 45.5 50.2 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 47.3 53 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 49.8 54.4 $Scale $OffsetX $OffsetY)
    )
    $path.AddLine(
        (New-ScaledPoint 49.8 54.4 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 47.5 66.5 $Scale $OffsetX $OffsetY)
    )
    $path.AddBezier(
        (New-ScaledPoint 47.5 66.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 47.3 67.6 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 48.1 68.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 49.2 68.5 $Scale $OffsetX $OffsetY)
    )
    $path.AddLine(
        (New-ScaledPoint 49.2 68.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 58.8 68.5 $Scale $OffsetX $OffsetY)
    )
    $path.AddBezier(
        (New-ScaledPoint 58.8 68.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 59.9 68.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 60.7 67.6 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 60.5 66.5 $Scale $OffsetX $OffsetY)
    )
    $path.AddLine(
        (New-ScaledPoint 60.5 66.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 58.2 54.4 $Scale $OffsetX $OffsetY)
    )
    $path.AddBezier(
        (New-ScaledPoint 58.2 54.4 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 60.7 53 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 62.5 50.2 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 62.5 47 $Scale $OffsetX $OffsetY)
    )
    $path.AddBezier(
        (New-ScaledPoint 62.5 47 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 62.5 42.3 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 58.7 38.5 $Scale $OffsetX $OffsetY),
        (New-ScaledPoint 54 38.5 $Scale $OffsetX $OffsetY)
    )
    $path.CloseFigure()

    $path
}

function Add-BrandBackground {
    param(
        [System.Drawing.Graphics]$Graphics,
        [int]$Width,
        [int]$Height
    )

    $bounds = [System.Drawing.Rectangle]::new(0, 0, $Width, $Height)
    $background = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        $bounds,
        [System.Drawing.Color]::FromArgb(255, 124, 108, 255),
        [System.Drawing.Color]::FromArgb(255, 46, 42, 158),
        45.0
    )
    try {
        $Graphics.FillRectangle($background, $bounds)
    } finally {
        $background.Dispose()
    }

    $highlightPath = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $highlightPath.AddEllipse(
        [single](-0.25 * $Width),
        [single](-0.55 * $Height),
        [single](1.25 * $Width),
        [single](1.45 * $Height)
    )
    $highlight = [System.Drawing.Drawing2D.PathGradientBrush]::new($highlightPath)
    try {
        $highlight.CenterColor = [System.Drawing.Color]::FromArgb(68, 255, 255, 255)
        $highlight.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 255, 255, 255))
        $Graphics.FillPath($highlight, $highlightPath)
    } finally {
        $highlight.Dispose()
        $highlightPath.Dispose()
    }
}

function Add-ShieldMark {
    param(
        [System.Drawing.Graphics]$Graphics,
        [double]$Scale,
        [double]$OffsetX,
        [double]$OffsetY
    )

    $shadowPath = New-ShieldPath $Scale $OffsetX ($OffsetY + (2.5 * $Scale))
    $shadow = [System.Drawing.SolidBrush]::new(
        [System.Drawing.Color]::FromArgb(46, 0, 0, 0)
    )
    try {
        $Graphics.FillPath($shadow, $shadowPath)
    } finally {
        $shadow.Dispose()
        $shadowPath.Dispose()
    }

    $shieldPath = New-ShieldPath $Scale $OffsetX $OffsetY
    $shieldBounds = [System.Drawing.RectangleF]::new(
        [single]($OffsetX + (25 * $Scale)),
        [single]($OffsetY + (24 * $Scale)),
        [single](58 * $Scale),
        [single](61 * $Scale)
    )
    $shield = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        $shieldBounds,
        [System.Drawing.Color]::White,
        [System.Drawing.Color]::FromArgb(255, 231, 228, 255),
        52.0
    )
    try {
        $Graphics.FillPath($shield, $shieldPath)
    } finally {
        $shield.Dispose()
        $shieldPath.Dispose()
    }

    $bevel = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $bevel.AddPolygon(
        [System.Drawing.PointF[]]@(
            (New-ScaledPoint 54 24 $Scale $OffsetX $OffsetY),
            (New-ScaledPoint 83 33.5 $Scale $OffsetX $OffsetY),
            (New-ScaledPoint 83 38 $Scale $OffsetX $OffsetY),
            (New-ScaledPoint 54 28.5 $Scale $OffsetX $OffsetY),
            (New-ScaledPoint 25 38 $Scale $OffsetX $OffsetY),
            (New-ScaledPoint 25 33.5 $Scale $OffsetX $OffsetY)
        )
    )
    $bevelBrush = [System.Drawing.SolidBrush]::new(
        [System.Drawing.Color]::FromArgb(140, 185, 178, 255)
    )
    try {
        $Graphics.FillPath($bevelBrush, $bevel)
    } finally {
        $bevelBrush.Dispose()
        $bevel.Dispose()
    }
}

function Save-StoreIcon {
    param([string]$Path)

    $bitmap = [System.Drawing.Bitmap]::new(
        512,
        512,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.CompositingQuality =
            [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        Add-BrandBackground $graphics 512 512
        Add-ShieldMark $graphics (512.0 / 108.0) 0 0
        $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

function Save-FeatureGraphic {
    param(
        [string]$Path,
        [string]$Tagline
    )

    $bitmap = [System.Drawing.Bitmap]::new(
        1024,
        500,
        [System.Drawing.Imaging.PixelFormat]::Format24bppRgb
    )
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.CompositingQuality =
            [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $graphics.TextRenderingHint =
            [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
        Add-BrandBackground $graphics 1024 500

        $halo = [System.Drawing.SolidBrush]::new(
            [System.Drawing.Color]::FromArgb(24, 255, 255, 255)
        )
        try {
            $graphics.FillEllipse($halo, 60, 55, 390, 390)
        } finally {
            $halo.Dispose()
        }

        Add-ShieldMark $graphics 3.45 65 61

        $titleFont = [System.Drawing.Font]::new(
            "Segoe UI",
            53,
            [System.Drawing.FontStyle]::Bold,
            [System.Drawing.GraphicsUnit]::Pixel
        )
        $taglineFont = [System.Drawing.Font]::new(
            "Segoe UI",
            27,
            [System.Drawing.FontStyle]::Regular,
            [System.Drawing.GraphicsUnit]::Pixel
        )
        $white = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
        $softWhite = [System.Drawing.SolidBrush]::new(
            [System.Drawing.Color]::FromArgb(224, 255, 255, 255)
        )
        try {
            $graphics.DrawString("Password", $titleFont, $white, 455, 174)
            $graphics.DrawString("Memory", $titleFont, $white, 455, 232)
            $graphics.DrawString($Tagline, $taglineFont, $softWhite, 460, 313)
        } finally {
            $softWhite.Dispose()
            $white.Dispose()
            $taglineFont.Dispose()
            $titleFont.Dispose()
        }

        $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$resolvedOutput = (Resolve-Path -LiteralPath $OutputDirectory).Path

Save-StoreIcon (Join-Path $resolvedOutput "icon-512.png")
Save-FeatureGraphic `
    (Join-Path $resolvedOutput "feature-graphic-en-US-1024x500.png") `
    "Local. Private. Yours."
Save-FeatureGraphic `
    (Join-Path $resolvedOutput "feature-graphic-es-ES-1024x500.png") `
    "Local. Privada. Tuya."

Write-Output "Generated Play Store assets in $resolvedOutput"
