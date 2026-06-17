FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Install Maven + Node.js
RUN apt-get update && apt-get install -y maven nodejs npm && rm -rf /var/lib/apt/lists/*

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Download dependencies and build
RUN mvn clean package -Dmaven.test.skip=true

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy jar file from builder
COPY --from=builder /app/target/*.jar app.jar

# Copy PPT generation scripts
COPY scripts/ ./scripts/

# Install pptxgenjs for PPT generation
RUN npm install -g pptxgenjs 2>/dev/null || true

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"
ENV PPT_NODE_SCRIPT=/app/scripts/generate-ppt.js
ENV FFMPEG_PATH=/usr/bin/ffmpeg

# Start application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]