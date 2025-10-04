# 📋 TODO — FitTrack90 (KMM Android-first)

Пошаговый план разработки приложения FitTrack90.  
Статусы: ⬜ todo | 🟡 in progress | ✅ done | ❌ blocked

---

## 1. Инициализация проекта
- ✅ [1.1] Создать KMM-проект в Android Studio (шаблон `KMM Application`).
- ✅ [1.2] Настроить **Gradle** и плагины (KMM, Serialization, Coroutines).
- ✅ [1.3] Добавить общие зависимости (`shared/commonMain`: Ktor, Serialization, Coroutines).
- ✅ [1.4] Добавить Android-зависимости (`shared/androidMain`: Room, WorkManager).
- ✅ [1.5] Настроить androidApp (Jetpack Compose, Material 3, Hilt, Navigation Compose).

---

## 2. Архитектура проекта
- ✅ [2.1] Создать структуру каталогов (`androidApp/`, `shared/`, `src/...`).
- ✅ [2.2] Определить модели (`Program`, `WorkoutDay`, `Exercise`, `SetLog`, `WorkoutLog`).
- ✅ [2.3] Создать интерфейсы репозиториев.
- ✅ [2.4] Реализовать репозитории в `androidMain` через Room.

---

## 3. Бизнес-логика (shared/commonMain)
- 🟡 [3.1] Реализовать Use Cases:
    - 🟡 Загрузка программ тренировок
    - 🟡 Планирование 90-дневного цикла
    - 🟡 Логирование тренировок и сетов
    - 🟡 Подсчёт прогресса пользователя
    - ⬜ Напоминания
- 🟡 [3.2] Написать unit-тесты бизнес-логики.

---

## 4. UI Android (androidApp)
- ✅ [4.1] Настроить навигацию (`Navigation Compose`).
- 🟡 [4.2] Реализовать экраны:
    - ✅ Onboarding (выбор единиц, выбор стартовой программы из списка)
    - ✅ Program List
    - ✅ Program Detail
    - ✅ Dashboard (кнопка «Начать новую тренировку», карточка Next Workout, история последних тренировок)
    - ✅ Workout Detail
    - ✅ Exercise Detail (детальный просмотр упражнения с демо-видео)
    - ✅ Progress Tracking (график веса, замеры, фото прогресса)
    - ✅ Calendar (месячный вид с планом и статусами)
    - ✅ Profile & Settings (смена акцентного цвета, единицы измерения)
- 🟡 [4.3] Настроить темы (Material 3, тёмная/светлая, настраиваемый акцент — ✅).
- ✅ [4.4] Добавить основные компоненты: BottomNavigation ✅, Advanced Settings ✅
- ✅ [4.5] Advanced Mode — полная настройка приложения (видимость экранов, значения по умолчанию, заметки)

---

## 5. Сервисы и фоновые задачи
- ✅ [5.1] Уведомления о тренировках (WorkManager).
- ⬜ [5.2] Напоминания о питании/воде (опционально).
- ✅ [5.3] Экспорт/импорт данных (CSV).
    - ✅ Импорт программ в CSV формате (Beast Program Import Format v1) — полная документация и реализация

---

## 6. Тестирование
- 🟡 [6.1] Unit-тесты `shared/commonMain`.
- ⬜ [6.2] Инструментальные тесты Android UI.
- ⬜ [6.3] E2E сценарии (например, прохождение недели программы).

---

## 7. Релиз Android
- ⬜ [7.1] Подготовить иконки, сплэш-экран.
- ⬜ [7.2] Настроить подпись APK.
- ⬜ [7.3] Собрать **release build**.
- ⬜ [7.4] Протестировать на реальных устройствах.
- ⬜ [7.5] Загрузить в **Google Play (internal testing)**.

---

## 8. Дальнейшие шаги (после Android MVP)
- ⬜ [8.1] Добавить `iosMain` реализацию (SQLDelight вместо Room).
- ⬜ [8.2] Реализовать iOS UI (SwiftUI).
- ⬜ [8.3] Настроить CI/CD (GitHub Actions).
- ⬜ [8.4] Расширить функционал (синхронизация, Apple Health, Google Fit).
