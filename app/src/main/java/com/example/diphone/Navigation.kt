package com.example.diphone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.diphone.feature.contacts.ContactDetailScreen
import com.example.diphone.feature.contacts.ContactsSearchScreen
import com.example.diphone.feature.contacts.EditContactScreen
import com.example.diphone.feature.calls.CallsSearchScreen
import com.example.diphone.feature.dialer.DefaultPhonePromptScreen
import com.example.diphone.feature.block.BlockFilterScreen
import com.example.diphone.feature.recordings.RecordingsScreen
import com.example.diphone.feature.settings.SettingsScreen
import com.example.diphone.ui.main.MainScreen
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.telecom.TelecomManager

@Composable
fun MainNavigation(showMissedCallsTab: Boolean = false) {
  // Start directly on MainTabs — show DefaultPhonePrompt as a dialog overlay instead
  val backStack = rememberNavBackStack(MainTabs)

  val context = LocalContext.current
  LaunchedEffect(Unit) {
      val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val roleManager = context.getSystemService(RoleManager::class.java)
          roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
      } else {
          val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
          telecomManager?.defaultDialerPackage == context.packageName
      }
      if (!isDefault) {
          backStack.add(DefaultPhonePrompt)
      }
  }

  NavDisplay(
    backStack = backStack,
    onBack = {
      // Only pop if there is more than 1 item — never pop to empty
      if (backStack.size > 1) backStack.removeLastOrNull()
    },
    entryProvider =
      entryProvider {
        entry<MainTabs> {
          MainScreen(
            showMissedCallsTab = showMissedCallsTab,
            onNavigateToSettings = { backStack.add(SettingsMain) },
            onNavigateToBlockFilter = { backStack.add(BlockFilter) },
            onNavigateToManageContacts = { backStack.add(SettingsManageContacts) },
            onNavigateToNewContact = { backStack.add(EditContact(null)) },
            onContactClick = { contactId -> backStack.add(ContactDetail(contactId)) },
            onUnknownClick = { _ -> /* Handle unknown number */ },
            onNavigateToRecordings = { backStack.add(RecordingsList) },
            onNavigateToContactsSearch = { backStack.add(ContactsSearch) },
            onNavigateToCallsSearch = { backStack.add(CallsSearch) }
          )
        }
        entry<DefaultPhonePrompt> {
          DefaultPhonePromptScreen(
            onDismiss = {
              if (backStack.size > 1) backStack.removeLastOrNull()
            },
            onRoleGranted = {
              if (backStack.size > 1) backStack.removeLastOrNull()
            }
          )
        }
        entry<ContactDetail> {
          ContactDetailScreen(
            contactId = it.contactId,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            onEdit = { contactId -> backStack.add(EditContact(contactId)) }
          )
        }
        entry<EditContact> {
          EditContactScreen(
            contactId = it.contactId,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            onSaveSuccess = { _ -> if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<RecordingsList> {
          RecordingsScreen(
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<ContactsSearch> {
          ContactsSearchScreen(
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            onContactClick = { contactId -> backStack.add(ContactDetail(contactId)) }
          )
        }
        entry<CallsSearch> {
          CallsSearchScreen(
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<SettingsMain> {
          SettingsScreen(
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            onNavigateToBlockFilter = { backStack.add(SettingsBlockFilter) },
            onNavigateToManageContacts = { backStack.add(SettingsManageContacts) },
            onNavigateToCallerId = { backStack.add(SettingsCallerId) },
            onNavigateToIncomingReminders = { backStack.add(SettingsIncomingReminders) },
            onNavigateToSpeedDial = { backStack.add(SettingsSpeedDial) },
            onNavigateToAnswerEnd = { backStack.add(SettingsAnswerEnd) },
            onNavigateToCallRecording = { backStack.add(SettingsCallRecording) }
          )
        }
        entry<SettingsManageContacts> {
          com.example.diphone.feature.settings.ManageContactsScreen(
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            onNavigateToContactsToDisplay = { backStack.add(SettingsContactsToDisplay) },
            onNavigateToDefaultSaveLocation = { backStack.add(SettingsDefaultSaveLocation) },
            onNavigateToImportExport = { backStack.add(SettingsImportExport) },
            onNavigateToSimContacts = { backStack.add(SettingsSimContacts) }
          )
        }
        entry<BlockFilter> {
          BlockFilterScreen(
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<SettingsCallerId> {
          com.example.diphone.feature.settings.CallerIdScreen(
            onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<SettingsIncomingReminders> {
          com.example.diphone.feature.settings.IncomingCallRemindersScreen(
            onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<SettingsSpeedDial> {
          com.example.diphone.feature.settings.SpeedDialScreen(
            onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<SettingsAnswerEnd> {
          com.example.diphone.feature.settings.AnswerEndCallsScreen(
            onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            onNavigateToReplySms = { backStack.add(SettingsReplySms) }
          )
        }
        entry<SettingsReplySms> {
          com.example.diphone.feature.settings.ReplySmsScreen(
            onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<SettingsCallRecording> {
          com.example.diphone.feature.settings.CallRecordingScreen(
            onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<SettingsContactsToDisplay> {
          com.example.diphone.feature.settings.ContactsToDisplayScreen(
            onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<SettingsDefaultSaveLocation> {
          com.example.diphone.feature.settings.DefaultSaveLocationScreen(
            onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<SettingsImportExport> {
          com.example.diphone.feature.settings.ImportExportScreen(
            onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<SettingsSimContacts> {
          com.example.diphone.feature.settings.SimContactsScreen(
            onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<SettingsBlockFilter> {
          com.example.diphone.feature.settings.BlockFilterSettingsScreen(
            onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            onNavigateToBlockCalls = { backStack.add(SettingsBlockCalls) }
          )
        }
        entry<SettingsBlockCalls> {
          com.example.diphone.feature.settings.BlockCallsSettingsScreen(
            onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
      },
  )
}
