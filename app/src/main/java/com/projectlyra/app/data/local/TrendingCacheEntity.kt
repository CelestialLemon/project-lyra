package com.projectlyra.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trending_cache",
    indices = [
        Index(value = ["position"], unique = true),
        Index(value = ["media_item_id"], unique = true),
    ],
)
data class TrendingCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "media_item_id")
    val mediaItemId: Long,
    val position: Int,
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long,
)
