package com.beast.app.data.repo

import com.beast.app.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for profile-related operations: user profile, body weight, measurements, personal records.
 */
class ProfileRepository(
    private val db: BeastDatabase
) {
    private val profileDao = db.profileDao()

    suspend fun getProfile(): UserProfileEntity? = withContext(Dispatchers.IO) {
        profileDao.getProfile()
    }

    suspend fun upsertProfile(profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        profileDao.upsertProfile(profile)
    }

    suspend fun insertBodyWeight(entry: BodyWeightEntryEntity) = withContext(Dispatchers.IO) {
        profileDao.replaceBodyWeight(entry)
    }

    suspend fun getBodyWeightHistory(limit: Int? = null): List<BodyWeightEntryEntity> = withContext(Dispatchers.IO) {
        val entries = profileDao.getBodyWeightEntries()
        if (limit != null && limit > 0) entries.takeLast(limit) else entries
    }

    suspend fun insertMeasurement(entry: BodyMeasurementEntity) = withContext(Dispatchers.IO) {
        profileDao.replaceMeasurement(entry)
    }

    suspend fun getMeasurements(limit: Int? = null): List<BodyMeasurementEntity> = withContext(Dispatchers.IO) {
        val entries = profileDao.getBodyMeasurements()
        if (limit != null && limit > 0) entries.takeLast(limit) else entries
    }

    suspend fun insertProgressPhoto(photo: ProgressPhotoEntity) = withContext(Dispatchers.IO) {
        profileDao.insertProgressPhoto(photo)
    }

    suspend fun deleteProgressPhoto(photoId: Long) = withContext(Dispatchers.IO) {
        profileDao.deleteProgressPhoto(photoId)
    }

    suspend fun getProgressPhotos(): List<ProgressPhotoEntity> = withContext(Dispatchers.IO) {
        profileDao.getProgressPhotos()
    }

    suspend fun insertPersonalRecord(entry: PersonalRecordEntity) = withContext(Dispatchers.IO) {
        profileDao.insertPersonalRecord(entry)
    }

    suspend fun getPersonalRecordDates(epochDays: List<Long>): Set<Long> = withContext(Dispatchers.IO) {
        if (epochDays.isEmpty()) return@withContext emptySet()
        profileDao.getPersonalRecordDates(epochDays).toSet()
    }

    suspend fun getTopPersonalRecords(limit: Int): List<PersonalRecordWithExercise> = withContext(Dispatchers.IO) {
        profileDao.getTopPersonalRecords(limit)
    }
}

