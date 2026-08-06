$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repoRoot '.env'

if (Test-Path -LiteralPath $envFile) {
    foreach ($line in Get-Content -LiteralPath $envFile) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
        $separator = $trimmed.IndexOf('=')
        if ($separator -lt 1) { throw "Linha inválida em .env. Use NOME=valor." }
        $name = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1)
        if ($name -notmatch '^[A-Z_][A-Z0-9_]*$') { throw "Nome de variável inválido em .env: $name" }
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}

foreach ($required in 'SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD', 'JWT_SECRET') {
    if (-not [Environment]::GetEnvironmentVariable($required, 'Process')) {
        throw "$required não está definida. Copie .env.example para .env e preencha sem versionar o arquivo."
    }
}

$frontendDirectory = Join-Path $repoRoot 'frontend'
if (-not (Test-Path -LiteralPath (Join-Path $frontendDirectory 'node_modules'))) {
    & npm.cmd ci --no-audit --no-fund --prefix $frontendDirectory
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar as dependências do frontend.' }
}

$backend = Start-Process -FilePath (Join-Path $repoRoot 'backend\mvnw.cmd') -ArgumentList 'spring-boot:run' -WorkingDirectory (Join-Path $repoRoot 'backend') -NoNewWindow -PassThru
$frontend = Start-Process -FilePath 'npm.cmd' -ArgumentList 'run', 'dev' -WorkingDirectory $frontendDirectory -NoNewWindow -PassThru

Write-Host 'Frontend: http://localhost:5173'
Write-Host 'Backend:  http://localhost:8080'
Write-Host 'Pressione Ctrl+C para encerrar ambos.'
try {
    while (-not $backend.HasExited -and -not $frontend.HasExited) { Start-Sleep -Seconds 1 }
} finally {
    if (-not $backend.HasExited) { Stop-Process -Id $backend.Id }
    if (-not $frontend.HasExited) { Stop-Process -Id $frontend.Id }
}
