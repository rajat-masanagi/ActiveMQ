# Lab 9 — End-to-end and failure experiments

## Goal

Run the whole system, verify its contract, and observe what asynchronous queues
and in-memory storage do during failures. No application files change.

## 1. Start everything

From the repository root, start the broker:

```powershell
docker compose up -d
```

Then start Eureka, Order Service, Email Service, and the frontend in separate
terminals, in that order. Wait for each Spring application to print `Started`.

Open these pages:

- React: <http://localhost:5173>
- Eureka: <http://localhost:8761>
- ActiveMQ queues: <http://localhost:8161/admin/queues.jsp>

## 2. Verify the happy path

Start with the API directly:

```powershell
$before = @(Invoke-RestMethod http://localhost:8081/orders)

$body = @{
    customerEmail = "learner@example.com"
    product = "Notebook"
    quantity = 2
} | ConvertTo-Json

$created = Invoke-WebRequest http://localhost:8081/orders `
    -Method Post `
    -ContentType "application/json" `
    -Body $body

$after = @(Invoke-RestMethod http://localhost:8081/orders)

[pscustomobject]@{
    PostStatus = $created.StatusCode
    BeforeCount = $before.Count
    AfterCount = $after.Count
}
```

Expect status 201 and an order-count increase of one. Confirm:

- Email Service prints one simulated email with matching values.
- ActiveMQ shows `order.created` with no pending message after consumption.
- Eureka shows both services as `UP`.
- Creating an order in React produces the same result.

## 3. Verify server-side validation

HTML validation helps users, but the server must still reject bad direct calls:

```powershell
$invalidOrders = @(
    @{ customerEmail = "wrong"; product = "Notebook"; quantity = 1 },
    @{ customerEmail = "learner@example.com"; product = " "; quantity = 1 },
    @{ customerEmail = "learner@example.com"; product = "Notebook"; quantity = 0 }
)

foreach ($order in $invalidOrders) {
    try {
        Invoke-WebRequest http://localhost:8081/orders `
            -Method Post `
            -ContentType "application/json" `
            -Body ($order | ConvertTo-Json) | Out-Null
    } catch {
        $_.Exception.Response.StatusCode.value__
    }
}
```

Expect three 400 statuses. The GET count and email log must not change.

## 4. Observe queue buffering

Stop Email Service with Ctrl+C. Create an order from React or PowerShell. The POST
still succeeds because ActiveMQ accepts the event independently of the consumer.

Refresh the ActiveMQ Queues page. The pending count for `order.created` should be
one. Restart Email Service:

```powershell
Set-Location backend/email-service
mvn spring-boot:run
```

The queued event is logged and the pending count returns to zero. This is the
main benefit demonstrated by asynchronous messaging.

## 5. Observe a broker failure

Record the current GET count, then stop the broker from the repository root:

```powershell
docker compose stop activemq
```

Submit another valid POST. Expect a 500-class failure because `JmsTemplate`
cannot send. GET should retain its previous count because the service publishes
before `orders.add`.

Restart the broker before continuing:

```powershell
docker compose start activemq
```

Allow the JMS connections a few seconds to recover, then verify a new POST works.

## 6. Observe in-memory storage

Stop only Order Service with Ctrl+C and restart it:

```powershell
Set-Location backend/order-service
mvn spring-boot:run
```

`GET /orders` returns an empty array. ActiveMQ and Email Service do not rebuild the
list because this lab has no database or event replay.

## 7. Check the completed file tree

Your handwritten project should now resemble:

```text
.
|-- compose.yaml
|-- backend/
|   |-- discovery-server/
|   |   |-- pom.xml
|   |   `-- src/main/
|   |       |-- java/com/example/discovery/DiscoveryServerApplication.java
|   |       `-- resources/application.yml
|   |-- order-service/
|   |   |-- pom.xml
|   |   `-- src/main/
|   |       |-- java/com/example/order/
|   |       |   |-- CreateOrderRequest.java
|   |       |   |-- Order.java
|   |       |   |-- OrderController.java
|   |       |   |-- OrderService.java
|   |       |   `-- OrderServiceApplication.java
|   |       `-- resources/application.yml
|   |-- email-service/
|   |   |-- pom.xml
|   |   `-- src/main/
|   |       |-- java/com/example/email/
|   |       |   |-- EmailServiceApplication.java
|   |       |   `-- OrderEmailListener.java
|   |       `-- resources/application.yml
`-- frontend/
    |-- index.html
    |-- package.json
    |-- package-lock.json
    `-- src/
        |-- App.jsx
        |-- main.jsx
        `-- styles.css
```

Build everything once without starting it:

```powershell
mvn -f backend/discovery-server/pom.xml package
mvn -f backend/order-service/pom.xml package
mvn -f backend/email-service/pom.xml package
npm.cmd --prefix frontend run build
```

## What this project deliberately does not solve

- A crash between publishing and storing can make the email and in-memory list
  disagree. A database plus transactional outbox is a common production design.
- A failed listener has no custom retry or dead-letter policy here.
- Restarting Order Service loses data because its list is process memory.
- Eureka is visible but is not used for routing. A gateway or discovery-aware
  HTTP client would give it an active routing role.
- Console logging proves message consumption but is not real email delivery.

These are boundaries, not hidden bugs. Adding all of them would obscure the two
ideas this project teaches: a small REST service and an asynchronous consumer.

## Checkpoint

You have demonstrated the happy path, all three validation failures, consumer
buffering, broker failure behavior, frontend error handling, Eureka registration,
and loss of in-memory data after restart.

## Troubleshooting

- Run `docker compose ps` and check all four application ports before debugging
  Java code.
- Read the first `Caused by:` line in a Spring error; later lines are often a
  consequence of the same problem.
- Use the ActiveMQ pending and consumer counts to decide whether a message was
  never published or simply not consumed.
- If a restart behaves unexpectedly, stop duplicate Java processes and confirm
  each port has only one listener with `Get-NetTCPConnection -State Listen`.
