# Change Log

## 4.6.41 - 2024-01-22
### App
- Перешли на использование compileOnly для spring-boot-starter-data-redis.

## 4.6.40 - 2024-01-19
### Report
- Изменили контракты ReportService.

## 4.6.39 - 2024-01-16
### Jms
- Изменен путь по умолчанию для data папки.

## 4.6.38 - 2024-01-11
### Cache
- Добавлено управление операцией PUT в DistributedCacheService через distributed op's в проперти файлах.

## 4.6.36 - 2023-12-21
### App
- Исправление ошибки отображение версии приложения из-за переноса пропертей.
- Расширение TransactionalExecutor.

## 4.6.35 - 2023-12-18
### Bpm
- Перенос исправления ошибки ConcurrentModificationException возникающая при загрузке workflow с EventTrigger из версии 4.5.40.

## 4.6.34 - 2023-12-14
### Dicts
- Исправили формирование логического выражения с использованием and и or.

## 4.6.33 - 2023-12-14
### Dicts
- Исправили получение ref полей при обращении к справочнику с использованием * в selectFields.

## 4.6.32 - 2023-12-12
### Dicts
- Исправили работу со справочниками в случае когда мета-информация и айтемы находятся в разных storage.

## 4.6.31 - 2023-11-29
### Dependency Upgrades
- Spring Boot 2.7.18.

## 4.6.30 - 2023-11-29
### Dicts
- Исправили получение записей справочника с отсутствующим ref полем.

## 4.6.29 - 2023-11-28
### Cache
- Убрана зависимость EhCacheConfigParser от jakarta.

## 4.6.28 - 2023-11-28
### Dicts
- Исправили создание новых AttachmentBinding при обновлении DictItem.

## 4.6.27 - 2023-11-24
### Cache
- EhCache смигрирован на 3 версию.
- Убрана жесткая зависимость от database.

## 4.6.26 - 2023-11-21
### Dependency Upgrades
- Flyway 9.22.3
- MinIO 8.5.7
- Flyway 9.22.3
- Guava 32.1.3-jre
- Hazelcast 5.3.6
- Apache POI 5.2.4
- Geolatte 1.9.1
- Kotlin 1.8.10

## 4.6.25 - 2023-11-17
### Cache
- Убрали обязательную зависимость от ehcache и hazelcast.

## 4.6.24 - 2023-11-10
### Dicts
- Исправили обновление записей справочника с engine Postgres, у которых идентификатор поля начинается с цифры.

## 4.6.23 - 2023-10-30
### All
- Стандартизация констант и утилит для дат.

## 4.6.22 - 2023-10-31
### Dicts
- Добавили наполнение справочника отсутствующими сервисными полями при миграции storage.

## 4.6.21 - 2023-10-31
### Dicts
- Дополнили кавычками обращение к полям в select/join clause для полей, начинающихся с цифры.

## 4.6.20 - 2023-10-30
### Dicts
- Исправлено получение DictItem из Postgres storage, у которых длина alias больше 63 символов.
- Реализовали формирование alias для Postgres storage вида t1_fieldId, t2_fieldId.
- Добавили ограничение на максимальную длину идентификатора поля в 32 символа.

## 4.6.19 - 2023-10-26
### Database
- Реализован механизм read only для атрибутов энтити (аннотация @ReadOnlyColumn).

## 4.6.18 - 2023-10-25
### App
- Добавили DateUtils.
### Api
- Добавили RemoteServiceUtils.

## 4.6.17 - 2023-10-25
### Dicts
- Исправлено получение DictItem из Postgres storage, у которых имена колонок начинались с цифры.

## 4.6.16 - 2023-10-24
### Dependency Upgrades
- Spring Boot 2.7.17

## 4.6.15 - 2023-10-24
### Dicts
- Исправлен приоритет вызова DictLockInitializer.

## 4.6.14 - 2023-10-23
### Dicts
- Вынесли новый интерфейс провайдеров для первоначальной миграции схемы справочников DictsStorageDDLProvider.
- Добавлена инициализация идентификаторов справочников в коллекции локов DictLockService до начала обработки миграций.
- Исправлена ошибка отсутствия идентификатора справочника в коллекции локов DictLockService при обработке миграций.

## 4.6.13 - 2023-10-23
### Audit
- Добавили проперти для активации AuditDDLProvider.
### Attachments
- Добавили проперти для активации AttachmentsDDLProvider.

