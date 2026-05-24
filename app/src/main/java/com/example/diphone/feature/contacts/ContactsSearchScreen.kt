package com.example.diphone.feature.contacts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.core.data.repository.Contact
import com.example.diphone.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsSearchScreen(
    onBack: () -> Unit,
    onContactClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { com.example.diphone.DiPhoneApp.getContainer(context) }

    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Load contacts stream with search filter
    val contacts by container.contactsRepository.getContactsStream(searchQuery, false).collectAsState(initial = emptyList())

    // Auto-focus the search bar when screen opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search contacts...", color = AppColors.Subtitle) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = AppColors.Primary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = AppColors.Subtitle)
                    }
                }
            }

            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp)

            if (searchQuery.isBlank()) {
                // Show hint
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = AppColors.SurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Type a name or number to search",
                            color = AppColors.Subtitle,
                            fontSize = 15.sp
                        )
                    }
                }
            } else if (contacts.isEmpty()) {
                // No results
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = AppColors.SurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No contacts found",
                            color = AppColors.Subtitle,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                // Results list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(contacts) { index, contact ->
                        SearchResultItem(
                            contact = contact,
                            searchQuery = searchQuery,
                            onClick = { onContactClick(contact.id) }
                        )
                        if (index < contacts.size - 1) {
                            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    contact: Contact,
    searchQuery: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (contact.isVip) AppColors.VipStar.copy(alpha = 0.15f)
                    else Color.White.copy(alpha = 0.05f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (contact.isVip) {
                Icon(Icons.Default.Star, contentDescription = null, tint = AppColors.VipStar, modifier = Modifier.size(20.dp))
            } else {
                Text(
                    text = contact.initial.toString(),
                    color = AppColors.Primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Highlighted name
            val annotatedName = buildAnnotatedString {
                val nameText = contact.name
                val queryLower = searchQuery.lowercase()
                val nameLower = nameText.lowercase()
                if (queryLower.isNotBlank() && nameLower.contains(queryLower)) {
                    val startIndex = nameLower.indexOf(queryLower)
                    val endIndex = startIndex + queryLower.length
                    append(nameText.substring(0, startIndex))
                    withStyle(style = SpanStyle(color = AppColors.Primary, fontWeight = FontWeight.Bold)) {
                        append(nameText.substring(startIndex, endIndex))
                    }
                    append(nameText.substring(endIndex))
                } else {
                    append(nameText)
                }
            }
            Text(
                text = annotatedName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Show phone number
            val phone = contact.phoneNumbers.firstOrNull() ?: ""
            if (phone.isNotBlank()) {
                Text(
                    text = phone,
                    color = AppColors.Subtitle,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }

            // Show company if available
            if (contact.company.isNotBlank()) {
                Text(
                    text = contact.company,
                    color = AppColors.SurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        // Call icon shortcut
        IconButton(onClick = {
            val number = contact.phoneNumbers.firstOrNull()
            if (number != null) {
                com.example.diphone.core.data.system.PhoneCallHelper.placeCall(context, number)
            }
        }) {
            Icon(
                Icons.Default.Call,
                contentDescription = "Call",
                tint = AppColors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
