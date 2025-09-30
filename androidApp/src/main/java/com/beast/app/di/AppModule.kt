package com.beast.app.di

import android.content.Context
import com.beast.shared.data.room.*
import com.beast.shared.repository.*
import com.beast.shared.reminders.ReminderScheduler
import com.beast.shared.reminders.ReminderSchedulerImpl
import com.beast.shared.data.prefs.SettingsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FitDatabase = DatabaseFactory(context).create()

    @Provides
    @Singleton
    fun provideProgramRepository(db: FitDatabase): ProgramRepository = ProgramRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideWorkoutDayRepository(db: FitDatabase): WorkoutDayRepository = WorkoutDayRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideExerciseRepository(db: FitDatabase): ExerciseRepository = ExerciseRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideWorkoutLogRepository(db: FitDatabase): WorkoutLogRepository = WorkoutLogRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideMeasurementRepository(db: FitDatabase): MeasurementRepository = MeasurementRepositoryImpl(db)

    @Provides
    @Singleton
    fun providePhotoProgressRepository(db: FitDatabase): PhotoProgressRepository = PhotoProgressRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideReminderScheduler(@ApplicationContext context: Context): ReminderScheduler = ReminderSchedulerImpl(context)

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository = SettingsRepositoryImpl(context)
}
