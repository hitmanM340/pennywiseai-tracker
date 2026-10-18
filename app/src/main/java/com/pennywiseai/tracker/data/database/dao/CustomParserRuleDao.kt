package com.pennywiseai.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pennywiseai.tracker.data.database.entity.CustomParserRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing custom parser rules in Room.
 */
@Dao
interface CustomParserRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CustomParserRuleEntity): Long

    @Update
    suspend fun update(rule: CustomParserRuleEntity)

    @Delete
    suspend fun delete(rule: CustomParserRuleEntity)

    @Query("SELECT * FROM custom_parser_rules WHERE isEnabled = 1 ORDER BY priority DESC, id ASC")
    suspend fun getEnabledRulesSync(): List<CustomParserRuleEntity>

    @Query("SELECT * FROM custom_parser_rules WHERE isEnabled = 1 ORDER BY priority DESC, id ASC")
    fun getEnabledRules(): Flow<List<CustomParserRuleEntity>>

    @Query("SELECT * FROM custom_parser_rules ORDER BY priority DESC, id ASC")
    fun getAllRules(): Flow<List<CustomParserRuleEntity>>

    @Query("DELETE FROM custom_parser_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
