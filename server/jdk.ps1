function Get-ProjectJdkTool([string]$ToolName) {
    if ($env:JAVA_HOME) {
        $taskCandidate = Join-Path $env:JAVA_HOME "bin\$ToolName.exe"
        if (Test-Path -LiteralPath $taskCandidate -PathType Leaf) { return $taskCandidate }
    }
    $taskCommand = Get-Command $ToolName -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($taskCommand) { return $taskCommand.Source }
    throw 'Install a JDK version 17 or newer and add its bin directory to PATH, or set JAVA_HOME.'
}
