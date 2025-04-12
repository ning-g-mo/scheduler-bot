@echo off
echo 正在启动定时任务机器人 v1.2.2...
echo.

set JAVA_OPTS=-Xmx512m -Dfile.encoding=UTF-8

if "%1"=="nogui" (
  echo 以无界面模式启动...
  java %JAVA_OPTS% -jar scheduler-bot-1.2.2.jar nogui
) else (
  java %JAVA_OPTS% -jar scheduler-bot-1.2.2.jar
)

if errorlevel 1 (
  echo 启动失败，请检查Java环境是否正确安装。
  pause
  exit /b 1
) 