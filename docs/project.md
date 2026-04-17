# Project: <Name>

## Mission
<One paragraph on what this application does and who it serves.>

## Tech Stack
- Language: Java 25
- Framework: Spring Boot 4.x
- Build tool: Maven
- Database: PostgreSQL 18
- ORM: Spring Data JPA / Hibernate
- Migrations: Flyway
- Messaging: (e.g., Apache Kafka, RabbitMQ, or none)
- Testing: JUnit 5, Mockito, Testcontainers, RestAssured
- Other: Spring Security, MapStruct, Lombok

## Architecture
<Describe your pattern: Layered / Hexagonal / DDD / Modular Monolith / Microservices>
<Include package structure conventions.>

## Conventions
- Package naming: com.example.<module>.<layer>
- REST base path: /api/v1
- Error handling: GlobalExceptionHandler via @RestControllerAdvice
- All endpoints require authentication unless annotated @Public
- <Any other conventions Claude should follow>

## Approved Dependencies
<List libraries already approved for use. Anything outside this list requires a flag>
