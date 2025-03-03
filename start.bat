@echo off
chcp 65001
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Xmx2G -jar scheduler-bot-1.0.jar
pause 