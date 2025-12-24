# KPO Payments

ДЗ-4 по дисциплине "Конструирование программного обеспечения" ПИ.

Микросервисы на Rust для обработки заказов и платежей с RabbitMQ и паттернами transactional outbox/inbox.

## Что здесь есть

- API шлюз (Nginx) на `:80`

Все остальные порты опубликованы только для отладки:
- Сервис заказов на `:8081`
- Сервис платежей на `:8082`
- Postgres для каждого сервиса (`:5433`, `:5434`)
- RabbitMQ (`:5672`, management `:15672`)

## Запуск (Docker)

```bash
docker compose up --build
```

Маршруты шлюза:
- `/orders` → сервис заказов
- `/accounts` → сервис платежей
- `/health` → health шлюза

## REST API

Заказы:
- `POST /orders` `{ user_id, amount_cents, description? }` → `{ order_id, status: "NEW" }`
- `GET /orders/{order_id}?user_id=...` → представление заказа
- `GET /orders?user_id=...` → список заказов

Платежи:
- `POST /accounts` `{ user_id }` → представление аккаунта
- `POST /accounts/{user_id}/topup` `{ amount_cents }` → представление аккаунта
- `GET /accounts/{user_id}` → представление аккаунта

## Postman коллекция

Файл коллекции: `postman_collection.json` (использует переменные `baseUrl`, `user_id`, `order_id`).

## События

Topic exchange в RabbitMQ: `events`

Routing keys:
- `orders.created`
- `payments.result`

Payloads:
- `orders.created`: `{ event_id, order_id, user_id, amount_cents }`
- `payments.result`: `{ event_id, order_id, user_id, amount_cents, status }`

## Поток обработки (очереди и сообщения)

1) Клиент создаёт заказ через `POST /orders`.
2) Orders в одной транзакции:
   - сохраняет заказ;
   - кладёт событие `orders.created` в outbox.
3) Фоновый outbox-публикатор Orders публикует событие в exchange `events` с routing key `orders.created`.
4) Очередь `payments.orders.created` подписана на `orders.created` и доставляет сообщение в Payments.
5) Payments получает сообщение из очереди и начинает обработку.
6) В одной транзакции Payments:
   - записывает `event_id` в inbox (дедупликация);
   - фиксирует `order_id` в ledger (блокирует повторное списание);
   - списывает баланс (если хватает средств);
   - пишет `payments.result` в outbox;
   - помечает запись inbox как обработанную.
7) После коммита сообщения отправляется ACK брокеру; при ошибке — NACK и повтор.
8) Фоновый outbox-публикатор Payments публикует `payments.result` в exchange `events`.
9) Очередь `orders.payments.result` подписана на `payments.result` и доставляет сообщение в Orders.
10) Orders принимает `payments.result` и идемпотентно обновляет статус заказа.

## Идемпотентность и гарантия “один раз”

В обработке задействованы четыре механизма. Ниже — какие конфликты они ловят и что происходит.

- Inbox (`event_id` как PK): повторная доставка того же `orders.created` приводит к конфликту вставки, обработка сразу прекращается.
- Ledger (`order_id` как PK): повторная попытка списания по тому же заказу конфликтует при вставке, списание не повторяется.
- Outbox: если сервис упал после коммита БД, но до публикации, событие сохранено и будет отправлено при следующем запуске.
- Обновление статуса в Orders: апдейт выполняется только если статус всё ещё `NEW`, поэтому повторные `payments.result` не меняют строку.
