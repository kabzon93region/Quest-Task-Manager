package com.quest3.taskmanager

data class KillResult(
    val killed: Int,
    val skippedProtected: Int,
    val failed: Int
)
