# Lab 1 — Prerequisites and mental model

## Goal

Confirm the local tools and understand the path one order follows. This lab does
not create application code.

## 1. Check the tools

From the repository root, run:

```powershell
java -version
mvn -version
node --version
npm.cmd --version
docker --version
docker compose version
```

You should see Java 21, Maven 3.9 or newer, Node 20.19 or newer, and working
Docker and Docker Compose commands. Exact patch versions can be newer.

## 2. Understand the flow

When a user submits the React form:

1. React sends JSON to `POST http://localhost:8081/orders`.
2. Order Service validates the JSON and creates an ID.
3. Order Service sends a map to the ActiveMQ queue `order.created`.
4. Only after the broker accepts the message does Order Service add the order to
   its in-memory list and return HTTP 201.
5. Email Service receives the queued map on another thread and writes a simulated
   email to its log.
6. A later `GET /orders` reads the in-memory list.

The HTTP response does not wait for Email Service. That is the important
asynchronous behavior in this project.

## 3. Know the folders

The four empty folders are intentional:

- `backend/discovery-server` will contain the Eureka server.
- `backend/order-service` will contain the REST API and JMS publisher.
- `backend/email-service` will contain the JMS listener.
- `frontend` will contain the React app.

The guides will tell you exactly when to create each file. Do not use Spring
Initializr or `npm create vite`; the point is to see the few files actually needed.

## Checkpoint

You can explain why React talks only to Order Service and why Order Service does
not call Email Service over HTTP.

## Troubleshooting

- If `java` or `mvn` is not recognized, reopen PowerShell after fixing `PATH`.
- The labs use `npm.cmd` because Windows PowerShell may block the `npm.ps1`
  wrapper under its execution policy. This avoids changing that security policy.
- If Docker commands work but containers cannot start, launch Docker Desktop and
  wait until its engine reports that it is running.
- If Node is too old, install a current LTS release before reaching Lab 8.
