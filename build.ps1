# Compila AutoFurnace contro l'HytaleServer.jar installato, usando il JDK portable in .tools\jdk-25.
#   .\build.ps1            -> crea build\AutoFurnace-<versione>.jar
#   .\build.ps1 -Install   -> e lo copia anche in %APPDATA%\Hytale\UserData\Mods
#   .\build.ps1 -Patchline pre-release  -> compila contro la pre-release
param(
    [switch]$Install,
    [ValidateSet('release', 'pre-release')]
    [string]$Patchline = 'release',
    [string]$ServerJar = "$env:APPDATA\Hytale\install\$Patchline\package\game\latest\Server\HytaleServer.jar"
)
$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$jdk = Join-Path $root '.tools\jdk-25\bin'
$build = Join-Path $root 'build'
$classes = Join-Path $build 'classes'

if (-not (Test-Path "$jdk\javac.exe")) { throw "JDK non trovato in $jdk" }
if (-not (Test-Path $ServerJar)) { throw "HytaleServer.jar non trovato: $ServerJar (usa -ServerJar <path>)" }

$version = (Get-Content (Join-Path $root 'src\main\resources\manifest.json') -Raw | ConvertFrom-Json).Version
$jar = Join-Path $build "AutoFurnace-$version.jar"

if (Test-Path $build) { Remove-Item -Recurse -Force $build }
New-Item -ItemType Directory -Force $classes | Out-Null

$sources = Get-ChildItem (Join-Path $root 'src\main\java') -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& "$jdk\javac.exe" --release 25 -encoding UTF-8 -proc:none -cp $ServerJar -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw "Compilazione fallita" }

Copy-Item -Recurse (Join-Path $root 'src\main\resources\*') $classes
& "$jdk\jar.exe" --create --file $jar -C $classes .
if ($LASTEXITCODE -ne 0) { throw "Creazione jar fallita" }
Write-Host "Creato $jar"

if ($Install) {
    $mods = "$env:APPDATA\Hytale\UserData\Mods"
    New-Item -ItemType Directory -Force $mods | Out-Null
    Get-ChildItem $mods -Filter 'AutoFurnace-*.jar' | Remove-Item -Force
    Copy-Item $jar $mods
    Write-Host "Installato in $mods"
}
