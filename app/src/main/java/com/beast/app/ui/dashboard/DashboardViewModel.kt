package com.beast.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beast.app.R
import com.beast.app.data.db.DatabaseProvider
import com.beast.app.data.repo.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseProvider.get(application.applicationContext)
    private val profileRepository = ProfileRepository(database)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { loadState() }
    }

    private suspend fun loadState() {
        val locale = Locale.getDefault()
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
        val today = LocalDate.now()
        val formattedDate = today.format(dateFormatter).replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
        }

        val profile = profileRepository.getProfile()
        val profileName = profile?.name?.takeIf { it.isNotBlank() }
        val initials = profileName?.parseInitials()
        val title = getApplication<Application>().getString(R.string.app_name)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            topBar = TopBarState(
                title = title,
                subtitle = formattedDate,
                profileName = profileName,
                profileInitials = initials
            )
        )
    }

    private fun String.parseInitials(): String {
        val parts = trim().split(' ').filter { it.isNotBlank() }
        if (parts.isEmpty()) return first().uppercase()
        return parts.take(2).joinToString(separator = "") { it.first().uppercase() }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val topBar: TopBarState = TopBarState()
)

data class TopBarState(
    val title: String = "",
    val subtitle: String = "",
    val profileName: String? = null,
    val profileInitials: String? = null
)
