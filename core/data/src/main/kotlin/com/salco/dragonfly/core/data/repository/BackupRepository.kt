package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.DatabaseTransactionRunner
import com.salco.dragonfly.core.database.DragonflyDatabase
import com.salco.dragonfly.core.database.dao.BackupDao
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
import com.salco.dragonfly.core.database.model.TransactionGroupEntity
import com.salco.dragonfly.core.database.model.TransactionTagCrossRef
import com.salco.dragonfly.core.model.TransactionFilter
import com.salco.dragonfly.core.model.minorUnitsToInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** What a backup file contains, shown before restoring it. */
data class BackupSummary(
    val exportedAt: Instant,
    val accounts: Int,
    val transactions: Int,
    val budgets: Int,
    val bills: Int,
    val piggyBanks: Int,
    val rules: Int,
)

/** A backup file read and validated, ready to be restored. */
class ParsedBackup internal constructor(internal val file: BackupFile) {
    val summary = BackupSummary(
        exportedAt = Instant.parse(file.exportedAt),
        accounts = file.accounts.size,
        transactions = file.transactions.size,
        budgets = file.budgets.size,
        bills = file.bills.size,
        piggyBanks = file.piggyBanks.size,
        rules = file.rules.size,
    )
}

class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface BackupRepository {
    /** Writes every piece of data as JSON to [output]. */
    suspend fun writeBackup(output: OutputStream)

    /** Reads and validates a backup without touching the current data; fails with [InvalidBackupException]. */
    suspend fun readBackup(input: InputStream): ParsedBackup

    /** Replaces all current data with [backup], atomically: on failure nothing changes. */
    suspend fun restoreBackup(backup: ParsedBackup)

    /** Writes all transactions as CSV (UTF-8, comma separated, dot decimals) for spreadsheets. */
    suspend fun writeTransactionsCsv(output: OutputStream)
}

/**
 * The backup file. Tables are stored as they are in the database; [databaseVersion] tells which
 * schema wrote them. Lists missing from older files default to empty.
 */