## 4.6.12 - 2023-10-19
### Audit
- Исправили проблему согласованности миграций с устаревшим провайдером essential. Добавили AuditDDLProvider.
### Attachments
- Исправили проблему согласованности миграций с устаревшим провайдером essential. Добавили AttachmentsDDLProvider.

## 4.6.11 - 2023-10-19
### Dicts
- Реализовали миграцию справочников между дата-сорсами через API.
- Добавили DictLockService для блокировки работы со справочником во время его изменения.
- Добавили блокирующие аннотации @LockDictSchemaModifyOperation, @LockDictOperation.
- Убрали возможность переименования схемы справочника через миграции и API.

## 4.6.10 - 2023-10-16
### Jobs
- Исправлена ошибка в параметрах запроса на изменение расписания.
- Добавлен эндпоинт, позволяющий увидеть cron-выражение или период между запусками для периодических задач.

## 4.6.9 - 2023-10-12
### Jms
- Добавление зависимостей activemq.

## 4.6.8 - 2023-10-09
### Database
- HikariCP добавлен в прямые зависимости.

## 4.6.7 - 2023-10-05
### App
- Добавлен SpringApplicationUtils для включения Startup Actuator Endpoint при запуске.

## 4.6.6 - 2023-10-03
### Dependency Upgrades
- Spring Boot 2.7.16
- Flyway 9.22.2
- Feign Jackson 12.5

## 4.6.5 - 2023-09-29
### Dicts
- Изменили валидацию при удалении справочника.

## 4.6.4 - 2023-09-28
### Dicts
- Доработали создание справочника без явного указания engine.

## 4.6.3 - 2023-09-27
### App
- Откат JmsConfiguration до версии с несколькими реализациями JmsListenerContainerFactory.

## 4.6.2 - 2023-09-27
### Dicts
- Убрана избыточная логика сохранения enum при конвертации реквеста на создание Dict.

## 4.6.1 - 2023-09-15
### All
- Декомпозиция проекта на стартеры.
- Очистили модули от лишних зависимостей.
- Перешли на использование api вместо compileOnly зависимостей, за исключением случаев, где клиенту не нужен полный перечень зависимостей модуля.

## 4.5.34 - 2023-09-14
### Dicts
- Исправлена десериализация и конвертация референсных полей (вложенных элементов) DictItemDto#dictData.

## 4.5.33 - 2023-09-08
### Report
- Добавили интерфейс ReportService и две реализации: AttachmentReportService, InMemoryReportService.
- Заменили ReportNotification на ReportTask, дополнили модель полями reportId, userId.
- Добавили реализацию по умолчанию для ReportTaskService: InMemoryReportTaskService.
- Изменили определение reportGenerator'a в локаторе с ReportFilter на ReportType.
- Расширили интерфейс ReportService для вызова генерации отчетов с передачей ReportType.

## 4.5.32 - 2023-09-07
### Dependency Upgrades
- Spring Boot 2.7.15
- EclipseLink 2.7.13
- Groovy 3.0.19
- MinIO 8.5.5
- Flyway 9.22.0
- Guava 32.1.2-jre

## 4.5.31 - 2023-08-18
### Report
- Добавлен метод для генерации отчета с userId при отсутствии security контекста.
- Добавлена передача userId в properties ReportNotification.
- Отказались от применения JMS в очереди отчетов в пользу taskExecutor.
- Добавлен лисенер на выполнение неотработанных нотификаций отчетов.

## 4.5.30 - 2023-08-11
### App
- Теперь доступна только одна по умолчанию не транзакционная версия JmsListenerContainerFactory, управления транзакциями для неё осуществляется стандартными средствами через аннотацию @Transactional. Везде, где использовалась nonTxJmsListenerContainerFactory, необходимо использовать jmsListenerContainerFactory.

## 4.5.29 - 2023-08-11
### App
- Добавлена абстракция переопределеного поведения json сериализации/десериализации из com.fasterxml.jackson.databind.

### Dicts
- Добавлена реализация абстракции json сериализации/десериализации для DictItem#dictData.

## 4.5.28 - 2023-08-08
### App
- Добавлен JmsListenerContainerFactory с транзакцией только на уровне JMS.

## 4.5.27 - 2023-08-03
### Dicts
- Обновление DictDto и конвертеров для работы с возможностью установки DictEngine.

