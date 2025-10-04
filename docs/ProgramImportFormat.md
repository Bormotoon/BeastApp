# 📥 Beast App - Program Import Format

## 🚀 Quick Start

**Recommended for beginners:** Use the ready-made template file:
- **Location:** `docs/import-template.csv`
- **Features:**
  - Pre-formatted structure with headers
  - Detailed instructions in Russian
  - Example data you can modify
  - Tips for popular programs (Body Beast, P90X, etc.)
  - Comment lines (starting with #) that are automatically ignored

Simply copy the template, fill in your workout data, and import!

---

## Overview

Beast App supports importing custom fitness programs via CSV files. This allows users to add their own 90-day (or any duration) workout programs with complete schedules.

---

## 🗂️ CSV File Format

### File Structure

The CSV file should contain the following columns (in order):

| Column | Required | Type | Description | Example |
|--------|----------|------|-------------|---------|
| `day` | ✅ Yes | Integer | Day number (1-based) | `1` |
| `title` | ✅ Yes | String | Workout title/name | `Chest & Triceps` |
| `description` | ❌ No | String | Workout description | `Build phase - focus on mass` |
| `duration` | ❌ No | Integer | Estimated duration in minutes | `45` |
| `exercises` | ❌ No | String | Semicolon-separated exercise IDs | `bench-press;incline-db-press` |
| `video_url` | ❌ No | String | URL to workout demo video | `https://example.com/video.mp4` |
| `rest_day` | ❌ No | Boolean | Is this a rest day? (true/false) | `false` |
| `notes` | ❌ No | String | Additional notes | `Use progressive overload` |

### File Metadata

The first few lines of the CSV can contain metadata (optional):

```csv
# PROGRAM_NAME: Body Beast: Huge
# DESCRIPTION: 90-day muscle building program
# AUTHOR: Beachbody
# DIFFICULTY: Advanced
# DURATION_DAYS: 90
# TAGS: Bodybuilding, Mass Building, Strength
```

Metadata lines start with `#` and are followed by `KEY: Value` pairs.

---

## 📄 Example CSV File

### Example 1: Simple Format

```csv
day,title,description,duration
1,Chest & Triceps,Build phase - chest and triceps workout,45
2,Back & Biceps,Build phase - back and biceps workout,45
3,Shoulders,Build phase - shoulder workout,40
4,Legs,Build phase - leg workout,50
5,Chest & Triceps,Build phase - chest and triceps workout,45
6,Back & Biceps,Build phase - back and biceps workout,45
7,Rest Day,Recovery day,0
```

### Example 2: Full Format with Metadata

```csv
# PROGRAM_NAME: Body Beast: Huge
# DESCRIPTION: A comprehensive 90-day muscle-building program
# AUTHOR: Beachbody
# DIFFICULTY: Advanced
# DURATION_DAYS: 90
# TAGS: Bodybuilding, Mass Building, Strength Training

day,title,description,duration,exercises,video_url,rest_day,notes
1,Chest & Triceps,Build phase - chest and triceps,45,bench-press;incline-db-press;cable-flyes;tricep-dips;overhead-extension,https://example.com/chest-tri.mp4,false,Focus on form
2,Back & Biceps,Build phase - back and biceps,45,deadlift;bent-over-row;pull-ups;barbell-curl;hammer-curl,https://example.com/back-bi.mp4,false,Progressive overload
3,Shoulders,Build phase - shoulders,40,military-press;lateral-raise;front-raise;rear-delt-flye;shrugs,https://example.com/shoulders.mp4,false,Control the weight
4,Legs,Build phase - legs,50,squats;leg-press;leg-curl;leg-extension;calf-raise,https://example.com/legs.mp4,false,Go deep on squats
5,Chest & Triceps,Build phase - chest and triceps,45,bench-press;incline-db-press;cable-flyes;tricep-dips;overhead-extension,https://example.com/chest-tri.mp4,false,Focus on form
6,Back & Biceps,Build phase - back and biceps,45,deadlift;bent-over-row;pull-ups;barbell-curl;hammer-curl,https://example.com/back-bi.mp4,false,Progressive overload
7,Rest Day,Recovery day,0,,,,true,Active recovery - light stretching
```

---

## 🔧 Exercise IDs

When specifying exercises in the `exercises` column, use semicolon-separated exercise IDs. These IDs should match exercises in the Beast App database.

### Common Exercise IDs

| Exercise ID | Exercise Name |
|-------------|---------------|
| `bench-press` | Barbell Bench Press |
| `incline-db-press` | Incline Dumbbell Press |
| `cable-flyes` | Cable Flyes |
| `tricep-dips` | Tricep Dips |
| `overhead-extension` | Overhead Tricep Extension |
| `deadlift` | Barbell Deadlift |
| `bent-over-row` | Bent Over Barbell Row |
| `pull-ups` | Pull-ups |
| `barbell-curl` | Barbell Curl |
| `hammer-curl` | Hammer Curl |
| `military-press` | Military Press |
| `lateral-raise` | Lateral Raise |
| `front-raise` | Front Raise |
| `rear-delt-flye` | Rear Delt Flye |
| `shrugs` | Barbell Shrugs |
| `squats` | Barbell Squat |
| `leg-press` | Leg Press |
| `leg-curl` | Leg Curl |
| `leg-extension` | Leg Extension |
| `calf-raise` | Standing Calf Raise |

If an exercise ID is not found in the database, it will be ignored during import.

---

## 📹 Video URLs

### Supported Formats

- **YouTube**: `https://www.youtube.com/watch?v=VIDEO_ID`
- **Vimeo**: `https://vimeo.com/VIDEO_ID`
- **Direct links**: `https://example.com/video.mp4`

Videos are optional and can be viewed from the workout detail screen.

---

## 🚀 Import Process

### Step 1: Prepare CSV File

1. Create a CSV file with the required columns
2. Add metadata at the top (optional)
3. Fill in workout schedule day by day
4. Save the file (e.g., `my-program.csv`)

### Step 2: Import in App

1. Open Beast App
2. Go to **Programs** tab
3. Tap the **"Import Program"** button (⬆️ icon)
4. Select your CSV file
5. Review the import preview
6. Confirm import

### Step 3: Verify Import

1. The new program will appear in your Programs list
2. You can set it as your active program
3. All workout days will be available in the Calendar

---

## ⚠️ Important Notes

### Validation Rules

- **Day numbers** must be sequential (1, 2, 3, ...)
- **Title** is required for each day
- **Duration** must be a positive integer (in minutes)
- **Rest day** flag overrides exercises (rest days won't log exercises)
- Duplicate day numbers will cause import to fail

### Exercise Handling

- If an exercise ID is not found, it will be skipped
- You can import programs without exercises (manual entry during workout)
- New exercises can be added to the database separately

### File Size Limits

- Maximum program duration: **365 days**
- Maximum file size: **5 MB**
- Maximum exercises per workout: **20**

---

## 🎯 Best Practices

1. **Test with small programs first** - Start with a 7-day program to verify format
2. **Use consistent naming** - Keep workout titles consistent for better tracking
3. **Include descriptions** - Help users understand workout focus
4. **Add video links** - Enhance user experience with demonstration videos
5. **Mark rest days clearly** - Use the `rest_day` flag for proper calendar display

---

## 📚 Example Programs

### P90X Classic Schedule (90 days)

```csv
# PROGRAM_NAME: P90X Classic
# DESCRIPTION: Tony Horton's revolutionary 90-day workout program
# AUTHOR: Beachbody
# DIFFICULTY: Advanced
# DURATION_DAYS: 90

day,title,description,duration
1,Chest & Back,Push-ups and pull-ups workout,53
2,Plyometrics,Jump training for explosive power,60
3,Shoulders & Arms,Biceps triceps and shoulders,60
4,Yoga X,Yoga flexibility and balance,90
5,Legs & Back,Lower body and back workout,60
6,Kenpo X,Cardio kickboxing workout,60
7,Rest or X Stretch,Recovery day,60
... (continue for 90 days)
```

### Insanity Schedule (60 days)

```csv
# PROGRAM_NAME: Insanity
# DESCRIPTION: Shaun T's max interval training program
# AUTHOR: Beachbody
# DIFFICULTY: Expert
# DURATION_DAYS: 60

day,title,description,duration
1,Fit Test,Initial fitness assessment,30
2,Plyometric Cardio Circuit,High intensity cardio,42
3,Cardio Power & Resistance,Cardio with resistance,40
... (continue for 60 days)
```

---

## 🔄 Future Enhancements

Planned features for future versions:

- [ ] JSON import format for more complex programs
- [ ] Exercise library import/export
- [ ] Program templates marketplace
- [ ] Automatic video download
- [ ] Phase-based program structure
- [ ] Custom workout builder UI

---

## 📞 Support

For issues with program import, please check:

1. CSV file format is correct
2. All required columns are present
3. Day numbers are sequential
4. File encoding is UTF-8
5. No special characters in titles

For additional help, visit: [Beast App GitHub Issues](https://github.com/Bormotoon/BeastApp/issues)

---

**Version:** 1.0  
**Last Updated:** 2025-10-04  
**Format Specification:** Beast Program Import Format v1

