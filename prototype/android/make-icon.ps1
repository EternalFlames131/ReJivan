$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

# ReJivan launcher icon generator (Windows). Produces legacy + adaptive icons
# in every density. Colors follow the app theme: navy #0E1420, teal #2FBF8F, white.
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$res  = Join-Path $root "app\src\main\res"
$navy = [System.Drawing.Color]::FromArgb(14, 20, 32)
$teal = [System.Drawing.Color]::FromArgb(47, 191, 143)
$white = [System.Drawing.Color]::White
$fontPath = "C:\Windows\Fonts\arialbd.ttf"

# Draw the logo (white "R" + teal heartbeat line) inside a `size` square at (ox,oy).
function Draw-Logo([System.Drawing.Graphics]$g, [float]$size, [float]$ox, [float]$oy) {
    $fs = $size * 0.40
    $fc = New-Object System.Drawing.Text.PrivateFontCollection
    $fc.AddFontFile($fontPath)
    $font = [System.Drawing.Font]::new($fc.Families[0], $fs, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $sf = [System.Drawing.StringFormat]::new()
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $rect = [System.Drawing.RectangleF]::new($ox, ($oy + $size * 0.18), $size, $size * 0.40)
    $g.DrawString("R", $font, [System.Drawing.Brushes]::White, $rect, $sf)
    $font.Dispose(); $fc.Dispose(); $sf.Dispose()

    $pen = [System.Drawing.Pen]::new($teal, [Math]::Max(2.0, $size * 0.028))
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    $y0 = $oy + $size * 0.70
    $s = [float]$size
    $pts = @(
        [System.Drawing.PointF]::new($ox + 0.10 * $s, $y0),
        [System.Drawing.PointF]::new($ox + 0.22 * $s, $y0),
        [System.Drawing.PointF]::new($ox + 0.29 * $s, $y0 - 0.10 * $s),
        [System.Drawing.PointF]::new($ox + 0.36 * $s, $y0 + 0.10 * $s),
        [System.Drawing.PointF]::new($ox + 0.42 * $s, $y0),
        [System.Drawing.PointF]::new($ox + 0.56 * $s, $y0),
        [System.Drawing.PointF]::new($ox + 0.61 * $s, $y0 - 0.10 * $s),
        [System.Drawing.PointF]::new($ox + 0.67 * $s, $y0 + 0.10 * $s),
        [System.Drawing.PointF]::new($ox + 0.73 * $s, $y0),
        [System.Drawing.PointF]::new($ox + 0.90 * $s, $y0)
    )
    $g.DrawLines($pen, $pts)
    $pen.Dispose()
}

function New-Icon([int]$px, [string]$out, [bool]$round, [bool]$transparentBg) {
    $bmp = [System.Drawing.Bitmap]::new($px, $px)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.Clear([System.Drawing.Color]::Transparent)

    $clip = [System.Drawing.Drawing2D.GraphicsPath]::new()
    if ($round) { $clip.AddEllipse(1, 1, $px - 2, $px - 2) }
    else {
        $rad = $px * 0.22
        $d = 2 * $rad
        $clip.AddArc(0, 0, $d, $d, 180, 90)
        $clip.AddArc($px - $d, 0, $d, $d, 270, 90)
        $clip.AddArc($px - $d, $px - $d, $d, $d, 0, 90)
        $clip.AddArc(0, $px - $d, $d, $d, 90, 90)
        $clip.CloseFigure()
    }
    $g.SetClip($clip)

    if (-not $transparentBg) {
        $g.Clear($navy)
        $pen = [System.Drawing.Pen]::new($teal, [Math]::Max(2.0, $px * 0.03))
        $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
        $g.DrawPath($pen, $clip)
        $pen.Dispose()
    }

    if ($transparentBg) {
        $fs = $px * 0.64       # adaptive-icon safe zone (~66%)
        $off = ($px - $fs) / 2
        Draw-Logo $g $fs $off $off
    } else {
        Draw-Logo $g $px 0 0
    }

    $g.Dispose()
    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

$dens = @{ "mdpi" = 1.0; "hdpi" = 1.5; "xhdpi" = 2.0; "xxhdpi" = 3.0; "xxxhdpi" = 4.0 }
foreach ($d in $dens.Keys) {
    $m = $dens[$d]
    $dir = Join-Path $res "mipmap-$d"
    New-Icon ([int](48 * $m)) (Join-Path $dir "ic_launcher.png")            $false $false
    New-Icon ([int](48 * $m)) (Join-Path $dir "ic_launcher_round.png")      $true  $false
    New-Icon ([int](108 * $m)) (Join-Path $dir "ic_launcher_foreground.png") $false $true
    Write-Host ("mipmap-{0}: legacy {1}px, foreground {2}px" -f $d, ([int](48*$m)), ([int](108*$m)))
}

# Legacy mipmap-anydpi-v26 adaptive XML already points at @color/ic_launcher_background + @mipmap/ic_launcher_foreground.
Write-Host "Icons regenerated."
