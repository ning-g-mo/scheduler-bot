FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 复制JAR文件和启动脚本
COPY target/scheduler-bot-1.1.1.jar /app/
COPY start.sh /app/

# 设置权限
RUN chmod +x /app/start.sh

# 创建配置目录和日志目录
RUN mkdir -p /app/logs /app/exports

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 设置编码
ENV LANG=zh_CN.UTF-8
ENV LC_ALL=zh_CN.UTF-8

# 设置卷，用于持久化配置和日志
VOLUME ["/app/config.yml", "/app/logs", "/app/exports"]

# 启动命令
CMD ["java", "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8", "-Xmx2G", "-jar", "scheduler-bot-1.1.1.jar"] 