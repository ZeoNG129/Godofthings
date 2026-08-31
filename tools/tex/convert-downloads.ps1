$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

Add-Type -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Collections.Generic;

public static class TexProc {
    // Flood-fill background removal from borders, tolerance = max channel diff vs dominant border color.
    public static double RemoveBackground(Bitmap bmp, int tol) {
        int w = bmp.Width, h = bmp.Height;
        Rectangle rect = new Rectangle(0, 0, w, h);
        BitmapData d = bmp.LockBits(rect, ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
        int stride = d.Stride;
        byte[] px = new byte[stride * h];
        System.Runtime.InteropServices.Marshal.Copy(d.Scan0, px, 0, px.Length);
        // dominant border color (RGB packed)
        Dictionary<int, int> counts = new Dictionary<int, int>();
        Action<int> count = delegate(int i) {
            int key = (px[i + 2] << 16) | (px[i + 1] << 8) | px[i];
            int c; counts.TryGetValue(key, out c); counts[key] = c + 1;
        };
        for (int x = 0; x < w; x++) { count(x * 4); count((h - 1) * stride + x * 4); }
        for (int y = 0; y < h; y++) { count(y * stride); count(y * stride + (w - 1) * 4); }
        int domKey = 0, domN = -1;
        foreach (KeyValuePair<int, int> kv in counts) { if (kv.Value > domN) { domN = kv.Value; domKey = kv.Key; } }
        int dr = (domKey >> 16) & 0xFF, dg = (domKey >> 8) & 0xFF, db = domKey & 0xFF;
        int total = w * h, removed = 0;
        Stack<int> stack = new Stack<int>();
        bool[] done = new bool[total];
        Func<int, bool> isBg = delegate(int i) {
            if (px[i + 3] == 0) return true;
            int dr2 = Math.Abs(px[i + 2] - dr), dg2 = Math.Abs(px[i + 1] - dg), db2 = Math.Abs(px[i] - db);
            return dr2 <= tol && dg2 <= tol && db2 <= tol;
        };
        for (int x = 0; x < w; x++) {
            int a = x, b = (h - 1) * w + x;
            if (!done[a] && isBg(a * 4)) { done[a] = true; stack.Push(a); }
            if (!done[b] && isBg(b * 4)) { done[b] = true; stack.Push(b); }
        }
        for (int y = 0; y < h; y++) {
            int a = y * w, b = y * w + w - 1;
            if (!done[a] && isBg(a * 4)) { done[a] = true; stack.Push(a); }
            if (!done[b] && isBg(b * 4)) { done[b] = true; stack.Push(b); }
        }
        while (stack.Count > 0) {
            int p = stack.Pop(); int i = p * 4;
            if (px[i + 3] != 0) { px[i + 3] = 0; removed++; }
            int x = p % w, y = p / w;
            if (x > 0)     { int q = p - 1;     if (!done[q] && isBg(i - 4))   { done[q] = true; stack.Push(q); } }
            if (x < w - 1) { int q = p + 1;     if (!done[q] && isBg(i + 4))   { done[q] = true; stack.Push(q); } }
            if (y > 0)     { int q = p - w;     if (!done[q] && isBg(i - stride)) { done[q] = true; stack.Push(q); } }
            if (y < h - 1) { int q = p + w;     if (!done[q] && isBg(i + stride)) { done[q] = true; stack.Push(q); } }
        }
        System.Runtime.InteropServices.Marshal.Copy(px, 0, d.Scan0, px.Length);
        bmp.UnlockBits(d);
        return (double)removed / total;
    }

    // Alpha-aware box-average downscale.
    public static Bitmap Downscale(Bitmap src, int size) {
        Bitmap dst = new Bitmap(size, size, PixelFormat.Format32bppArgb);
        Rectangle rect = new Rectangle(0, 0, src.Width, src.Height);
        BitmapData d = src.LockBits(rect, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        int stride = d.Stride;
        byte[] px = new byte[stride * src.Height];
        System.Runtime.InteropServices.Marshal.Copy(d.Scan0, px, 0, px.Length);
        src.UnlockBits(d);
        BitmapData dd = dst.LockBits(new Rectangle(0, 0, size, size), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
        int dstride = dd.Stride;
        byte[] outPx = new byte[dstride * size];
        double fx = (double)src.Width / size, fy = (double)src.Height / size;
        for (int oy = 0; oy < size; oy++) {
            int y0 = (int)Math.Floor(oy * fy), y1 = Math.Min(src.Height, (int)Math.Ceiling((oy + 1) * fy));
            for (int ox = 0; ox < size; ox++) {
                int x0 = (int)Math.Floor(ox * fx), x1 = Math.Min(src.Width, (int)Math.Ceiling((ox + 1) * fx));
                double sa = 0, sr = 0, sg = 0, sb = 0; int n = 0;
                for (int y = y0; y < y1; y++) {
                    int row = y * stride;
                    for (int x = x0; x < x1; x++) {
                        int i = row + x * 4;
                        int a = px[i + 3]; n++;
                        sa += a;
                        if (a > 0) {
                            double wgt = a / 255.0;
                            sr += px[i + 2] * wgt; sg += px[i + 1] * wgt; sb += px[i] * wgt;
                        }
                    }
                }
                int oi = oy * dstride + ox * 4;
                if (sa > 0) {
                    double aw = sa / 255.0; // effective opaque pixel count
                    if (aw > 0.0001) {
                        double r = sr / aw, g = sg / aw, b = sb / aw;
                        outPx[oi + 3] = (byte)Math.Round(Math.Min(255.0, sa / n));
                        outPx[oi + 2] = (byte)Math.Round(Math.Min(255.0, r));
                        outPx[oi + 1] = (byte)Math.Round(Math.Min(255.0, g));
                        outPx[oi]     = (byte)Math.Round(Math.Min(255.0, b));
                    }
                }
            }
        }
        System.Runtime.InteropServices.Marshal.Copy(outPx, 0, dd.Scan0, outPx.Length);
        dst.UnlockBits(dd);
        return dst;
    }

    // Force every pixel fully opaque (block tiles).
    public static void ForceOpaque(Bitmap bmp) {
        Rectangle rect = new Rectangle(0, 0, bmp.Width, bmp.Height);
        BitmapData d = bmp.LockBits(rect, ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
        byte[] px = new byte[d.Stride * bmp.Height];
        System.Runtime.InteropServices.Marshal.Copy(d.Scan0, px, 0, px.Length);
        for (int i = 3; i < px.Length; i += 4) px[i] = 255;
        System.Runtime.InteropServices.Marshal.Copy(px, 0, d.Scan0, px.Length);
        bmp.UnlockBits(d);
    }

    // Binary-ish alpha: kill residue (a<10 -> 0), snap near-opaque (a>240 -> 255).
    public static void CleanupAlpha(Bitmap bmp) {
        Rectangle rect = new Rectangle(0, 0, bmp.Width, bmp.Height);
        BitmapData d = bmp.LockBits(rect, ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
        byte[] px = new byte[d.Stride * bmp.Height];
        System.Runtime.InteropServices.Marshal.Copy(d.Scan0, px, 0, px.Length);
        for (int i = 3; i < px.Length; i += 4) {
            if (px[i] < 10) px[i] = 0; else if (px[i] > 240) px[i] = 255;
        }
        System.Runtime.InteropServices.Marshal.Copy(px, 0, d.Scan0, px.Length);
        bmp.UnlockBits(d);
    }
}
'@ -ReferencedAssemblies System.Drawing

$src = 'D:\' + [char]0x4E0B + [char]0x8F7D  # D:\下载 (unicode-escaped: PS5.1 reads BOM-less scripts as ANSI)
$stage = 'build\tex-2.0.5'
Remove-Item $stage -Recurse -Force -ErrorAction SilentlyContinue
New-Item "$stage\item" -ItemType Directory -Force | Out-Null
New-Item "$stage\block" -ItemType Directory -Force | Out-Null

# mapping: source file -> dest kind/name
$map = @(
  @('god_chestplate.png',          'item', 'god_chestplate.png'),
  @('god_helmet.png',              'item', 'god_helmet.png'),
  @('god_leggings.png',            'item', 'god_leggings.png'),
  @('god_boots.png',               'item', 'god_boots.png'),
  @('god_favor_wand.png',          'item', 'god_favor_wand.png'),
  @('god_favor_wand_fortune.png',  'item', 'god_favor_wand_fortune.png'),
  @('god_unbreakable.png',         'item', 'god_unbreakable.png'),
  @('enchant_scroll.png',          'item', 'enchant_scroll.png'),
  @('god_change.png',              'item', 'god_change.png'),
  @('god_craft_item.png',          'item', 'god_craft.png'),
  @('superflat_teleporter_item.png','item','superflat_teleporter.png'),
  @('void_teleporter_item.png',    'item', 'void_teleporter.png'),
  @('god_furnace_side.png',        'block', 'god_furnace_side.png'),
  @('god_furnace_top.png',         'block', 'god_furnace_top.png'),
  @('god_miner_side.png',          'block', 'god_miner_side.png'),
  @('god_miner_top.png',           'block', 'god_miner_top.png'),
  @('god_resource_side.png',       'block', 'god_resource_side.png'),
  @('god_resource_top.png',        'block', 'god_resource_top.png'),
  @('god_drop_side.png',           'block', 'god_drop_side.png'),
  @('god_drop_top.png',            'block', 'god_drop_top.png'),
  @('god_enchant_side.png',        'block', 'god_enchant_side.png'),
  @('god_enchant_top.png',         'block', 'god_enchant_top.png'),
  @('god_heaven_enchant_side.png', 'block', 'god_heaven_enchant_side.png'),
  @('god_heaven_enchant_top.png',  'block', 'god_heaven_enchant_top.png'),
  @('god_craft_side.png',          'block', 'god_craft_side.png'),
  @('god_craft_top.png',           'block', 'god_craft_top.png'),
  @('superflat_teleporter.png',    'block', 'superflat_teleporter.png'),
  @('void_teleporter.png',         'block', 'void_teleporter.png')
)

$report = @()
foreach ($m in $map) {
  $in = Join-Path $src $m[0]
  if (-not (Test-Path $in)) { $report += "MISSING $($m[0])"; continue }
  $bmpIn0 = New-Object System.Drawing.Bitmap($in)
  # CRITICAL: source PNGs may be 24bppRgb -> LockBits write-back is lost on conversion.
  # Clone to standard 32bppArgb before any in-place pixel work.
  $rect = New-Object System.Drawing.Rectangle 0, 0, $bmpIn0.Width, $bmpIn0.Height
  $bmpIn = $bmpIn0.Clone($rect, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $bmpIn0.Dispose()
  $hadAlpha = $false
  $bgRemoved = 0.0
  if ($m[1] -eq 'item') {
    # detect existing transparency: sample grid
    $transp = 0; $samples = 0
    for ($y = 0; $y -lt 1024; $y += 32) { for ($x = 0; $x -lt 1024; $x += 32) { $samples++; if ($bmpIn.GetPixel($x, $y).A -lt 250) { $transp++ } } }
    if ($transp -gt $samples * 0.02) { $hadAlpha = $true }
    else { $bgRemoved = [TexProc]::RemoveBackground($bmpIn, 30) }
  }
  $out = [TexProc]::Downscale($bmpIn, 16)
  $bmpIn.Dispose()
  if ($m[1] -eq 'block') { [TexProc]::ForceOpaque($out) } else { [TexProc]::CleanupAlpha($out) }
  $dest = Join-Path $stage "$($m[1])\$($m[2])"
  $out.Save($dest, [System.Drawing.Imaging.ImageFormat]::Png)
  # coverage stat: non-transparent px count of 256
  $cov = 0
  for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { if ($out.GetPixel($x, $y).A -gt 0) { $cov++ } } }
  $out.Dispose()
  $bgTxt = if ($m[1] -eq 'item') { if ($hadAlpha) { 'alpha-ok' } else { 'bg-removed {0:P0}' -f $bgRemoved } } else { 'tile' }
  $report += ("{0,-8} {1,-28} coverage {2,3}/256  {3}" -f $m[1], $m[2], $cov, $bgTxt)
}
$report
