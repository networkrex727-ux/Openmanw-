package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Entities
@Entity(tableName = "db_connections")
data class DBConnection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // SQLite, MySQL, PostgreSQL, Redis, MongoDB
    val host: String,
    val port: Int,
    val user: String,
    val databaseName: String
)

@Entity(tableName = "saved_api_requests")
data class SavedApiRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val collectionName: String,
    val name: String,
    val method: String, // GET, POST, PUT, DELETE, etc.
    val url: String,
    val headersJson: String, // JSON serialized key-value pairs
    val body: String
)

@Entity(tableName = "vcs_commits")
data class VcsCommit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val commitHash: String,
    val message: String,
    val author: String,
    val timestamp: Long,
    val changedFilesJson: String // Map of filePath -> "ADDED" / "MODIFIED" / "DELETED"
)

// 2. DAO
@Dao
interface DevHiveDao {
    // Database Connections
    @Query("SELECT * FROM db_connections ORDER BY name ASC")
    fun getAllConnections(): Flow<List<DBConnection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: DBConnection)

    @Query("DELETE FROM db_connections WHERE id = :id")
    suspend fun deleteConnectionById(id: Int)

    // Saved API Requests
    @Query("SELECT * FROM saved_api_requests ORDER BY collectionName, name ASC")
    fun getAllApiRequests(): Flow<List<SavedApiRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiRequest(request: SavedApiRequest)

    @Query("DELETE FROM saved_api_requests WHERE id = :id")
    suspend fun deleteApiRequestById(id: Int)

    // VCS Commits
    @Query("SELECT * FROM vcs_commits ORDER BY timestamp DESC")
    fun getAllCommits(): Flow<List<VcsCommit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommit(commit: VcsCommit)

    @Query("DELETE FROM vcs_commits WHERE id = :id")
    suspend fun deleteCommitById(id: Int)

    @Query("DELETE FROM vcs_commits")
    suspend fun clearAllCommits()
}

// 3. Room Database
@Database(
    entities = [DBConnection::class, SavedApiRequest::class, VcsCommit::class],
    version = 1,
    exportSchema = false
)
abstract class DevHiveDatabase : RoomDatabase() {
    abstract fun devHiveDao(): DevHiveDao

    companion object {
        @Volatile
        private var INSTANCE: DevHiveDatabase? = null

        fun getDatabase(context: Context): DevHiveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DevHiveDatabase::class.java,
                    "devhive_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// 4. Repository
class DevHiveRepository(private val dao: DevHiveDao) {
    val connections: Flow<List<DBConnection>> = dao.getAllConnections()
    val apiRequests: Flow<List<SavedApiRequest>> = dao.getAllApiRequests()
    val commits: Flow<List<VcsCommit>> = dao.getAllCommits()

    suspend fun saveConnection(connection: DBConnection) {
        dao.insertConnection(connection)
    }

    suspend fun deleteConnection(id: Int) {
        dao.deleteConnectionById(id)
    }

    suspend fun saveApiRequest(request: SavedApiRequest) {
        dao.insertApiRequest(request)
    }

    suspend fun deleteApiRequest(id: Int) {
        dao.deleteApiRequestById(id)
    }

    suspend fun saveCommit(commit: VcsCommit) {
        dao.insertCommit(commit)
    }

    suspend fun deleteCommit(id: Int) {
        dao.deleteCommitById(id)
    }

    suspend fun clearAllCommits() {
        dao.clearAllCommits()
    }
}
