@echo off
echo =========================================
echo  AUTO GIT COMMIT AND PUSH
echo =========================================
echo.

echo [1/3] Staging all changes...
git add -A

echo.
set /p msg="Enter commit message (default: 'update'): "
if "%msg%"=="" set msg=update

echo.
echo [2/3] Committing changes...
git commit -m "%msg%"

echo.
echo [3/3] Pushing to GitHub...
git push

echo.
echo =========================================
echo  Success! Action workflow will build now.
echo =========================================
pause
