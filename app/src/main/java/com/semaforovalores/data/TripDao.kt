package com.semaforovalores.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.semaforovalores.model.TripHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripHistory): Long

    @Query("SELECT * FROM trip_history ORDER BY timestamp DESC LIMIT 300")
    fun observeAll(): Flow<List<TripHistory>>

    @Query("DELETE FROM trip_history")
    suspend fun clear()
}
