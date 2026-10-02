# check-lang.ps1 -- static consistency checks for God of Things (language + assets).
#
# Checks:
#   1. zh_cn.json and en_us.json have exactly the same key set (both directions)
#   2. no empty values in either file
#   3. every translatable("literal") found in the Java sources has a lang entry
#   4. each runtime-concatenated key prefix still resolves to at least one real key
#   5. every registered block has a blockstate json
#   6. every registered non-block item has a models/item json
#   7. every registered item / block has a display-name lang key
#   8. every registered entity has an entity.godofthings.<id> lang key
#   9. reverse: a blockstate whose block id is no longer registered (stale resource)
#  10. reverse: a models/item json whose item id is no longer registered (warning only,
#      variant models such as the staff's silk-touch override are legitimate)
#
# ASCII-only on purpose: PowerShell 5.1 misreads a UTF-8-no-BOM script containing Chinese,
# and this script must run both locally (PS 5.1 / pwsh) and in CI (pwsh on Linux).
#
# Exit code 0 = all good, 1 = problems found (CI friendly).

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$langDir = Join-Path $root 'src\main\resources\assets\godofthings\lang'
$resDir = Join-Path $root 'src\main\resources\assets\godofthings'
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

# Item models that intentionally belong to no item id: model overrides picked by the
# minecraft:custom_model_data component (see EndlessBeafItem / EnchantmentSwitchPacket).
$itemModelAllow = @(
    'wondrous_staff_silk_touch'
)

function Read-Lang([string]$path) {
    $text = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
    return $text | ConvertFrom-Json
}

$problems = 0

# ---------------------------------------------------------------- language files
$zh = Read-Lang (Join-Path $langDir 'zh_cn.json')
$en = Read-Lang (Join-Path $langDir 'en_us.json')
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

# ---------------------------------------------------------------- java side scan
$keys = @{}
foreach ($k in $enKeys) { $keys[$k] = $true }

$missing = New-Object System.Collections.Generic.SortedSet[string]
$blocks = New-Object System.Collections.Generic.HashSet[string]
$items = New-Object System.Collections.Generic.HashSet[string]
$entities = New-Object System.Collections.Generic.HashSet[string]

$reBlock = [regex]'BLOCKS\.register(?:Block)?\(\s*"([a-z0-9_]+)"'
$reItem = [regex]'ITEMS\.register(?:Item)?\(\s*"([a-z0-9_]+)"'
$reEntity = [regex]'(?<![A-Z_])(?:ENTITIES|ENTITY_TYPES)\.register\(\s*"([a-z0-9_]+)"'
$reTrans = [regex]'translatable\(\s*"([^"]+)"'

$scanned = 0
foreach ($file in Get-ChildItem -Path $javaDir -Recurse -Filter *.java) {
    $scanned++
    $text = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
    foreach ($m in $reTrans.Matches($text)) {
        $key = $m.Groups[1].Value
        if ($prefixAllow -contains $key) { continue }
        if (-not $keys.ContainsKey($key)) { [void]$missing.Add($key) }
    }
    foreach ($m in $reBlock.Matches($text)) { [void]$blocks.Add($m.Groups[1].Value) }
    foreach ($m in $reItem.Matches($text)) { [void]$items.Add($m.Groups[1].Value) }
    foreach ($m in $reEntity.Matches($text)) { [void]$entities.Add($m.Groups[1].Value) }
}

