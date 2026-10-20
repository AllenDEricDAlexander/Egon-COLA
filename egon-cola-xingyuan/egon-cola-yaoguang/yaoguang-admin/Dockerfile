FROM eclipse-temurin:21-jre-jammy

ARG APP_UID=10001
ARG APP_GID=10001

RUN groupadd --gid "${APP_GID}" yaoguang \
    && useradd --uid "${APP_UID}" --gid "${APP_GID}" \
        --no-create-home --shell /usr/sbin/nologin yaoguang

WORKDIR /app
COPY --chown=yaoguang:yaoguang \
    target/yaoguang-admin-exec.jar \
    /app/app.jar

USER yaoguang
EXPOSE 8090

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
