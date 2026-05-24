package com.example.diphone.core.data.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.example.diphone.core.data.local.AppDao
import com.example.diphone.core.data.local.ContactAugment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Contact(
    val id: Long,
    val name: String,
    val phoneNumbers: List<String>,
    val emails: List<String> = emptyList(),
    val company: String = "",
    val jobTitle: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    val accountType: String = "This device",
    val accountName: String = "Local Device",
    val isVip: Boolean = false,
    val customRingtone: String? = null,
    val isBlocked: Boolean = false,
    val routeToVoicemail: Boolean = false,
    val groups: List<String> = emptyList()
) {
    val initial: Char
        get() = if (name.isNotBlank()) name.first().uppercaseChar() else '#'
}

class ContactsRepository(
    private val context: Context,
    private val appDao: AppDao
) {
    private val _mockContacts = MutableStateFlow<List<Contact>>(emptyList())
    // Cache for system contacts fetched on IO thread
    private val _systemContacts = MutableStateFlow<List<Contact>>(emptyList())

    private val contentObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            refreshSystemContacts()
        }
    }

    init {
        // Initialize mock contacts immediately
        _mockContacts.value = createMockContacts()
        // Load system contacts on IO thread in background
        CoroutineScope(Dispatchers.IO + Job()).launch {
            val sys = tryFetchSystemContacts()
            _systemContacts.value = sys
        }
        try {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                contentObserver
            )
        } catch (_: Exception) {}
    }

    fun getContactsStream(filterText: String = "", onlyWithNumbers: Boolean = false): Flow<List<Contact>> {
        val augmentsFlow = appDao.getAllContactAugments()

        return combine(augmentsFlow, _systemContacts, _mockContacts) { augments, systemContacts, mocks ->
            val augmentMap = augments.associateBy { it.contactId }

            // 1. Select source (System first, fallback to mock if empty/no permission)
            val baseList = if (systemContacts.isNotEmpty()) systemContacts else mocks

            // 2. Merge with Room DB augmentation profiles (VIP star, custom ringtone, blocklist, voicemail routing)
            var merged = baseList.map { contact ->
                val augment = augmentMap[contact.id]
                contact.copy(
                    isVip = augment?.isVip ?: contact.isVip,
                    customRingtone = augment?.customRingtone ?: contact.customRingtone,
                    isBlocked = augment?.isBlocked ?: contact.isBlocked,
                    routeToVoicemail = augment?.routeToVoicemail ?: contact.routeToVoicemail
                )
            }

            // 3. Perform "smart merge" duplicates algorithm
            merged = performSmartMerge(merged)

            // 4. Apply filters
            if (onlyWithNumbers) {
                merged = merged.filter { it.phoneNumbers.isNotEmpty() }
            }

            if (filterText.isNotBlank()) {
                val query = filterText.lowercase().trim()
                merged = merged.filter {
                    it.name.lowercase().contains(query) ||
                    it.phoneNumbers.any { num -> num.replace(" ", "").contains(query) }
                }
            }

            // 5. Sort: Pure A-to-Z alphabetical sorting
            merged.sortedBy { it.name.lowercase() }
        }
    }

    fun getContactByIdStream(id: Long): Flow<Contact?> {
        return getContactsStream().map { list -> list.find { it.id == id } }
    }

    // Refresh system contacts (call after permissions are granted)
    fun refreshSystemContacts() {
        CoroutineScope(Dispatchers.IO + Job()).launch {
            _systemContacts.value = tryFetchSystemContacts()
        }
    }

    suspend fun getContactById(id: Long): Contact? = withContext(Dispatchers.IO) {
        val augment = appDao.getContactAugment(id)
        val systemContact = tryFetchSystemContactById(id)
        val baseContact = systemContact ?: _mockContacts.value.find { it.id == id }
        
        return@withContext baseContact?.copy(
            isVip = augment?.isVip ?: baseContact.isVip,
            customRingtone = augment?.customRingtone ?: baseContact.customRingtone,
            isBlocked = augment?.isBlocked ?: baseContact.isBlocked,
            routeToVoicemail = augment?.routeToVoicemail ?: baseContact.routeToVoicemail
        )
    }

    suspend fun toggleVipStatus(contactId: Long, isVip: Boolean) = withContext(Dispatchers.IO) {
        val current = appDao.getContactAugment(contactId)
        appDao.insertContactAugment(
            ContactAugment(
                contactId = contactId,
                isVip = isVip,
                customRingtone = current?.customRingtone,
                isBlocked = current?.isBlocked ?: false,
                routeToVoicemail = current?.routeToVoicemail ?: false
            )
        )
        // Also update local mock list if it belongs there
        _mockContacts.update { list ->
            list.map { if (it.id == contactId) it.copy(isVip = isVip) else it }
        }
    }

    suspend fun updateCustomRingtone(contactId: Long, ringtoneUri: String?) = withContext(Dispatchers.IO) {
        val current = appDao.getContactAugment(contactId)
        appDao.insertContactAugment(
            ContactAugment(
                contactId = contactId,
                isVip = current?.isVip ?: false,
                customRingtone = ringtoneUri,
                isBlocked = current?.isBlocked ?: false,
                routeToVoicemail = current?.routeToVoicemail ?: false
            )
        )
    }

    suspend fun toggleBlockedStatus(contactId: Long, isBlocked: Boolean) = withContext(Dispatchers.IO) {
        val current = appDao.getContactAugment(contactId)
        appDao.insertContactAugment(
            ContactAugment(
                contactId = contactId,
                isVip = current?.isVip ?: false,
                customRingtone = current?.customRingtone,
                isBlocked = isBlocked,
                routeToVoicemail = current?.routeToVoicemail ?: false
            )
        )
        
        // Block/unblock numbers associated with this contact
        val contact = getContactById(contactId)
        contact?.phoneNumbers?.forEach { num ->
            val cleanNum = num.replace(" ", "").replace("-", "")
            if (isBlocked) {
                appDao.insertBlockedNumber(
                    com.example.diphone.core.data.local.BlockedNumber(
                        number = cleanNum,
                        contactId = contactId,
                        contactName = contact.name,
                        reason = "MANUAL"
                    )
                )
            } else {
                appDao.deleteBlockedNumberByNumber(cleanNum)
            }
        }
    }

    suspend fun toggleVoicemailRouting(contactId: Long, route: Boolean) = withContext(Dispatchers.IO) {
        val current = appDao.getContactAugment(contactId)
        appDao.insertContactAugment(
            ContactAugment(
                contactId = contactId,
                isVip = current?.isVip ?: false,
                customRingtone = current?.customRingtone,
                isBlocked = current?.isBlocked ?: false,
                routeToVoicemail = route
            )
        )
    }

    suspend fun mergeAllDuplicates() = withContext(Dispatchers.IO) {
        val baseList = if (_systemContacts.value.isNotEmpty()) _systemContacts.value else _mockContacts.value
        if (baseList.size <= 1) return@withContext
        
        val mergedGroups = mutableListOf<MutableList<Contact>>()
        
        for (contact in baseList) {
            var addedToGroup = false
            for (group in mergedGroups) {
                val match = group.any { member ->
                    member.name.trim().equals(contact.name.trim(), ignoreCase = true) ||
                    member.phoneNumbers.any { n1 ->
                        contact.phoneNumbers.any { n2 ->
                            val clean1 = n1.replace(" ", "").replace("-", "").takeLast(10)
                            val clean2 = n2.replace(" ", "").replace("-", "").takeLast(10)
                            clean1.isNotEmpty() && clean1 == clean2
                        }
                    }
                }
                if (match) {
                    group.add(contact)
                    addedToGroup = true
                    break
                }
            }
            if (!addedToGroup) {
                mergedGroups.add(mutableListOf(contact))
            }
        }
        
        val ops = ArrayList<ContentProviderOperation>()
        val mockMergedList = ArrayList<Contact>()
        val processedIds = mutableSetOf<Long>()
        
        for (group in mergedGroups) {
            if (group.size <= 1) {
                mockMergedList.addAll(group)
                continue
            }
            
            val survivor = group[0]
            val duplicates = group.subList(1, group.size)
            
            val allNames = group.map { it.name }.distinct()
            val mergedName = allNames.maxByOrNull { it.length } ?: survivor.name
            val allNumbers = group.flatMap { it.phoneNumbers }.distinct()
            val allEmails = group.flatMap { it.emails }.distinct()
            val allCompanies = group.map { it.company }.firstOrNull { it.isNotBlank() } ?: survivor.company
            val allJobTitles = group.map { it.jobTitle }.firstOrNull { it.isNotBlank() } ?: survivor.jobTitle
            val allNotes = group.map { it.notes }.firstOrNull { it.isNotBlank() } ?: survivor.notes
            
            val survivorRawId = getRawContactId(survivor.id)
            if (survivorRawId != null) {
                for (dup in duplicates) {
                    val dupRawId = getRawContactId(dup.id)
                    if (dupRawId != null) {
                        ops.add(
                            ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                                .withSelection("${ContactsContract.RawContacts._ID} = ?", arrayOf(dupRawId.toString()))
                                .build()
                        )
                    }
                }
                
                val existingNumbers = survivor.phoneNumbers
                for (num in allNumbers) {
                    if (!existingNumbers.contains(num) && num.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, survivorRawId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, num)
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                                .build()
                        )
                    }
                }
                
                val existingEmails = survivor.emails
                for (email in allEmails) {
                    if (!existingEmails.contains(email) && email.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, survivorRawId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                                .build()
                        )
                    }
                }
            }
            
            val mergedContact = survivor.copy(
                name = mergedName,
                phoneNumbers = allNumbers,
                emails = allEmails,
                company = allCompanies,
                jobTitle = allJobTitles,
                notes = allNotes
            )
            mockMergedList.add(mergedContact)
            processedIds.addAll(group.map { it.id })
        }
        
        if (ops.isNotEmpty()) {
            try {
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        _mockContacts.update { mockMergedList }
        refreshSystemContacts()
    }
    
    private fun getRawContactId(contactId: Long): Long? {
        var rawContactId: Long? = null
        var cursor: android.database.Cursor? = null
        try {
            cursor = context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                rawContactId = cursor.getLong(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return rawContactId
    }


    suspend fun saveContact(
        contactId: Long?,
        name: String,
        numbers: List<String>,
        emails: List<String>,
        company: String = "",
        jobTitle: String = "",
        notes: String = "",
        accountType: String = "This device",
        accountName: String = "Local Device",
        groups: List<String> = emptyList()
    ): Long = withContext(Dispatchers.IO) {
        // Attempt system ContactsContract save with real account info
        val systemId = trySaveToSystemContacts(contactId, name, numbers, emails, company, jobTitle, notes, accountType, accountName)
        
        val targetId = systemId ?: contactId ?: (System.currentTimeMillis() and Long.MAX_VALUE)
        
        val newContact = Contact(
            id = targetId,
            name = name,
            phoneNumbers = numbers.filter { it.isNotBlank() },
            emails = emails.filter { it.isNotBlank() },
            company = company,
            jobTitle = jobTitle,
            notes = notes,
            accountType = accountType,
            accountName = accountName,
            groups = groups
        )
        
        _mockContacts.update { list ->
            val existing = list.find { it.id == targetId }
            if (existing != null) {
                list.map { if (it.id == targetId) newContact else it }
            } else {
                list + newContact
            }
        }

        // Refresh system contacts so the newly saved contact appears in the list
        refreshSystemContacts()
        
        return@withContext targetId
    }

    suspend fun deleteContact(contactId: Long) = withContext(Dispatchers.IO) {
        tryDeleteFromSystemContacts(contactId)
        
        _mockContacts.update { list ->
            list.filterNot { it.id == contactId }
        }
    }

    // Merge logic: finds duplicate contacts and unifies them in display
    private fun performSmartMerge(contacts: List<Contact>): List<Contact> {
        if (contacts.size <= 1) return contacts
        
        val mergedList = ArrayList<Contact>()
        // Map of normalized name to list of contacts with that name
        val nameGroups = contacts.groupBy { it.name.trim().lowercase() }
        
        for ((_, group) in nameGroups) {
            if (group.size == 1) {
                mergedList.add(group[0])
                continue
            }
            
            // For multiple contacts with the same name, merge those that have overlapping phone numbers
            val subgroupList = ArrayList<Contact>()
            for (c in group) {
                var merged = false
                // Check if c overlaps with any contact already in subgroupList
                for (k in subgroupList.indices) {
                    val existing = subgroupList[k]
                    val overlap = c.phoneNumbers.any { n1 -> 
                        existing.phoneNumbers.any { n2 -> 
                            val clean1 = n1.replace(" ", "").takeLast(10)
                            val clean2 = n2.replace(" ", "").takeLast(10)
                            clean1 == clean2
                        }
                    }
                    if (overlap || c.phoneNumbers.isEmpty() || existing.phoneNumbers.isEmpty()) {
                        subgroupList[k] = existing.copy(
                            phoneNumbers = (existing.phoneNumbers + c.phoneNumbers).distinct(),
                            emails = (existing.emails + c.emails).distinct(),
                            company = if (existing.company.isBlank()) c.company else existing.company,
                            jobTitle = if (existing.jobTitle.isBlank()) c.jobTitle else existing.jobTitle,
                            notes = if (existing.notes.isBlank()) c.notes else existing.notes,
                            isVip = existing.isVip || c.isVip,
                            groups = (existing.groups + c.groups).distinct()
                        )
                        merged = true
                        break
                    }
                }
                if (!merged) {
                    subgroupList.add(c)
                }
            }
            mergedList.addAll(subgroupList)
        }
        return mergedList
    }

    // --- System Content Resolver Queries ---
    private fun tryFetchSystemContacts(): List<Contact> {
        val list = mutableListOf<Contact>()
        val resolver = context.contentResolver
        
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )
        
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null, null,
                null
            )
            
            if (cursor != null && cursor.moveToFirst()) {
                val idCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val starredCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)
                
                val contactMap = mutableMapOf<Long, Contact>()
                do {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Unknown"
                    val number = cursor.getString(numCol) ?: ""
                    val photo = cursor.getString(photoCol)
                    val starred = cursor.getInt(starredCol) > 0
                    
                    val existing = contactMap[id]
                    if (existing != null) {
                        if (number.isNotBlank() && !existing.phoneNumbers.contains(number)) {
                            contactMap[id] = existing.copy(phoneNumbers = existing.phoneNumbers + number)
                        }
                    } else {
                        contactMap[id] = Contact(
                            id = id,
                            name = name,
                            phoneNumbers = if (number.isNotBlank()) listOf(number) else emptyList(),
                            photoUri = photo,
                            isVip = starred
                        )
                    }
                } while (cursor.moveToNext())
                list.addAll(contactMap.values)
            }
        } catch (e: Exception) {
            // Permission denied or provider unavailable
        } finally {
            cursor?.close()
        }
        return list
    }

    private fun tryFetchSystemContactById(id: Long): Contact? {
        val resolver = context.contentResolver
        var cursor: android.database.Cursor? = null
        try {
            var name = "Unknown"
            var photo: String? = null
            var starred = false
            var accountType = "This device"
            var accountName = "Local Device"
            
            val rawProj = arrayOf(
                ContactsContract.RawContacts.ACCOUNT_TYPE,
                ContactsContract.RawContacts.ACCOUNT_NAME
            )
            val rawCursor = resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                rawProj,
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(id.toString()),
                null
            )
            rawCursor?.use { c ->
                if (c.moveToFirst()) {
                    accountType = c.getString(0) ?: "This device"
                    accountName = c.getString(1) ?: "Local Device"
                }
            }
            
            val contactCursor = resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts.STARRED
                ),
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(id.toString()),
                null
            )
            contactCursor?.use { c ->
                if (c.moveToFirst()) {
                    name = c.getString(0) ?: "Unknown"
                    photo = c.getString(1)
                    starred = c.getInt(2) > 0
                }
            }
            
            val numbers = mutableListOf<String>()
            val emails = mutableListOf<String>()
            var company = ""
            var jobTitle = ""
            var notes = ""
            
            val dataProj = arrayOf(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3
            )
            cursor = resolver.query(
                ContactsContract.Data.CONTENT_URI,
                dataProj,
                "${ContactsContract.Data.CONTACT_ID} = ?",
                arrayOf(id.toString()),
                null
            )
            
            if (cursor != null && cursor.moveToFirst()) {
                val mimeCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
                val data1Col = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
                val data3Col = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3)
                
                do {
                    val mime = cursor.getString(mimeCol)
                    val data1 = cursor.getString(data1Col)
                    
                    when (mime) {
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                            if (!data1.isNullOrBlank() && !numbers.contains(data1)) {
                                numbers.add(data1)
                            }
                        }
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                            if (!data1.isNullOrBlank() && !emails.contains(data1)) {
                                emails.add(data1)
                            }
                        }
                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                            company = data1 ?: ""
                            jobTitle = cursor.getString(data3Col) ?: ""
                        }
                        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                            notes = data1 ?: ""
                        }
                    }
                } while (cursor.moveToNext())
            }
            
            return Contact(
                id = id,
                name = name,
                phoneNumbers = numbers,
                emails = emails,
                company = company,
                jobTitle = jobTitle,
                notes = notes,
                photoUri = photo,
                accountType = accountType,
                accountName = accountName,
                isVip = starred
            )
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun trySaveToSystemContacts(
        contactId: Long?,
        name: String,
        numbers: List<String>,
        emails: List<String>,
        company: String,
        jobTitle: String,
        notes: String,
        accountType: String = "Local",
        accountName: String = "Device"
    ): Long? {
        val ops = ArrayList<ContentProviderOperation>()
        
        try {
            if (accountType == "SIM") {
                try {
                    val values = android.content.ContentValues()
                    values.put("tag", name)
                    values.put("number", numbers.firstOrNull() ?: "")
                    context.contentResolver.insert(Uri.parse("content://icc/adn"), values)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (contactId == null) {
                val rawContactInsertIndex = ops.size
                
                val realAccountType: String? = when (accountType) {
                    "com.google" -> "com.google"
                    "SIM" -> null
                    else -> null
                }
                val realAccountName: String? = when (accountType) {
                    "com.google" -> accountName
                    else -> null
                }

                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, realAccountType)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, realAccountName)
                        .build()
                )
                
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                        .build()
                )
                
                for (number in numbers) {
                    if (number.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                                .build()
                        )
                    }
                }
                
                for (email in emails) {
                    if (email.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                                .build()
                        )
                    }
                }
                
                if (company.isNotBlank() || jobTitle.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                            .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, jobTitle)
                            .build()
                    )
                }
                
                if (notes.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                            .build()
                    )
                }
                
                val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                if (results.isNotEmpty() && results[0].uri != null) {
                    val rawContactId = results[0].uri!!.lastPathSegment?.toLongOrNull()
                    if (rawContactId != null) {
                        var actualContactId: Long? = null
                        var lookupCursor: Cursor? = null
                        try {
                            lookupCursor = context.contentResolver.query(
                                ContactsContract.RawContacts.CONTENT_URI,
                                arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                                "${ContactsContract.RawContacts._ID} = ?",
                                arrayOf(rawContactId.toString()),
                                null
                            )
                            if (lookupCursor != null && lookupCursor.moveToFirst()) {
                                actualContactId = lookupCursor.getLong(0)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            lookupCursor?.close()
                        }
                        return actualContactId ?: rawContactId
                    }
                }
            } else {
                val rawContactId = getRawContactId(contactId) ?: return null
                
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} IN (?, ?, ?, ?, ?)",
                            arrayOf(
                                rawContactId.toString(),
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
                            )
                        )
                        .build()
                )
                
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                        .build()
                )
                
                for (number in numbers) {
                    if (number.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                                .build()
                        )
                    }
                }
                
                for (email in emails) {
                    if (email.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                                .build()
                        )
                    }
                }
                
                if (company.isNotBlank() || jobTitle.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                            .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, jobTitle)
                            .build()
                    )
                }
                
                if (notes.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                            .build()
                    )
                }
                
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                return contactId
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun lookupContactName(number: String): String? {
        val resolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        
        // Fallback to mock contacts
        return lookupMockContactName(number)
    }

    fun lookupMockContactName(number: String): String? {
        val cleanIncoming = number.filter { it.isDigit() }
        if (cleanIncoming.isEmpty()) return null
        return _mockContacts.value.find { contact ->
            contact.phoneNumbers.any { num ->
                val cleanNum = num.filter { it.isDigit() }
                if (cleanNum.length >= 10 && cleanIncoming.length >= 10) {
                    cleanNum.takeLast(10) == cleanIncoming.takeLast(10)
                } else {
                    cleanNum == cleanIncoming
                }
            }
        }?.name
    }

    fun findContactIdByNumber(number: String): Long? {
        val resolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                if (idIndex != -1) {
                    return cursor.getLong(idIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        
        // Fallback to mock contacts
        val cleanIncoming = number.filter { it.isDigit() }
        if (cleanIncoming.isNotEmpty()) {
            return _mockContacts.value.find { contact ->
                contact.phoneNumbers.any { num ->
                    val cleanNum = num.filter { it.isDigit() }
                    if (cleanNum.length >= 10 && cleanIncoming.length >= 10) {
                        cleanNum.takeLast(10) == cleanIncoming.takeLast(10)
                    } else {
                        cleanNum == cleanIncoming
                    }
                }
            }?.id
        }
        return null
    }

    private fun tryDeleteFromSystemContacts(contactId: Long) {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString())
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            // Ignore
        }
    }

    // --- Rich Mock Contacts Generator for Million-Dollar Aesthetics ---
    private fun createMockContacts(): List<Contact> {
        return listOf(
            Contact(
                id = 101,
                name = "Aparna",
                phoneNumbers = listOf("+91 72070 31145"),
                emails = listOf("aparna@gmail.com"),
                company = "DiPhone Team",
                jobTitle = "Lead Designer",
                notes = "Always calls regarding UI styling refinements",
                accountType = "Google",
                accountName = "mallipurapuravi@gmail.com",
                isVip = true
            ),
            Contact(
                id = 102,
                name = "Mallipuram Siva",
                phoneNumbers = listOf("+91 98851 07897", "+91 63038 57495"),
                emails = listOf("siva@gmail.com"),
                company = "Antigravity Corp",
                jobTitle = "Lead Architect",
                notes = "Co-coder on this Android application",
                accountType = "Google",
                accountName = "mallipuramsiva123@gmail.com",
                isVip = true
            ),
            Contact(
                id = 103,
                name = "Cybercrime Helpline",
                phoneNumbers = listOf("1930"),
                company = "Govt. of India",
                accountType = "SIM1",
                accountName = "SIM1 Contacts"
            ),
            Contact(
                id = 104,
                name = "Best Offer",
                phoneNumbers = listOf("121"),
                accountType = "SIM1",
                accountName = "SIM1 Contacts"
            ),
            Contact(
                id = 105,
                name = "Airtel Store",
                phoneNumbers = listOf("121"),
                accountType = "SIM1",
                accountName = "SIM1 Contacts"
            ),
            Contact(
                id = 106,
                name = "AL Hellotunes",
                phoneNumbers = listOf("543211"),
                accountType = "SIM1",
                accountName = "SIM1 Contacts"
            ),
            Contact(
                id = 107,
                name = "Advanced Talktime",
                phoneNumbers = listOf("*141#"),
                accountType = "SIM1",
                accountName = "SIM1 Contacts"
            ),
            Contact(
                id = 108,
                name = "Priya Sharma",
                phoneNumbers = listOf("+91 98765 43210"),
                emails = listOf("priya@sharma.in"),
                company = "Indie Tech",
                jobTitle = "Product Lead",
                accountType = "Google",
                accountName = "designclub49@gmail.com",
                groups = listOf("Friends")
            ),
            Contact(
                id = 109,
                name = "Rahul Verma",
                phoneNumbers = listOf("+91 87654 32109"),
                company = "Global Logic",
                jobTitle = "Developer",
                accountType = "This device",
                accountName = "Local Device",
                groups = listOf("Family")
            ),
            Contact(
                id = 110,
                name = "Sneha Reddy",
                phoneNumbers = listOf("+91 65432 10987"),
                emails = listOf("sneha.reddy@outlook.com"),
                company = "Reddy & Co",
                accountType = "This device",
                accountName = "Local Device",
                groups = listOf("Friends")
            ),
            Contact(
                id = 111,
                name = "Amit Patel",
                phoneNumbers = listOf("+91 76543 21098"),
                company = "Symphony Inc",
                jobTitle = "Sales Manager",
                accountType = "Google",
                accountName = "mallipuramsiva123@gmail.com",
                groups = listOf("Co-workers")
            )
        )
    }
}
