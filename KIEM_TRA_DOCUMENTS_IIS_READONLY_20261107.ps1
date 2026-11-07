param(
    [string]$IisPhp = 'C:\inetpub\wwwroot\api_documents.php',
    [string]$PublicBase = 'https://tunertools.top'
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

function Flag([string]$Text, [string]$Pattern) {
    return [regex]::IsMatch($Text, $Pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
}
function Inspect-Php([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { return }
    $f = Get-Item -LiteralPath $Path
    $bytes = [System.IO.File]::ReadAllBytes($f.FullName)
    $raw = [System.IO.File]::ReadAllText($f.FullName)
    $firstHex = if ($bytes.Length -gt 0) {
        (($bytes | Select-Object -First ([Math]::Min(12, $bytes.Length)) | ForEach-Object { $_.ToString('X2') }) -join ' ')
    } else { '(empty)' }
    $hash = (Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash
    Write-Host ''
    Write-Host ('FILE    : ' + $f.FullName) -ForegroundColor Cyan
    Write-Host ('SIZE    : ' + $f.Length + ' bytes')
    Write-Host ('MODIFIED: ' + $f.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss'))
    Write-Host ('SHA256  : ' + $hash.Substring(0, 16) + '...')
    Write-Host ('HEADER  : ' + $firstHex)
    Write-Host ('PHP tag : ' + (Flag $raw '<\?(?:php|=)'))
    Write-Host ('auth_login text     : ' + (Flag $raw 'auth[_ -]?login|document[_ -]?login'))
    Write-Host ('permission_me text  : ' + (Flag $raw 'permission[_ -]?me|permissions[_ -]?me|document[_ -]?me'))
    Write-Host ('document session    : ' + (Flag $raw 'requireDocumentSession|session[_ -]?token|authorization'))
    Write-Host ('PHP action handling : ' + (Flag $raw '\$action|\brequestValue\s*\(|\$_(?:GET|POST|REQUEST)'))
    Write-Host ('list/view/download  : ' + (Flag $raw '(?:\blist\b|\bview\b|\bdownload\b)'))
    Write-Host ('include/require     : ' + (Flag $raw '\b(?:include|require)(?:_once)?\b'))
}
function Inspect-Http([string]$Url) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 12 -MaximumRedirection 0
        $status = [int]$response.StatusCode
        $ctype = [string]$response.Headers['Content-Type']
        Write-Host ('HTTP ' + $status + ' (' + $ctype + ') : ' + $Url)
        try {
            $j = $response.Content | ConvertFrom-Json
            foreach ($field in @('service', 'version', 'code', 'success', 'ok', 'mode', 'authorization')) {
                if ($null -ne $j.PSObject.Properties[$field]) {
                    $value = $j.$field
                    if ($value -is [string] -and $value.Length -gt 100) { $value = $value.Substring(0, 100) }
                    Write-Host ('  ' + $field + ' = ' + $value)
                }
            }
        } catch { Write-Host '  Response is not parseable as a JSON object.' }
    } catch {
        $status = 'network_error'
        if ($null -ne $_.Exception.Response) {
            try { $status = [int]$_.Exception.Response.StatusCode } catch { }
        }
        Write-Host ('HTTP ' + $status + ' : ' + $Url)
    }
}

Write-Host '=== TUNERTOOLS DOCUMENTS IIS DIAGNOSTICS - READ ONLY ===' -ForegroundColor Cyan
Write-Host 'No PHP edits; no credentials or session tokens printed.'

$paths = New-Object 'System.Collections.Generic.List[string]'
$paths.Add($IisPhp)
$paths.Add('C:\TunerTools\Web\api_documents.php')
$paths.Add('C:\TunerTools\Web\tunertools\api_documents.php')
$paths.Add('C:\inetpub\wwwroot\tunertools\api_documents.php')

try {
    Import-Module WebAdministration -ErrorAction Stop
    Write-Host ''
    Write-Host '=== IIS WEBSITE PHYSICAL ROOTS ===' -ForegroundColor Cyan
    foreach ($site in @(Get-Website)) {
        $siteRoot = [Environment]::ExpandEnvironmentVariables([string]$site.PhysicalPath)
        Write-Host ('SITE: ' + $site.Name + ' | ' + $site.State + ' | ' + $siteRoot)
        if (-not [string]::IsNullOrWhiteSpace($siteRoot)) {
            $paths.Add((Join-Path $siteRoot 'api_documents.php'))
        }
    }
    foreach ($app in @(Get-WebApplication -ErrorAction SilentlyContinue)) {
        $appRoot = [Environment]::ExpandEnvironmentVariables([string]$app.PhysicalPath)
        if (-not [string]::IsNullOrWhiteSpace($appRoot)) {
            $paths.Add((Join-Path $appRoot 'api_documents.php'))
        }
    }
} catch {
    Write-Host 'IIS WebAdministration unavailable; inspecting standard paths only.' -ForegroundColor Yellow
}

Write-Host ''
Write-Host '=== LOCAL PHP CANDIDATES ===' -ForegroundColor Cyan
$count = 0
foreach ($path in @($paths | Sort-Object -Unique)) {
    if (Test-Path -LiteralPath $path -PathType Leaf) {
        Inspect-Php $path
        $count++
    }
}
if ($count -eq 0) {
    Write-Host 'No api_documents.php file found in inspected paths.' -ForegroundColor Yellow
}

Write-Host ''
Write-Host '=== LIVE PUBLIC ROUTES (NO SESSION TOKEN) ===' -ForegroundColor Cyan
$base = $PublicBase.TrimEnd('/')
Inspect-Http ($base + '/api_documents.php?action=health')
Inspect-Http ($base + '/api_documents.php?action=permission_me')
Write-Host ''
Write-Host 'Done. Please share only this diagnostic output, not the PHP credentials.' -ForegroundColor Green
