package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppRepository
import com.example.ui.theme.*
import com.example.ui.util.LanguageStrings

private val INDIAN_STATES = listOf(
    "Gujarat",
    "Andhra Pradesh",
    "Arunachal Pradesh",
    "Assam",
    "Bihar",
    "Chhattisgarh",
    "Goa",
    "Haryana",
    "Himachal Pradesh",
    "Jharkhand",
    "Karnataka",
    "Kerala",
    "Madhya Pradesh",
    "Maharashtra",
    "Manipur",
    "Meghalaya",
    "Mizoram",
    "Nagaland",
    "Odisha",
    "Punjab",
    "Rajasthan",
    "Sikkim",
    "Tamil Nadu",
    "Telangana",
    "Tripura",
    "Uttar Pradesh",
    "Uttarakhand",
    "West Bengal",
    "Delhi",
    "Jammu and Kashmir",
    "Ladakh",
    "Chandigarh",
    "Puducherry"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    repository: AppRepository,
    isTutorialMode: Boolean = false,
    onFinish: () -> Unit
) {
    val currentLang by repository.language.collectAsState()
    val savedProfile by repository.storeProfile.collectAsState()

    var storeName by remember { mutableStateOf(savedProfile.storeName) }
    var ownerName by remember { mutableStateOf(savedProfile.ownerName) }
    var phone by remember { mutableStateOf(savedProfile.phone) }
    var address by remember { mutableStateOf(savedProfile.address) }
    var state by remember { mutableStateOf(savedProfile.state.ifBlank { "Gujarat" }) }
    var gstin by remember { mutableStateOf(savedProfile.gstin) }
    var upiId by remember { mutableStateOf(savedProfile.upiId) }

    var stateDropdownExpanded by remember { mutableStateOf(false) }
    var showValidationError by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = SoftBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LanguageStrings.get("onboarding_title", currentLang),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ChipActiveBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ghanshyam Kids Wear",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = LanguageStrings.get("onboarding_shop_subtitle", currentLang),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it; showValidationError = false },
                        label = { Text(LanguageStrings.get("store_name_label", currentLang)) },
                        placeholder = { Text("e.g. Ghanshyam Kids Wear") },
                        singleLine = true,
                        isError = showValidationError && storeName.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it; showValidationError = false },
                        label = { Text(LanguageStrings.get("owner_name_label", currentLang)) },
                        placeholder = { Text("e.g. Proprietor Name") },
                        singleLine = true,
                        isError = showValidationError && ownerName.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; showValidationError = false },
                        label = { Text(LanguageStrings.get("store_phone", currentLang)) },
                        placeholder = { Text("e.g. +91 98200 00000") },
                        singleLine = true,
                        isError = showValidationError && phone.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it; showValidationError = false },
                        label = { Text(LanguageStrings.get("store_address", currentLang)) },
                        placeholder = { Text("e.g. Ring Road Cloth Market, Surat") },
                        singleLine = true,
                        isError = showValidationError && address.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Indian State Selector
                    ExposedDropdownMenuBox(
                        expanded = stateDropdownExpanded,
                        onExpandedChange = { stateDropdownExpanded = !stateDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(LanguageStrings.get("state_label", currentLang)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = stateDropdownExpanded,
                            onDismissRequest = { stateDropdownExpanded = false }
                        ) {
                            INDIAN_STATES.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        state = item
                                        stateDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it },
                        label = { Text(LanguageStrings.get("gstin_optional_label", currentLang)) },
                        placeholder = { Text("24AAAAA0000A1Z5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text(LanguageStrings.get("upi_optional_label", currentLang)) },
                        placeholder = { Text("shop@upi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showValidationError) {
                        Text(
                            text = "Please fill in all required fields (Shop Name, Owner, Phone, Address)",
                            color = UdharRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val cleanStore = storeName.trim()
                            val cleanOwner = ownerName.trim()
                            val cleanPhone = phone.trim()
                            val cleanAddress = address.trim()

                            if (cleanStore.isBlank() || cleanOwner.isBlank() || cleanPhone.isBlank() || cleanAddress.isBlank()) {
                                showValidationError = true
                            } else {
                                repository.updateStoreProfile(
                                    storeName = cleanStore,
                                    address = cleanAddress,
                                    phone = cleanPhone,
                                    upiId = upiId.trim(),
                                    gstin = gstin.trim(),
                                    ownerName = cleanOwner,
                                    state = state
                                )
                                repository.completeOnboarding()
                                onFinish()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            LanguageStrings.get("btn_save_launch", currentLang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
