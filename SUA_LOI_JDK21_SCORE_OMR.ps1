#Requires -Version 5.1
# Corrected setup of Microsoft OpenJDK 21 for TunerTools Score-OMR.
# Uses the JDK ZIP already downloaded; verifies it against Microsoft's official checksum.
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$app = 'C:\TunerTools\Tools\ScoreOMR_Web_Test'
$zip = Join-Path $app 'microsoft-jdk-21.zip'
$shaPath = Join-Path $app 'microsoft-jdk-21.zip.sha256sum.txt'
$jdkDir = Join-Path $app 'JDK21'
$baseUrl = 'https://aka.ms/download-jdk/microsoft-jdk-21.0.13.0.2-windows-x64.zip'

if (-not (Test-Path -LiteralPath $app -PathType Container)) {
    throw "Khong tim thay thu muc: $app"
}
if (-not (Test-Path -LiteralPath $zip -PathType Leaf)) {
    Write-Host 'Khong tim thay ZIP da tai. Dang tai lai tu Microsoft...' -ForegroundColor Cyan
    Invoke-WebRequest -Uri $baseUrl -OutFile $zip -UseBasicParsing
}

Write-Host '[1/4] Dang tai file SHA-256 nho tu Microsoft...' -ForegroundColor Cyan
Invoke-WebRequest -Uri ($baseUrl + '.sha256sum.txt') -OutFile $shaPath -UseBasicParsing
$raw = [System.IO.File]::ReadAllBytes($shaPath)
$pattern = '(?i)(?<![a-f0-9])[a-f0-9]{64}(?![a-f0-9])'
$expected = ''
foreach ($encoding in @([Text.Encoding]::UTF8, [Text.Encoding]::Unicode, [Text.Encoding]::BigEndianUnicode, [Text.Encoding]::ASCII)) {
    $candidate = $encoding.GetString($raw)
    $match = [regex]::Match($candidate, $pattern)
    if ($match.Success) { $expected = $match.Value.ToUpperInvariant(); break }
}
if (-not $expected) {
    Write-Host "File checksum da luu: $shaPath" -ForegroundColor Yellow
    Write-Host 'Microsoft did not return a recognizable SHA-256 value; do not skip verification.' -ForegroundColor Yellow
    throw 'Khong doc duoc checksum. Kiem tra ket noi Microsoft hoac gui lai file .sha256sum.txt.'
}

Write-Host '[2/4] Kiem tra SHA-256 cua ZIP...' -ForegroundColor Cyan
$actual = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToUpperInvariant()
if ($actual -ne $expected) {
    throw "ZIP khong khop SHA-256! File: $zip - Hay xoa ZIP da tai va chay lai."
}
Write-Host '[OK] SHA-256 khop checksum chinh thuc.' -ForegroundColor Green

Write-Host '[3/4] Kiem tra / giai nen JDK 21...' -ForegroundColor Cyan
New-Item -ItemType Directory -Path $jdkDir -Force | Out-Null
$javac = Get-ChildItem -LiteralPath $jdkDir -Filter 'javac.exe' -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $javac) {
    Expand-Archive -LiteralPath $zip -DestinationPath $jdkDir -Force -ErrorAction Stop
    $javac = Get-ChildItem -LiteralPath $jdkDir -Filter 'javac.exe' -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
}
if (-not $javac) { throw 'Khong tim thay javac.exe trong JDK21 sau giai nen.' }
$env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javac.FullName)
$env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path $java)) { throw 'Khong tim thay java.exe trong JDK21.' }
Write-Host "JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Green
# Java often writes version information to stderr; avoid PowerShell 5.1 NativeCommandError.
$oldPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    & $java -version
    $javaExit = $LASTEXITCODE
    & $javac.FullName -version
    $javacExit = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $oldPreference
}
if ($javaExit -ne 0) { throw 'java -version that bai.' }
if ($javacExit -ne 0) { throw 'javac -version that bai.' }

Write-Host '[4/4] Khoi dong Score-OMR...' -ForegroundColor Cyan
$run = Join-Path $app 'CHAY_SCORE_OMR_WEB.ps1'
if (-not (Test-Path -LiteralPath $run)) { throw "Khong tim thay: $run" }
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $run
if ($LASTEXITCODE -ne 0) { throw 'Score-OMR dung do loi. Xem thong bao phia tren.' }
