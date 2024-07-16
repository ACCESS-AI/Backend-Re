################### BUILD ###################
FROM gradle:8.5.0-jdk17-alpine AS build

WORKDIR /home/gradle/Chatbot

# Clone the chatbot library
RUN apk add --no-cache git && \
    git clone https://github.com/ACCESS-AI/Chatbot.git .

RUN chmod +x gradlew
RUN ./gradlew publish

# Build backend
COPY --chown=gradle:gradle . /home/gradle/src

WORKDIR /home/gradle/src

RUN gradle bootJar

RUN ls -la /home/gradle/src/build/libs

################### RUN ###################
FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY --from=build /home/gradle/src/build/libs/*.jar /app/backend.jar

EXPOSE 8081

CMD ["java", "-jar", "/app/backend.jar"]