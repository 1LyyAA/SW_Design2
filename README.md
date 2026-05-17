# Currency Rate Microservices

Учебный проект с двумя Spring Boot сервисами, которые обмениваются данными по gRPC, используют ZooKeeper для discovery, Pact для contract testing и Prometheus/Grafana для мониторинга.

## Состав проекта

- `currency-rate-provider` - gRPC-сервер, который генерирует курс валютной пары `USDRUB`.
- `rate-printer` - клиент, который раз в 5 секунд находит доступный provider через ZooKeeper, отправляет gRPC-запрос и печатает курс.
- `docker-compose.yml` - локальная инфраструктура: ZooKeeper, Pact Broker, PostgreSQL, Prometheus, Grafana.
- `start.bash` - полный Pact flow: поднять инфраструктуру, сгенерировать pact, опубликовать его и проверить provider.
- `MONITORING.md` - отдельная инструкция по Prometheus/Grafana.
- `infra/` - конфиги Prometheus, Grafana datasource и dashboard.

## Архитектура

```text
rate-printer
    |
    | reads provider addresses from ZooKeeper
    v
ZooKeeper (/services)
    ^
    | registers ephemeral provider node
    |
currency-rate-provider

rate-printer -- gRPC GetRate(pair) --> currency-rate-provider
```

При старте `currency-rate-provider` регистрирует свой адрес в ZooKeeper по пути `/services`. Клиент `rate-printer` читает список provider-инстансов, случайно выбирает один адрес и вызывает gRPC-метод `GetRate`.

## gRPC API

Контракт описан в `rate.proto`:

```proto
service CurrencyRateService {
  rpc GetRate (RateRequest) returns (RateResponse);
}

message RateRequest {
  string pair = 1;
}

message RateResponse {
  string pair = 1;
  double rate = 2;
  int64 timestamp = 3;
}
```

Сейчас поддерживается валютная пара `USDRUB`. Для остальных пар provider возвращает gRPC-ошибку `INVALID_ARGUMENT`.

## Технологии

- Java 21
- Spring Boot
- Spring gRPC
- Protocol Buffers
- Apache ZooKeeper и Apache Curator
- Pact JVM и Pact Broker
- PostgreSQL для Pact Broker
- Spring Boot Actuator
- Micrometer Prometheus Registry
- Prometheus
- Grafana
- Docker Compose
- Maven

## Требования

- JDK 21
- Maven или Maven Wrapper из модулей проекта
- Docker и Docker Compose
- Bash для `start.bash`, `build.sh`, `release.sh`, `start.sh` и `kill.sh`

На Windows можно запускать сборку через `mvnw.cmd`, на Linux/macOS через `./mvnw`.

## Быстрый старт

Из корня проекта поднимите инфраструктуру:

```bash
docker compose up -d
```

Будут доступны:

- ZooKeeper: `localhost:2181`
- Pact Broker: `http://localhost:9292`
- Prometheus: `http://localhost:9095`
- Grafana: `http://localhost:3000`
- PostgreSQL для Pact Broker: внешний порт `5433`

Остановить инфраструктуру:

```bash
docker compose down
```

Полностью удалить данные контейнеров:

```bash
docker compose down -v
```

## Сборка, релиз и выполнение

В проекте стадии разделены:

- `build.sh` - только собирает jar-артефакт;
- `release.sh` - только готовит release-каталог из уже собранного jar и конфигурации;
- `start.sh` - только запускает уже готовый jar, без сборки.

### Build

Собрать provider:

```bash
cd currency-rate-provider
./build.sh
```

Собрать client:

```bash
cd rate-printer
./build.sh
```

### Release

Подготовить release provider:

```bash
cd currency-rate-provider
./release.sh 0.0.1-SNAPSHOT
```

Подготовить release client:

```bash
cd rate-printer
./release.sh 0.0.1-SNAPSHOT
```

Release-каталог содержит jar и копию `application.properties`. Скрипт release не запускает приложение и не выполняет сборку.

### Run

