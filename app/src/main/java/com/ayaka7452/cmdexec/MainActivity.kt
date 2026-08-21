package com.ayaka7452.cmdexec

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private val editIds = intArrayOf(
        R.id.edit_cmd0, R.id.edit_cmd1, R.id.edit_cmd2, R.id.edit_cmd3
    )
    private val statusIds = intArrayOf(
        R.id.status_cmd0, R.id.status_cmd1, R.id.status_cmd2, R.id.status_cmd3
    )
    private val runIds = intArrayOf(
        R.id.btn_run0, R.id.btn_run1, R.id.btn_run2, R.id.btn_run3
    )
    private val editTexts = arrayOfNulls<EditText>(CommandStore.COUNT)
    private val statusViews = arrayOfNulls<TextView>(CommandStore.COUNT)
    private val runButtons = arrayOfNulls<MaterialButton>(CommandStore.COUNT)
    private val running = BooleanArray(CommandStore.COUNT)
    private val saveHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        for (slot in 0 until CommandStore.COUNT) {
            val edit = findViewById<EditText>(editIds[slot])
            val status = findViewById<TextView>(statusIds[slot])
            val run = findViewById<MaterialButton>(runIds[slot])
            editTexts[slot] = edit
            statusViews[slot] = status
            runButtons[slot] = run

            edit.setText(CommandStore.get(this, slot))
            edit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) = scheduleSave()
            })
            run.setOnClickListener { runCommand(slot) }
            updateStatus(slot, getString(R.string.status_ready))
        }

        ShortcutHelper.publish(this)
    }

    override fun onResume() {
        super.onResume()
        // 从快捷菜单返回时，把最新保存的命令同步回输入框
        for (slot in 0 until CommandStore.COUNT) {
            val edit = editTexts[slot] ?: continue
            val saved = CommandStore.get(this, slot)
            if (edit.text.toString() != saved) {
                edit.setText(saved)
                edit.setSelection(edit.text.length)
            }
        }
        ShortcutHelper.publish(this)
    }

    private fun scheduleSave() {
        saveHandler.removeCallbacksAndMessages(null)
        saveHandler.postDelayed({
            var changed = false
            for (slot in 0 until CommandStore.COUNT) {
                val text = editTexts[slot]?.text?.toString()?.trim().orEmpty()
                if (text != CommandStore.get(this, slot)) {
                    CommandStore.set(this, slot, text)
                    changed = true
                }
            }
            if (changed) {
                ShortcutHelper.publish(this)
            }
        }, 400L)
    }

    private fun runCommand(slot: Int) {
        val edit = editTexts[slot] ?: return
        val command = edit.text.toString().trim()
        CommandStore.set(this, slot, command)

        if (command.isEmpty()) {
            updateStatus(slot, getString(R.string.status_empty))
            return
        }
        if (running[slot]) return

        setRunning(slot, true)
        updateStatus(slot, getString(R.string.status_running))
        RootExec.run(command) { result ->
            runOnUiThread {
                setRunning(slot, false)
                updateStatus(
                    slot,
                    when {
                        result.timedOut -> getString(R.string.status_timeout)
                        result.exitCode == 0 -> getString(R.string.status_ok)
                        result.exitCode != null ->
                            getString(R.string.status_failed, result.exitCode)
                        else -> getString(R.string.status_failed_no_code)
                    }
                )
                showOutputDialog(slot, command, result)
            }
        }
    }

    private fun setRunning(slot: Int, isRunning: Boolean) {
        running[slot] = isRunning
        runButtons[slot]?.isEnabled = !isRunning
    }

    private fun updateStatus(slot: Int, text: String) {
        statusViews[slot]?.text = text
    }

    private fun showOutputDialog(slot: Int, command: String, result: RootExec.Result) {
        val content = buildString {
            append(getString(R.string.dialog_command, command))
            append('\n')
            append(getString(R.string.dialog_exit_code, result.exitCode?.toString() ?: "—"))
            append("\n----------------\n")
            append(if (result.output.isBlank()) getString(R.string.dialog_no_output) else result.output)
        }
        val body = TextView(this).apply {
            text = content
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(56, 36, 56, 24)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_title, slot + 1))
            .setView(body)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