@Serializable
internal data class BackupFile(
    val format: String = FORMAT,
    @SerialName("format_version") val formatVersion: Int = 1,
    @SerialName("database_version") val databaseVersion: Int,
    @SerialName("exported_at") val exportedAt: String,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val bills: List<BillEntity> = emptyList(),
    @SerialName("transaction_groups") val transactionGroups: List<TransactionGroupEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    @SerialName("transaction_tags") val transactionTags: List<TransactionTagCrossRef> = emptyList(),
    @SerialName("recurring_transactions") val recurringTransactions: List<RecurringTransactionEntity> = emptyList(),
    @SerialName("piggy_banks") val piggyBanks: List<PiggyBankEntity> = emptyList(),
    @SerialName("piggy_bank_events") val piggyBankEvents: List<PiggyBankEventEntity> = emptyList(),
    val rules: List<RuleEntity> = emptyList(),
    @SerialName("rule_triggers") val ruleTriggers: List<RuleTriggerEntity> = emptyList(),
    @SerialName("rule_actions") val ruleActions: List<RuleActionEntity> = emptyList(),
) {
    companion object {
        const val FORMAT = "dragonfly-backup"
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal class OfflineBackupRepository(
    private val backupDao: BackupDao,
    private val transactionsRepository: TransactionsRepository,
    private val runInTransaction: DatabaseTransactionRunner,
    private val clock: Clock,
) : BackupRepository {

    @Inject
    constructor(
        backupDao: BackupDao,
        transactionsRepository: TransactionsRepository,
        runInTransaction: DatabaseTransactionRunner,
    ) : this(backupDao, transactionsRepository, runInTransaction, Clock.systemUTC())

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override suspend fun writeBackup(output: OutputStream) {
        // Read everything in one transaction so the snapshot is consistent.
        val file = runInTransaction {
            BackupFile(
                databaseVersion = DragonflyDatabase.VERSION,
                exportedAt = Instant.now(clock).toString(),
                accounts = backupDao.accounts(),
                categories = backupDao.categories(),
                tags = backupDao.tags(),
                budgets = backupDao.budgets(),
                bills = backupDao.bills(),
                transactionGroups = backupDao.transactionGroups(),
                transactions = backupDao.transactions(),
                transactionTags = backupDao.transactionTags(),
                recurringTransactions = backupDao.recurringTransactions(),
                piggyBanks = backupDao.piggyBanks(),
                piggyBankEvents = backupDao.piggyBankEvents(),
                rules = backupDao.rules(),
                ruleTriggers = backupDao.ruleTriggers(),
                ruleActions = backupDao.ruleActions(),
            )
        }
        withContext(Dispatchers.IO) { json.encodeToStream(file, output) }
    }

    override suspend fun readBackup(input: InputStream): ParsedBackup {
        val file = withContext(Dispatchers.IO) {
            try {
                json.decodeFromStream<BackupFile>(input)
            } catch (e: Exception) {
                throw InvalidBackupException("Not a Dragonfly backup", e)
            }
        }
        if (file.format != BackupFile.FORMAT) throw InvalidBackupException("Unknown format ${file.format}")
        if (file.databaseVersion > DragonflyDatabase.VERSION) {
            throw InvalidBackupException("Backup from a newer version (${file.databaseVersion})")
        }
        return ParsedBackup(file)
    }

    override suspend fun restoreBackup(backup: ParsedBackup) {
        val f = backup.file
        runInTransaction {
            with(backupDao) {
                clearTransactionTags()
                clearPiggyBankEvents()
                clearPiggyBanks()
                clearRecurringTransactions()
                clearTransactions()
                clearTransactionGroups()
                clearRuleTriggers()
                clearRuleActions()
                clearRules()
                clearBills()
                clearBudgets()
                clearTags()
                clearCategories()
                clearAccounts()
                // Parents first, so every foreign key finds its row.
                insertAccounts(f.accounts)
                insertCategories(f.categories)
                insertTags(f.tags)
                insertBudgets(f.budgets)
                insertBills(f.bills)
                insertTransactionGroups(f.transactionGroups)
                insertTransactions(f.transactions)
                insertTransactionTags(f.transactionTags)
                insertRecurringTransactions(f.recurringTransactions)
                insertPiggyBanks(f.piggyBanks)
                insertPiggyBankEvents(f.piggyBankEvents)
                insertRules(f.rules)
                insertRuleTriggers(f.ruleTriggers)
                insertRuleActions(f.ruleActions)
            }
        }
    }

    override suspend fun writeTransactionsCsv(output: OutputStream) {
        val transactions = transactionsRepository.searchTransactions(TransactionFilter()).first().sortedBy { it.date }
        withContext(Dispatchers.IO) {
            output.bufferedWriter(Charsets.UTF_8).use { writer ->
                // A byte order mark lets Excel recognise UTF-8 (accents, €).
                writer.write("\uFEFF")
                writer.write(CSV_HEADER.joinToString(",") + "\r\n")
                transactions.forEach { t ->
                    val row = listOf(
                        t.date.toString(),
                        t.type.name.lowercase(),
                        t.description,
                        minorUnitsToInput(t.amount, t.currencyCode),
                        t.currencyCode,
                        t.foreignAmount?.let { minorUnitsToInput(it, t.destination.currencyCode) }.orEmpty(),
                        t.source.name,
                        t.destination.name,
                        t.category?.name.orEmpty(),
                        t.tags.joinToString(" ") { it.name },
                        t.budget?.name.orEmpty(),
                        t.bill?.name.orEmpty(),
                        t.notes.orEmpty(),
                        t.groupTitle.orEmpty(),
                    )
                    writer.write(row.joinToString(",") { it.csvEscaped() } + "\r\n")
                }
            }
        }
    }

    private companion object {
        val CSV_HEADER = listOf(
            "date", "type", "description", "amount", "currency", "foreign_amount", "source", "destination",
            "category", "tags", "budget", "bill", "notes", "split_group",
        )
    }
}

/** Quotes a CSV field when needed, doubling inner quotes (RFC 4180). */
internal fun String.csvEscaped(): String =
    if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + replace("\"", "\"\"") + "\"" else this

/** Suggested file name for a backup made on [date]. */
fun backupFileName(date: LocalDate): String = "dragonfly-backup-$date.json"

fun csvFileName(date: LocalDate): String = "dragonfly-transazioni-$date.csv"
