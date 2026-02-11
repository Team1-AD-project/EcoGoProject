@echo off
REM Start MongoDB SSH tunnel in background
start cmd /c "ssh -i D:\ssh\EcoGo.pem -o IdentitiesOnly=yes -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=3 -o ExitOnForwardFailure=yes -N -L 127.0.0.1:27017:127.0.0.1:27017 bitnami@47.129.124.55 -p 22"

REM Wait a moment for tunnel to establish
timeout /t 3 /nobreak

REM Start Spring Boot backend
echo Starting EcoGo Backend...
call mvnw.cmd -q spring-boot:run

pause
