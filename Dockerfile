# Stage 1: Build bằng Maven
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Chạy ứng dụng Java (Sửa lỗi "not found" ở đây)
FROM eclipse-temurin:17-jdk-focal
WORKDIR /app
# Copy file .jar từ thư mục target sang
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]