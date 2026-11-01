# Za Snake production preflight for Windows PowerShell (no Python required).
# Run from the Android Studio project root: powershell -ExecutionPolicy Bypass -File tools/production_preflight.ps1
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
  $failed = 0
  function Check([string]$name, [bool]$ok) {
      if ($ok) { Write-Host "[PASS] $name" -ForegroundColor Green }
      else { Write-Host "[BLOCKED] $name" -ForegroundColor Red; $script:failed += 1 }
  }
  $jsonPath = Join-Path $root "app/google-services.json"
  Check "Firebase JSON exists" (Test-Path $jsonPath)
  if (Test-Path $jsonPath) {
    $j = Get-Content -Raw $jsonPath | ConvertFrom-Json
    $packages = @($j.client | ForEach-Object { $_.client_info.android_client_info.package_name })
    Check "Firebase package matches" ($packages -contains "com.zayedart.zasnake")
    Check "Firebase project matches" ($j.project_info.project_id -eq "za-snake")
  }
  $rootGradle = Get-Content -Raw "build.gradle.kts"
  $appGradle = Get-Content -Raw "app/build.gradle.kts"
  $strings = Get-Content -Raw "app/src/main/res/values/strings.xml"
  $adPath = "app/src/release/res/values/admob_ids.xml"
  $ad = if (Test-Path $adPath) { Get-Content -Raw $adPath } else { "" }
  $uiPath = "app/src/main/java/com/ahmedzayed/zasnake/ui/ZaSnakeApp.kt"
  $ui = if (Test-Path $uiPath) { Get-Content -Raw $uiPath } else { "" }
  Check "Google Services plugin installed" (($rootGradle -match 'com.google.gms.google-services') -and ($appGradle -match 'id\("com.google.gms.google-services"\)'))
  Check "Android applicationId" ($appGradle -match 'applicationId\s*=\s*"com.zayedart.zasnake"')
  Check "Version 1.0.0" ($appGradle -match 'versionName\s*=\s*"1.0.0"')
  Check "PGS numeric project ID" ($strings -match '>914975720790<')
  Check "11 achievement IDs replaced" ($strings -notmatch 'REPLACE_WITH_PLAY_CONSOLE_ACHIEVEMENT_ID')
  Check "Release AdMob application ID" ($ad -match 'ca-app-pub-6495262421269253~4884698925')
  Check "Revive placement ID" ($ad -match 'ca-app-pub-6495262421269253/5814637214')
  Check "Double coins placement ID" ($ad -match 'ca-app-pub-6495262421269253/4581446596')
  Check "Real-money purchase flow enabled & implemented" (($ui -notmatch 'onClick\s*=\s*\{\s*\}\s*,\s*enabled\s*=\s*false') -and ($ui -match 'launchBillingFlow'))
  Write-Host "`nBlocking static checks: $failed" -ForegroundColor Yellow
  Write-Host 'Manual: Sync and build in Android Studio; test PGS OAuth/Cloud Save, UMP, ads, Billing server and Signed AAB.'
  if ($failed -gt 0) { exit 1 }
  exit 0
} finally { Pop-Location }
