package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppProject
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM app_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<AppProject>>

    @Query("SELECT * FROM app_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): AppProject?

    @Query("SELECT COUNT(*) FROM app_projects")
    fun getProjectCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: AppProject)

    @Update
    suspend fun updateProject(project: AppProject)

    @Delete
    suspend fun deleteProject(project: AppProject)

    @Query("DELETE FROM app_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}