`start.sh` запускает готовый jar. По умолчанию используется jar из `target`, но можно передать release-артефакт через переменную `JAR`. Если рядом с jar лежит `application.properties`, скрипт подключит его как внешний Spring Boot config:

```bash
JAR=release/0.0.1-SNAPSHOT/currency-rate-provider.jar ./start.sh
```

```bash
JAR=release/0.0.1-SNAPSHOT/rate-printer.jar ./start.sh
```

## Запуск сервисов

Сначала соберите и запустите `currency-rate-provider`:

```bash
cd currency-rate-provider
./build.sh
./start.sh
```

По умолчанию provider использует:

- gRPC-порт: `9090`
- HTTP/Actuator-порт: `8081`
- ZooKeeper: `localhost:2181`
- service path в ZooKeeper: `/services`

Затем соберите и запустите `rate-printer`:

```bash
cd rate-printer
./build.sh
./start.sh
```

Клиент каждые 5 секунд выбирает provider и печатает результат:

```text
Request sent to 127.0.0.1:9090
USD/RUB: 91.23 (timestamp: 2026-05-17 18:00:00)
```

## Несколько provider-инстансов

Для запуска трех provider-инстансов используйте готовый скрипт:

```bash
cd currency-rate-provider
./start.sh
```

Скрипт запускает уже собранный jar. Если jar отсутствует, он завершится с ошибкой и попросит сначала выполнить `./build.sh`.

- gRPC `9090`, Actuator `8081`, лог `producer-9090.log`
- gRPC `9091`, Actuator `8082`, лог `producer-9091.log`
- gRPC `9092`, Actuator `8083`, лог `producer-9092.log`

Остановить provider-процессы:

```bash
cd currency-rate-provider
./kill.sh
```

Остановить client-процесс:

```bash
cd rate-printer
./kill.sh
```

## Одноразовость и graceful shutdown

Приложения корректно обрабатывают `SIGTERM` и завершаются с ожиданием активной работы:

- `currency-rate-provider` использует `spring.grpc.server.shutdown-grace-period=30s`, поэтому gRPC-сервер при остановке ждет завершения текущих RPC до 30 секунд.
- Оба сервиса используют `server.shutdown=graceful` и `spring.lifecycle.timeout-per-shutdown-phase=30s`.
- `rate-printer` использует `spring.task.scheduling.shutdown.await-termination=true`, поэтому при остановке Spring ждет завершения текущей scheduled-задачи до 30 секунд.
- `kill.sh` сначала отправляет `SIGTERM`, ждет graceful shutdown до 35 секунд и только после таймаута отправляет `SIGKILL`.

Ключевой сценарий для IX пункта:

```bash
cd currency-rate-provider
./kill.sh
```

Скрипт не считает процесс остановленным сразу после сигнала, а дожидается фактического завершения Java-процессов.

## Логгирование

Логи пишутся стандартным Spring Boot логгером.

При запуске через `start.sh` логи client выводятся в терминал, а stdout/stderr каждого provider-инстанса пишутся в файлы `producer-9090.log`, `producer-9091.log`, `producer-9092.log`.

### Сервер

`currency-rate-provider` логирует gRPC-вызов `GetRate`:

- входящий запрос: валютная пара `pair`;
- успешный ответ: `pair`, `rate`, `timestamp`;
- ошибочный ответ: `pair`, gRPC-статус и сообщение ошибки.

Примеры:

```text
Received rate request: pair=USDRUB
Sent rate response: pair=USDRUB, rate=91.23, timestamp=1710000000000
Sent rate error response: pair=EURRUB, status=INVALID_ARGUMENT, message=Unsupported currency pair: EURRUB
```

### Клиент

`rate-printer` логирует gRPC-запрос и ответ:

- отправка запроса: `target`, `pair`;
- получение ответа: `target`, `pair`, `rate`, `timestamp`.

Примеры:

```text
Sending rate request: target=127.0.0.1:9090, pair=USDRUB
Received rate response: target=127.0.0.1:9090, pair=USDRUB, rate=91.23, timestamp=1710000000000
```

