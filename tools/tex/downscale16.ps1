# Batch-downscale generated texture PNGs to 16x16 with nearest-neighbor (no blur).
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools\tex\downscale16.ps1 -Path <file-or-folder> [-OutDir <dir>] [-Size 16]
# Default OutDir: <input>\_16 ; result keeps file names, ready to copy into assets.
param(
  [Parameter(Mandatory = $true)][string]$Path,
  [string]$OutDir,
  [int]$Size = 16
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

if (-not (Test-Path $Path)) { Write-Error "not found: $Path" }
$isFolder = Test-Path $Path -PathType Container
if (-not $OutDir) { $OutDir = Join-Path $Path '_16' }
New-Item $OutDir -ItemType Directory -Force | Out-Null

$files = if ($isFolder) { Get-ChildItem $Path -Filter *.png -File } else { Get-Item $Path }
if ($files.Count -eq 0) { Write-Error "no png found under $Path" }

foreach ($f in $files) {
  $src = New-Object System.Drawing.Bitmap($f.FullName)
  try {
    $dst = New-Object System.Drawing.Bitmap($Size, $Size)
    $g = [System.Drawing.Graphics]::FromImage($dst)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighSpeed
    $g.DrawImage($src, (New-Object System.Drawing.Rectangle(0, 0, $Size, $Size)))
    $g.Dispose()
    $out = Join-Path $OutDir $f.Name
    $dst.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    $dst.Dispose()
    Write-Host ("OK {0}  {1}x{2} -> {3}x{4}  => {5}" -f $f.Name, $src.Width, $src.Height, $Size, $Size, $out)
  }
  finally { $src.Dispose() }
}
Write-Host "done: $($files.Count) file(s)"
