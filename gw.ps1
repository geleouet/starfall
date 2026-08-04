# Starfall build wrapper -- PowerShell.
#
# The exact counterpart of ./gw, which is bash and therefore unusable from a
# PowerShell prompt. Both pin the same two things and must stay in step:
#
#   1. JDK 21. The machine's JAVA_HOME points at JDK 24, which the pinned Gradle
#      cannot run on. Setting it here -- for this process only -- means a build
#      behaves identically whoever invokes it, without touching the machine's
#      environment.
#   2. The Gradle distribution under tools/, so no ambient `gradle` on PATH can
#      decide what this project builds with.
#
# Usage:
#   .\gw.ps1 run --args="play-fold"       # play a fight
#   .\gw.ps1 test --rerun-tasks           # the suite, forced (see below)
#   .\gw.ps1 capture -Pscene=duel-parry -Pout=out/captures/x -Pframes=24 -Pcols=6
#
# Note on `test`: capture frames are not declared Gradle inputs, so a green
# UP-TO-DATE build can certify a suite that never ran against them. Use
# --rerun-tasks whenever the answer matters.
#
# ASCII only, deliberately. Windows PowerShell 5.1 reads a .ps1 with no BOM as
# ANSI, so a UTF-8 em dash in a comment mangles into bytes that break string
# parsing further down the file. This script was written with em dashes once and
# would not parse at all. Keep it plain.

$ErrorActionPreference = 'Stop'

$jdk = 'C:\Program Files\Java\jdk-21.0.10'
if (-not (Test-Path $jdk)) {
    Write-Error "JDK 21 not found at $jdk -- ./gw pins the same path; update both together."
    exit 1
}

$gradle = Join-Path $PSScriptRoot 'tools\gradle-8.10.2\bin\gradle.bat'
if (-not (Test-Path $gradle)) {
    Write-Error "Pinned Gradle not found at $gradle"
    exit 1
}

$env:JAVA_HOME = $jdk
& $gradle --project-dir $PSScriptRoot --console=plain @args

# Propagate Gradle's exit code. Without this a failing build would look like a
# passing one to anything that chains off this script.
exit $LASTEXITCODE
