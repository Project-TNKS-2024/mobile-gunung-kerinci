package com.dicoding.gunungkerinci.data.local.warning

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PeringatanDiniDao {

    @Query("SELECT * FROM peringatan_dini_cache")
    suspend fun getAll(): List<PeringatanDiniEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PeringatanDiniEntity>)

    @Query("DELETE FROM peringatan_dini_cache")
    suspend fun clear()

    @Query("SELECT * FROM peringatan_dini_meta WHERE id = 0")
    suspend fun getMeta(): PeringatanDiniMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMeta(meta: PeringatanDiniMetaEntity)
}
