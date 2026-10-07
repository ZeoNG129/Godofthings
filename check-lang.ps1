# check-lang.ps1 -- static consistency checks for God of Things (language + assets).
#
# Checks:
#   1. zh_cn.json and en_us.json have exactly the same key set (both directions)
#   2. no empty values in either file
#   3. every translatable("literal") found in the Java sources has a lang entry
#   4. each runtime-concatenated key prefix still resolves to at least one real key
#   5. doc count claims in AGENTS.md / README.md match the source tree
#   6. repo root is free of extracted-jar contamination (v5.15.6 incident)
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
    'gui.godofthings.backpack.sort.',
    'gui.godofthings.god_change.time_',
    'gui.godofthings.god_change.weather_',
    # Two more runtime concatenations come from the manual:
    #   gui.godofthings.manual.tab.<category>  (ManualCategory.label)
    #   manual.godofthings.<registry id / system.<id>>  (ManualCatalog.make)
    # Every concrete key under them is checked separately (see the "manual coverage" block below),
    # so registering the bare prefixes here is enough.
    'gui.godofthings.manual.tab.',
    'manual.godofthings.'
)

$itemModelAllow = @()

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

# ---------------------------------------------------------------- manual coverage
# Every registered item / block must have a manual entry (ManualCatalog builds entries straight
# from the registry, so a missing lang key means the entry would render as a raw key), and every
# system entry declared in ManualCatalog.SYSTEM_IDS must have both a title and a body.
$missingManual = New-Object System.Collections.Generic.SortedSet[string]
$allContent = New-Object System.Collections.Generic.HashSet[string]
foreach ($id in $blocks) { [void]$allContent.Add($id) }
foreach ($id in $items) { [void]$allContent.Add($id) }
foreach ($id in $allContent) {
    if (-not $keys.ContainsKey("manual.godofthings.$id")) { [void]$missingManual.Add($id) }
}
if ($missingManual.Count -gt 0) {
    Write-Host ("[FAIL] registered items/blocks without a manual entry (manual.godofthings.<id>) ({0}):" -f $missingManual.Count)
    foreach ($id in $missingManual) { Write-Host ("        {0}" -f $id) }
    $problems++
}

