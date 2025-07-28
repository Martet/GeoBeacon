package com.xzmitk01.geobeacon.data

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class StatsData(
    val totalConnections: Int,
    val uniqueUsers: Int,
    val totalCompletions: Int,
    val avgCompletionTime: Int,
    val maxCompletionTime: Int,
    val lastCompletionTime: Int,
    val maxConcurrentConnections: Int,
) {
    private fun formatTime(time: Int): String {
        val time = time / 1000
        val hours = time / 3600
        val minutes = (time % 3600) / 60
        val seconds = time % 60
        return "${if (hours > 0) "${hours}h " else ""}${minutes}m ${seconds}s"
    }

    val avgCompletionTimeFormatted: String
        get() = formatTime(avgCompletionTime)

    val maxCompletionTimeFormatted: String
        get() = formatTime(maxCompletionTime)

    val lastCompletionTimeFormatted: String
        get() = {
            val timestamp = System.currentTimeMillis() - lastCompletionTime
            val date = java.util.Date(timestamp)
            val format = java.text.SimpleDateFormat.getDateTimeInstance()
            format.format(date)
        }.invoke()

    val successRate: String
        get() = "${(totalCompletions.toDouble() / uniqueUsers.toDouble() * 100).toInt()}%"

    constructor(byteArray: ByteArray) : this(
        totalConnections = ByteBuffer.wrap(byteArray, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int,
        uniqueUsers = ByteBuffer.wrap(byteArray, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int,
        totalCompletions = ByteBuffer.wrap(byteArray, 8, 4).order(ByteOrder.LITTLE_ENDIAN).int,
        avgCompletionTime = ByteBuffer.wrap(byteArray, 12, 4).order(ByteOrder.LITTLE_ENDIAN).int,
        maxCompletionTime = ByteBuffer.wrap(byteArray, 16, 4).order(ByteOrder.LITTLE_ENDIAN).int,
        maxConcurrentConnections = ByteBuffer.wrap(byteArray, 20, 1).order(ByteOrder.LITTLE_ENDIAN).get().toInt(),
        lastCompletionTime = ByteBuffer.wrap(byteArray, 21, 4).order(ByteOrder.LITTLE_ENDIAN).int
    )
}
