# CD Web Backend

## Project Structure
- `src/main/java/com/cdweb/be/`: Project source code.
    - `config/`: Security, CORS, Swagger configurations.
    - `controller/`: REST controllers.
    - `service/`: Business logic interfaces and implementations.
    - `repository/`: JPA repositories.
    - `entity/`: Database entities.
    - `dto/`: Request/Response data transfer objects.
    - `exception/`: Custom exceptions and global handlers.
    - `util/`: Utility classes.
    - `mapper/`: MapStruct/ModelMapper mappers.
- `src/main/resources/`: Configuration and resources.
- `docs/`: Documentation.

## Prerequisites
- Java 17+
- MySQL Server

## Setup
1. Clone the repository.
2. Update database credentials in `src/main/resources/application.yml`.
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```
