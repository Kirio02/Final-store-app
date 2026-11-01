package com.example.ui.util

import com.example.model.AppLanguage

object LanguageStrings {

    fun get(key: String, language: AppLanguage = AppLanguage.HINGLISH): String {
        val entry = map[key] ?: return key
        return when (language) {
            AppLanguage.HINGLISH -> entry.hinglish
            AppLanguage.HINDI -> entry.hindi
            AppLanguage.GUJARATI -> entry.gujarati
            AppLanguage.ENGLISH -> entry.english
        }
    }

    private data class Trans(
        val hinglish: String,
        val hindi: String,
        val gujarati: String,
        val english: String
    )

    private val map = mapOf(
        "app_title" to Trans("Ghanshyam Billing", "घनश्याम बिलिंग", "ઘનશ્યામ બિલિંગ", "Ghanshyam Billing"),
        "tab_khata" to Trans("Khata", "खाता", "ખાતાવહી", "Khata"),
        "tab_billing" to Trans("Billing", "बिलिंग", "બિલિંગ", "Billing"),
        "tab_stock" to Trans("Stock", "स्टॉक", "સ્ટોક", "Stock"),
        "tab_cashbook" to Trans("Cashbook", "कैशबुक", "રોકડમેળ", "Cashbook"),
        "tab_profile" to Trans("Shop", "दुकान", "દુકાન", "Shop"),
        "you_gave" to Trans("Aapne Diye (Udhar)", "आपने दिए (उधार)", "તમે આપ્યા (ઉધાર)", "You Gave (Credit)"),
        "you_got" to Trans("Aapko Mile (Jama)", "आपको मिले (जमा)", "તમને મળ્યા (જમા)", "You Got (Debit)"),
        "total_receivable" to Trans("Total Lena Hai", "कुल लेना है", "કુલ લેવાના છે", "Total Receivable"),
        "total_payable" to Trans("Total Dena Hai", "कुल देना है", "કુલ આપવાના છે", "Total Payable"),
        "add_customer" to Trans("Customer Add Karo", "ग्राहक जोड़ें", "ગ્રાહક ઉમેરો", "Add Customer"),
        "search_hint" to Trans("Search customer ya phone...", "ग्राहक या फोन खोजें...", "ગ્રાહક કે ફોન શોધો...", "Search customer or phone..."),
        "filter_all" to Trans("Sabhi", "सभी", "બધા", "All"),
        "filter_receive" to Trans("Lena Hai", "लेना है", "લેવાના છે", "You'll Get"),
        "filter_give" to Trans("Dena Hai", "देना है", "આપવાના છે", "You'll Give"),
        "net_balance" to Trans("Net Balance", "शुद्ध शेष", "ચોખ્ખી બાકી", "Net Balance"),
        "cancelled_out" to Trans("Cancelled Out (Kharij)", "रद्द किया गया", "રદ કરેલ", "Cancelled Out"),
        "active_entry" to Trans("Valid Entry", "सक्रिय प्रविष्टि", "માન્ય એન્ટ્રી", "Active Entry"),
        "ai_assistant" to Trans("AI Assistant", "एआई सहायक", "AI સહાયક", "AI Assistant"),
        "listening" to Trans("Sun raha hoon...", "सुन रहा हूँ...", "સાંભળી રહ્યો છું...", "Listening..."),
        "tap_to_speak" to Trans("Bolne ke liye Mic dabayein", "बोलने के लिए माइक दबाएं", "બોલવા માટે માઇક દબાવો", "Tap mic to speak"),
        "generate_bill" to Trans("Bill Banayein", "बिल बनाएं", "બિલ બનાવો", "Generate Bill"),
        "import_khatabook" to Trans("Khatabook Se Import Karein", "खाताबुक से इम्पोर्ट करें", "ખાતાબુકમાંથી ઇમ્પોર્ટ કરો", "Import from Khatabook"),
        "store_address" to Trans("Store Ka Pata", "दुकान का पता", "દુકાનનું સરનામું", "Store Address"),
        "store_upi" to Trans("Store UPI ID", "दुकान की UPI आईडी", "દુકાન UPI ID", "Store UPI ID"),
        "store_phone" to Trans("Store Phone Number", "दुकान का फोन नंबर", "દુકાનનો ફોન નંબર", "Store Phone Number"),
        "import_csv_title" to Trans("CSV Se Customers Import Karein", "CSV से ग्राहक इम्पोर्ट करें", "CSV માંથી ગ્રાહક ઇમ્પોર્ટ કરો", "Import Customers from CSV"),
        "import_csv_desc" to Trans("Columns: name, phone, balance (positive = lena hai)", "कॉलम: name, phone, balance (धनात्मक = लेना है)", "કૉલમ: name, phone, balance (ધન = લેવાના છે)", "Columns: name, phone, balance (positive = owes shop)"),
        "btn_import_csv" to Trans("CSV File Se Import Karein", "CSV फाइल से इम्पोर्ट करें", "CSV ફાઇલમાંથી ઇમ્પોર્ટ કરો", "Import from CSV File"),
        "csv_preview_title" to Trans("CSV Data Preview", "CSV डेटा पूर्वावलोकन", "CSV ડેટા પૂર્વાવલોકન", "CSV Data Preview"),
        "btn_confirm_import" to Trans("Import Karein", "इम्पोर्ट करें", "ઇમ્પોર્ટ કરો", "Import"),
        "backup_file_title" to Trans("Data Backup & Restore", "डेटा बैकअप और रीस्टोर", "ડેટા બેકઅપ અને પુનઃસ્થાપિત", "Data Backup & Restore"),
        "backup_file_desc" to Trans("Plain JSON backup file save ya restore karein", "साधारण JSON बैकअप फाइल सेव या रीस्टोर करें", "સાદી JSON બેકઅપ ફાઇલ સેવ અથવા પુનઃસ્થાપિત કરો", "Save or restore plain JSON backup file"),
        "export_backup" to Trans("Backup File Save Karein", "बैकअप फाइल सेव करें", "બેકઅપ ફાઇલ સેવ કરો", "Save Backup File"),
        "share_backup" to Trans("Backup Share Karein", "बैकअप शेयर करें", "બેકઅપ શેર કરો", "Share Backup"),
        "restore_backup" to Trans("Backup File Se Restore", "बैकअप फाइल से रीस्टोर", "બેકઅપ ફાઇલમાંથી પુનઃસ્થાપિત કરો", "Restore from Backup File"),
        "restore_summary_title" to Trans("Restore Ki Pushti Karein", "रीस्टोर की पुष्टि करें", "પુનઃસ્થાપનાની પુષ્ટિ કરો", "Confirm Restore"),
        "restore_warning" to Trans("Purana data is backup se replace ho jayega. Safety ke liye current data backup me save kiya gaya hai.", "मौजूदा डेटा इस बैकअप से बदल जाएगा। सुरक्षा के लिए पिछला डेटा सुरक्षित कर लिया गया है।", "હાલનો ડેટા આ બેકઅપ દ્વારા બદલવામાં આવશે. સુરક્ષા માટે હાલનો ડેટા સુરક્ષિત કરવામાં આવ્યો છે.", "Current data will be replaced by this backup. A safety copy has been stored."),
        "btn_confirm_restore" to Trans("Restore Karein", "रीस्टोर करें", "પુનઃસ્થાપિત કરો", "Restore"),
        "clear_data_title" to Trans("Sabhi Data Clear Karein", "सभी डेटा साफ़ करें", "બધો ડેટા સાફ કરો", "Clear All Data"),
        "clear_data_desc" to Trans("Khata, Bill, Stock aur Cashbook hamesha ke liye delete ho jayega. Pehle ek automatic backup file save karni hogi.", "खाता, बिल, स्टॉक और कैशबुक हमेशा के लिए हट जाएंगे। पहले एक स्वचालित बैकअप फाइल सेव करनी होगी।", "ખાતાવહી, બિલ, સ્ટોક અને રોકડમેળ કાયમ માટે કાઢી નાખવામાં આવશે. પહેલા એક ઓટોમેટિક બેકઅપ ફાઇલ સેવ કરવી પડશે.", "All Khata, bills, stock, and cashbook will be wiped. An automatic backup file must be saved first."),
        "type_delete_hint" to Trans("Type karein: DELETE", "टाइप करें: DELETE", "ટાઇપ કરો: DELETE", "Type: DELETE"),
        "btn_backup_and_clear" to Trans("Backup Banayein & Data Clear Karein", "बैकअप बनाएं और डेटा हटाएं", "બેકઅપ બનાવો અને ડેટા સાફ કરો", "Create Backup & Clear Data"),
        "cash_in_hand" to Trans("Cash In Hand (Nagad)", "हाथ में नकद", "હાથ પર રોકડ", "Cash In Hand"),
        "digital_cash" to Trans("Digital (UPI / Bank)", "डिजिटल (UPI/बैंक)", "ડિજિટલ (UPI/બેંક)", "Digital (UPI / Bank)"),
        "net_total_cash" to Trans("Net Total Balance", "कुल शुद्ध बैलेंस", "કુલ ચોખ્ખી બાકી", "Net Total Balance"),
        "payment_mode_label" to Trans("Payment Mode", "भुगतान मोड", "ચુકવણી મોડ", "Payment Mode"),
        "also_add_cashbook" to Trans("Cashbook me bhi entry jodein", "कैशबुक में भी एंट्री जोड़ें", "રોકડમેળમાં પણ એન્ટ્રી ઉમેરો", "Also add to Cashbook"),
        "delete_bill_title" to Trans("Bill Delete Karein", "बिल हटाएं", "બિલ કાઢી નાખો", "Delete Invoice"),
        "delete_bill_desc" to Trans("Kya aap is bill ko delete karna chahte hain? Cashbook aur udhar entries bhi delete ho jayengi.", "क्या आप इस बिल को हटाना चाहते हैं? कैशबुक और उधार प्रविष्टियां भी हटा दी जाएंगी।", "શું તમે આ બિલ કાઢી નાખવા માંગો છો? રોકડમેળ અને ઉધાર એન્ટ્રીઓ પણ કાઢી નાખવામાં આવશે.", "Delete this invoice? Linked cashbook and credit entries will also be removed."),
        "onboarding_title" to Trans("Dukaan Ka Profile Setup Karein", "दुकान की प्रोफाइल सेट करें", "દુકાનની પ્રોફાઇલ સેટ કરો", "Set Up Shop Profile"),
        "store_name_label" to Trans("Shop Name *", "दुकान का नाम *", "દુકાનનું નામ *", "Shop Name *"),
        "owner_name_label" to Trans("Owner Name *", "मालिक का नाम *", "માલિકનું નામ *", "Owner Name *"),
        "state_label" to Trans("State *", "राज्य *", "રાજ્ય *", "State *"),
        "gstin_optional_label" to Trans("GSTIN (Optional)", "GSTIN (वैकल्पिक)", "GSTIN (વૈકલ્પિક)", "GSTIN (Optional)"),
        "upi_optional_label" to Trans("UPI ID for Payments (Optional)", "भुगतान के लिए UPI ID (वैकल्पिक)", "ચુકવણી માટે UPI ID (વૈકલ્પિક)", "UPI ID for Payments (Optional)"),
        "btn_save_launch" to Trans("Save & Launch Shop", "सेव करें और दुकान शुरू करें", "સેવ કરો અને દુકાન શરૂ કરો", "Save & Launch Shop"),
        "corrupt_data_banner" to Trans("Data load karne me error! Corrupt data safe rakha gaya hai.", "डेटा लोड करने में त्रुटि! सुरक्षित प्रतिलिपि सहेज ली गई है।", "ડેટા લોડ કરવામાં ભૂલ! સુરક્ષિત નકલ સાચવી રાખવામાં આવી છે.", "Error loading data! A safe backup copy was retained."),
        "btn_start_fresh" to Trans("Start Fresh", "नए सिरे से शुरू करें", "નવેસરથી શરૂ કરો", "Start Fresh"),
        "btn_cancel" to Trans("Cancel", "रद्द करें", "રદ કરો", "Cancel"),
        "btn_retry" to Trans("Retry", "पुनः प्रयास करें", "ફરી પ્રયાસ કરો", "Retry"),
        "btn_export_damaged" to Trans("Export Damaged Data", "क्षतिग्रस्त डेटा निर्यात करें", "ખરાબ થયેલ ડેટા નિકાસ કરો", "Export Damaged Data"),
        "btn_start_fresh_confirm" to Trans("Start Fresh (Archive Old Data)", "पुराना डेटा हटाएं और नया शुरू करें", "જૂનો ડેટા કાઢી નાખો અને નવું શરૂ કરો", "Start Fresh (Archive Old Data)"),
        "add_phone_number_hint" to Trans("Add a phone number", "फोन नंबर जोड़ें", "ફોન નંબર ઉમેરો", "Add a phone number"),
        "start_fresh_dialog_title" to Trans("Start Fresh Pushti Karein", "नए सिरे से शुरू करने की पुष्टि करें", "નવેસરથી શરૂ કરવાની પુષ્ટિ કરો", "Confirm Start Fresh"),
        "start_fresh_dialog_desc" to Trans("Purana data 'app_saved_data_discarded' me safe rakha jayega aur dobara load nahi hoga. Pushti ke liye DELETE type karein.", "पुराना डेटा बैकअप में सुरक्षित रहेगा और दोबारा लोड नहीं होगा। पुष्टि के लिए DELETE टाइप करें।", "જૂનો ડેટા સાચવવામાં આવશે અને ફરીથી લોડ થશે નહીં. પુષ્ટિ માટે DELETE ટાઇપ કરો.", "Old data will be archived safely and will not reload. Type DELETE to confirm."),
        "progress_loading" to Trans("Dukaan ka data load ho raha hai...", "दुकान का डेटा लोड हो रहा है...", "દુકાનનો ડેટા લોડ થઈ રહ્યો છે...", "Loading store data..."),
        "progress_exporting" to Trans("Export ho raha hai...", "निर्यात हो रहा है...", "નિકાસ થઈ રહી છે...", "Exporting..."),
        "progress_restoring" to Trans("Backup restore ho raha hai...", "बैकअप रीस्टोर हो रहा है...", "બેકઅપ પુનઃસ્થાપિત થઈ રહ્યું છે...", "Restoring backup..."),
        "progress_importing_csv" to Trans("CSV import ho rahi hai...", "CSV इम्पोर्ट हो रही है...", "CSV ઇમ્પોર્ટ થઈ રહી છે...", "Importing CSV..."),
        "progress_generating_pdf" to Trans("PDF Invoice taiyar ho raha hai...", "PDF बिल तैयार हो रहा है...", "PDF બિલ તૈયાર થઈ રહ્યું છે...", "Generating PDF invoice..."),
        "btn_dismiss" to Trans("Theek hai", "ठीक है", "ઠીક છે", "Dismiss")
    )
}
