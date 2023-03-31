FROM maven:3.8-jdk-8

COPY src /app/src  

COPY pom.xml /app  

RUN mvn -f /app/pom.xml install -DskipTests

COPY . .

EXPOSE 7300

CMD java -jar /app/target/people-backend.jar --spring.profiles.active=local