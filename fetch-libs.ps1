# fetch-libs.ps1 -- restore the compile-only dependency jars into libs/.
#
# Why this exists: the 19 third-party mod jars needed for an offline build are no longer
# committed to the repository (repo size + we should not ship other people's jars inside
# the source tree). They live as assets of the "libs-1.21.1" release instead; run this
# script once after cloning and `gradlew build` works offline again.
#
# ASCII-only on purpose (PowerShell 5.1 misreads UTF-8-no-BOM scripts containing Chinese).
#
# Usage:
#   ./fetch-libs.ps1                     # restore into ./libs
#   ./fetch-libs.ps1 -Dest <dir>         # restore into another directory (used for a dry run)
#   ./fetch-libs.ps1 -IncludeTestMods    # also fetch guideme (AE2's runtime dep) so that
#                                        # `gradlew runGameTestServer` works; CI uses this

param(
    [string]$Dest = '',
    [switch]$IncludeTestMods
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrWhiteSpace($Dest)) {
    $Dest = Join-Path $root 'libs'
}
$libs = $Dest

$owner = 'ZeoNG129'
$repoName = 'Godofthings'
$tag = 'libs-1.21.1'
$baseUrl = "https://github.com/$owner/$repoName/releases/download/$tag"

# local name as build.gradle expects it  ->  asset name on the release.
# Only EMI differs: GitHub replaces '+' in an asset name with '.'.
# v5.12.1: trimmed to the 4 jars actually imported by the code (JEI x2 / EMI / AE2).
# The 15 former "wandering staff" integration jars (Draconic, Mekanism, occultism, ...)
# were removed together with the dead compileOnly entries in build.gradle.
$files = @(
    @{ local = 'ae2-1.21.1-neoforge.jar'; remote = '' },
    @{ local = 'emi-1.1.24+1.21.1+neoforge.jar'; remote = 'emi-1.1.24.1.21.1.neoforge.jar' },
    @{ local = 'jei-1.21.1-common-api-19.51.0.417.jar'; remote = '' },
    @{ local = 'jei-1.21.1-neoforge-api-19.51.0.417.jar'; remote = '' }
)

if ($IncludeTestMods) {
    # guideme is AE2's runtime dependency (not a compile dependency of this mod): it is only
    # needed when running the game tests. Keep comments in this file ASCII-only -- a non-ASCII
    # comment in a UTF-8-no-BOM .ps1 can be misread by PowerShell and swallow the next line.
    $files += @{ local = 'guideme-21.1.19.jar'; remote = '' }
}

# Optional local proxy (NBVPN listens on 7890 once connected; 7897 is the fallback).
# NOTE: this script also runs on Linux (GitHub Actions), so it must not use Windows-only
# cmdlets -- Test-NetConnection does not exist in pwsh on Linux, and 'curl.exe' is 'curl' there.
function Test-LocalPort([int]$Port) {
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $client.Connect('127.0.0.1', $Port)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

$curl = if ($env:OS -eq 'Windows_NT') { 'curl.exe' } else { 'curl' }

$proxyArgs = @()
foreach ($port in @(7890, 7897)) {
    if (Test-LocalPort $port) {
        $proxyArgs = @('-x', "http://127.0.0.1:$port")
        Write-Host "using local proxy 127.0.0.1:$port"
        break
    }
}

New-Item -ItemType Directory -Force -Path $libs | Out-Null

$downloaded = 0
$skipped = 0
foreach ($entry in $files) {
    $local = $entry.local
    $remote = $entry.remote
    if ([string]::IsNullOrWhiteSpace($remote)) {
        $remote = $local
    }
    $dest = Join-Path $libs $local
    if ((Test-Path -LiteralPath $dest) -and ((Get-Item -LiteralPath $dest).Length -gt 0)) {
        $skipped++
        continue
    }
    Write-Host "downloading $local"
    & $curl -sS -L @proxyArgs -o $dest "$baseUrl/$remote"
    if ((-not (Test-Path -LiteralPath $dest)) -or ((Get-Item -LiteralPath $dest).Length -eq 0)) {
        if (Test-Path -LiteralPath $dest) { Remove-Item -LiteralPath $dest -Force }
        throw "download failed (empty or missing): $local  <-  $remote"
    }
    $downloaded++
}

$jars = Get-ChildItem -LiteralPath $libs -Filter *.jar
$total = ($jars | Measure-Object -Property Length -Sum).Sum
Write-Host ("libs ready: {0} downloaded, {1} already present, {2} jar(s), {3:N1} MB -> {4}" -f `
        $downloaded, $skipped, $jars.Count, ($total / 1MB), $libs)
