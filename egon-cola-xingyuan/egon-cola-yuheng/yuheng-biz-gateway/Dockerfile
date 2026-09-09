FROM eclipse-temurin:21-jre-jammy

ARG APP_UID=10001
ARG APP_GID=10001

RUN groupadd --gid "${APP_GID}" yuheng \
    && useradd --uid "${APP_UID}" --gid "${APP_GID}" \
        --no-create-home --shell /usr/sbin/nologin yuheng

WORKDIR /app
COPY --chown=yuheng:yuheng \
    target/yuheng-biz-gateway-exec.jar \
    /app/app.jar

RUN mkdir -p /var/lib/egon-yuheng \
    && chown yuheng:yuheng /var/lib/egon-yuheng

USER yuheng
EXPOSE 18081 18082 18083 19090
VOLUME ["/var/lib/egon-yuheng"]

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
