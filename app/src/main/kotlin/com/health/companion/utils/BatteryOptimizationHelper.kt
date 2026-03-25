package com.health.companion.utils

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import timber.log.Timber

/**
 * Handles battery optimization exemption requests across all Android OEMs.
 *
 * Why this matters:
 *  • Stock Android Doze: handled by FOREGROUND_SERVICE_TYPE_DATA_SYNC + WakeLock
 *  • ColorOS (OPPO/Realme): has its own "Battery Saver" + "Autostart" that kills
 *    services independently of Doze, even foreground ones.
 *  • MIUI (Xiaomi): similar aggressive killing
 *  • OneUI (Samsung): less aggressive but still needs exemption for long-lived WS
 *
 * Strategy:
 *  1. Request standard Android battery optimization exemption (works everywhere)
 *  2. On known OEM ROMs, additionally show a dialog pointing to the OEM-specific
 *     "Autostart" or "Background app activity" settings page.
 *
 * Call [requestIfNeeded] from MainActivity.onResume() — it's idempotent and only
 * shows UI once per session.
 */
object BatteryOptimizationHelper {

    private var shownThisSession = false

    fun requestIfNeeded(activity: Activity) {
        if (shownThisSession) return
        val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(activity.packageName)) {
            // Already exempt — still check OEM autostart for ColorOS/MIUI
            maybeShowOemAutostartHint(activity)
            return
        }
        shownThisSession = true
        showBatteryOptimizationDialog(activity)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Standard Android battery optimization exemption
    // ─────────────────────────────────────────────────────────────────────────

    private fun showBatteryOptimizationDialog(activity: Activity) {
        try {
            AlertDialog.Builder(activity)
                .setTitle("Разреши работу в фоне")
                .setMessage(
                    "Чтобы напоминания приходили вовремя даже когда приложение свёрнуто, " +
                    "нужно отключить ограничения батареи для AI Health Companion.\n\n" +
                    "Нажми «Разрешить» и выбери «Не ограничивать»."
                )
                .setPositiveButton("Разрешить") { _, _ ->
                    requestBatteryExemption(activity)
                }
                .setNegativeButton("Позже", null)
                .show()
        } catch (e: Exception) {
            Timber.w(e, "BatteryOptimizationHelper: could not show dialog")
            requestBatteryExemption(activity)
        }
    }

    private fun requestBatteryExemption(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
            Timber.d("BatteryOptimizationHelper: opened battery exemption dialog")
        } catch (e: Exception) {
            // Fallback: open general battery settings
            try {
                activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e2: Exception) {
                Timber.w(e2, "BatteryOptimizationHelper: cannot open battery settings")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OEM-specific autostart hints (ColorOS, MIUI, Samsung, etc.)
    // ─────────────────────────────────────────────────────────────────────────

    private fun maybeShowOemAutostartHint(activity: Activity) {
        val oemIntent = getOemAutostartIntent() ?: return
        if (!isIntentAvailable(activity, oemIntent)) return
        if (shownThisSession) return
        shownThisSession = true

        val oemName = getOemName()
        try {
            AlertDialog.Builder(activity)
                .setTitle("Автозапуск ($oemName)")
                .setMessage(
                    "На устройствах $oemName нужно дополнительно разрешить автозапуск, " +
                    "иначе напоминания не будут приходить в фоне.\n\n" +
                    "Открыть настройки автозапуска?"
                )
                .setPositiveButton("Открыть") { _, _ ->
                    try {
                        activity.startActivity(oemIntent)
                    } catch (e: Exception) {
                        Timber.w(e, "BatteryOptimizationHelper: cannot open OEM autostart")
                    }
                }
                .setNegativeButton("Позже", null)
                .show()
        } catch (e: Exception) {
            Timber.w(e, "BatteryOptimizationHelper: could not show OEM dialog")
        }
    }

    private fun getOemAutostartIntent(): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            // OPPO / Realme / OnePlus (ColorOS)
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
            // OnePlus OxygenOS (some models use ColorOS now, some still OxygenOS)
            manufacturer.contains("oneplus") -> Intent().apply {
                component = ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
            }
            // Xiaomi / Redmi / POCO (MIUI / HyperOS)
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            // Huawei / Honor (EMUI / HarmonyOS)
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            // Samsung (OneUI) — less aggressive, but still useful
            manufacturer.contains("samsung") -> Intent().apply {
                component = ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            }
            // Vivo (FuntouchOS / OriginOS)
            manufacturer.contains("vivo") -> Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
            else -> null
        }
    }

    private fun getOemName(): String = when (Build.MANUFACTURER.lowercase()) {
        "oppo" -> "OPPO/ColorOS"
        "realme" -> "Realme/ColorOS"
        "oneplus" -> "OnePlus"
        "xiaomi", "redmi" -> "Xiaomi/MIUI"
        "huawei" -> "Huawei/EMUI"
        "honor" -> "Honor"
        "samsung" -> "Samsung"
        "vivo" -> "Vivo"
        else -> Build.MANUFACTURER
    }

    private fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        return try {
            context.packageManager.resolveActivity(intent, 0) != null
        } catch (e: Exception) {
            false
        }
    }
}
