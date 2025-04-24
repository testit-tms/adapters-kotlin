# Chapter 1: Менеджер Адаптера (AdapterManager)


Добро пожаловать в мир `adapters-kotlin`! Это первая глава нашего путешествия, и мы начнем с самого сердца адаптера — **Менеджера Адаптера (`AdapterManager`)**.

Представьте, что вы управляете большим заводом по производству... ну, скажем, результатов тестов! У вас есть цеха (тестовые классы), станки (тесты), бригады (настроечные методы `before`/`after`), и все это должно слаженно работать, а готовую продукцию (результаты тестов) нужно вовремя отправлять заказчику (в систему Test IT). Кто всем этим руководит? Правильно, **главный диспетчер**.

В нашем адаптере роль такого диспетчера выполняет `AdapterManager`.

## Зачем нужен `AdapterManager`?

Основная задача адаптера — собрать информацию о ходе выполнения ваших автоматических тестов и отправить ее в систему управления тестированием Test IT. Но это не так просто, как кажется. Нужно:

1.  Понять, когда начался весь тестовый запуск.
2.  Знать, когда стартует и финиширует каждый отдельный тест.
3.  Отслеживать "подготовительные" и "завершающие" шаги (fixtures/hooks).
4.  Собирать детали: статус (успех/провал/пропуск), сообщения об ошибках, вложения (скриншоты, логи), ссылки.
5.  Правильно все это упаковать и отправить в Test IT по сети.

Делать все это вручную в каждом тесте было бы ужасно неудобно. `AdapterManager` берет всю эту сложную координацию на себя. Он как главный дирижер оркестра, где музыканты — это ваши тесты и другие части адаптера.

## Что делает `AdapterManager`? Ключевые Задачи

`AdapterManager` — это центральный узел, который отвечает за множество вещей:

*   **Управление жизненным циклом:** Он знает, когда начинается (`startTests`) и заканчивается (`stopTests`) вся сессия тестирования.
*   **Координация тестов:** Запускает (`startTestCase`), останавливает (`stopTestCase`), обновляет информацию (`updateTestCase`) для каждого отдельного теста. Он также планирует тесты к запуску (`scheduleTestCase`).
*   **Работа с "контейнерами":** Тесты часто группируются (например, по классам). `AdapterManager` управляет этими группами (`startClassContainer`, `stopClassContainer`, `startMainContainer`, `stopMainContainer`).
*   **Обработка Fixtures (Setup/Teardown):** Запускает и останавливает шаги настройки и очистки (`startPrepareFixture`, `stopFixture` и их вариации).
*   **Управление шагами (Steps):** Если ваши тесты состоят из более мелких шагов, `AdapterManager` может управлять и ими (`startStep`, `stopStep`).
*   **Взаимодействие с Test IT:** Он использует [API Клиент TMS (TmsApiClient)](09_api_клиент_tms__tmsapiclient__.md) для реальной отправки данных в Test IT.
*   **Хранение результатов:** Временно хранит результаты в `ResultStorage` перед отправкой.
*   **Конфигурация:** Получает все необходимые настройки из [Конфигурация Адаптера и Клиента (AdapterConfig, ClientConfiguration)](02_конфигурация_адаптера_и_клиента__adapterconfig__clientconfiguration__.md).

Вам, как пользователю тестов, обычно не нужно напрямую вызывать методы `AdapterManager`. Это делают специальные "слушатели" (listeners), которые интегрируются с вашим тестовым фреймворком (как Kotest, JUnit и т.д.). Но понимание роли `AdapterManager` поможет вам понять, как адаптер работает "под капотом".

## Как получить `AdapterManager`?

Обычно доступ к `AdapterManager` осуществляется через статический метод в классе `Adapter`:

```kotlin
// Получаем экземпляр Менеджера Адаптера
val adapterManager = Adapter.getAdapterManager()

// Теперь можно использовать adapterManager для управления тестами
// (обычно это делает listener фреймворка)
```

Этот код просто запрашивает "главного диспетчера". Если его еще нет, он будет создан со всеми необходимыми настройками.

## Пример (Концептуальный): Жизненный цикл теста

Давайте представим, как `AdapterManager` управляет одним тестом (это упрощенная картина, реальный код находится в listeners):

1.  **Планирование теста:** Перед запуском теста listener сообщает `AdapterManager`, что есть такой тест.

    ```kotlin
    // Listener создает информацию о тесте
    val testInfo = TestResultCommon(uuid = "уникальный-id-теста-123", ...)

    // Listener передает ее AdapterManager
    adapterManager.scheduleTestCase(testInfo)
    // Результат: AdapterManager сохраняет информацию о тесте в ResultStorage.
    ```

