# 🤖 AGENTS.md — Beast App (FitTrack90, KMM Android-first)

## ⚡ Для GPT-5-Codex: Минимум, но достаточно

Это — правила для **GPT-5-Codex в режиме Low/Medium**. Меньше инструкций = быстрее и лучше код.

---

## 🎯 Суть проекта (One-liner)

**Beast App** — Android трекер тренировок Body Beast (90 дней, 3 фазы, Dynamic Set Training). Импорт программ (JSON), MVVM, Room DB, Material Design 3, Kotlin.

---

## 🧩 Стек (Копируй и используй)

```kotlin
// Language & Framework
Language: Kotlin (coroutines, Flow)
UI: Jetpack Compose (future) / Material Design 3
Architecture: MVVM + Repository pattern
Database: Room (SQLite)
Navigation: Jetpack Navigation Component
Testing: JUnit, Mockito

// Key Libraries (build.gradle)
androidx.room:room-runtime:2.6.0
androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0
androidx.navigation:navigation-fragment-ktx:2.7.0
com.google.code.gson:gson:2.10.1
com.github.PhilJay:MPAndroidChart:v3.1.0
```

---

## 📁 Структура проекта (Типичная)

```
app/
├── ui/
│   ├── screens/              # Каждый экран = отдельный Composable
│   │   ├── DashboardScreen.kt
│   │   ├── ActiveWorkoutScreen.kt
│   │   ├── CalendarScreen.kt
│   │   └── ...
│   ├── components/           # Переиспользуемые UI компоненты
│   │   ├── WorkoutCard.kt
│   │   ├── ExerciseRow.kt
│   │   └── TimerWidget.kt
│   └── theme/                # Material Design 3 tema
│       ├── Color.kt
│       ├── Typography.kt
│       └── Theme.kt
├── viewmodel/
│   ├── DashboardViewModel.kt
│   ├── ActiveWorkoutViewModel.kt
│   └── ...
├── data/
│   ├── db/                   # Room entities & DAOs
│   │   ├── entities/
│   │   ├── dao/
│   │   └── Database.kt
│   ├── repository/           # Repository pattern
│   │   ├── ProgramRepository.kt
│   │   ├── WorkoutRepository.kt
│   │   └── ...
│   └── model/                # DTOs, models
├── domain/
│   ├── usecase/              # UseCase classes (optional, for MVP 2.0)
│   └── calculator/           # Бизнес-логика (1RM, volume, etc.)
├── utils/
│   ├── ImportParser.kt       # JSON парсер
│   ├── Converters.kt         # Type converters
│   └── Extensions.kt          # Kotlin extensions
└── MainActivity.kt           # Entry point
```

---

## 🧠 Коротко о кодировании для Codex

**Пиши:**
- Простые, четкие классы (Data Classes для entities)
- Функции 10-20 строк максимум
- Очевидные имена (не `x`, `calc`, `proc`)

**НЕ пиши:**
- Preambles (длинные комментарии перед кодом)
- Подробные многострочные комментарии
- Лишние `/**` dokumentation (Codex это видит)

**Пример (ХОРОШО для Codex):**
```kotlin
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val exerciseType: ExerciseType, // STRENGTH, CARDIO, ISOMETRIC
    val primaryMuscleGroup: String,
    val equipment: List<String>,
    val instructions: String,
    val videoUrl: String?
)

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE primaryMuscleGroup = :muscleGroup")
    suspend fun getByMuscleGroup(muscleGroup: String): List<Exercise>

    @Insert
    suspend fun insert(exercise: Exercise)
}
```

---

## 🎨 Принципы UI

- **Material Design 3**: используй `MaterialTheme`, `Surface`, `Button`, `TextField` из `androidx.compose.material3`
- **Spacing**: 8dp базовый шаг (используй `Modifier.padding(8.dp)`)
- **Цвета**: из `Color.kt` (тема)
- **Иконки**: Material Icons из `androidx.compose.material:material-icons-extended`
- **Адаптивность**: `BoxWithConstraints`, `Row/Column` для разных размеров экрана

---

## ⚙️ Как структурировать импорт программ

**PROGRAM_FORMAT.md (JSON Schema) — One Example:**

```json
{
  "program_name": "Body Beast - Huge Beast",
  "duration_days": 90,
  "phases": [
    {
      "name": "Build",
      "weeks": [1, 2, 3],
      "workouts": [
        {
          "id": "bb_build_1",
          "day_of_week": 1,
          "name": "Build: Chest/Tris",
          "duration_minutes": 45,
          "muscle_groups": ["chest", "triceps"],
          "exercises": [
            {
              "exercise_id": "barbell_bench",
              "name": "Barbell Bench Press",
              "set_type": "PROGRESSIVE_SET",
              "target_reps": "15,12,8,8,12,15",
              "rest_seconds": 90,
              "equipment": ["barbell", "bench"]
            }
          ]
        }
      ]
    }
  ]
}
```

**Парсер (ImportParser.kt):**
```kotlin
object ImportParser {
    fun parseProgram(json: String): Program = Gson().fromJson(json, Program::class.java)
}
```

---

## 📋 Быстрая Контрольная Таблица для Codex Промптов

### Когда писать промпт для Codex:

| Что писать | Промпт |
|---|---|
| Создать Entity + DAO | Напиши Room Entity для WorkoutLog и его DAO с методом getByDate |
| Кейс использования | Реализуй ImportProgramUseCase с парсером JSON |
| UI экран | Напиши Composable для DashboardScreen с карточкой прогресса |
| ViewModel | Создай DashboardViewModel с LiveData для currentDay и currentPhase |
| Extension функция | Напиши функцию расчёта Estimated 1RM по формуле Brzycki |
| Таймер отдыха | Реализуй RestTimerViewModel с CountDownLatch и автоматическим переходом |

**ВАЖНО**: Всегда попроси Codex использовать AGENTS.md для контекста.

---

## 🔄 Workflow разработки (для вас)

1. **Планирование (Claude Sonnet 4.5):** архитектура, диаграммы, data flow
2. **Кодирование основ (GPT-5-Codex Low):** entities, DAO, repositories (очень быстро)
3. **UI экраны (GPT-5-Codex Low):** RecyclerView/Composable компоненты
4. **Логика (GPT-5-Codex Medium):** активные тренировки, таймер, расчеты
5. **Интеграция (GPT-5-Codex Medium):** импорт, синхронизация, архитектура
6. **Исправления (GPT-5-Codex Low):** bugs, оптимизация после тестирования

---

## 🛡️ Как НЕ сломать проект

- **Всегда работай с ветками** (feature/dashboard, feature/active-workout)
- **Antes каждого большого изменения**: убедись, что существующие экраны ещё работают
- **Room миграции**: используй `migrate(1, 2) { // логика }`
- **State management**: LiveData/Flow в ViewModel, не в Composable
- **Тестирование**: минимум 1 unit-тест на usecase / repository

---

## 📌 Типичные ошибки (Avoid)

❌ Пытаться сохранить всё в одну Room Entity
❌ UI логика в Activity/Fragment вместо ViewModel
❌ Синхронный Room запрос (`get...()` вместо `suspend` или Flow)
❌ Хранить context в ViewModel
❌ Забыть про null-safety (всё должно быть `?` или `!!` осознанно)
❌ Создавать Composable функции без `@Composable` аннотации

---

## ✅ Главное правило для этого проекта

**Просто пиши код, следуя структуре выше. Codex поймёт. Если не нравится результат — переформулируй промпт короче (не добавляй слова, убирай). Когда есть сомнение — спроси Claude.**

