#!/bin/bash
echo "正在启动定时任务机器人 v1.2.2..."
echo

JAVA_OPTS="-Xmx512m -Dfile.encoding=UTF-8"

if [ "$1" = "nogui" ]; then
  echo "以无界面模式启动..."
  java $JAVA_OPTS -jar scheduler-bot-1.2.2.jar nogui
else
  java $JAVA_OPTS -jar scheduler-bot-1.2.2.jar
fi

if [ $? -ne 0 ]; then
  echo "启动失败，请检查Java环境是否正确安装。"
  exit 1
fi 