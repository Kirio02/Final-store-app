package com.example.model

import java.util.UUID

data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val address: String = "",
    val gstin: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val amount: Double,
    val type: String, // "GAVE" (Udhar) or "GOT" (Jama)
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val isCancelled: Boolean = false, // Manual cancel out tick box
    val billNumber: String = ""
)

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val itemCode: String = "",
    val salePrice: Double,
    val purchasePrice: Double = 0.0,
    val stock: Int = 0,
    val category: String = "General",
    val sizes: List<String> = emptyList(),
    val gstPercent: Double = 0.0
)

data class InvoiceItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Int = 1,
    val unitPrice: Double,
    val gstPercent: Double = 0.0,
    val total: Double = quantity * unitPrice
)

data class Invoice(
    val id: String = UUID.randomUUID().toString(),
    val billNumber: String,
    val date: Long = System.currentTimeMillis(),
    val customerName: String,
    val customerPhone: String = "",
    val items: List<InvoiceItem> = emptyList(),
    val subtotal: Double = 0.0,
    val gstAmount: Double = 0.0,
    val discount: Double = 0.0,
    val additionalCharges: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMode: String = "CASH", // CASH, UPI, CREDIT
    val stateOfSupply: String = "Gujarat",
    val gstin: String = "",
    val notes: String = "",
    val themeName: String = "PREMIUM_BLUE"
)

data class StoreProfile(
    val storeName: String = "",
    val address: String = "",
    val phone: String = "",
    val upiId: String = "",
    val gstin: String = "",
    val ownerName: String = "",
    val state: String = "Gujarat"
)

data class CashEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "IN" or "OUT"
    val amount: Double,
    val category: String,
    val remark: String = "",
    val date: Long = System.currentTimeMillis(),
    val paymentMode: String = "CASH", // CASH, UPI, BANK, CHEQUE
    val linkedId: String? = null,
    val source: String = "manual" // "manual", "invoice", "khata", "settlement"
)

data class ImportResult(
    val success: Boolean,
    val customerCount: Int = 0,
    val transactionCount: Int = 0,
    val inventoryCount: Int = 0,
    val invoiceCount: Int = 0,
    val cashbookCount: Int = 0,
    val errorMessage: String? = null,
    val repairedCount: Int = 0
) {
    val isValid: Boolean get() = success
    val message: String get() = errorMessage ?: (
        if (repairedCount > 0) {
            "Import successful ($customerCount customers, $transactionCount txs, $inventoryCount items, $invoiceCount bills, $cashbookCount cash entries). Note: $repairedCount unlinked transaction(s) attached to 'Recovered entries'."
        } else {
            "Import successful ($customerCount customers, $transactionCount txs, $inventoryCount items, $invoiceCount bills, $cashbookCount cash entries)"
        }
    )
}

data class CsvCustomerPreview(
    val name: String,
    val phone: String,
    val balance: Double,
    val isDuplicate: Boolean
)

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    HINGLISH("hinglish", "Hinglish", "Hinglish (Default)"),
    HINDI("hi", "Hindi", "हिंदी"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી"),
    ENGLISH("en", "English", "English")
}

data class InvoiceThemeConfig(
    val id: String,
    val name: String,
    val primaryColorHex: String,
    val accentColorHex: String,
    val isPremium: Boolean = false,
    val badgeText: String = ""
)
