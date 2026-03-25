package com.health.companion.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.local.ProfileCache
import com.health.companion.data.remote.api.UpdateMedCardRequest
import com.health.companion.data.remote.api.UpdateProfileRequest
import com.health.companion.data.repositories.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ProfileFormState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    // Basic
    val name: String = "",
    val nickname: String = "",
    val age: String = "",
    val language: String = "ru",
    val avatarEmoji: String? = null,
    val email: String = "",
    // Med card
    val height: String = "",
    val weight: String = "",
    val bloodGroup: String = "",
    val allergies: String = "",
    val diseases: String = "",
    // UI
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class ProfileScreenViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val profileCache: ProfileCache
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileFormState())
    val state: StateFlow<ProfileFormState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Try new endpoint first
            repo.getProfileMe().onSuccess { p ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        name = p.name,
                        nickname = p.nickname ?: "",
                        age = p.age?.toString() ?: "",
                        language = p.language ?: "ru",
                        avatarEmoji = p.avatarEmoji,
                        email = p.email ?: "",
                        height = p.medCard?.height?.toString() ?: "",
                        weight = p.medCard?.weight?.toString() ?: "",
                        bloodGroup = p.medCard?.bloodGroup ?: "",
                        allergies = p.medCard?.allergies?.joinToString(", ") ?: "",
                        diseases = p.medCard?.diseases?.joinToString(", ") ?: ""
                    )
                }
                return@launch
            }

            // Fallback: old endpoint → pre-fill name/email, show empty form
            Timber.d("profile/me unavailable, falling back to /profile")
            repo.getProfile()
                .onSuccess { legacy ->
                    _state.update {
                        it.copy(isLoading = false, name = legacy.user.name, email = legacy.user.email)
                    }
                }
                .onFailure {
                    Timber.w(it, "both profile endpoints failed, showing empty form")
                    _state.update { s -> s.copy(isLoading = false) }
                }
        }
    }

    // ── Field setters ──────────────────────────────────────

    fun onName(v: String)        { _state.update { it.copy(name = v) } }
    fun onNickname(v: String)    { _state.update { it.copy(nickname = v) } }
    fun onAge(v: String)         { if (v.all { c -> c.isDigit() }) _state.update { it.copy(age = v) } }
    fun onLanguage(v: String)    { _state.update { it.copy(language = v) } }
    fun onAvatar(v: String?)     { _state.update { it.copy(avatarEmoji = v) } }
    fun onHeight(v: String)      { if (v.all { c -> c.isDigit() }) _state.update { it.copy(height = v) } }
    fun onWeight(v: String)      { if (v.all { c -> c.isDigit() }) _state.update { it.copy(weight = v) } }
    fun onBloodGroup(v: String)  { _state.update { it.copy(bloodGroup = v) } }
    fun onAllergies(v: String)   { _state.update { it.copy(allergies = v) } }
    fun onDiseases(v: String)    { _state.update { it.copy(diseases = v) } }

    fun consumeMessage() { _state.update { it.copy(message = null) } }

    // ── Save ───────────────────────────────────────────────

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val profileReq = UpdateProfileRequest(
                name = s.name.takeIf { it.isNotBlank() },
                nickname = s.nickname.takeIf { it.isNotBlank() },
                age = s.age.toIntOrNull(),
                language = s.language.takeIf { it.isNotBlank() },
                avatarEmoji = s.avatarEmoji
            )
            val medReq = UpdateMedCardRequest(
                height = s.height.toIntOrNull(),
                weight = s.weight.toIntOrNull(),
                bloodGroup = s.bloodGroup.takeIf { it.isNotBlank() },
                allergies = s.allergies.split(",").map { it.trim() }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() },
                diseases = s.diseases.split(",").map { it.trim() }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
            )

            val profileResult = async { repo.updateProfileMe(profileReq) }
            val medResult = async { repo.updateMedCard(medReq) }

            val pOk = profileResult.await().isSuccess
            val mOk = medResult.await().isSuccess

            // Обновляем кэш профиля чтобы "Ещё" показал свежие данные мгновенно
            if (pOk) {
                profileCache.update(
                    name = s.name.takeIf { it.isNotBlank() },
                    nickname = s.nickname.takeIf { it.isNotBlank() },
                    email = s.email.takeIf { it.isNotBlank() },
                    avatarEmoji = s.avatarEmoji
                )
            }

            _state.update {
                it.copy(
                    isSaving = false,
                    message = when {
                        pOk && mOk -> "Сохранено"
                        pOk -> "Профиль сохранён, медкарта — ошибка"
                        mOk -> "Медкарта сохранена, профиль — ошибка"
                        else -> "Не удалось сохранить"
                    }
                )
            }
        }
    }
}
