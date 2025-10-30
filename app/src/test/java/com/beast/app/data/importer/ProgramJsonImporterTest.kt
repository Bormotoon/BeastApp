package com.beast.app.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProgramJsonImporterTest {

    private val importer = ProgramJsonImporter()

    @Test
    fun `parse valid json succeeds`() {
        val json = """
            {
              "version": "v1",
              "title": "Sample CSV Program",
              "description": "Simple CSV program for testing conversion",
              "author": "Test",
              "durationDays": 7,
              "weightUnit": "kg",
              "days": [
                {
                  "dayIndex": 1,
                  "title": "Chest & Triceps",
                  "description": "Build phase - chest and triceps",
                  "durationMinutes": 45,
                  "video_url": "https://example.com/chest.mp4",
                  "rest_day": false,
                  "exercisesOrder": [
                    "bench-press",
                    "incline-db-press"
                  ],
                  "notes": "Focus on form"
                },
                {
                  "dayIndex": 2,
                  "title": "Back & Biceps",
                  "description": "Build phase - back and biceps",
                  "durationMinutes": 45,
                  "video_url": "https://example.com/back.mp4",
                  "rest_day": false,
                  "exercisesOrder": [
                    "deadlift",
                    "row"
                  ]
                },
                {
                  "dayIndex": 7,
                  "title": "Rest Day",
                  "description": "Recovery day",
                  "durationMinutes": 0,
                  "rest_day": true,
                  "notes": "Active recovery"
                }
              ]
            }
        """.trimIndent()

        val model = importer.parse(json)
        assertEquals("Sample CSV Program", model.title)
        assertEquals(7, model.durationDays)
        assertEquals(3, model.days.size)
        assertEquals(1, model.days[0].dayIndex)
        assertEquals("Chest & Triceps", model.days[0].title)
    }

    @Test
    fun `duplicate dayIndex throws`() {
        val json = """
            {
              "title": "Program with duplicate",
              "durationDays": 2,
              "days": [
                { "dayIndex": 1, "title": "Day 1" },
                { "dayIndex": 1, "title": "Day 1 duplicate" }
              ]
            }
        """.trimIndent()

        assertThrows(ProgramJsonValidationException::class.java) {
            importer.parse(json)
        }
    }
}