## 4.5.26 - 2023-08-03
### Dicts
- Добавили поддержку PostgreSQL для справочников.
- Добавили миграцию сущностей Dict, VersionScheme между datasource-ами.

## 4.5.25 - 2023-07-28
### App
- Добавлен DateUtils.
- Добавлен модуль report.
- Добавлен ReportService. Сервис поддержки приемы запросов и генерации отчетов.
- Добавлен набор абстракции с общей функциональной логикой для реализации excel отчетов.
- Добавлен ReportNotificationAdvice. Предоставляет возможность описания дополнительных действий в сочетании с нотификациями.

## 4.5.24 - 2023-07-27
### App
- Фикс конфигурации JpaModelGenPlugin.

## 4.5.23 - 2023-07-27
### App
- Доработали ApiEnumService. Теперь можно указать пакеты, в которых будет происходить поиск аннотации @ApiEnum, не ограничиваясь com.proit.
- Доработали MetaModelVerifier. Теперь также можно указать пакеты для поиска моделей, не ограничиваясь com.proit.
- Однотипная доработка для DefaultEntityManagerFactoryCustomizer.

## 4.5.22 - 2023-07-26
### Dependency Upgrades
- Spring Boot 2.7.14
- Spring Cloud 3.1.7

### App
- Добавлены аннотации @ConditionalOnJpa, @ConditionalOnJms, @ConditionalOnAudit как дополнительный уровень абстракции между конфигурацией и компонентами библиотеки.

## 4.5.21 - 2023-07-10
### Starter Feign
- Понизили приоритет вызова Interceptor-а и убрали прерывание запроса при отсутствии авторизации.

### Dicts
- Добавили вторые версии эндпоинтов RemoteDictDataEndpoint и RemoteDictDataService без передачи userId параметром.

## 4.5.20 - 2023-07-06
### App
- Добавлен параметр конфигурации app.transaction.template для управления созданием TransactionTemplate.

## 4.5.19 - 2023-07-03
### App
- Изменена обязательность поля objectId в audit.

## 4.5.18 - 2023-06-27
### Dependency Upgrades
- Spring Boot 2.7.13
- MinIO 8.4.6
- Guava 32.0.1-jre
- Commons-io 2.13.0
- Commons-codec 1.16.0

## 4.5.17 - 2023-06-19
### Dicts
- Добавлен метод для получения списка id элементов справочника без пагинации.

# Change Log
## 4.5.16 - 2023-06-08
### Dicts
- Исправлена ошибка валидации Date/LocalDate/LocalDateTime при миграции DictItem с типом поля DATE/TIMESTAMP.

## 4.5.15 - 2023-06-02
### Dicts
- Исправлена ошибка удаления таблиц при откате миграции.

## 4.5.14 - 2023-05-31
### App
- Исправлено отображение _java.util.Map_ для SpringDoc.

## 4.5.13 - 2023-05-31
### App
- Исправлена ошибка с десериализацией JobParams в ScheduledJobsEndpoint.

## 4.5.12 - 2023-05-30
### Dicts
- Добавлен принудительный маппинг Date к LocalDate или LocalDateTime при получении DictItem.

## 4.5.11 - 2023-05-30
### Dependency Upgrades
- Spring Boot 2.7.12
- Spring Cloud 3.1.5
- Groovy 3.0.17
- Spring Doc 1.6.15
- Flyway 9.19.1
- Hazelcast 5.2.3
- Guava 32.0.0-jre

### App
- Добавлена возможность менять порядок менеджеров транзакций при объединении в цепочки через параметр конфигурации app.transaction.chaining.

## 4.5.9 - 2023-05-10
### Dicts
- Проведена финальная реорганизация кода: маппинг DictData перенесен в слой Backend, пакет exception структурирован, единая точка входа для DictDataBackend создана в виде DictDataBackendProvider.

## 4.5.8 - 2023-05-10
### Dicts
- В миграциях исправлена ошибка обновления при использовании JsonValue внутри массивов.

## 4.5.7 - 2023-05-10
### Dicts
- Проброшен id пользователя в валидацию DictDataItem при создании справочника с внешним ключом на другой справочник.

## 4.5.6 - 2023-04-24
### Dicts
- Добавлен вызов метода после обновления записи в DictDataServiceAdvice.

## 4.5.5 - 2023-04-23
### Dicts
- Исправлен маппинг JSON полей при обновлении и создании записей в миграциях.

