# Currency Rate Microservices

Учебный проект с двумя Spring Boot сервисами, которые обмениваются данными по gRPC и используют ZooKeeper для discovery. Проект также содержит настройку Pact Broker и Pact-тесты для проверки контракта между consumer и provider.

## Состав проекта

- `currency-rate-provider` - gRPC provider, который генерирует курс валютной пары `USDRUB`.
- `rate-printer` - consumer, который раз в 5 секунд находит доступный provider через ZooKeeper, вызывает gRPC-метод и печатает курс в консоль.
- `docker-compose.yml` - инфраструктура для локального запуска ZooKeeper, PostgreSQL и Pact Broker.
- `start.bash` - сценарий для полного Pact flow: поднять инфраструктуру, сгенерировать consumer pact, опубликовать его в Pact Broker и проверить provider.

## Архитектура

```text
rate-printer
    |
    | discovery через ZooKeeper (/services)
    v
ZooKeeper
    ^
    | регистрация provider instance
    |
currency-rate-provider

rate-printer -- gRPC GetRate(pair) --> currency-rate-provider
```

`currency-rate-provider` при старте регистрирует свой адрес в ZooKeeper как ephemeral node. `rate-printer` читает список доступных provider-инстансов из ZooKeeper, случайно выбирает один адрес и отправляет запрос `GetRate`.

Контракт gRPC API описан в `rate.proto`:

```proto
service CurrencyRateService {
  rpc GetRate (RateRequest) returns (RateResponse);
}
```

Сейчас поддерживается валютная пара `USDRUB`. Для остальных пар provider возвращает gRPC-ошибку `INVALID_ARGUMENT`.

## Технологии

- Java 21
- Spring Boot
- Spring gRPC
- Protocol Buffers
- Apache ZooKeeper и Apache Curator
- Pact JVM с protobuf/gRPC plugin
- PostgreSQL для Pact Broker
- Docker Compose
- Maven

## Требования

- JDK 21
- Maven или Maven Wrapper из модулей проекта
- Docker и Docker Compose
- Bash для запуска `start.bash` и shell-скриптов из `currency-rate-provider`

## Быстрый запуск инфраструктуры

Из корня проекта:

```bash
docker compose up -d
```

Будут запущены:

- ZooKeeper: `localhost:2181`
- Pact Broker: `http://localhost:9292`
- PostgreSQL для Pact Broker: внешний порт `5433`

Остановить инфраструктуру:

```bash
docker compose down
```

## Запуск сервисов

Сначала поднимите инфраструктуру:

```bash
docker compose up -d
```

Затем запустите один или несколько provider-инстансов:

```bash
cd currency-rate-provider
./mvnw spring-boot:run
```

По умолчанию provider слушает gRPC порт `9090` и регистрируется в ZooKeeper по пути `/services`.

Для запуска нескольких provider-инстансов можно использовать готовый скрипт:

```bash
cd currency-rate-provider
./start.sh
```

Он собирает приложение и запускает provider на портах `9090`, `9091` и `9092`.

После этого запустите consumer:

```bash
cd rate-printer
./mvnw spring-boot:run
```

`rate-printer` будет каждые 5 секунд выбирать provider-инстанс и печатать в консоль результат вида:

```text
Request sent to 127.0.0.1:9090
USD/RUB: 91.23 (timestamp: 2026-05-16 10:00:00)
```

## Pact flow

Для полного сценария consumer-driven contract testing из корня проекта:

```bash
./start.bash
```

Скрипт выполняет следующие шаги:

1. Поднимает ZooKeeper, Pact Broker и PostgreSQL через Docker Compose.
2. Запускает consumer Pact-тест в `rate-printer`.
3. Публикует сгенерированный pact в Pact Broker.
4. Запускает provider verification в `currency-rate-provider`.
5. Публикует результат проверки в Pact Broker.

После выполнения можно открыть Pact Broker:

```text
http://localhost:9292
```

## Тесты

Consumer Pact-тест:

```bash
cd rate-printer
./mvnw -Dtest=RatePrinterGrpcConsumerPactTest test
```

Provider verification:

```bash
cd currency-rate-provider
./mvnw verify -Djava.net.preferIPv4Stack=true -Dpact.verifier.publishResults=true -Dpact.provider.version=0.0.1-SNAPSHOT
```

Обычные тесты модуля:

```bash
cd rate-printer
./mvnw test
```

```bash
cd currency-rate-provider
./mvnw test
```

## Структура каталогов

```text
.
├── currency-rate-provider
│   ├── src/main/java/.../grpc
│   ├── src/main/java/.../service
│   ├── src/main/java/.../zookeeper
│   ├── src/main/proto/rate.proto
│   └── src/test/java/.../pact
├── rate-printer
│   ├── src/main/java/.../discovery
│   ├── src/main/java/.../service
│   ├── src/main/proto/rate.proto
│   └── src/test/java/.../pact
├── docker-compose.yml
└── start.bash
```

## Основные настройки

`currency-rate-provider/src/main/resources/application.properties`:

```properties
spring.grpc.server.port=9090
zookeeper.connect-string=localhost:2181
zookeeper.service-path=/services
```

`rate-printer/src/main/resources/application.properties`:

```properties
server.port=8080
spring.cloud.zookeeper.connect-string=localhost:2181
spring.cloud.zookeeper.discovery.enabled=true
```

## Полезные команды

Остановить provider-процессы, запущенные через `currency-rate-provider/start.sh`:

```bash
cd currency-rate-provider
./kill.sh
```

Посмотреть логи provider-инстансов:

```bash
cd currency-rate-provider
tail -f producer-9090.log
```

Полностью пересоздать инфраструктуру:

```bash
docker compose down -v
docker compose up -d
```
