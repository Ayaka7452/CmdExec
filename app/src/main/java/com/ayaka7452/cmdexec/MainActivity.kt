package com.ayaka7452.cmdexec

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private val nameEdits = arrayOfNulls<EditText>(CommandStore.COUNT)
    private val cmdEdits = arrayOfNulls<EditText>(CommandStore.COUNT)
    private val modeGroups = arrayOfNulls<MaterialButtonToggleGroup>(CommandStore.COUNT)
    private val runButtons = arrayOfNulls<MaterialButton>(CommandStore.COUNT)
    private val clearButtons = arrayOfNulls<MaterialButton>(CommandStore.COUNT)
    private val statusViews = arrayOfNulls<TextView>(CommandStore.COUNT)
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

        val container = findViewById<LinearLayout>(R.id.command_container)
        for (slot in 0 until CommandStore.COUNT) {
            val row = layoutInflater.inflate(R.layout.item_command, container, false) as LinearLayout
            container.addView(row)

            val nameEdit = row.findViewById<EditText>(R.id.edit_name)
            val cmdEdit = row.findViewById<EditText>(R.id.edit_cmd)
            val modeGroup = row.findViewById<MaterialButtonToggleGroup>(R.id.mode_group)
            val runBtn = row.findViewById<MaterialButton>(R.id.btn_run)
            val clearBtn = row.findViewById<MaterialButton>(R.id.btn_clear)
            val status = row.findViewById<TextView>(R.id.status)

            nameEdits[slot] = nameEdit
            cmdEdits[slot] = cmdEdit
            modeGroups[slot] = modeGroup
            runButtons[slot] = runBtn
            clearButtons[slot] = clearBtn
            statusViews[slot] = status

            nameEdit.setText(CommandStore.getName(this, slot))
            cmdEdit.setText(CommandStore.get(this, slot))
            modeGroup.check(
                if (CommandStore.getMode(this, slot) == CommandStore.MODE_SH) {
                    R.id.mode_shell
                } else {
                    R.id.mode_root
                }
            )

            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) = scheduleSave()
            }
            nameEdit.addTextChangedListener(watcher)
            cmdEdit.addTextChangedListener(watcher)
            modeGroup.addOnButtonCheckedListener { _, _, isChecked ->
                if (isChecked) scheduleSave()
            }

            runBtn.setOnClickListener { runCommand(slot) }
            clearBtn.setOnClickListener { confirmClear(slot) }
            updateStatus(slot, getString(R.string.status_ready))
        }

        ShortcutHelper.publish(this)
    }

    override fun onResume() {
        super.onResume()
        for (slot in 0 until CommandStore.COUNT) {
            val savedName = CommandStore.getName(this, slot)
            val savedCmd = CommandStore.get(this, slot)
            nameEdits[slot]?.let { edit ->
                if (edit.text.toString() != savedName) {
                    edit.setText(savedName)
                    edit.setSelection(edit.text.length)
                }
            }
            cmdEdits[slot]?.let { edit ->
                if (edit.text.toString() != savedCmd) {
                    edit.setText(savedCmd)
                    edit.setSelection(edit.text.length)
                }
            }
            modeGroups[slot]?.check(
                if (CommandStore.getMode(this, slot) == CommandStore.MODE_SH) {
                    R.id.mode_shell
                } else {
                    R.id.mode_root
                }
            )
        }
        ShortcutHelper.publish(this)
    }

    private fun scheduleSave() {
        saveHandler.removeCallbacksAndMessages(null)
        saveHandler.postDelayed({
            var changed = false
            for (slot in 0 until CommandStore.COUNT) {
                val name = nameEdits[slot]?.text?.toString()?.trim().orEmpty()
                val command = cmdEdits[slot]?.text?.toString()?.trim().orEmpty()
                val mode = currentMode(slot)
                if (name != CommandStore.getName(this, slot) ||
                    command != CommandStore.get(this, slot) ||
                    mode != CommandStore.getMode(this, slot)
                ) {
                    CommandStore.setName(this, slot, name)
                    CommandStore.set(this, slot, command)
                    CommandStore.setMode(this, slot, mode)
                    changed = true
                }
            }
            if (changed) {
                ShortcutHelper.publish(this)
            }
        }, 400L)
    }

    private fun saveSlot(slot: Int) {
        CommandStore.setName(this, slot, nameEdits[slot]?.text?.toString().orEmpty())
        CommandStore.set(this, slot, cmdEdits[slot]?.text?.toString().orEmpty())
        CommandStore.setMode(this, slot, currentMode(slot))
    }

    private fun currentMode(slot: Int): String =
        if (modeGroups[slot]?.checkedButtonId == R.id.mode_shell) {
            CommandStore.MODE_SH
        } else {
            CommandStore.MODE_ROOT
        }

    private fun runCommand(slot: Int) {
        saveSlot(slot)
        val command = cmdEdits[slot]?.text?.toString()?.trim().orEmpty()
        if (command.isEmpty()) {
            updateStatus(slot, getString(R.string.status_empty))
            return
        }
        if (running[slot]) return

        val mode = CommandStore.getMode(this, slot)
        setRunning(slot, true)
        updateStatus(slot, getString(R.string.status_running))
        RootExec.run(command, mode) { result ->
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

    private fun confirmClear(slot: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_dialog_title)
            .setMessage(R.string.clear_dialog_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear_confirm) { _, _ ->
                CommandStore.setName(this, slot, "")
                CommandStore.set(this, slot, "")
                nameEdits[slot]?.setText("")
                cmdEdits[slot]?.setText("")
                updateStatus(slot, getString(R.string.status_cleared))
                ShortcutHelper.publish(this)
            }
            .show()
    }

    private fun setRunning(slot: Int, isRunning: Boolean) {
        running[slot] = isRunning
        runButtons[slot]?.isEnabled = !isRunning
        clearButtons[slot]?.isEnabled = !isRunning
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
