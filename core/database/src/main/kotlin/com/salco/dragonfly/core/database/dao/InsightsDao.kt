package com.salco.dragonfly.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class MonthlySummaryRow(
    /** "YYYY-MM". */
    val month: String,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    val income: Long,
    val expenses: Long,
)

data class CategorySpendingRow(
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    val amount: Long,
)

data class NetWorthFlowRow(
    val month: String,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    val delta: Long,
)

data class OpeningBalanceRow(
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    val total: Long,
)

// Dates are stored as epoch days; SQLite turns them into "YYYY-MM" months.
private const val MONTH_OF_DATE = "strftime('%Y-%m', t.date * 86400, 'unixepoch')"

// Accounts counted in net worth, matching the accounts screen.
private const val NET_WORTH_ACCOUNT = "a.type IN ('ASSET', 'LIABILITY') AND a.active = 1 AND a.include_in_net_worth = 1"

@Dao
interface InsightsDao {

    @Query(
        """
        SELECT $MONTH_OF_DATE AS month, t.currency_code,
            SUM(CASE WHEN t.type = 'DEPOSIT' THEN t.amount ELSE 0 END) AS income,
            SUM(CASE WHEN t.type = 'WITHDRAWAL' THEN t.amount ELSE 0 END) AS expenses
        FROM transactions t
        WHERE t.type != 'TRANSFER' AND t.date BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY month, t.currency_code
        ORDER BY month
        """,
    )
    fun observeMonthlySummaries(fromEpochDay: Long, toEpochDay: Long): Flow<List<MonthlySummaryRow>>

    @Query(
        """
        SELECT t.category_id, c.name AS category_name, t.currency_code, SUM(t.amount) AS amount
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.category_id
        WHERE t.type = 'WITHDRAWAL' AND t.date BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY t.category_id, t.currency_code
        ORDER BY amount DESC
        """,
    )
    fun observeCategorySpending(fromEpochDay: Long, toEpochDay: Long): Flow<List<CategorySpendingRow>>

    /** Monthly change of net worth: money entering net-worth accounts minus money leaving them. */
    @Query(
        """
        SELECT month, currency_code, SUM(delta) AS delta FROM (
            SELECT $MONTH_OF_DATE AS month, a.currency_code AS currency_code, COALESCE(t.foreign_amount, t.amount) AS delta
            FROM transactions t JOIN accounts a ON a.id = t.destination_account_id
            WHERE $NET_WORTH_ACCOUNT
            UNION ALL
            SELECT $MONTH_OF_DATE AS month, a.currency_code AS currency_code, -t.amount AS delta
            FROM transactions t JOIN accounts a ON a.id = t.source_account_id
            WHERE $NET_WORTH_ACCOUNT
        )
        GROUP BY month, currency_code
        ORDER BY month
        """,
    )
    fun observeNetWorthFlows(): Flow<List<NetWorthFlowRow>>

    @Query(
        """
        SELECT a.currency_code, SUM(a.opening_balance) AS total FROM accounts a
        WHERE $NET_WORTH_ACCOUNT
        GROUP BY a.currency_code
        """,
    )
    fun observeOpeningBalances(): Flow<List<OpeningBalanceRow>>
}
