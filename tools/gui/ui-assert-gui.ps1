$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$gui = 'E:\MC\Mod\1.20.1\Godofthings\src\main\resources\assets\godofthings\textures\gui'

function Get-Px($bmp) {
  $rect = New-Object System.Drawing.Rectangle(0,0,$bmp.Width,$bmp.Height)
  $data = $bmp.LockBits($rect,[System.Drawing.Imaging.ImageLockMode]::ReadOnly,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $bytes = New-Object byte[] ($bmp.Width*$bmp.Height*4)
  [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0,$bytes,0,$bytes.Length)
  $bmp.UnlockBits($data); return ,$bytes
}
# expectations: name -> @{ H; maxX; slotTops = list of "y:x1,x2,..." distinct inner-row tops }
$exp = @{
  'god_furnace'         = @{ H=172; maxX=193; tops=@{18='9,27,45,63,81,99'; 54='9,27,45,63,81,99'; 85='9,27,45,63,81,99,117,135,153'; 103='9,27,45,63,81,99,117,135,153'; 121='9,27,45,63,81,99,117,135,153'; 143='9,27,45,63,81,99,117,135,153'} }
  'god_furnace_config'  = @{ H=146; maxX=175; tops=@{} }
  'god_miner'           = @{ H=234; maxX=175; tops=@{157='9,27,45,63,81,99,117,135,153'; 175='9,27,45,63,81,99,117,135,153'; 193='9,27,45,63,81,99,117,135,153'; 215='9,27,45,63,81,99,117,135,153'} }
  'god_resource'        = @{ H=166; maxX=175; tops=@{36='80'; 85='9,27,45,63,81,99,117,135,153'; 103='9,27,45,63,81,99,117,135,153'; 121='9,27,45,63,81,99,117,135,153'; 143='9,27,45,63,81,99,117,135,153'} }
  'god_drop'            = @{ H=166; maxX=175; tops=@{36='80'; 85='9,27,45,63,81,99,117,135,153'; 103='9,27,45,63,81,99,117,135,153'; 121='9,27,45,63,81,99,117,135,153'; 143='9,27,45,63,81,99,117,135,153'} }
  'god_enchant'         = @{ H=256; maxX=175; tops=@{36='80'; 179='9,27,45,63,81,99,117,135,153'; 197='9,27,45,63,81,99,117,135,153'; 215='9,27,45,63,81,99,117,135,153'; 237='9,27,45,63,81,99,117,135,153'} }
  'god_change'          = @{ H=120; maxX=175; tops=@{} }
}
$fail = 0
foreach ($name in $exp.Keys) {
  $e = $exp[$name]
  $bmp = New-Object System.Drawing.Bitmap((Join-Path $gui "$name.png"))
  $px = Get-Px $bmp; $w = 256
  $minX=256;$minY=256;$maxX=-1;$maxY=-1
  $tops = @{}
  for ($y=0; $y -lt 256; $y++) {
    $run=0; $runX=0; $isTop=$false
    for ($x=0; $x -lt 256; $x++) {
      $i = ($y*$w+$x)*4
      if ($px[$i+3] -gt 0) { if($x -lt $minX){$minX=$x}; if($x -gt $maxX){$maxX=$x}; if($y -lt $minY){$minY=$y}; if($y -gt $maxY){$maxY=$y} }
      $isGray = ([math]::Abs($px[$i+2]-139) -le 2) -and ([math]::Abs($px[$i+1]-139) -le 2) -and ([math]::Abs($px[$i]-139) -le 2)
      if ($isGray) {
        if ($run -eq 0) {
          $runX=$x
          if ($y -eq 0) { $isTop=$true }
          else { $pi = (($y-1)*$w+$x)*4; if (-not (([math]::Abs($px[$pi+2]-139) -le 2) -and ([math]::Abs($px[$pi+1]-139) -le 2) -and ([math]::Abs($px[$pi]-139) -le 2))) { $isTop=$true } else { $isTop=$false } }
        }
        $run++
      }
      else {
        if ($run -ge 14 -and $isTop) { $k=$y; if(-not $tops.ContainsKey($k)){$tops[$k]=@()}; $tops[$k]+=$runX }
        $run=0; $isTop=$false
      }
    }
    if ($run -ge 14 -and $isTop) { $k=$y; if(-not $tops.ContainsKey($k)){$tops[$k]=@()}; $tops[$k]+=$runX }
  }
  $ok = $true
  if ($maxX -ne $e.maxX) { "FAIL $name maxX=$maxX expect $($e.maxX)"; $ok=$false }
  if ($maxY -ne $e.H-1) { "FAIL $name maxY=$maxY expect $($e.H-1)"; $ok=$false }
  $gotKeys = @($tops.Keys | Sort-Object)
  $expKeys = @($e.tops.Keys | Sort-Object {[int]$_})
  if (($gotKeys -join ',') -ne ($expKeys -join ',')) { "FAIL $name rows=[$($gotKeys -join ',')] expect=[$($expKeys -join ',')]"; $ok=$false }
  foreach ($k in $expKeys) {
    if ($tops.ContainsKey($k)) {
      $got = @($tops[$k] | Sort-Object | ForEach-Object { $_ })
      $want = @($e.tops[$k] -split ',' | ForEach-Object { [int]$_ })
      $d = Compare-Object $got $want
      if ($d) { "FAIL $name row $k xs=[$($got -join ',')] want=[$($want -join ',')]"; $ok=$false }
    }
  }
  if ($ok) { "PASS $name (H=$($e.H) maxX=$maxX rows=[$($expKeys -join ',')])" } else { $fail++ }
  $bmp.Dispose()
}
if ($fail -eq 0) { "ALL PASS" } else { "$fail FAILURES" }