## 4.5.4 - 2023-04-22
### Dicts
- Добавлен новый тип поля в справочниках для хранения геометрии: GeoJSON.

## 4.5.3 - 2023-04-21
### Dicts
- Добавлена возможность использования в миграции JSON полей при обновлении и создании записей.

## 4.5.2 - 2023-04-17
### App
- Добавлены аннотации для описания элементов Enum'ов: @ApiEnum, @ApiEnumDescription.

## 4.5.1 - 2023-04-15
### App
- Добавлена возможность запускать джобы с набором изменяемых параметров.
- Реализовано новое API для работы с джобами. Старое помечено как @Deprecated.

## 4.5.0 - 2023-04-05
### Dicts
- Провели предварительный рефакторинг компонента, структурировали ответственность слоев бэкенд адаптера, маппинга, валидации, сервиса домена.
- Подготовили компонент к добавлению нового persistent адаптера и датасорса.
- Актуализированы тесты.

## 4.4.45 - 2023-04-12
### BPM
- Изменен контракт эндпоинта _/api/bpm/process/kill_. Отказались от использования **@PathVariable**.
- Доработан метод kill JbpmProcessEngine.

## 4.4.43 - 2023-03-27
### Dependency Upgrades
- Spring Boot 2.7.10
- Gradle Plugins 1.0.62

## 4.4.41 - 2023-03-16
### Dicts
- Расширили паттерны дат для Constant в QueryParser, теперь доступна возможность приведения строки к дате и дате-времени в запросах по фильтру ("timestampField in ('2020-03-01'::date, '2020-03-01+03:00'::date)", "timestampField in ('2020-03-01'::timestamp, '2020-03-01+03:00'::timestamp)").

## 4.4.40 - 2023-03-16
### Dependency Upgrades
- EclipseLink 2.7.11
- Groovy 3.0.16
- Gradle Plugins 1.0.60

### Разное
- Добавлен модуль starter-feign-session, позволяющий переносить сессию пользователя при вызовах удалённых сервисов через Feign.

## 4.4.39 - 2023-03-15
### App
- Дали возможность переопределять ObjectWriter в AbstractJsonConverter.

## 4.4.38 - 2023-03-13
### Dependency Upgrades
- Spring Boot 2.7.9
- Spring Cloud 3.1.5
- Gradle Plugins 1.0.59

### Dicts
- Починили экспорт справочников в SQL и CSV.

## 4.4.36 - 2023-02-10
### App
- Добавлен TransactionalUtils для обработки кода после коммита транзакции.

## 4.4.35 - 2023-02-09
### BPM
- Добавлен параметр конфигурации app.bpm.defaultTerminatingEndEventScope для управления поведением по умолчанию для узлов терминирующего завершения внутри подпроцессов.

## 4.4.34 - 2023-01-25
### Dependency Upgrades
- Spring Boot 2.7.8

## 4.4.31 - 2023-01-20
### Dicts
- Исправили фильтрацию по полям decimal.

## 4.4.30 - 2023-01-19
### Dependency Upgrades
- Spring Boot 2.7.7
- Spring Doc 1.6.14
- Flyway 9.10.2
- Groovy 3.0.14

### App
- Исправили отображение наименования компонента с периодичными задачами в актуаторе.
- Исправили работу DeleteUnboundedAttachmentsJob.
- Параметр конфигурации app.attachments.store-path заменен на app.attachments.directory.path.

### BPM
- Добавлен индекс на колонку instance_id таблицы process.

## 4.4.24 - 2022-12-20
### Dependency Upgrades
- jBPM 7.73.0.Final

### App
- Исправлена обработка параметра конфигурации app.api.stackTraceOnError.
- OkResponse параметризован теперь Void, а не Object.

### BPM
- Добавлен метод processService.startProcessOnEvent для запуска процесса по событию без указания схемы.
- Исправлен gradle плагин bpmn::pack, иногда для схем без скриптов он генерировал пустой файл.
- Доработан метод удаления неактуальных таймеров в TimerManager для совместимости с новой версией jBPM.
- Расширены тесты, добавлены проверки миграции между разными ревизиями одного процесса.

## 4.4.16 - 2022-12-07
### Dependency Upgrades
- Spring Boot 2.7.6
- Spring Doc 1.6.13
- Flyway 9.8.2
- Hazelcast 5.2.1
- MinIO 8.4.6
- Flyway 9.1.6
- Geolatte 1.9.0

