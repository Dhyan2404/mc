@echo off
setlocal enabledelayedexpansion

:: Prompt the user for a dynamic commit message
set /p commit_message="Enter your commit message: "

:: If the message is empty, fall back to the default "UR CHANGE"
if "!commit_message!"=="" set commit_message=UR CHANGE

echo.
echo Running Git commands...
echo.

:: Directly execute the standalone Git chain
git add . && git commit -m "!commit_message!" && git push origin main

:: Check if the operations succeeded
if %errorlevel% equ 0 (
    echo.
    echo Successfully pushed to origin main!
    echo This window will automatically close in 5 seconds...
    timeout /t 5 >nul
    exit
) else (
    echo.
    echo [ERROR] An error occurred during execution.
    pause
)
