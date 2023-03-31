# WATU - People Networks

WATU Application which makes connecting people easier.

## Getting Started

Thanks for downloading WATU Codebase. These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

JDK
MongoDB
Redis
AWS Secret and Access Key

### Installing

JDK Setup:
https://docs.oracle.com/javase/10/install/installation-jdk-and-jre-macos.htm

Mongo Setup:
https://docs.mongodb.com/manual/tutorial/install-mongodb-on-os-x/

Redis Setup:
https://medium.com/@petehouston/install-and-config-redis-on-mac-os-x-via-homebrew-eb8df9a4f298

AWS Secret and Access Key:
Please contact your Infra Administrator for the keys.

## Set Up on local
Create a copy of application-development.properties file in the same folder and rename it to application-local.properties

### Update Configurations in application-local.properties file

Replace ${MONGO_URI:} with mongoURI
```
spring.data.mongodb.uri=mongodb://localhost:27017
```

Replace ${REDIS_HOST:} with redisHost and ${REDIS_PORT:} with Redis Port
```
redis.host=localhost
redis.port=6379
```

Replace ${AWS_ACCESS_KEY:} with AWS Access Key and ${AWS_SECRET_KEY:} with AWS Secret Key For Local Environment
```
aws.access-key=<AWS Access Key>
aws.secret-key=<AWS Secret Key>
```

Note: Due to security concerns, for environments other than "local", IAM role is required to be set up on the server to access AWS.

## Running the code

```
mvn spring-boot:run -Dserver.port=<port> -Dspring.profiles.active=<environment>
```
Environments:
1.     local
2.     development
3.     integration
4.     staging
5.     pre-production
6.     production


## Running the tests

```
mvn test  -DactivatedProperties=test
```


