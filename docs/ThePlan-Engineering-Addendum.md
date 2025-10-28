# Дополнение к ThePlan: Инженерный план реализации (Engineering Deep Dive)

Дата: 2025-10-26

Этот документ расширяет ThePlan детальными техническими аспектами реализации: архитектура, данные, движок тренировки, мультимедиа, безопасность, локализация, доступность, аналитика, тестирование, CI/CD, документация и метрики качества.

---

## A. Архитектура приложения
- Паттерн: MVVM + Clean Architecture (слои: presentation/domain/data), однонаправленный поток данных (UDF).
- DI: Hilt (Android) или Koin; модульные графы зависимостей по фичам.
- Навигация: Jetpack Navigation (Compose), deep links, SavedStateHandle.
- Асинхронность: Kotlin Coroutines + Flow (cancel-safe таймеры, structured concurrency).
- Модульность:
  - feature-dashboard, feature-program, feature-workout, feature-history, feature-progress, feature-profile
  - core-ui (дизайн‑система), core-common (утилиты), data (Room/репозитории), domain (use-cases)
- KMM: старт Android-first (Room, DataStore). Интерфейсы совместимы с потенциальной миграцией домена/данных в shared/SQLDelight.

## B. Данные и хранение
- Room-таблицы: programs, phases, workouts, exercises, workout_exercises (M2M), workout_logs, set_logs, user_profile, body_measurements, personal_records, favorites, notes, goals, achievements, photos, nutrition_entries.
- Индексы: по датам (workout_logs.date), по exerciseId/workoutId (set_logs), по name (exercises), по programId/phaseId.
- Миграции: версияция v1→v2 (PR), v2→v3 (индексы, новые поля). Тесты миграций.
- Настройки: DataStore (единицы, тема, интервалы отдыха по типам сетов, авто‑таймер, звук/вибрация, язык). 
- Импорт программ:
  - JSON Schema v1 для PROGRAM_FORMAT (валидация, обратная совместимость, поле version).
  - YAML/CSV: конвертация в JSON модель перед импортом.
  - Мастер импорта: загрузка → предпросмотр → корректировки → импорт.

## C. Движок активной тренировки
- State machine: Idle → InProgress → Resting → Paused → Completed/Aborted; события: Start, NextSet, NextExercise, Pause, Resume, RestStart, RestEnd, Finish, Abort.
- Типы сетов: Single, Super, Giant, Multi, Force(5x5/10s), Progressive(15‑12‑8‑…‑15), Combo, Circuit, Tempo(TUT). Strategy/Policy для расчёта целей и таймингов.
- Таймеры и фон:
  - Надёжность: Foreground Service + persistent notification; при закрытии приложения таймеры продолжают работать.
  - Doze/ограничения батареи: при необходимости точные будильники, иначе coroutines + Handler/Choreographer.
  - Keep screen on (во время подходов/отдыха), блокировка сна экрана опционально.
- Автозаполнение и рекомендации веса: подстановка прошлых значений; эвристики прогрессии (пост‑MVP).
- Edge cases: поворот/убийство процесса, входящий звонок, низкая память, перезапуск во время отдыха, пропуски подходов.

## D. Мультимедиа
- Видео техники: ExoPlayer, кеш, офлайн превью/GIF, mini‑player; Picture‑in‑Picture.
- Аудио: Audio Focus (ducking/pausing), звуки таймера, вибро‑сигналы; пользовательский выбор звуков.

## E. Приватность и безопасность
- Фото прогресса: Scoped Storage/MediaStore, приватные каталоги, READ_MEDIA_IMAGES (API 33+), биометрический доступ к разделу фото.
- Экспорт/импорт: JSON/CSV/Excel (post‑MVP), ShareSheet, шифрование экспортов по желанию.
- Шифрование локальных данных: EncryptedFile/EncryptedSharedPreferences; опционально SQLCipher для Room.
- Ключи/токены: Android Keystore. Безопасное хранение.

## F. Локализация (i18n) и доступность (a11y)
- RU/EN, plurals, формат даты/времени, единицы (кг↔lbs), RTL‑совместимость.
- A11y: contentDescription, порядок фокуса, ≥48dp touch targets, контраст; тесты TalkBack/Accessibility Scanner.

## G. Аналитика и телеметрия
- Firebase Analytics/Crashlytics/Performance (или аналог), события: старт/пауза/завершение тренировки, PR, импорт/ошибки импортера, напоминания, streak.
- Opt‑out в настройках; сбор минимум PII.

## H. Тестирование и качество
- Unit: use‑cases (импорт/парсинг, расчёты volume/1RM), репозитории, state machine тренировки.
- Инструментальные: Room/миграции, Foreground Service таймера, сценарии активной тренировки.
- UI (Compose): smoke‑тесты основных флоу; Snapshot (светлая/тёмная тема, крупный шрифт).
- Линтеры: detekt, ktlint/spotless, Android Lint; форматирование и pre‑commit hooks.

## I. CI/CD и релизы
- GitHub Actions: сборка, юнит/UI тесты, линтеры, артефакты (APK/AAB), отчёты.
- SemVer + Conventional Commits; CHANGELOG генерация; релизные заметки.
- Publish (post‑MVP): подпись, fastlane/gradle Play Publisher.

## J. Документация и соглашения
- ARCHITECTURE.md, CONTRIBUTING.md, CODE_OF_CONDUCT.md, SECURITY.md, CHANGELOG.md.
- PROGRAM_FORMAT.schema.json + примеры (валид/невалид) в docs/.

## K. Performance budget
- Запуск < 3 с; переходы < 200 мс; задержка таймера < 50 мс.
- Crash‑free users > 99.5%; APK < 50 МБ без медиа.

## L. Этапы внедрения
- Этап 1 (MVP Core): модели/БД, импорт JSON, активная тренировка + таймер, история, базовые графики.
- Этап 2: календарь, PR, улучшенный Dashboard, экспорт, онбординг.
- Этап 3: мультимедиа (ExoPlayer, PiP), аналитика/Crashlytics, локализация EN.
- Этап 4: библиотека упражнений, уведомления, приватность/биометрия, фото‑прогресс.
- Этап 5 (Community): мастер импорта/превью, маркетплейс программ, облако.

