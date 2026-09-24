$env:JAVA_HOME = "C:\Program Files\Android\openjdk\jdk-21.0.8"
$androidSdkRoot = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk"

New-Item -ItemType Directory -Force -Path "$androidSdkRoot\cmdline-tools" | Out-Null
$zipPath = "$env:TEMP\android-cmdline-tools.zip"

# Try multiple known URLs
$urls = @(
    "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip",
    "https://dl.google.com/android/repository/commandlinetools-win-10406996_latest.zip",
    "https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip",
    "https://dl.google.com/android/repository/commandlinetools-win-8512546_latest.zip",
    "https://dl.google.com/android/repository/commandlinetools-win-7302018_latest.zip"
)

$downloaded = $false
foreach ($url in $urls) {
    Write-Host "Trying: $url"
    try {
        $wc = New-Object System.Net.WebClient
        $wc.Headers.Add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        $wc.DownloadFile($url, $zipPath)
        Write-Host "Downloaded successfully!"
        $downloaded = $true
        break
    } catch {
        Write-Host "Failed: $($_.Exception.Message)"
    }
}

if (-not $downloaded) {
    Write-Host "All URLs failed. Trying alternative approach..."
    # Try using the XML repository file to find the right URL
    $repoUrl = "https://dl.google.com/android/repository/repository2-3.xml"
    try {
        $wc = New-Object System.Net.WebClient
        $wc.Headers.Add("User-Agent", "Mozilla/5.0")
        $xml = $wc.DownloadString($repoUrl)
        Write-Host "Got repository XML"
        # Extract commandlinetools URLs
        $matches = [regex]::Matches($xml, 'commandlinetools-win-(\d+)_latest\.zip')
        foreach ($m in $matches) {
            $ver = $m.Groups[1].Value
            $testUrl = "https://dl.google.com/android/repository/commandlinetools-win-${ver}_latest.zip"
            Write-Host "Trying extracted URL: $testUrl"
            try {
                $wc.DownloadFile($testUrl, $zipPath)
                Write-Host "Downloaded successfully!"
                $downloaded = $true
                break
            } catch {
                Write-Host "Failed"
            }
        }
    } catch {
        Write-Host "Repository fetch failed: $_"
    }
}

if ($downloaded -and (Test-Path $zipPath)) {
    Write-Host "Extracting..."
    Expand-Archive -Path $zipPath -DestinationPath "$androidSdkRoot\cmdline-tools" -Force
    
    if (Test-Path "$androidSdkRoot\cmdline-tools\cmdline-tools") {
        if (Test-Path "$androidSdkRoot\cmdline-tools\latest") {
            Remove-Item "$androidSdkRoot\cmdline-tools\latest" -Recurse -Force
        }
        Rename-Item "$androidSdkRoot\cmdline-tools\cmdline-tools" "$androidSdkRoot\cmdline-tools\latest"
    }
    
    Write-Host "Done! Testing sdkmanager..."
    & "$androidSdkRoot\cmdline-tools\latest\bin\sdkmanager.bat" --version
} else {
    Write-Host "FAILED: Could not download Android SDK command line tools"
    Write-Host "Please download manually from: https://developer.android.com/studio#command-line-tools-only"
}
