package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLockDao {
    @Query("SELECT * FROM app_locks ORDER BY isLocked DESC, appName ASC")
    fun getAllAppLocks(): Flow<List<AppLockEntity>>

    @Query("SELECT * FROM app_locks WHERE isLocked = 1")
    fun getLockedApps(): Flow<List<AppLockEntity>>

    @Query("SELECT * FROM app_locks WHERE isLocked = 1")
    suspend fun getLockedAppsSync(): List<AppLockEntity>

    @Query("SELECT * FROM app_locks WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppLock(packageName: String): AppLockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(appLock: AppLockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appLocks: List<AppLockEntity>)

    @Query("UPDATE app_locks SET isLocked = :isLocked WHERE packageName = :packageName")
    suspend fun updateLockStatus(packageName: String, isLocked: Boolean)

    @Query("UPDATE app_locks SET unlockCount = unlockCount + 1 WHERE packageName = :packageName")
    suspend fun incrementUnlockCount(packageName: String)

    @Query("DELETE FROM app_locks WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM app_locks")
    suspend fun deleteAll()
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC")
    fun getAllVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE type = :type ORDER BY createdAt DESC")
    fun getItemsByType(type: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): VaultItemEntity?

    @Query("SELECT * FROM vault_items")
    suspend fun getAllItemsSync(): List<VaultItemEntity>

    @Query("SELECT COUNT(*) FROM vault_items WHERE type = 'PHOTO'")
    fun getPhotoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_items WHERE type != 'PHOTO'")
    fun getDocCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItemEntity): Long

    @Update
    suspend fun updateItem(item: VaultItemEntity)

    @Delete
    suspend fun deleteItem(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vault_items")
    suspend fun deleteAll()
}

@Dao
interface SecurityLogDao {
    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<SecurityLogEntity>>

    @Insert
    suspend fun insertLog(log: SecurityLogEntity)

    @Query("DELETE FROM security_logs")
    suspend fun clearLogs()
}
