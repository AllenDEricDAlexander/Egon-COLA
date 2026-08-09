FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/egon-cola-platform-dynamic-config-center-admin-exec.jar app.jar
EXPOSE 18080 19080
ENTRYPOINT ["java","-jar","/app/app.jar"]
