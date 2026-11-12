<#
.SYNOPSIS
    Iniciador del Ecosistema Dulce Mayor para Windows (PowerShell)
.DESCRIPTION
    Levanta el Backend REST API (Spring Boot), Landing Page (Thymeleaf) y los dos frontends React (Vite).
.PARAMETER Stop
    Detiene cualquier proceso activo en los puertos 8080, 8081, 5173 y 5174.
.PARAMETER Help
    Muestra la ayuda de uso.
#>
[CmdletBinding()]
param(
    [switch]$Stop,
    [switch]$Help
)

$ErrorActionPreference = "Continue"

$ROOT_DIR = $PSScriptRoot
if (-not $ROOT_DIR) {
    $ROOT_DIR = (Get-Location).Path
}

$LOG_DIR = Join-Path $ROOT_DIR ".logs"
if (-not (Test-Path $LOG_DIR)) {
    New-Item -ItemType Directory -Path $LOG_DIR -Force | Out-Null
}

function Show-Usage {
    Write-Host "Uso: .\start.ps1 [-Stop] [-Help]" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Opciones:"
    Write-Host "  (sin argumentos)   Levanta el backend y las 3 aplicaciones frontend"
    Write-Host "  -Stop              Detiene cualquier instancia activa en los puertos 8080, 8081, 5173 y 5174"
    Write-Host "  -Help              Muestra esta ayuda"
}

if ($Help) {
    Show-Usage
    exit 0
}

function Stop-Ports {
    $ports = @(8080, 8081, 5173, 5174)
    foreach ($port in $ports) {
        try {
            $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
            if ($connections) {
                $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
                foreach ($procId in $pids) {
                    if ($procId -gt 0 -and $procId -ne $PID) {
                        try {
                            cmd.exe /c "taskkill /PID $procId /T /F" 2>&1 | Out-Null
                            Write-Host "[STOP] Proceso $procId liberado en puerto $port" -ForegroundColor Yellow
                        } catch { }
                    }
                }
            }
        } catch { }
    }
    # Wait briefly for OS to release listening sockets
    for ($i = 0; $i -lt 10; $i++) {
        $stillListening = Get-NetTCPConnection -LocalPort $ports -State Listen -ErrorAction SilentlyContinue
        if (-not $stillListening) { break }
        Start-Sleep -Milliseconds 300
    }
}

if ($Stop) {
    Write-Host "[INFO] Liberando puertos 8080, 8081, 5173 y 5174..." -ForegroundColor Yellow
    Stop-Ports
    Write-Host "[OK] Puertos liberados." -ForegroundColor Green
    exit 0
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "    Dulce Mayor - Iniciador del Ecosistema Completo (Win)   " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# Verify dependencies
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] 'java' no está instalado o no se encuentra en el PATH." -ForegroundColor Red
    exit 1
}

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] 'npm' no está instalado o no se encuentra en el PATH." -ForegroundColor Red
    exit 1
}

# Prepare frontend dependencies and env files
function Prepare-ReactApp($appDir, $appName) {
    $envExample = Join-Path $appDir ".env.example"
    $envFile = Join-Path $appDir ".env"
    if ((Test-Path $envExample) -and -not (Test-Path $envFile)) {
        Write-Host "[SETUP] Creando archivo .env por defecto en $appName..." -ForegroundColor Cyan
        Copy-Item $envExample $envFile -Force
    }

    $nodeModules = Join-Path $appDir "node_modules"
    if (-not (Test-Path $nodeModules)) {
        Write-Host "[SETUP] Instalando dependencias npm en $appName..." -ForegroundColor Cyan
        Push-Location $appDir
        try {
            npm install
        } finally {
            Pop-Location
        }
    }
}

Prepare-ReactApp (Join-Path $ROOT_DIR "client\central-management") "client\central-management"
Prepare-ReactApp (Join-Path $ROOT_DIR "client\order-management") "client\order-management"

# Free ports
Stop-Ports

