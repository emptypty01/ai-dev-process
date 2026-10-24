FROM eclipse-temurin:21-jre
WORKDIR /app
COPY backend/target/order-demo-*.jar app.jar
ENV SERVER_ADDRESS=0.0.0.0 SERVER_PORT=8080 JAVA_TOOL_OPTIONS=-Xms128m\ -Xmx384m
EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
