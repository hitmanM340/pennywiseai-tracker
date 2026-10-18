package com.pennywiseai.tracker.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Room Entity representing a user-defined custom SMS parsing rule.
 * Stores validated regular expressions and capture group parameters.
 */
@Entity(
    tableName = "custom_parser_rules",
    indices = [
        Index(value = ["isEnabled", "priority"], name = "index_custom_parser_rules_isEnabled_priority")
    ]
)
@Serializable
data class CustomParserRuleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    @SerialName("id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    @SerialName("name")
    val name: String = "",

    @ColumnInfo(name = "senderPattern")
    @SerialName("senderPattern")
    val senderPattern: String = "",

    @ColumnInfo(name = "amountRegex")
    @SerialName("amountRegex")
    val amountRegex: String = "",

    @ColumnInfo(name = "merchantRegex")
    @SerialName("merchantRegex")
    val merchantRegex: String? = null,

    @ColumnInfo(name = "accountRegex")
    @SerialName("accountRegex")
    val accountRegex: String? = null,

    @ColumnInfo(name = "transactionType")
    @SerialName("transactionType")
    val transactionType: String = "EXPENSE",

    @ColumnInfo(name = "category")
    @SerialName("category")
    val category: String? = null,

    @ColumnInfo(name = "isEnabled", defaultValue = "1")
    @SerialName("isEnabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "priority", defaultValue = "0")
    @SerialName("priority")
    val priority: Int = 0,

    @ColumnInfo(name = "createdAt")
    @SerialName("createdAt")
    val createdAt: String = java.time.Instant.now().toString()
)
