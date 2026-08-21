package com.ayaka7452.cmdexec

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * 发布/更新桌面长按菜单里的 4 个快捷项。
 * 快捷项 ID 固定（cmd_0..cmd_3），命令变化时原位刷新。
 */
object ShortcutHelper {

    fun publish(context: Context) {
        val shortcuts = (0 until CommandStore.COUNT).map { slot ->
            val command = CommandStore.get(context, slot)
            val label = context.getString(R.string.cmd_short_label, slot + 1)
            val longLabel = if (command.isBlank()) {
                context.getString(R.string.cmd_short_long_empty, slot + 1)
            } else {
                context.getString(R.string.cmd_short_long_filled, slot + 1, command.take(24))
            }
            ShortcutInfoCompat.Builder(context, "cmd_$slot")
                .setShortLabel(label)
                .setLongLabel(longLabel)
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(
                    Intent(context, ShortcutActivity::class.java).apply {
                        action = CommandStore.ACTION_RUN
                        putExtra(CommandStore.EXTRA_SLOT, slot)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}
