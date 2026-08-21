package com.ayaka7452.cmdexec

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 桌面长按菜单的中转页：透明、不渲染任何 UI，
 * 收到命令槽位后立即转交后台服务执行，并立刻结束自身。
 */
class ShortcutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val slot = intent.getIntExtra(CommandStore.EXTRA_SLOT, 0)
            .coerceIn(0, CommandStore.COUNT - 1)

        startService(
            Intent(this, RootCommandService::class.java)
                .putExtra(CommandStore.EXTRA_SLOT, slot)
        )
        finish()
    }
}
