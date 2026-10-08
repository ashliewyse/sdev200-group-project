[CmdletBinding()]
param([string]$BindAddress = '127.0.0.1', [ValidateRange(0,65535)][int]$Port = 8080)
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'build.ps1')
. (Join-Path $PSScriptRoot 'jdk.ps1')
$taskJava = Get-ProjectJdkTool 'java'
$taskClasspath = (Join-Path $PSScriptRoot 'build') + ';' + (Join-Path $PSScriptRoot 'lib\gson-2.14.0.jar')
& $taskJava -cp $taskClasspath GameServer --host $BindAddress --port $Port
if ($LASTEXITCODE -ne 0) { throw "Server exited with code $LASTEXITCODE." }
