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

    private const val PREFS_NAME = "cmd_exec"
    private const val KEY_PREFIX = "cmd_"

    fun get(context: Context, slot: Int): String =
        prefs(context).getString(KEY_PREFIX + slot, "") ?: ""

    fun set(context: Context, slot: Int, command: String) {
        prefs(context).edit().putString(KEY_PREFIX + slot, command.trim()).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
