FROM eclipse-temurin:21-jre-jammy

ARG APP_UID=10001
ARG APP_GID=10001

RUN groupadd --gid "${APP_GID}" yuheng \
    && useradd --uid "${APP_UID}" --gid "${APP_GID}" \
        --no-create-home --shell /usr/sbin/nologin yuheng

WORKDIR /app
COPY --chown=yuheng:yuheng \
    target/yuheng-mcp-gateway-exec.jar \
    /app/app.jar

RUN mkdir -p /var/lib/egon-yuheng-mcp \
    && chown yuheng:yuheng /var/lib/egon-yuheng-mcp

USER yuheng
ENV YUHENG_MCP_ENGINE_DATA_DIRECTORY=/var/lib/egon-yuheng-mcp/rules
EXPOSE 18084 18085
VOLUME ["/var/lib/egon-yuheng-mcp"]

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
