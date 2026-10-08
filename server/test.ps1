[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'build.ps1')
. (Join-Path $PSScriptRoot 'jdk.ps1')
$taskJavac = Get-ProjectJdkTool 'javac'
$taskJava = Get-ProjectJdkTool 'java'
$taskBuild = Join-Path $PSScriptRoot 'build'
$taskClasspath = $taskBuild + ';' + (Join-Path $PSScriptRoot 'lib\gson-2.14.0.jar')
$taskTests = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'tests') -Filter '*.java' | ForEach-Object FullName)
& $taskJavac --release 17 -Xlint:all,-classfile -cp $taskClasspath -d $taskBuild @taskTests
if ($LASTEXITCODE -ne 0) { throw "Test compilation failed (exit $LASTEXITCODE)." }
foreach ($taskSuite in @('GameRulesTest', 'HttpApiTest')) {
    & $taskJava -cp $taskClasspath $taskSuite
    if ($LASTEXITCODE -ne 0) { throw "$taskSuite failed (exit $LASTEXITCODE)." }
}
Write-Host 'All game and HTTP checks passed.'
