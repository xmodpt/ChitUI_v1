package com.chitui.client.data.model

data class StorageInfo(
    val totalSpace: Long,
    val freeSpace: Long,
    val usedSpace: Long
) {
    val usedPercent: Float
        get() = if (totalSpace > 0) (usedSpace.toFloat() / totalSpace * 100) else 0f
}

data class USBGadgetStatus(
    val enabled: Boolean,
    val type: String?, // virtual or physical
    val storage: StorageInfo?
)
