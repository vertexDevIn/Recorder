package `in`.vertexdev.mobile.call_rec.repo

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import `in`.vertexdev.mobile.call_rec.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

@ActivityRetainedScoped
private val Context.dataStore by preferencesDataStore(Constants.MAIN_DATA_STORE_REPOSITORIES)

class DataStoreRepo @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferenceKeys {
        val testFolderSelected = booleanPreferencesKey("testFolderSelected")
        val isLoggedIn = booleanPreferencesKey("isLoggedIn")
        val folderUri = stringPreferencesKey("folderUri")
        val token = stringPreferencesKey("KenTo")
        val authenticated = booleanPreferencesKey("authenticated")
        val apiKey = stringPreferencesKey("yek")
        val allPermissionsGranted = booleanPreferencesKey("allPermGranted")
        val firstName = stringPreferencesKey("firstName")
        val userId = stringPreferencesKey("userId")
        val lastName = stringPreferencesKey("lastName")
        val mobile = stringPreferencesKey("mobile")
        val userType = stringPreferencesKey("type")
        val email = stringPreferencesKey("email")
        val profilePictureUri = stringPreferencesKey("profilePictureUri") // New Key
        val notificationEnabled = booleanPreferencesKey("notificationEnabled") // New Key
        val pageCount = stringPreferencesKey("pageCount")
        val deleteRecordings = booleanPreferencesKey("deleteRecordings")
    }

    private val dataStore = context.dataStore


    suspend fun saveDeleteRecording(value: Boolean): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.deleteRecordings] = value
        }
        return true
    }

    suspend fun saveAllPermGrantedSelected(value: Boolean): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.allPermissionsGranted] = value
        }
        return true
    }

    suspend fun saveFolderSelected(value: Boolean): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.testFolderSelected] = value
        }
        return true
    }

    suspend fun saveIsLoggedIn(value: Boolean): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.isLoggedIn] = value
        }
        return true
    }

    suspend fun saveAuthenticated(value: Boolean): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.authenticated] = value
        }
        return true
    }

    suspend fun saveFolderUri(value: String): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.folderUri] = value
        }
        return true
    }
    suspend fun saveUserId(value: String): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.userId] = value
        }
        return true
    }
    suspend fun savePageCount(value: String): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.pageCount] = value
        }
        return true
    }

    suspend fun saveApiKey(value: String): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.apiKey] = value
        }
        return true
    }

    suspend fun saveToken(value: String): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.token] = value
        }
        return true
    }

    suspend fun saveFirstName(value: String): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.firstName] = value
        }
        return true
    }

    suspend fun saveLastName(value: String): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.lastName] = value
        }
        return true
    }

    suspend fun saveMobile(value: String): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.mobile] = value
        }
        return true
    }

    suspend fun saveUserType(value: String): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.userType] = value
        }
        return true
    }

    suspend fun saveEmail(value: String): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.email] = value
        }
        return true
    }

    suspend fun saveProfilePictureUri(value: String): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.profilePictureUri] = value
        }
        return true
    }

    suspend fun saveNotificationEnabled(value: Boolean): Boolean {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.notificationEnabled] = value
        }
        return true
    }

    val readDataStoreValue: Flow<MainPref> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val valueOfFolderSelected = preferences[PreferenceKeys.testFolderSelected] ?: false
            val valueOfIsLoggedIn = preferences[PreferenceKeys.isLoggedIn] ?: false
            val authenticated = preferences[PreferenceKeys.authenticated] ?: false
            val valueOfFolderUri = preferences[PreferenceKeys.folderUri] ?: ""
            val valueOfToken = preferences[PreferenceKeys.token] ?: ""
            val valueOfApiKey = preferences[PreferenceKeys.apiKey] ?: ""
            val allPermGranted = preferences[PreferenceKeys.allPermissionsGranted] ?: false
            val firstName = preferences[PreferenceKeys.firstName] ?: ""
            val lastName = preferences[PreferenceKeys.lastName] ?: ""
            val mobile = preferences[PreferenceKeys.mobile] ?: ""
            val userType = preferences[PreferenceKeys.userType] ?: ""
            val email = preferences[PreferenceKeys.email] ?: ""
            val profilePictureUri = preferences[PreferenceKeys.profilePictureUri] ?: ""
            val notificationEnabled = preferences[PreferenceKeys.notificationEnabled] ?: false
            val userId = preferences[PreferenceKeys.userId] ?: ""
            val pageCount = preferences[PreferenceKeys.pageCount] ?: "0"
            val deleteRecordings = preferences[PreferenceKeys.deleteRecordings] ?: true

            MainPref(
                valueOfFolderSelected,
                valueOfIsLoggedIn,
                valueOfFolderUri,
                valueOfToken,
                authenticated,
                valueOfApiKey,
                allPermGranted,
                firstName,
                lastName,
                mobile,
                userType,
                email,
                profilePictureUri,
                notificationEnabled,
                userId,
                pageCount,
                deleteRecordings
            )
        }

    data class MainPref(
        val folderSelected: Boolean,
        val isLoggedIn: Boolean,
        val folderPath: String,
        val token: String,
        val authenticated: Boolean,
        val apiKey: String,
        val allPermGranted: Boolean,
        val firstName: String,
        val lastName: String,
        val mobile: String,
        val userType: String,
        val email: String,
        val profilePictureUri: String,
        val notificationEnabled: Boolean,
        val userId:String,
        val pageCount:String,
        val deleteRecordings:Boolean
    )
}
