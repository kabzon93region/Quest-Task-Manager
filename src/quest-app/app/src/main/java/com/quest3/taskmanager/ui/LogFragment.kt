package com.quest3.taskmanager.ui

import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.quest3.taskmanager.AppSettings
import com.quest3.taskmanager.FileLogger
import com.quest3.taskmanager.LogFileReader
import com.quest3.taskmanager.LogListener
import com.quest3.taskmanager.R
import com.quest3.taskmanager.databinding.FragmentLogBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LogFragment : Fragment() {
    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!

    private var fileOffset = 0L
    private var logFile: File? = null

    private val logListener = LogListener { line ->
        if (_binding == null) return@LogListener
        view?.post {
            if (_binding == null || !viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                return@post
            }
            appendPreservingState("$line\n")
            syncFileOffset()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val path = AppSettings.prefs(requireContext()).getString(
            AppSettings.KEY_LOG_PATH,
            AppSettings.DEFAULT_LOG_PATH
        ) ?: AppSettings.DEFAULT_LOG_PATH
        binding.logPathText.text = path
        binding.btnLogRefresh.setOnClickListener { reloadFull() }
        reloadFull()
    }

    override fun onStart() {
        super.onStart()
        FileLogger.addLogListener(logListener)
        startPolling()
    }

    override fun onStop() {
        FileLogger.removeLogListener(logListener)
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startPolling() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(POLL_MS)
                    pollFile()
                }
            }
        }
    }

    private fun reloadFull() {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            val file = withContext(Dispatchers.IO) { LogFileReader.resolveLogFile(ctx) }
            logFile = file
            if (file == null) {
                showEmpty(true)
                binding.logText.text = ""
                fileOffset = 0
                return@launch
            }
            val text = withContext(Dispatchers.IO) { LogFileReader.readAll(file) }
            fileOffset = file.length()
            showEmpty(text.isBlank())
            binding.logText.text = text
            if (text.isNotBlank()) {
                binding.logScroll.post { scrollToBottom(binding.logScroll, binding.logText) }
            }
        }
    }

    private fun pollFile() {
        val file = logFile ?: LogFileReader.resolveLogFile(requireContext()).also { logFile = it } ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val chunk = withContext(Dispatchers.IO) { LogFileReader.readFromOffset(file, fileOffset) }
            if (chunk.wasTruncated) {
                fileOffset = 0
                showEmpty(chunk.text.isBlank())
                binding.logText.text = chunk.text
                fileOffset = chunk.newOffset
                return@launch
            }
            if (chunk.text.isEmpty()) return@launch
            fileOffset = chunk.newOffset
            showEmpty(false)
            appendPreservingState(chunk.text)
            syncFileOffset()
        }
    }

    private fun syncFileOffset() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (logFile == null) logFile = LogFileReader.resolveLogFile(requireContext())
            val len = logFile?.length() ?: return@launch
            withContext(Dispatchers.Main) { fileOffset = len }
        }
    }

    private fun appendPreservingState(text: String) {
        val scroll = binding.logScroll
        val tv = binding.logText
        if (text.isEmpty()) return

        val scrollY = scroll.scrollY
        val atBottom = isAtBottom(scroll, tv)
        val selStart = tv.selectionStart
        val selEnd = tv.selectionEnd
        val hadSelection = selStart >= 0 && selEnd >= 0 && selStart != selEnd
        val textLenBefore = tv.text?.length ?: 0

        tv.append(text)

        scroll.post {
            if (atBottom) {
                scrollToBottom(scroll, tv)
            } else {
                scroll.scrollTo(0, scrollY)
                if (hadSelection && selStart <= textLenBefore && selEnd <= textLenBefore) {
                    val spannable = tv.text
                    if (spannable is Spannable) {
                        try {
                            Selection.setSelection(spannable, selStart, selEnd)
                        } catch (_: IndexOutOfBoundsException) {
                            // selection no longer valid
                        }
                    }
                }
            }
        }
    }

    private fun showEmpty(empty: Boolean) {
        binding.logEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.logScroll.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun isAtBottom(scroll: ScrollView, text: TextView): Boolean {
        val child = scroll.getChildAt(0) ?: return true
        val diff = child.bottom - (scroll.height + scroll.scrollY)
        return diff <= SCROLL_BOTTOM_THRESHOLD_PX
    }

    private fun scrollToBottom(scroll: ScrollView, text: TextView) {
        scroll.scrollTo(0, (text.bottom - scroll.height).coerceAtLeast(0))
    }

    companion object {
        private const val POLL_MS = 1500L
        private const val SCROLL_BOTTOM_THRESHOLD_PX = 48
    }
}
