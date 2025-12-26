package com.chitui.client.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class PrintFile(
    @SerializedName("FileURL")
    val fileUrl: String,

    @SerializedName("Name")
    val name: String,

    @SerializedName("Size")
    val size: Long,

    @SerializedName("CreateTime")
    val createTime: Long,

    @SerializedName("PrintTime")
    val printTime: Long? = null,

    @SerializedName("Layer")
    val layer: Int? = null,

    @SerializedName("ThumbnailOffset")
    val thumbnailOffset: Long? = null
) : Parcelable

data class FileListResponse(
    @SerializedName("Data")
    val data: FileListData
)

data class FileListData(
    @SerializedName("Files")
    val files: List<PrintFile>,

    @SerializedName("TotalCapacity")
    val totalCapacity: Long,

    @SerializedName("FreeCapacity")
    val freeCapacity: Long
)

data class UploadProgress(
    val fileName: String,
    val progress: Float,
    val uploadedBytes: Long,
    val totalBytes: Long,
    val speed: String?,
    val timeRemaining: String?
)
