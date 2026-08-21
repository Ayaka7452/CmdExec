package com.ayaka7452.cmdexec

import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 通过 su（KernelSU/Magisk）以 root 执行命令。
 * 在后台线程运行，结果通过回调返回。
 */
object RootExec {

    data class Result(
        val exitCode: Int?,
        val output: String,
        val timedOut: Boolean = false
    )

    private const val TIMEOUT_SECONDS = 60L

    fun run(command: String, mode: String, onResult: (Result) -> Unit) {
        Thread {
            onResult(runSync(command, mode))
        }.start()
    }

    /** 同步执行，供后台服务使用。 */
    fun runSync(command: String, mode: String): Result = execute(command, mode)

    private fun execute(command: String, mode: String): Result {
        var process: Process? = null
        return try {
            val argv = if (mode == CommandStore.MODE_SH) {
                arrayOf("sh", "-c", command)
            } else {
                arrayOf("su", "-c", command)
            }
            process = ProcessBuilder(*argv)
                .redirectErrorStream(true)
                .start()

            var output = ""
            val reader = Thread {
                try {
                    output = process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.trim()
                } catch (_: IOException) {
                    // 流被关闭/中断时忽略
                }
            }
            reader.start()

            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                reader.join(2000)
                val suffix = "\n\n[执行超时（${TIMEOUT_SECONDS} 秒），已强制终止]"
                Result(null, output + suffix, timedOut = true)
            } else {
                reader.join(2000)
                Result(process.exitValue(), output)
            }
        } catch (e: IOException) {
            Result(null, "无法启动命令进程。\n${e.message ?: e.toString()}")
        } catch (e: Exception) {
            Result(null, "执行出错：${e.message ?: e.toString()}")
        } finally {
            process?.destroy()
        }
    }
}
