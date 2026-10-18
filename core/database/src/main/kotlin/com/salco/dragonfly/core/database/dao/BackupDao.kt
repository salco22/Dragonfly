package com.salco.dragonfly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.BillEntity
import com.salco.dragonfly.core.database.model.BudgetEntity
import com.salco.dragonfly.core.database.model.CategoryEntity
import com.salco.dragonfly.core.database.model.PiggyBankEntity
import com.salco.dragonfly.core.database.model.PiggyBankEventEntity
import com.salco.dragonfly.core.database.model.RecurringTransactionEntity
import com.salco.dragonfly.core.database.model.RuleActionEntity
import com.salco.dragonfly.core.database.model.RuleEntity
import com.salco.dragonfly.core.database.model.RuleTriggerEntity
import com.salco.dragonfly.core.database.model.TagEntity
import com.salco.dragonfly.core.database.model.TransactionEntity
import com.salco.dragonfly.core.database.model.TransactionTagCrossRef

/** Raw access to every table, used to write and restore full backups. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM accounts") suspend fun accounts(): List<AccountEntity>
    @Query("SELECT * FROM categories") suspend fun categories(): List<CategoryEntity>
    @Query("SELECT * FROM tags") suspend fun tags(): List<TagEntity>
    @Query("SELECT * FROM budgets") suspend fun budgets(): List<BudgetEntity>
    @Query("SELECT * FROM bills") suspend fun bills(): List<BillEntity>
    @Query("SELECT * FROM transactions") suspend fun transactions(): List<TransactionEntity>
    @Query("SELECT * FROM transaction_tags") suspend fun transactionTags(): List<TransactionTagCrossRef>
    @Query("SELECT * FROM recurring_transactions") suspend fun recurringTransactions(): List<RecurringTransactionEntity>
    @Query("SELECT * FROM piggy_banks") suspend fun piggyBanks(): List<PiggyBankEntity>
    @Query("SELECT * FROM piggy_bank_events") suspend fun piggyBankEvents(): List<PiggyBankEventEntity>
    @Query("SELECT * FROM rules") suspend fun rules(): List<RuleEntity>
    @Query("SELECT * FROM rule_triggers") suspend fun ruleTriggers(): List<RuleTriggerEntity>
    @Query("SELECT * FROM rule_actions") suspend fun ruleActions(): List<RuleActionEntity>

    // Deleting accounts, categories, tags, budgets, bills and rules cascades or nulls everything else.
    @Query("DELETE FROM transaction_tags") suspend fun clearTransactionTags()
    @Query("DELETE FROM piggy_bank_events") suspend fun clearPiggyBankEvents()
    @Query("DELETE FROM piggy_banks") suspend fun clearPiggyBanks()
    @Query("DELETE FROM recurring_transactions") suspend fun clearRecurringTransactions()
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM rule_triggers") suspend fun clearRuleTriggers()
    @Query("DELETE FROM rule_actions") suspend fun clearRuleActions()
    @Query("DELETE FROM rules") suspend fun clearRules()
    @Query("DELETE FROM bills") suspend fun clearBills()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM tags") suspend fun clearTags()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM accounts") suspend fun clearAccounts()

    @Insert suspend fun insertAccounts(items: List<AccountEntity>)
    @Insert suspend fun insertCategories(items: List<CategoryEntity>)
    @Insert suspend fun insertTags(items: List<TagEntity>)
    @Insert suspend fun insertBudgets(items: List<BudgetEntity>)
    @Insert suspend fun insertBills(items: List<BillEntity>)
    @Insert suspend fun insertTransactions(items: List<TransactionEntity>)
    @Insert suspend fun insertTransactionTags(items: List<TransactionTagCrossRef>)
    @Insert suspend fun insertRecurringTransactions(items: List<RecurringTransactionEntity>)
    @Insert suspend fun insertPiggyBanks(items: List<PiggyBankEntity>)
    @Insert suspend fun insertPiggyBankEvents(items: List<PiggyBankEventEntity>)
    @Insert suspend fun insertRules(items: List<RuleEntity>)
    @Insert suspend fun insertRuleTriggers(items: List<RuleTriggerEntity>)
    @Insert suspend fun insertRuleActions(items: List<RuleActionEntity>)
}
