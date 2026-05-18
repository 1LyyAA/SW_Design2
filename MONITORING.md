# Мониторинг

Проект отдает метрики Spring Boot Actuator в формате Prometheus и автоматически подключает готовый Grafana dashboard.

## Endpoints

- `rate-printer`: `http://localhost:8080/actuator/prometheus`
- `currency-rate-provider` по умолчанию: `http://localhost:8081/actuator/prometheus`
- provider-инстансы из `currency-rate-provider/start.sh`:
  - gRPC `9090`, метрики `8081`
  - gRPC `9091`, метрики `8082`
  - gRPC `9092`, метрики `8083`

## Запуск

Запустите инфраструктуру из корня репозитория:

```bash
docker compose up -d
```

После этого запустите сервисы обычным способом. Grafana будет доступна по адресу:

```text
http://localhost:3000
```

Логин и пароль по умолчанию:

```text
admin / admin
```

Dashboard находится в папке `Spring` и называется `Spring Services JVM Metrics`.

Prometheus доступен по адресу:

```text
http://localhost:9095
```

## Добавление новых клиентов или provider-ов

У каждого Java-процесса должен быть свой HTTP-порт для Actuator-метрик. Например:

```bash
java -jar target/currency-rate-provider-0.0.1-SNAPSHOT.jar --spring.grpc.server.port=9093 --server.port=8084
```

Затем добавьте `host.docker.internal:8084` в `infra/prometheus/prometheus.yml` внутри scrape job `spring-services`. Labels нужны, чтобы Grafana корректно группировала сервисы:

```yaml
- targets:
    - host.docker.internal:8084
  labels:
    application: currency-rate-provider
    role: provider
```

После изменения targets перезапустите Prometheus:

```bash
docker compose restart prometheus
```
