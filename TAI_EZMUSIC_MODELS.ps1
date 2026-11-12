#Requires -Version 5.1
<#
Download the publicly listed model files for inspection only.
Default: Orpheus (4 files). Optional: Echo (5 files) or Both (9 files).
Requires Windows PowerShell 5.1+ and a network connection.
#>
[CmdletBinding()]
param(
    [ValidateSet('Orpheus', 'Echo', 'Both')]
    [string]$Model = 'Orpheus',

    [string]$OutputRoot = (Join-Path $env:USERPROFILE 'Downloads\EZMusic_AI_Models'),

    [switch]$Force,
    [switch]$NoZip
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
$baseUrl = 'https://api.ez3d.fun/ezmusic'
$required = @{
    'orpheus-android-1' = @('layout.onnx', 'encoder.onnx', 'decoder.onnx', 'orpheus.json')
    'echo-android-1'    = @('attributes.onnx', 'backbone.onnx', 'echo.json', 'scorer.onnx', 'windows.f32')
}

switch ($Model) {
    'Orpheus' { $models = @('orpheus-android-1') }
    'Echo'    { $models = @('echo-android-1') }
    'Both'    { $models = @('orpheus-android-1', 'echo-android-1') }
}

New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null

function Get-RemoteHash($entry) {
    foreach ($key in @('sha256', 'sha_256')) {
        $property = $entry.PSObject.Properties[$key]
        if ($null -ne $property -and $null -ne $property.Value) {
            $value = [string]$property.Value
            $value = $value -replace '^sha256:', ''
            if ($value -match '^[0-9a-fA-F]{64}$') { return $value.ToLowerInvariant() }
        }
    }
    return ''
}

foreach ($modelId in $models) {
    Write-Host "`n=== $modelId ===" -ForegroundColor Cyan
    $manifestUrl = "$baseUrl/v1/models/$modelId/current"
    try {
        $manifest = Invoke-RestMethod -Uri $manifestUrl -Method Get -TimeoutSec 30 -Headers @{ Accept = 'application/json' }
    }
    catch {
        Write-Warning "Khong lay duoc manifest: $manifestUrl - $($_.Exception.Message)"
        continue
    }

    $version = [string]$manifest.version
    if ($version -notmatch '^[a-zA-Z0-9_.-]{1,80}$') {
        Write-Warning "Phien ban model khong hop le: $version"
        continue
    }
    $files = @($manifest.files)
    $selected = @()
    foreach ($expectedName in $required[$modelId]) {
        $match = @($files | Where-Object { [string]$_.path -ceq $expectedName })
        if ($match.Count -ne 1) {
            Write-Warning "Manifest khong co chinh xac 1 file: $expectedName (tim thay $($match.Count))"
        }
        else {
            $selected += $match[0]
        }
    }
    if ($selected.Count -ne $required[$modelId].Count) {
        Write-Warning "Thieu file trong manifest. Bo qua model $modelId de tranh goi tai khong day du."
        continue
    }

    $versionDir = Join-Path (Join-Path $OutputRoot $modelId) $version
    New-Item -ItemType Directory -Path $versionDir -Force | Out-Null
    $manifestPath = Join-Path $versionDir 'manifest.json'
    $manifest | ConvertTo-Json -Depth 30 | Set-Content -Path $manifestPath -Encoding UTF8

    $results = @()
    $allValid = $true
    foreach ($entry in $selected) {
        $name = [string]$entry.path
        if ($name -notmatch '^[a-zA-Z0-9_.-]+$') { throw "Ten tep khong an toan: $name" }
        $expectedBytes = [long]$entry.bytes
        $remoteHash = Get-RemoteHash $entry
        $url = "$baseUrl/v1/models/$modelId/$version/$name"
        $destination = Join-Path $versionDir $name
        $partFile = "$destination.part"
        $canReuse = $false

        if ((Test-Path -LiteralPath $destination) -and -not $Force) {
            $existing = Get-Item -LiteralPath $destination
            if (($expectedBytes -le 0) -or ($existing.Length -eq $expectedBytes)) {
                if (-not $remoteHash) {
                    $canReuse = $true
                }
                elseif ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant() -eq $remoteHash) {
                    $canReuse = $true
                }
            }
        }

        if ($canReuse) {
            Write-Host "DA CO: $name" -ForegroundColor DarkGreen
        }
        else {
            $downloaded = $false
            for ($attempt = 1; $attempt -le 3; $attempt++) {
                try {
                    if (Test-Path -LiteralPath $partFile) { Remove-Item -LiteralPath $partFile -Force }
                    Write-Host "TAI ($attempt/3): $name" -ForegroundColor Yellow
                    Invoke-WebRequest -Uri $url -Method Get -OutFile $partFile -UseBasicParsing -TimeoutSec 300
                    $length = (Get-Item -LiteralPath $partFile).Length
                    if ($expectedBytes -gt 0 -and $length -ne $expectedBytes) {
                        throw "Dung luong sai: thuc te $length bytes / manifest $expectedBytes bytes"
                    }
                    if ($remoteHash) {
                        $partHash = (Get-FileHash -LiteralPath $partFile -Algorithm SHA256).Hash.ToLowerInvariant()
                        if ($partHash -ne $remoteHash) { throw "SHA-256 khong khop" }
                    }
                    Move-Item -LiteralPath $partFile -Destination $destination -Force
                    $downloaded = $true
                    break
                }
                catch {
                    Write-Warning "Loi $name ($attempt/3): $($_.Exception.Message)"
                    if (Test-Path -LiteralPath $partFile) { Remove-Item -LiteralPath $partFile -Force }
                    if ($attempt -lt 3) { Start-Sleep -Seconds (2 * $attempt) }
                }
            }
            if (-not $downloaded) { $allValid = $false; continue }
        }

        $actualBytes = (Get-Item -LiteralPath $destination).Length
        $hash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
        $lengthOk = ($expectedBytes -le 0 -or $actualBytes -eq $expectedBytes)
        $hashOk = (-not $remoteHash -or $hash -eq $remoteHash)
        $valid = ($lengthOk -and $hashOk)
        if (-not $valid) { $allValid = $false }
        $status = if (-not $valid) { 'FAILED' } elseif ($remoteHash) { 'SHA256_VERIFIED' } else { 'SIZE_OK_HASH_LOCAL_ONLY' }
        $results += [PSCustomObject]@{
            Model = $modelId
            Version = $version
            File = $name
            Bytes = $actualBytes
            SHA256 = $hash
            SHA256_From_Server = $remoteHash
            Status = $status
            URL = $url
            LocalPath = $destination
        }
        Write-Host ("{0} : {1:N2} MiB | {2}" -f $name, ($actualBytes / 1MB), $status) -ForegroundColor Green
    }

    $csvPath = Join-Path $versionDir 'download_report.csv'
    if ($results.Count -gt 0) {
        $results | Export-Csv -LiteralPath $csvPath -NoTypeInformation -Encoding UTF8
        $results | Format-Table File, @{Label='MiB'; Expression={[math]::Round($_.Bytes / 1MB, 2)}}, Status -AutoSize
    }
    if ($results.Count -ne $required[$modelId].Count -or -not $allValid) {
        Write-Warning "Model $modelId CHUA tai day du/hop le. Vui long xem loi phia tren."
        continue
    }
    Write-Host "DA TAI DU: $($results.Count) file - $versionDir" -ForegroundColor Green

    if (-not $NoZip) {
        $zipPath = Join-Path $OutputRoot "$modelId-$version.zip"
        Compress-Archive -Path (Join-Path $versionDir '*') -DestinationPath $zipPath -Force -CompressionLevel Optimal
        Write-Host "ZIP DE GUI PHAN TICH: $zipPath" -ForegroundColor Cyan
    }
}
Write-Host "`nXong. Thu muc goc: $OutputRoot" -ForegroundColor Cyan
