package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppRepository
import com.example.model.Customer
import com.example.model.Transaction
import com.example.ui.theme.*
import com.example.ui.util.LanguageStrings
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: Customer,
    repository: AppRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLang by repository.language.collectAsState()
    val storeProfile by repository.storeProfile.collectAsState()
    val transactions by repository.transactions.collectAsState()
    val customerTx = transactions.filter { it.customerId == customer.id }
    val balance = repository.getCustomerBalance(customer.id)

    val customers by repository.customers.collectAsState()
    val currentCustomer = customers.find { it.id == customer.id } ?: customer
    val hasPhone = currentCustomer.phone.isNotBlank()

    var showAddTxDialog by remember { mutableStateOf(false) }
    var txTypeToAdd by remember { mutableStateOf("GAVE") } // GAVE or GOT

    // Smart Settle dialog state
    var txToSettle by remember { mutableStateOf<Transaction?>(null) }
    var settleAmountText by remember { mutableStateOf("") }

    var showEditPhoneDialog by remember { mutableStateOf(false) }
    var phoneInputText by remember { mutableStateOf(currentCustomer.phone) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.clickable { if (!hasPhone) { phoneInputText = ""; showEditPhoneDialog = true } }) {
                        Text(currentCustomer.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 18.sp)
                        Text(
                            text = if (hasPhone) currentCustomer.phone else LanguageStrings.get("add_phone_number_hint", currentLang),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasPhone) TextSecondary else UdharRed,
                            fontWeight = if (hasPhone) FontWeight.Normal else FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (hasPhone) {
                                val uri = Uri.parse("tel:${currentCustomer.phone}")
                                context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                            }
                        },
                        enabled = hasPhone
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Call",
                            tint = if (hasPhone) PrimaryBlue else TextTertiary
                        )
                    }
                    IconButton(
                        onClick = {
                            if (hasPhone) {
                                val shopName = storeProfile.storeName.ifBlank { "Ghanshyam Kids Wear" }
                                val upiText = if (storeProfile.upiId.isNotBlank()) " Kripya UPI id: ${storeProfile.upiId} par bhugtan karein." else ""
                                val shareText = "Namaste ${currentCustomer.name}, $shopName ki taraf se aapka pending hisaab: ₹${"%.2f".format(balance)}.$upiText"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share WhatsApp Reminder"))
                            }
                        },
                        enabled = hasPhone
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = if (hasPhone) JamaGreen else TextTertiary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
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
                            txTypeToAdd = "GAVE"
                            showAddTxDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UdharRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("You Gave (Udhar)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            txTypeToAdd = "GOT"
                            showAddTxDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JamaGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("You Got (Jama)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SoftBackground)
        ) {
            if (!hasPhone) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { phoneInputText = ""; showEditPhoneDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = ChipInactiveBg),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = LanguageStrings.get("add_phone_number_hint", currentLang),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Add +",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Net Balance Card (Khatabook color convention: Receivable/Lena Hai is JamaGreen, Payable/Dena Hai is UdharRed)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CURRENT NET BALANCE", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (balance != 0.0) {
                            Icon(
                                if (balance > 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (balance > 0) JamaGreen else UdharRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = "₹${"%.2f".format(Math.abs(balance))}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                balance > 0 -> JamaGreen
                                balance < 0 -> UdharRed
                                else -> TextPrimary
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            balance > 0 -> "You'll Receive (Aapko Lena Hai)"
                            balance < 0 -> "You'll Pay (Aapko Dena Hai)"
                            else -> "Account Settled (Hisaab Barabar)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            balance > 0 -> JamaGreen
                            balance < 0 -> UdharRed
                            else -> TextSecondary
                        },
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ChipInactiveBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tick any entry below to cancel it out live", fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }

            // Transactions Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction History (${customerTx.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Tap checkbox to cancel out",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            if (customerTx.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No entries yet for this customer", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Text("Add Udhar or Jama using buttons below", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customerTx) { tx ->
                        val isGave = tx.type == "GAVE"
                        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.date))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tx.isCancelled) ChipInactiveBg.copy(alpha = 0.5f) else SurfaceWhite
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Manual Tick Box (Smart Settle notebook method)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = tx.isCancelled,
                                        onCheckedChange = {
                                            if (tx.isCancelled) {
                                                // Un-cancel / restore transaction
                                                repository.toggleTransactionCancel(tx.id)
                                            } else {
                                                // Trigger Smart Settle dialog
                                                txToSettle = tx
                                                settleAmountText = "%.2f".format(tx.amount)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = JamaGreen,
                                            uncheckedColor = TextSecondary
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isGave) "You Gave (Udhar)" else "You Got (Jama)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tx.isCancelled) TextSecondary else TextPrimary
                                            )
                                            if (tx.isCancelled) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(JamaGreen.copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        "SETTLED",
                                                        color = JamaGreen,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (tx.note.isNotBlank()) "${tx.note} • $dateStr" else dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                // Amount with strikethrough if cancelled
                                Text(
                                    text = "${if (isGave) "-" else "+"}₹${"%.2f".format(tx.amount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (tx.isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (tx.isCancelled) {
                                        TextTertiary
                                    } else {
                                        if (isGave) UdharRed else JamaGreen
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTxDialog) {
        var amountText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf("") }
        var alsoAddToCashbook by remember { mutableStateOf(true) }
        var selectedPaymentMode by remember { mutableStateOf("CASH") }

        AlertDialog(
            onDismissRequest = { showAddTxDialog = false },
            title = {
                Text(if (txTypeToAdd == "GAVE") "You Gave (Udhar)" else "You Got (Jama)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Customer: ${customer.name}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        placeholder = { Text("e.g. 500") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note / Bill Description") },
                        placeholder = { Text("e.g. Wholesale garment items") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageStrings.get("also_add_cashbook", currentLang),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = alsoAddToCashbook,
                            onCheckedChange = { alsoAddToCashbook = it }
                        )
                    }

                    if (alsoAddToCashbook) {
                        Text(
                            text = LanguageStrings.get("payment_mode_label", currentLang),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("CASH", "UPI", "BANK", "CHEQUE").forEach { mode ->
                                FilterChip(
                                    selected = selectedPaymentMode == mode,
                                    onClick = { selectedPaymentMode = mode },
                                    label = { Text(mode, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            val tx = repository.addTransaction(
                                customerId = customer.id,
                                amount = amt,
                                type = txTypeToAdd,
                                note = noteText
                            )
                            if (alsoAddToCashbook) {
                                repository.addCashEntry(
                                    type = if (txTypeToAdd == "GOT") "IN" else "OUT",
                                    amount = amt,
                                    category = if (txTypeToAdd == "GOT") "Khata Settlement" else "Khata Credit",
                                    remark = if (txTypeToAdd == "GOT") "Received from ${customer.name}" else "Given to ${customer.name}",
                                    paymentMode = selectedPaymentMode,
                                    linkedId = tx.id,
                                    source = "khata"
                                )
                            }
                            showAddTxDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (txTypeToAdd == "GAVE") UdharRed else JamaGreen
                    )
                ) {
                    Text("Save Entry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTxDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Smart Settle Dialog (The Notebook Method)
    if (txToSettle != null) {
        val tx = txToSettle!!
        val isGave = tx.type == "GAVE"
        val parsedAmt = settleAmountText.toDoubleOrNull() ?: 0.0
        var alsoAddToCashbook by remember { mutableStateOf(true) }
        var settlePaymentMode by remember { mutableStateOf("CASH") }

        AlertDialog(
            onDismissRequest = { txToSettle = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = JamaGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Smart Settle Entry", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Customer: ${customer.name}",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Original Entry: ${if (isGave) "You Gave (Udhar)" else "You Got (Jama)"} of ₹${"%.2f".format(tx.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = settleAmountText,
                        onValueChange = { settleAmountText = it },
                        label = { Text("Settled Amount (₹)") },
                        placeholder = { Text("e.g. ${tx.amount}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick buttons for Full or Half payment
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { settleAmountText = "%.2f".format(tx.amount) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Full: ₹${"%.2f".format(tx.amount)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (tx.amount > 100) {
                            FilledTonalButton(
                                onClick = { settleAmountText = "%.2f".format(tx.amount / 2) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Half: ₹${"%.2f".format(tx.amount / 2)}", fontSize = 11.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageStrings.get("also_add_cashbook", currentLang),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = alsoAddToCashbook,
                            onCheckedChange = { alsoAddToCashbook = it }
                        )
                    }

                    if (alsoAddToCashbook) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("CASH", "UPI", "BANK", "CHEQUE").forEach { mode ->
                                FilterChip(
                                    selected = settlePaymentMode == mode,
                                    onClick = { settlePaymentMode = mode },
                                    label = { Text(mode, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }

                    // Explanation Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ChipInactiveBg)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = when {
                                parsedAmt >= tx.amount ->
                                    "✓ Full Payment: Original line will be crossed out${if (alsoAddToCashbook) " and ₹${"%.2f".format(tx.amount)} recorded in Cashbook as ${if (isGave) "Cash IN ($settlePaymentMode)" else "Cash OUT ($settlePaymentMode)"}" else ""}."
                                parsedAmt in 0.01..<tx.amount ->
                                    "✓ Partial Payment: Original line will be crossed out${if (alsoAddToCashbook) ", ₹${"%.2f".format(parsedAmt)} recorded in Cashbook as ${if (isGave) "Cash IN ($settlePaymentMode)" else "Cash OUT ($settlePaymentMode)"}" else ""}, and a new entry of ₹${"%.2f".format(tx.amount - parsedAmt)} will be added for the remaining balance."
                                else -> "Enter the payment amount to settle."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (parsedAmt > 0) {
                            repository.smartSettleTransaction(
                                transactionId = tx.id,
                                settledAmount = parsedAmt,
                                customerName = customer.name,
                                paymentMode = settlePaymentMode,
                                addToCashbook = alsoAddToCashbook
                            )
                            txToSettle = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JamaGreen)
                ) {
                    Text("Confirm Settle", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToSettle = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showEditPhoneDialog = false },
            title = { Text("Update Phone Number", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Add a phone number to enable Call and WhatsApp reminders for ${currentCustomer.name}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = phoneInputText,
                        onValueChange = { phoneInputText = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("e.g. +91 98765 43210") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = phoneInputText.trim()
                        repository.updateCustomer(
                            id = currentCustomer.id,
                            name = currentCustomer.name,
                            phone = cleaned,
                            address = currentCustomer.address,
                            gstin = currentCustomer.gstin
                        )
                        showEditPhoneDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPhoneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
