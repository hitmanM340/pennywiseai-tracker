package com.pennywiseai.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
<<<<<<< HEAD
import androidx.room.Update
=======
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
import com.pennywiseai.tracker.data.database.entity.CustomParserRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing custom parser rules in Room.
<<<<<<< HEAD
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

=======
 * 
 * This interface outlines the SQL queries that Room will execute.
 * @Dao flags this interface for Room compilation.
 */
@Dao
interface CustomParserRuleDao {

    // Inserts a new rule. If it already exists, replace it.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CustomParserRuleEntity): Long

    // Deletes an existing rule
    @Delete
    suspend fun delete(rule: CustomParserRuleEntity)

    // Fetches all rules matching a specific sender or package name (e.g. "GPay" or "SBI")
    @Query("SELECT * FROM custom_parser_rules WHERE package_or_sender = :sender AND is_active = 1 ORDER BY created_at DESC")
    suspend fun getRulesForSender(sender: String): List<CustomParserRuleEntity>

    // Fetches all custom rules, exposing them as a reactive Flow for UI updates
    @Query("SELECT * FROM custom_parser_rules ORDER BY created_at DESC")
    fun getAllRules(): Flow<List<CustomParserRuleEntity>>

    // Deletes a rule by its ID
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
    @Query("DELETE FROM custom_parser_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
