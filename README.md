# Orders to Email: a handwritten microservices lab

This repository is a workbook, not a finished application. You will type every
application file yourself by following the labs in [`labs`](labs). The project is
small on purpose: each file introduces one idea and remains short enough to
understand in one sitting.

## What you will build

```text
React :5173 --REST--> Order Service :8081 --JMS--> ActiveMQ :61616
                         |                         |
                         |                         v
                         +--> Eureka :8761 <-- Email Service :8082
                                                    |
                                                    v
                                            simulated email log
```

The Order Service has the only two business endpoints:

- `POST /orders` creates an in-memory order and publishes an event.
- `GET /orders` lists the orders created since the service started.

The Email Service has no business endpoint. It listens to the `order.created`
queue and prints a simulated email. Eureka shows which services are running;
ActiveMQ, rather than Eureka, carries the order event.

## Learning goals

By the end you will be able to explain:

- how a Spring Boot application starts and how constructor injection works;
- how a controller, service, record, and validation annotations fit together;
- how `JmsTemplate` publishes and `@JmsListener` consumes asynchronously;
- what a queue does when a consumer is temporarily offline;
- what Eureka registration does—and what it does not do in this project;
- how a small React form calls a REST API with `fetch`;
- why in-memory storage and publish-then-store are learning simplifications.

## Versions and ports

| Part | Version | Port |
| --- | --- | --- |
| Java | 21 | — |
| Spring Boot | 4.0.8 | — |
| Spring Cloud | 2025.1.3 | — |
| Eureka server | managed by Spring Cloud | 8761 |
| Order Service | Spring Boot | 8081 |
| Email Service | Spring Boot | 8082 |
| ActiveMQ Classic | 6.3.0 | 61616 (JMS), 8161 (console) |
| React | 19.2 | 5173 |
| Vite | 8.1 | 5173 |

The Spring versions follow the official
[Spring Cloud compatibility table](https://spring.io/projects/spring-cloud/).
Spring Boot documents both the
[Web MVC and ActiveMQ starters](https://docs.spring.io/spring-boot/4.0/reference/using/build-systems.html).

## Lab checklist

- [ ] [Lab 1 — Prerequisites and mental model](labs/01-prerequisites.md)
- [ ] [Lab 2 — ActiveMQ with Docker](labs/02-activemq.md)
- [ ] [Lab 3 — Eureka discovery server](labs/03-eureka-server.md)
- [ ] [Lab 4 — Order REST API](labs/04-order-rest-api.md)
- [ ] [Lab 5 — Publish an order event](labs/05-order-publisher.md)
- [ ] [Lab 6 — Consume and log an email](labs/06-email-consumer.md)
- [ ] [Lab 7 — Observe Eureka registration](labs/07-eureka-registration.md)
- [ ] [Lab 8 — React frontend](labs/08-react-frontend.md)
- [ ] [Lab 9 — End-to-end and failure experiments](labs/09-end-to-end.md)

## Normal startup order

Use a separate PowerShell terminal for each long-running process:

1. `docker compose up -d`
2. `cd backend/discovery-server; mvn spring-boot:run`
3. `cd backend/order-service; mvn spring-boot:run`
4. `cd backend/email-service; mvn spring-boot:run`
5. `cd frontend; npm.cmd run dev`

Do not copy a complete solution from elsewhere. Type a block, run its checkpoint,
and only then continue. Compiler errors are part of the lab: compare the package,
file name, imports, and punctuation with the guide before changing anything.

## Deliberate boundaries

There is no database, gateway, authentication, real SMTP account, shared event
library, retry policy, dead-letter queue, or transactional outbox. The final lab
explains where those ideas would fit without making this learning project larger.
