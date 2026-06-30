# Verify Background Processor

A centralized background job processing system for the Verify platform. Handles long-running and CPU-intensive jobs with a plugin-based extensible architecture for custom job providers.

## Features

- Plugin-based job provider architecture
- Multiple email service integrations (SMTP, MailGun, SendGrid, Mandrill, SparkPost, Amazon SES)
- User synchronization services
- License management integration
- Webhook notification services
- Job scheduling and monitoring dashboard

## Architecture

| Module | Description |
|--------|-------------|
| `ezto-bg-manager` | Main application with job execution engine |
| `ezto-bg-extensions` | Mail service providers and extensions |
| `ezto-bg-sync-services` | User synchronization jobs |
| `ezto-bg-scheduler-services` | Job scheduling |
| `ezto-bg-email-services` | Email template management |
| `ezto-bg-license-services` | License manager integration |
| `ezto-bg-hook-notifier-services` | Webhook notification services |
| `ezto-bg-helpers` | Utility functions |
| `ezto-seed` | Template for external job providers |

## Tech Stack

- Java 21
- Spring Boot 3.4.5
- Maven
- PostgreSQL

## Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL 14+

## Setup

1. Configure database connection:
   ```bash
   cp env.sh env.sh.local
   # Edit env.sh.local with your database credentials
   source env.sh.local
   ```

2. Build and run:
   ```bash
   ./build.sh
   ```

3. Run via JAR:
   ```bash
   java -Dspring.config.location=application.properties -jar ezto-bg.jar
   ```

4. Access:
   - API: http://localhost:8090/
   - Dashboard: http://localhost:8000

### Dev mode (against the shared dev infra)

Start the backing infra once from the repo root, then run this service on the host:

```bash
docker compose -f docker-compose.dev.yml up -d        # postgres/minio/es/maildev on localhost
```

Point it at the dev Postgres (db `jobrnr`), then build & run:

```bash
export JOB_DB_TYPE=psql
export JOB_DB_URL=jdbc:postgresql://localhost:5432/jobrnr
export JOB_DB_USER=admin
export JOB_DB_PASSWORD=admin
./build.sh
java -Dspring.config.location=application.properties -jar ezto-bg.jar
```

## Implementing Custom Jobs

1. Add the dependency:
   ```xml
   <dependency>
       <groupId>com.ezto</groupId>
       <artifactId>ezto-core</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```

2. Create a class implementing `IJobProvider`
3. Provide a unique ID in `getProviderId()` method
4. Add job logic in `doExecute()` method
5. Build with `mvn clean package`
6. Copy the JAR to `build_{version}/providers` folder and restart

## Environment Variables

| Variable | Description |
|----------|-------------|
| `JOB_DB_URL` | PostgreSQL JDBC URL |
| `JOB_DB_TYPE` | Database type (postgres) |
| `JOB_DB_USER` | Database username |
| `JOB_DB_PASSWORD` | Database password |
| `JOB_WORKERS` | Number of worker threads |

## License

Apache License 2.0 - see [LICENSE](./LICENSE) for details.
