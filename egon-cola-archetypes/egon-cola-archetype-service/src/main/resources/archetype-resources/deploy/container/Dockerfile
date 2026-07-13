# syntax=docker/dockerfile:1
#set( $symbol_dollar = '$' )

ARG BUILD_IMAGE=eclipse-temurin:21-jdk-jammy
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy

FROM ${symbol_dollar}{BUILD_IMAGE} AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests package

FROM ${symbol_dollar}{RUNTIME_IMAGE} AS extractor
WORKDIR /workspace
ARG JAR_FILE=${rootArtifactId}-starter/target/*.jar
COPY --from=builder /workspace/${symbol_dollar}{JAR_FILE} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM ${symbol_dollar}{RUNTIME_IMAGE} AS runtime
WORKDIR /app

ARG CONTAINER_ENGINE=oci
ARG APP_UID=10001
ARG APP_GID=10001

LABEL org.opencontainers.image.build.engine="${symbol_dollar}{CONTAINER_ENGINE}"

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

RUN case "${symbol_dollar}{CONTAINER_ENGINE}" in \
        oci|docker|podman|nerdctl) ;; \
        *) echo "Unsupported CONTAINER_ENGINE=${symbol_dollar}{CONTAINER_ENGINE}" >&2; exit 64 ;; \
    esac \
    && groupadd --gid "${symbol_dollar}{APP_GID}" app \
    && useradd --uid "${symbol_dollar}{APP_UID}" --gid "${symbol_dollar}{APP_GID}" --no-create-home --shell /usr/sbin/nologin app \
    && apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/logs \
    && chown -R app:app /app

COPY --from=extractor /workspace/extracted/dependencies/ ./
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor /workspace/extracted/application/ ./

USER app

EXPOSE 8081 50051

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8081/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${symbol_dollar}JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
