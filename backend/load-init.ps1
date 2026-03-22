$pgDump = (Get-ChildItem "C:\Program Files\PostgreSQL" -Recurse -Filter pg_dump.exe -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName)
$outputFile = Join-Path $PWD "init.dump.sql"
$containerName = "smarthouse-db"

Write-Host "Using pg_dump: $pgDump"
Write-Host "Current directory: $PWD"
Write-Host "Target dump file: $outputFile"
Write-Host "Docker container: $containerName"

$env:PGPASSWORD = "123456"
& $pgDump -h localhost -p 5432 -U postgres -d smarthouse --inserts --clean --if-exists -f $outputFile
Remove-Item Env:PGPASSWORD

if (Test-Path $outputFile) {
    Write-Host "Dump created successfully at: $((Resolve-Path $outputFile).Path)"
} else {
    Write-Host "Dump failed: file not found."
}

Write-Host "`n=== Current database ==="
docker exec -i $containerName psql -U postgres -d smarthouse -c "SELECT current_database();"

Write-Host "`n=== Existing schemas ==="
docker exec -i $containerName psql -U postgres -d smarthouse -c "SELECT schema_name FROM information_schema.schemata ORDER BY schema_name;"

Write-Host "`n=== Existing tables ==="
docker exec -i $containerName psql -U postgres -d smarthouse -c "SELECT schemaname, tablename FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema') ORDER BY schemaname, tablename;"

Write-Host "`n=== Search path ==="
docker exec -i $containerName psql -U postgres -d smarthouse -c "SHOW search_path;"