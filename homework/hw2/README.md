# HSE-Bank: Модуль учета финансов

## 1. Реализованный функционал
- Счета: создание, редактирование, удаление, список (`account create|edit|delete|list`). Автообновление баланса при операциях.
- Категории: создание, редактирование, удаление, список (`category create|edit|delete|list`). Типы: доход / расход.
- Операции: добавление, редактирование (коррекция баланса), удаление, список с фильтром по счету (`operation add|edit|delete|list`).
- Пересчёт баланса счёта: автоматический (от нуля) и с произвольного стартового значения; опциональное применение результата (`recalculate auto|custom`).
- Аналитика: суммарный доход, расход, дельта; агрегирование по выбранному счёту (`analytics totals`).
- Статистика команд: сбор и вывод агрегатов, очистка (`analytics stats|clear-stats`).
- Измерение времени выполнения команд (опция `--timing`).
- Импорт / экспорт всех данных в JSON / YAML / CSV (`data import|export --format`). Поддерживает очистку перед импортом (`--clear`).
- Сохранение истории выполнений команд (аудит в файл + в БД, для статистики).
- Валидация входных данных (имена, суммы, описания).

## 2. SOLID
- SRP (Single Responsibility): фабрики (`*Factory`), валидаторы (каждый класс проверяет одно правило), стратегии пересчёта (`AutoRecalculationStrategy`, `CustomStartingBalanceStrategy`), декораторы (`TimingDecorator`, `LoggingDecorator`), сериализация/десериализация разделены (`DatabaseSerializer`, `DatabaseDeserializer`).
- OCP (Open/Closed): добавление новых стратегий пересчёта, новых валидаторов (через цепочку), новых форматов сериализации (расширение `Serializer` / `Deserializer`) без изменения существующего кода.
- LSP (Liskov): реализации интерфейсов `RecalculationStrategy`, `Command<T>`, `Validator<T>` взаимозаменяемы без нарушения логики.
- ISP (Interface Segregation): минимальные абстракции `Command<T>`, `RecalculationStrategy`, `CommandObserver` без лишних методов.
- DIP (Dependency Inversion): зависимости (БД, фабрики, наблюдатели, сериализаторы) внедряются через DI (Koin), высокоуровневый код опирается на абстракции (`RecalculationStrategy`, `CommandObserver`).

## 3. GRASP
- High Cohesion: каждый пакет (domain, factories, validation, strategies, observers, serialization, cli) решает узко сфокусированные задачи.
- Low Coupling: взаимодействие через интерфейсы (Command, Strategy, Observer), фабрики скрывают детали создания; DI уменьшает жёсткие зависимости.
- Creator: фабрики создают доменные объекты и выполняют валидацию.
- Controller: CLI-команды выступают контроллерами пользовательских сценариев.

## 4. Использованные паттерны
Порождающие:
- Factory Method: `BankAccountFactory`, `CategoryFactory`, `OperationFactory`.

Структурные:
- Facade: группирующие CLI-команды (`AccountCommands`, `CategoryCommands`, `OperationCommands`, `AnalyticsCommands`, `DataCommands`) предоставляют упрощённый интерфейс сценариев.
- Decorator: `CommandDecorator`, `TimingDecorator`, `LoggingDecorator` — добавляют поведение (логирование, тайминг) без изменения команд.

Поведенческие:
- Command: `RecalculateBalanceCommand`, `CalculateTotalsCommand`.
- Strategy: `RecalculationStrategy`.
- Observer: `ObservableCommand`, `CommandObserver`.
- Template Method: `DatabaseSerializer`, `DatabaseDeserializer`
- Chain of Responsibility: `Validator<T>` объединяются через `then`.

## 5. Инструкция по запуску
1. Установить JDK 24.
2. Клонировать проект и перейти в директорию.
3. Сборка: `./gradlew fatJar`
4. Запуск CLI (интерактивное чтение stdin включено): `java --enable-native-access=ALL-UNNAMED -jar .\build\libs\kpo-bank-1.0-SNAPSHOT-all.jar`
   Внимание! CLI создает/использует test.db и command_audit.log в текущей директории.

   Примеры:
   - Создать счёт: `java --enable-native-access=ALL-UNNAMED -jar .\build\libs\kpo-bank-1.0-SNAPSHOT-all.jar account create --name Main --balance 1000`
   - Создать категорию: `java --enable-native-access=ALL-UNNAMED -jar .\build\libs\kpo-bank-1.0-SNAPSHOT-all.jar category create --name Cafe --type SPENDING`
   - Добавить операцию: `java --enable-native-access=ALL-UNNAMED -jar .\build\libs\kpo-bank-1.0-SNAPSHOT-all.jar operation add --account 1 --category 1 --amount 250 --description Lunch --type SPENDING`
   - Аналитика: `java --enable-native-access=ALL-UNNAMED -jar .\build\libs\kpo-bank-1.0-SNAPSHOT-all.jar analytics totals --account 1 --timing`
   - Экспорт: `java --enable-native-access=ALL-UNNAMED -jar .\build\libs\kpo-bank-1.0-SNAPSHOT-all.jar data export ./backup --format json`
   - Импорт (с очисткой): `java --enable-native-access=ALL-UNNAMED -jar .\build\libs\kpo-bank-1.0-SNAPSHOT-all.jar data import ./backup --format json --clear`
   - Пересчёт баланса: `java --enable-native-access=ALL-UNNAMED -jar .\build\libs\kpo-bank-1.0-SNAPSHOT-all.jar account recalculate auto --account 1`
