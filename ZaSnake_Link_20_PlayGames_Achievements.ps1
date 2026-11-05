# Za Snake: attach all 20 Google Play Games achievement IDs to the existing Android client.
# Run from project root: .\ZaSnake_Link_20_PlayGames_Achievements.ps1
# No versionCode changes, uploads, or console actions are performed.
# Creates a timestamped backup of both touched files before changing them.

$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$resourcesPath = Join-Path $root 'app\src\main\res\values\strings.xml'
$bridgePath = Join-Path $root 'app\src\main\java\com\ahmedzayed\zasnake\platform\PlayGamesBridge.kt'
foreach ($p in @($resourcesPath, $bridgePath)) {
    if (-not (Test-Path -LiteralPath $p -PathType Leaf)) {
        throw "Missing required project file: $p. Run this script from the ZaSnakeAndroid project root."
    }
}

$achievementIds = [ordered]@{
    'first'       = 'CgkI1oq2xtAaEAIQBQ'
    'apples50'    = 'CgkI1oq2xtAaEAIQBg'
    'apples250'   = 'CgkI1oq2xtAaEAIQCg'
    'score10'     = 'CgkI1oq2xtAaEAIQAg'
    'score50'     = 'CgkI1oq2xtAaEAIQEw'
    'golden'      = 'CgkI1oq2xtAaEAIQDg'
    'barber'      = 'CgkI1oq2xtAaEAIQEQ'
    'long30'      = 'CgkI1oq2xtAaEAIQAw'
    'games10'     = 'CgkI1oq2xtAaEAIQBw'
    'clock15'     = 'CgkI1oq2xtAaEAIQEg'
    'dedicated'   = 'CgkI1oq2xtAaEAIQCw'
    'apples500'   = 'CgkI1oq2xtAaEAIQCA'
    'apples1000'  = 'CgkI1oq2xtAaEAIQFQ'
    'games50'     = 'CgkI1oq2xtAaEAIQDA'
    'games100'    = 'CgkI1oq2xtAaEAIQFA'
    'golden10'    = 'CgkI1oq2xtAaEAIQBA'
    'golden50'    = 'CgkI1oq2xtAaEAIQEA'
    'scissors25'  = 'CgkI1oq2xtAaEAIQDQ'
    'play60min'   = 'CgkI1oq2xtAaEAIQCQ'
    'score100'    = 'CgkI1oq2xtAaEAIQDw'
}
if (($achievementIds.Values | Select-Object -Unique).Count -ne 20) { throw 'Achievement IDs are not unique.' }

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$originalXml = [System.IO.File]::ReadAllText($resourcesPath, [System.Text.Encoding]::UTF8)
$originalKt = [System.IO.File]::ReadAllText($bridgePath, [System.Text.Encoding]::UTF8)
$xml = $originalXml
$kt = $originalKt

if (([regex]::Matches($xml, '</resources>')).Count -ne 1) { throw 'Unexpected strings.xml structure.' }
foreach ($key in $achievementIds.Keys) {
    $resourceName = 'pgs_achievement_' + $key
    $id = $achievementIds[$key]
    $pattern = '(<string\s+name="' + [regex]::Escape($resourceName) + '"\s+translatable="false">)([^<]*)(</string>)'
    $hitList = [regex]::Matches($xml, $pattern)
    if ($hitList.Count -gt 1) { throw "Duplicate resource name: $resourceName" }
    if ($hitList.Count -eq 1) {
        $xml = [regex]::Replace($xml, $pattern, ('$1' + $id + '$3'))
    } else {
        $entry = '    <string name="' + $resourceName + '" translatable="false">' + $id + '</string>' + "`r`n"
        $xml = [regex]::Replace($xml, '</resources>', ($entry + '</resources>'))
    }
}

# Only inject missing cases, preserving the existing 'when' statement and local age policy.
$missing = @()
foreach ($key in $achievementIds.Keys) {
    $case = '"' + $key + '" -> R.string.pgs_achievement_' + $key
    if (-not $kt.Contains($case)) { $missing += $key }
}
$anchor = '            "dedicated" -> R.string.pgs_achievement_dedicated'
if ($missing.Count -gt 0) {
    if (-not $kt.Contains($anchor) -or ([regex]::Matches($kt, [regex]::Escape($anchor))).Count -ne 1) {
        throw 'Could not safely locate the achievement when() block in PlayGamesBridge.kt.'
    }
    foreach ($key in $missing) {
        if ($key -in @('first','apples50','apples250','score10','score50','golden','barber','long30','games10','clock15','dedicated')) {
            throw "Original PlayGamesBridge mapping is missing: $key. Manual review needed."
        }
    }
    $addedCases = ($missing | ForEach-Object { '            "' + $_ + '" -> R.string.pgs_achievement_' + $_ }) -join "`r`n"
    $kt = $kt.Replace($anchor, $anchor + "`r`n" + $addedCases)
}

# Validate all IDs and mappings before writing either file.
foreach ($key in $achievementIds.Keys) {
    $name = 'pgs_achievement_' + $key
    $id = $achievementIds[$key]
    if (-not $xml.Contains('name="' + $name + '" translatable="false">' + $id + '</string>')) {
        throw "Final XML verification failed for $key."
    }
    if (-not $kt.Contains('"' + $key + '" -> R.string.' + $name)) {
        throw "Final Kotlin verification failed for $key."
    }
}

if ($xml -eq $originalXml -and $kt -eq $originalKt) {
    Write-Host 'All 20 achievements are already linked. No changes needed.' -ForegroundColor Green
    exit 0
}

$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$xmlBackup = "$resourcesPath.$stamp.bak"
$ktBackup = "$bridgePath.$stamp.bak"
Copy-Item -LiteralPath $resourcesPath -Destination $xmlBackup -ErrorAction Stop
Copy-Item -LiteralPath $bridgePath -Destination $ktBackup -ErrorAction Stop
try {
    [System.IO.File]::WriteAllText($resourcesPath, $xml, $utf8NoBom)
    [System.IO.File]::WriteAllText($bridgePath, $kt, $utf8NoBom)
} catch {
    Copy-Item -LiteralPath $xmlBackup -Destination $resourcesPath -Force
    Copy-Item -LiteralPath $ktBackup -Destination $bridgePath -Force
    throw
}
Write-Host 'SUCCESS: All 20 achievement IDs linked in strings.xml and PlayGamesBridge.kt.' -ForegroundColor Green
Write-Host 'Backups:'
Write-Host "  $xmlBackup"
Write-Host "  $ktBackup"
Write-Host 'Next: .\gradlew.bat :app:compileReleaseKotlin :app:testDebugUnitTest'
