package com.ayaka7452.cmdexec

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 后台执行 root 命令的服务，不显示任何界面。
 * 由桌面长按菜单的中转页启动，执行完成后自动停止。
 */
class RootCommandService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val slot = intent?.getIntExtra(CommandStore.EXTRA_SLOT, -1) ?: -1
        val command = if (slot in 0 until CommandStore.COUNT) {
            CommandStore.get(this, slot)
        } else {
            ""
        }
        val mode = CommandStore.getMode(this, slot)

        if (command.isBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        Thread {
            RootExec.runSync(command, mode)
            stopSelf(startId)
        }.start()

        return START_NOT_STICKY
    }
}