$catalogFile = Join-Path $javaDir 'com\godofthings\manual\ManualCatalog.java'
if (Test-Path -LiteralPath $catalogFile) {
    $catalogText = [System.IO.File]::ReadAllText($catalogFile, [System.Text.Encoding]::UTF8)
    $listMatch = [regex]::Match($catalogText, 'SYSTEM_IDS\s*=\s*List\.of\((.*?)\);', 'Singleline')
    if (-not $listMatch.Success) {
        Write-Host '[FAIL] cannot find SYSTEM_IDS in ManualCatalog.java'
        $problems++
    } else {
        $missingSystem = @()
        foreach ($m in [regex]::Matches($listMatch.Groups[1].Value, '"([a-z0-9_]+)"')) {
            $sid = $m.Groups[1].Value
            foreach ($suffix in @('', '.title')) {
                if (-not $keys.ContainsKey("manual.godofthings.system.$sid$suffix")) {
                    $missingSystem += "manual.godofthings.system.$sid$suffix"
                }
            }
        }
        if ($missingSystem.Count -gt 0) {
            Write-Host ("[FAIL] system manual entries missing a title/body key ({0}):" -f $missingSystem.Count)
            foreach ($k in $missingSystem) { Write-Host ("        {0}" -f $k) }
            $problems++
        }
        $systemCount = ([regex]::Matches($listMatch.Groups[1].Value, '"([a-z0-9_]+)"')).Count
        Write-Host ("manual: {0} item/block entries + {1} system entries" -f $allContent.Count, $systemCount)
    }
} else {
    Write-Host '[warn] ManualCatalog.java not found, manual coverage not checked'
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

# ---------------------------------------------------------------- tag entries must reference registered ids
# A tag json referencing an id that is no longer registered makes the WHOLE tag fail to load
# at runtime (every block in it silently loses e.g. its mining tier) - this really happened
# with mineable/pickaxe when god_heaven_enchant was deleted but its tag entry survived.
# Only godofthings: entries are validated (other namespaces are not ours); "#..." tag
# references point at other tags, not registry entries, and are skipped.
$reTagId = [regex]'"godofthings:([a-z0-9_./-]+)"'
$staleTagEntries = New-Object System.Collections.Generic.SortedSet[string]
$tagFiles = 0
foreach ($tagFile in Get-ChildItem -Path (Join-Path $root 'src\main\resources\data') -Recurse -Filter *.json) {
    $tagDirMatch = [regex]::Match($tagFile.FullName, '\\tags\\([a-z_]+)\\')
    if (-not $tagDirMatch.Success) { continue }
    $tagRegistry = $tagDirMatch.Groups[1].Value
    if ($tagRegistry -ne 'block' -and $tagRegistry -ne 'item') { continue }
    $tagFiles++
    $tagText = [System.IO.File]::ReadAllText($tagFile.FullName, [System.Text.Encoding]::UTF8)
    foreach ($m in $reTagId.Matches($tagText)) {
        $id = $m.Groups[1].Value
        $registered = if ($tagRegistry -eq 'block') { $blocks.Contains($id) } else { $items.Contains($id) }
        if (-not $registered) {
            [void]$staleTagEntries.Add(('{0} {1}  <- {2}' -f $tagRegistry, $id, $tagFile.FullName.Substring($root.Length + 1)))
        }
    }
}
if ($staleTagEntries.Count -gt 0) {
    Write-Host ("[FAIL] tag entries referencing unregistered godofthings ids ({0}) across {1} tag file(s):" -f $staleTagEntries.Count, $tagFiles)
    foreach ($e in $staleTagEntries) { Write-Host ("        {0}" -f $e) }
    $problems++
} else {
    Write-Host ("tags: {0} block/item tag file(s), all godofthings entries registered" -f $tagFiles)
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


# ---------------------------------------------------------------- docs must match the source of truth
# Count claims in AGENTS.md / README.md are checked against the actual source tree, so a
# stale doc like the 5.14.0-era "19 tests / 7 grid BEs / 30 jars" cannot happen again.
# Chinese claims are matched via codepoint-built patterns so this file stays ASCII-only.
function Get-CjkPattern([int[]]$cp) { return -join ($cp | ForEach-Object { [char]$_ }) }
$actualGameTests = 0
$gtActual = @{}
foreach ($f in Get-ChildItem -Path (Join-Path $root 'src/main/java/com/godofthings/gametest') -Filter *.java) {
    $n = ([regex]::Matches((Get-Content $f.FullName -Raw), '@GameTest\(')).Count
    $gtActual[$f.BaseName] = $n
    $actualGameTests += $n
}
$actualAe2Bes = 0
foreach ($f in Get-ChildItem -Path (Join-Path $root 'src/main/java/com/godofthings/block/entity') -Filter *.java) {
    if ((Get-Content $f.FullName -Raw) -match 'implements[^\r\n]*IGridConnectedBlockEntity') { $actualAe2Bes++ }
}
$actualAdvancements = @(Get-ChildItem -Path (Join-Path $root 'src/main/resources/data/godofthings/advancement') -Filter *.json -File).Count
$agentsText = Get-Content (Join-Path $root 'AGENTS.md') -Raw -Encoding UTF8
$readmeText = Get-Content (Join-Path $root 'README.md') -Raw -Encoding UTF8
$docsText = $agentsText + $readmeText
$cjkRegTest    = Get-CjkPattern 0x56DE,0x5F52,0x6D4B,0x8BD5
$cjkParenOpen  = Get-CjkPattern 0xFF08
$cjkParenClose = Get-CjkPattern 0xFF09
$cjkGong       = Get-CjkPattern 0x5171
$cjkTiao       = Get-CjkPattern 0x6761
$cjkXiang      = Get-CjkPattern 0x9879
$cjkBlockEnt   = Get-CjkPattern 0x4E2A,0x65B9,0x5757,0x5B9E,0x4F53
$cjkGridMach   = Get-CjkPattern 0x53F0,0x4EA7,0x8D44,0x6E90,0x673A,0x5668
$cjkAchieve    = Get-CjkPattern 0x4E2A,0x6210,0x5C31
$cjkRegistered = Get-CjkPattern 0x6CE8,0x518C,0x7684
$cjkBlocks     = Get-CjkPattern 0x4E2A,0x65B9,0x5757
$cjkEntities   = Get-CjkPattern 0x4E2A,0x5B9E,0x4F53
$cjkItems      = Get-CjkPattern 0x4E2A,0x7269,0x54C1
$cjkPrefixes   = Get-CjkPattern 0x4E2A,0x952E,0x524D,0x7F00
$reTotalClaim = $cjkRegTest + $cjkParenOpen + '\s*' + $cjkGong + '\s*(\d+)\s*(' + $cjkTiao + '|' + $cjkXiang + ')' + $cjkParenClose
$totalClaims = [regex]::Matches($docsText, $reTotalClaim)
if ($totalClaims.Count -lt 1) {
    Write-Host '[FAIL] docs: no regression-test total claim found in AGENTS.md/README.md'
    $problems++
} else {
    foreach ($m in $totalClaims) {
        if ([int]$m.Groups[1].Value -ne $actualGameTests) {
            Write-Host ('[FAIL] docs claim {0} game tests but source has {1}' -f $m.Groups[1].Value, $actualGameTests)
            $problems++
        }
    }
}
foreach ($cls in $gtActual.Keys) {
    $reCls = [regex]::Escape($cls) + '`' + $cjkParenOpen + '\s*(\d+)\s*' + $cjkTiao + $cjkParenClose
    $m = [regex]::Match($agentsText, $reCls)
    if (-not $m.Success) {
        Write-Host ('[FAIL] docs: no per-class count claim for game test {0}' -f $cls)
        $problems++
    } elseif ([int]$m.Groups[1].Value -ne $gtActual[$cls]) {
        Write-Host ('[FAIL] docs claim {0} tests in {1} but source has {2}' -f $m.Groups[1].Value, $cls, $gtActual[$cls])
        $problems++
    }
}
$reAe2Agents = '(\d+)\s*' + $cjkBlockEnt
$ae2Matched = $false
foreach ($m in [regex]::Matches($agentsText, $reAe2Agents)) {
    $ae2Matched = $true
    if ([int]$m.Groups[1].Value -ne $actualAe2Bes) {
        Write-Host ('[FAIL] docs claim {0} AE2 grid BEs but source has {1}' -f $m.Groups[1].Value, $actualAe2Bes)
        $problems++
    }
}
if (-not $ae2Matched) {
    Write-Host '[FAIL] docs: no AE2 grid-connected block-entity count claim in AGENTS.md'
    $problems++
}
$reAe2Readme = '(\d+)\s*' + $cjkGridMach
$ae2rMatched = $false
foreach ($m in [regex]::Matches($readmeText, $reAe2Readme)) {
    $ae2rMatched = $true
    if ([int]$m.Groups[1].Value -ne $actualAe2Bes) {
        Write-Host ('[FAIL] docs claim {0} AE2 grid machines but source has {1}' -f $m.Groups[1].Value, $actualAe2Bes)
        $problems++
    }
}
if (-not $ae2rMatched) {
    Write-Host '[FAIL] docs: no AE2 grid-machine count claim in README.md'
    $problems++
}
$reAdv = '(\d+)\s*' + $cjkAchieve
$advMatched = $false
foreach ($m in [regex]::Matches($docsText, $reAdv)) {
    $advMatched = $true
    if ([int]$m.Groups[1].Value -ne $actualAdvancements) {
        Write-Host ('[FAIL] docs claim {0} advancements but source has {1}' -f $m.Groups[1].Value, $actualAdvancements)
        $problems++
    }
}
if (-not $advMatched) {
    Write-Host '[FAIL] docs: no advancement count claim found in AGENTS.md/README.md'
    $problems++
}
$reCounts = $cjkRegistered + '\s*(\d+)\s*' + $cjkBlocks + '\s*/\s*(\d+)\s*' + $cjkItems + '\s*/\s*(\d+)\s*' + $cjkEntities
$mCounts = [regex]::Match($readmeText, $reCounts)
if (-not $mCounts.Success) {
    Write-Host '[FAIL] docs: no registered block/item/entity count claim in README.md'
    $problems++
} else {
    if ([int]$mCounts.Groups[1].Value -ne $blocks.Count) {
        Write-Host ('[FAIL] README claims {0} blocks but check-lang found {1}' -f $mCounts.Groups[1].Value, $blocks.Count)
        $problems++
    }
    if ([int]$mCounts.Groups[2].Value -ne $items.Count) {
        Write-Host ('[FAIL] README claims {0} items but check-lang found {1}' -f $mCounts.Groups[2].Value, $items.Count)
        $problems++
    }
    if ([int]$mCounts.Groups[3].Value -ne $entities.Count) {
        Write-Host ('[FAIL] README claims {0} entities but check-lang found {1}' -f $mCounts.Groups[3].Value, $entities.Count)
        $problems++
    }
}
$rePrefix = '(\d+)\s*' + $cjkPrefixes
$mPrefix = [regex]::Match($readmeText, $rePrefix)
if (-not $mPrefix.Success) {
    Write-Host '[FAIL] docs: no key-prefix count claim in README.md'
    $problems++
} elseif ([int]$mPrefix.Groups[1].Value -ne $prefixAllow.Count) {
    Write-Host ('[FAIL] README claims {0} key prefixes but check-lang has {1}' -f $mPrefix.Groups[1].Value, $prefixAllow.Count)
    $problems++
}
Write-Host ('docs: gametests {0} / ae2 grid BEs {1} / advancements {2} -- claims in AGENTS.md + README.md verified' -f $actualGameTests, $actualAe2Bes, $actualAdvancements)

# ------------------------------------------------- repo root must stay free of extracted jars
# v5.15.6 accidentally committed an extracted ToolBelt jar at the repo root (assets/, data/,
# META-INF/, dev/, .cache/, loadscreens/, lowercase licenses/ ...). Windows is case-insensitive,
# so all names below are matched case-insensitively EXCEPT 'licenses', which is compared
# case-SENSITIVELY (-ceq) so our real uppercase LICENSES/ archive is not flagged.
foreach ($badName in @('.cache', 'assets', 'data', 'dev', 'loadscreens', 'meta-inf')) {
    $hit = Get-ChildItem -Path $root -Directory | Where-Object { $_.Name -ieq $badName }
    if ($hit) {
        Write-Host ('[FAIL] repo root contains extracted-jar directory {0} (delete it; the mod lives under src/main/resources)' -f $badName)
        $problems++
    }
}
$lowerLic = Get-ChildItem -Path $root -Directory | Where-Object { $_.Name -ceq 'licenses' }
if ($lowerLic) {
    Write-Host "[FAIL] repo root contains lowercase 'licenses' (our archive is uppercase LICENSES/; a lowercase copy means an extracted jar was dumped here)"
    $problems++
}
if ($problems -eq 0) {
    Write-Host 'root: clean (no extracted-jar contamination)'
}

if ($problems -eq 0) {
    Write-Host "checks: OK"
    exit 0
}

Write-Host ("checks: {0} problem group(s) found" -f $problems)
exit 1
