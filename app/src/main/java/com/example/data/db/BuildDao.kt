package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BuildRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildDao {
    @Query("SELECT * FROM build_records ORDER BY startedAt DESC")
    fun getAllBuilds(): Flow<List<BuildRecord>>

    @Query("SELECT * FROM build_records WHERE projectId = :projectId ORDER BY startedAt DESC")
    fun getBuildsForProject(projectId: String): Flow<List<BuildRecord>>

    @Query("SELECT * FROM build_records WHERE id = :id LIMIT 1")
    suspend fun getBuildById(id: String): BuildRecord?

    @Query("SELECT COUNT(*) FROM build_records WHERE status = 'SUCCESS'")
    fun getSuccessfulBuildCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM build_records WHERE status = 'FAILED'")
    fun getFailedBuildCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuild(build: BuildRecord)

    @Update
    suspend fun updateBuild(build: BuildRecord)

    @Delete
    suspend fun deleteBuild(build: BuildRecord)

    @Query("DELETE FROM build_records WHERE id = :id")
    suspend fun deleteBuildById(id: String)
}
