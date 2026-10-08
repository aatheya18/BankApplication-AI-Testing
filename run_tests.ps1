$ErrorActionPreference = "Stop"

# Download JUnit Platform Console Standalone
$junitUrl = "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar"
$junitJar = "junit-platform-console-standalone-1.10.2.jar"
if (-not (Test-Path $junitJar)) {
    Write-Host "Downloading JUnit..."
    Invoke-WebRequest -Uri $junitUrl -OutFile $junitJar
}

# Download JaCoCo CLI and Agent
$jacocoVersion = "0.8.12"
$jacocoZip = "jacoco-$jacocoVersion.zip"
$jacocoDir = "jacoco"
if (-not (Test-Path $jacocoDir)) {
    Write-Host "Downloading JaCoCo..."
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/jacoco/jacoco/$jacocoVersion/jacoco-$jacocoVersion.zip" -OutFile $jacocoZip
    Expand-Archive -Path $jacocoZip -DestinationPath $jacocoDir
}

# Compile source files
Write-Host "Compiling source files..."
if (Test-Path "out\classes") { Remove-Item -Recurse -Force "out\classes" }
New-Item -ItemType Directory -Force -Path "out\classes" | Out-Null
javac -d out\classes Bank.java BankAccount.java

# Compile test files
Write-Host "Compiling test files..."
if (Test-Path "out\test-classes") { Remove-Item -Recurse -Force "out\test-classes" }
New-Item -ItemType Directory -Force -Path "out\test-classes" | Out-Null
javac -cp "out\classes;$junitJar" -d out\test-classes test\BankAccountTest.java test\BankTest.java

# Run tests with JaCoCo agent
Write-Host "Running tests with JaCoCo..."
$jacocoAgent = "$jacocoDir\lib\jacocoagent.jar"
$javaArgs = "-javaagent:$jacocoAgent=destfile=jacoco.exec,append=false", "-cp", "out\classes;out\test-classes;$junitJar", "org.junit.platform.console.ConsoleLauncher", "--scan-classpath"
& java $javaArgs

# Generate report
Write-Host "Generating JaCoCo report..."
$jacocoCli = "$jacocoDir\lib\jacococli.jar"
& java -jar $jacocoCli report jacoco.exec --classfiles out\classes --sourcefiles . --html report_jacoco --csv report_jacoco.csv

Write-Host "Done! Report generated in report_jacoco directory."
