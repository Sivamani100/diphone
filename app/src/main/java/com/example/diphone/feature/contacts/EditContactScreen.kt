package com.example.diphone.feature.contacts

import android.accounts.AccountManager
import android.telephony.SubscriptionManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.theme.AppColors
import kotlinx.coroutines.launch

data class AccountOption(
    val displayLabel: String,
    val accountType: String,
    val accountName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactScreen(
    contactId: Long?,
    onBack: () -> Unit,
    onSaveSuccess: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { com.example.diphone.DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    val phoneNumbers = remember { mutableStateListOf<String>() }
    val emails = remember { mutableStateListOf<String>() }

    // Discover real accounts dynamically
    val availableAccounts = remember {
        val accounts = mutableListOf<AccountOption>()
        
        // 1. Device (local)
        accounts.add(AccountOption(
            displayLabel = "This device",
            accountType = "Local",
            accountName = "Device",
            icon = Icons.Default.PhoneAndroid
        ))
        
        // 2. Google accounts from AccountManager
        try {
            val am = AccountManager.get(context)
            val googleAccounts = am.getAccountsByType("com.google")
            for (acct in googleAccounts) {
                accounts.add(AccountOption(
                    displayLabel = acct.name,
                    accountType = "com.google",
                    accountName = acct.name,
                    icon = Icons.Default.Email
                ))
            }
        } catch (_: Exception) {}
        
        // 3. SIM slots from SubscriptionManager
        try {
            val sm = context.getSystemService(SubscriptionManager::class.java)
            val subs = sm?.activeSubscriptionInfoList
            if (subs != null) {
                for (sub in subs) {
                    val label = sub.displayName?.toString() ?: "SIM ${sub.simSlotIndex + 1}"
                    accounts.add(AccountOption(
                        displayLabel = "$label (SIM ${sub.simSlotIndex + 1})",
                        accountType = "SIM",
                        accountName = "SIM${sub.simSlotIndex + 1}",
                        icon = Icons.Default.SimCard
                    ))
                }
            }
        } catch (_: Exception) {
            // Fallback: add generic SIM option
            accounts.add(AccountOption(
                displayLabel = "SIM 1",
                accountType = "SIM",
                accountName = "SIM1",
                icon = Icons.Default.SimCard
            ))
        }
        
        accounts
    }

    var selectedAccountIndex by remember { mutableIntStateOf(0) }
    var showAccountMenu by remember { mutableStateOf(false) }
    
    val selectedAccount = availableAccounts.getOrElse(selectedAccountIndex) { availableAccounts[0] }

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(contactId) {
        if (contactId != null) {
            val contact = container.contactsRepository.getContactById(contactId)
            if (contact != null) {
                name = contact.name
                company = contact.company
                jobTitle = contact.jobTitle
                notes = contact.notes
                phoneNumbers.addAll(contact.phoneNumbers.ifEmpty { listOf("") })
                emails.addAll(contact.emails.ifEmpty { listOf("") })
                // Try to match the loaded contact's account to our available list
                val matchIndex = availableAccounts.indexOfFirst { 
                    it.accountName == contact.accountName || it.accountType == contact.accountType 
                }
                if (matchIndex >= 0) selectedAccountIndex = matchIndex
            }
        } else {
            phoneNumbers.add("")
            emails.add("")
        }
        isLoading = false
    }


    // Constraint visual feedback
    val isSimSelected = selectedAccount.accountType == "SIM"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding()
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AppColors.Primary)
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                    Text(
                        text = if (contactId == null) "Create contact" else "Edit contact",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        scope.launch {
                            val savedId = container.contactsRepository.saveContact(
                                contactId = contactId,
                                name = name,
                                numbers = phoneNumbers.filter { it.isNotBlank() },
                                emails = emails.filter { it.isNotBlank() },
                                company = company,
                                jobTitle = jobTitle,
                                notes = notes,
                                accountType = selectedAccount.accountType,
                                accountName = selectedAccount.accountName
                            )
                            onSaveSuccess(savedId)
                        }
                    }) {
                        Text("Save", color = AppColors.Primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Save Location Header — dynamic accounts
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.Surface)
                                .clickable { showAccountMenu = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = selectedAccount.icon,
                                contentDescription = null,
                                tint = AppColors.Subtitle,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Saving to", color = AppColors.Subtitle, fontSize = 12.sp)
                                Text(selectedAccount.displayLabel, color = Color.White, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            
                            DropdownMenu(
                                expanded = showAccountMenu,
                                onDismissRequest = { showAccountMenu = false },
                                modifier = Modifier.background(AppColors.Surface)
                            ) {
                                availableAccounts.forEachIndexed { index, account ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = account.icon,
                                                    contentDescription = null,
                                                    tint = if (index == selectedAccountIndex) AppColors.Primary else AppColors.Subtitle,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    account.displayLabel,
                                                    color = if (index == selectedAccountIndex) AppColors.Primary else Color.White
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedAccountIndex = index
                                            showAccountMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        if (isSimSelected) {
                            Text(
                                "SIM cards only save name and 2 numbers",
                                color = AppColors.Warning,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Avatar Picker
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.Surface)
                                    .clickable { /* Photo Picker Action */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo", tint = AppColors.Subtitle, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // Name Field
                    item {
                        EditTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "First name",
                            icon = Icons.Default.Person
                        )
                    }

                    if (!isSimSelected) {
                        item {
                            EditTextField(
                                value = company,
                                onValueChange = { company = it },
                                label = "Company",
                                icon = Icons.Default.Business
                            )
                            EditTextField(
                                value = jobTitle,
                                onValueChange = { jobTitle = it },
                                label = "Job Title"
                            )
                        }
                    }

                    // Phone Numbers
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        phoneNumbers.forEachIndexed { index, number ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EditTextField(
                                    value = number,
                                    onValueChange = { phoneNumbers[index] = it },
                                    label = "Phone",
                                    icon = if (index == 0) Icons.Default.Call else null,
                                    keyboardType = KeyboardType.Phone,
                                    modifier = Modifier.weight(1f)
                                )
                                if (index > 0 || phoneNumbers.size > 1) {
                                    IconButton(onClick = { phoneNumbers.removeAt(index) }) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = AppColors.Decline)
                                    }
                                }
                            }
                        }
                        if (!isSimSelected || phoneNumbers.size < 2) {
                            TextButton(
                                onClick = { phoneNumbers.add("") },
                                modifier = Modifier.padding(start = 48.dp)
                            ) {
                                Text("Add phone", color = AppColors.Primary)
                            }
                        }
                    }

                    // Emails
                    if (!isSimSelected) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            emails.forEachIndexed { index, email ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    EditTextField(
                                        value = email,
                                        onValueChange = { emails[index] = it },
                                        label = "Email",
                                        icon = if (index == 0) Icons.Default.Email else null,
                                        keyboardType = KeyboardType.Email,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (index > 0 || emails.size > 1) {
                                        IconButton(onClick = { emails.removeAt(index) }) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = AppColors.Decline)
                                        }
                                    }
                                }
                            }
                            TextButton(
                                onClick = { emails.add("") },
                                modifier = Modifier.padding(start = 48.dp)
                            ) {
                                Text("Add email", color = AppColors.Primary)
                            }
                        }

                        // Notes
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            EditTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = "Notes",
                                icon = Icons.Default.Notes
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.Subtitle,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 16.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = AppColors.Subtitle) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.SurfaceVariant,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = AppColors.Primary
            ),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
    }
}
