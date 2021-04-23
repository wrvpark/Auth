FROM maven:3.8.1-openjdk-17-slim as build
RUN mkdir /app
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn install -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM openjdk:17-slim
VOLUME /tmp
ARG DEPENDENCY=/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
EXPOSE 8080
ENTRYPOINT ["java","-cp","app:app/lib/*","com.wrvpark.authserver.AuthServerApplication"]

#FROM maven:3.8.1-openjdk-17-slim as build
#RUN mkdir /app
#COPY . /app
#WORKDIR /app
#RUN mvn clean install
#EXPOSE 8080
#ENTRYPOINT ["mvn", "spring-boot:run"]