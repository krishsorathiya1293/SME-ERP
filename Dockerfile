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
# 2) Run stage (Optimized for Render/AWS Free Tier)
########################################
# Use JRE instead of JDK to save ~200MB of RAM/Disk
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the JAR
COPY --from=builder /app/web-app/target/*.jar app.jar

# Optimization: Limit Java Memory to fit in free tier (1GB RAM)
# This prevents the container from being killed for using too much RAM
ENV JAVA_OPTS="-Xmx512M -Xms256M"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]