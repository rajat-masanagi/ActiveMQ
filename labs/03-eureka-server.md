# Lab 3 — Eureka discovery server

## Goal

Handwrite a minimal Eureka server and open its registry dashboard.

## 1. Create the directories

From the repository root:

```powershell
New-Item -ItemType Directory -Force backend/discovery-server/src/main/java/com/example/discovery
New-Item -ItemType Directory -Force backend/discovery-server/src/main/resources
Remove-Item backend/discovery-server/.gitkeep
```

Java requires the folder path to match the package name
`com.example.discovery`.

## 2. Write the Maven build

Create `backend/discovery-server/pom.xml`:

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
    <artifactId>discovery-server</artifactId>
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
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
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

The Boot parent manages Java build defaults. The Spring Cloud BOM manages
compatible Cloud dependency versions. `webmvc` supplies the embedded web server,
and the Eureka server starter supplies the registry.

## 3. Write the application class

Create
`backend/discovery-server/src/main/java/com/example/discovery/DiscoveryServerApplication.java`:

```java
package com.example.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
```

`@SpringBootApplication` enables configuration, auto-configuration, and component
scanning. `@EnableEurekaServer` turns this Boot application into a registry.

## 4. Configure the server

Create `backend/discovery-server/src/main/resources/application.yml`:

```yaml
server:
  port: 8761

spring:
  application:
    name: discovery-server

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

The server does not register with or download a registry from itself.

## 5. Run it

```powershell
Set-Location backend/discovery-server
mvn spring-boot:run
```

Wait for `Started DiscoveryServerApplication`, then open
<http://localhost:8761>. The instances section is empty until Labs 4 and 6.
Stop the process with Ctrl+C when needed.

## Checkpoint

The Eureka dashboard loads on port 8761 and Maven reports no compilation errors.

## Troubleshooting

- An XML error near the start of the POM often means a missing closing tag.
- If Java cannot find the main class, compare the package declaration with the
  directory path and file name exactly.
- An informational `NoProviderFoundException` about Bean Validation is harmless
  here; the registry has no request model to validate.
- If port 8761 is busy, find and stop the existing process with
  `Get-NetTCPConnection -LocalPort 8761`.
