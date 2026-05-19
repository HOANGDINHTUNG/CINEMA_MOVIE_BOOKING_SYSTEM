$dumpPath = Join-Path $PSScriptRoot '_dump_raw.sql'
$seedPath = Join-Path $PSScriptRoot 'seed.sql'

$tables = @(
    'roles', 'users', 'user_profiles', 'genres', 'movies', 'movie_genres',
    'rooms', 'seats', 'showtimes', 'showtime_seats', 'combos',
    'bookings', 'tickets', 'booking_combos', 'payments'
)

$lines = [System.IO.File]::ReadAllLines($dumpPath, [System.Text.Encoding]::UTF8)
$sb = New-Object System.Text.StringBuilder

[void]$sb.AppendLine('-- =============================================================================')
[void]$sb.AppendLine('-- Smart Cinema - DU LIEU EXPORT TU DATABASE smart_cinema_db')
[void]$sb.AppendLine('-- Tao tu mysqldump - dong bo voi DB hien tai')
[void]$sb.AppendLine('-- INSERT IGNORE: chay lai an toan, khong trung khoa')
[void]$sb.AppendLine('-- Mat khau demo (BCrypt 123456): $2a$12$Kry5fB4yN8Doz3xshxS7Y...')
[void]$sb.AppendLine('-- =============================================================================')

foreach ($table in $tables) {
    [void]$sb.AppendLine('')
    [void]$sb.AppendLine("-- ========== $($table.ToUpper()) ==========")

    $inSection = $false
    $count = 0
    foreach ($line in $lines) {
        if ($line -match "-- Dumping data for table ``$table``") {
            $inSection = $true
            continue
        }
        if ($inSection -and $line -match '^-- Dumping data for table ') {
            break
        }
        if ($inSection -and $line -match '^INSERT INTO ') {
            $fixed = $line -replace '^INSERT INTO ', 'INSERT IGNORE INTO '
            $fixed = $fixed -replace '`', ''
            [void]$sb.AppendLine($fixed)
            $count++
        }
    }
    [void]$sb.AppendLine("-- ($count rows)")
}

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($seedPath, $sb.ToString(), $utf8NoBom)
Write-Host "Wrote seed.sql ($((Get-Item $seedPath).Length) bytes)"
