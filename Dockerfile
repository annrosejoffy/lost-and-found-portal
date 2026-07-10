FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY . .

RUN javac -d bin src/*.java

ENV PORT=8080
EXPOSE 8080

CMD ["java", "-cp", "bin", "WebServer"]
