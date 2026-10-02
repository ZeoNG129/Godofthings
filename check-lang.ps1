# check-lang.ps1 -- language file consistency check for God of Things.
#
# Checks:
#   1. zh_cn.json and en_us.json have exactly the same key set (both directions)
#   2. no empty values in either file
#   3. every translatable("literal") found in the Java sources has a lang entry
#   4. each runtime-concatenated key prefix still resolves to at least one real key
#
# ASCII-only on purpose: PowerShell 5.1 misreads a UTF-8-no-BOM script containing Chinese,
# and this script must run both locally (PS 5.1 / pwsh) and in CI.
#
# Exit code 0 = all good, 1 = problems found (CI friendly).

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$langDir = Join-Path $root 'src\main\resources\assets\godofthings\lang'
$javaDir = Join-Path $root 'src\main\java'

# Key prefixes that are concatenated at runtime (e.g. translatable("gui....side." + dir)).
# Verified by hand: every combination they can produce exists in the lang files.
$prefixAllow = @(
    'gui.godofthings.dimension_config.mode.',
    'gui.godofthings.dimension_config.preview.role.',
    'gui.godofthings.god_change.time_',
    'gui.godofthings.god_change.weather_',
    'gui.godofthings.wireless_logistics.',
    'gui.godofthings.wireless_logistics.side.'
)

function Read-Lang([string]$path) {
    $text = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
    return $text | ConvertFrom-Json
}

$problems = 0

$zhPath = Join-Path $langDir 'zh_cn.json'
$enPath = Join-Path $langDir 'en_us.json'
$zh = Read-Lang $zhPath
$en = Read-Lang $enPath
$zhKeys = @($zh.PSObject.Properties.Name)
$enKeys = @($en.PSObject.Properties.Name)

Write-Host ("zh_cn keys : {0}" -f $zhKeys.Count)
Write-Host ("en_us keys : {0}" -f $enKeys.Count)

$onlyZh = @(Compare-Object $zhKeys $enKeys | Where-Object { $_.SideIndicator -eq '<=' } | ForEach-Object { $_.InputObject })
$onlyEn = @(Compare-Object $zhKeys $enKeys | Where-Object { $_.SideIndicator -eq '=>' } | ForEach-Object { $_.InputObject })

if ($onlyZh.Count -gt 0) {
    Write-Host ("[FAIL] missing in en_us ({0}):" -f $onlyZh.Count)
    $onlyZh | ForEach-Object { Write-Host ("        {0}" -f $_) }
    $problems++
}
if ($onlyEn.Count -gt 0) {
    Write-Host ("[FAIL] missing in zh_cn ({0}):" -f $onlyEn.Count)
    $onlyEn | ForEach-Object { Write-Host ("        {0}" -f $_) }
    $problems++
}

foreach ($pair in @(@('zh_cn', $zh), @('en_us', $en))) {
    $name = $pair[0]
    $obj = $pair[1]
    $empty = @($obj.PSObject.Properties | Where-Object { [string]::IsNullOrWhiteSpace($_.Value) } | ForEach-Object { $_.Name })
    if ($empty.Count -gt 0) {
        Write-Host ("[FAIL] empty values in {0} ({1}): {2}" -f $name, $empty.Count, ($empty -join ', '))
        $problems++
    }
}

# --- every translatable("literal") in the sources must exist ---
$keys = @{}
foreach ($k in $enKeys) { $keys[$k] = $true }

$missing = New-Object System.Collections.Generic.SortedSet[string]
$scanned = 0
foreach ($file in Get-ChildItem -Path $javaDir -Recurse -Filter *.java) {
    $scanned++
    $text = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
    foreach ($m in [regex]::Matches($text, 'translatable\(\s*"([^"]+)"')) {
        $key = $m.Groups[1].Value
        if ($prefixAllow -contains $key) { continue }
        if (-not $keys.ContainsKey($key)) { [void]$missing.Add($key) }
    }
}

Write-Host ("java files scanned: {0}" -f $scanned)
if ($missing.Count -gt 0) {
    Write-Host ("[FAIL] translatable() keys with no lang entry ({0}):" -f $missing.Count)
    foreach ($k in $missing) { Write-Host ("        {0}" -f $k) }
    $problems++
}

# --- each runtime prefix must still resolve ---
foreach ($prefix in $prefixAllow) {
    $hits = @($enKeys | Where-Object { $_.StartsWith($prefix) })
    if ($hits.Count -eq 0) {
        Write-Host ("[FAIL] runtime prefix resolves to nothing: {0}" -f $prefix)
        $problems++
    }
}

if ($problems -eq 0) {
    Write-Host "lang check: OK"
    exit 0
}

Write-Host ("lang check: {0} problem group(s) found" -f $problems)
exit 1
