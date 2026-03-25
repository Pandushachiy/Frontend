package com.health.companion.presentation.screens.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.*
import com.health.companion.data.repositories.SkillsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillsViewModel @Inject constructor(
    private val repo: SkillsRepository
) : ViewModel() {

    private val _skills = MutableStateFlow<List<SkillDTO>>(emptyList())
    val skills: StateFlow<List<SkillDTO>> = _skills.asStateFlow()

    private val _enabledCount = MutableStateFlow(0)
    val enabledCount: StateFlow<Int> = _enabledCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _configSkill = MutableStateFlow<SkillDTO?>(null)
    val configSkill: StateFlow<SkillDTO?> = _configSkill.asStateFlow()

    /** skill_name::key → revealed full value */
    private val _revealedKeys = MutableStateFlow<Map<String, String>>(emptyMap())
    val revealedKeys: StateFlow<Map<String, String>> = _revealedKeys.asStateFlow()

    /** skill_name::key currently being revealed (loading) */
    private val _revealingKey = MutableStateFlow<String?>(null)
    val revealingKey: StateFlow<String?> = _revealingKey.asStateFlow()

    /** Editing single config key: Pair(skillName, key) */
    private val _editingConfigKey = MutableStateFlow<Pair<String, String>?>(null)
    val editingConfigKey: StateFlow<Pair<String, String>?> = _editingConfigKey.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _revealedKeys.value = emptyMap()
            repo.getSkills()
                .onSuccess { resp ->
                    _skills.value = resp.skills
                    _enabledCount.value = resp.enabledCount
                }
                .onFailure { _error.value = it.localizedMessage }
            _isLoading.value = false
        }
    }

    fun toggle(name: String) {
        val skill = _skills.value.find { it.name == name } ?: return
        viewModelScope.launch {
            repo.toggle(name, !skill.enabled)
                .onSuccess { enabled ->
                    _skills.value = _skills.value.map {
                        if (it.name == name) it.copy(enabled = enabled) else it
                    }
                    _enabledCount.value = _skills.value.count { it.enabled }
                }
                .onFailure { _error.value = it.localizedMessage }
        }
    }

    fun delete(name: String) {
        viewModelScope.launch {
            repo.delete(name)
                .onSuccess {
                    _skills.value = _skills.value.filterNot { it.name == name }
                    _enabledCount.value = _skills.value.count { it.enabled }
                }
                .onFailure { _error.value = it.localizedMessage }
        }
    }

    fun showConfig(skill: SkillDTO) { _configSkill.value = skill }
    fun hideConfig() { _configSkill.value = null }

    fun startEditingKey(skillName: String, key: String) {
        _editingConfigKey.value = skillName to key
    }

    fun cancelEditingKey() {
        _editingConfigKey.value = null
    }

    private val _revealError = MutableStateFlow<String?>(null)
    val revealError: StateFlow<String?> = _revealError.asStateFlow()

    fun clearRevealError() { _revealError.value = null }

    fun revealConfigKey(skillName: String, key: String) {
        val compositeKey = "$skillName::$key"
        if (_revealedKeys.value.containsKey(compositeKey)) {
            _revealedKeys.value = _revealedKeys.value - compositeKey
            return
        }
        viewModelScope.launch {
            _revealingKey.value = compositeKey
            _revealError.value = null
            repo.revealConfigKey(skillName, key)
                .onSuccess { value ->
                    _revealedKeys.value = _revealedKeys.value + (compositeKey to value)
                }
                .onFailure {
                    _revealError.value = compositeKey
                }
            _revealingKey.value = null
        }
    }

    fun setConfigKey(skillName: String, key: String, value: String) {
        viewModelScope.launch {
            repo.setConfigKey(skillName, key, value)
                .onSuccess { resp ->
                    _skills.value = _skills.value.map { skill ->
                        if (skill.name == skillName) {
                            val newConfigSet = skill.configSet.toMutableMap()
                            newConfigSet[key] = ConfigKeyInfoDTO(
                                filled = true,
                                maskedValue = resp.maskedValue,
                                required = skill.configSet[key]?.required ?: true
                            )
                            skill.copy(
                                config = skill.config + (key to value),
                                configSet = newConfigSet,
                                isConfigured = true
                            )
                        } else skill
                    }
                    _editingConfigKey.value = null
                    _configSkill.value = null
                    val compositeKey = "$skillName::$key"
                    _revealedKeys.value = _revealedKeys.value - compositeKey
                }
                .onFailure { _error.value = it.localizedMessage }
        }
    }

    fun clearError() { _error.value = null }
}
