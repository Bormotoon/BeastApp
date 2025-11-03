# BeastApp

BeastApp - Android-приложение для программы Body Beast. Оно помогает выбрать нужный цикл (Huge или Lean), планировать тренировки на 90 дней и отслеживать прогресс в одном месте.

## Ключевые возможности
- Онбординг и выбор стартовой программы с датой начала и системой измерения веса.
- Главный экран с прогрессом по дням, фазам и быстрым доступом к сегодняшней тренировке.
- Календарь с цветовой индикацией статуса тренировки и bottom sheet с деталями дня.
- Активная тренировка с таблицей подходов, авто-таймером отдыха и поддержкой специальных сетов (Super, Giant, Progressive, Force).
- Экран завершения тренировки с итоговой статистикой и зафиксированными рекордами.
- История, графики прогресса и профиль пользователя с динамикой веса.
- Импорт шаблонов программ из JSON и локальное резервное копирование данных.

## Технологический стек
- Kotlin 1.9+, Coroutines, Flow.
- Jetpack Compose + Material Design 3, Navigation Component.
- MVVM, ViewModel, Repository pattern.
- Room (SQLite) с TypeConverter для коллекций и enum-ов.
- Gson для импорта программ.
- MPAndroidChart и Kizitonwose Calendar для визуализаций.

## Требования
- Android Studio Koala или новее (рекомендуется последняя версия).
- Android SDK: minSdk = 24, target/compileSdk = 35.
- JDK 17.

## Структура проекта
- `app/src/main/java/com/beast/app/ui` - Compose-экраны и компоненты.
- `app/src/main/java/com/beast/app/viewmodel` - ViewModel слоя представления.
- `app/src/main/java/com/beast/app/data` - Room сущности, DAO, репозитории и импортёр программ.
- `app/src/main/java/com/beast/app/domain` - бизнес-логика и use case (по мере расширения).
- `docs/` - спецификации форматов, дорожная карта и TODO.

## Быстрый старт в Android Studio
1. Откройте папку `BeastApp` через **File > Open**.
2. Дождитесь синхронизации Gradle и загрузки зависимостей.
3. Создайте AVD (например, Pixel 7 на Android 14/15) в **Tools > Device Manager**.
4. Запустите конфигурацию `app` на выбранном устройстве.

## Сборка из командной строки
```bash
./gradlew assembleDebug
```

APK будет находиться в `app/build/outputs/apk/debug/`.

### Юнит- и инструментальные тесты
```bash
./gradlew test
./gradlew connectedDebugAndroidTest
```

## Полезная документация
- `docs/PROGRAM_FORMAT.md` - схема JSON для импорта программ.
- `docs/DATA_MODEL_REFERENCE.md` - модели данных и enum-ы.
- `docs/TODO.md`, `docs/ROADMAP.md`, `docs/ThePlan.md` - актуальные планы и статус задач.
- `docs/examples/` - примеры CSV/JSON для импорта и проверки валидации.

## Примечания
- Gradle Wrapper уже настроен (`gradle-wrapper.jar` включён в репозиторий).
- Для ускорения сборки рекомендуется запускать `./gradlew --daemon assembleDebug` после первой синхронизации.
