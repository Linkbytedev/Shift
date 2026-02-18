
param (
    [string]$SourceFile,
    [string]$DestinationRoot
)

Add-Type -AssemblyName System.Drawing

function Resize-Image {
    param (
        [string]$InputFile,
        [string]$OutputFile,
        [int]$Size
    )

    $image = [System.Drawing.Image]::FromFile($InputFile)
    $bitmap = New-Object System.Drawing.Bitmap($Size, $Size)
    $graph = [System.Drawing.Graphics]::FromImage($bitmap)
    $graph.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graph.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graph.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graph.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    $graph.DrawImage($image, 0, 0, $Size, $Size)
    
    $dir = Split-Path -Parent $OutputFile
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir | Out-Null
    }

    $bitmap.Save($OutputFile, [System.Drawing.Imaging.ImageFormat]::Png)
    
    $image.Dispose()
    $bitmap.Dispose()
    $graph.Dispose()
    
    Write-Host "Created $OutputFile ($Size x $Size)"
}

$sizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($folder in $sizes.Keys) {
    $size = $sizes[$folder]
    $destPath = Join-Path $DestinationRoot "$folder\ic_launcher.png"
    Resize-Image -InputFile $SourceFile -OutputFile $destPath -Size $size
    
    $destPathRound = Join-Path $DestinationRoot "$folder\ic_launcher_round.png"
    Resize-Image -InputFile $SourceFile -OutputFile $destPathRound -Size $size
}
