# Lab 2 — ActiveMQ with Docker

## Goal

Run one ActiveMQ Classic broker. Port `61616` carries JMS messages and port
`8161` serves the browser console.

## 1. Write the Compose file

Create `compose.yaml` in the repository root:

```yaml
services:
  activemq:
    image: apache/activemq:6.3.0
    container_name: learning-activemq
    ports:
      - "61616:61616"
      - "8161:8161"
    environment:
      ACTIVEMQ_CONNECTION_USER: student
      ACTIVEMQ_CONNECTION_PASSWORD: student
      ACTIVEMQ_WEB_USER: student
      ACTIVEMQ_WEB_PASSWORD: student
```

`services` is the Compose list of containers. The two `ports` entries make the
broker and its console reachable from Windows. Explicit environment variables
give both the applications and browser console the predictable credentials
`student` / `student`.

The official image documents the connector and web-console ports and these
[credential variables](https://github.com/apache/activemq/tree/main/assembly/src/docker).

## 2. Start and inspect the broker

```powershell
docker compose up -d
docker compose ps
Test-NetConnection localhost -Port 61616
```

Open <http://localhost:8161/admin/> and sign in with `student` / `student`.
The Queues page is initially empty.

Useful commands are:

```powershell
docker compose logs activemq
docker compose stop activemq
docker compose start activemq
docker compose down
```

`down` removes the container, so use `stop` during later failure experiments when
you want to restart the same broker.

## Checkpoint

`docker compose ps` reports the container as running, TCP port 61616 succeeds,
and the ActiveMQ console opens.

## Troubleshooting

- If a port is already in use, stop the other program rather than changing the
  lab ports; all later configuration expects these values.
- If the browser initially refuses the connection, inspect
  `docker compose logs activemq` and allow the broker a few more seconds.
- If login fails, recreate the container after checking the four environment
  variable names: `docker compose down`, then `docker compose up -d`.