# Helper to build Java command with classpath if available
function Get-JavaCommand($appDir, $mainClass) {
    $cpFile = Join-Path $appDir ".mvn\runtime-classpath.txt"
    $classesDir = Join-Path $appDir "target\classes"
    $userProfile = [Environment]::GetFolderPath("UserProfile")
    $m2Repo = Join-Path $userProfile ".m2\repository"

    if ((Test-Path $classesDir) -and (Test-Path $cpFile)) {
        $lines = Get-Content $cpFile
        $allFound = $true
        $cpList = @($classesDir)

        foreach ($relJar in $lines) {
            $relJar = $relJar.Trim()
            if ($relJar.Length -eq 0) { continue }
            $fullJar = Join-Path $m2Repo $relJar.Replace("/", "\")
            if (Test-Path $fullJar) {
                $cpList += $fullJar
            } else {
                $allFound = $false
                break
            }
        }

        if ($allFound) {
            $cpString = $cpList -join ";"
            return @{
                Executable = "java"
                Arguments = @("-cp", $cpString, $mainClass)
            }
        }
    }

    # Fallback to mvnw.cmd
    return @{
        Executable = "cmd.exe"
        Arguments = @("/c", "mvnw.cmd", "spring-boot:run")
    }
}

$runningProcesses = @()

function Start-LoggedService($name, $color, $workDir, $logName, $execPath, $argsList) {
    $logPath = Join-Path $LOG_DIR $logName
    if (Test-Path $logPath) {
        Remove-Item $logPath -Force -ErrorAction SilentlyContinue
    }

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $execPath
    if ($argsList -is [array]) {
        $psi.Arguments = ($argsList | ForEach-Object {
            if ($_ -match '\s') { "`"$_`"" } else { $_ }
        }) -join " "
    } else {
        $psi.Arguments = $argsList
    }
    $psi.WorkingDirectory = $workDir
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $psi
    $process.EnableRaisingEvents = $true

    $msgData = @{
        Name    = $name
        Color   = $color
        LogPath = $logPath
    }

    $outAction = {
        $data = $EventArgs.Data
        if ($data) {
            $meta = $Event.MessageData
            $timestamp = (Get-Date).ToString("HH:mm:ss")
            $line = "[$timestamp] $($meta.Name) : $data"
            Write-Host $line -ForegroundColor $meta.Color
            Add-Content -Path $meta.LogPath -Value $data -ErrorAction SilentlyContinue
        }
    }

    Register-ObjectEvent -InputObject $process -EventName OutputDataReceived -Action $outAction -MessageData $msgData | Out-Null
    Register-ObjectEvent -InputObject $process -EventName ErrorDataReceived -Action $outAction -MessageData $msgData | Out-Null

    [void]$process.Start()
    $process.BeginOutputReadLine()
    $process.BeginErrorReadLine()

    return $process
}

Write-Host "[START] Iniciando Backend REST API (puerto 8080)..." -ForegroundColor Blue
$serverCmd = Get-JavaCommand (Join-Path $ROOT_DIR "server") "ing.software.dulcemayor.DulceMayorApplication"
$procServer = Start-LoggedService "SERVER" "Cyan" (Join-Path $ROOT_DIR "server") "server.log" $serverCmd.Executable $serverCmd.Arguments
$runningProcesses += $procServer

Write-Host "[START] Iniciando Landing Page Thymeleaf (puerto 8081)..." -ForegroundColor Magenta
$landingCmd = Get-JavaCommand (Join-Path $ROOT_DIR "client\landing") "ing.software.dulcemayor.landing.LandingApplication"
$procLanding = Start-LoggedService "LANDING" "Magenta" (Join-Path $ROOT_DIR "client\landing") "landing.log" $landingCmd.Executable $landingCmd.Arguments
$runningProcesses += $procLanding

Write-Host "[START] Iniciando Central Management React (puerto 5173)..." -ForegroundColor Green
$procCentral = Start-LoggedService "CENTRAL" "Green" (Join-Path $ROOT_DIR "client\central-management") "central-management.log" "cmd.exe" @("/c", "npm", "run", "dev", "--", "--port", "5173", "--strictPort")
$runningProcesses += $procCentral

Write-Host "[START] Iniciando Order Management React (puerto 5174)..." -ForegroundColor Yellow
$procOrder = Start-LoggedService "ORDER" "Yellow" (Join-Path $ROOT_DIR "client\order-management") "order-management.log" "cmd.exe" @("/c", "npm", "run", "dev", "--", "--port", "5174", "--strictPort")
$runningProcesses += $procOrder

Write-Host ""
Write-Host "------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "Servicios levantándose:" -ForegroundColor White
Write-Host "  • Backend API:          http://localhost:8080/api" -ForegroundColor Cyan
Write-Host "  • Consola H2:           http://localhost:8080/h2-console" -ForegroundColor Cyan
Write-Host "  • Landing Page:         http://localhost:8081" -ForegroundColor Magenta
Write-Host "  • Central Management:   http://localhost:5173" -ForegroundColor Green
Write-Host "  • Order Management:     http://localhost:5174" -ForegroundColor Yellow
Write-Host ""
Write-Host "  Logs guardados en: .logs\" -ForegroundColor DarkGray
Write-Host "  Presiona Ctrl+C para detener todos los servicios." -ForegroundColor White
Write-Host "------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host ""

try {
    while ($true) {
        Start-Sleep -Seconds 1
    }
} finally {
    Write-Host ""
    Write-Host "[INFO] Deteniendo todos los servicios de Dulce Mayor..." -ForegroundColor Yellow
    foreach ($proc in $runningProcesses) {
        if ($proc -and -not $proc.HasExited) {
            try {
                # Kill process tree
                cmd.exe /c "taskkill /PID $($proc.Id) /T /F" 2>&1 | Out-Null
            } catch { }
        }
    }
    Stop-Ports
    Write-Host "[OK] Todos los servicios fueron detenidos correctamente." -ForegroundColor Green
}
