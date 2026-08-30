# Regenerate 7 machine GUI textures with cube-exact geometry (256x256 canvas)
Add-Type -AssemblyName System.Drawing
$gui = 'E:\MC\Mod\1.20.1\Godofthings\src\main\resources\assets\godofthings\textures\gui'

$ErrorActionPreference = 'Stop'
function BR([int]$r,[int]$g,[int]$b){ New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255,$r,$g,$b)) }
$panel     = BR 0xC6 0xC6 0xC6
$bevW      = BR 0xFF 0xFF 0xFF
$bevD      = BR 0x55 0x55 0x55
$cellDark  = BR 0x37 0x37 0x37
$cellWhite = BR 0xFF 0xFF 0xFF
$cellInner = BR 0x8B 0x8B 0x8B
$insBd     = BR 0x16 0x18 0x1D
$insBg     = BR 0x3A 0x3A 0x3A
$gearCol   = [System.Drawing.Color]::FromArgb(255,0xC9,0xC9,0xC9)
$gearHi    = [System.Drawing.Color]::FromArgb(255,0xE8,0xE8,0xE8)

$script:g = $null
function FR([int]$x1,[int]$y1,[int]$x2,[int]$y2,$brush){
  $script:g.FillRectangle($brush,[float]$x1,[float]$y1,[float]($x2-$x1+1),[float]($y2-$y1+1))
}
function DrawPanel([int]$h){
  FR 0 0 175 ($h-1) $panel
  FR 0 0 175 1 $bevW          # top 2 rows white
  FR 0 0 1 ($h-1) $bevW       # left 2 cols white
  FR 174 0 175 ($h-1) $bevD   # right 2 cols dark
  FR 0 ($h-2) 175 ($h-1) $bevD # bottom 2 rows dark
}
function DrawCell([int]$sx,[int]$sy){
  FR ($sx-1) ($sy-1) ($sx+16) $sy $cellDark       # top 2 rows
  FR ($sx-1) ($sy-1) $sx ($sy+16) $cellDark       # left 2 cols
  FR ($sx+15) ($sy-1) ($sx+16) ($sy+16) $cellWhite # right 2 cols
  FR ($sx-1) ($sy+15) ($sx+16) ($sy+16) $cellWhite # bottom 2 rows
  FR ($sx+1) ($sy+1) ($sx+14) ($sy+14) $cellInner  # inner 14x14
}
function DrawInset([int]$x1,[int]$y1,[int]$x2,[int]$y2){
  FR $x1 $y1 $x2 $y2 $insBd
  FR ($x1+1) ($y1+1) ($x2-1) ($y2-1) $insBg
}
function InvGrid([int]$gy,[int]$hy){   # player inv 3x9 + hotbar
  for($r=0;$r -lt 3;$r++){ for($c=0;$c -lt 9;$c++){ DrawCell (8+$c*18) ($gy+$r*18) } }
  for($c=0;$c -lt 9;$c++){ DrawCell (8+$c*18) $hy }
}
function DrawGear{  # 18x18 gear sprite at (176,16) for GodFurnaceScreen blit
  FR 176 16 193 33 $insBg
  for($y=16;$y -le 33;$y++){
    for($x=176;$x -le 193;$x++){
      $dx = $x - 184.5; $dy = $y - 24.5
      $d = [math]::Sqrt($dx*$dx+$dy*$dy)
      $adx = [math]::Abs($dx); $ady = [math]::Abs($dy)
      $isGear = $false
      if($d -le 6.2 -and $d -gt 2.5){ $isGear = $true }
      if(($adx -le 1.5) -and ($d -gt 6.2) -and ($d -le 8.4)){ $isGear = $true }
      if(($ady -le 1.5) -and ($d -gt 6.2) -and ($d -le 8.4)){ $isGear = $true }
      if(([math]::Abs($adx-$ady) -le 0.9) -and ($d -gt 6.2) -and ($d -le 8.6)){ $isGear = $true }
      if($isGear){
        $col = $gearCol
        if(($dx -lt -1) -and ($dy -lt -1) -and ($d -le 6.2)){ $col = $gearHi }
        $bmp.SetPixel($x,$y,$col)
      }
    }
  }
}
function New-Tex([string]$name,[int]$h,[scriptblock]$body){
  $script:bmp = New-Object System.Drawing.Bitmap(256,256)
  $script:g = [System.Drawing.Graphics]::FromImage($script:bmp)
  DrawPanel $h
  & $body
  $script:g.Dispose()
  $path = Join-Path $gui "$name.png"
  $script:bmp.Save($path,[System.Drawing.Imaging.ImageFormat]::Png)
  $script:bmp.Dispose()
  "OK $name ($h)"
}

# 1. furnace: 6+6 slots + labels inset + gear sprite; inv 84 / hotbar 142
New-Tex 'god_furnace' 172 {
  DrawInset 118 12 170 76
  for($i=0;$i -lt 6;$i++){ DrawCell (8+$i*18) 17; DrawCell (8+$i*18) 53 }
  InvGrid 84 142
  DrawGear
}
# 2. furnace config: hint chip only
New-Tex 'god_furnace_config' 146 {
  DrawInset 4 92 172 108
}
# 3. miner: status inset; inv 156 / hotbar 214 (H=234)
New-Tex 'god_miner' 234 {
  DrawInset 4 90 172 122
  InvGrid 156 214
}
# 4+5. resource / drop: single input slot; inv 84 / hotbar 142 (H=166)
foreach($t in @('god_resource','god_drop')){
  New-Tex $t 166 {
    DrawCell 79 35
    InvGrid 84 142
  }
}
# 6. enchant: input slot + list inset; inv 178 / hotbar 236 (H=256, fits 256 canvas)
New-Tex 'god_enchant' 256 {
  DrawCell 79 35
  DrawInset 4 54 172 155
  InvGrid 178 236
}
# 7. change: full content inset
New-Tex 'god_change' 120 {
  DrawInset 4 14 172 116
}
"done"
