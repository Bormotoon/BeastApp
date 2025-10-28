# Beast Program JSON v1 (Spec)

Добавлено: явное поле `version` (по умолчанию `"v1"`) и уточнена модель данных.

Цель: перенос программ тренировок в Beast App через JSON-формат.

Пример:
```json
{
  "version": "v1",
  "id": "bodybeast-huge",
  "title": "Body Beast: Huge",
  "description": "90-day mass-building program by Sagi Kalev",
  "durationDays": 90,
  "difficulty": "INTERMEDIATE",
  "author": "Sagi Kalev",
  "weightUnit": "kg",
  "phases": [
    {
      "id": "phase-1",
      "name": "Build",
      "durationWeeks": 3,
      "days": [1,2,3,4,5,6,7]
    }
  ],
  "days": [
    { "dayIndex": 1, "title": "Build: Chest/Triceps", "exercisesOrder": ["bench_press", "incline_press"] },
    { "dayIndex": 2, "title": "Build: Back/Biceps", "exercisesOrder": ["pull_up", "barbell_row"] }
  ]
}
```

> Примечание: `weightUnit` опционально: если поле не указано, считается `kg`.

Поля:
- `version` (string): версия формата, default `v1`.
- `id` (string): уникальный id программы.
- `title` (string): название.
- `description` (string): описание (опционально).
- `durationDays` (int): длительность в днях.
- `difficulty` (string): сложность (см. `docs/DATA_MODEL_REFERENCE.md` для enum).
- `author` (string): автор (опционально).
- `weightUnit` (string, optional): `kg` или `lbs` — указывает в каких единицах заданы веса в JSON.
- `phases` (array, optional): структурное деление программы на фазы. Каждая фаза содержит `name`, `durationWeeks`, и опционально список `days` (номеров дней).
- `days` (array): плоский список дней (1..N). Если `phases` отсутствует — импортер создаёт одну фазу по умолчанию, покрывающую все дни.
  - `dayIndex` (int): номер дня (1..durationDays).
  - `title` (string): название дня.
  - `exercisesOrder` (array of string): id упражнений (опционально).

Валидация и импорт:
- Если поле `version` отсутствует — считать `v1`.
- При совпадении `id` выполняется upsert (обновление программы). Дни перезаписываются по `dayIndex`.
- Если `phases` не указаны — создаётся одна фаза по умолчанию.
- `weightUnit` по умолчанию `kg`. Импортер конвертирует веса в единицу пользователя.

См. `docs/IMPORT_FORMAT_V1_SPEC.md` для подробной схемы и JSON Schema валидации.
