# 빌드 단계 — Gradle 래퍼로 bootJar를 만든다 (테스트는 CI에서 돈다)
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# 실행 단계
FROM eclipse-temurin:25-jre
# 엽서 글씨(PostcardRenderer)용 한·중·일 글꼴. 기본 이미지에는 DejaVu(라틴·베트남어·키릴)만 있어 한글이 네모로 나온다
# 패키지는 고딕·명조 × 보통·굵게 4개 파일(약 90MB)이라, 쓰는 고딕 보통(NotoSansCJK-Regular, 약 19MB)만 남긴다
RUN apt-get update && apt-get install -y --no-install-recommends fonts-noto-cjk \
    && rm -f /usr/share/fonts/opentype/noto/NotoSerifCJK-* /usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc \
    && fc-cache -f && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
