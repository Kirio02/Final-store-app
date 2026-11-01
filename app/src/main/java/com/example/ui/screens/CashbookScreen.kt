package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppRepository
import com.example.model.CashEntry
import com.example.ui.theme.*
import com.example.ui.util.LanguageStrings
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashbookScreen(
    repository: AppRepository
) {
    val entries by repository.cashbook.collectAsState()

    val cashIn = entries.filter { it.paymentMode == "CASH" && it.type == "IN" }.sumOf { it.amount }
    val cashOut = entries.filter { it.paymentMode == "CASH" && it.type == "OUT" }.sumOf { it.amount }
    val netCash = cashIn - cashOut

    val digitalIn = entries.filter { it.paymentMode != "CASH" && it.type == "IN" }.sumOf { it.amount }
    val digitalOut = entries.filter { it.paymentMode != "CASH" && it.type == "OUT" }.sumOf { it.amount }
    val netDigital = digitalIn - digitalOut

    val netTotal = netCash + netDigital

    var showDialog by remember { mutableStateOf(false) }
    var dialogType by remember { mutableStateOf("IN") } // IN or OUT

    Scaffold(
        containerColor = SoftBackground,
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite,
                shadowElevation = 8.dp,
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            dialogType = "IN"
                            showDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JamaGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Cash In", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            dialogType = "OUT"
                            showDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UdharRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("- Cash Out", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Net Total Balance & Separated Cash vs Digital Breakdown
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        LanguageStrings.get("net_total_cash").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${"%.2f".format(netTotal)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (netTotal >= 0) PrimaryBlue else UdharRed
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = CardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2-Column Split: Cash In Hand vs Digital (UPI/Bank)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cash in Hand
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftBackground)
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(LanguageStrings.get("cash_in_hand"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹${"%.2f".format(netCash)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = if (netCash >= 0) JamaGreen else UdharRed)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("In: ₹${"%.2f".format(cashIn)}", style = MaterialTheme.typography.labelSmall, color = JamaGreen)
                            Text("Out: ₹${"%.2f".format(cashOut)}", style = MaterialTheme.typography.labelSmall, color = UdharRed)
                        }

                        // Digital (UPI/Bank)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftBackground)
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = JamaGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(LanguageStrings.get("digital_cash"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹${"%.2f".format(netDigital)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = if (netDigital >= 0) JamaGreen else UdharRed)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("In: ₹${"%.2f".format(digitalIn)}", style = MaterialTheme.typography.labelSmall, color = JamaGreen)
                            Text("Out: ₹${"%.2f".format(digitalOut)}", style = MaterialTheme.typography.labelSmall, color = UdharRed)
                        }
                    }
                }
            }

            // Entries List
            Text(
                "Cashbook Entries (${entries.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No cashbook entries yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text("Record cash in or cash out using the buttons below", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        val isIn = entry.type == "IN"
                        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(entry.date))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isIn) JamaGreen.copy(alpha = 0.15f) else UdharRed.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (isIn) JamaGreen else UdharRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(entry.category, fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (entry.paymentMode == "CASH") PrimaryBlue.copy(alpha = 0.12f) else JamaGreen.copy(alpha = 0.12f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    entry.paymentMode,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (entry.paymentMode == "CASH") PrimaryBlue else JamaGreen
                                                )
                                            }
                                        }
                                        Text(
                                            if (entry.remark.isNotBlank()) "${entry.remark} • $dateStr" else dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = "${if (isIn) "+" else "-"}₹${"%.2f".format(entry.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIn) JamaGreen else UdharRed,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var amountText by remember { mutableStateOf("") }
        var categoryText by remember { mutableStateOf("") }
        var remarkText by remember { mutableStateOf("") }
        var selectedMode by remember { mutableStateOf("CASH") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(if (dialogType == "IN") "Add Cash In" else "Add Cash Out (Kharcha)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(LanguageStrings.get("payment_mode_label"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CASH", "UPI", "BANK", "CHEQUE").forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = { categoryText = it },
                        label = { Text("Category") },
                        placeholder = { Text(if (dialogType == "IN") "e.g. Counter Sales, Advance" else "e.g. Chai, Transport, Rent") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = remarkText,
                        onValueChange = { remarkText = it },
                        label = { Text("Remark / Note") },
                        placeholder = { Text("Optional details") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            repository.addCashEntry(
                                type = dialogType,
                                amount = amt,
                                category = if (categoryText.isNotBlank()) categoryText else (if (dialogType == "IN") "Counter Cash" else "Shop Expense"),
                                remark = remarkText,
                                paymentMode = selectedMode,
                                source = "manual"
                            )
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (dialogType == "IN") JamaGreen else UdharRed)
                ) {
                    Text("Save Entry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
