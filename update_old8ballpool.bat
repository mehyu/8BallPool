@echo off
title OLD8BallPool Android - Auto Update and Push
color 0A

echo ===================================================
echo   OLD8BallPool (Android) - Auto Update to GitHub
echo ===================================================
echo.

cd /d "%~dp0"

echo [1/3] Staging changes...
git add -A

set "commit_msg="
set /p commit_msg="Enter commit message (or press ENTER for default): "
if "%commit_msg%"=="" (
    set "commit_msg=Auto update OLD8BallPool Android [%date% %time%]"
)

echo.
echo [2/3] Committing changes...
git commit -m "%commit_msg%"

echo.
echo [3/3] Pushing to GitHub...
git push

echo.
echo ===================================================
echo   Update successfully pushed!
echo   GitHub Action is now building your Android APK.
echo ===================================================
echo.
pause
