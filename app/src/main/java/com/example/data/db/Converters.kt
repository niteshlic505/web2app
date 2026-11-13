package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.BuildStatus
import com.example.data.model.BuildType
import com.example.data.model.ConversionMode
import com.example.data.model.NavStyle

class Converters {
    @TypeConverter
    fun fromConversionMode(value: ConversionMode): String = value.name

    @TypeConverter
    fun toConversionMode(value: String): ConversionMode = runCatching {
        ConversionMode.valueOf(value)
    }.getOrDefault(ConversionMode.SMART_WEBVIEW)

    @TypeConverter
    fun fromNavStyle(value: NavStyle): String = value.name

    @TypeConverter
    fun toNavStyle(value: String): NavStyle = runCatching {
        NavStyle.valueOf(value)
    }.getOrDefault(NavStyle.BOTTOM_NAV)

    @TypeConverter
    fun fromBuildType(value: BuildType): String = value.name

    @TypeConverter
    fun toBuildType(value: String): BuildType = runCatching {
        BuildType.valueOf(value)
    }.getOrDefault(BuildType.DEBUG_APK)

    @TypeConverter
    fun fromBuildStatus(value: BuildStatus): String = value.name

    @TypeConverter
    fun toBuildStatus(value: String): BuildStatus = runCatching {
        BuildStatus.valueOf(value)
    }.getOrDefault(BuildStatus.QUEUED)
}
