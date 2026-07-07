FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/egon-cola-component-dynamic-config-center-admin.jar app.jar
EXPOSE 18080
ENTRYPOINT ["java","-jar","/app/app.jar"]
