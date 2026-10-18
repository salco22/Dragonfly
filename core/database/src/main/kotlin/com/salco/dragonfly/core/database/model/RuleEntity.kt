package com.salco.dragonfly.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.salco.dragonfly.core.model.ActionType
import com.salco.dragonfly.core.model.Rule
import com.salco.dragonfly.core.model.RuleAction
import com.salco.dragonfly.core.model.RuleTrigger
import com.salco.dragonfly.core.model.TriggerField
import com.salco.dragonfly.core.model.TriggerOperator

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "match_all") val matchAll: Boolean,
    val active: Boolean,
    @ColumnInfo(name = "stop_processing") val stopProcessing: Boolean,
    val position: Int,
)

@Entity(
    tableName = "rule_triggers",
    foreignKeys = [ForeignKey(entity = RuleEntity::class, parentColumns = ["id"], childColumns = ["rule_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("rule_id")],
)
data class RuleTriggerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "rule_id") val ruleId: Long,
    /** Order of the trigger within its rule. */
    val sequence: Int,
    val field: TriggerField,
    val operator: TriggerOperator,
    val value: String,
)

@Entity(
    tableName = "rule_actions",
    foreignKeys = [ForeignKey(entity = RuleEntity::class, parentColumns = ["id"], childColumns = ["rule_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("rule_id")],
)
data class RuleActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "rule_id") val ruleId: Long,
    /** Order of the action within its rule; actions run in this order. */
    val sequence: Int,
    val type: ActionType,
    val value: String,
)

data class PopulatedRule(
    @Embedded val rule: RuleEntity,
    @Relation(parentColumn = "id", entityColumn = "rule_id") val triggers: List<RuleTriggerEntity>,
    @Relation(parentColumn = "id", entityColumn = "rule_id") val actions: List<RuleActionEntity>,
)

fun PopulatedRule.asExternalModel() = Rule(
    id = rule.id,
    name = rule.name,
    triggers = triggers.sortedBy { it.sequence }.map { RuleTrigger(it.field, it.operator, it.value) },
    actions = actions.sortedBy { it.sequence }.map { RuleAction(it.type, it.value) },
    matchAll = rule.matchAll,
    active = rule.active,
    stopProcessing = rule.stopProcessing,
    position = rule.position,
)
