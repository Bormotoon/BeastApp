package com.beast.shared.reminders

interface ReminderScheduler {
    fun scheduleAt(timestampMillis: Long, title: String, message: String)
}

