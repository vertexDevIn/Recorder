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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.vertexdev.mobile.call_rec.models.Folder
import `in`.vertexdev.mobile.call_rec.models.LeadCategory
import `in`.vertexdev.mobile.call_rec.models.Type
import `in`.vertexdev.mobile.call_rec.repo.AuthRepository
import `in`.vertexdev.mobile.call_rec.repo.DataStoreRepo
import `in`.vertexdev.mobile.call_rec.util.State
import `in`.vertexdev.mobile.call_rec.util.Utils.sortedByName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject


// AuthViewModel.kt
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repository: AuthRepository,
    private val dataStore: DataStoreRepo,
) : AndroidViewModel(application) {

    val datastore = dataStore.readDataStoreValue




    private val _leadCategory = MutableStateFlow<State<List<LeadCategory>>>(State.loading())
    val leadCategory: StateFlow<State<List<LeadCategory>>> = _leadCategory.asStateFlow()

    private val _studentCategory = MutableStateFlow<State<List<LeadCategory>>>(State.loading())
    val studentCategory: StateFlow<State<List<LeadCategory>>> = _studentCategory.asStateFlow()

    init{
       //  delete24HoursBeforeRecordings()
    }

//    private fun delete24HoursBeforeRecordings() = viewModelScope.launch(Dispatchers.IO) {
//        val deleteActive =  datastore.first().deleteRecordings
//        Log.d("DeleteRecordings", "Starting deletion of files older than 24 hours")
//        if(deleteActive){
//            try {
//
//                val selectedFolderUri = datastore.first().folderPath
//                Log.d("DeleteRecordings", "Selected folder URI: $selectedFolderUri")
//
//                val folderUri = Uri.parse(selectedFolderUri)
//
//                // Grant permission to access the folder
//                val contentResolver = getApplication<Application>().contentResolver
//                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//                contentResolver.takePersistableUriPermission(folderUri, takeFlags)
//                Log.d("DeleteRecordings", "Permission granted for folder access")
//
//                // Build URI to access folder contents
//                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
//                    folderUri,
//                    DocumentsContract.getTreeDocumentId(folderUri)
//                )
//                Log.d("DeleteRecordings", "Child URI: $childrenUri")
//
//                // Projection for the query
//                val projection = arrayOf(
//                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
//                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
//                    OpenableColumns.DISPLAY_NAME,
//                    DocumentsContract.Document.COLUMN_FLAGS // Check for deletable files
//                )
//
//                // Prepare time window: 24 hours ago in milliseconds
//                val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
//                Log.d("DeleteRecordings", "Time threshold (24 hours ago): $twentyFourHoursAgo")
//
//                // Query folder contents (without filtering by lastModified)
//                val cursor = contentResolver.query(
//                    childrenUri,
//                    projection,
//                    null, // No filtering here
//                    null,
//                    null
//                )
//
//                cursor?.use { cur ->
//                    val idColumn = cur.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
//                    val nameColumn = cur.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
//                    val lastModifiedColumn = cur.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
//                    val flagsColumn = cur.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS)
//
//                    var fileCount = 0
//                    var nonDeletableCount = 0
//                    while (cur.moveToNext()) {
//                        val documentId = cur.getString(idColumn)
//                        val fileName = cur.getString(nameColumn)
//                        val lastModified = cur.getLong(lastModifiedColumn)
//                        val flags = cur.getInt(flagsColumn)
//
//                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(
//                            folderUri,
//                            documentId
//                        )
//
//                        // Log the file details
//                        Log.d("FileToDelete", "File: $fileName, Last Modified: $lastModified")
//
//                        // Manually check if the file is older than 24 hours
//                        if (lastModified < twentyFourHoursAgo) {
//                            // Check if the file supports deletion
//                            if (flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0) {
//                                try {
//                                    // Use DocumentsContract.deleteDocument() to delete the file
//                                    DocumentsContract.deleteDocument(contentResolver, fileUri)
//                                    Log.d("FileDeleted", "Successfully deleted: $fileName")
//                                    fileCount++
//                                } catch (e: Exception) {
//                                    Log.e("FileDeleteError", "Error deleting file: $fileName", e)
//                                }
//                            } else {
//                                Log.d("FileNotDeletable", "File $fileName does not support deletion.")
//                                nonDeletableCount++
//                            }
//                        } else {
//                            Log.d("FileNotOldEnough", "File $fileName is not older than 24 hours.")
//                        }
//                    }
//                    Log.d("DeleteRecordings", "Total files deleted: $fileCount")
//                    Log.d("DeleteRecordings", "Total files that could not be deleted: $nonDeletableCount")
//                } ?: Log.d("DeleteRecordings", "No files found")
//            } catch (e: Exception) {
//                Log.e("DeleteRecordings", "Error during deletion", e)
//            }
//            Log.d("DeleteRecordings", "Deletion process completed")
//        }
//
//    }



    fun getData1(filter: String) = viewModelScope.launch {

        _leadCategory.value = State.loading()

        val toDate = Calendar.getInstance()
        val fromDate = toDate.clone() as Calendar

        when (filter) {
            "today" -> {
                // No changes needed for today's date
            }

            "last_7_days" -> fromDate.add(Calendar.DAY_OF_YEAR, -7)
            "last_30_days" -> fromDate.add(Calendar.DAY_OF_YEAR, -30)
            "last_6_months" -> fromDate.add(Calendar.MONTH, -6)
            else -> throw IllegalArgumentException("Invalid filter: $filter")
        }
        val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        val formattedFromDate = formatter.format(fromDate.time)
        val formattedToDate = formatter.format(toDate.time)
        val apiKey = datastore.first().apiKey
         val userId = datastore.first().userId
        Log.d("TAG", "verifyOtp: apiKey::$apiKey")

        val response = repository.getApiData(apiKey, "lead",
            listOf(userId), formattedFromDate, formattedToDate)
         if (response != null) {
             Log.d("TAG", "getData1:${response.body()} ")
             if (response.isSuccessful) {
                 val catData = response.body()
                 if (catData != null) {
                     _leadCategory.value = State.Success(catData)


                 } else {
                     _leadCategory.value = State.Failed("Data is null")
                 }

             } else {
                 _leadCategory.value = State.Failed(response.message())
             }
         }

    }
     fun getData2(filter: String) = viewModelScope.launch {
        _studentCategory.value = State.loading()
        val toDate = Calendar.getInstance()
        val fromDate = toDate.clone() as Calendar

        when (filter) {
            "today" -> {
                // No changes needed for today's date
            }

            "last_7_days" -> fromDate.add(Calendar.DAY_OF_YEAR, -7)
            "last_30_days" -> fromDate.add(Calendar.DAY_OF_YEAR, -30)
            "last_6_months" -> fromDate.add(Calendar.MONTH, -6)
            else -> throw IllegalArgumentException("Invalid filter: $filter")
        }
        val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        val formattedFromDate = formatter.format(fromDate.time)
        val formattedToDate = formatter.format(toDate.time)
        val apiKey = datastore.first().apiKey
         val userId = datastore.first().userId


        val response = repository.getStudentStats(apiKey, listOf(userId), formattedFromDate, formattedToDate)
         if (response != null) {
             Log.d("TAG", "getData2:${response.body()} ")
             if (response.isSuccessful) {
                 val catData = response.body()
                 if (catData != null) {
                     _studentCategory.value = State.Success(catData)


                 } else {
                     _studentCategory.value = State.Failed("Data is null")
                 }

             } else {
                 _studentCategory.value = State.Failed(response.message())
             }
         }


    }

    fun getAndSaveServerInfo() = viewModelScope.launch(Dispatchers.IO) {
        try {
            // Fetch the publicKey from the datastore
            val publicKey = datastore.first().apiKey

            // Execute the API call
            val response = repository.getFileUploadDomain(publicKey).execute()

            // Check if the response is successful
            if (response.isSuccessful && response.body() != null) {
                // Convert the response body to a JSON string
                val responseBody = response.body()!!.string()

                // Parse the JSON response (assuming it's an array as per your example)
                val jsonArray = JSONArray(responseBody)

                // Get the first object in the array (if it exists)
                if (jsonArray.length() > 0) {
                    val jsonObject = jsonArray.getJSONObject(0)

                    // Extract the "server_pagenation_count"
                    val serverPagenationCount = jsonObject.getString("server_pagenation_count")

                    // Log or process the server_pagenation_count
                    Log.d("TAG", "Server Pagenation Count: $serverPagenationCount")
                    dataStore.savePageCount(serverPagenationCount)
                } else {
                    Log.d("TAG", "getAndSaveServerInfo::No data found in response")
                }
            } else {
                Log.d("TAG", "getAndSaveServerInfo::Failed::${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("TAG", "Error in getAndSaveServerInfo", e)
        }
    }

     fun getFolders(): List<Folder> {
        val folders = mutableMapOf<String, Folder>() // To store unique folders by ID

        // Define the URI for all files (not limited to audio)
        val folderUri = MediaStore.Files.getContentUri("external")

        // Define the projection for columns we need
        val projection = arrayOf(
            MediaStore.Files.FileColumns.BUCKET_ID,         // Folder ID
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME, // Folder name
            MediaStore.Files.FileColumns.DATA                // File path
        )

        // Define the selection criteria to filter out folders with null names
        val selection = MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME + " IS NOT NULL"

        // Query the content provider for all files
        getApplication<Application>().contentResolver.query(
            folderUri, projection, selection, null, null
        )?.use { cursor ->
            // Get column indices
            val bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            // Iterate through each row in the cursor
            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdIndex)
                val bucketName = cursor.getString(bucketNameIndex)
                val filePath = cursor.getString(dataIndex)

                // Create or retrieve a folder entry based on the bucket ID
                val folder = folders.getOrPut(bucketId) {
                    Folder(
                        id = bucketId,
                        name = bucketName,
                        thumbnailUri = Uri.parse("file://$filePath"), // Set file path as thumbnail
                        itemCount = 0, // Initialize count to be incremented below
                        folderPath = filePath, // Set file path as a placeholder for folder path
                        folderType = Type.Audio // Set type to Audio
                    )
                }

                // Increment item count for the folder
                folder.itemCount += 1
            }
        }

        // Return the list of all folders, including empty folders
        return folders.values.toList()
    }



}