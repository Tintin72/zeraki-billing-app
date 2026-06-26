# Billing Application API

A Spring Boot REST API for managing customers, invoices, and payments with dashboard analytics.

## Tech Stack

- Java 17
- Spring Boot 4.1.0
- MySQL 8.0
- Redis 7 (dashboard caching)
- Flyway (schema migrations)
- Docker / Docker Compose

## Prerequisites

- Docker and Docker Compose installed

## Running the Application

```bash
docker compose up --build -d
```

The API will be available at `http://localhost:8080`.  
Swagger UI: `http://localhost:8080/swagger-ui.html`

Database migrations run automatically on startup via Flyway.

## API Endpoints

### Customers
```
POST   /api/customers
GET    /api/customers
GET    /api/customers/{id}
PUT    /api/customers/{id}
DELETE /api/customers/{id}
```

### Invoices
```
POST   /api/invoices
GET    /api/invoices
GET    /api/invoices/{id}
PUT    /api/invoices/{id}
DELETE /api/invoices/{id}
GET    /api/invoices/overdue?customerId=&startDate=&endDate=
```

### Payments
```
POST   /api/payments
GET    /api/payments
GET    /api/payments/{id}
```

### Dashboard
```
GET    /api/dashboard/summary?startDate=&endDate=
GET    /api/dashboard/top-customers?startDate=&endDate=
GET    /api/dashboard/monthly-revenue?startDate=&endDate=
```

Date format for query params: `YYYY-MM-DD`

## Caching

Dashboard endpoints are cached in Redis with a 5-minute TTL. The cache key includes the `startDate` and `endDate` parameters, so each unique date range is cached independently. Cache entries expire automatically — there is no explicit eviction on write.

## Business Rules

- Invoice status is derived automatically from payments: `PENDING` → `PARTIALLY_PAID` → `PAID`
- A payment cannot exceed the invoice amount
- An invoice with payments cannot be deleted
- Customer email must be unique
- Invoice `dueDate` must be in the future
- Payment `paymentDate` cannot be in the future

## Database Migrations

Migrations live in `src/main/resources/db/migration/` and follow the naming convention `V{n}__{description}.sql`. Flyway applies any unrun migrations on startup and tracks history in the `flyway_schema_history` table.

To add a new migration, create the next versioned file — never edit existing ones.

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/billingdb` | JDBC connection URL |
| `SPRING_DATASOURCE_USERNAME` | `root` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | Database password |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
