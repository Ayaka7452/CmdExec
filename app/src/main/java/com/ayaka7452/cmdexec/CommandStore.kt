package com.ayaka7452.cmdexec

import android.content.Context
import android.content.SharedPreferences

/**
 * 保存 4 条 root 命令（本地 SharedPreferences，自动持久化）。
 */
object CommandStore {
    const val COUNT = 4
    const val EXTRA_SLOT = "extra_slot"
    const val ACTION_RUN = "com.ayaka7452.cmdexec.action.RUN"
    const val MODE_ROOT = "root"
    const val MODE_SH = "sh"

    private const val PREFS_NAME = "cmd_exec"
    private const val KEY_PREFIX = "cmd_"
    private const val NAME_PREFIX = "name_"
    private const val MODE_PREFIX = "mode_"

    fun get(context: Context, slot: Int): String =
        prefs(context).getString(KEY_PREFIX + slot, "") ?: ""

    fun set(context: Context, slot: Int, command: String) {
        prefs(context).edit().putString(KEY_PREFIX + slot, command.trim()).apply()
    }

    fun getName(context: Context, slot: Int): String =
        prefs(context).getString(NAME_PREFIX + slot, "") ?: ""

    fun setName(context: Context, slot: Int, name: String) {
        prefs(context).edit().putString(NAME_PREFIX + slot, name.trim()).apply()
    }

    fun getMode(context: Context, slot: Int): String {
        val value = prefs(context).getString(MODE_PREFIX + slot, MODE_ROOT) ?: MODE_ROOT
        return if (value == MODE_SH) MODE_SH else MODE_ROOT
    }

    fun setMode(context: Context, slot: Int, mode: String) {
        val value = if (mode == MODE_SH) MODE_SH else MODE_ROOT
        prefs(context).edit().putString(MODE_PREFIX + slot, value).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
