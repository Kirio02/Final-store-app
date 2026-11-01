package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.AppRepository
import com.example.model.Customer
import com.example.model.PartyType
import com.example.model.Transaction
import com.example.ui.theme.*
import com.example.ui.util.LanguageStrings
import com.example.ui.util.PhoneUtil
import com.example.ui.util.StatementDateRange
import com.example.ui.util.StatementFilter
import com.example.ui.util.StatementGenerator
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val customers by repository.customers.collectAsState()

    val currentCustomer = customers.find { it.id == customer.id } ?: customer
    val isSupplier = currentCustomer.type == PartyType.SUPPLIER.raw
    val hasPhone = currentCustomer.phone.isNotBlank()
    val customerTx = transactions.filter { it.customerId == currentCustomer.id }
        .sortedByDescending { it.date }
    val balance = repository.getCustomerBalance(currentCustomer.id)

    // Multi-select state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedBillIds = remember { mutableStateListOf<String>() }

    // Dialogs state
    var showAddTxDialog by remember { mutableStateOf(false) }
    var txTypeToAdd by remember { mutableStateOf("GAVE") } // GAVE or GOT

    var billToMarkReceived by remember { mutableStateOf<Transaction?>(null) }
    var showMarkAllReceivedDialog by remember { mutableStateOf(false) }
    var showStatementDialog by remember { mutableStateOf(false) }
    var txToEdit by remember { mutableStateOf<Transaction?>(null) }
    var showEditPhoneDialog by remember { mutableStateOf(false) }
    var phoneInputText by remember { mutableStateOf(currentCustomer.phone) }

    // Back handling for multi-select
    BackHandler(enabled = isMultiSelectMode) {
        isMultiSelectMode = false
        selectedBillIds.clear()
    }

    Scaffold(
        topBar = {
            if (isMultiSelectMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedBillIds.size} ${LanguageStrings.get("selected_bills", currentLang)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedBillIds.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Selection")
                        }
                    },
                    actions = {
                        if (selectedBillIds.isNotEmpty()) {
                            IconButton(onClick = { showMarkAllReceivedDialog = true }) {
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = LanguageStrings.get("mark_all_received", currentLang),
                                    tint = JamaGreen
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ChipActiveBg)
                )
            } else {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.clickable {
                                if (!hasPhone) {
                                    phoneInputText = ""
                                    showEditPhoneDialog = true
                                }
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentCustomer.name,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSupplier) ChipInactiveBg else ChipActiveBg
                                ) {
                                    Text(
                                        text = if (isSupplier) LanguageStrings.get("supplier", currentLang) else LanguageStrings.get("customer", currentLang),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSupplier) TextSecondary else PrimaryBlue,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            val displayPhone = PhoneUtil.toDisplay(currentCustomer.phone)
                            Text(
                                text = if (displayPhone.isNotBlank()) displayPhone else LanguageStrings.get("add_phone_number_hint", currentLang),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (displayPhone.isNotBlank()) TextSecondary else UdharRed,
                                fontWeight = if (displayPhone.isNotBlank()) FontWeight.Normal else FontWeight.SemiBold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    actions = {
                        // Call
                        IconButton(
                            onClick = {
                                if (hasPhone) {
                                    val dialNumber = PhoneUtil.toWhatsAppNumber(currentCustomer.phone)
                                    val uri = Uri.parse("tel:$dialNumber")
                                    context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                                }
                            },
                            enabled = hasPhone
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = LanguageStrings.get("btn_call", currentLang),
                                tint = if (hasPhone) PrimaryBlue else TextTertiary
                            )
                        }

                        // SMS Reminder
                        IconButton(
                            onClick = {
                                if (hasPhone) {
                                    sendSmsReminder(
                                        context = context,
                                        party = currentCustomer,
                                        shopName = storeProfile.storeName.ifBlank { "Ghanshyam Kids Wear" },
                                        balance = balance,
                                        upiId = storeProfile.upiId,
                                        lang = currentLang
                                    )
                                }
                            },
                            enabled = hasPhone
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = LanguageStrings.get("btn_sms", currentLang),
                                tint = if (hasPhone) PrimaryBlue else TextTertiary
                            )
                        }

                        // WhatsApp Reminder
                        IconButton(
                            onClick = {
                                if (hasPhone) {
                                    sendWhatsAppReminder(
                                        context = context,
                                        party = currentCustomer,
                                        shopName = storeProfile.storeName.ifBlank { "Ghanshyam Kids Wear" },
                                        balance = balance,
                                        upiId = storeProfile.upiId,
                                        lang = currentLang
                                    )
                                }
                            },
                            enabled = hasPhone
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = LanguageStrings.get("btn_whatsapp", currentLang),
                                tint = if (hasPhone) JamaGreen else TextTertiary
                            )
                        }

                        // Statement Dialog
                        IconButton(onClick = { showStatementDialog = true }) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = LanguageStrings.get("statement", currentLang),
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
                )
            }
        },
        bottomBar = {
            if (isMultiSelectMode) {
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showMarkAllReceivedDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = JamaGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = selectedBillIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${LanguageStrings.get("mark_all_received", currentLang)} (${selectedBillIds.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
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
                        // Left Button
                        val leftLabel = if (isSupplier) LanguageStrings.get("you_gave_supplier", currentLang) else LanguageStrings.get("you_gave", currentLang)
                        val leftColor = if (isSupplier) PrimaryBlue else UdharRed
                        Button(
                            onClick = {
                                txTypeToAdd = "GAVE"
                                showAddTxDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = leftColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(leftLabel, fontWeight = FontWeight.Bold)
                        }

                        // Right Button
                        val rightLabel = if (isSupplier) LanguageStrings.get("you_got_supplier", currentLang) else LanguageStrings.get("you_got", currentLang)
                        val rightColor = if (isSupplier) UdharRed else JamaGreen
                        Button(
                            onClick = {
                                txTypeToAdd = "GOT"
                                showAddTxDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = rightColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(rightLabel, fontWeight = FontWeight.Bold)
                        }
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
            // Missing phone prompt banner
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

            // Current Net Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = LanguageStrings.get("closing_balance", currentLang).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (Math.abs(balance) > 0.001) {
                            Icon(
                                imageVector = if (balance > 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
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
                                balance > 0.001 -> JamaGreen
                                balance < -0.001 -> UdharRed
                                else -> TextPrimary
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            balance > 0.001 -> "Lena Hai (You'll Get)"
                            balance < -0.001 -> "Dena Hai (You'll Give)"
                            else -> "Account Settled (Hisaab Barabar)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            balance > 0.001 -> JamaGreen
                            balance < -0.001 -> UdharRed
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
                        Text(
                            text = "Hold any bill to select multiple • Mark received or edit entries",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Timeline Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Entries (${customerTx.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (customerTx.isNotEmpty() && !isMultiSelectMode) {
                    Text(
                        text = "Hold to select",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            if (customerTx.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No entries yet for this party", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Text("Add credit bills or payments using buttons below", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customerTx, key = { it.id }) { tx ->
                        val isCreditBill = (currentCustomer.type == PartyType.CUSTOMER.raw && tx.type == "GAVE" && tx.linkedToTxId == null) ||
                                (currentCustomer.type == PartyType.SUPPLIER.raw && tx.type == "GOT" && tx.linkedToTxId == null)
                        val isLinkedPayment = tx.linkedToTxId != null
                        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tx.date))

                        if (isCreditBill) {
                            val receivedAmt = repository.getBillReceivedAmount(tx.id)
                            val outstandingAmt = repository.getBillOutstandingAmount(tx)
                            val status = repository.getBillStatus(tx)
                            val isSelected = selectedBillIds.contains(tx.id)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (isMultiSelectMode) {
                                                if (isSelected) selectedBillIds.remove(tx.id) else selectedBillIds.add(tx.id)
                                            } else {
                                                txToEdit = tx
                                            }
                                        },
                                        onLongClick = {
                                            if (!isMultiSelectMode) {
                                                isMultiSelectMode = true
                                                selectedBillIds.clear()
                                                selectedBillIds.add(tx.id)
                                            }
                                        }
                                    ),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) ChipActiveBg else SurfaceWhite
                                ),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            if (isMultiSelectMode) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        if (checked == true) selectedBillIds.add(tx.id) else selectedBillIds.remove(tx.id)
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }

                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = if (isSupplier) LanguageStrings.get("you_got_supplier", currentLang) else LanguageStrings.get("you_gave", currentLang),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    // Status Chip
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = when (status) {
                                                            AppRepository.BillStatus.RECEIVED -> JamaGreen.copy(alpha = 0.15f)
                                                            AppRepository.BillStatus.PARTLY_RECEIVED -> WarningAmber.copy(alpha = 0.15f)
                                                            AppRepository.BillStatus.PENDING -> UdharRed.copy(alpha = 0.15f)
                                                        }
                                                    ) {
                                                        val statusLabel = when (status) {
                                                            AppRepository.BillStatus.RECEIVED -> LanguageStrings.get("status_received", currentLang)
                                                            AppRepository.BillStatus.PARTLY_RECEIVED -> "${LanguageStrings.get("status_partly_received", currentLang)} (₹${"%.0f".format(receivedAmt)})"
                                                            AppRepository.BillStatus.PENDING -> LanguageStrings.get("status_pending", currentLang)
                                                        }
                                                        Text(
                                                            text = statusLabel,
                                                            color = when (status) {
                                                                AppRepository.BillStatus.RECEIVED -> JamaGreen
                                                                AppRepository.BillStatus.PARTLY_RECEIVED -> WarningAmber
                                                                AppRepository.BillStatus.PENDING -> UdharRed
                                                            },
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
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

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "₹${"%.2f".format(tx.amount)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSupplier) JamaGreen else UdharRed
                                            )
                                            if (outstandingAmt > 0.001 && status != AppRepository.BillStatus.PENDING) {
                                                Text(
                                                    text = "Baki: ₹${"%.2f".format(outstandingAmt)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = UdharRed,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Action row: Delivered/Packed checkbox + Mark Received Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Plain Completed checkbox (e.g. goods delivered / packed) - NO MONEY EFFECT
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = tx.isCompleted,
                                                onCheckedChange = { repository.toggleBillCompleted(tx.id) },
                                                colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                            )
                                            Text(
                                                text = LanguageStrings.get("completed_checkbox", currentLang),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (tx.isCompleted) PrimaryBlue else TextSecondary,
                                                fontWeight = if (tx.isCompleted) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }

                                        // Mark Received Action
                                        if (outstandingAmt > 0.001) {
                                            FilledTonalButton(
                                                onClick = { billToMarkReceived = tx },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = LanguageStrings.get("mark_received", currentLang),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Payment entry or other transaction
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { txToEdit = tx },
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
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (tx.type == "GOT") JamaGreen.copy(alpha = 0.15f) else UdharRed.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (tx.type == "GOT") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (tx.type == "GOT") JamaGreen else UdharRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (tx.type == "GOT") {
                                                        if (isSupplier) LanguageStrings.get("you_got_supplier", currentLang) else LanguageStrings.get("you_got", currentLang)
                                                    } else {
                                                        if (isSupplier) LanguageStrings.get("you_gave_supplier", currentLang) else LanguageStrings.get("you_gave", currentLang)
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = ChipInactiveBg
                                                ) {
                                                    Text(
                                                        text = tx.paymentMode,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextSecondary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
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

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (tx.type == "GOT") "+" else "-"}₹${"%.2f".format(tx.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.type == "GOT") JamaGreen else UdharRed
                                        )

                                        if (isLinkedPayment) {
                                            TextButton(
                                                onClick = { repository.undoPayment(tx.id) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) {
                                                Text(
                                                    text = LanguageStrings.get("undo_received", currentLang),
                                                    fontSize = 11.sp,
                                                    color = UdharRed,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Transaction Dialog (Udhar / Jama)
    if (showAddTxDialog) {
        var amountText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf("") }
        var alsoAddToCashbook by remember { mutableStateOf(true) }
        var selectedPaymentMode by remember { mutableStateOf("CASH") }

        val isGave = txTypeToAdd == "GAVE"
        val dialogTitle = if (isGave) {
            if (isSupplier) LanguageStrings.get("you_gave_supplier", currentLang) else LanguageStrings.get("you_gave", currentLang)
        } else {
            if (isSupplier) LanguageStrings.get("you_got_supplier", currentLang) else LanguageStrings.get("you_got", currentLang)
        }

        AlertDialog(
            onDismissRequest = { showAddTxDialog = false },
            title = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${LanguageStrings.get("party", currentLang)}: ${currentCustomer.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(LanguageStrings.get("amount_label", currentLang)) },
                        placeholder = { Text("e.g. 1500") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(LanguageStrings.get("note_label", currentLang)) },
                        placeholder = { Text("e.g. Wholesale garment bill") },
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
                        if (amt > 0.001) {
                            val newTx = repository.addTransaction(
                                customerId = currentCustomer.id,
                                amount = amt,
                                type = txTypeToAdd,
                                note = noteText.trim(),
                                paymentMode = selectedPaymentMode
                            )
                            if (alsoAddToCashbook) {
                                val isCashIn = txTypeToAdd == "GOT"
                                repository.addCashEntry(
                                    type = if (isCashIn) "IN" else "OUT",
                                    amount = amt,
                                    category = if (isCashIn) "Khata Settlement" else "Khata Credit",
                                    remark = if (isCashIn) "From ${currentCustomer.name}" else "To ${currentCustomer.name}",
                                    paymentMode = selectedPaymentMode,
                                    linkedId = newTx.id,
                                    source = "khata"
                                )
                            }
                            showAddTxDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGave) (if (isSupplier) PrimaryBlue else UdharRed) else (if (isSupplier) UdharRed else JamaGreen)
                    )
                ) {
                    Text(LanguageStrings.get("btn_save", currentLang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTxDialog = false }) {
                    Text(LanguageStrings.get("btn_cancel", currentLang))
                }
            }
        )
    }

    // Mark Bill Received Dialog
    if (billToMarkReceived != null) {
        val bill = billToMarkReceived!!
        val outstanding = repository.getBillOutstandingAmount(bill)
        var receivedInput by remember { mutableStateOf("%.2f".format(outstanding)) }
        var selectedMode by remember { mutableStateOf("CASH") }
        var alsoCashbook by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { billToMarkReceived = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = JamaGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(LanguageStrings.get("mark_received_sheet_title", currentLang), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${LanguageStrings.get("party", currentLang)}: ${currentCustomer.name}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${LanguageStrings.get("total_bill_amount", currentLang)}: ₹${"%.2f".format(bill.amount)} • ${LanguageStrings.get("outstanding_amount", currentLang)}: ₹${"%.2f".format(outstanding)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = receivedInput,
                        onValueChange = { receivedInput = it },
                        label = { Text(LanguageStrings.get("amount_label", currentLang)) },
                        placeholder = { Text("e.g. $outstanding") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { receivedInput = "%.2f".format(outstanding) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Full: ₹${"%.2f".format(outstanding)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (outstanding > 100) {
                            FilledTonalButton(
                                onClick = { receivedInput = "%.2f".format(outstanding / 2) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Half: ₹${"%.2f".format(outstanding / 2)}", fontSize = 11.sp)
                            }
                        }
                    }

                    Text(
                        text = LanguageStrings.get("payment_mode_label", currentLang),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CASH", "UPI", "BANK", "CHEQUE").forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
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
                            checked = alsoCashbook,
                            onCheckedChange = { alsoCashbook = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = receivedInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0.001) {
                            repository.markBillReceived(
                                billId = bill.id,
                                amount = amt,
                                paymentMode = selectedMode,
                                addToCashbook = alsoCashbook
                            )
                            billToMarkReceived = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JamaGreen)
                ) {
                    Text(LanguageStrings.get("btn_save", currentLang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { billToMarkReceived = null }) {
                    Text(LanguageStrings.get("btn_cancel", currentLang))
                }
            }
        )
    }

    // Mark All Received Dialog (Multi-select)
    if (showMarkAllReceivedDialog) {
        var bulkPaymentMode by remember { mutableStateOf("CASH") }
        var bulkAddToCashbook by remember { mutableStateOf(true) }

        val totalSelectedOutstanding = remember(selectedBillIds.toList(), customerTx) {
            customerTx.filter { selectedBillIds.contains(it.id) }
                .sumOf { repository.getBillOutstandingAmount(it) }
        }

        AlertDialog(
            onDismissRequest = { showMarkAllReceivedDialog = false },
            title = { Text(LanguageStrings.get("mark_all_received", currentLang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${selectedBillIds.size} ${LanguageStrings.get("selected_bills", currentLang)}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Total outstanding amount to record: ₹${"%.2f".format(totalSelectedOutstanding)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JamaGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = LanguageStrings.get("payment_mode_label", currentLang),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CASH", "UPI", "BANK", "CHEQUE").forEach { mode ->
                            FilterChip(
                                selected = bulkPaymentMode == mode,
                                onClick = { bulkPaymentMode = mode },
                                label = { Text(mode, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
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
                            checked = bulkAddToCashbook,
                            onCheckedChange = { bulkAddToCashbook = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.markBillsAllReceived(
                            billIds = selectedBillIds.toList(),
                            paymentMode = bulkPaymentMode,
                            addToCashbook = bulkAddToCashbook
                        )
                        showMarkAllReceivedDialog = false
                        isMultiSelectMode = false
                        selectedBillIds.clear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JamaGreen)
                ) {
                    Text(LanguageStrings.get("btn_save", currentLang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkAllReceivedDialog = false }) {
                    Text(LanguageStrings.get("btn_cancel", currentLang))
                }
            }
        )
    }

    // Statement Dialog (PDF & WhatsApp Text Export)
    if (showStatementDialog) {
        var filterType by remember { mutableStateOf(StatementFilter.ALL_ENTRIES) }
        var dateRange by remember { mutableStateOf(StatementDateRange.ALL) }

        AlertDialog(
            onDismissRequest = { showStatementDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(LanguageStrings.get("statement", currentLang), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${LanguageStrings.get("party", currentLang)}: ${currentCustomer.name}",
                        fontWeight = FontWeight.SemiBold
                    )

                    // Statement Type Filter Chips
                    Text(
                        text = "Statement Type",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterType == StatementFilter.ALL_ENTRIES,
                            onClick = { filterType = StatementFilter.ALL_ENTRIES },
                            label = { Text(LanguageStrings.get("full_statement", currentLang), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = filterType == StatementFilter.PENDING_BILLS_ONLY,
                            onClick = { filterType = StatementFilter.PENDING_BILLS_ONLY },
                            label = { Text(LanguageStrings.get("pending_bills_only", currentLang), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Date Range Filter Chips
                    Text(
                        text = "Date Range",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = dateRange == StatementDateRange.THIS_MONTH,
                            onClick = { dateRange = StatementDateRange.THIS_MONTH },
                            label = { Text(LanguageStrings.get("date_this_month", currentLang), fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = dateRange == StatementDateRange.THREE_MONTHS,
                            onClick = { dateRange = StatementDateRange.THREE_MONTHS },
                            label = { Text(LanguageStrings.get("date_3_months", currentLang), fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = dateRange == StatementDateRange.ALL,
                            onClick = { dateRange = StatementDateRange.ALL },
                            label = { Text(LanguageStrings.get("date_all", currentLang), fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = CardBorder)

                    // Share PDF Button
                    Button(
                        onClick = {
                            try {
                                val statementData = StatementGenerator.computeStatement(
                                    customer = currentCustomer,
                                    allTransactions = customerTx,
                                    dateRange = dateRange,
                                    filter = filterType
                                )
                                val pdfFile = StatementGenerator.generateStatementPdf(
                                    context = context,
                                    statement = statementData,
                                    storeProfile = storeProfile
                                )
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    pdfFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Statement PDF"))
                                showStatementDialog = false
                            } catch (e: Exception) {
                                android.util.Log.e("CustomerDetailScreen", "Failed to generate statement PDF: ${e.message}", e)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(LanguageStrings.get("share_pdf", currentLang), fontWeight = FontWeight.Bold)
                    }

                    // Share WhatsApp Text Button
                    OutlinedButton(
                        onClick = {
                            try {
                                val statementData = StatementGenerator.computeStatement(
                                    customer = currentCustomer,
                                    allTransactions = customerTx,
                                    dateRange = dateRange,
                                    filter = filterType
                                )
                                val textContent = StatementGenerator.generateStatementText(
                                    statement = statementData,
                                    storeProfile = storeProfile,
                                    language = currentLang
                                )
                                val waNumber = PhoneUtil.toWhatsAppNumber(currentCustomer.phone)
                                if (waNumber.isNotBlank()) {
                                    val enc = URLEncoder.encode(textContent, "UTF-8")
                                    val waUri = Uri.parse("https://wa.me/$waNumber?text=$enc")
                                    val waIntent = Intent(Intent.ACTION_VIEW, waUri)
                                    context.startActivity(waIntent)
                                } else {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, textContent)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Statement Text"))
                                }
                                showStatementDialog = false
                            } catch (e: Exception) {
                                android.util.Log.e("CustomerDetailScreen", "Failed to share statement text: ${e.message}", e)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = JamaGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(LanguageStrings.get("share_whatsapp_text", currentLang), fontWeight = FontWeight.Bold, color = JamaGreen)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStatementDialog = false }) {
                    Text(LanguageStrings.get("btn_dismiss", currentLang))
                }
            }
        )
    }

    // Edit Transaction Dialog
    if (txToEdit != null) {
        val editingTx = txToEdit!!
        var editAmount by remember { mutableStateOf("%.2f".format(editingTx.amount)) }
        var editNote by remember { mutableStateOf(editingTx.note) }
        var editMode by remember { mutableStateOf(editingTx.paymentMode) }

        AlertDialog(
            onDismissRequest = { txToEdit = null },
            title = { Text(LanguageStrings.get("edit_entry", currentLang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text(LanguageStrings.get("amount_label", currentLang)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text(LanguageStrings.get("note_label", currentLang)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = LanguageStrings.get("payment_mode_label", currentLang),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CASH", "UPI", "BANK", "CHEQUE").forEach { mode ->
                            FilterChip(
                                selected = editMode == mode,
                                onClick = { editMode = mode },
                                label = { Text(mode, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    // Delete button inside edit dialog
                    TextButton(
                        onClick = {
                            repository.deleteTransaction(editingTx.id)
                            txToEdit = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = UdharRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(LanguageStrings.get("btn_delete", currentLang), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = editAmount.toDoubleOrNull() ?: 0.0
                        if (parsed > 0.001) {
                            repository.editTransaction(
                                txId = editingTx.id,
                                amount = parsed,
                                note = editNote.trim(),
                                date = editingTx.date,
                                paymentMode = editMode
                            )
                            txToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(LanguageStrings.get("btn_save", currentLang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToEdit = null }) {
                    Text(LanguageStrings.get("btn_cancel", currentLang))
                }
            }
        )
    }

    // Edit Phone Number Dialog
    if (showEditPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showEditPhoneDialog = false },
            title = { Text("Update Phone Number", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Add a phone number to enable Call and WhatsApp reminders for ${currentCustomer.name}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = phoneInputText,
                        onValueChange = { phoneInputText = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("e.g. 98765 43210") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = PhoneUtil.normalize(phoneInputText)
                        repository.updateCustomer(
                            id = currentCustomer.id,
                            name = currentCustomer.name,
                            phone = cleaned,
                            address = currentCustomer.address,
                            gstin = currentCustomer.gstin,
                            type = currentCustomer.type
                        )
                        showEditPhoneDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(LanguageStrings.get("btn_save", currentLang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPhoneDialog = false }) {
                    Text(LanguageStrings.get("btn_cancel", currentLang))
                }
            }
        )
    }
}

private fun sendWhatsAppReminder(
    context: Context,
    party: Customer,
    shopName: String,
    balance: Double,
    upiId: String,
    lang: com.example.model.AppLanguage
) {
    val waNumber = PhoneUtil.toWhatsAppNumber(party.phone)
    if (waNumber.isBlank()) return

    val upiPart = if (upiId.isNotBlank()) " UPI ID: $upiId" else ""
    val balStr = "%.2f".format(Math.abs(balance))
    val msg = LanguageStrings.get("reminder_message", lang).format(party.name, shopName, balStr, upiPart)
    try {
        val enc = URLEncoder.encode(msg, "UTF-8")
        val uri = Uri.parse("https://wa.me/$waNumber?text=$enc")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to SMS
        try {
            val smsUri = Uri.parse("smsto:$waNumber")
            val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                putExtra("sms_body", msg)
            }
            context.startActivity(smsIntent)
        } catch (se: Exception) {
            android.util.Log.e("CustomerDetailScreen", "Cannot open WhatsApp or SMS: ${se.message}", se)
        }
    }
}

private fun sendSmsReminder(
    context: Context,
    party: Customer,
    shopName: String,
    balance: Double,
    upiId: String,
    lang: com.example.model.AppLanguage
) {
    val waNumber = PhoneUtil.toWhatsAppNumber(party.phone)
    if (waNumber.isBlank()) return
    val upiPart = if (upiId.isNotBlank()) " UPI ID: $upiId" else ""
    val balStr = "%.2f".format(Math.abs(balance))
    val msg = LanguageStrings.get("reminder_message", lang).format(party.name, shopName, balStr, upiPart)
    try {
        val smsUri = Uri.parse("smsto:$waNumber")
        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", msg)
        }
        context.startActivity(smsIntent)
    } catch (e: Exception) {
        android.util.Log.e("CustomerDetailScreen", "Cannot send SMS: ${e.message}", e)
    }
}
