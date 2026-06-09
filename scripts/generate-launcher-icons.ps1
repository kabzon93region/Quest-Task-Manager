Add-Type -AssemblyName System.Drawing

function Draw-Icon { param([int]$Size)
    $bmp = New-Object System.Drawing.Bitmap $Size, $Size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::FromArgb(255, 13, 17, 23))
    $cx = $Size / 2.0; $scale = $Size / 108.0
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 0, 217, 255))), ($cx - 28*$scale), ($cx - 20*$scale), (56*$scale), (40*$scale))
    $g.FillRectangle([System.Drawing.Brushes]::White, ($cx - 22*$scale), ($cx - 14*$scale), (44*$scale), (6*$scale))
    $g.FillRectangle([System.Drawing.Brushes]::White, ($cx - 22*$scale), ($cx - 2*$scale), (44*$scale), (6*$scale))
    $g.FillRectangle([System.Drawing.Brushes]::White, ($cx - 22*$scale), ($cx + 10*$scale), (44*$scale), (6*$scale))
    $g.Dispose(); return $bmp
}

$resRoot = Join-Path $PSScriptRoot "..\src\quest-app\app\src\main\res"
$sizes = @{ "mipmap-mdpi"=48; "mipmap-hdpi"=72; "mipmap-xhdpi"=96; "mipmap-xxhdpi"=144; "mipmap-xxxhdpi"=192 }
foreach ($folder in $sizes.Keys) {
    $dir = Join-Path $resRoot $folder
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $bmp = Draw-Icon -Size $sizes[$folder]
    $bmp.Save((Join-Path $dir "ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Save((Join-Path $dir "ic_launcher_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}
Write-Host "Icons OK"