### Версия приложения

Оба сервиса логируют имя и версию при старте:

```text
Application started: name=rate-printer, version=0.0.1-SNAPSHOT
```

Версия задается в `application.properties` каждого сервиса:

```properties
app.version=0.0.1-SNAPSHOT
```

## Мониторинг

Оба сервиса публикуют Actuator/Prometheus метрики:

- `rate-printer`: `http://localhost:8080/actuator/prometheus`
- `currency-rate-provider` по умолчанию: `http://localhost:8081/actuator/prometheus`
- provider-инстансы из `start.sh`: `8081`, `8082`, `8083`

Prometheus читает цели из `infra/prometheus/prometheus.yml`.

Grafana доступна по адресу:

```text
http://localhost:3000
```

Логин и пароль:

```text
admin / admin
```

Dashboard находится в папке `Spring` и называется `Spring Services JVM Metrics`.

Подробности есть в `MONITORING.md`.

## Pact flow

Полный сценарий contract testing запускается из корня проекта:

```bash
./start.bash
```

Скрипт выполняет:

1. Запускает инфраструктуру через Docker Compose.
2. Запускает consumer Pact-тест в `rate-printer`.
3. Публикует pact в Pact Broker.
4. Запускает provider verification в `currency-rate-provider`.
5. Публикует результат проверки в Pact Broker.

После выполнения Pact Broker доступен по адресу:

```text
http://localhost:9292
```

## Тесты и сборка

Собрать provider без запуска приложения:

```bash
cd currency-rate-provider
./build.sh
```

Собрать client без запуска приложения:

```bash
cd rate-printer
./build.sh
```

Запустить обычные тесты:

```bash
cd currency-rate-provider
./mvnw test
```

```bash
cd rate-printer
./mvnw test
```

Запустить consumer Pact-тест:

```bash
cd rate-printer
./mvnw -Dtest=RatePrinterGrpcConsumerPactTest test
```

Запустить provider verification:

```bash
cd currency-rate-provider
./mvnw verify -Djava.net.preferIPv4Stack=true -Dpact.verifier.publishResults=true -Dpact.provider.version=0.0.1-SNAPSHOT
```

## Основные настройки

`currency-rate-provider/src/main/resources/application.properties`:

```properties
spring.application.name=currency-rate-provider
app.version=0.0.1-SNAPSHOT
server.port=8081
spring.grpc.server.port=9090
spring.grpc.server.shutdown-grace-period=30s
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
zookeeper.connect-string=localhost:2181
zookeeper.service-path=/services
```

`rate-printer/src/main/resources/application.properties`:

```properties
spring.application.name=rate-printer
app.version=0.0.1-SNAPSHOT
server.port=8080
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
spring.task.scheduling.shutdown.await-termination=true
spring.task.scheduling.shutdown.await-termination-period=30s
spring.cloud.zookeeper.connect-string=localhost:2181
spring.cloud.zookeeper.discovery.enabled=true
```

## Структура каталогов

```text
.
├── currency-rate-provider
│   ├── src/main/java/.../grpc
│   ├── src/main/java/.../service
│   ├── src/main/java/.../zookeeper
│   ├── src/main/proto/rate.proto
│   ├── src/test/java/.../pact
│   ├── build.sh
│   ├── release.sh
│   ├── start.sh
│   └── kill.sh
├── rate-printer
│   ├── src/main/java/.../discovery
│   ├── src/main/java/.../service
│   ├── src/main/proto/rate.proto
│   ├── src/test/java/.../pact
│   ├── build.sh
│   ├── release.sh
│   ├── start.sh
│   └── kill.sh
├── infra
│   ├── prometheus
│   └── grafana
├── docker-compose.yml
├── MONITORING.md
└── start.bash
```

## Полезные команды

Посмотреть логи provider-инстанса:

```bash
cd currency-rate-provider
tail -f producer-9090.log
```

Перезапустить Prometheus после изменения targets:

```bash
docker compose restart prometheus
```

Проверить состояние контейнеров:

```bash
docker compose ps
```
