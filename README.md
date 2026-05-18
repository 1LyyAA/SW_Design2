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

Release-каталог содержит jar, `application*.properties` и env-файлы. Скрипт release не запускает приложение и не выполняет сборку.

### Run

`start.sh` запускает готовый jar. По умолчанию используется jar из `target`, профиль `dev` и файл окружения `.env.dev`. Для production передайте `ENV_FILE=.env.prod`.

Если рядом с jar лежат `application*.properties`, скрипт подключит их как внешний Spring Boot config:

```bash
ENV_FILE=.env.dev ./start.sh
```

```bash
ENV_FILE=.env.prod JAR=release/0.0.1-SNAPSHOT/currency-rate-provider.jar ./start.sh
```

```bash
ENV_FILE=.env.prod JAR=release/0.0.1-SNAPSHOT/rate-printer.jar ./start.sh
```

## Паритет dev/prod

Для X пункта окружения разработки и production разделены конфигурацией, но используют один и тот же код, jar и start-скрипты:

- общий `application.properties` содержит настройки, не зависящие от окружения;
- `application-dev.properties` и `application-prod.properties` содержат значения, зависящие от окружения;
- `.env.dev` и `.env.prod` задают одинаковые по смыслу переменные окружения;
- окружение выбирается через `SPRING_PROFILES_ACTIVE`.

Dev-запуск:

```bash
cd currency-rate-provider
ENV_FILE=.env.dev ./start.sh
```

```bash
cd rate-printer
ENV_FILE=.env.dev ./start.sh
```

Production-запуск использует тот же jar и тот же `start.sh`, но другой env-файл:

```bash
cd currency-rate-provider
ENV_FILE=.env.prod JAR=release/0.0.1-SNAPSHOT/currency-rate-provider.jar ./start.sh
```

```bash
cd rate-printer
ENV_FILE=.env.prod JAR=release/0.0.1-SNAPSHOT/rate-printer.jar ./start.sh
```

Ключевые переменные provider:

```text
SPRING_PROFILES_ACTIVE
APP_VERSION
ZOOKEEPER_CONNECT_STRING
ZOOKEEPER_SERVICE_PATH
PROVIDER_INSTANCES
```

`PROVIDER_INSTANCES` задает пары `grpcPort:httpPort`, например `9090:8081 9091:8082`.

Ключевые переменные client:

```text
SPRING_PROFILES_ACTIVE
APP_VERSION
HTTP_PORT
ZOOKEEPER_CONNECT_STRING
ZOOKEEPER_SERVICE_PATH
```

## Запуск сервисов

Сначала соберите и запустите `currency-rate-provider`:

```bash
cd currency-rate-provider
./build.sh
./start.sh
```

В dev-профиле provider по умолчанию использует:

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

- gRPC `9090`, Actuator `8081`
- gRPC `9091`, Actuator `8082`
- gRPC `9092`, Actuator `8083`

Список инстансов задается в `currency-rate-provider/.env.dev` через `PROVIDER_INSTANCES`. В `currency-rate-provider/.env.prod` по умолчанию настроен один provider-инстанс `9090:8081`.

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

Логи рассматриваются как поток событий и пишутся стандартным Spring Boot логгером в stdout/stderr.

При запуске через `start.sh` и client, и provider-инстансы пишут логи в stdout/stderr текущего процесса. Приложение не раскладывает логи по файлам; в production их должен забирать внешний рантайм или лог-агрегатор, например `docker logs`, journald, Kubernetes logging или Prometheus/Loki stack.

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

Версия задается через переменную `APP_VERSION` в `.env.dev` или `.env.prod`, а общий `application.properties` читает ее как `app.version`:

```properties
app.version=${APP_VERSION:0.0.1-SNAPSHOT}
```

## Мониторинг

Оба сервиса публикуют Actuator/Prometheus метрики:

- `rate-printer`: `http://localhost:8080/actuator/prometheus`
- `currency-rate-provider` по умолчанию: `http://localhost:8081/actuator/prometheus`
- provider-инстансы из dev-профиля `start.sh`: `8081`, `8082`, `8083`

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
spring.profiles.default=dev
app.version=${APP_VERSION:0.0.1-SNAPSHOT}
spring.grpc.server.shutdown-grace-period=30s
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

`currency-rate-provider/src/main/resources/application-dev.properties`:

