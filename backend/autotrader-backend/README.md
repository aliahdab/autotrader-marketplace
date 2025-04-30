# Autotrader Marketplace Backend

This is the backend service for the Autotrader Marketplace application, built with Spring Boot.

## Prerequisites

- Java 21 or higher
- Gradle (or use the included Gradle wrapper)
- PostgreSQL (for production, H2 is used for development)

## Project Structure

```
autotrader-backend/
├── src/main/java/com/autotrader/autotraderbackend/
│   ├── AutotraderBackendApplication.java    # Main application class
│   ├── config/                              # Configuration classes
│   ├── controller/                          # REST controllers
│   ├── model/                               # Entity models
│   ├── payload/                             # Request/response objects
│   ├── repository/                          # Data access layer
│   ├── security/                            # Security related classes
│   └── service/                             # Service layer
└── src/main/resources/
    └── application.properties               # Application configuration
```

## Getting Started

### Building the Application

```bash
./gradlew clean build
```

### Running the Application

```bash
./gradlew bootRun
```

The application will start on port 8080 by default.

### Environment Configuration

The application uses different configurations for development and production:

- **Development**: Uses H2 in-memory database by default
- **Production**: Requires PostgreSQL database connection

Configure the database connection in `application.properties` for production or use environment variables.

## API Documentation

### Authentication Endpoints

- `POST /api/auth/signup` - Register a new user
- `POST /api/auth/signin` - Login and receive JWT token

### Status Endpoints

- `GET /status` - Check if service is running
- `GET /api/status` - Check if API is accessible

### Car Listing Endpoints (Coming Soon)

- `GET /api/cars` - Get all car listings
- `GET /api/cars/{id}` - Get car by ID
- `POST /api/cars` - Create a new car listing
- `PUT /api/cars/{id}` - Update car listing
- `DELETE /api/cars/{id}` - Delete car listing

## Testing

### Unit and Integration Tests

Run all standard tests with:
```bash
./gradlew test
```

### API Tests with Postman

We have automated API tests using Postman collections and Newman:

```bash
# Run all tests including Postman API tests
./gradlew apiTest

# Run just the API tests directly with the script
./src/test/scripts/run_postman_tests.sh
```

The API tests verify:
- User registration
- User authentication with JWT
- Role-based permissions
- CRUD operations for car listings

Test reports are generated in HTML and JUnit XML formats:
- HTML: `build/test-reports/postman/report.html`
- JUnit XML: `build/test-reports/postman/junit-report.xml`

### CI/CD Integration

The Postman tests are integrated into our CI/CD pipeline using GitHub Actions. Every push to main or develop branches triggers the test suite.

## Security

The application uses JWT (JSON Web Token) for authentication. All requests to protected endpoints should include the JWT token in the Authorization header:

```
Authorization: Bearer <your_jwt_token>
```

## Development Notes

### H2 Console

The H2 console is enabled in development mode and can be accessed at:

```
http://localhost:8080/h2-console
```

JDBC URL: Check the application startup logs for the current URL

Username: SA
Password: (leave empty)

### Lombok

This project uses Lombok to reduce boilerplate code. Make sure your IDE has Lombok plugin installed.

## Project Roadmap

Refer to `PROJECT_PLAN.md` for the detailed project roadmap and planned features.

## Troubleshooting

### Common Issues

1. **401 Unauthorized**: Make sure you're including the JWT token in your request header
2. **Database Connection**: Verify your PostgreSQL connection details in production

### Logging

Set logging levels in `application.properties`:

```properties
logging.level.org.springframework.web=DEBUG
logging.level.com.autotrader=DEBUG
logging.level.org.springframework.security=DEBUG
```

## Contributing

1. Follow the standard Git flow (feature branches, pull requests)
2. Write tests for new features
3. Update documentation as needed
