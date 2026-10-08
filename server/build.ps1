[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'jdk.ps1')
$taskJavac = Get-ProjectJdkTool 'javac'
$taskJar = Join-Path $PSScriptRoot 'lib\gson-2.14.0.jar'
$taskBuild = Join-Path $PSScriptRoot 'build'
$taskExpected = (Get-Content -LiteralPath "$taskJar.sha256" -Raw).Trim()
if ((Get-FileHash -LiteralPath $taskJar -Algorithm SHA256).Hash -ine $taskExpected) { throw 'Gson jar checksum mismatch.' }
New-Item -ItemType Directory -Force -Path $taskBuild | Out-Null
$taskSources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Filter '*.java' | ForEach-Object FullName)
& $taskJavac --release 17 -Xlint:all,-classfile -cp $taskJar -d $taskBuild @taskSources
if ($LASTEXITCODE -ne 0) { throw "Java compilation failed (exit $LASTEXITCODE)." }
Write-Host 'Server compiled successfully.'
