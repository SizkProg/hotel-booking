FROM eclipse-temurin:17-jre
ARG MODULE
COPY ${MODULE}/target/${MODULE}-1.0.0.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
