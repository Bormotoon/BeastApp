# FitTrack90 — Полная спецификация проекта (KMM)

Универсальное фитнес-приложение с поддержкой 90-дневных и других программ тренировок. Первая цель — полнофункциональное Android-приложение, затем возможна адаптация под iOS.

---

## 🎯 Цели

* Поддержка 90-дневных и других программ (BodyBeast, P90X, кастомные).
* Быстрое логирование силовых тренировок.
* Ведение прогресса: фото (front/side/back), замеры тела, графики.
* Календарь с планом/напоминаниями.
* Возможность создавать собственные программы.
* Синхронизация и бэкапы (Google Drive / локально).
* Приватность: по умолчанию локальное хранение.

---

## 🏗️ Архитектура проекта

### Общий подход

* **Kotlin Multiplatform Mobile (KMM)**.
* Общий модуль `shared`: бизнес-логика, модели данных, use cases.
* Android UI: Jetpack Compose + Material 3.
* БД: Room (Android), позже SQLDelight (для iOS).
* DI: Hilt.
* Уведомления: WorkManager.
* Сеть: Ktor (в будущем для синхронизации).

### 📂 Структура проекта

```
FitTrack90/
 ├─ androidApp/          # Android UI (Compose)
 │   ├─ src/main/java/.../ui   # Экраны, ViewModels
 │   ├─ src/main/java/.../di   # Hilt модули
 │   ├─ src/main/java/.../utils
 │   └─ MainActivity.kt
 │
 ├─ shared/              # KMM модуль (Kotlin)
 │   ├─ src/commonMain/  # Общий код (модели, use cases, репозитории)
 │   ├─ src/androidMain/ # Android-специфично (Room, Camera API, Storage)
 │   └─ src/iosMain/     # (будет позже) iOS реализация
 │
 └─ build.gradle.kts
```

---

## 🗄️ Модели данных (commonMain)

```kotlin
Program(id, title, description, durationDays, difficulty, thumbnail, tags, author)
WorkoutDay(id, programId, dayIndex, title, durationEstimate, exercisesOrder[])
Exercise(id, name, primaryMuscles[], equipment[], demoVideoUrl, defaultSets, defaultReps, restSec)
SetLog(id, workoutDayId, exerciseId, setIndex, reps, weight, rpe, timestamp)
WorkoutLog(id, programId, dayIndex, date, completed, notes)
Measurement(id, date, weight, chest, waist, hips, additionalFields)
PhotoProgress(id, date, angle, fileUri)
UserProfile(id, displayName, unitsMetric, timezone, preferences)
```

---

## 📱 Основные экраны (androidApp)

### Onboarding

* Выбор единиц измерения.
* Выбор цели (bulk/cut/tone).
* Базовые параметры.
* Выбор программы (готовая/кастомная).

### Home / Dashboard

* Прогресс текущей программы (% выполнено).
* Следующая тренировка (карточка + кнопка Start).
* Быстрые действия: Фото, Замеры, Быстрый лог.
* История последних тренировок.

### Programs

* Сетка карточек: название, длительность, цель.
* Фильтры и поиск.
* Import / Favorite / Preview.

### Program Detail

* Hero area: картинка/видео, описание.
* CTA «Start».
* Tabs: Overview | Schedule | Exercises.
* Schedule: неделя → дни → карточки тренировок.

### Daily Workout / Session

* Экран текущей тренировки.
* Блок упражнения: название, демо, target sets, поля reps/вес.
* +/− кнопки для ввода.
* Done Set → таймер отдыха.
* Свайпы для переключения упражнений.
* Summary в конце.

### Exercise Detail

* Видео/анимация, техника, мышцы.
* История выполнений.
* Alternatives.

### Progress

* Фото (front/side/back), сравнение.
* Замеры (вес, талия и пр.).
* Графики (вес/замеры).
* Экспорт PDF/CSV.

### Calendar

* Месячный вид: статус тренировок (done/skipped/rest).
* Tap → быстрые действия.
* Sync с Google Calendar.

### Custom Program Builder

* Drag & drop недель/дней.
* Возможность импортировать шаблоны.
* Сохранение public/private.

### Settings

* Units, язык, напоминания.
* Backup (локально/Google Drive).
* Dark mode.
* Data export/delete.

---

## 🎨 UI/UX (Material Design 3)

* Bottom Navigation: Home, Programs, Calendar, Progress, Profile.
* AppBar: заголовок + search/sync.
* FAB: Start workout / Quick log.
* Cards для элементов.
* Цветовая схема с динамическим акцентом.
* Доступность: контраст ≥ 4.5:1.
* Touch targets ≥ 48dp.

---

## 🚀 MVP Scope (Android)

1. Логирование тренировок.
2. Импорт 90-дневной программы.
3. Фото и замеры прогресса.
4. Календарь + напоминания.
5. Backup локально.

---

## 📋 Issues для GitHub

### Data Layer (shared/commonMain + androidMain)

* [ ] Создать сущность `Program` (data class + DAO + тесты).
* [ ] Создать сущность `WorkoutDay` (data class + DAO).
* [ ] Создать сущность `Exercise` (data class + DAO).
* [ ] Создать `WorkoutLog` и `SetLog` (DAO + связи).
* [ ] Создать `Measurement` и `PhotoProgress`.
* [ ] Настроить Room Database и Hilt-модуль.

### UI Layer (androidApp)

* [ ] OnboardingScreen (единицы, цель, программа).
* [ ] HomeScreen (прогресс, следующая тренировка, история).
* [ ] ProgramsScreen (список карточек + фильтры).
* [ ] ProgramDetailScreen (Overview | Schedule | Exercises).
* [ ] WorkoutSessionScreen (логирование сетов, таймер).
* [ ] ExerciseDetailScreen (видео, техника, история).
* [ ] ProgressScreen (фото, замеры, графики).
* [ ] CalendarScreen (месячный вид, статусы).
* [ ] SettingsScreen (единицы, язык, бэкап, тёмная тема).

### Utils & Infra

* [ ] Камера + галерея (сохранение фото в локальное хранилище).
* [ ] Экспорт/импорт JSON (программы и логи).
* [ ] Backup локально (файл + восстановление).
* [ ] WorkManager для уведомлений.
* [ ] Навигация: sealed class Routes.
* [ ] Темизация Material 3 (светлая/тёмная).

---

## 🛠️ Инструкции для Copilot

* Создавать каждый экран как `@Composable` с ViewModel.
* Использовать `sealed class` для навигации.
* Автогенерировать boilerplate: DAO, ViewModel, репозитории.
* Разбивать задачи на мелкие issue.
* README и Issues держать в актуальном виде.

---

## 📈 Roadmap (после MVP)

* Google Drive sync.
* Wear OS поддержка.
* Расширенные графики.
* PDF отчёты.
* Premium (подписка, отключение рекламы).
* iOS-приложение (SwiftUI UI + shared бизнес-логика).
