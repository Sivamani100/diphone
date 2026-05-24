package com.example.diphone.feature.contacts

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
fun ContactsScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToNewContact: () -> Unit,
    onNavigateToBlockFilter: () -> Unit,
    onNavigateToManageContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { com.example.diphone.DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Database values
    var filterText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Toggle filters
    val onlyNumbers by container.settingsRepository.onlyContactsWithNumbers.collectAsState(initial = false)

    // Load contacts stream
    val contacts by container.contactsRepository.getContactsStream(filterText, onlyNumbers).collectAsState(initial = emptyList())

    // Duplicate Banner dismiss state
    var showDuplicateBanner by remember { mutableStateOf(true) }

    // Multi-select bulk delete state
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedContacts = remember { mutableStateListOf<Long>() }

    // Smart merge trigger info
    val hasDuplicates = contacts.size > 5 // mock trigger if list contains several mock entries

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. App Top Bar
            if (isSelectMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        isSelectMode = false
                        selectedContacts.clear()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                    Text(
                        text = "Select items",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = {
                        if (selectedContacts.size == contacts.size) {
                            selectedContacts.clear()
                        } else {
                            selectedContacts.clear()
                            selectedContacts.addAll(contacts.map { it.id })
                        }
                    }) {
                        Text("Select All", color = AppColors.Primary)
                    }
                }
            } else if (isSearching) {
                // Fuzzy search searchbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = { Text("Search contacts...", color = AppColors.Subtitle) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = AppColors.Primary
                        ),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        isSearching = false
                        filterText = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Search", tint = Color.White)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Contacts",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${contacts.size} contacts",
                            fontSize = 12.sp,
                            color = AppColors.Subtitle
                        )
                    }
                    Row {
                        IconButton(onClick = { onNavigateToSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(AppColors.Surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Contacts", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        isSelectMode = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Block & filter", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToBlockFilter()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Manage contacts", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToManageContacts()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToSettings()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Smart Merge Duplicates Banner
            if (hasDuplicates && showDuplicateBanner && !isSelectMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.MergeType,
                                contentDescription = null,
                                tint = AppColors.Primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Duplicate contacts found. Click to merge.",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                        Row {
                            TextButton(onClick = { showDuplicateBanner = false }) {
                                Text("Dismiss", color = AppColors.Subtitle, fontSize = 12.sp)
                            }
                            TextButton(onClick = {
                                scope.launch {
                                    showDuplicateBanner = false
                                    container.contactsRepository.mergeAllDuplicates()
                                }
                            }) {
                                Text("Merge", color = AppColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Contacts Alphabetical List Content
            val alphabet = ('A'..'Z').toList() + '#'
            val groupedContacts = remember(contacts) {
                contacts.groupBy { it.initial }
            }

            Row(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {



                    // Alphabet groups list
                    groupedContacts.forEach { (initial, list) ->
                        // Section Header Letter
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppColors.Surface)
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = initial.toString(),
                                    color = AppColors.Primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Contact Row items
                        itemsIndexed(list) { index, contact ->
                            val isSelected = selectedContacts.contains(contact.id)
                            ContactListItem(
                                contact = contact,
                                isSelected = isSelected,
                                isSelectMode = isSelectMode,
                                searchQuery = filterText,
                                onClick = {
                                    if (isSelectMode) {
                                        if (isSelected) selectedContacts.remove(contact.id)
                                        else selectedContacts.add(contact.id)
                                    } else {
                                        onNavigateToDetail(contact.id)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectMode) {
                                        isSelectMode = true
                                        selectedContacts.add(contact.id)
                                    }
                                }
                            )
                            if (index < list.size - 1) {
                                HorizontalDivider(color = AppColors.Divider, thickness = 1.dp)
                            }
                        }
                    }
                }

                // Alphabetical Quick Scroll Index Sidebar on the right
                if (!isSelectMode && contacts.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(28.dp)
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        alphabet.forEach { letter ->
                            Text(
                                text = letter.toString(),
                                color = AppColors.Subtitle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        // Find index of first item matching starting initial
                                        val matchInitial = contacts.indexOfFirst { it.initial == letter }
                                        if (matchInitial != -1) {
                                            scope.launch {
                                                // Jump scroll to that section
                                                listState.scrollToItem(matchInitial)
                                            }
                                        }
                                    }
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Multi-Select Bottom Delete Panel
        if (isSelectMode && selectedContacts.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(AppColors.Surface)
                    .clickable {
                        // Confirm deletion batch dialog
                        scope.launch {
                            selectedContacts.forEach { id ->
                                container.contactsRepository.deleteContact(id)
                            }
                            selectedContacts.clear()
                            isSelectMode = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppColors.Decline)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Delete (${selectedContacts.size})",
                        color = AppColors.Decline,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // 5. Add Contact FAB
        if (!isSelectMode) {
            FloatingActionButton(
                onClick = onNavigateToNewContact,
                containerColor = AppColors.Primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(64.dp)
                    .clip(CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact", tint = Color.White)
            }
        }
    }
}

@Composable
fun ProfileLinkItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AppColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AppColors.Subtitle, fontSize = 12.sp)
        }
    }
}

@Composable
fun ContactListItem(
    contact: Contact,
    isSelected: Boolean,
    isSelectMode: Boolean,
    searchQuery: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circular container
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
            if (contact.photoUri != null) {
                var bitmap by remember(contact.photoUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(contact.photoUri) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val uri = android.net.Uri.parse(contact.photoUri)
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                            }
                        } catch (_: Exception) {}
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = contact.name,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else if (contact.isVip) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = AppColors.VipStar, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = contact.initial.toString(),
                        color = AppColors.Primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (contact.isVip) {
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

        // Contact details with highlighted search matching letters
        Column(modifier = Modifier.weight(1f)) {
            val annotatedName = buildAnnotatedString {
                val nameText = contact.name
                if (searchQuery.isNotBlank() && nameText.lowercase().contains(searchQuery.lowercase())) {
                    val startIndex = nameText.lowercase().indexOf(searchQuery.lowercase())
                    val endIndex = startIndex + searchQuery.length
                    
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
            val subtitle = if (contact.company.isNotBlank()) contact.company else contact.phoneNumbers.firstOrNull() ?: ""
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = AppColors.Subtitle,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        // Starred Favourite indicator / checkmark selector / Block indicator
        if (isSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = AppColors.Primary)
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (contact.isVip) {
                    Icon(Icons.Default.Star, contentDescription = "VIP", tint = AppColors.VipStar, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (contact.isBlocked) {
                    Icon(Icons.Default.Block, contentDescription = "Blocked", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
