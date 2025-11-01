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
        profileDao.insertBodyWeight(entry)
    }

    suspend fun insertMeasurement(entry: BodyMeasurementEntity) = withContext(Dispatchers.IO) {
        profileDao.insertMeasurement(entry)
    }

    suspend fun insertPersonalRecord(entry: PersonalRecordEntity) = withContext(Dispatchers.IO) {
        profileDao.insertPersonalRecord(entry)
    }

    suspend fun getPersonalRecordDates(epochDays: List<Long>): Set<Long> = withContext(Dispatchers.IO) {
        if (epochDays.isEmpty()) return@withContext emptySet()
        profileDao.getPersonalRecordDates(epochDays).toSet()
    }
}

