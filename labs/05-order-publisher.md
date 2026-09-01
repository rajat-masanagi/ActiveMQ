# Lab 5 — Publish an order event

## Goal

Change Order Service so it sends a JMS map to ActiveMQ before storing and
returning an order.

## 1. Add the ActiveMQ dependency

In `backend/order-service/pom.xml`, add this dependency inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-activemq</artifactId>
</dependency>
```

The starter creates a JMS connection factory and `JmsTemplate` from application
configuration. Your code does not open or close broker connections itself.

## 2. Add broker configuration

Extend the existing `spring` section in
`backend/order-service/src/main/resources/application.yml` so the complete file is:

```yaml
server:
  port: 8081

spring:
  application:
    name: order-service
  activemq:
    broker-url: tcp://localhost:61616
    user: student
    password: student

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

YAML indentation represents nesting. `activemq` and `application` are both
children of `spring`.

## 3. Replace the service class

Replace all of `backend/order-service/src/main/java/com/example/order/OrderService.java`:

```java
package com.example.order;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final String ORDER_CREATED_QUEUE = "order.created";

    private final List<Order> orders = new CopyOnWriteArrayList<>();
    private final JmsTemplate jmsTemplate;

    public OrderService(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public Order create(CreateOrderRequest request) {
        Order order = new Order(
                UUID.randomUUID(),
                request.customerEmail(),
                request.product(),
                request.quantity()
        );

        Map<String, Object> event = Map.of(
                "orderId", order.id().toString(),
                "customerEmail", order.customerEmail(),
                "product", order.product(),
                "quantity", order.quantity()
        );

        jmsTemplate.convertAndSend(ORDER_CREATED_QUEUE, event);
        orders.add(order);
        return order;
    }

    public List<Order> findAll() {
        return List.copyOf(orders);
    }
}
```

Spring injects its configured `JmsTemplate` through the constructor. The default
JMS converter turns `Map` into a standard JMS `MapMessage`; no custom JSON class
or shared Java library is needed. The UUID is converted to text because JMS maps
support a limited set of value types.

Publishing happens before `orders.add`. Therefore a broker failure makes POST
fail and the order does not appear in this process's list. This ordering is easy
to reason about, but it is not a replacement for a production transactional
outbox.

## 4. Run and inspect the queue

Make sure ActiveMQ and Eureka are running. Restart Order Service so Maven loads
the new dependency:

```powershell
Set-Location backend/order-service
mvn spring-boot:run
```

Create an order as in Lab 4, then open the Queues page at
<http://localhost:8161/admin/queues.jsp>. A queue named `order.created` should
exist. Because Email Service is not running yet, its pending message count should
increase.

## Checkpoint

A successful POST still returns 201 and the `order.created` queue contains the
message. GET contains the new order.

## Troubleshooting

- `Connection refused` means the broker is not listening on port 61616; run
  `docker compose ps` from the repository root.
- An authentication error usually means the application and Compose credentials
  differ or the broker container was not recreated after editing credentials.
- If `JmsTemplate` cannot be imported, ensure the new dependency is inside the
  POM's `<dependencies>` element and restart Maven.
