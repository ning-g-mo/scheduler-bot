#!/bin/bash

# 设置UTF-8编码
export LANG=zh_CN.UTF-8

# 启动Java程序
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Xmx2G -jar scheduler-bot-1.0.jar 