### App
- Расширили конфигурацию JMS, включили логирование ошибок IO.
- Расширено логирование AutoMigrateProcessesJob.
- В список допустимых по умолчанию вложений добавлен формат xlsm.

### BPM
- Оптимизировали поиск Task по TaskFilter.
- Исправлена ошибка, при которой из метода processService.getProcessTimers возвращались уже неактуальные таймеры.
- Исправлен экспорт схем процессов для случая, когда в редакторе в поле версия было установлено значение "0", принудительно меняем версию на "1.0".

## 4.4.7 - 2022-11-14
### Dependency Upgrades
- MinIO 8.4.5

### App
- Исправлено получение параметров поля справочника для типа boolean.
- Исправлены параметры запроса для функционала по изменения расписания для job наследованных от AbstractJob.
- Добавлен функционал по изменения расписания для job наследованных от AbstractJob.
- Добавлены геттеры для SimpleHealthIndicatorComponent.
- Добавлены геттеры для JobHealthIndicator.

### BPM
- Восстановлена работа метода processService.updateProcessTimer().

### Dicts
- Добавлена обработка списка Attachments в BindingDictDataServiceAdvice.

## 4.3.38 - 2022-10-25
### Dicts
- Расширили методы ValidationService и MappingService для обработки TIMESTAMP и DATE полей типами String и Date.
- Добавлена возможность использования операторов сравнения в миграциях для where и delete.
- Исправлены параметры запроса в RemoteDictDataService на удаление элемента из справочника.
- Добавлена возможность использовать оператор != с update при миграции справочников.

### Разное
- Добавлена возможность корректного проксирования объектов с глубиной наследования >2. Тесты для ProxyModel.

## 4.3.33 - 2022-10-20
### Dependency Upgrades
- Spring Boot 2.7.4
- Spring Cloud 3.1.4
- Spring Doc 1.6.11
- EclipseLink 2.7.11
- Groovy 3.0.13
- Flyway 9.3.1
- Hazelcast 5.1.3
- MinIO 8.3.9
- Groovy 3.0.12
- Flyway 9.1.6

### Разное
- Для noTimeoutDataSource добавлен отдельный менеджер транзакций noTimeoutDataSourceTransactionManager.

## 4.3.32 - 2022-10-19
- В конфигурации flyway значение параметра **transactionalLock** установлено **false** для избежания проблем с принудительным завершением соединений idle in transaction.

## 4.3.31 - 2022-10-17
### Dicts
- Добавлена возможность сортировки по внутреннему справочнику (один уровень вложенности).
- Добавлена возможность указать причину удаления справочника.
- В RemoteDictData добавлены отсутствующие методы из DictDataService.
- В модели CRUD реквестов dictItem добавлены конструкторы.
- Исправлена валидация обязательности поля при передаче null значения.
- Исправлена ошибка при формировании AttachmentBinding для необязательного поля с пустым значением.
- Исправлено наименование методов проверки существования записей справочника в RemoteDictData.

### Разное
- Добавлен метод для получения информации о вложениях по коллекции идентификаторов.
- В JdkSerializationRedisSerializerNoSerializationEx добавлена обработка ClassNotFoundException.
- В SecurityUtils добавлена возможность выполнять действия от указанного пользователя.

## 4.3.23 - 2022-09-14
- Исправлено отображение параметров запроса для типов Pageable и Sort в swagger.

## 4.3.22 - 2022-09-13
- Исправлено отображение имени филда в запросе при HttpMessageNotReadableException, когда тип поля - список.

## 4.3.12 - 2022-08-05

### Dependency Upgrades
- Spring Boot 2.7.3
- Spring Doc 1.6.11
- Groovy 3.0.12
- Flyway 9.1.6

## 4.3.0 - 2022-06-28
- Перешли на Spring Boot 2.7.x.

### Dependency Upgrades
- Spring Boot 2.7.1
- Spring Cloud 3.1.3
- jBPM 7.71.0.Final
- Flyway 8.5.13

# Пример описания изменений

[Описание формата](http://keepachangelog.com/)

## 0.0.1 - 2022-06-28

Суть релиза.

### Added
- Добавили фичу.

### Changed
- Изменили поведение.

### Fixed
- Исправили ошибку.

### Documentation
- Расширили документацию.

### Dependency Upgrades
- Зависимость с версией.
