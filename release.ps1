# release.ps1 - Publish a God of Things release to GitHub Releases.
# Usage:
#   .\release.ps1                     # uses mod_version from gradle.properties
#   .\release.ps1 -Version 1.0.1      # explicit version
#   .\release.ps1 -Notes "changelog"  # custom release notes (markdown)
# Prerequisites:
#   1. gradlew build has produced build\libs\godofthings-<version>.jar
#   2. git proxy is configured (127.0.0.1:7890) or network is reachable directly
param(
    [string]$Version = "",
    [string]$Notes = ""
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $MyInvocation.MyCommand.Path
$owner = 'ZeoNG129'
$repoName = 'Godofthings'

# 1. resolve version from gradle.properties if not given
if (-not $Version) {
    $m = Select-String -Path (Join-Path $repo 'gradle.properties') -Pattern '^mod_version=(.+)$'
    if (-not $m) { throw 'mod_version not found in gradle.properties' }
    $Version = $m.Matches.Groups[1].Value
}
$tag = "v$Version"

# 2. jar must already be built
$jar = Join-Path $repo "build\libs\godofthings-$Version.jar"
if (-not (Test-Path $jar)) { throw "jar not found: $jar - run 'gradlew build' first" }

# 3. route through local proxy if reachable; otherwise fall back to direct
$proxyUp = (Test-NetConnection -ComputerName 127.0.0.1 -Port 7890 -WarningAction SilentlyContinue).TcpTestSucceeded
if ($proxyUp) {
    $env:HTTPS_PROXY = 'http://127.0.0.1:7890'
    $env:HTTP_PROXY = 'http://127.0.0.1:7890'
} else {
    Remove-Item Env:HTTPS_PROXY -ErrorAction SilentlyContinue
    Remove-Item Env:HTTP_PROXY -ErrorAction SilentlyContinue
}

# 4. reuse GitHub token already stored in Git Credential Manager
$cred = "protocol=https`nhost=github.com`n`n" | git credential fill 2>$null
$token = (($cred | Select-String '^password=').ToString() -replace '^password=','')
if (-not $token) { throw 'failed to obtain GitHub token from Git Credential Manager' }

# 5. create + push annotated tag
if (git -C $repo tag -l $tag) {
    Write-Warning "tag $tag already exists locally, reusing"
} else {
    git -C $repo tag -a $tag -m "God of Things $Version"
}
if ($proxyUp) {
    git -C $repo push origin $tag
} else {
    git -C $repo -c http.proxy= -c https.proxy= push origin $tag
}
Write-Host "tag pushed: $tag"

# 6. create release (JSON body must go through a file to survive PowerShell quoting)
if (-not $Notes) { $Notes = "God of Things $Version release." }
$body = @{
    tag_name    = $tag
    name        = "God of Things $Version (1.21.1 NeoForge)"
    body        = $Notes
    draft       = $false
    prerelease  = $false
} | ConvertTo-Json -Compress
$tmp = Join-Path $env:TEMP 'gh_release_body.json'
[System.IO.File]::WriteAllText($tmp, $body, [System.Text.UTF8Encoding]::new($false))

$resp = curl.exe -sS -X POST `
    -H "Authorization: token $token" `
    -H "Accept: application/vnd.github+json" `
    -H "X-GitHub-Api-Version: 2022-11-28" `
    -H "Content-Type: application/json" `
    --data-binary "@$tmp" `
    "https://api.github.com/repos/$owner/$repoName/releases"
$rel = $resp | ConvertFrom-Json
if (-not $rel.id) { throw "release create failed: $resp" }

# 7. upload jar as a release asset
$uploadUrl = "https://uploads.github.com/repos/$owner/$repoName/releases/$($rel.id)/assets?name=godofthings-$Version.jar"
curl.exe -sS -X POST `
    -H "Authorization: token $token" `
    -H "Content-Type: application/octet-stream" `
    --data-binary "@$jar" `
    $uploadUrl | Out-Null

Write-Host ""
Write-Host "published: $($rel.html_url)"
Write-Host "download : https://github.com/$owner/$repoName/releases/download/$tag/godofthings-$Version.jar"
