param(
    [switch]$SkipBuild,
    [switch]$Clean,
    [switch]$EnableNativeAccess
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$jar = "lib\sqlite-jdbc-3.53.0.0.jar"
$outDir = "out"

if (-not (Test-Path $jar)) {
    Write-Error "Missing JDBC jar: $jar"
}

if ($Clean -and (Test-Path $outDir)) {
    Remove-Item -Recurse -Force $outDir
}

if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory $outDir | Out-Null
}

if (-not $SkipBuild) {
    Write-Host "Compiling sources..."

    $compileArgs = @(
        "-cp", $jar,
        "-d", $outDir,
        "-sourcepath", "src",
        "src\Main.java",
        "src\model\*.java",
        "src\dao\*.java",
        "src\controller\*.java",
        "src\util\*.java",
        "src\view\*.java"
    )

    & javac @compileArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Compilation failed with exit code $LASTEXITCODE"
    }
}

Write-Host "Starting application..."

$javaArgs = @("-cp", "out;lib\sqlite-jdbc-3.53.0.0.jar")
if ($EnableNativeAccess) {
    $javaArgs += "--enable-native-access=ALL-UNNAMED"
}
$javaArgs += "Main"

& java @javaArgs
exit $LASTEXITCODE
