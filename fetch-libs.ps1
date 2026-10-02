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
#   ./fetch-libs.ps1              # restore into ./libs
#   ./fetch-libs.ps1 -Dest <dir>  # restore into another directory (used for a dry run)

param(
    [string]$Dest = ''
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
$files = @(
    @{ local = 'ae2-1.21.1-neoforge.jar'; remote = '' },
    @{ local = 'Applied-Mekanistics-1.6.3.jar'; remote = '' },
    @{ local = 'AppliedFlux-1.21-2.1.5-neoforge.jar'; remote = '' },
    @{ local = 'BrandonsCore-1.21.1-3.2.1.309.jar'; remote = '' },
    @{ local = 'CodeChickenLib-1.21.1-4.6.1.526.jar'; remote = '' },
    @{ local = 'Draconic-Evolution-1.21.1-3.1.4.632.jar'; remote = '' },
    @{ local = 'emi-1.1.24+1.21.1+neoforge.jar'; remote = 'emi-1.1.24.1.21.1.neoforge.jar' },
    @{ local = 'enderio-8.2.12-beta.jar'; remote = '' },
    @{ local = 'FluxNetworks-1.21.1-8.0.0.jar'; remote = '' },
    @{ local = 'ftb-teams-neoforge-2101.1.10.jar'; remote = '' },
    @{ local = 'geckolib-neoforge-1.21.1-4.9.2.jar'; remote = '' },
    @{ local = 'jade-1.21.1-neoforge.jar'; remote = '' },
    @{ local = 'jei-1.21.1-common-api-19.51.0.417.jar'; remote = '' },
    @{ local = 'jei-1.21.1-neoforge-api-19.51.0.417.jar'; remote = '' },
    @{ local = 'Mekanism-1.21.1-10.7.19.85.jar'; remote = '' },
    @{ local = 'modonomicon-1.21.1-neoforge-1.120.3.jar'; remote = '' },
    @{ local = 'occultism-1.21.1-neoforge-1.224.4.jar'; remote = '' },
    @{ local = 'productivebees-1.21.1-13.13.5.jar'; remote = '' },
    @{ local = 'SmartBrainLib-neoforge-1.21.1-1.16.11.jar'; remote = '' }
)

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