2.  **Запуск теста:** Когда фреймворк начинает выполнять тест.

    ```kotlin
    val testUuid = "уникальный-id-теста-123"

    // Listener сообщает AdapterManager о старте
    adapterManager.startTestCase(testUuid)
    // Результат: AdapterManager находит тест в ResultStorage,
    // устанавливает ему статус "RUNNING" и запоминает время начала.
    ```

3.  **Добавление деталей (например, вложения):** Во время выполнения теста мы можем добавить скриншот.

    ```kotlin
    // Код вашего теста делает скриншот и сохраняет его в файл
    val screenshotPath = "path/to/screenshot.png"

    // Используем хелпер Adapter для добавления
    Adapter.addAttachments(listOf(screenshotPath))
    // Результат: Adapter вызывает adapterManager, который добавляет
    // информацию о вложении к текущему активному тесту.
    ```

4.  **Завершение теста:** Когда тест закончен (успешно или с ошибкой).

    ```kotlin
    val testUuid = "уникальный-id-теста-123"
    val status = ItemStatus.PASSED // или FAILED, SKIPPED
    var error: Throwable? = null // null, если успех

    // Listener обновляет финальный статус
    adapterManager.updateTestCase(testUuid) { testResult ->
        testResult.itemStatus = status
        testResult.throwable = error
        // ... другие обновления ...
    }

    // Listener сообщает AdapterManager о завершении
    adapterManager.stopTestCase(testUuid)
    // Результат: AdapterManager устанавливает статус "FINISHED",
    // время окончания и через Writer отправляет результат в Test IT.
    ```

Эти примеры показывают *взаимодействие* с `AdapterManager`. Сам `AdapterManager` координирует эти вызовы с другими компонентами.

## Под капотом: Как работает `AdapterManager`?

Давайте заглянем глубже. Представьте снова наш завод.

1.  **Заказ поступил (scheduleTestCase):** Диспетчер (`AdapterManager`) получает заявку (информацию о тесте) и кладет ее в папку "Ожидают запуска" (`ResultStorage`).
2.  **Начало производства (startTestCase):** Диспетчер берет заявку из папки, ставит на ней отметку "В работе", записывает время начала и передает в цех (запускает тест). Он также запоминает, какой именно тест сейчас активен (`ThreadContext`).
3.  **Обновление информации (updateTestCase/addAttachments):** Если в процессе приходят новые данные (например, отчет о дефекте - ошибка, или сертификат качества - вложение), диспетчер находит текущую заявку и добавляет эту информацию.
4.  **Продукция готова (stopTestCase):** Когда тест завершен, диспетчер ставит отметку "Готово", записывает время окончания, собирает все данные по заявке и передает их в отдел отгрузки ([Writer](04_запись_результатов__testitwriter__.md)). Отдел отгрузки упаковывает все и отправляет заказчику ([ApiClient](09_api_клиент_tms__tmsapiclient__.md) -> Test IT). Заявка убирается из папки "В работе".

Диаграмма последовательности для простого теста:

```mermaid
sequenceDiagram
    participant Listener as Тестовый Фреймворк (Listener)
    participant AM as AdapterManager
    participant Storage as ResultStorage (Хранилище)
    participant Writer as Writer (Отправщик)
    participant API as TmsApiClient (API Test IT)

    Listener->>+AM: scheduleTestCase(данные_теста)
    AM->>+Storage: Положить(uuid, данные_теста)
    Storage-->>-AM: OK
    AM-->>-Listener: OK

    Listener->>+AM: startTestCase(uuid)
    AM->>+Storage: Взять(uuid)
    Storage-->>-AM: данные_теста
    AM->>AM: Установить статус RUNNING, время start
    AM->>Storage: Обновить(uuid, данные_теста)
    Storage-->>-AM: OK
    AM->>AM: Запомнить текущий тест (ThreadContext)
    AM-->>-Listener: OK

    Note over Listener, API: Тест выполняется...

    Listener->>+AM: stopTestCase(uuid)
    AM->>+Storage: Взять(uuid)
    Storage-->>-AM: данные_теста
    AM->>AM: Установить статус FINISHED, время stop
    AM->>+Writer: ОтправитьТест(данные_теста)
    Writer->>+API: ЗагрузитьРезультат(данные_теста)
    API-->>-Writer: OK (Test IT принял результат)
    Writer-->>-AM: OK
    AM->>+Storage: Удалить(uuid) (или пометить как отправленный)
    Storage-->>-AM: OK
    AM->>AM: Забыть текущий тест (ThreadContext)
    AM-->>-Listener: OK
```

