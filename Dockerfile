FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY . .

RUN mkdir -p out/main \
    && javac --release 21 -d out/main $(find src/main/java -name '*.java')

EXPOSE 8080
CMD ["java", "-cp", "out/main", "com.kaysonmirain.telemetry.Main", "--serve", "8080"]
