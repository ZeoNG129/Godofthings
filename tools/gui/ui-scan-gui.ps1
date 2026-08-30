# Scan GUI textures: canvas size, content bbox, slot-inner-gray (#8B8B8B) runs per row
Add-Type -AssemblyName System.Drawing
$gui = 'E:\MC\Mod\1.20.1\Godofthings\src\main\resources\assets\godofthings\textures\gui'
$targets = @('god_furnace','god_furnace_config','god_miner','god_resource','god_drop','god_enchant','god_change','creative_energy_cube_gui')

function Get-Px($bmp) {
  $rect = New-Object System.Drawing.Rectangle(0,0,$bmp.Width,$bmp.Height)
  $data = $bmp.LockBits($rect,[System.Drawing.Imaging.ImageLockMode]::ReadOnly,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $len = $bmp.Width*$bmp.Height*4
  $bytes = New-Object byte[] $len
  [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0,$bytes,0,$len)
  $bmp.UnlockBits($data)
  return ,$bytes
}

foreach ($name in $targets) {
  $path = Join-Path $gui "$name.png"
  if (-not (Test-Path $path)) { "=== $name : MISSING ==="; continue }
  $bmp = New-Object System.Drawing.Bitmap($path)
  $w = $bmp.Width; $h = $bmp.Height
  $px = Get-Px $bmp
  $minX=$w;$minY=$h;$maxX=-1;$maxY=-1
  for ($y=0; $y -lt $h; $y++) {
    for ($x=0; $x -lt $w; $x++) {
      $a = $px[($y*$w+$x)*4+3]
      if ($a -gt 0) {
        if ($x -lt $minX) {$minX=$x}; if ($x -gt $maxX) {$maxX=$x}
        if ($y -lt $minY) {$minY=$y}; if ($y -gt $maxY) {$maxY=$y}
      }
    }
  }
  "=== $name : ${w}x${h}  contentBBox=($minX,$minY)-($maxX,$maxY) ==="
  # gray runs: scan rows for runs of >=5 consecutive #8B8B8B (tolerance 6)
  $found = 0
  for ($y=0; $y -lt $h; $y++) {
    $run = 0; $runX = 0
    for ($x=0; $x -lt $w; $x++) {
      $i = ($y*$w+$x)*4
      $isGray = ([math]::Abs($px[$i+2]-139) -le 6) -and ([math]::Abs($px[$i+1]-139) -le 6) -and ([math]::Abs($px[$i]-139) -le 6) -and ($px[$i+3] -gt 200)
      if ($isGray) { if ($run -eq 0) {$runX=$x}; $run++ }
      else {
        if ($run -ge 5 -and $found -lt 40) { "  grayRun y=$y x=$runX len=$run"; $found++ }
        $run = 0
      }
    }
    if ($run -ge 5 -and $found -lt 40) { "  grayRun y=$y x=$runX len=$run"; $found++ }
  }
  if ($found -eq 0) { "  (no gray slot runs)" }
  $bmp.Dispose()
}
# cube palette anchors
$cbmp = New-Object System.Drawing.Bitmap((Join-Path $gui 'creative_energy_cube_gui.png'))
$cpx = Get-Px $cbmp
$cw = $cbmp.Width
"=== cube palette anchors ==="
$anchors = @(@(0,0),@(1,1),@(2,2),@(174,164),@(175,165),@(174,165),@(88,80),@(78,29),@(79,30),@(95,46),@(94,45),@(78,46),@(95,29))
foreach ($p in $anchors) {
  $i = ($p[1]*$cw+$p[0])*4
  "  ({0},{1}) = #{2:X2}{3:X2}{4:X2} a={5}" -f $p[0],$p[1],$cpx[$i+2],$cpx[$i+1],$cpx[$i],$cpx[$i+3]
}
$cbmp.Dispose()
