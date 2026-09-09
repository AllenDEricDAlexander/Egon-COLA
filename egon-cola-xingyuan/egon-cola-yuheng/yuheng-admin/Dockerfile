FROM eclipse-temurin:21-jre-jammy

ARG APP_UID=10001
ARG APP_GID=10001

RUN groupadd --gid "${APP_GID}" yuheng \
    && useradd --uid "${APP_UID}" --gid "${APP_GID}" \
        --no-create-home --shell /usr/sbin/nologin yuheng

WORKDIR /app
COPY --chown=yuheng:yuheng \
    target/yuheng-admin-exec.jar \
    /app/app.jar

USER yuheng
EXPOSE 18080

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
