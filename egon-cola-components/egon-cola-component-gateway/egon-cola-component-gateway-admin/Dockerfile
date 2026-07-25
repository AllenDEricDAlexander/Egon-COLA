FROM eclipse-temurin:21-jre-jammy

ARG APP_UID=10001
ARG APP_GID=10001

RUN groupadd --gid "${APP_GID}" gateway \
    && useradd --uid "${APP_UID}" --gid "${APP_GID}" \
        --no-create-home --shell /usr/sbin/nologin gateway

WORKDIR /app
COPY --chown=gateway:gateway \
    target/egon-cola-component-gateway-admin.jar \
    /app/app.jar

USER gateway
EXPOSE 18080

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
