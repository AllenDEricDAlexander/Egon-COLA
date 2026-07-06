# syntax=docker/dockerfile:1.7
#set( $symbol_dollar = '$' )

FROM eclipse-temurin:21-jre-jammy AS extractor
WORKDIR /workspace

ARG STARTER_MODULE=${rootArtifactId}-starter
ARG JAR_FILE=${symbol_dollar}{STARTER_MODULE}/target/*.jar

COPY ${symbol_dollar}{JAR_FILE} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

RUN groupadd -r app && useradd -r -g app app

COPY --from=extractor /workspace/extracted/dependencies/ ./
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor /workspace/extracted/application/ ./

USER app

EXPOSE 8081 50051

ENTRYPOINT ["sh", "-c", "exec java ${symbol_dollar}JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
