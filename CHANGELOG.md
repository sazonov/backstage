# Change Log

## 4.3.28 - 2022-10-04
- Добавлен метод для получения информации о вложениях по коллекции идентификаторов

## 4.3.27 - 2022-10-03
- В JdkSerializationRedisSerializerNoSerializationEx добавлена обработка ClassNotFoundException
- Исправлено наименование методов проверки существования записей справочника в RemoteDictData

## 4.3.26 - 2022-09-26
- В RemoteDictData добавлены отсутствующие методы из DictDataService
- В модели CRUD реквестов dictItem добавлены конструкторы

## 4.3.25 - 2022-09-22
- Методы запуска Runnable и Callable в новом потоке без потери аутентификации - static

## 4.3.24 - 2022-09-22
- Добавлена поддержка настройки аутентификации в SecurityContext;
- Добавлена возможность запускать Runnable и Callable в новом потоке, не теряя аутентификацию

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

Перешли на Spring Boot 2.7.x.

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
- Добавили фичу

### Changed
- Изменили поведение

### Fixed
- Исправили ошибку

### Documentation
- Расширили документацию

### Dependency Upgrades
- Обновили зависимость