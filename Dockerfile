# Build stage
FROM maven:3.8.4-openjdk-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Runtime stage
FROM tomcat:10-jdk17-openjdk-slim

# Remove default ROOT application
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copy the built WAR to Tomcat
COPY --from=builder /app/target/java-security.war /usr/local/tomcat/webapps/java-security.war

# Create a symbolic link to make it accessible at root (optional)
RUN ln -s /usr/local/tomcat/webapps/java-security /usr/local/tomcat/webapps/ROOT

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/ping || exit 1

# Set Tomcat to run in foreground
ENV CATALINA_BASE=/usr/local/tomcat
CMD ["catalina.sh", "run"]
