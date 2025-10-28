# Формат файла для импорта программ

```json
{
  "program_name": "Body Beast - Huge Beast",
  "description": "90-дневная силовая программа на массу",
  "author": "Sagi Kalev",
  "duration_days": 90,
  "days": [
    {
      "day": 1,
      "name": "Build: Chest/Tris",
      "muscle_groups": ["chest", "triceps"],
      "image_url": "https://ex.com/build_chest_img.jpg",
      "workout": [
        {
          "exercise": "Bench Press",
          "set_type": "Progressive Set",
          "target_reps": [15, 12, 8, 8, 12, 15],
          "equipment": ["barbell", "bench"],
          "video_url": "https://youtube.com/bench_press_tutorial"
        }
      ]
    }
  ]
}
```

> Примечание: В реальном файле `days` содержит объекты для каждого тренировочного дня (1..N). В примере выше показан один день; добавляйте дополнительные объекты в массив `days` по аналогии.

**Минимальный набор:**

- `program_name` — название программы
- `description` — коротко о программе
- `duration_days` — длительность
- `days` — массив тренировочных дней, каждый со списком упражнений
- Для упражнения: `exercise`, `set_type`, `target_reps`, `equipment`, дополнительные медиа (`image_url`, `video_url`)

**Детальная документация и примеры в репозитории**
