# Avvia un server dedicato locale con la UI Noesis lato server abilitata (preview) e la mod installata.
#   .\start-testserver.ps1                         -> release
#   .\start-testserver.ps1 -Patchline pre-release  -> pre-release (il client deve essere sulla stessa patchline)
# Collegati da Hytale a: localhost:5520
# Primo avvio: nella console del server esegui  auth login device  e segui il link per autenticarlo.
param(
    [ValidateSet('release', 'pre-release')]
    [string]$Patchline = 'release'
)
$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$pkg = "$env:APPDATA\Hytale\install\$Patchline\package"
$server = Join-Path $root 'testserver'

New-Item -ItemType Directory -Force (Join-Path $server 'mods') | Out-Null
Get-ChildItem (Join-Path $server 'mods') -Filter 'AutoFurnace-*.jar' | Remove-Item -Force
$jar = Get-ChildItem (Join-Path $root 'build') -Filter 'AutoFurnace-*.jar' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($jar) { Copy-Item $jar.FullName (Join-Path $server 'mods') }

Write-Host "Avvio server $Patchline"
Set-Location $server
& "$pkg\jre\latest\bin\java.exe" "-Dhytale.serverside_ui_preview=true" -jar "$pkg\game\latest\Server\HytaleServer.jar" --assets "$pkg\game\latest\Assets.zip"
