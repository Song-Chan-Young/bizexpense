# BizExpense 배포 이미지: React 빌드 결과를 Spring Boot 가 함께 서비스하는 단일 컨테이너

# 1) 프론트엔드 빌드
FROM node:22-slim AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) 백엔드 빌드 (프론트 빌드 결과를 static 리소스로 포함)
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts lombok.config ./
COPY gradle ./gradle
# 의존성만 먼저 받아 두면 소스만 바뀐 재빌드가 빨라진다
RUN ./gradlew dependencies --no-daemon -q > /dev/null
COPY src ./src
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN ./gradlew bootJar --no-daemon -x test

# 3) 실행
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Seoul
COPY --from=backend /app/build/libs/app.jar app.jar
EXPOSE 8080
# 무료/소형 인스턴스(512MB) 메모리에 맞춰 힙을 컨테이너 메모리 비율로 제한
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=Asia/Seoul", "-jar", "/app/app.jar"]
