package com.salco.dragonfly.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Kind of account a Firefly III transaction refers to, from its `source_type`/`destination_type`. */
enum class FireflyAccountKind {
    ASSET,
    LIABILITY,
    EXPENSE,
    REVENUE,

    /** Firefly's "Cash account": money paid or received with no named counterparty. */
    CASH,
    INITIAL_BALANCE,
    RECONCILIATION,
    OTHER,
}

enum class FireflyType { WITHDRAWAL, DEPOSIT, TRANSFER, OPENING_BALANCE, RECONCILIATION }

data class FireflyAccount(val name: String, val kind: FireflyAccountKind) {
    val isOwn: Boolean get() = kind == FireflyAccountKind.ASSET || kind == FireflyAccountKind.LIABILITY
}

/** One row (a "journal") of a Firefly III transaction export. */
data class FireflyTransaction(
    val line: Int,
    val journalId: String,
    /** Rows sharing a group id are the parts of a split transaction. */
    val groupId: String,
    val groupTitle: String?,
    val type: FireflyType,
    val date: LocalDate,
    /** Positive, in minor units of [currencyCode]. */
    val amount: Long,
    val currencyCode: String,
    /** Positive, in minor units of [foreignCurrencyCode], for transfers between currencies. */
    val foreignAmount: Long?,
    val foreignCurrencyCode: String?,
    val description: String,
    val source: FireflyAccount,
    val destination: FireflyAccount,
    val category: String?,
    val budget: String?,
    val bill: String?,
    val tags: List<String>,
    val notes: String?,
)

/** A parsed Firefly III export: its transactions and the lines that could not be read. */
data class FireflyExport(
    val transactions: List<FireflyTransaction>,
    val invalidLines: List<Int>,
) {
    /** The user's own accounts (asset and liability) with their currency, by name. */
    val ownAccounts: Map<String, Pair<FireflyAccountKind, String>> by lazy {
        buildMap {
            transactions.forEach { t ->
                listOf(t.source, t.destination).filter { it.isOwn }.forEach { account ->
                    val currency = if (account == t.destination && t.foreignCurrencyCode != null) t.foreignCurrencyCode else t.currencyCode
                    putIfAbsent(account.name, account.kind to currency)
                }
            }
        }
    }

    /** Opening balance of each own account that has one: positive when the account started with money. */
    val openingBalances: Map<String, Long> by lazy {
        transactions.filter { it.type == FireflyType.OPENING_BALANCE }.associate { t ->
            if (t.destination.isOwn) t.destination.name to t.amount else t.source.name to -t.amount
        }
    }

    val categories: Set<String> by lazy { transactions.mapNotNull { it.category }.toSortedSet() }
    val budgets: Set<String> by lazy { transactions.mapNotNull { it.budget }.toSortedSet() }
    val tags: Set<String> by lazy { transactions.flatMap { it.tags }.toSortedSet() }

    /** Transactions to create: everything except opening balances, which become account balances. */
    val movements: List<FireflyTransaction> get() = transactions.filter { it.type != FireflyType.OPENING_BALANCE }

    fun count(type: FireflyType): Int = transactions.count { it.type == type }
}

/** True when [header] is the header of a Firefly III transaction export. */
fun isFireflyExport(header: List<String>): Boolean =
    listOf("journal_id", "group_id", "type", "amount", "source_type", "destination_type").all { it in header }

/**
 * Reads a Firefly III export ([rows] includes the header). Amounts are written with up to 12
 * decimals and a sign that does not depend on the direction, which the type gives instead.
 * Rows may lack trailing columns; tags are separated by commas.
 */
fun parseFireflyExport(rows: List<List<String>>): FireflyExport {
    val header = rows.first()
    val transactions = mutableListOf<FireflyTransaction>()
    val invalid = mutableListOf<Int>()
    rows.drop(1).forEachIndexed { index, row ->
        val line = index + 2
        fun value(column: String): String? = header.indexOf(column).takeIf { it >= 0 }
            ?.let { row.getOrNull(it)?.trim() }?.takeIf { it.isNotEmpty() }

        val type = when (value("type")?.lowercase()) {
            "withdrawal" -> FireflyType.WITHDRAWAL
            "deposit" -> FireflyType.DEPOSIT
            "transfer" -> FireflyType.TRANSFER
            "opening balance" -> FireflyType.OPENING_BALANCE
            "reconciliation" -> FireflyType.RECONCILIATION
            else -> null
        }
        val currency = value("currency_code") ?: "EUR"
        val amount = value("amount")?.let { fireflyAmount(it, currency) }
        // Dates carry a time and an offset ("2026-09-26T12:05:00+02:00"): the local day is what matters.
        val date = value("date")?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val source = value("source_name")?.let { FireflyAccount(it, accountKind(value("source_type"))) }
        val destination = value("destination_name")?.let { FireflyAccount(it, accountKind(value("destination_type"))) }
        if (type == null || amount == null || amount == 0L || date == null || source == null || destination == null) {
            invalid += line
            return@forEachIndexed
        }
        val foreignCurrency = value("foreign_currency_code")
        transactions += FireflyTransaction(
            line = line,
            journalId = value("journal_id") ?: line.toString(),
            groupId = value("group_id") ?: "line-$line",
            groupTitle = value("group_title"),
            type = type,
            date = date,
            amount = amount,
            currencyCode = currency,
            foreignAmount = foreignCurrency?.let { code -> value("foreign_amount")?.let { fireflyAmount(it, code) } },
            foreignCurrencyCode = foreignCurrency,
            description = value("description") ?: "",
            source = source,
            destination = destination,
            category = value("category"),
            budget = value("budget"),
            bill = value("bill"),
            tags = value("tags")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct().orEmpty(),
            notes = value("notes"),
        )
    }
    return FireflyExport(transactions, invalid)
}

/** "-100.000000000000" in [currencyCode]'s minor units, without sign; null when it is not a number. */
internal fun fireflyAmount(text: String, currencyCode: String): Long? = runCatching {
    BigDecimal(text.trim()).abs().setScale(fractionDigits(currencyCode), RoundingMode.HALF_UP)
        .movePointRight(fractionDigits(currencyCode)).longValueExact()
}.getOrNull()

private fun accountKind(type: String?): FireflyAccountKind = when (type?.lowercase()) {
    "asset account", "default account" -> FireflyAccountKind.ASSET
    "loan", "debt", "mortgage", "liability credit account" -> FireflyAccountKind.LIABILITY
    "expense account", "beneficiary account" -> FireflyAccountKind.EXPENSE
    "revenue account" -> FireflyAccountKind.REVENUE
    "cash account" -> FireflyAccountKind.CASH
    "initial balance account" -> FireflyAccountKind.INITIAL_BALANCE
    "reconciliation account" -> FireflyAccountKind.RECONCILIATION
    else -> FireflyAccountKind.OTHER
}
