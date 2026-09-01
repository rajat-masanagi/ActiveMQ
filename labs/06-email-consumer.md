# Lab 6 — Consume and log an email

## Goal

Build Email Service. It has no business controller: it receives `order.created`
messages and logs a simulated email.

## 1. Create the directories

From the repository root:

```powershell
New-Item -ItemType Directory -Force backend/email-service/src/main/java/com/example/email
New-Item -ItemType Directory -Force backend/email-service/src/main/resources
Remove-Item backend/email-service/.gitkeep
```

## 2. Write the Maven build

Create `backend/email-service/pom.xml`:

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
    <artifactId>email-service</artifactId>
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
            <artifactId>spring-boot-starter-activemq</artifactId>
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

`webmvc` gives this process an embedded server on port 8082 so Eureka can
describe a live service instance. We deliberately create no controller, so it
adds no business endpoints. The other two dependencies provide JMS and Eureka.

## 3. Configure the service

Create `backend/email-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8082

spring:
  application:
    name: email-service
  activemq:
    broker-url: tcp://localhost:61616
    user: student
    password: student

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

The queue name is not configuration here; it appears once, on the listener, to
keep the lab small.

## 4. Write the main class

Create `backend/email-service/src/main/java/com/example/email/EmailServiceApplication.java`:

```java
package com.example.email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@EnableJms
@SpringBootApplication
public class EmailServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailServiceApplication.class, args);
    }
}
```

`@EnableJms` searches Spring components for `@JmsListener` methods and creates
listener containers for them. `@SpringBootApplication` finds the listener class
because it is in the same package.

## 5. Write the listener

Create `backend/email-service/src/main/java/com/example/email/OrderEmailListener.java`:

```java
package com.example.email;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEmailListener {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEmailListener.class);

    @JmsListener(destination = "order.created")
    public void sendEmail(Map<String, Object> event) {
        log.info("""

                ===== SIMULATED EMAIL =====
                To: {}
                Subject: Order {} received
                Body: Thanks for ordering {} x {}.
                ===========================
                """,
                event.get("customerEmail"),
                event.get("orderId"),
                event.get("quantity"),
                event.get("product"));
    }
}
```

`@Component` creates the listener object. `@JmsListener` subscribes its method to
the queue. Spring converts the JMS `MapMessage` back into a Java map before
calling the method. The logger uses `{}` placeholders, keeping values separate
from the format string.

## 6. Run the consumer

Keep ActiveMQ, Eureka, and Order Service running. In a new terminal:

```powershell
Set-Location backend/email-service
mvn spring-boot:run
```

Any message left by Lab 5 should be consumed and printed immediately. Create one
more order and watch a second simulated email appear. Refresh ActiveMQ's Queues
page; the pending count should return to zero.

## Checkpoint

Each valid POST produces exactly one simulated email log containing the same
email, order ID, product, and quantity as the order event.

## Troubleshooting

- If the listener starts but sees nothing, confirm publisher and consumer both
  spell `order.created` exactly.
- If Spring cannot convert the payload, confirm the publisher sends the `event`
  map rather than the `Order` record.
- If port 8082 is busy, stop the other process occupying it rather than changing
  the documented port.
