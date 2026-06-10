package com.quest3.taskmanager

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

data class LogChunk(
    val newOffset: Long,
    val text: String,
    val wasTruncated: Boolean
)

object LogFileReader {
    private val utf8 = StandardCharsets.UTF_8

    fun resolveLogFile(context: Context): File? {
        FileLogger.configure(context)
        val candidates = buildList {
            FileLogger.getReadableLogFile()?.let { add(it) }
            val path = AppSettings.prefs(context).getString(
                AppSettings.KEY_LOG_PATH,
                AppSettings.DEFAULT_LOG_PATH
            )
            if (!path.isNullOrBlank()) add(File(path))
            add(File("/storage/emulated/0/Download/QTaskManager.log"))
            add(File("/sdcard/Download/QTaskManager.log"))
            add(File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "QTaskManager.log"
            ))
        }
        return candidates.firstOrNull { it.exists() && it.isFile }
    }

    fun readFromOffset(file: File, offset: Long): LogChunk {
        val length = file.length()
        if (length < offset) {
            return LogChunk(0, readAll(file), wasTruncated = true)
        }
        if (length == offset) {
            return LogChunk(offset, "", wasTruncated = false)
        }
        val size = (length - offset).toInt()
        if (size <= 0) return LogChunk(offset, "", wasTruncated = false)
        val bytes = ByteArray(size)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(offset)
            raf.readFully(bytes)
        }
        return LogChunk(length, String(bytes, utf8), wasTruncated = false)
    }

    fun readAll(file: File): String =
        try {
            file.readText(utf8)
        } catch (_: Exception) {
            ""
        }
}
