@echo off
setlocal
cd /d "%~dp0"
if not exist ..\build mkdir ..\build
javac -d ..\build *.java
if errorlevel 1 exit /b 1
if /i "%~1"=="test" goto test
java -cp ..\build Chess.Main
exit /b
:test
javac -cp ..\build -d ..\build ..\tests\*.java
if errorlevel 1 exit /b 1
java -Xmx512m -Djava.awt.headless=true -cp ..\build Chess.ChessTest
