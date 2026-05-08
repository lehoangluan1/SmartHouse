param(
    [string]$EnvFile = ".env",
    [switch]$Persist
)

if (-not [System.IO.Path]::IsPathRooted($EnvFile)) {
    $backendEnvFile = Join-Path $PSScriptRoot $EnvFile
    $rootEnvFile = Join-Path (Split-Path $PSScriptRoot -Parent) $EnvFile
    if (Test-Path $backendEnvFile) {
        $EnvFile = $backendEnvFile
    }
    else {
        $EnvFile = $rootEnvFile
    }
}

if (-not (Test-Path $EnvFile)) {
    Write-Error "env Not Found: $EnvFile"
    exit 1
}

Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()

    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
        return
    }

    $parts = $line -split "=", 2
    if ($parts.Count -ne 2) {
        Write-Warning "Line Skipping: $line"
        return
    }

    $name = $parts[0].Trim()
    $value = $parts[1].Trim()

    if ($value.StartsWith('"') -and $value.EndsWith('"')) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    elseif ($value.StartsWith("'") -and $value.EndsWith("'")) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    Set-Item -Path "Env:$name" -Value $value

    Write-Host ("Loaded {0} = {1}" -f $name, $value)
    Write-Host ("Length of {0} = {1}" -f $name, $value.Length)

    if ($Persist) {
        [Environment]::SetEnvironmentVariable($name, $value, "User")
        Write-Host "Persisted $name to User environment"
    }

    Write-Host ""
}

Write-Host "Successfully loaded file: $EnvFile"
Write-Host "Done"