Write-Host ("java files scanned: {0}   blocks: {1}   items: {2}   entities: {3}" -f `
        $scanned, $blocks.Count, $items.Count, $entities.Count)

if ($missing.Count -gt 0) {
    Write-Host ("[FAIL] translatable() keys with no lang entry ({0}):" -f $missing.Count)
    foreach ($k in $missing) { Write-Host ("        {0}" -f $k) }
    $problems++
}

foreach ($prefix in $prefixAllow) {
    $hits = @($enKeys | Where-Object { $_.StartsWith($prefix) })
    if ($hits.Count -eq 0) {
        Write-Host ("[FAIL] runtime prefix resolves to nothing: {0}" -f $prefix)
        $problems++
    }
}

# ---------------------------------------------------------------- asset coverage
$missingBlockstate = New-Object System.Collections.Generic.SortedSet[string]
foreach ($id in $blocks) {
    $state = Join-Path $resDir ("blockstates\{0}.json" -f $id)
    if (-not (Test-Path -LiteralPath $state)) { [void]$missingBlockstate.Add($id) }
}
if ($missingBlockstate.Count -gt 0) {
    Write-Host ("[FAIL] registered blocks without a blockstate ({0}):" -f $missingBlockstate.Count)
    foreach ($id in $missingBlockstate) { Write-Host ("        {0}" -f $id) }
    $problems++
}

$missingModel = New-Object System.Collections.Generic.SortedSet[string]
foreach ($id in $items) {
    if ($blocks.Contains($id)) { continue }   # block items are rendered from the block model
    $model = Join-Path $resDir ("models\item\{0}.json" -f $id)
    if (-not (Test-Path -LiteralPath $model)) { [void]$missingModel.Add($id) }
}
if ($missingModel.Count -gt 0) {
    Write-Host ("[FAIL] registered items without a models/item json ({0}):" -f $missingModel.Count)
    foreach ($id in $missingModel) { Write-Host ("        {0}" -f $id) }
    $problems++
}

$missingName = New-Object System.Collections.Generic.SortedSet[string]
foreach ($id in ($blocks + $items)) {
    if (-not ($keys.ContainsKey("item.godofthings.$id") -or $keys.ContainsKey("block.godofthings.$id"))) {
        [void]$missingName.Add($id)
    }
}
if ($missingName.Count -gt 0) {
    Write-Host ("[FAIL] registered items/blocks without a display-name lang key ({0}):" -f $missingName.Count)
    foreach ($id in $missingName) { Write-Host ("        {0}" -f $id) }
    $problems++
}

$missingEntityName = New-Object System.Collections.Generic.SortedSet[string]
foreach ($id in $entities) {
    if (-not $keys.ContainsKey("entity.godofthings.$id")) { [void]$missingEntityName.Add($id) }
}
if ($missingEntityName.Count -gt 0) {
    Write-Host ("[FAIL] registered entities without an entity.godofthings.<id> lang key ({0}):" -f $missingEntityName.Count)
    foreach ($id in $missingEntityName) { Write-Host ("        {0}" -f $id) }
    $problems++
}

$stateDir = Join-Path $resDir 'blockstates'
$staleStates = @()
foreach ($file in Get-ChildItem -Path $stateDir -Filter *.json) {
    $id = $file.BaseName
    if (-not $blocks.Contains($id)) { $staleStates += $id }
}
if ($staleStates.Count -gt 0) {
    Write-Host ("[FAIL] blockstates for blocks that are no longer registered ({0}):" -f $staleStates.Count)
    $staleStates | ForEach-Object { Write-Host ("        {0}" -f $_) }
    $problems++
}

$modelDir = Join-Path $resDir 'models\item'
$staleModels = @()
foreach ($file in Get-ChildItem -Path $modelDir -Filter *.json) {
    $id = $file.BaseName
    if ($itemModelAllow -contains $id) { continue }
    if (-not ($items.Contains($id) -or $blocks.Contains($id))) { $staleModels += $id }
}
if ($staleModels.Count -gt 0) {
    Write-Host ("[warn] models/item json for ids that are no longer registered ({0}) -- variant models are allowed, see -itemModelAllow:" -f $staleModels.Count)
    $staleModels | ForEach-Object { Write-Host ("        {0}" -f $_) }
}

# ---------------------------------------------------------------- scripts must stay ASCII-only
# A UTF-8-no-BOM .ps1 containing non-ASCII can be misread by PowerShell 5.1, and a mangled
# comment can even swallow the next line of code (this really happened to fetch-libs.ps1).
foreach ($script in Get-ChildItem -Path $root -Filter *.ps1) {
    $bytes = [System.IO.File]::ReadAllBytes($script.FullName)
    $bad = 0
    foreach ($b in $bytes) { if ($b -gt 127) { $bad++ } }
    if ($bad -gt 0) {
        Write-Host ("[FAIL] {0} contains {1} non-ASCII byte(s); keep repo scripts ASCII-only" -f $script.Name, $bad)
        $problems++
    }
}

if ($problems -eq 0) {
    Write-Host "checks: OK"
    exit 0
}

Write-Host ("checks: {0} problem group(s) found" -f $problems)
exit 1
