# Guide for using backend-project

Use this sample Spring Boot REST API to sync your contacts.

## Prerequisites

Following platform is required to run the application:

- Java JDK 8
  - Set `JAVA_HOME` system variable with a valid JDK path and add `${JAVA_HOME}/bin` to the PATH variable

## Quick Start

Download the source code of the sample from github.

Build you application:

```bash
./gradlew build
```

**Note:** On Windows use `gradlew` instead of `./gradlew`

Start your application:

```bash
./gradlew bootRun
```

## Use the Application

Just make a Http Get Request to `localhost:9000/back-end-project-api/contacts/sync`!