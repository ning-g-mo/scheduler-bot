@echo off
chcp 65001
:: 启用ANSI颜色支持
reg add HKCU\Console /v VirtualTerminalLevel /t REG_DWORD /d 1 /f > nul 2>&1
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Xmx2G -jar scheduler-bot-1.0.jar
pause 