package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.dao.AccountDao
import com.salco.dragonfly.core.database.dao.RuleDao
import com.salco.dragonfly.core.database.model.asExternalModel
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.Rule
import com.salco.dragonfly.core.model.RuleSubject
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.applyRules
import javax.inject.Inject

/** Runs the user's rules on a transaction before it is saved. */
internal class RuleApplier @Inject constructor(
    private val ruleDao: RuleDao,
    private val accountDao: AccountDao,
) {
    suspend fun apply(draft: TransactionDraft, rules: List<Rule>? = null): TransactionDraft {
        val active = rules ?: ruleDao.getActive().map { it.asExternalModel() }
        if (active.isEmpty()) return draft
        val subject = draft.toSubject()
        val result = applyRules(active, subject)
        return if (result == subject) draft else draft.with(result)
    }

    private suspend fun AccountRef.name(): String = when (this) {
        is AccountRef.Existing -> accountDao.getById(id)?.name.orEmpty()
        is AccountRef.New -> name
    }

    private suspend fun TransactionDraft.toSubject() = RuleSubject(
        type = type,
        description = description,
        amount = amount,
        notes = notes,
        accountName = (if (type == TransactionType.DEPOSIT) destination else source).name(),
        counterpartyName = (if (type == TransactionType.DEPOSIT) source else destination).name(),
        categoryName = categoryName,
        tagNames = tagNames,
        budgetId = budgetId,
        billId = billId,
    )
}

internal fun TransactionDraft.with(subject: RuleSubject) = copy(
    description = subject.description,
    notes = subject.notes,
    categoryName = subject.categoryName,
    tagNames = subject.tagNames,
    budgetId = subject.budgetId,
    billId = subject.billId,
)