### Немного кода изнутри

Создание `AdapterManager`:

```kotlin
// Упрощенный конструктор AdapterManager
class AdapterManager(
    // Настройки для подключения к Test IT
    private var clientConfiguration: ClientConfiguration,
    // Настройки самого адаптера
    private var adapterConfig: AdapterConfig,
    // Клиент для общения с API Test IT
    private var client: ApiClient,
    // Компонент для записи/отправки результатов
    private var writer: Writer?,
    // Хранилище результатов
    private var storage: ResultStorage,
    // Контекст текущего выполняемого теста/шага
    private var threadContext: ThreadContext
) {
    // ... остальной код ...
}

// Как он обычно создается (через Adapter.getAdapterManager)
// 1. Загружаются настройки (AdapterConfig, ClientConfiguration)
// 2. Создается ApiClient
// 3. Создается ResultStorage
// 4. Создается Writer (например, HttpWriter)
// 5. Вызывается конструктор AdapterManager со всем этим
```

Пример логики `startTestCase`:

```kotlin
// Упрощенный метод startTestCase в AdapterManager
fun startTestCase(uuid: String) {
    // ... (проверка настроек) ...

    // Очищаем контекст предыдущего теста/шага
    threadContext.clear()

    // Находим запланированный тест в хранилище
    val found = storage.getTestResult(uuid)
    if (!found.isPresent) {
        // Логирование ошибки, если тест не найден
        return
    }
    val testResult = found.get()

    // Обновляем статус и время начала
    testResult.setItemStage(ItemStage.RUNNING)
    testResult.start = System.currentTimeMillis()

    // Запоминаем, что этот тест сейчас активен
    threadContext.start(uuid)

    // ... (логирование) ...
}
```

Пример логики `stopTestCase`:

```kotlin
// Упрощенный метод stopTestCase в AdapterManager
fun stopTestCase(uuid: String) {
    // ... (проверка настроек) ...

    // Находим активный тест в хранилище
    val found = storage.getTestResult(uuid)
    if (!found.isPresent) {
        // Логирование ошибки
        return
    }
    val testResult = found.get()

    // ... (вызов listenerManager.beforeTestStop) ...

    // Обновляем статус и время окончания
    testResult.setItemStage(ItemStage.FINISHED)
    testResult.stop = System.currentTimeMillis()

    // Очищаем контекст (этот тест больше не активен)
    threadContext.clear()

    // ... (логирование) ...

    // Передаем готовый результат компоненту Writer для отправки
    writer?.writeTest(testResult) // Writer уже сам разберется с ApiClient
}
```

## Ключевые зависимости `AdapterManager`

`AdapterManager` не работает в вакууме. Он тесно связан с другими частями адаптера:

*   **[Конфигурация Адаптера и Клиента (AdapterConfig, ClientConfiguration)](02_конфигурация_адаптера_и_клиента__adapterconfig__clientconfiguration__.md):** Отсюда он берет все настройки – куда подключаться, как себя вести и т.д.
*   **`ResultStorage`:** Внутренний компонент для временного хранения данных о тестах, фикстурах, шагах до их отправки.
*   **[Запись Результатов (TestItWriter)](04_запись_результатов__testitwriter__.md) / [Запись Результатов по HTTP (HttpWriter)](08_запись_результатов_по_http__httpwriter__.md):** Компоненты, ответственные за финальную подготовку и отправку данных через `ApiClient`.
*   **[API Клиент TMS (TmsApiClient)](09_api_клиент_tms__tmsapiclient__.md):** Непосредственно общается с API Test IT для создания тест-ранов и отправки результатов.

## Заключение

Мы познакомились с `AdapterManager` — главным координатором и диспетчером адаптера `adapters-kotlin`. Он управляет всем процессом сбора и отправки результатов тестов в Test IT, скрывая сложность от конечного пользователя и тестовых фреймворков. Он как мозг всей операции, связывающий воедино конфигурацию, хранилище, клиент API и жизненный цикл тестов.

Теперь, когда мы знаем, *кто* главный, логично узнать, *откуда* он получает свои инструкции. В следующей главе мы подробно разберем конфигурационные файлы и классы, которые определяют поведение `AdapterManager`.

**Далее:** [Глава 2: Конфигурация Адаптера и Клиента (AdapterConfig, ClientConfiguration)](02_конфигурация_адаптера_и_клиента__adapterconfig__clientconfiguration__.md)

---

Generated by [AI Codebase Knowledge Builder](https://github.com/The-Pocket/Tutorial-Codebase-Knowledge)