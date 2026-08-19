
# ── Stage 1: Build ────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
	
WORKDIR /app
	
# Copy Maven wrapper và pom trước để cache dependencies
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
	
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -q

# Copy source và build
COPY src src
RUN ./mvnw clean package -DskipTests -q
	
# ── Stage 2: Runtime ──────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
	
WORKDIR /app
	
# Tạo user non-root để bảo mật
RUN addgroup -S semd && adduser -S semd -G semd
	
COPY --from=build /app/target/*.jar app.jar
	
RUN chown semd:semd app.jar
USER semd
	
EXPOSE 8080
	
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
