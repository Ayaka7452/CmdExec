package com.ayaka7452.cmdexec

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

/**
 * 桌面长按菜单点击后执行对应命令的页面。
 */
class ShortcutActivity : AppCompatActivity() {

    private var slot = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shortcut)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        slot = intent.getIntExtra(CommandStore.EXTRA_SLOT, 0)
            .coerceIn(0, CommandStore.COUNT - 1)

        findViewById<MaterialButton>(R.id.btn_rerun).setOnClickListener { run() }
        findViewById<MaterialButton>(R.id.btn_open_main).setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            finish()
        }

        run()
    }

    private fun run() {
        val command = CommandStore.get(this, slot)
        findViewById<TextView>(R.id.tv_shortcut_title).text =
            getString(R.string.cmd_title_slot, slot + 1)
        findViewById<TextView>(R.id.tv_command).text =
            if (command.isBlank()) getString(R.string.command_empty_hint) else command

        val status = findViewById<TextView>(R.id.tv_status)
        val output = findViewById<TextView>(R.id.tv_output)
        val rerun = findViewById<MaterialButton>(R.id.btn_rerun)

        if (command.isBlank()) {
            status.text = getString(R.string.status_shortcut_empty)
            output.text = ""
            return
        }

        rerun.isEnabled = false
        status.text = getString(R.string.status_running)
        output.text = ""

        RootExec.run(command) { result ->
            runOnUiThread {
                rerun.isEnabled = true
                status.text = when {
                    result.timedOut -> getString(R.string.status_timeout)
                    result.exitCode == 0 -> getString(R.string.status_ok)
                    result.exitCode != null ->
                        getString(R.string.status_failed, result.exitCode)
                    else -> getString(R.string.status_failed_no_code)
                }
                output.text =
                    if (result.output.isBlank()) getString(R.string.dialog_no_output) else result.output
            }
        }
    }
}
