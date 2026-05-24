package com.example.diphone

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object MainTabs : NavKey
@Serializable data object DefaultPhonePrompt : NavKey
@Serializable data object CallsTab : NavKey
@Serializable data object ContactsTab : NavKey
@Serializable data object BlockFilter : NavKey
@Serializable data object RecordingsList : NavKey
@Serializable data object ContactsSearch : NavKey
@Serializable data object CallsSearch : NavKey

@Serializable data class ContactDetail(val contactId: Long) : NavKey
@Serializable data class EditContact(val contactId: Long?) : NavKey // null = New Contact

// Settings
@Serializable data object SettingsMain : NavKey
@Serializable data object SettingsCallerId : NavKey
@Serializable data object SettingsIncomingReminders : NavKey
@Serializable data object SettingsSpeedDial : NavKey
@Serializable data object SettingsAnswerEnd : NavKey
@Serializable data object SettingsReplySms : NavKey
@Serializable data object SettingsCallRecording : NavKey
@Serializable data object SettingsManageContacts : NavKey
@Serializable data object SettingsContactsToDisplay : NavKey
@Serializable data object SettingsDefaultSaveLocation : NavKey
@Serializable data object SettingsSimContacts : NavKey
@Serializable data object SettingsImportExport : NavKey
@Serializable data object SettingsBlockFilter : NavKey
@Serializable data object SettingsBlockCalls : NavKey

