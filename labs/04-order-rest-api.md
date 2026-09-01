# Lab 4 — Order REST API

## Goal

Build the Order Service with two endpoints, request validation, a small service
layer, and thread-safe in-memory storage. Messaging is added in the next lab.

## 1. Create the directories

From the repository root:

```powershell
New-Item -ItemType Directory -Force backend/order-service/src/main/java/com/example/order
New-Item -ItemType Directory -Force backend/order-service/src/main/resources
Remove-Item backend/order-service/.gitkeep
```

## 2. Write the Maven build

Create `backend/order-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.8</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>order-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2025.1.3</spring-cloud.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

`webmvc` provides JSON REST support, `validation` provides Jakarta validation
annotations, and the Eureka client registers this service when it starts. The
Cloud BOM makes the Eureka dependency compatible with Boot.

## 3. Configure the service

Create `backend/order-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: order-service

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

`spring.application.name` becomes the Eureka service ID. `defaultZone` is
case-sensitive because Eureka treats it as a map key.

## 4. Write the main class

Create `backend/order-service/src/main/java/com/example/order/OrderServiceApplication.java`:

```java
package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

No Eureka annotation is required. Having the Eureka client dependency and
configuration on the classpath enables registration automatically.

## 5. Write the request and response records

Create `backend/order-service/src/main/java/com/example/order/CreateOrderRequest.java`:

```java
package com.example.order;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotBlank @Email String customerEmail,
        @NotBlank String product,
        @Positive int quantity
) {
}
```

Create `backend/order-service/src/main/java/com/example/order/Order.java`:

```java
package com.example.order;

import java.util.UUID;

public record Order(
        UUID id,
        String customerEmail,
        String product,
        int quantity
) {
}
```

A Java record is a compact immutable data carrier. `@NotBlank` rejects null,
empty, and whitespace-only text. `@Email` checks the email shape, while
`@Positive` rejects zero and negative quantities.

## 6. Write the service layer

Create `backend/order-service/src/main/java/com/example/order/OrderService.java`:

```java
package com.example.order;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final List<Order> orders = new CopyOnWriteArrayList<>();

    public Order create(CreateOrderRequest request) {
        Order order = new Order(
                UUID.randomUUID(),
                request.customerEmail(),
                request.product(),
                request.quantity()
        );
        orders.add(order);
        return order;
    }

    public List<Order> findAll() {
        return List.copyOf(orders);
    }
}
```

`@Service` makes the class a Spring-managed component. A
`CopyOnWriteArrayList` is safe when several HTTP threads use it. `List.copyOf`
returns a read-only snapshot rather than exposing the stored list.

## 7. Write the controller

Create `backend/order-service/src/main/java/com/example/order/OrderController.java`:

```java
package com.example.order;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @GetMapping
    public List<Order> findAll() {
        return orderService.findAll();
    }
}
```

`@RestController` serializes return values as JSON. `@RequestMapping` supplies
the shared path. `@PostMapping` and `@GetMapping` create exactly two business
routes. `@RequestBody` reads JSON, `@Valid` applies the record constraints, and
`@ResponseStatus` changes a successful POST from 200 to 201. Constructor
injection supplies the one `OrderService`. `@CrossOrigin` permits only the local
Vite origin.

## 8. Run and call the API

Keep Eureka running, then use a new PowerShell terminal:

```powershell
Set-Location backend/order-service
mvn spring-boot:run
```

In another terminal:

```powershell
Invoke-RestMethod http://localhost:8081/orders

$body = @{
    customerEmail = "learner@example.com"
    product = "Notebook"
    quantity = 2
} | ConvertTo-Json

Invoke-RestMethod http://localhost:8081/orders `
    -Method Post `
    -ContentType "application/json" `
    -Body $body

Invoke-RestMethod http://localhost:8081/orders
```

The first response is an empty array, POST returns an order with a UUID, and the
last response contains that order.

Try invalid input:

```powershell
$bad = @{ customerEmail = "not-an-email"; product = ""; quantity = 0 } | ConvertTo-Json
try {
    Invoke-WebRequest http://localhost:8081/orders -Method Post -ContentType "application/json" -Body $bad
} catch {
    $_.Exception.Response.StatusCode.value__
}
```

The displayed status is 400.

## Checkpoint

GET and POST behave as described, invalid input returns 400, and Eureka eventually
shows `ORDER-SERVICE`. Restarting Order Service clears its list.

## Troubleshooting

- If port 8081 is busy, stop the other process using it; do not change the port.
- If validation annotations do nothing, check both the validation dependency and
  `@Valid` on the controller parameter.
- Eureka connection warnings are expected if the discovery server is stopped;
  the REST API can still start and work.