```properties
server.port=${HTTP_PORT:8081}
spring.grpc.server.port=${GRPC_PORT:9090}
zookeeper.connect-string=${ZOOKEEPER_CONNECT_STRING:localhost:2181}
zookeeper.service-path=${ZOOKEEPER_SERVICE_PATH:/services}
```

`currency-rate-provider/src/main/resources/application-prod.properties`:

```properties
server.port=${HTTP_PORT}
spring.grpc.server.port=${GRPC_PORT}
zookeeper.connect-string=${ZOOKEEPER_CONNECT_STRING}
zookeeper.service-path=${ZOOKEEPER_SERVICE_PATH}
```

При запуске provider через `start.sh` фактические `server.port` и `spring.grpc.server.port` передаются аргументами командной строки из `PROVIDER_INSTANCES`. Переменные `HTTP_PORT` и `GRPC_PORT` нужны для прямого запуска jar без `start.sh`.

`currency-rate-provider/.env.dev`:

```text
SPRING_PROFILES_ACTIVE=dev
APP_VERSION=0.0.1-SNAPSHOT
ZOOKEEPER_CONNECT_STRING=localhost:2181
ZOOKEEPER_SERVICE_PATH=/services
PROVIDER_INSTANCES="9090:8081 9091:8082 9092:8083"
```

`currency-rate-provider/.env.prod`:

```text
SPRING_PROFILES_ACTIVE=prod
APP_VERSION=0.0.1-SNAPSHOT
ZOOKEEPER_CONNECT_STRING=zookeeper:2181
ZOOKEEPER_SERVICE_PATH=/services
PROVIDER_INSTANCES="9090:8081"
```

`rate-printer/src/main/resources/application.properties`:

```properties
spring.application.name=rate-printer
spring.profiles.default=dev
app.version=${APP_VERSION:0.0.1-SNAPSHOT}
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
spring.task.scheduling.shutdown.await-termination=true
spring.task.scheduling.shutdown.await-termination-period=30s
spring.cloud.zookeeper.discovery.enabled=true
```

`rate-printer/src/main/resources/application-dev.properties`:

```properties
server.port=${HTTP_PORT:8080}
zookeeper.connect-string=${ZOOKEEPER_CONNECT_STRING:localhost:2181}
zookeeper.service-path=${ZOOKEEPER_SERVICE_PATH:/services}
spring.cloud.zookeeper.connect-string=${ZOOKEEPER_CONNECT_STRING:localhost:2181}
```

`rate-printer/src/main/resources/application-prod.properties`:

```properties
server.port=${HTTP_PORT}
zookeeper.connect-string=${ZOOKEEPER_CONNECT_STRING}
zookeeper.service-path=${ZOOKEEPER_SERVICE_PATH}
spring.cloud.zookeeper.connect-string=${ZOOKEEPER_CONNECT_STRING}
```

`rate-printer/.env.dev`:

```text
SPRING_PROFILES_ACTIVE=dev
APP_VERSION=0.0.1-SNAPSHOT
HTTP_PORT=8080
ZOOKEEPER_CONNECT_STRING=localhost:2181
ZOOKEEPER_SERVICE_PATH=/services
```

`rate-printer/.env.prod`:

```text
SPRING_PROFILES_ACTIVE=prod
APP_VERSION=0.0.1-SNAPSHOT
HTTP_PORT=8080
ZOOKEEPER_CONNECT_STRING=zookeeper:2181
ZOOKEEPER_SERVICE_PATH=/services
```

## Структура каталогов

```text
.
├── currency-rate-provider
│   ├── src/main/java/.../grpc
│   ├── src/main/java/.../service
│   ├── src/main/java/.../zookeeper
│   ├── src/main/proto/rate.proto
│   ├── src/main/resources/application-dev.properties
│   ├── src/main/resources/application-prod.properties
│   ├── src/test/java/.../pact
│   ├── .env.dev
│   ├── .env.prod
│   ├── build.sh
│   ├── release.sh
│   ├── start.sh
│   └── kill.sh
├── rate-printer
│   ├── src/main/java/.../discovery
│   ├── src/main/java/.../service
│   ├── src/main/proto/rate.proto
│   ├── src/main/resources/application-dev.properties
│   ├── src/main/resources/application-prod.properties
│   ├── src/test/java/.../pact
│   ├── .env.dev
│   ├── .env.prod
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

Посмотреть поток логов provider-инстансов:

```bash
cd currency-rate-provider
./start.sh
```

Перезапустить Prometheus после изменения targets:

```bash
docker compose restart prometheus
```

Проверить состояние контейнеров:

```bash
docker compose ps
```
