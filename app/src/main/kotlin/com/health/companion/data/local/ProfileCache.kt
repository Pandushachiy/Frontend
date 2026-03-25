package com.health.companion.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единый персистентный кэш профиля пользователя.
 *
 * Логика работы:
 *  - Данные читаются синхронно при старте → UI показывает кэш мгновенно
 *  - После сетевого запроса вызывается [update] → кэш обновляется
 *  - При выходе из аккаунта вызывается [clear]
 *
 * Инжектируется через Hilt как @Singleton во все ViewModel/Repository,
 * которым нужен профиль. Никаких дублирующих SharedPreferences в VM!
 */
@Singleton
class ProfileCache @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("profile_cache_v2", Context.MODE_PRIVATE)

    // ── Getters (synchronous, safe to call on main thread) ─────────

    fun getName(): String = prefs.getString(KEY_NAME, null).orEmpty()

    fun getDisplayName(): String =
        prefs.getString(KEY_NICKNAME, null)
            ?.takeIf { it.isNotBlank() }
            ?: prefs.getString(KEY_NAME, null)
            ?: ""

    fun getEmail(): String = prefs.getString(KEY_EMAIL, null).orEmpty()

    fun getAvatarEmoji(): String? = prefs.getString(KEY_AVATAR_EMOJI, null)

    // ── Update (call after successful network fetch) ────────────────

    fun update(
        name: String? = null,
        nickname: String? = null,
        email: String? = null,
        avatarEmoji: String? = null
    ) {
        prefs.edit().apply {
            if (name != null) putString(KEY_NAME, name)
            if (nickname != null) putString(KEY_NICKNAME, nickname)
            if (email != null) putString(KEY_EMAIL, email)
            if (avatarEmoji != null) putString(KEY_AVATAR_EMOJI, avatarEmoji)
        }.apply()
    }

    // ── Clear (call on logout) ──────────────────────────────────────

    fun clear() {
        prefs.edit().clear().apply()
    }

    // ── Keys ────────────────────────────────────────────────────────

    companion object {
        private const val KEY_NAME         = "name"
        private const val KEY_NICKNAME     = "nickname"
        private const val KEY_EMAIL        = "email"
        private const val KEY_AVATAR_EMOJI = "avatar_emoji"
    }
}
