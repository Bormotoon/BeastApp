package com.beast.shared.usecase

import com.beast.shared.reminders.ReminderScheduler

class ScheduleReminderUseCase(private val scheduler: ReminderScheduler) {
    operator fun invoke(timestampMillis: Long, title: String, message: String) {
        scheduler.scheduleAt(timestampMillis, title, message)
    }
}

