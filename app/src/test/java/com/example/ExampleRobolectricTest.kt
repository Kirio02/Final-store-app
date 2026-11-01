package com.example

import android.content.Context
import com.example.data.AppRepository
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.ui.ai.AiResponse
import com.example.ui.ai.GhanshyamAiEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var repository: AppRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("ghanshyam_billing_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repository = AppRepository(context, asyncInit = false)
        repository.clearAllData()
    }

    @Test
    fun testInitialEmptyState() {
        assertTrue("Customers must be initially empty", repository.customers.value.isEmpty())
        assertTrue("Transactions must be initially empty", repository.transactions.value.isEmpty())
        assertTrue("Inventory must be initially empty", repository.inventory.value.isEmpty())
        assertTrue("Invoices must be initially empty", repository.invoices.value.isEmpty())
        assertTrue("Cashbook must be initially empty", repository.cashbook.value.isEmpty())
        assertFalse("Onboarding should be incomplete initially", repository.hasCompletedOnboarding.value)
    }

    @Test
    fun testTransactionCancelOutFeature() {
        val cust = repository.addCustomer("Rajesh Sharma", "+91 98200 12345")
        val txGave = repository.addTransaction(cust.id, 1850.0, "GAVE", "Old Udhar")
        val txGot = repository.addTransaction(cust.id, 400.0, "GOT", "Cash Payment")

        // Initial balance: 1850 - 400 = 1450
        assertEquals(1450.0, repository.getCustomerBalance(cust.id), 0.01)

        // Cancel out the 1850 udhar entry using the tick box feature
        repository.toggleTransactionCancel(txGave.id)

        // Balance must recalculate: 0 - 400 = -400 (shop owes customer or advance)
        assertEquals(-400.0, repository.getCustomerBalance(cust.id), 0.01)

        // Un-cancel
        repository.toggleTransactionCancel(txGave.id)
        assertEquals(1450.0, repository.getCustomerBalance(cust.id), 0.01)
    }

    @Test
    fun testKhataCashbookDecouplingAndReversibility() {
        val cust = repository.addCustomer("Sunil Kumar", "+91 91234 56789")

        // 1. Plain Khata addTransaction must NOT automatically post to cashbook
        val tx = repository.addTransaction(cust.id, 500.0, "GAVE", "Purchased shirt")
        assertEquals("Plain addTransaction must not post to cashbook", 0, repository.cashbook.value.size)

        // 2. Settlement with cashbook sync
        repository.smartSettleTransaction(
            transactionId = tx.id,
            settledAmount = 500.0,
            customerName = "Sunil Kumar",
            addToCashbook = true,
            paymentMode = "CASH"
        )
        assertEquals("Settling with cashbook sync adds 1 entry", 1, repository.cashbook.value.size)
        val settleEntry = repository.cashbook.value.first()
        assertEquals("IN", settleEntry.type)
        assertEquals(500.0, settleEntry.amount, 0.01)
        assertEquals("CASH", settleEntry.paymentMode)

        // 3. Invoice creation: CASH bill posts ONE cash in, CREDIT posts 0 cash entries
        val cashInvoice = Invoice(
            billNumber = "BILL-001",
            date = System.currentTimeMillis(),
            customerName = "Cash Customer",
            customerPhone = "",
            items = listOf(InvoiceItem(name = "T-Shirt", unitPrice = 200.0, quantity = 1, gstPercent = 5.0, total = 210.0)),
            subtotal = 200.0,
            gstAmount = 10.0,
            discount = 0.0,
            additionalCharges = 0.0,
            totalAmount = 210.0,
            paymentMode = "CASH"
        )
        repository.createInvoice(cashInvoice)
        assertEquals("Cash bill posts 1 additional entry to cashbook", 2, repository.cashbook.value.size)
        val invoiceCashEntry = repository.cashbook.value.find { it.linkedId == cashInvoice.id }
        assertNotNull("Invoice cashbook entry must exist", invoiceCashEntry)
        assertEquals("invoice", invoiceCashEntry!!.source)

        // 4. Deleting invoice must remove the linked cashbook entry
        repository.deleteInvoice(cashInvoice.id)
        assertNull("Deleted invoice cashbook entry must be removed", repository.cashbook.value.find { it.linkedId == cashInvoice.id })
    }

    @Test
    fun testRealCsvCustomerImport() {
        val csvData = """
            name,phone,balance
            Ramesh Kumar,9820011111,1500
            Suresh Shah,9820022222,-450
            ,9800000000,100
        """.trimIndent()

        val previews = repository.previewCustomersFromCsv(csvData)
        assertEquals("Should parse 2 valid rows and skip empty name", 2, previews.size)
        assertEquals("Ramesh Kumar", previews[0].name)
        assertEquals(1500.0, previews[0].balance, 0.01)

        val importedCount = repository.importCustomersFromCsv(previews)
        assertEquals("Should import 2 customers", 2, importedCount)
        assertEquals(2, repository.customers.value.size)

        val ramesh = repository.customers.value.find { it.name == "Ramesh Kumar" }
        assertNotNull(ramesh)
        assertEquals(1500.0, repository.getCustomerBalance(ramesh!!.id), 0.01)

        val suresh = repository.customers.value.find { it.name == "Suresh Shah" }
        assertNotNull(suresh)
        assertEquals(-450.0, repository.getCustomerBalance(suresh!!.id), 0.01)
    }

    @Test
    fun testAiEngineOutOfScopeAndReport() = runBlocking {
        val ai = GhanshyamAiEngine(repository)

        // Test out of scope
        val outOfScopeRes = ai.processQuery("What is the weather in Paris?")
        assertTrue("Should detect out of scope", outOfScopeRes is AiResponse.OutOfScope)

        // Test report generation
        val reportRes = ai.processQuery("Show me today sales report and statement")
        assertTrue("Should generate business report", reportRes is AiResponse.Success)
        val succ = reportRes as AiResponse.Success
        assertNotNull(succ.reportContent)
        assertTrue(succ.reportContent!!.contains("GHANSHYAM BILLING BUSINESS REPORT"))
    }

    @Test
    fun testPdfInvoiceGeneration() {
        val invoice = Invoice(
            billNumber = "INV-TEST-001",
            date = System.currentTimeMillis(),
            customerName = "Priya Sharma",
            customerPhone = "+91 98765 00000",
            items = listOf(
                InvoiceItem(
                    name = "Kids Cotton Frock",
                    quantity = 2,
                    unitPrice = 450.0,
                    gstPercent = 5.0,
                    total = 945.0
                )
            ),
            subtotal = 900.0,
            gstAmount = 45.0,
            discount = 45.0,
            additionalCharges = 0.0,
            totalAmount = 900.0,
            paymentMode = "CASH"
        )

        val pdfFile = com.example.ui.util.PdfInvoiceGenerator.generateInvoicePdf(
            context = context,
            storeProfile = repository.storeProfile.value,
            invoice = invoice
        )

        assertTrue("Generated PDF file should exist", pdfFile.exists())
        assertTrue("Generated PDF file should not be empty", pdfFile.length() > 0)
    }

    @Test
    fun testCsvSpreadsheetExports() {
        // Add sample data
        val cust = repository.addCustomer("Anita Verma", "+91 99999 11111")
        repository.addTransaction(cust.id, 1200.0, "GAVE", "Clothes")
        repository.addInventoryItem(
            name = "Boys Denim Jeans",
            itemCode = "BD-01",
            salePrice = 650.0,
            purchasePrice = 400.0,
            stock = 15,
            category = "Boys Wear",
            sizes = listOf("24", "26", "28"),
            gstPercent = 5.0
        )

        // 1. Khata Ledger CSV
        val khataCsv = repository.exportKhataLedgerCsv(context)
        assertTrue("Khata CSV exists", khataCsv.exists())
        val khataContent = khataCsv.readText()
        assertTrue("Header present in Khata CSV", khataContent.contains("Customer Name,Phone,Net Balance,Last Transaction Date"))
        assertTrue("Anita Verma included in CSV", khataContent.contains("Anita Verma"))

        // 2. Sales Report CSV
        val salesCsv = repository.exportSalesReportCsv(context)
        assertTrue("Sales CSV exists", salesCsv.exists())
        val salesContent = salesCsv.readText()
        assertTrue("Header present in Sales CSV", salesContent.contains("Bill No,Date,Customer Name,Subtotal,GST,Discount,Total Amount,Payment Mode"))

        // 3. Inventory Stock CSV
        val invCsv = repository.exportInventoryCsv(context)
        assertTrue("Inventory CSV exists", invCsv.exists())
        val invContent = invCsv.readText()
        assertTrue("Header present in Inventory CSV", invContent.contains("Item Code,Item Name,Category,Stock Quantity,Purchase Price,Sale Price"))
        assertTrue("Boys Denim Jeans present in CSV", invContent.contains("Boys Denim Jeans"))
    }

    @Test
    fun testBackupAndRestoreWithValidation() {
        repository.addCustomer("Deepak Patel", "+91 98765 43210")
        val jsonBackup = repository.exportDataAsJsonString()
        assertTrue("Backup contains customers", jsonBackup.contains("Deepak Patel"))

        val validation = repository.validateBackupJson(jsonBackup)
        assertTrue("Backup validation passes", validation.isValid)
        assertEquals(1, validation.customerCount)

        val backupFile = repository.exportBackupFile(context)
        assertTrue("Backup file created", backupFile.exists())
        assertTrue("Backup file has content", backupFile.length() > 0)

        // Clear and restore
        repository.clearAllData()
        assertTrue("Customers cleared", repository.customers.value.isEmpty())

        val importResult = repository.importDataFromJsonString(jsonBackup)
        assertTrue("Restore succeeded", importResult.success)
        assertEquals("Customer restored", 1, repository.customers.value.size)
        assertEquals("Deepak Patel", repository.customers.value[0].name)

        // Corrupted JSON test
        val corruptResult = repository.importDataFromJsonString("{ corrupted json data")
        assertFalse("Corrupted JSON restore must fail safely", corruptResult.success)
    }

    @Test
    fun testSafeStorageParsingDoesNotSwallowExceptions() {
        val prefs = context.getSharedPreferences("ghanshyam_billing_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_saved_data", "{ broken json without closing").commit()

        val freshRepo = AppRepository(context, asyncInit = false)
        assertNotNull("Corrupt data banner should be shown", freshRepo.corruptDataBanner.value)
        assertTrue("Corrupt backup key must exist in prefs", prefs.all.keys.any { it.startsWith("app_saved_data_corrupt_") })
    }

    @Test
    fun testOneTimeMigrationFromSharedPreferencesToRoom() {
        val prefs = context.getSharedPreferences("ghanshyam_billing_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repository.clearAllData()

        val legacyJson = """
            {
                "customers": [
                    {"id": "c-101", "name": "Bhavin Shah", "phone": "9898012345", "address": "Ring Road, Surat", "gstin": ""}
                ],
                "transactions": [
                    {"id": "t-201", "customerId": "c-101", "amount": 2500.0, "type": "GAVE", "note": "Festival Stock", "date": 1600000000000, "isCancelled": false}
                ],
                "inventory": [
                    {"id": "i-301", "name": "Silk Kurti Set", "itemCode": "SKS-01", "salePrice": 850.0, "purchasePrice": 500.0, "stock": 20, "category": "Girls Wear", "sizes": ["28", "30", "32"], "gstPercent": 5.0}
                ],
                "invoices": [],
                "cashbook": [
                    {"id": "cb-401", "type": "IN", "amount": 2500.0, "category": "Sales", "remark": "Advance", "date": 1600000000000, "paymentMode": "CASH", "linkedId": null, "source": "manual"}
                ]
            }
        """.trimIndent()

        prefs.edit().putString("app_saved_data", legacyJson).commit()

        val migratedRepo = AppRepository(context, asyncInit = false)

        // 1. Check old key was renamed to app_saved_data_migrated and app_saved_data was removed
        assertFalse("app_saved_data must no longer exist after migration", prefs.contains("app_saved_data"))
        assertTrue("app_saved_data_migrated must exist", prefs.contains("app_saved_data_migrated"))
        assertEquals(legacyJson, prefs.getString("app_saved_data_migrated", null))

        // 2. Check Room entities were populated in the transaction
        assertEquals(1, migratedRepo.customers.value.size)
        assertEquals("Bhavin Shah", migratedRepo.customers.value[0].name)
        assertEquals(1, migratedRepo.transactions.value.size)
        assertEquals(2500.0, migratedRepo.transactions.value[0].amount, 0.01)
        assertEquals(1, migratedRepo.inventory.value.size)
        assertEquals("Silk Kurti Set", migratedRepo.inventory.value[0].name)
        assertEquals(listOf("28", "30", "32"), migratedRepo.inventory.value[0].sizes)
        assertEquals(1, migratedRepo.cashbook.value.size)
        assertEquals(2500.0, migratedRepo.cashbook.value[0].amount, 0.01)
    }

    @Test
    fun testRoomPersistenceAcrossRepositoryInstances() {
        val cust = repository.addCustomer("Ketan Patel", "9825099999")
        val inv = repository.addInventoryItem(
            name = "Party Wear Blazer",
            itemCode = "PWB-99",
            salePrice = 1200.0,
            purchasePrice = 750.0,
            stock = 10,
            category = "Boys Formal",
            sizes = listOf("32", "34"),
            gstPercent = 12.0
        )

        // Instantiate a second repository pointing to the same Room database
        val repo2 = AppRepository(context, asyncInit = false)
        val loadedCust = repo2.customers.value.find { it.id == cust.id }
        assertNotNull("Customer must persist in Room across repository instances", loadedCust)
        assertEquals("Ketan Patel", loadedCust!!.name)

        val loadedInv = repo2.inventory.value.find { it.id == inv.id }
        assertNotNull("Inventory must persist in Room across repository instances", loadedInv)
        assertEquals("Party Wear Blazer", loadedInv!!.name)
        assertEquals(listOf("32", "34"), loadedInv.sizes)
    }

    @Test
    fun testCascadeCustomerDeletionWithRoom() {
        val cust = repository.addCustomer("Mahesh Joshi", "9900011223")
        val tx = repository.addTransaction(cust.id, 900.0, "GAVE", "Shorts")

        assertEquals(1, repository.customers.value.count { it.id == cust.id })
        assertEquals(1, repository.transactions.value.count { it.customerId == cust.id })

        repository.deleteCustomer(cust.id)

        assertEquals("Customer deleted", 0, repository.customers.value.count { it.id == cust.id })
        assertEquals("Cascade deleted customer transactions", 0, repository.transactions.value.count { it.customerId == cust.id })
    }

    @Test
    fun testTolerantMigrationAttachesOrphansToRecoveredEntries() {
        val prefs = context.getSharedPreferences("ghanshyam_billing_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repository.clearAllData()

        val legacyJsonWithOrphan = """
            {
                "customers": [
                    {"id": "c-1", "name": "Valid Customer", "phone": "9898011111", "address": "", "gstin": ""}
                ],
                "transactions": [
                    {"id": "tx-orphan-1", "customerId": "unknown-nonexistent-id", "amount": 1500.0, "type": "GAVE", "note": "Orphan Udhar", "date": 1600000000000, "isCancelled": false}
                ],
                "inventory": [],
                "invoices": [],
                "cashbook": []
            }
        """.trimIndent()

        prefs.edit().putString("app_saved_data", legacyJsonWithOrphan).commit()

        val repo = AppRepository(context, asyncInit = false)
        assertFalse("app_saved_data must no longer exist after migration", prefs.contains("app_saved_data"))
        assertTrue("app_saved_data_migrated must exist", prefs.contains("app_saved_data_migrated"))

        // Should have created "Recovered entries" customer
        val recoveredCust = repo.customers.value.find { it.name == "Recovered entries" }
        assertNotNull("Recovered entries customer should be auto-created for orphans", recoveredCust)

        // Transaction should be attached to this customer
        val orphanTx = repo.transactions.value.find { it.id == "tx-orphan-1" }
        assertNotNull("Orphan transaction must be saved", orphanTx)
        assertEquals(recoveredCust!!.id, orphanTx!!.customerId)

        // Migration notice must report recovered count
        assertNotNull(repo.migrationNotice.value)
        assertTrue(repo.migrationNotice.value!!.contains("1"))
    }

    @Test
    fun testTolerantBackupImportAttachesOrphansToRecoveredEntries() {
        val backupWithOrphan = """
            {
                "customers": [
                    {"id": "c-backup-1", "name": "Existing Party", "phone": "9999900000", "address": "", "gstin": ""}
                ],
                "transactions": [
                    {"id": "tx-orphan-2", "customerId": "missing-party-id", "amount": 750.0, "type": "GOT", "note": "Orphan Jama", "date": 1600000000000, "isCancelled": false}
                ],
                "inventory": [],
                "invoices": [],
                "cashbook": []
            }
        """.trimIndent()

        val result = repository.importDataFromJsonString(backupWithOrphan)
        assertTrue("Import must succeed tolerantly", result.success)
        assertEquals("Should report 1 repaired orphan transaction", 1, result.repairedCount)

        val recoveredCust = repository.customers.value.find { it.name == "Recovered entries" }
        assertNotNull("Recovered entries party must exist", recoveredCust)
        val orphanTx = repository.transactions.value.find { it.id == "tx-orphan-2" }
        assertNotNull("Orphan transaction must exist in repository", orphanTx)
        assertEquals(recoveredCust!!.id, orphanTx!!.customerId)
    }

    @Test
    fun testBlockedWritesWhenMigrationFailsAndRawDataPreserved() {
        val prefs = context.getSharedPreferences("ghanshyam_billing_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val damagedRawJson = """{ "customers": [ { incomplete json """
        prefs.edit()
            .putString("app_saved_data", damagedRawJson)
            .putString("legacy_migration_error", "Broken JSON syntax in legacy store")
            .commit()

        val brokenRepo = AppRepository(context, asyncInit = false)
        assertTrue("Writes must be blocked when migration error is active", brokenRepo.isMigrationBlocked.value)
        assertEquals(damagedRawJson, prefs.getString("app_saved_data", null)) // MUST BE UNTOUCHED

        // Verify write operation throws IllegalStateException
        try {
            brokenRepo.addCustomer("Should Fail", "9876543210")
            fail("addCustomer must throw IllegalStateException when migration is blocked")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("blocked"))
        }

        // Verify app_saved_data is still untouched
        assertEquals(damagedRawJson, prefs.getString("app_saved_data", null))
    }

    @Test
    fun testStartFreshArchivesDamagedJson() {
        val prefs = context.getSharedPreferences("ghanshyam_billing_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val damagedRawJson = """{ "damaged": true }"""
        prefs.edit()
            .putString("app_saved_data", damagedRawJson)
            .putString("legacy_migration_error", "Fatal corruption")
            .commit()

        val repo = AppRepository(context, asyncInit = false)
        assertTrue("Initially blocked", repo.isMigrationBlocked.value)

        repo.startFreshFromDamagedData()

        assertFalse("app_saved_data must be removed after starting fresh", prefs.contains("app_saved_data"))
        assertFalse("legacy_migration_error must be removed", prefs.contains("legacy_migration_error"))
        assertFalse("Writes should no longer be blocked", repo.isMigrationBlocked.value)

        val discardedKey = prefs.all.keys.find { it.startsWith("app_saved_data_discarded_") }
        assertNotNull("Archived discarded key must exist", discardedKey)
        assertEquals(damagedRawJson, prefs.getString(discardedKey, null))

        // Writes must now succeed without error
        val newCust = repo.addCustomer("Fresh Customer", "9800012345")
        assertNotNull(newCust)
        assertEquals(1, repo.customers.value.size)
    }

    @Test
    fun testInvoiceThemeLegacyKeyRead() {
        val prefs = context.getSharedPreferences("ghanshyam_billing_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repository.clearAllData()
        runBlocking {
            repository.database.appSettingsDao().delete()
        }

        // Test that "invoice_theme" pref key is read
        prefs.edit().putString("invoice_theme", "EMERALD_GREEN").commit()

        val repo = AppRepository(context, asyncInit = false)
        assertEquals("EMERALD_GREEN", repo.selectedInvoiceTheme.value)
    }
}

