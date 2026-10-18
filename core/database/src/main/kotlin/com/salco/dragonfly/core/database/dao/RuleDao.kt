package com.salco.dragonfly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.salco.dragonfly.core.database.model.PopulatedRule
import com.salco.dragonfly.core.database.model.RuleActionEntity
import com.salco.dragonfly.core.database.model.RuleEntity
import com.salco.dragonfly.core.database.model.RuleTriggerEntity
import com.salco.dragonfly.core.model.Rule
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RuleDao {

    @Transaction
    @Query("SELECT * FROM rules ORDER BY position, id")
    abstract fun observeAll(): Flow<List<PopulatedRule>>

    @Transaction
    @Query("SELECT * FROM rules WHERE id = :id")
    abstract fun observeById(id: Long): Flow<PopulatedRule?>

    @Transaction
    @Query("SELECT * FROM rules WHERE active = 1 ORDER BY position, id")
    abstract suspend fun getActive(): List<PopulatedRule>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM rules")
    abstract suspend fun nextPosition(): Int

    @Upsert
    protected abstract suspend fun upsertRule(rule: RuleEntity): Long

    @Query("DELETE FROM rule_triggers WHERE rule_id = :ruleId")
    protected abstract suspend fun clearTriggers(ruleId: Long)

    @Query("DELETE FROM rule_actions WHERE rule_id = :ruleId")
    protected abstract suspend fun clearActions(ruleId: Long)

    @Insert
    protected abstract suspend fun insertTriggers(triggers: List<RuleTriggerEntity>)

    @Insert
    protected abstract suspend fun insertActions(actions: List<RuleActionEntity>)

    @Query("UPDATE rules SET active = :active WHERE id = :id")
    abstract suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM rules WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    /** Saves the rule with its triggers and actions, replacing the old ones; new rules go last. */
    @Transaction
    open suspend fun save(rule: Rule): Long {
        val position = if (rule.id == 0L) nextPosition() else rule.position
        val rowId = upsertRule(
            RuleEntity(rule.id, rule.name.trim(), rule.matchAll, rule.active, rule.stopProcessing, position),
        )
        val id = if (rule.id != 0L) rule.id else rowId
        clearTriggers(id)
        clearActions(id)
        insertTriggers(rule.triggers.mapIndexed { i, t -> RuleTriggerEntity(ruleId = id, sequence = i, field = t.field, operator = t.operator, value = t.value) })
        insertActions(rule.actions.mapIndexed { i, a -> RuleActionEntity(ruleId = id, sequence = i, type = a.type, value = a.value) })
        return id
    }
}
