package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.ActionItem
import com.example.data.model.TranscriptSegment
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val actionItemListType = Types.newParameterizedType(List::class.java, ActionItem::class.java)
    private val segmentListType = Types.newParameterizedType(List::class.java, TranscriptSegment::class.java)

    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)
    private val actionItemListAdapter = moshi.adapter<List<ActionItem>>(actionItemListType)
    private val segmentListAdapter = moshi.adapter<List<TranscriptSegment>>(segmentListType)

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return stringListAdapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            stringListAdapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromActionItemList(value: List<ActionItem>?): String {
        return actionItemListAdapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toActionItemList(value: String?): List<ActionItem> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            actionItemListAdapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromSegmentList(value: List<TranscriptSegment>?): String {
        return segmentListAdapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toSegmentList(value: String?): List<TranscriptSegment> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            segmentListAdapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
