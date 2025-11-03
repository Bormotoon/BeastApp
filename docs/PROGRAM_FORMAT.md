# Beast Program JSON Import Format (v1)

Документ описывает финальный формат JSON, который использует Beast App для импорта тренировочных программ. Формат соответствует DTO из `app/src/main/java/com/beast/app/data/importer/ProgramImportModels.kt`.

## Минимальный рабочий пример

```json
{
  "version": "v1",
  "id": "bodybeast-huge",
  "title": "Body Beast: Huge",
  "description": "90 day mass building plan",
  "durationDays": 90,
  "days": [
    {
      "dayIndex": 1,
      "title": "Build: Chest and Triceps"
    }
  ]
}
```

Поле `version` опционально. Если его нет, импортер считает, что файл написан в формате `v1`.

## Структура корневого объекта

| Поле | Тип | Обязательное | Описание |
|------|-----|--------------|----------|
| `version` | string | Нет | Версия схемы. Сейчас поддерживается только `v1`. |
| `id` | string | Нет | Уникальный идентификатор программы. Если отсутствует, приложение сгенерирует slug из `title`. |
| `title` | string | Да | Название программы, отображается в UI. |
| `description` | string | Нет | Краткое описание для экрана программы. |
| `author` | string | Нет | Имя автора или источника программы. |
| `durationDays` | integer (>0) | Да | Число дней в программе. Используется для проверки заполненности календаря. |
| `difficulty` | string | Нет | Строка с уровнем сложности, например `BEGINNER`, `INTERMEDIATE`, `ADVANCED`. Значения завязаны на enum в `DATA_MODEL_REFERENCE.md`. |
| `weightUnit` | string | Нет | `kg` или `lbs`. Определяет единицы измерения в файле. По умолчанию `kg`. |
| `phases` | array of PhaseJson | Нет | Список фаз. Позволяет разбить программу на блоки. |
| `days` | array of DayJson | Да | Список тренировочных дней. Должен содержать уникальные `dayIndex` от 1 до `durationDays`. |

## Объект фазы (PhaseJson)

| Поле | Тип | Обязательное | Описание |
|------|-----|--------------|----------|
| `id` | string | Нет | Уникальный идентификатор фазы. Если пропущено, приложение создаст slug из `name`. |
| `name` | string | Да | Название фазы, например `Build`. |
| `durationWeeks` | integer (>0) | Да | Продолжительность фазы в неделях. Используется для отображения иерархии на экране программы. |
| `days` | array of integer | Нет | Номера дней, которые входят в фазу. Если отсутствует, считается, что дни распределены по порядку. |

Фаза опциональна. Если массив `phases` не указан, приложение создает одну фазу, покрывающую всю программу.

## Объект дня (DayJson)

| Поле | Тип | Обязательное | Описание |
|------|-----|--------------|----------|
| `dayIndex` | integer (>=1) | Да | Порядковый номер дня. Должен быть уникальным внутри программы. |
| `title` | string | Да | Название тренировки или пометка выходного. |
| `description` | string | Нет | Дополнительное описание или план тренировки. |
| `durationMinutes` | integer (>0) | Нет | Ожидаемая продолжительность тренировки. |
| `video_url` | string | Нет | Ссылка на видео с обзором тренировки. |
| `rest_day` | boolean | Нет | Отключает упражнения и помечает день как отдых. |
| `exercisesOrder` | array of string | Нет | Список идентификаторов упражнений в порядке выполнения. ID должны существовать в базе. |
| `notes` | string | Нет | Свободный текст с любыми комментариями. |

> Поля `video_url` и `rest_day` сохраняются в JSON snake_case, так как импортер напрямую отображает их на DTO.

## Расширенный пример

```json
{
  "version": "v1",
  "id": "bodybeast-lean",
  "title": "Body Beast: Lean",
  "description": "Lean Beast variant with additional cardio",
  "author": "Beast App Team",
  "durationDays": 90,
  "difficulty": "INTERMEDIATE",
  "weightUnit": "kg",
  "phases": [
    {
      "id": "phase-build",
      "name": "Build",
      "durationWeeks": 3,
      "days": [1, 2, 3, 4, 5, 6, 7]
    },
    {
      "id": "phase-bulk",
      "name": "Bulk",
      "durationWeeks": 3
    }
  ],
  "days": [
    {
      "dayIndex": 1,
      "title": "Build: Chest and Triceps",
      "description": "Dynamic set training for upper body",
      "durationMinutes": 50,
      "video_url": "https://example.com/bodybeast/chest-tris.mp4",
      "exercisesOrder": [
        "bench_press_flat",
        "incline_db_press",
        "dumbbell_fly",
        "close_grip_pushup"
      ],
      "notes": "Use progressive overload"
    },
    {
      "dayIndex": 2,
      "title": "Build: Back and Biceps",
      "durationMinutes": 48,
      "exercisesOrder": ["pull_up_wide", "barbell_row", "reverse_curl"]
    },
    {
      "dayIndex": 7,
      "title": "Rest and Mobility",
      "rest_day": true,
      "notes": "Light stretching and foam rolling"
    }
  ]
}
```

## Правила валидации

- `title` не может быть пустой строкой.
- `durationDays` должен быть строго больше нуля.
- Список `days` обязательный и не может быть пустым.
- Каждое поле `dayIndex` должно быть больше либо равно 1.
- `dayIndex` должны быть уникальными. Дубликаты вызывают ошибку импорта.
- Если указаны `phases`, каждая должна содержать минимум `name` и `durationWeeks`.
- Значение `weightUnit`, если присутствует, должно быть `kg` или `lbs`.
- Если `rest_day = true`, приложение игнорирует `exercisesOrder` при расчете объема.

## Соответствие с примерами в репозитории

- `docs/examples/valid_program.json` содержит полностью корректный файл, который можно использовать как шаблон.
- `docs/examples/invalid_program.json` и другие файлы в каталоге `docs/examples/` демонстрируют типовые ошибки (дубликаты дней, невалидные единицы измерения, неверный формат). Используйте их, чтобы проверить поведение импорта.

## Рекомендации по подготовке JSON

- Убедитесь, что общее количество дней совпадает с `durationDays`.
- Пользуйтесь единым стилем идентификаторов (строчные буквы и подчеркивания или дефисы). Пример: `bench_press_flat`.
- Для строк используйте UTF-8 без BOM.
- Перед импортом проверяйте JSON через валидатор для быстрого обнаружения синтаксических ошибок.
- Если программа подразумевает несколько фаз, старайтесь указывать массив `days` внутри каждой фазы, чтобы UI мог подсветить соответствующие недели.

## История изменений

- **2025-11-03:** Сформирована финальная версия спецификации v1 и приведена в соответствие с реализацией импорта.
