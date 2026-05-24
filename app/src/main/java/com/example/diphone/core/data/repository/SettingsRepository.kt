package com.example.diphone.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "diphone_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val SHOW_PROFILE_PIC = booleanPreferencesKey("show_profile_pic")
        val ONLY_CONTACTS_WITH_NUMBERS = booleanPreferencesKey("only_contacts_with_numbers")
        val DEFAULT_SAVE_LOCATION = stringPreferencesKey("default_save_location")
        val CONTACTS_TO_DISPLAY = stringPreferencesKey("contacts_to_display")
        val SORT_BY = stringPreferencesKey("sort_by") // "First name" or "Last name"
        val NAME_ORDER = stringPreferencesKey("name_order") // "First name, last name" or "Last name, first name"
        
        val CALLER_ID_ENABLED = booleanPreferencesKey("caller_id_enabled")
        val QUICK_ACTIONS_ENABLED = booleanPreferencesKey("quick_actions_enabled")
        val UPDATE_MOBILE_NETWORK = booleanPreferencesKey("update_mobile_network")
        val IDENTIFICATION_PROGRAMME = booleanPreferencesKey("identification_programme")
        
        val INCOMING_CALL_STYLE = stringPreferencesKey("incoming_call_style") // "Banner" or "Full screen"
        val FLASH_ON_CALL = booleanPreferencesKey("flash_on_call")
        val ASCENDING_RINGTONE = booleanPreferencesKey("ascending_ringtone")
        
        val VIBRATE_ON_ANSWER = booleanPreferencesKey("vibrate_on_answer")
        val VIBRATE_ON_END = booleanPreferencesKey("vibrate_on_end")
        val AUTO_ANSWER_ENABLED = booleanPreferencesKey("auto_answer_enabled")
        val AUTO_ANSWER_DELAY = intPreferencesKey("auto_answer_delay") // seconds
        val POWER_BUTTON_ENDS_CALL = booleanPreferencesKey("power_button_ends_call")
        val DEFAULT_TO_SPEAKER = booleanPreferencesKey("default_to_speaker")
        
        val REPLY_SMS_ENABLED = booleanPreferencesKey("reply_sms_enabled")
        val REPLY_TEMPLATE_1 = stringPreferencesKey("reply_template_1")
        val REPLY_TEMPLATE_2 = stringPreferencesKey("reply_template_2")
        val REPLY_TEMPLATE_3 = stringPreferencesKey("reply_template_3")
        val REPLY_TEMPLATE_4 = stringPreferencesKey("reply_template_4")
        
        val RECORD_ALL_CALLS = booleanPreferencesKey("record_all_calls")
        val MAX_RECORDINGS = stringPreferencesKey("max_recordings") // "10", "50", "100", "500", "No limit"
    }

    val showProfilePic: Flow<Boolean> = context.dataStore.data.map { it[SHOW_PROFILE_PIC] ?: true }
    val onlyContactsWithNumbers: Flow<Boolean> = context.dataStore.data.map { it[ONLY_CONTACTS_WITH_NUMBERS] ?: false }
    val defaultSaveLocation: Flow<String> = context.dataStore.data.map { it[DEFAULT_SAVE_LOCATION] ?: "This device" }
    val contactsToDisplay: Flow<String> = context.dataStore.data.map { it[CONTACTS_TO_DISPLAY] ?: "All contacts" }
    val sortBy: Flow<String> = context.dataStore.data.map { it[SORT_BY] ?: "First name" }
    val nameOrder: Flow<String> = context.dataStore.data.map { it[NAME_ORDER] ?: "First name, last name" }

    val callerIdEnabled: Flow<Boolean> = context.dataStore.data.map { it[CALLER_ID_ENABLED] ?: true }
    val quickActionsEnabled: Flow<Boolean> = context.dataStore.data.map { it[QUICK_ACTIONS_ENABLED] ?: true }
    val updateMobileNetwork: Flow<Boolean> = context.dataStore.data.map { it[UPDATE_MOBILE_NETWORK] ?: true }
    val identificationProgramme: Flow<Boolean> = context.dataStore.data.map { it[IDENTIFICATION_PROGRAMME] ?: true }

    val incomingCallStyle: Flow<String> = context.dataStore.data.map { it[INCOMING_CALL_STYLE] ?: "Banner" }
    val flashOnCall: Flow<Boolean> = context.dataStore.data.map { it[FLASH_ON_CALL] ?: false }
    val ascendingRingtone: Flow<Boolean> = context.dataStore.data.map { it[ASCENDING_RINGTONE] ?: true }

    val vibrateOnAnswer: Flow<Boolean> = context.dataStore.data.map { it[VIBRATE_ON_ANSWER] ?: true }
    val vibrateOnEnd: Flow<Boolean> = context.dataStore.data.map { it[VIBRATE_ON_END] ?: true }
    val autoAnswerEnabled: Flow<Boolean> = context.dataStore.data.map { it[AUTO_ANSWER_ENABLED] ?: false }
    val autoAnswerDelay: Flow<Int> = context.dataStore.data.map { it[AUTO_ANSWER_DELAY] ?: 5 }
    val powerButtonEndsCall: Flow<Boolean> = context.dataStore.data.map { it[POWER_BUTTON_ENDS_CALL] ?: true }
    val defaultToSpeaker: Flow<Boolean> = context.dataStore.data.map { it[DEFAULT_TO_SPEAKER] ?: false }

    val replySmsEnabled: Flow<Boolean> = context.dataStore.data.map { it[REPLY_SMS_ENABLED] ?: true }
    val replyTemplate1: Flow<String> = context.dataStore.data.map { it[REPLY_TEMPLATE_1] ?: "I'll call you back later." }
    val replyTemplate2: Flow<String> = context.dataStore.data.map { it[REPLY_TEMPLATE_2] ?: "Can't talk now. What's up?" }
    val replyTemplate3: Flow<String> = context.dataStore.data.map { it[REPLY_TEMPLATE_3] ?: "Can't talk now. Call me back later." }
    val replyTemplate4: Flow<String> = context.dataStore.data.map { it[REPLY_TEMPLATE_4] ?: "I'll be there soon." }

    val recordAllCalls: Flow<Boolean> = context.dataStore.data.map { it[RECORD_ALL_CALLS] ?: false }
    val maxRecordings: Flow<String> = context.dataStore.data.map { it[MAX_RECORDINGS] ?: "No limit" }

    suspend fun updateShowProfilePic(value: Boolean) = setPreference(SHOW_PROFILE_PIC, value)
    suspend fun updateOnlyContactsWithNumbers(value: Boolean) = setPreference(ONLY_CONTACTS_WITH_NUMBERS, value)
    suspend fun updateDefaultSaveLocation(value: String) = setPreference(DEFAULT_SAVE_LOCATION, value)
    suspend fun updateContactsToDisplay(value: String) = setPreference(CONTACTS_TO_DISPLAY, value)
    suspend fun updateSortBy(value: String) = setPreference(SORT_BY, value)
    suspend fun updateNameOrder(value: String) = setPreference(NAME_ORDER, value)

    suspend fun updateCallerIdEnabled(value: Boolean) = setPreference(CALLER_ID_ENABLED, value)
    suspend fun updateQuickActionsEnabled(value: Boolean) = setPreference(QUICK_ACTIONS_ENABLED, value)
    suspend fun updateUpdateMobileNetwork(value: Boolean) = setPreference(UPDATE_MOBILE_NETWORK, value)
    suspend fun updateIdentificationProgramme(value: Boolean) = setPreference(IDENTIFICATION_PROGRAMME, value)

    suspend fun updateIncomingCallStyle(value: String) = setPreference(INCOMING_CALL_STYLE, value)
    suspend fun updateFlashOnCall(value: Boolean) = setPreference(FLASH_ON_CALL, value)
    suspend fun updateAscendingRingtone(value: Boolean) = setPreference(ASCENDING_RINGTONE, value)

    suspend fun updateVibrateOnAnswer(value: Boolean) = setPreference(VIBRATE_ON_ANSWER, value)
    suspend fun updateVibrateOnEnd(value: Boolean) = setPreference(VIBRATE_ON_END, value)
    suspend fun updateAutoAnswerEnabled(value: Boolean) = setPreference(AUTO_ANSWER_ENABLED, value)
    suspend fun updateAutoAnswerDelay(value: Int) = setPreference(AUTO_ANSWER_DELAY, value)
    suspend fun updatePowerButtonEndsCall(value: Boolean) = setPreference(POWER_BUTTON_ENDS_CALL, value)
    suspend fun updateDefaultToSpeaker(value: Boolean) = setPreference(DEFAULT_TO_SPEAKER, value)

    suspend fun updateReplySmsEnabled(value: Boolean) = setPreference(REPLY_SMS_ENABLED, value)
    suspend fun updateReplyTemplate1(value: String) = setPreference(REPLY_TEMPLATE_1, value)
    suspend fun updateReplyTemplate2(value: String) = setPreference(REPLY_TEMPLATE_2, value)
    suspend fun updateReplyTemplate3(value: String) = setPreference(REPLY_TEMPLATE_3, value)
    suspend fun updateReplyTemplate4(value: String) = setPreference(REPLY_TEMPLATE_4, value)
    
    suspend fun updateRecordAllCalls(value: Boolean) = setPreference(RECORD_ALL_CALLS, value)
    suspend fun updateMaxRecordings(value: String) = setPreference(MAX_RECORDINGS, value)

    suspend fun resetReplies() {
        context.dataStore.edit { prefs ->
            prefs.remove(REPLY_TEMPLATE_1)
            prefs.remove(REPLY_TEMPLATE_2)
            prefs.remove(REPLY_TEMPLATE_3)
            prefs.remove(REPLY_TEMPLATE_4)
        }
    }

    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }
}
