# Lab 7 — Observe Eureka registration

## Goal

Observe both business services registering with Eureka and separate service
discovery from message delivery. No files change in this lab.

## 1. Start the three Spring applications

Use three PowerShell terminals from the repository root:

```powershell
Set-Location backend/discovery-server
mvn spring-boot:run
```

```powershell
Set-Location backend/order-service
mvn spring-boot:run
```

```powershell
Set-Location backend/email-service
mvn spring-boot:run
```

The Eureka client starter is the dependency that activates registration. In each
service, `spring.application.name` supplies its registry name and
`eureka.client.serviceUrl.defaultZone` tells it where the server lives. No
`@EnableEurekaClient` annotation is needed.

Registration and heartbeats are periodic, so the dashboard can take roughly 30
seconds to reflect a start or stop.

## 2. Inspect the registry

Open <http://localhost:8761>. Under **Instances currently registered with
Eureka**, expect:

```text
Application       Status
EMAIL-SERVICE     UP
ORDER-SERVICE     UP
```

You can also inspect Eureka's machine-readable registry:

```powershell
Invoke-WebRequest http://localhost:8761/eureka/apps `
    -Headers @{ Accept = "application/xml" } |
    Select-Object -ExpandProperty Content
```

The XML includes each application name, host, port, and status.

## 3. Identify what Eureka is doing

Eureka knows where Order Service and Email Service are running, but neither
service asks Eureka for the other one in this project. Their data path is:

```text
Order Service --> ActiveMQ queue --> Email Service
```

This means an order event still reaches Email Service if Eureka is temporarily
stopped, provided ActiveMQ and both services remain running. In a larger system,
a gateway or an HTTP client could use Eureka to locate Order Service, but that is
outside this lab.

## Checkpoint

Both applications appear as `UP`, and you can explain why stopping Eureka does
not remove or transport any `order.created` message.

## Troubleshooting

- Wait for the next heartbeat before diagnosing a missing or stale instance.
- Check that the Eureka server started first and is reachable on port 8761.
- Check the exact camel-case key `defaultZone`; `default-zone` does not work for
  this Eureka map property.
- If an instance remains after it stops, refresh later; expiry is not immediate.
