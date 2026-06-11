package com.quest3.taskmanager.ui

/** Счётчик параллельных загрузок — индикатор не гаснет, пока активна хотя бы одна операция. */
class LoadTracker(private val onActiveChanged: (Boolean) -> Unit) {
    private var active = 0

    fun begin() {
        active++
        onActiveChanged(true)
    }

    fun end() {
        if (active > 0) active--
        onActiveChanged(active > 0)
    }
}
