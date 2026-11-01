package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.AppRepository
import com.example.model.AppLanguage
import com.example.model.Customer
import com.example.model.PartyType
import com.example.ui.theme.*
import com.example.ui.util.LanguageStrings
import com.example.ui.util.PhoneUtil
import java.net.URLEncoder

@Composable
fun KhataScreen(
    repository: AppRepository,
    onCustomerClick: (Customer) -> Unit
) {
    val context = LocalContext.current
    val customers by repository.customers.collectAsState()
    val transactions by repository.transactions.collectAsState()
    val currentLang by repository.language.collectAsState()
    val storeProfile by repository.storeProfile.collectAsState()
    val lang = currentLang

    val totalReceivable = remember(transactions, customers) { repository.getTotalReceivable() }
    val totalPayable = remember(transactions, customers) { repository.getTotalPayable() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, CUSTOMERS, SUPPLIERS, PENDING
    var showAddPartyDialog by remember { mutableStateOf(false) }

    val filteredCustomers = remember(customers, transactions, searchQuery, selectedFilter) {
        customers.filter { cust ->
            val matchesSearch = cust.name.contains(searchQuery, ignoreCase = true) ||
                    cust.phone.contains(searchQuery)
            val balance = repository.getCustomerBalance(cust.id)
            val matchesFilter = when (selectedFilter) {
                "CUSTOMERS" -> cust.type == PartyType.CUSTOMER.raw
                "SUPPLIERS" -> cust.type == PartyType.SUPPLIER.raw
                "PENDING" -> Math.abs(balance) > 0.001
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        containerColor = SoftBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPartyDialog = true },
                containerColor = PrimaryBlue,
                contentColor = SurfaceWhite,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = LanguageStrings.get("add_party", lang)) },
                text = { Text(LanguageStrings.get("add_party", lang), fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Receivable / Payable Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = JamaGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = LanguageStrings.get("total_receivable", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${"%.2f".format(totalReceivable)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = JamaGreen
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = UdharRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = LanguageStrings.get("total_payable", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${"%.2f".format(totalPayable)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = UdharRed
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(LanguageStrings.get("search_hint", lang), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                )
            )

            // Filter Chips: All / Customers / Suppliers / Pending
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "${LanguageStrings.get("parties", lang)} (${customers.size})",
                    "CUSTOMERS" to LanguageStrings.get("filter_customers", lang),
                    "SUPPLIERS" to LanguageStrings.get("filter_suppliers", lang),
                    "PENDING" to LanguageStrings.get("filter_pending", lang)
                )
                items(filters) { (key, label) ->
                    val isSelected = selectedFilter == key
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedFilter = key },
                        color = if (isSelected) PrimaryBlue else ChipInactiveBg,
                        border = if (isSelected) null else CardDefaults.outlinedCardBorder()
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) SurfaceWhite else TextPrimary
                        )
                    }
                }
            }

            // Party List
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (customers.isEmpty()) "Khata is clean & empty" else "No matching parties",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (customers.isEmpty()) "Tap '+ ${LanguageStrings.get("add_party", lang)}' to start recording" else "Try clearing your search or filter",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCustomers) { cust ->
                        val balance = repository.getCustomerBalance(cust.id)
                        val isSupplier = cust.type == PartyType.SUPPLIER.raw

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCustomerClick(cust) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isSupplier) ChipInactiveBg else ChipActiveBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cust.name.firstOrNull()?.uppercase() ?: "P",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSupplier) TextPrimary else PrimaryBlue,
                                            fontSize = 18.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = cust.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSupplier) ChipInactiveBg else ChipActiveBg
                                            ) {
                                                Text(
                                                    text = if (isSupplier) LanguageStrings.get("supplier", lang) else LanguageStrings.get("customer", lang),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isSupplier) TextSecondary else PrimaryBlue,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        val displayPhone = PhoneUtil.toDisplay(cust.phone)
                                        Text(
                                            text = if (displayPhone.isNotBlank()) displayPhone else LanguageStrings.get("add_phone_number_hint", lang),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (displayPhone.isNotBlank()) TextSecondary else TextTertiary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${"%.2f".format(Math.abs(balance))}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            balance > 0.001 -> JamaGreen
                                            balance < -0.001 -> UdharRed
                                            else -> TextSecondary
                                        }
                                    )
                                    Text(
                                        text = when {
                                            balance > 0.001 -> "Lena Hai (Get)"
                                            balance < -0.001 -> "Dena Hai (Give)"
                                            else -> "Settled"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            balance > 0.001 -> JamaGreen
                                            balance < -0.001 -> UdharRed
                                            else -> TextSecondary
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Party Dialog
    if (showAddPartyDialog) {
        var partyType by remember { mutableStateOf(PartyType.CUSTOMER) }
        var partyName by remember { mutableStateOf("") }
        var partyPhone by remember { mutableStateOf("") }
        var partyAddress by remember { mutableStateOf("") }
        var openingBalance by remember { mutableStateOf("") }
        var notifyOnWhatsApp by remember { mutableStateOf(true) }

        // Contact Picker Launcher via ACTION_PICK on Phone.CONTENT_URI (no READ_CONTACTS needed)
        val contactPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val contactUri = result.data?.data ?: return@rememberLauncherForActivityResult
                try {
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (nameIdx >= 0) {
                                val retrievedName = cursor.getString(nameIdx)
                                if (!retrievedName.isNullOrBlank()) {
                                    partyName = retrievedName
                                }
                            }
                            if (numIdx >= 0) {
                                val retrievedNum = cursor.getString(numIdx)
                                if (!retrievedNum.isNullOrBlank()) {
                                    partyPhone = PhoneUtil.normalize(retrievedNum)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("KhataScreen", "Error picking phone contact: ${e.message}", e)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddPartyDialog = false },
            title = { Text(LanguageStrings.get("add_party", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Party Type Selector (Customer vs Supplier)
                    Text(
                        text = LanguageStrings.get("party_type_label", lang),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = partyType == PartyType.CUSTOMER,
                            onClick = { partyType = PartyType.CUSTOMER },
                            label = { Text(LanguageStrings.get("customer", lang)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = partyType == PartyType.SUPPLIER,
                            onClick = { partyType = PartyType.SUPPLIER },
                            label = { Text(LanguageStrings.get("supplier", lang)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Contact import button
                    OutlinedButton(
                        onClick = {
                            try {
                                val pickIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                contactPickerLauncher.launch(pickIntent)
                            } catch (e: Exception) {
                                android.util.Log.e("KhataScreen", "Failed to launch contact picker: ${e.message}", e)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import from Contacts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                    }

                    HorizontalDivider(color = CardBorder)

                    OutlinedTextField(
                        value = partyName,
                        onValueChange = { partyName = it },
                        label = { Text("${LanguageStrings.get("party", lang)} Name *") },
                        placeholder = { Text("e.g. Ramesh Kumar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = partyPhone,
                        onValueChange = { partyPhone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("e.g. 98765 43210") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = partyAddress,
                        onValueChange = { partyAddress = it },
                        label = { Text("Address (Optional)") },
                        placeholder = { Text("Shop or locality") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = openingBalance,
                        onValueChange = { openingBalance = it },
                        label = { Text(LanguageStrings.get("opening_balance_label", lang)) },
                        placeholder = { Text("₹ 0") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Notify on WhatsApp switch
                    val hasPhone = partyPhone.isNotBlank()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageStrings.get("notify_whatsapp", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasPhone) TextPrimary else TextTertiary
                        )
                        Switch(
                            checked = notifyOnWhatsApp && hasPhone,
                            onCheckedChange = { notifyOnWhatsApp = it },
                            enabled = hasPhone
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (partyName.isNotBlank()) {
                            val cleanPhone = PhoneUtil.normalize(partyPhone)
                            val addedCustomer = repository.addCustomer(
                                name = partyName.trim(),
                                phone = cleanPhone,
                                address = partyAddress.trim(),
                                type = partyType.raw
                            )
                            val opBal = openingBalance.trim().toDoubleOrNull() ?: 0.0
                            if (opBal > 0.001) {
                                // For Customer: positive balance means you will get -> GAVE
                                // For Supplier: you owe them -> GOT
                                val txType = if (partyType == PartyType.CUSTOMER) "GAVE" else "GOT"
                                repository.addTransaction(
                                    customerId = addedCustomer.id,
                                    type = txType,
                                    amount = opBal,
                                    note = "Opening Balance",
                                    paymentMode = "CASH"
                                )
                            }
                            if (notifyOnWhatsApp && cleanPhone.isNotBlank()) {
                                sendWhatsAppAddNotification(
                                    context = context,
                                    phone = cleanPhone,
                                    partyName = partyName.trim(),
                                    shopName = storeProfile.storeName.ifBlank { "Ghanshyam Kids Wear" },
                                    openingBal = opBal,
                                    lang = lang
                                )
                            }
                            showAddPartyDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(LanguageStrings.get("btn_save", lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPartyDialog = false }) {
                    Text(LanguageStrings.get("btn_cancel", lang))
                }
            }
        )
    }
}

private fun sendWhatsAppAddNotification(
    context: Context,
    phone: String,
    partyName: String,
    shopName: String,
    openingBal: Double,
    lang: AppLanguage
) {
    val waNumber = PhoneUtil.toWhatsAppNumber(phone)
    if (waNumber.isBlank()) return
    val opBalStr = if (openingBal > 0.001) "%.2f".format(openingBal) else "0.00"
    val message = LanguageStrings.get("notify_added_message", lang).format(partyName, shopName, opBalStr)
    try {
        val encodedMsg = URLEncoder.encode(message, "UTF-8")
        val waUri = Uri.parse("https://wa.me/$waNumber?text=$encodedMsg")
        val waIntent = Intent(Intent.ACTION_VIEW, waUri)
        context.startActivity(waIntent)
    } catch (e: Exception) {
        // Fallback to SMS
        try {
            val smsUri = Uri.parse("smsto:$waNumber")
            val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                putExtra("sms_body", message)
            }
            context.startActivity(smsIntent)
        } catch (se: Exception) {
            android.util.Log.e("KhataScreen", "Cannot launch WhatsApp or SMS: ${se.message}", se)
        }
    }
}
