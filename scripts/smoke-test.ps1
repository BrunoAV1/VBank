$ErrorActionPreference = 'Stop'
$baseUrl = if ($args.Count -gt 0) { $args[0] } else { 'http://localhost:8080' }
node (Join-Path $PSScriptRoot 'smoke-test.mjs') $baseUrl
exit $LASTEXITCODE

