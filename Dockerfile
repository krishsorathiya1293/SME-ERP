########################################
# 1) Build stage (Keep as is)
########################################
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN sed -i 's/\r$//' mvnw
RUN chmod +x mvnw
RUN ./mvnw -DskipTests clean package -U

########################################
# 2) Run stage (Optimized for AWS Free Tier)
########################################
# Use official Playwright image which includes Java 21, Browsers, and System Dependencies
FROM mcr.microsoft.com/playwright/java:v1.49.0-jammy
WORKDIR /app

# Copy the JAR
COPY --from=builder /app/web-app/target/*.jar app.jar

# Tell Playwright to use system-installed browsers (already in the Docker image)
# This prevents runtime extraction from nested JARs which causes ZipException
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# Optimization: Limit Java Memory to fit in t3.micro (1GB RAM)
# This prevents the container from being killed by AWS for using too much RAM
ENV JAVA_OPTS="-Xmx512M -Xms256M"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]