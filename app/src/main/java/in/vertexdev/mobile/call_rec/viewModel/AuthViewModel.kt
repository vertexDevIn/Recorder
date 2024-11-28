package `in`.vertexdev.mobile.call_rec.viewModel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.vertexdev.mobile.call_rec.models.Folder
import `in`.vertexdev.mobile.call_rec.models.Type
import `in`.vertexdev.mobile.call_rec.models.UserResponse
import `in`.vertexdev.mobile.call_rec.models.VerifyOtpResponse
import `in`.vertexdev.mobile.call_rec.repo.AuthRepository
import `in`.vertexdev.mobile.call_rec.repo.DataStoreRepo
import `in`.vertexdev.mobile.call_rec.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// AuthViewModel.kt
@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val repository: AuthRepository,
    private val dataStore: DataStoreRepo,
) : AndroidViewModel(application) {

    val datastore = dataStore.readDataStoreValue




    fun login(apiKey: String) = flow<State<Boolean>> {
        emit(State.loading())
        val response = repository.login(apiKey)
        if (response.isSuccessful) {
            val userResponse = response.body()
            if (userResponse != null) {
                Log.d("TAG", "login:$userResponse ")

                val isAuthenticated = userResponse[0].keyAuthenticated
                val token = userResponse[0].oneTimeToken
                if (token != null) {
                    dataStore.saveToken(token)
                }
                dataStore.saveApiKey(apiKey)
                dataStore.saveFirstName(userResponse[0].fname)
                dataStore.saveLastName(userResponse[0].lname)
                dataStore.saveEmail(userResponse[0].email)
                dataStore.saveUserType(userResponse[0].type)
                dataStore.saveUserId(userResponse[0].id)

                when (isAuthenticated) {
                    0 -> {
                        if (token != null) {
                            dataStore.saveAuthenticated(false)
                            emit(State.Success(true))
                        } else {
                            emit(State.failed("One time token not found"))
                        }
                    }

                    1 -> {
                        dataStore.saveIsLoggedIn(true)
                        dataStore.saveAuthenticated(true)
                        emit(State.success(false))
                    }

                    else -> {
                        emit(State.failed("Something went wrong"))
                    }
                }


            } else {
                Log.d("TAG", "login:userResponse:null ")
                emit(State.Failed("Something Went Wrong, Please try Again"))

            }

        } else {
            emit(State.Failed("Invalid Key"))
        }
    }.flowOn(Dispatchers.IO)

    fun saveFolderPath(folderUri:String) = flow<State<Boolean>> {
        Log.d("TAG", "saveFolderPath:$folderUri ")
        emit(State.loading())
        dataStore.saveFolderUri(folderUri)
        dataStore.saveFolderSelected(true)
        emit(State.Success(true))


    }.flowOn(Dispatchers.IO)

    fun verifyOtp(otp: String) = flow<State<Boolean>> {
        emit(State.loading())
        val apiKey = datastore.first().apiKey
        Log.d("TAG", "verifyOtp: apiKey::$apiKey")
        val token = datastore.first().token

        val response =
            repository.checkAuth(
                apiKey = apiKey,
                oneTimeToken = token,
                otp = otp
            )

        if (response.isSuccessful) {
            Log.d("TAG", "verifyOtp:Successfully ")
            val dataAll = response.body()
            val data = dataAll?.get(0)
            if (data != null) {

                if (data.app_key != null
                    && data.public_api_key != null
                    && data.status != null
                ) {
                    if (data.status == 0) {
                        dataStore.saveIsLoggedIn(true)
                        dataStore.saveAuthenticated(false)
                        emit(State.Success(true))
                    } else {
                        emit((State.failed("Unknown Error")))
                    }

                } else {
                    emit((State.failed("Unknown Error")))
                }

            } else {
                emit(State.Failed("Unable to parse data"))
            }

        } else {
            Log.d("TAG", "verifyOtp:Failed ")

            emit(State.Failed("Incorrect Otp"))
        }

    }.flowOn(Dispatchers.IO)

    fun resendOtp() = flow<State<Boolean>> {
        emit(State.loading())
        val apiKey = datastore.first().apiKey
        Log.d("TAG", "resendOtp: apiKey::$apiKey")
        val response = repository.resendOtp(apiKey)
        if (response.isSuccessful) {
            Log.d("TAG", "resendOtp:Successfully::${response.body()} ")
            val userResponse = response.body()

            if (userResponse != null) {
                val token = userResponse[0].oneTimeToken
                if (token != null) {
                    dataStore.saveToken(token)
                }
                emit(State.Success(true))
            } else {
                emit(State.Failed("Response null"))
            }


        } else {
            Log.d("TAG", "resendOtp:Failed ")
            emit(State.Failed(response.message()))
        }


    }

    fun saveAllPermissionGranted(value: Boolean) = viewModelScope.launch {
        dataStore.saveAllPermGrantedSelected(value)
    }

    fun logout() {
        //Todo:://
    }

    fun getUser() = flow<State<UserResponse>> {
        val apiKey = datastore.first().apiKey
        val response = repository.getUser(apiKey)
        if (response.isSuccessful) {
            emit(State.success(response.body()!![0]))
        } else {
            emit(State.failed(response.message()))
        }

    }.flowOn(Dispatchers.IO)

    fun saveDeleteRecordings(checked: Boolean) = viewModelScope.launch {
        dataStore.saveDeleteRecording(checked)
        if (checked) {
            delete24HoursBeforeRecordings()
        }


    }


    private fun delete24HoursBeforeRecordings() = viewModelScope.launch(Dispatchers.IO) {
        val deleteActive = datastore.first().deleteRecordings
        Log.d("DeleteRecordings", "Starting deletion of files older than 24 hours")
        if (deleteActive) {
            try {

                val selectedFolderUri = datastore.first().folderPath
                Log.d("DeleteRecordings", "Selected folder URI: $selectedFolderUri")

                val folderUri = Uri.parse(selectedFolderUri)

                // Grant permission to access the folder
                val contentResolver = getApplication<Application>().contentResolver
                val takeFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(folderUri, takeFlags)
                Log.d("DeleteRecordings", "Permission granted for folder access")

                // Build URI to access folder contents
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    folderUri,
                    DocumentsContract.getTreeDocumentId(folderUri)
                )
                Log.d("DeleteRecordings", "Child URI: $childrenUri")

                // Projection for the query
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    OpenableColumns.DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_FLAGS // Check for deletable files
                )

                // Prepare time window: 24 hours ago in milliseconds
                val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                Log.d("DeleteRecordings", "Time threshold (24 hours ago): $twentyFourHoursAgo")

                // Query folder contents (without filtering by lastModified)
                val cursor = contentResolver.query(
                    childrenUri,
                    projection,
                    null, // No filtering here
                    null,
                    null
                )

                cursor?.use { cur ->
                    val idColumn =
                        cur.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameColumn = cur.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    val lastModifiedColumn =
                        cur.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    val flagsColumn =
                        cur.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS)

                    var fileCount = 0
                    var nonDeletableCount = 0
                    while (cur.moveToNext()) {
                        val documentId = cur.getString(idColumn)
                        val fileName = cur.getString(nameColumn)
                        val lastModified = cur.getLong(lastModifiedColumn)
                        val flags = cur.getInt(flagsColumn)

                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(
                            folderUri,
                            documentId
                        )

                        // Log the file details
                        Log.d("FileToDelete", "File: $fileName, Last Modified: $lastModified")

                        // Manually check if the file is older than 24 hours
                        if (lastModified < twentyFourHoursAgo) {
                            // Check if the file supports deletion
                            if (flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0) {
                                try {
                                    // Use DocumentsContract.deleteDocument() to delete the file
                                    DocumentsContract.deleteDocument(contentResolver, fileUri)
                                    Log.d("FileDeleted", "Successfully deleted: $fileName")
                                    fileCount++
                                } catch (e: Exception) {
                                    Log.e("FileDeleteError", "Error deleting file: $fileName", e)
                                }
                            } else {
                                Log.d(
                                    "FileNotDeletable",
                                    "File $fileName does not support deletion."
                                )
                                nonDeletableCount++
                            }
                        } else {
                            Log.d("FileNotOldEnough", "File $fileName is not older than 24 hours.")
                        }
                    }
                    Log.d("DeleteRecordings", "Total files deleted: $fileCount")
                    Log.d(
                        "DeleteRecordings",
                        "Total files that could not be deleted: $nonDeletableCount"
                    )
                } ?: Log.d("DeleteRecordings", "No files found")
            } catch (e: Exception) {
                Log.e("DeleteRecordings", "Error during deletion", e)
            }
            Log.d("DeleteRecordings", "Deletion process completed")
        }

    }

    private val _currentFolderPath = MutableStateFlow<String>(Environment.getExternalStorageDirectory().path)
    val currentFolderPath: StateFlow<String> get() = _currentFolderPath

    fun updateCurrentFolderPath(path: String) {
        _currentFolderPath.value = path

    }

    fun getFolders(): List<Folder> {
        val path = _currentFolderPath.value ?: Environment.getExternalStorageDirectory().path
        return if (path == Environment.getExternalStorageDirectory().path) {
            getAllFoldersFromRoot()
        } else {
            getFoldersFromPath(path)
        }
    }

    fun goBackHandled(): Boolean {
        _currentFolderPath.value.let { path ->
            val rootPath = Environment.getExternalStorageDirectory().path
            // Check if the current path is already the root
            return if (path == rootPath) {
                false // Already at root, no further navigation possible
            } else {
                val parent = File(path).parentFile?.path
                if (parent != null) {
                    _currentFolderPath.value = parent
                    true
                } else {
                    false
                }
            }
        }
    }


    fun getAllFoldersFromRoot(): List<Folder> {
        val root = Environment.getExternalStorageDirectory() // Access external storage root
        val folders = mutableListOf<Folder>()

        root.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            folders.add(
                Folder(
                    id = dir.path.hashCode().toString(), // Generate a unique ID from path
                    name = dir.name,
                    thumbnailUri = null, // You can set this if there's a specific file you want to show as thumbnail
                    itemCount = dir.listFiles()?.size ?: 0,
                    folderPath = dir.path,
                    folderType = Type.Others // Or any appropriate type you define
                )
            )
        }
        return folders
    }

    fun getFoldersFromPath(folderPath: String): List<Folder> {
        val folders = mutableListOf<Folder>()

        // Create a File object for the specified path
        val directory = File(folderPath)

        // Check if the directory exists and is indeed a directory
        if (directory.exists() && directory.isDirectory) {
            // List all immediate subdirectories within the specified path
            directory.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
                // Add each subdirectory as a Folder object
                folders.add(
                    Folder(
                        id = subDir.path.hashCode().toString(), // Generate a unique ID from path
                        name = subDir.name,
                        thumbnailUri = Uri.parse("file://${subDir.path}"), // Set directory path as thumbnail
                        itemCount = subDir.listFiles()?.size ?: 0, // Count of items within the folder
                        folderPath = subDir.path, // Set the path of the folder
                        folderType = Type.Others // Set a generic type or any specific type you prefer
                    )
                )
            }
        }

        // Return the list of immediate subdirectories
        return folders
    }

}
