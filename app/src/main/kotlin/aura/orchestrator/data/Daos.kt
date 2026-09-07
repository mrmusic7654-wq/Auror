package aura.orchestrator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<ConversationEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(c: ConversationEntity)
    @Delete suspend fun delete(c: ConversationEntity)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY createdAtEpochMs ASC")
    fun observeForConversation(cid: String): Flow<List<MessageEntity>>
    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY createdAtEpochMs ASC LIMIT :limit")
    suspend fun latestForConversation(cid: String, limit: Int = 200): List<MessageEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(m: MessageEntity)
    @Query("DELETE FROM messages WHERE conversationId = :cid")
    suspend fun clearConversation(cid: String)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<ProjectEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(p: ProjectEntity)
    @Query("SELECT * FROM projects WHERE id = :id") suspend fun byId(id: String): ProjectEntity?
    @Delete suspend fun delete(p: ProjectEntity)
    @Query("SELECT COUNT(*) FROM projects") suspend fun count(): Int
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE id = :id") fun observeById(id: String): Flow<TaskEntity?>
    @Query("SELECT * FROM tasks WHERE status IN (:statuses) ORDER BY createdAtEpochMs DESC")
    fun observeInStatuses(statuses: List<String>): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE status IN (:statuses)")
    suspend fun listInStatuses(statuses: List<String>): List<TaskEntity>
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun byId(id: String): TaskEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(t: TaskEntity)
    @Update suspend fun update(t: TaskEntity)
    @Query("SELECT COUNT(*) FROM tasks") suspend fun count(): Int
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun delete(id: String)
}

@Dao
interface ExecutionLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(l: ExecutionLogEntity)
    @Query("SELECT * FROM execution_logs WHERE taskId = :taskId ORDER BY createdAtEpochMs DESC LIMIT :limit")
    fun observeForTask(taskId: String, limit: Int = 200): Flow<List<ExecutionLogEntity>>
    @Query("SELECT COUNT(*) FROM execution_logs") suspend fun count(): Int
}

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(m: MemoryEntity)
    @Query("SELECT * FROM memories WHERE (:projectId IS NULL OR projectId = :projectId) ORDER BY importance DESC, createdAtEpochMs DESC")
    fun observeAll(projectId: String?): Flow<List<MemoryEntity>>
    @Query("DELETE FROM memories WHERE id = :id") suspend fun delete(id: String)
    @Query("SELECT COUNT(*) FROM memories") suspend fun count(): Int
}
