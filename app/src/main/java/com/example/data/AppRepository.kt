package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AppRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ghanshyam_billing_prefs", Context.MODE_PRIVATE)

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices: StateFlow<List<Invoice>> = _invoices.asStateFlow()

    private val _cashbook = MutableStateFlow<List<CashEntry>>(emptyList())
    val cashbook: StateFlow<List<CashEntry>> = _cashbook.asStateFlow()

    private val _storeProfile = MutableStateFlow(StoreProfile())
    val storeProfile: StateFlow<StoreProfile> = _storeProfile.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.HINGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _selectedInvoiceTheme = MutableStateFlow("PREMIUM_BLUE")
    val selectedInvoiceTheme: StateFlow<String> = _selectedInvoiceTheme.asStateFlow()

    private val _corruptDataBanner = MutableStateFlow<String?>(null)
    val corruptDataBanner: StateFlow<String?> = _corruptDataBanner.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _hasCompletedOnboarding.value = prefs.getBoolean("has_completed_onboarding", false)

        val langCode = prefs.getString("selected_language", AppLanguage.HINGLISH.code)
        _language.value = AppLanguage.entries.find { it.code == langCode } ?: AppLanguage.HINGLISH

        _selectedInvoiceTheme.value = prefs.getString("invoice_theme", "PREMIUM_BLUE") ?: "PREMIUM_BLUE"

        // Load store profile with clean defaults
        val storeName = prefs.getString("store_name", "") ?: ""
        val storeAddress = prefs.getString("store_address", "") ?: ""
        val storePhone = prefs.getString("store_phone", "") ?: ""
        val storeUpi = prefs.getString("store_upi", "") ?: ""
        val storeGstin = prefs.getString("store_gstin", "") ?: ""
        val ownerName = prefs.getString("owner_name", "") ?: ""
        val storeState = prefs.getString("store_state", "Gujarat") ?: "Gujarat"

        _storeProfile.value = StoreProfile(
            storeName = storeName,
            address = storeAddress,
            phone = storePhone,
            upiId = storeUpi,
            gstin = storeGstin,
            ownerName = ownerName,
            state = storeState
        )

        val isInitialized = prefs.getBoolean("has_clean_initialized_v7", false)
        if (!isInitialized) {
            // Fresh clean start: everything empty as explicitly requested
            _customers.value = emptyList()
            _transactions.value = emptyList()
            _inventory.value = emptyList()
            _invoices.value = emptyList()
            _cashbook.value = emptyList()
            prefs.edit().putBoolean("has_clean_initialized_v7", true).commit()
            saveData()
        } else {
            loadFromStorage()
        }
    }

    fun completeOnboarding() {
        _hasCompletedOnboarding.value = true
        prefs.edit().putBoolean("has_completed_onboarding", true).commit()
    }

    fun resetOnboarding() {
        _hasCompletedOnboarding.value = false
        prefs.edit().putBoolean("has_completed_onboarding", false).commit()
    }

    fun setLanguage(newLanguage: AppLanguage) {
        _language.value = newLanguage
        prefs.edit().putString("selected_language", newLanguage.code).commit()
    }

    fun setInvoiceTheme(themeId: String) {
        _selectedInvoiceTheme.value = themeId
        prefs.edit().putString("invoice_theme", themeId).commit()
    }

    fun dismissCorruptDataBanner() {
        _corruptDataBanner.value = null
    }

    fun startFreshAfterCorruption() {
        _customers.value = emptyList()
        _transactions.value = emptyList()
        _inventory.value = emptyList()
        _invoices.value = emptyList()
        _cashbook.value = emptyList()
        _corruptDataBanner.value = null
        saveData()
    }

    fun updateStoreProfile(
        storeName: String,
        address: String,
        phone: String,
        upiId: String,
        gstin: String,
        ownerName: String,
        state: String = "Gujarat"
    ) {
        _storeProfile.value = StoreProfile(storeName, address, phone, upiId, gstin, ownerName, state)
        prefs.edit()
            .putString("store_name", storeName)
            .putString("store_address", address)
            .putString("store_phone", phone)
            .putString("store_upi", upiId)
            .putString("store_gstin", gstin)
            .putString("owner_name", ownerName)
            .putString("store_state", state)
            .commit()
    }

    // Customer operations
    fun addCustomer(name: String, phone: String, address: String = "", gstin: String = ""): Customer {
        val newCustomer = Customer(
            name = name.trim(),
            phone = phone.trim(),
            address = address.trim(),
            gstin = gstin.trim()
        )
        _customers.value = listOf(newCustomer) + _customers.value
        saveData()
        return newCustomer
    }

    fun deleteCustomer(customerId: String) {
        val custTxIds = _transactions.value.filter { it.customerId == customerId }.map { it.id }.toSet()
        _customers.value = _customers.value.filter { it.id != customerId }
        _transactions.value = _transactions.value.filter { it.customerId != customerId }
        _cashbook.value = _cashbook.value.filter { it.linkedId !in custTxIds }
        saveData()
    }

    // Transaction operations (Udhar / Jama)
    // Note: addTransaction() must NOT post to the cashbook by itself! Caller controls cashbook entry.
    fun addTransaction(
        customerId: String,
        amount: Double,
        type: String, // "GAVE" or "GOT"
        note: String = "",
        billNumber: String = ""
    ): Transaction {
        val newTx = Transaction(
            customerId = customerId,
            amount = amount,
            type = type,
            note = note,
            billNumber = billNumber
        )
        _transactions.value = listOf(newTx) + _transactions.value
        saveData()
        return newTx
    }

    // Manual tick box to cancel out an udhar entry (e.g. "You gave - 1850")
    fun toggleTransactionCancel(transactionId: String) {
        _transactions.value = _transactions.value.map { tx ->
            if (tx.id == transactionId) {
                tx.copy(isCancelled = !tx.isCancelled)
            } else {
                tx
            }
        }
        saveData()
    }

    // Smart Settle (The Notebook Method): Handles full or partial settlements and balances Cashbook
    fun smartSettleTransaction(
        transactionId: String,
        settledAmount: Double,
        customerName: String,
        paymentMode: String = "CASH",
        addToCashbook: Boolean = true
    ): Boolean {
        val tx = _transactions.value.find { it.id == transactionId } ?: return false
        val cleanSettled = settledAmount.coerceIn(0.01, tx.amount)

        // 1. Mark original transaction as cancelled (struck through in notebook)
        _transactions.value = _transactions.value.map {
            if (it.id == transactionId) it.copy(isCancelled = true) else it
        }

        // 2. Add balancing entry to Cashbook if enabled
        if (addToCashbook) {
            if (tx.type == "GAVE") {
                // Settling Udhar: customer gave payment to shop -> Cash IN
                addCashEntry(
                    type = "IN",
                    amount = cleanSettled,
                    category = "Khata Settlement",
                    remark = if (cleanSettled >= tx.amount - 0.001) {
                        "Settled Udhar from $customerName"
                    } else {
                        "Partial payment from $customerName (Orig: ₹${"%.2f".format(tx.amount)})"
                    },
                    paymentMode = paymentMode,
                    linkedId = tx.id,
                    source = "settlement"
                )
            } else if (tx.type == "GOT") {
                // Settling Jama / advance: shop returns/adjusts -> Cash OUT
                addCashEntry(
                    type = "OUT",
                    amount = cleanSettled,
                    category = "Khata Settlement",
                    remark = if (cleanSettled >= tx.amount - 0.001) {
                        "Settled Jama for $customerName"
                    } else {
                        "Partial settlement for $customerName"
                    },
                    paymentMode = paymentMode,
                    linkedId = tx.id,
                    source = "settlement"
                )
            }
        }

        // 3. Partial Payment: If settledAmount < tx.amount
        val remaining = tx.amount - cleanSettled
        if (remaining > 0.009) {
            val remainingTx = Transaction(
                customerId = tx.customerId,
                amount = remaining,
                type = tx.type,
                note = "Remaining balance (after ₹${"%.2f".format(cleanSettled)} settled)${if (tx.note.isNotBlank()) " • " + tx.note else ""}",
                billNumber = tx.billNumber,
                date = System.currentTimeMillis()
            )
            _transactions.value = listOf(remainingTx) + _transactions.value
        }

        saveData()
        return true
    }

    fun deleteTransaction(transactionId: String) {
        _transactions.value = _transactions.value.filter { it.id != transactionId }
        // Money-moving logic is reversible: remove linked cashbook entries
        _cashbook.value = _cashbook.value.filter { it.linkedId != transactionId }
        saveData()
    }

    // Balance calculations
    fun getCustomerBalance(customerId: String): Double {
        val activeTx = _transactions.value.filter { it.customerId == customerId && !it.isCancelled }
        val gave = activeTx.filter { it.type == "GAVE" }.sumOf { it.amount }
        val got = activeTx.filter { it.type == "GOT" }.sumOf { it.amount }
        return gave - got // Positive means customer owes shop (Udhar/Receivable)
    }

    fun getTotalReceivable(): Double {
        return _customers.value.map { getCustomerBalance(it.id) }.filter { it > 0 }.sum()
    }

    fun getTotalPayable(): Double {
        return _customers.value.map { getCustomerBalance(it.id) }.filter { it < 0 }.sumOf { -it }
    }

    // Inventory operations
    fun addInventoryItem(
        name: String,
        itemCode: String,
        salePrice: Double,
        purchasePrice: Double = 0.0,
        stock: Int,
        category: String = "General",
        sizes: List<String> = emptyList(),
        gstPercent: Double = 0.0
    ): InventoryItem {
        val item = InventoryItem(
            name = name.trim(),
            itemCode = itemCode.trim(),
            salePrice = salePrice,
            purchasePrice = purchasePrice,
            stock = stock,
            category = category.trim(),
            sizes = sizes,
            gstPercent = gstPercent
        )
        _inventory.value = listOf(item) + _inventory.value
        saveData()
        return item
    }

    fun updateInventoryStock(itemId: String, delta: Int) {
        _inventory.value = _inventory.value.map { item ->
            if (item.id == itemId) {
                item.copy(stock = (item.stock + delta).coerceAtLeast(0))
            } else {
                item
            }
        }
        saveData()
    }

    fun deleteInventoryItem(itemId: String) {
        _inventory.value = _inventory.value.filter { it.id != itemId }
        saveData()
    }

    // Invoices operations
    fun createInvoice(invoice: Invoice): Invoice {
        _invoices.value = listOf(invoice) + _invoices.value

        // If items are in inventory, reduce stock
        invoice.items.forEach { line ->
            val matching = _inventory.value.find { it.name.equals(line.name, ignoreCase = true) }
            if (matching != null) {
                updateInventoryStock(matching.id, -line.quantity)
            }
        }

        // paymentMode CASH or UPI posts ONE Cash IN for invoice total (source = "invoice", linkedId = invoice.id)
        if (invoice.paymentMode == "CASH" || invoice.paymentMode == "UPI") {
            addCashEntry(
                type = "IN",
                amount = invoice.totalAmount,
                category = "Sales",
                remark = "Bill #${invoice.billNumber} (${if (invoice.customerName.isNotBlank()) invoice.customerName else "Cash Customer"})",
                paymentMode = invoice.paymentMode,
                linkedId = invoice.id,
                source = "invoice"
            )
        } else if (invoice.paymentMode == "CREDIT" && invoice.customerName.isNotBlank()) {
            // CREDIT posts nothing to the cashbook and creates the udhar (GAVE) entry only
            var customer = _customers.value.find { it.name.equals(invoice.customerName, ignoreCase = true) }
            if (customer == null) {
                customer = addCustomer(invoice.customerName, invoice.customerPhone)
            }
            addTransaction(
                customerId = customer.id,
                amount = invoice.totalAmount,
                type = "GAVE",
                note = "Bill #${invoice.billNumber}",
                billNumber = invoice.billNumber
            )
        }

        saveData()
        return invoice
    }

    fun deleteInvoice(invoiceId: String) {
        val invoice = _invoices.value.find { it.id == invoiceId } ?: return

        // 1. Restore inventory stock
        invoice.items.forEach { line ->
            val matching = _inventory.value.find { it.name.equals(line.name, ignoreCase = true) }
            if (matching != null) {
                updateInventoryStock(matching.id, line.quantity)
            }
        }

        // 2. Remove linked cashbook entries
        _cashbook.value = _cashbook.value.filter { it.linkedId != invoice.id }

        // 3. Remove linked credit transaction if created
        if (invoice.billNumber.isNotBlank()) {
            val txsToRemove = _transactions.value.filter { it.billNumber == invoice.billNumber }
            val txIds = txsToRemove.map { it.id }.toSet()
            _transactions.value = _transactions.value.filter { it.billNumber != invoice.billNumber }
            _cashbook.value = _cashbook.value.filter { it.linkedId !in txIds }
        }

        // 4. Remove invoice
        _invoices.value = _invoices.value.filter { it.id != invoiceId }
        saveData()
    }

    // Cashbook operations
    fun addCashEntry(
        type: String, // "IN" or "OUT"
        amount: Double,
        category: String,
        remark: String = "",
        paymentMode: String = "CASH",
        linkedId: String? = null,
        source: String = "manual"
    ): CashEntry {
        val entry = CashEntry(
            type = type,
            amount = amount,
            category = category,
            remark = remark,
            paymentMode = paymentMode,
            linkedId = linkedId,
            source = source
        )
        _cashbook.value = listOf(entry) + _cashbook.value
        saveData()
        return entry
    }

    fun deleteCashEntry(id: String) {
        _cashbook.value = _cashbook.value.filter { it.id != id }
        saveData()
    }

    // Real CSV Customer Import (columns: name, phone, balance; positive balance = customer owes shop)
    fun previewCustomersFromCsv(csvText: String): List<CsvCustomerPreview> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val existing = _customers.value
        val result = mutableListOf<CsvCustomerPreview>()

        val firstLineLower = lines[0].lowercase()
        val startIndex = if (firstLineLower.contains("name") || firstLineLower.contains("phone") || firstLineLower.contains("balance")) 1 else 0

        for (i in startIndex until lines.size) {
            val parts = lines[i].split(",").map { it.trim().removeSurrounding("\"") }
            if (parts.isEmpty()) continue
            val rawName = parts.getOrNull(0) ?: ""
            if (rawName.isBlank()) continue

            val rawPhone = parts.getOrNull(1) ?: ""
            val rawBalanceStr = parts.getOrNull(2) ?: "0"
            val balance = rawBalanceStr.toDoubleOrNull() ?: 0.0

            val normPhone = rawPhone.replace(Regex("[^0-9]"), "")
            val isDup = existing.any { cust ->
                val existingNorm = cust.phone.replace(Regex("[^0-9]"), "")
                if (normPhone.isNotBlank() && existingNorm.isNotBlank()) {
                    normPhone == existingNorm
                } else {
                    cust.name.trim().equals(rawName.trim(), ignoreCase = true)
                }
            } || result.any { r ->
                val prevNorm = r.phone.replace(Regex("[^0-9]"), "")
                if (normPhone.isNotBlank() && prevNorm.isNotBlank()) {
                    normPhone == prevNorm
                } else {
                    r.name.trim().equals(rawName.trim(), ignoreCase = true)
                }
            }

            result.add(CsvCustomerPreview(name = rawName, phone = rawPhone, balance = balance, isDuplicate = isDup))
        }
        return result
    }

    fun importCustomersFromCsv(previews: List<CsvCustomerPreview>): Int {
        val nonDuplicates = previews.filter { !it.isDuplicate }
        var importedCount = 0

        for (item in nonDuplicates) {
            val cust = addCustomer(name = item.name, phone = item.phone)
            if (Math.abs(item.balance) > 0.009) {
                addTransaction(
                    customerId = cust.id,
                    amount = Math.abs(item.balance),
                    type = if (item.balance > 0) "GAVE" else "GOT",
                    note = "Opening Balance (CSV Import)"
                )
            }
            importedCount++
        }
        saveData()
        return importedCount
    }

    fun importCustomersFromCsv(csvText: String): Pair<Int, Int> {
        val preview = previewCustomersFromCsv(csvText)
        val imported = importCustomersFromCsv(preview)
        return Pair(imported, preview.size - imported)
    }

    fun clearAllData() {
        _customers.value = emptyList()
        _transactions.value = emptyList()
        _inventory.value = emptyList()
        _invoices.value = emptyList()
        _cashbook.value = emptyList()
        saveData()
    }

    private fun saveData() {
        try {
            val json = JSONObject()

            // Customers
            val custArr = JSONArray()
            _customers.value.forEach { c ->
                val o = JSONObject()
                o.put("id", c.id)
                o.put("name", c.name)
                o.put("phone", c.phone)
                o.put("address", c.address)
                o.put("gstin", c.gstin)
                o.put("createdAt", c.createdAt)
                custArr.put(o)
            }
            json.put("customers", custArr)

            // Transactions
            val txArr = JSONArray()
            _transactions.value.forEach { t ->
                val o = JSONObject()
                o.put("id", t.id)
                o.put("customerId", t.customerId)
                o.put("amount", t.amount)
                o.put("type", t.type)
                o.put("note", t.note)
                o.put("date", t.date)
                o.put("isCancelled", t.isCancelled)
                o.put("billNumber", t.billNumber)
                txArr.put(o)
            }
            json.put("transactions", txArr)

            // Inventory
            val invArr = JSONArray()
            _inventory.value.forEach { item ->
                val o = JSONObject()
                o.put("id", item.id)
                o.put("name", item.name)
                o.put("itemCode", item.itemCode)
                o.put("salePrice", item.salePrice)
                o.put("purchasePrice", item.purchasePrice)
                o.put("stock", item.stock)
                o.put("category", item.category)
                o.put("sizes", JSONArray(item.sizes))
                o.put("gstPercent", item.gstPercent)
                invArr.put(o)
            }
            json.put("inventory", invArr)

            // Invoices
            val invsArr = JSONArray()
            _invoices.value.forEach { inv ->
                val o = JSONObject()
                o.put("id", inv.id)
                o.put("billNumber", inv.billNumber)
                o.put("date", inv.date)
                o.put("customerName", inv.customerName)
                o.put("customerPhone", inv.customerPhone)
                o.put("subtotal", inv.subtotal)
                o.put("gstAmount", inv.gstAmount)
                o.put("discount", inv.discount)
                o.put("additionalCharges", inv.additionalCharges)
                o.put("totalAmount", inv.totalAmount)
                o.put("paymentMode", inv.paymentMode)
                o.put("stateOfSupply", inv.stateOfSupply)
                o.put("gstin", inv.gstin)
                o.put("notes", inv.notes)
                o.put("themeName", inv.themeName)

                val itemsArr = JSONArray()
                inv.items.forEach { line ->
                    val io = JSONObject()
                    io.put("id", line.id)
                    io.put("name", line.name)
                    io.put("quantity", line.quantity)
                    io.put("unitPrice", line.unitPrice)
                    io.put("gstPercent", line.gstPercent)
                    io.put("total", line.total)
                    itemsArr.put(io)
                }
                o.put("items", itemsArr)
                invsArr.put(o)
            }
            json.put("invoices", invsArr)

            // Cashbook
            val cashArr = JSONArray()
            _cashbook.value.forEach { c ->
                val o = JSONObject()
                o.put("id", c.id)
                o.put("type", c.type)
                o.put("amount", c.amount)
                o.put("category", c.category)
                o.put("remark", c.remark)
                o.put("date", c.date)
                o.put("paymentMode", c.paymentMode)
                if (c.linkedId != null) o.put("linkedId", c.linkedId)
                o.put("source", c.source)
                cashArr.put(o)
            }
            json.put("cashbook", cashArr)

            // Critical write: use commit() rather than apply()
            prefs.edit().putString("app_saved_data", json.toString()).commit()
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to save data: ${e.message}", e)
        }
    }

    private fun loadFromStorage() {
        val saved = prefs.getString("app_saved_data", null) ?: return
        try {
            val json = JSONObject(saved)

            // Parse into temporary lists first
            val tempCustomers = mutableListOf<Customer>()
            val tempTransactions = mutableListOf<Transaction>()
            val tempInventory = mutableListOf<InventoryItem>()
            val tempInvoices = mutableListOf<Invoice>()
            val tempCashbook = mutableListOf<CashEntry>()

            if (json.has("customers")) {
                val arr = json.getJSONArray("customers")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    tempCustomers.add(Customer(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        phone = o.optString("phone", ""),
                        address = o.optString("address", ""),
                        gstin = o.optString("gstin", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
            }

            if (json.has("transactions")) {
                val arr = json.getJSONArray("transactions")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    tempTransactions.add(Transaction(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        customerId = o.optString("customerId", ""),
                        amount = o.optDouble("amount", 0.0),
                        type = o.optString("type", "GAVE"),
                        note = o.optString("note", ""),
                        date = o.optLong("date", System.currentTimeMillis()),
                        isCancelled = o.optBoolean("isCancelled", false),
                        billNumber = o.optString("billNumber", "")
                    ))
                }
            }

            if (json.has("inventory")) {
                val arr = json.getJSONArray("inventory")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val sizesArr = o.optJSONArray("sizes")
                    val sizes = mutableListOf<String>()
                    if (sizesArr != null) {
                        for (s in 0 until sizesArr.length()) {
                            sizes.add(sizesArr.getString(s))
                        }
                    }
                    tempInventory.add(InventoryItem(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", ""),
                        itemCode = o.optString("itemCode", ""),
                        salePrice = o.optDouble("salePrice", 0.0),
                        purchasePrice = o.optDouble("purchasePrice", 0.0),
                        stock = o.optInt("stock", 0),
                        category = o.optString("category", "General"),
                        sizes = sizes,
                        gstPercent = o.optDouble("gstPercent", 0.0)
                    ))
                }
            }

            if (json.has("invoices")) {
                val arr = json.getJSONArray("invoices")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val itemsArr = o.optJSONArray("items")
                    val itemsList = mutableListOf<InvoiceItem>()
                    if (itemsArr != null) {
                        for (j in 0 until itemsArr.length()) {
                            val io = itemsArr.getJSONObject(j)
                            itemsList.add(InvoiceItem(
                                id = io.optString("id", UUID.randomUUID().toString()),
                                name = io.optString("name", ""),
                                quantity = io.optInt("quantity", 1),
                                unitPrice = io.optDouble("unitPrice", 0.0),
                                gstPercent = io.optDouble("gstPercent", 0.0),
                                total = io.optDouble("total", 0.0)
                            ))
                        }
                    }
                    tempInvoices.add(Invoice(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        billNumber = o.optString("billNumber", ""),
                        date = o.optLong("date", System.currentTimeMillis()),
                        customerName = o.optString("customerName", ""),
                        customerPhone = o.optString("customerPhone", ""),
                        items = itemsList,
                        subtotal = o.optDouble("subtotal", 0.0),
                        gstAmount = o.optDouble("gstAmount", 0.0),
                        discount = o.optDouble("discount", 0.0),
                        additionalCharges = o.optDouble("additionalCharges", 0.0),
                        totalAmount = o.optDouble("totalAmount", 0.0),
                        paymentMode = o.optString("paymentMode", "CASH"),
                        stateOfSupply = o.optString("stateOfSupply", "Gujarat"),
                        gstin = o.optString("gstin", ""),
                        notes = o.optString("notes", ""),
                        themeName = o.optString("themeName", "PREMIUM_BLUE")
                    ))
                }
            }

            if (json.has("cashbook")) {
                val arr = json.getJSONArray("cashbook")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    tempCashbook.add(CashEntry(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        type = o.optString("type", "IN"),
                        amount = o.optDouble("amount", 0.0),
                        category = o.optString("category", ""),
                        remark = o.optString("remark", ""),
                        date = o.optLong("date", System.currentTimeMillis()),
                        paymentMode = o.optString("paymentMode", "CASH"),
                        linkedId = if (o.has("linkedId") && !o.isNull("linkedId")) o.getString("linkedId") else null,
                        source = o.optString("source", "manual")
                    ))
                }
            }

            // Assign to state only if parsing completes successfully
            _customers.value = tempCustomers
            _transactions.value = tempTransactions
            _inventory.value = tempInventory
            _invoices.value = tempInvoices
            _cashbook.value = tempCashbook
            _corruptDataBanner.value = null
        } catch (e: Exception) {
            Log.e("AppRepository", "Error parsing saved data: ${e.message}", e)
            val timestamp = System.currentTimeMillis()
            prefs.edit().putString("app_saved_data_corrupt_$timestamp", saved).commit()
            _corruptDataBanner.value = "Data parsing error detected. Corrupt data backed up safely."
            // Do NOT call saveData() to prevent overwriting
        }
    }

    // ==========================================
    // HONEST PLAIN-JSON BACKUP & RESTORE
    // ==========================================

    fun exportDataAsJsonString(): String {
        return prefs.getString("app_saved_data", "{}") ?: "{}"
    }

    fun validateBackupJson(jsonStr: String): ImportResult {
        return try {
            val json = JSONObject(jsonStr)
            val expectedKeys = listOf("customers", "transactions", "inventory", "invoices", "cashbook")
            var foundAny = false
            for (k in expectedKeys) {
                if (json.has(k)) foundAny = true
            }
            if (!foundAny) {
                return ImportResult(success = false, errorMessage = "Backup JSON missing expected data arrays")
            }

            var custCount = 0
            if (json.has("customers")) {
                val arr = json.getJSONArray("customers")
                custCount = arr.length()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    if (o.optString("name", "").isBlank()) {
                        return ImportResult(success = false, errorMessage = "Customer at position $i missing name")
                    }
                }
            }

            var txCount = 0
            if (json.has("transactions")) {
                val arr = json.getJSONArray("transactions")
                txCount = arr.length()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    if (!o.has("amount")) {
                        return ImportResult(success = false, errorMessage = "Transaction at position $i missing amount")
                    }
                }
            }

            var invCount = 0
            if (json.has("inventory")) {
                val arr = json.getJSONArray("inventory")
                invCount = arr.length()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    if (o.optString("name", "").isBlank()) {
                        return ImportResult(success = false, errorMessage = "Inventory item at position $i missing name")
                    }
                }
            }

            var billCount = 0
            if (json.has("invoices")) {
                val arr = json.getJSONArray("invoices")
                billCount = arr.length()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    if (!o.has("billNumber")) {
                        return ImportResult(success = false, errorMessage = "Invoice at position $i missing billNumber")
                    }
                }
            }

            var cashCount = 0
            if (json.has("cashbook")) {
                val arr = json.getJSONArray("cashbook")
                cashCount = arr.length()
            }

            ImportResult(
                success = true,
                customerCount = custCount,
                transactionCount = txCount,
                inventoryCount = invCount,
                invoiceCount = billCount,
                cashbookCount = cashCount
            )
        } catch (e: Exception) {
            Log.e("AppRepository", "Validation failed for backup JSON: ${e.message}", e)
            ImportResult(success = false, errorMessage = "Invalid JSON structure: ${e.localizedMessage ?: "Syntax error"}")
        }
    }

    fun importDataFromJsonString(jsonStr: String): ImportResult {
        val validation = validateBackupJson(jsonStr)
        if (!validation.success) {
            return validation
        }

        return try {
            // Before replacing, save the current data under "app_saved_data_prev"
            val currentRaw = prefs.getString("app_saved_data", "{}") ?: "{}"
            prefs.edit().putString("app_saved_data_prev", currentRaw).commit()

            // Save the validated new data
            prefs.edit().putString("app_saved_data", jsonStr).commit()
            loadFromStorage()
            validation
        } catch (e: Exception) {
            Log.e("AppRepository", "Error applying backup: ${e.message}", e)
            ImportResult(success = false, errorMessage = "Error applying backup: ${e.localizedMessage}")
        }
    }

    fun exportBackupFile(context: Context): File {
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(backupDir, "GKW_Backup_$dateStr.json")
        file.writeText(exportDataAsJsonString())
        return file
    }

    fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ghanshyam Kids Wear Plain JSON Data Backup")
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is the plain JSON database backup file for Ghanshyam Kids Wear & Traders."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Plain JSON Backup File"))
    }

    // ==========================================
    // 3. EXCEL / CSV SPREADSHEET EXPORTS
    // ==========================================

    private fun escapeCsv(value: Any?): String {
        val str = value?.toString() ?: ""
        return if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }

    /**
     * Export Khata Ledger (CSV/Excel-compatible)
     * Columns: Customer Name, Phone, Net Balance, Last Transaction Date
     */
    fun exportKhataLedgerCsv(context: Context): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(reportsDir, "Khata_Ledger_$dateStr.csv")

        val sb = StringBuilder()
        sb.append("Customer Name,Phone,Net Balance,Last Transaction Date\n")

        _customers.value.forEach { customer ->
            val custTxns = _transactions.value.filter { it.customerId == customer.id && !it.isCancelled }
            val totalGave = custTxns.filter { it.type == "GAVE" }.sumOf { it.amount }
            val totalGot = custTxns.filter { it.type == "GOT" }.sumOf { it.amount }
            val netBalance = totalGave - totalGot
            val lastTxnDateMillis = custTxns.maxOfOrNull { it.date } ?: customer.createdAt
            val lastTxnDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(lastTxnDateMillis))

            sb.append(escapeCsv(customer.name)).append(",")
            sb.append(escapeCsv(customer.phone)).append(",")
            sb.append(escapeCsv("%.2f".format(netBalance))).append(",")
            sb.append(escapeCsv(lastTxnDateStr)).append("\n")
        }

        file.writeText(sb.toString())
        return file
    }

    /**
     * Export Sales Report (CSV/Excel-compatible)
     * Columns: Bill No, Date, Customer Name, Subtotal, GST, Discount, Total Amount, Payment Mode
     */
    fun exportSalesReportCsv(context: Context): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(reportsDir, "Sales_Report_$dateStr.csv")

        val sb = StringBuilder()
        sb.append("Bill No,Date,Customer Name,Subtotal,GST,Discount,Total Amount,Payment Mode\n")

        _invoices.value.forEach { invoice ->
            val dateFormatted = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date(invoice.date))
            sb.append(escapeCsv(invoice.billNumber)).append(",")
            sb.append(escapeCsv(dateFormatted)).append(",")
            sb.append(escapeCsv(invoice.customerName)).append(",")
            sb.append(escapeCsv("%.2f".format(invoice.subtotal))).append(",")
            sb.append(escapeCsv("%.2f".format(invoice.gstAmount))).append(",")
            sb.append(escapeCsv("%.2f".format(invoice.discount))).append(",")
            sb.append(escapeCsv("%.2f".format(invoice.totalAmount))).append(",")
            sb.append(escapeCsv(invoice.paymentMode)).append("\n")
        }

        file.writeText(sb.toString())
        return file
    }

    /**
     * Export Inventory Stock (CSV/Excel-compatible)
     * Columns: Item Code, Item Name, Category, Stock Quantity, Purchase Price, Sale Price
     */
    fun exportInventoryCsv(context: Context): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(reportsDir, "Inventory_Stock_$dateStr.csv")

        val sb = StringBuilder()
        sb.append("Item Code,Item Name,Category,Stock Quantity,Purchase Price,Sale Price\n")

        _inventory.value.forEach { item ->
            sb.append(escapeCsv(item.itemCode)).append(",")
            sb.append(escapeCsv(item.name)).append(",")
            sb.append(escapeCsv(item.category)).append(",")
            sb.append(escapeCsv(item.stock)).append(",")
            sb.append(escapeCsv("%.2f".format(item.purchasePrice))).append(",")
            sb.append(escapeCsv("%.2f".format(item.salePrice))).append("\n")
        }

        file.writeText(sb.toString())
        return file
    }

    fun shareCsvFile(context: Context, file: File, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            putExtra(Intent.EXTRA_TEXT, "Spreadsheet report from Ghanshyam Kids Wear & Traders.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
