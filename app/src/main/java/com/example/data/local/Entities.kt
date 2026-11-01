package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.model.*
import java.util.UUID

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val address: String = "",
    val gstin: String = "",
    val type: String = "CUSTOMER", // "CUSTOMER" or "SUPPLIER"
    val createdAt: Long = System.currentTimeMillis()
)

fun CustomerEntity.toDomain() = Customer(
    id = id,
    name = name,
    phone = phone,
    address = address,
    gstin = gstin,
    type = type,
    createdAt = createdAt
)

fun Customer.toEntity() = CustomerEntity(
    id = id,
    name = name,
    phone = phone,
    address = address,
    gstin = gstin,
    type = type,
    createdAt = createdAt
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["date"]),
        Index(value = ["linkedToTxId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val amount: Double,
    val type: String, // "GAVE", "GOT"
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val paymentMode: String = "CASH",
    val linkedToTxId: String? = null,
    val isCompleted: Boolean = false,
    val billNumber: String = ""
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    customerId = customerId,
    amount = amount,
    type = type,
    note = note,
    date = date,
    paymentMode = paymentMode,
    linkedToTxId = linkedToTxId,
    isCompleted = isCompleted,
    billNumber = billNumber
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    customerId = customerId,
    amount = amount,
    type = type,
    note = note,
    date = date,
    paymentMode = paymentMode,
    linkedToTxId = linkedToTxId,
    isCompleted = isCompleted,
    billNumber = billNumber
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val itemCode: String = "",
    val salePrice: Double,
    val purchasePrice: Double,
    val stock: Int,
    val category: String = "General",
    val sizes: List<String> = emptyList(),
    val gstPercent: Double = 0.0
)

fun InventoryItemEntity.toDomain() = InventoryItem(
    id = id,
    name = name,
    itemCode = itemCode,
    salePrice = salePrice,
    purchasePrice = purchasePrice,
    stock = stock,
    category = category,
    sizes = sizes,
    gstPercent = gstPercent
)

fun InventoryItem.toEntity() = InventoryItemEntity(
    id = id,
    name = name,
    itemCode = itemCode,
    salePrice = salePrice,
    purchasePrice = purchasePrice,
    stock = stock,
    category = category,
    sizes = sizes,
    gstPercent = gstPercent
)

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["date"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val billNumber: String,
    val date: Long = System.currentTimeMillis(),
    val customerName: String,
    val customerPhone: String = "",
    val subtotal: Double,
    val gstAmount: Double,
    val discount: Double = 0.0,
    val additionalCharges: Double = 0.0,
    val totalAmount: Double,
    val paymentMode: String = "CASH",
    val stateOfSupply: String = "Gujarat",
    val gstin: String = "",
    val notes: String = "",
    val themeName: String = "PREMIUM_BLUE"
)

@Entity(
    tableName = "invoice_items",
    indices = [
        Index(value = ["invoiceId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InvoiceItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val invoiceId: String,
    val name: String,
    val quantity: Int = 1,
    val unitPrice: Double,
    val gstPercent: Double = 0.0,
    val total: Double
)

fun InvoiceItemEntity.toDomain() = InvoiceItem(
    id = id,
    name = name,
    quantity = quantity,
    unitPrice = unitPrice,
    gstPercent = gstPercent,
    total = total
)

fun InvoiceItem.toEntity(invoiceId: String) = InvoiceItemEntity(
    id = id,
    invoiceId = invoiceId,
    name = name,
    quantity = quantity,
    unitPrice = unitPrice,
    gstPercent = gstPercent,
    total = total
)

data class InvoiceWithItems(
    @Embedded
    val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemEntity>
)

fun InvoiceWithItems.toDomain() = Invoice(
    id = invoice.id,
    billNumber = invoice.billNumber,
    date = invoice.date,
    customerName = invoice.customerName,
    customerPhone = invoice.customerPhone,
    items = items.map { it.toDomain() },
    subtotal = invoice.subtotal,
    gstAmount = invoice.gstAmount,
    discount = invoice.discount,
    additionalCharges = invoice.additionalCharges,
    totalAmount = invoice.totalAmount,
    paymentMode = invoice.paymentMode,
    stateOfSupply = invoice.stateOfSupply,
    gstin = invoice.gstin,
    notes = invoice.notes,
    themeName = invoice.themeName
)

fun Invoice.toEntity() = InvoiceEntity(
    id = id,
    billNumber = billNumber,
    date = date,
    customerName = customerName,
    customerPhone = customerPhone,
    subtotal = subtotal,
    gstAmount = gstAmount,
    discount = discount,
    additionalCharges = additionalCharges,
    totalAmount = totalAmount,
    paymentMode = paymentMode,
    stateOfSupply = stateOfSupply,
    gstin = gstin,
    notes = notes,
    themeName = themeName
)

@Entity(
    tableName = "cash_entries",
    indices = [
        Index(value = ["date"])
    ]
)
data class CashEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "IN", "OUT"
    val amount: Double,
    val category: String,
    val remark: String = "",
    val date: Long = System.currentTimeMillis(),
    val paymentMode: String = "CASH",
    val linkedId: String? = null,
    val source: String = "manual"
)

fun CashEntryEntity.toDomain() = CashEntry(
    id = id,
    type = type,
    amount = amount,
    category = category,
    remark = remark,
    date = date,
    paymentMode = paymentMode,
    linkedId = linkedId,
    source = source
)

fun CashEntry.toEntity() = CashEntryEntity(
    id = id,
    type = type,
    amount = amount,
    category = category,
    remark = remark,
    date = date,
    paymentMode = paymentMode,
    linkedId = linkedId,
    source = source
)

@Entity(tableName = "store_profile")
data class StoreProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "",
    val address: String = "",
    val phone: String = "",
    val upiId: String = "",
    val gstin: String = "",
    val ownerName: String = "",
    val state: String = "Gujarat"
)

fun StoreProfileEntity.toDomain() = StoreProfile(
    storeName = storeName,
    address = address,
    phone = phone,
    upiId = upiId,
    gstin = gstin,
    ownerName = ownerName,
    state = state
)

fun StoreProfile.toEntity() = StoreProfileEntity(
    id = 1,
    storeName = storeName,
    address = address,
    phone = phone,
    upiId = upiId,
    gstin = gstin,
    ownerName = ownerName,
    state = state
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val language: String = "hinglish",
    val theme: String = "PREMIUM_BLUE",
    val hasCompletedOnboarding: Boolean = false
)
