package com.salco.dragonfly.feature.bills.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.bills.detail.BillDetailScreen
import com.salco.dragonfly.feature.bills.edit.BillEditScreen
import com.salco.dragonfly.feature.bills.list.BillsScreen
import kotlinx.serialization.Serializable

@Serializable
data object BillsRoute

@Serializable
data class BillDetailRoute(val billId: Long)

/** [billId] is 0 when creating a new bill. */
@Serializable
data class BillEditRoute(val billId: Long = 0L)

fun NavController.navigateToBills() = navigate(BillsRoute)

fun NavController.navigateToBillDetail(billId: Long) = navigate(BillDetailRoute(billId))

fun NavController.navigateToBillEdit(billId: Long = 0L) = navigate(BillEditRoute(billId))

fun NavGraphBuilder.billsScreens(
    onBack: () -> Unit,
    onBillClick: (Long) -> Unit,
    onAddBill: () -> Unit,
    onEditBill: (Long) -> Unit,
    onPayBill: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
) {
    composable<BillsRoute> { BillsScreen(onBack = onBack, onBillClick = onBillClick, onAddBill = onAddBill, onPayBill = onPayBill) }
    composable<BillDetailRoute> {
        BillDetailScreen(onBack = onBack, onEdit = onEditBill, onPay = onPayBill, onTransactionClick = onTransactionClick)
    }
    composable<BillEditRoute> { BillEditScreen(onBack = onBack) }
}
