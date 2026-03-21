FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

# ✅ Give executable permission to mvnw
RUN chmod +x mvnw

# ✅ Build the project
RUN ./mvnw clean package -DskipTests

# Expose the app port
EXPOSE 8080

# ✅ Replace with your actual JAR file
CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]

