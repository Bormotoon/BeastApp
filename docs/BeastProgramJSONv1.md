# Beast Program JSON v1 (Draft)

Цель: перенос программ тренировок в Beast App через простой JSON-формат.

Пример:
```json
{
  "id": "bodybeast-huge",
  "title": "Body Beast: Huge",
  "description": "90-day mass-building program by Sagi Kalev",
  "durationDays": 90,
  "difficulty": "Intermediate/Advanced",
  "author": "Sagi Kalev",
  "days": [
    { "dayIndex": 1, "title": "Build: Chest/Triceps", "exercisesOrder": ["bench_press", "incline_press"] },
    { "dayIndex": 2, "title": "Build: Back/Biceps", "exercisesOrder": ["pull_up", "barbell_row"] }
  ]
}
```

Поля:
- id (string): уникальный id программы.
- title (string): название.
- description (string): описание (опционально).
- durationDays (int): длительность в днях.
- difficulty (string): сложность (опционально).
- author (string): автор (опционально).
- days (array): список дней.
  - dayIndex (int): номер дня (1..durationDays).
  - title (string): название дня.
  - exercisesOrder (array of string): id упражнений (опционально).

Валидация:
- dayIndex уникален в рамках программы, 1..durationDays.
- Размер days ≤ durationDays (можно импортировать частично, недостающие дни будут пустыми/Rest).

Импорт:
- При импорте программа с тем же id обновляется (upsert), дни перезаписываются (upsert по id = `${programId}-day-${dayIndex}`).
- exercisesOrder сохраняется как есть (сопоставление с Exercise по id выполняется отдельно).

Версионирование:
- Поле version может быть добавлено позже. Текущее значение: v1.

