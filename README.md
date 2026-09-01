# Order Processing with ActiveMQ

A small event-driven microservices system that demonstrates asynchronous order processing with Apache ActiveMQ Classic, Spring Boot, Eureka, and React.

## Overview

The application accepts orders through a browser-based React UI. The Order Service publishes an `order.created` event to ActiveMQ. The Email Service consumes that event and records a simulated email, which is displayed back in the UI.

```text
┌─────────────┐   HTTP POST    ┌────────────────┐      JMS       ┌─────────────────┐
│ React UI    │ ──────────────> │ Order Service  │ ─────────────> │ ActiveMQ Broker │
│ :5173       │ <────────────── │ :8081          │               │ :61616          │
└─────────────┘   HTTP GET     └────────────────┘               │ queue:          │
       ▲                         │                              │ order.created   │
       │                         │                              └────────┬────────┘
       │                         │                                       │
       │                         │                          JMS consume  │
       │                         │                                       ▼
       │                         │                              ┌─────────────────┐
       └──────── HTTP GET ───────┴──────────────────────────────│ Email Service   │
              /emails                                          │ :8082          │
                                                              └─────────────────┘

                         ┌─────────────────┐
                         │ Eureka Server   │
                         │ :8761           │
                         └─────────────────┘
```

Eureka registers the Spring services for discovery. ActiveMQ is responsible for transporting business events; Eureka does not carry messages.

## Components

| Component | Responsibility | Port |
|---|---|---:|
| React frontend | Create orders and display orders and consumed email events | 5173 |
| Order Service | Validate requests, create orders, publish events, list orders | 8081 |
| ActiveMQ Classic | Persist and deliver asynchronous JMS messages | 61616 |
| ActiveMQ Console | Broker administration and queue inspection | 8161 |
| Email Service | Consume `order.created` and simulate email delivery | 8082 |
| Eureka Server | Service registration and discovery dashboard | 8761 |

## Order event flow

1. The user submits an order from the React frontend.
2. The frontend sends `POST /orders` to the Order Service.
3. The Order Service validates the email, product, and quantity.
4. The service creates an order ID and publishes an `order.created` message with the order details using `JmsTemplate`.
5. ActiveMQ stores the message in the queue until a consumer receives it.
6. The Email Service receives the message through `@JmsListener`.
7. The Email Service records a simulated email and exposes the recent events through `GET /emails`.
8. The frontend polls `/emails` and displays the consumed message.

## Why use ActiveMQ?

The Order Service and Email Service are loosely coupled. The Order Service does not need the Email Service to be available at the exact moment an order is created. If the consumer is temporarily stopped, ActiveMQ retains the message and delivers it when the consumer reconnects.

This is asynchronous communication: the producer publishes an event and continues, while the consumer processes it independently.

## API

### Create an order

```http
POST /orders
Content-Type: application/json

{
  "customerEmail": "learner@example.com",
  "product": "Notebook",
  "quantity": 2
}
```

### List orders

```http
GET /orders
```

### List consumed email events

```http
GET /emails
```

The current implementation keeps data in process memory. Restarting a service clears its in-memory data.

## Run with Docker Compose

From this directory:

```powershell
docker compose up -d --build
docker compose ps
```

Open:

- Application: http://localhost:5173
- Eureka dashboard: http://localhost:8761
- ActiveMQ console: http://localhost:8161/admin/

ActiveMQ credentials are `student` / `student`. If the console returns HTTP 403 in a browser, use an InPrivate/Incognito window and the trailing-slash URL. The application itself can still be demonstrated through the React UI.

Stop or restart an individual service:

```powershell
docker compose stop email-service
docker compose start email-service
```

View container logs when needed:

```powershell
docker compose logs -f email-service
docker compose logs -f order-service
```

## Demonstration scenario

1. Open the React application and Eureka dashboard.
2. Create an order and show it in the Orders section.
3. Show the corresponding entry in **Emails consumed by ActiveMQ**.
4. Stop the Email Service:

   ```powershell
   docker compose stop email-service
   ```

5. Create another order. The request can still be accepted because the broker stores the event.
6. Start the Email Service again:

   ```powershell
   docker compose start email-service
   ```

7. Show the queued event appearing in the frontend after consumption.
8. Use Eureka to show `ORDER-SERVICE` and `EMAIL-SERVICE` registered as `UP`.

## Design boundaries

This is a demonstration system, so it intentionally leaves out several production concerns:

- persistent database storage;
- transactional outbox for atomic database-and-message publishing;
- custom retry policy and dead-letter queue;
- authentication and authorization;
- real SMTP/email delivery;
- schema versioning for events;
- centralized monitoring and distributed tracing.

In production, the order would normally be stored in a database, an outbox would prevent database/message inconsistencies, and failed messages would be retried or routed to a dead-letter queue.

## Project structure

```text
backend/
├── discovery-server/    Eureka server
├── order-service/       REST API and ActiveMQ producer
└── email-service/       ActiveMQ consumer and email event API
frontend/                React application
compose.yaml             Docker Compose deployment
```

## Technology stack

- Java 21
- Spring Boot 4
- Spring JMS and ActiveMQ Classic
- Spring Cloud Netflix Eureka
- React and Vite
- Docker Compose
