FROM gradle:8.12-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/kontekscek-backend/ ./
EXPOSE 8080
ENV PORT=8080
CMD ["bin/kontekscek-backend"]
