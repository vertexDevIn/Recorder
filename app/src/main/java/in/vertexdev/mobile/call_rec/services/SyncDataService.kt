//package `in`.vertexdev.mobile.call_rec.services
//
//import android.app.PendingIntent
//import android.app.Service
//import android.content.ContentResolver
//import android.content.ContentUris
//import android.content.ContentValues
//import android.content.Context
//import android.content.Intent
//import android.database.Cursor
//import android.net.Uri
//import android.os.Build
//import android.os.Environment
//import android.os.IBinder
//import android.provider.CallLog
//import android.provider.ContactsContract
//import android.provider.DocumentsContract
//import android.provider.MediaStore
//import android.provider.OpenableColumns
//import android.util.Log
//import androidx.annotation.RequiresApi
//import androidx.core.app.NotificationCompat
//import com.google.firebase.crashlytics.FirebaseCrashlytics
//import dagger.hilt.android.AndroidEntryPoint
//import `in`.vertexdev.mobile.call_rec.R
//import `in`.vertexdev.mobile.call_rec.models.Lead
//import `in`.vertexdev.mobile.call_rec.models.ShouldUpload
//import `in`.vertexdev.mobile.call_rec.repo.AuthRepository
//import `in`.vertexdev.mobile.call_rec.repo.DataStoreRepo
//import `in`.vertexdev.mobile.call_rec.room.database.AppDatabase
//import `in`.vertexdev.mobile.call_rec.room.entitiy.AudioFile
//import `in`.vertexdev.mobile.call_rec.room.entitiy.UploadedCallLog
//import `in`.vertexdev.mobile.call_rec.util.Constants.APP_KEY
//import `in`.vertexdev.mobile.call_rec.util.Constants.UPLOAD_URL_BASE
//import `in`.vertexdev.mobile.call_rec.util.NotificationDismissedReceiver
//import `in`.vertexdev.mobile.call_rec.util.State
//import `in`.vertexdev.mobile.call_rec.util.Utils.parseLeads
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.flow.flow
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.MultipartBody
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody
//import org.json.JSONArray
//import org.json.JSONObject
//import java.io.BufferedReader
//import java.io.File
//import java.io.IOException
//import java.io.InputStreamReader
//import java.io.UnsupportedEncodingException
//import java.net.HttpURLConnection
//import java.net.URL
//import java.net.URLEncoder
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import java.util.concurrent.TimeUnit
//import javax.inject.Inject
//
//
//@AndroidEntryPoint
//class SyncDataService() : Service() {
//
//
//    @Inject
//    lateinit var appRepository: AuthRepository
//
//    @Inject
//    lateinit var dataStoreRepo: DataStoreRepo
//
//    private val serviceJob = Job()
//    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
//
//    private var fileUploadDomain: String? = null
//
//
//    enum class Actions {
//        START, STOP
//    }
//
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        when (intent?.action) {
//            Actions.START.toString() -> {
//                start()
//            }
//
//            Actions.STOP.toString() -> {
//                stopSelf()
//            }
//
//
//        }
//        return START_STICKY
//
//    }
//
//    private fun start() {
//
////        val stopIntent = Intent(this, SyncDataService::class.java).apply {
////            action = Actions.STOP.toString()
////        }
////        val stopPendingIntent: PendingIntent = PendingIntent.getService(
////            this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
////        )
//
//        //Intent to detect notification removal
//        val deleteIntent = Intent(this, NotificationDismissedReceiver::class.java)
//        val deletePendingIntent = PendingIntent.getBroadcast(
//            this, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification = NotificationCompat.Builder(this, "file_uploader")
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentTitle("Data Sync in Progress")
//            .setContentText("Call-Rec is syncing data in background")
//            //  .addAction(R.drawable.coffe, "Stop Sync", stopPendingIntent)
//            .setDeleteIntent(deletePendingIntent) // Set the delete intent here
//            .setOngoing(true)
//            .build()
//        startForeground(1, notification)
//        getFileUploadDomain()
////        startSync()
//    }
//
//    private fun getFileUploadDomain() {
//        serviceScope.launch {
//            val publicApiKey = dataStoreRepo.readDataStoreValue.first().apiKey
//            val response = appRepository.getFileUploadDomain(publicApiKey).execute()
//
//            if (response.isSuccessful) {
//                response.body()?.let { responseBody ->
//                    try {
//                        // Parse the response
//                        val jsonArray =
//                            JSONArray(responseBody.string()) // Parse the response string as JSON Array
//                        if (jsonArray.length() > 0) {
//                            val jsonObject =
//                                jsonArray.getJSONObject(0) // Get the first object in the array
//                            val serverDomain =
//                                jsonObject.getString("server_domain") // Extract the server_domain
//                            Log.d(
//                                "SyncDataService",
//                                "getFileUploadDomain:Success::Server Domain: $serverDomain"
//                            )
//                            logMessage("getFileUploadDomain:Success::Server Domain: $serverDomain")
//                            fileUploadDomain = serverDomain
//                            startSync()
//
//
//                            // You can now use the serverDomain value in your code
//                        } else {
//                            Log.d(
//                                "SyncDataService",
//                                "getFileUploadDomain:Success::No Data returned"
//                            )
//                            logMessage("getFileUploadDomain:Success::No Data returned")
//                        }
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                        Log.e("SyncDataService", "Error parsing response: ${e.message}")
//                        logMessage("getFileUploadDomain::: 175::Error parsing response: ${e.message}")
//                    }
//                }
//            } else {
//                Log.d("SyncDataService", "getFileUploadDomain:Failed::${response.body()} ")
//                logMessage("getFileUploadDomain: 177 Failed::${response.body()}")
//            }
//        }
//    }
//
//
//    private var logFileUri: Uri? = null
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun createLogFile() {
//        val fileName = "logfile-recorrder.txt"
//
//        // Query the MediaStore to check if the file already exists
//        val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
//        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
//        val selectionArgs = arrayOf(fileName)
//
//        val queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
//        contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                // File already exists, get its URI
//                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
//                val id = cursor.getLong(idColumn)
//                logFileUri = ContentUris.withAppendedId(queryUri, id)
//                Log.d("LogService", "Log file already exists at: $logFileUri")
//            } else {
//                // File doesn't exist, so create it
//                val contentValues = ContentValues().apply {
//                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
//                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
//                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
//                }
//
//                logFileUri = contentResolver.insert(queryUri, contentValues)
//                logFileUri?.let {
//                    Log.d("LogService", "Log file created at: $it")
//                } ?: Log.e("LogService", "Failed to create log file.")
//            }
//        }
//    }
//
//
//    private fun startSync() {
//        FirebaseCrashlytics.getInstance().log("startSync - Started")
//
//        serviceScope.launch {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                createLogFile()
//            }
//            while (isActive) {
//                logMessage("startSync - Started")
//                FirebaseCrashlytics.getInstance().log("startSync - Loop Started")
//                logMessage("######################################################################################3")
//                logMessage("startSync - startSync - Loop Started")
//                try {
//                    uploadCallLogs() // Perform sync operation
//                    FirebaseCrashlytics.getInstance().recordException(Exception("Loop Ended"))
//                } catch (e: Exception) {
//                    Log.e("SyncDataService", "Error syncing call logs: ${e.message}")
//                    FirebaseCrashlytics.getInstance()
//                        .log("SyncDataService -  Error syncing call logs: ${e.message}")
//                    FirebaseCrashlytics.getInstance().recordException(e)
//                    logMessage("startSync -205 Error syncing call logs: ${e.message}")
//                } finally {
//
//                    delay(30 * 1000) // Delay for 30 Seconds , always
//                }
//            }
//        }
//    }
//
//    private var uniqueCallId = ""
//
//    private suspend fun uploadCallLogs() = withContext(Dispatchers.IO) {
//
//        val contentResolver: ContentResolver = contentResolver
//
//        // Calculate the timestamp for 1 hours ago
//        val oneHourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
//        val cursor: Cursor? = contentResolver.query(
//            CallLog.Calls.CONTENT_URI,
//            null,
//            "${CallLog.Calls.DATE} >= ?",
//            arrayOf(oneHourAgo.toString()),
//            "${CallLog.Calls.DATE} DESC"
//        )
//        val publicApiKey = dataStoreRepo.readDataStoreValue.first().apiKey
//        val database = AppDatabase.getDatabase(applicationContext) // Get the database instance
//        val uploadedCallLogDao =
//            database.uploadedCallLogDao() // Check if the call log ID already exists in the database
//
//        cursor?.use {
//            try {
//                val dateFormat = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.getDefault())
//
//                val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
//                val durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION)
//                val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
//                val typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE)
//
//                while (cursor.moveToNext()) {
//                    val logObject = JSONObject()
//
//                    val number = cursor.getString(numberIndex)
//                    val duration = cursor.getInt(durationIndex)
//                    val timestamp = cursor.getLong(dateIndex)
//                    val type = cursor.getInt(typeIndex)
//
//                    val dateTime = dateFormat.format(Date(timestamp))
//                    val callType = getCallType(type)
//
//
//                    val callDurationSeconds = cursor.getLong(durationIndex) // Duration in seconds
//                    val callStartTimeMillis =
//                        cursor.getLong(dateIndex) // Start time in milliseconds
//
//                    // Calculate the call end time
//                    val callEndTimeMillis = callStartTimeMillis + (callDurationSeconds * 1000)
//                    val countryCode =
//                        "+91" // Example: You can make this dynamic based on user location or preferences.
//                    val cleanedPhoneNumber = if (number.startsWith(countryCode)) {
//                        number.removePrefix(countryCode)
//                    } else {
//                        number
//                    }
//                    Log.d(
//                        "SyncDataService",
//                        "uploadCallLogs:cleanedPhoneNumber::$cleanedPhoneNumber "
//                    )
//                    logMessage("uploadCallLogs:cleanedPhoneNumber::$cleanedPhoneNumber ")
//                    FirebaseCrashlytics.getInstance()
//                        .log("SyncDataService - uploadCallLogs:cleanedPhoneNumber::$cleanedPhoneNumber ")
//
//
//
//
//                    logObject.put("dateTime", dateTime)
//                    logObject.put("duration", duration)
//                    logObject.put("name", getContactName(number))
//                    logObject.put("phoneNumber", cleanedPhoneNumber)
//                    logObject.put("rawType", type)
//                    logObject.put("timestamp", timestamp)
//
//                    logObject.put("type", callType.uppercase(Locale.ROOT))
//
//                    val payload = "[${logObject}]"
//                    val encodedPayload = encodePayload(payload)
//                    uniqueCallId = generateCallLogId(number, timestamp, type)
//
//                    if (encodedPayload != null) {
//
//                        val requestUrl =
//                            "$UPLOAD_URL_BASE&payload=$payload&public_api_key=$publicApiKey&app_key=$APP_KEY&grant_access=1"
//
//                        val ll = uploadedCallLogDao.getUploadedCallLogById(uniqueCallId)
//
//                        val isAlreadyUploaded =
//                            ll?.fileUploaded ?: false
//
//                        Log.d("SyncDataService", "uploadCallLogs:$isAlreadyUploaded  ")
//                        Log.d("SyncDataService", "requestUrl:$requestUrl  ")
//                        FirebaseCrashlytics.getInstance()
//                            .log("SyncDataService - uploadCallLogs:$isAlreadyUploaded  ")
//
//                        logMessage("uploadCallLogs::isAlreadyUploaded:::$isAlreadyUploaded")
//
//
//
//
//
//
//
//                        if (!isAlreadyUploaded) {
//
//                            checkIfThisCallShouldBeUploaded(cleanedPhoneNumber).collect {
//                                when (it) {
//                                    is State.Failed -> {}
//                                    is State.Loading -> {}
//                                    is State.Success -> {
//                                        Log.d(
//                                            "SyncDataService",
//                                            "checkIfThisCallShouldBeUploaded::${it.data} "
//                                        )
//                                        FirebaseCrashlytics.getInstance()
//                                            .log("SyncDataService - checkIfThisCallShouldBeUploaded::${it.data} ")
//                                        logMessage(" checkIfThisCallShouldBeUploaded::${it.data} ")
//
//                                        if (it.data.upload) {
//
//                                            startUploadLogTask(
//                                                requestUrl,
//                                                callEndTimeMillis,
//                                                getContactName(number),
//                                                cleanedPhoneNumber,
//                                                duration.toString(),
//                                                callType.uppercase(Locale.ROOT),
//                                                it.data.id,
//                                                it.data.tagStudent
//
//                                            )
//
//                                        } else {
//                                            uploadedCallLogDao.insert(
//                                                UploadedCallLog(
//                                                    uniqueCallId,
//                                                    getContactName(number),
//                                                    true,
//                                                    callEndTimeMillis
//                                                )
//                                            )
//                                        }
//
//                                    }
//                                }
//
//                            }
//
//                        } else {
//                            Log.d("SyncDataService", "uploadCallLogs:Already Uploaded ")
//                            FirebaseCrashlytics.getInstance()
//                                .log("SyncDataService - uploadCallLogs:Already Uploaded ")
////                            withContext(Dispatchers.Main){
////                                addLogMessage("SyncDataService,uploadCallLogs:Already Uploaded::$cleanedPhoneNumber")
////                            }
//                        }
//
//
//                    } else {
//                        FirebaseCrashlytics.getInstance()
//                            .log("SyncDataService - uploadCallLogs :: Encoded Null")
//                        Log.d(
//                            "SyncDataService",
//                            "uploadCallLogs:cleanedPhoneNumber:: uploadCallLogs :: Encoded Null "
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                FirebaseCrashlytics.getInstance()
//                    .log("SyncDataService - uploadCallLogs :: Error : ${e.message}")
//                Log.d(
//                    "SyncDataService",
//                    "uploadCallLogs:cleanedPhoneNumber:: Error : ${e.message} "
//                )
//
//            }
//        }
//    }
//
//    private suspend fun checkIfThisCallShouldBeUploaded(phoneNumber: String) =
//        flow<State<ShouldUpload>> {
//
//            val publicKey = dataStoreRepo.readDataStoreValue.first().apiKey
//
//            // Perform the lead search
//            val searchLeadResponse = appRepository.search(publicKey, phoneNumber, "lead", "1")
//
//            // Check if the lead search was successful
//            if (searchLeadResponse.isSuccessful) {
//                val responseString = searchLeadResponse.body()!!.string()
//                Log.d("TAG", "search:leadResponseString:$responseString ")
//                logMessage("search:leadResponseString:$responseString ")
//
//
//                if (responseString.contains("Developer Error")) {
//                    // Handle the error case here
//
//                } else {
//                    // Parse the response and check if leads are found
//                    val leads: List<Lead>? = parseLeads(responseString)
//                    if (leads != null && leads.isNotEmpty()) {
//                        val j = leads.find { it.student_mobile == phoneNumber }
//                        if (j != null) {
//                            emit(State.success(ShouldUpload(true, j.id, "lead")))
//                            return@flow // Exit the flow since the lead has been foun
//                        }
//
//
//                    }
//                }
//            }
//
//            // If no leads are found, perform the student search
//            val searchStudentResponse = appRepository.search(publicKey, phoneNumber, "", "1")
//            if (searchStudentResponse.isSuccessful) {
//                val responseString = searchStudentResponse.body()!!.string()
//                Log.d("TAG", "search:studentResponseString:$responseString ")
//                logMessage("search:studentResponseString:$responseString ")
//
//
//                if (responseString.contains("Developer Error")) {
//                    // Handle the error case here
//                    emit(State.Success(ShouldUpload(false, "", "")))
//                } else {
//                    // Parse the response and check if students are found
//                    val students: List<Lead>? =
//                        parseLeads(responseString) // Assuming `parseStudents` exists
//                    if (!students.isNullOrEmpty()) {
//                        // Student found, emit success with student data
//                        val j = students.find { it.student_mobile == phoneNumber }
//                        if (j != null) {
//                            emit(State.success(ShouldUpload(true, j.id, "")))
//                            return@flow // Exit the flow since the lead has been found
//                        } else {
//                            // Neither lead nor student found
//                            emit(State.success(ShouldUpload(false, "", "")))
//                        }
//
//                    } else {
//                        // Neither lead nor student found
//                        emit(State.success(ShouldUpload(false, "", "")))
//                    }
//                }
//            } else {
//                // Handle the failure case for the student search
//                emit(State.success(ShouldUpload(false, "", "")))
//            }
//        }
//
//    private suspend fun startUploadLogTask(
//        requestUrl: String,
//
//        timestamp: Long,
//        name: String,
//        number: String,
//        callDuration: String,
//        recordStatus: String,
//        fromId: String,
//        studentTag: String
//    ) = withContext(Dispatchers.IO) {
//
//
//        var urlConnection: HttpURLConnection? = null
//        var reader: BufferedReader? = null
//
//
//
//        try {
//            val url = URL(requestUrl)
//            urlConnection = url.openConnection() as HttpURLConnection
//            urlConnection.requestMethod = "GET"
//
//            val responseCode = urlConnection.responseCode
//            Log.i("SyncDataService", "Response Code: $responseCode")
//            FirebaseCrashlytics.getInstance()
//                .log("SyncDataService -  startUploadLogTask - Response Code: $responseCode ")
//
//            logMessage("startUploadLogTask - Response Code: $responseCode ")
//
//
//            val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
//                urlConnection.inputStream
//            } else {
//                urlConnection.errorStream
//            }
//
//            reader = BufferedReader(InputStreamReader(inputStream))
//            val response = StringBuilder()
//
//            // Read the response
//            reader.useLines { lines ->
//                lines.forEach { response.append(it) }
//            }
//
//            // Log the response content
//            Log.i("SyncDataService", "Response: ${response.toString()}")
//            FirebaseCrashlytics.getInstance()
//                .log("SyncDataService -  startUploadLogTask - Response: ${response.toString()} ")
//
//            logMessage("startUploadLogTask - Response: ${response.toString()} ")
//
//
//            // Call the upload files function
//            findAndUploadRecordingForLog(
//                timestamp,
//                name,
//                number,
//                callDuration,
//                recordStatus,
//                fromId,
//                studentTag,
//
//                )
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Log.e("SyncDataService", "Error: ${e.message}")
//            FirebaseCrashlytics.getInstance()
//                .log("SyncDataService -  startUploadLogTask - Error: ${e.message} ")
//
//            logMessage("startUploadLogTask - 532 Error: ${e.message} ")
//
//
//        } finally {
//            urlConnection?.disconnect()
//            reader?.close()
//        }
//    }
//
//
//    private suspend fun findAndUploadRecordingForLog(
//        timeStamp: Long,
//        name: String,
//        number: String,
//        callDuration: String,
//        recordStatus: String,
//        fromId: String,
//        studentTag: String,
//
//
//        ) {
//        var found = false
//        val selectedFolderPath = dataStoreRepo.readDataStoreValue.first().folderPath
//        Log.d("SyncDataService", "uploadFiles:selectedFolderPath::$selectedFolderPath")
//        FirebaseCrashlytics.getInstance()
//            .log("SyncDataService -  uploadFiles:selectedFolderPath::$selectedFolderPath ")
//
//        val folderUri = Uri.parse(selectedFolderPath) // Convert string to Uri
//        Log.d("SyncDataService", "uploadFiles:folderUri::$folderUri")
//        FirebaseCrashlytics.getInstance()
//            .log("SyncDataService -  uploadFiles:folderUri::$folderUri ")
//
//        logMessage("uploadFiles:selectedFolderPath::$selectedFolderPath")
//        logMessage("uploadFiles:uploadFiles:folderUri::$folderUri ")
//
//        // Grant permission to access folder
//        val takeFlags =
//            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//        contentResolver.takePersistableUriPermission(folderUri, takeFlags)
//
//        // Use ContentResolver to query the folder's contents
//        val contentResolver = applicationContext.contentResolver
//
//        // Prepare the query for the folder's documents
//        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
//            folderUri,
//            DocumentsContract.getTreeDocumentId(folderUri)
//        )
//
//        // Projection for the query: we want the document ID and last modified date
//        val projection = arrayOf(
//            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
//            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
//            OpenableColumns.DISPLAY_NAME,
//            DocumentsContract.Document.COLUMN_MIME_TYPE
//        )
//
//        // Prepare time window: last hour in milliseconds
//        val oneHourAgo = System.currentTimeMillis() - 60 * 60 * 1000
//
//        Log.d("SyncDataService", "Children URI: $childrenUri")
//        FirebaseCrashlytics.getInstance()
//            .log("SyncDataService - Children URI: $childrenUri ")
//        logMessage("uploadFiles:Children URI: $childrenUri")
//
//
//        // Query folder contents
//        val cursor = contentResolver.query(
//            childrenUri,
//            projection,
//            "${DocumentsContract.Document.COLUMN_LAST_MODIFIED} > ?",
//            arrayOf(oneHourAgo.toString()),
//            "${DocumentsContract.Document.COLUMN_LAST_MODIFIED} DESC"
//        )
//
//        cursor?.use {
//            if (it.count > 0) {
//                Log.d("SyncDataService", "greater than  ${it.count}")
//                logMessage("uploadFiles:greater ${it.count}")
//
//                FirebaseCrashlytics.getInstance()
//                    .log("SyncDataService - greater than 0")
//                // Get column indices
//                val documentIdColumnIndex =
//                    it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
//                val displayNameColumnIndex =
//                    it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
//                val mimeTypeColumnIndex =
//                    it.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
//                val lastModifiedColumnIndex =
//                    it.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
//
//                while (it.moveToNext()) {
//                    // Fetch document ID, display name, and MIME type
//                    val documentId = it.getStringOrNull(documentIdColumnIndex) ?: continue
//                    val displayName = it.getStringOrNull(displayNameColumnIndex) ?: continue
//                    val mimeType = it.getStringOrNull(mimeTypeColumnIndex) ?: continue
//                    val lastModifiedTime = it.getLongOrNull(lastModifiedColumnIndex)
//                    val fileUniqueId = displayName + lastModifiedTime
//                    Log.d("SyncDataService", "uploadFiles:Name log ::${name} ")
//                    Log.d("SyncDataService", "uploadFiles:displayName  ::${displayName} ")
//                    FirebaseCrashlytics.getInstance()
//                        .log("SyncDataService - uploadFiles:Name log ::${name} ")
//                    FirebaseCrashlytics.getInstance()
//                        .log("SyncDataService -uploadFiles:displayName  ::${displayName} ")
//
//                    logMessage("uploadFiles:Name log ::${name}")
//                    logMessage("uploadFiles:displayName  ::${displayName}")
//
//
//                    // Check if the MIME type corresponds to audio formats and name contains the substring
//                    if (mimeType.startsWith("audio/")
//                    ) {
//                        // Check if last modified time matches the provided timestamp (within 1 second range)
//                        Log.d(
//                            "SyncDataService",
//                            "uploadFiles:LOG End time ::${timeStamp.toFormattedDateTime()} "
//                        )
//
//                        FirebaseCrashlytics.getInstance()
//                            .log("SyncDataService -uploadFiles:LOG End time ::${timeStamp.toFormattedDateTime()} ")
//
//                        logMessage("uploadFiles:LOG End time ::${timeStamp.toFormattedDateTime()}")
//
//                        if (lastModifiedTime != null) {
//                            Log.d(
//                                "SyncDataService",
//                                "uploadFiles:file time ::${lastModifiedTime.toFormattedDateTime()} "
//                            )
//
//                            FirebaseCrashlytics.getInstance()
//                                .log("SyncDataService -uploadFiles:file time : ${lastModifiedTime.toFormattedDateTime()}")
//
//                            logMessage("uploadFiles:file time : ${lastModifiedTime.toFormattedDateTime()}")
//
//                        }
//                        // Calculate the time difference directly using Long values
//                        val timeDifference = Math.abs((lastModifiedTime ?: 0L) - timeStamp)
//
//                        Log.d(
//                            "SyncDataService",
//                            "uploadFiles:file time timeDifference ::${timeDifference} "
//                        )
//                        logMessage("uploadFiles:file time timeDifference ::${timeDifference}")
//
//                        FirebaseCrashlytics.getInstance()
//                            .log("SyncDataService -uploadFiles:file time timeDifference ::${timeDifference}")
//
//
//
//
//                        if (displayName.contains(name) || displayName.contains(number)) {
//                            if (lastModifiedTime != null && timeDifference <= 25000) {
//                                found = true
//                                // Build the document URI for audio files
//                                val fileUri =
//                                    DocumentsContract.buildDocumentUriUsingTree(
//                                        folderUri,
//                                        documentId
//                                    )
//
//                                // Log file details
//                                Log.d("SyncDataService", "Uploading matching audio file: $fileUri")
//                                logMessage("Uploading matching audio file: $fileUri")
//
//                                FirebaseCrashlytics.getInstance()
//                                    .log("SyncDataService -Uploading matching audio file: $fileUri")
//
//
//                                val recordedDateTime = formatTimestamp(lastModifiedTime)
//                                Log.d("TAG", "findAndUploadRecordingForLog:$recordedDateTime ")
//
//                                logMessage("findAndUploadRecordingForLog:$recordedDateTime")
//
//                                FirebaseCrashlytics.getInstance()
//                                    .log("SyncDataService -findAndUploadRecordingForLog:$recordedDateTime")
//
//                                // Upload the file
//                                if (fileUploadDomain != null) {
//                                    val url =
//                                        "https://$fileUploadDomain/gallery/receiver?app_key=$APP_KEY"
//                                    val result = uploadFileIfNeeded(
//                                        fileUniqueId,
//                                        applicationContext,
//                                        fileUri,
//                                        url,
//                                        callDuration,
//                                        recordStatus,
//                                        fromId,
//                                        studentTag,
//                                        recordedDateTime
//                                    )
//                                    logMessage("findAndUploadRecordingForLog::completed$result")
//
//                                    if (result) {
//                                        // Exit the loop as file is found
//
//                                        break
//                                    }
//
//                                } else {
//
//                                    Log.d("SyncDataService", "fileUploadDomain null")
//                                    FirebaseCrashlytics.getInstance()
//                                        .log("SyncDataService -fileUploadDomain null")
//                                    logMessage("fileUploadDomain null")
//
//                                }
//                            } else {
//                                Log.d("SyncDataService", "Modified date do not match")
//                                FirebaseCrashlytics.getInstance()
//                                    .log("SyncDataService -Modified date do not match")
//                                logMessage("Modified date do not match")
//
//                            }
//
//                        } else {
//                            Log.d("SyncDataService", "File does not contain name or number")
//                            FirebaseCrashlytics.getInstance()
//                                .log("File does not contain name or number")
//                            logMessage("File does not contain name or number")
//
//                        }
//
//                    } else {
//                        Log.d("SyncDataService", "Skipped file: $displayName, MIME type: $mimeType")
//                        FirebaseCrashlytics.getInstance()
//                            .log("SyncDataService -Skipped file: $displayName, MIME type: $mimeType")
//                        logMessage("Skipped file: $displayName, MIME type: $mimeType")
//                    }
//                }
//                cursor.close() // Don't forget to close the cursor
//
//                val database =
//                    AppDatabase.getDatabase(applicationContext) // Get the database instance
//                val uploadedCallLogDao =
//                    database.uploadedCallLogDao() // Check if the call log ID already exists in the database
//
//                if (!found) {
//
//                    uploadedCallLogDao.insert(
//                        UploadedCallLog(
//                            uniqueCallId,
//                            getContactName(number),
//                            false,
//                            0L
//                        )
//                    )
//                } else {
//                    uploadedCallLogDao.insert(
//                        UploadedCallLog(
//                            uniqueCallId,
//                            getContactName(number),
//                            true,
//                            0L
//                        )
//                    )
//
//                    Log.d("SyncDataService", "File found : ")
//                    FirebaseCrashlytics.getInstance()
//                        .log("SyncDataService -File found")
//                    logMessage("File found :")
//                }
//            } else {
//                Log.d("SyncDataService", "Folder has no files")
//                FirebaseCrashlytics.getInstance()
//                    .log("SyncDataService -Folder has no files")
//                logMessage("SyncDataService -Folder has no files")
//            }
//        } ?: run {
//            Log.e("SyncDataService", "Unable to query files in the folder.")
//            FirebaseCrashlytics.getInstance()
//                .log("SyncDataService -Unable to query files in the folder")
//            logMessage("SyncDataService -Unable to query files in the folder")
//        }
//
//    }
//
//    private fun generateCallLogId(phoneNumber: String, timestamp: Long, type: Int): String {
//        return phoneNumber + "_" + timestamp + "_" + type
//    }
//
//    private suspend fun uploadFile(
//        uniqueId: String,
//        context: Context,
//        fileUri: Uri,
//        base: String,
//        callDuration: String,
//        recordStatus: String,
//        fromId: String,
//        studentTag: String,
//        recordedDateTime: String,
//
//        ) {
//        Log.d("SyncDataService", "uploadFiles:uploadFile::$base ")
//        Log.d("SyncDataService", "uploadFiles:uploadFile::Started ")
//        FirebaseCrashlytics.getInstance()
//            .log("SyncDataService ::uploadFiles:uploadFile::Started")
//        logMessage("uploadFiles:uploadFile::$base ")
//
//        val client = OkHttpClient()
//
//        try {
//            // Open InputStream from the Uri
//            val inputStream = context.contentResolver.openInputStream(fileUri)
//            inputStream?.use { stream ->
//                val requestFile =
//                    RequestBody.create("audio/*".toMediaTypeOrNull(), stream.readBytes())
//                val requestBody = MultipartBody.Builder()
//                    .setType(MultipartBody.FORM)
//                    .addFormDataPart(
//                        "sendfile",
//                        fileUri.lastPathSegment ?: "file",
//                        requestFile
//                    ) // Use lastPathSegment as filename
//
//                    .build()
//
//                val request = Request.Builder()
//                    .url(base)
//                    .post(requestBody)
//                    .build()
//
//                // Make the request in a coroutine
//                val response = client.newCall(request).execute()
//                if (response.isSuccessful) {
//                    // Ensure the response body is not null
//                    val responseBody = response.body?.string()
//                    if (responseBody != null) {
//                        try {
//                            // Parse the response JSON
//                            val jsonArray = JSONArray(responseBody)
//                            val jsonObject = jsonArray.getJSONObject(0)
//
//                            // Extract the "filename"
//                            val filename = jsonObject.getString("filename")
//                            Log.d(
//                                "SyncDataService",
//                                "uploadFiles:uploadFile::Successful:: $filename"
//                            )
//                            FirebaseCrashlytics.getInstance()
//                                .log("SyncDataService ::uploadFiles:uploadFile::Successful:: $filename")
//
//                            logMessage("uploadFiles:uploadFile::Successful:: $filename ")
//
//                            addAudioDetails(
//                                uniqueId,
//                                filename,
//                                fromId = fromId,
//                                studentTag = studentTag,
//                                callDuration.toInt(),
//                                recordStatus,
//                                recordedDateTime
//
//                            )
//
//
//                            // Handle successful upload
//                            // (Add further logic for successful upload handling here)
//                        } catch (e: Exception) {
//                            Log.e("SyncDataService", "Error parsing response: ${e.message}")
//                            FirebaseCrashlytics.getInstance()
//                                .log("SyncDataService ::uploadFiles:Error parsing response: ${e.message}")
//                            logMessage("uploadFiles:Error parsing response: ${e.message}")
//
//                        }
//                    } else {
//                        Log.e("SyncDataService", "uploadFiles: Response body is null")
//                        FirebaseCrashlytics.getInstance()
//                            .log("SyncDataService ::uploadFiles:uploadFiles: Response body is null")
//                        logMessage("uploadFiles: Response body is null")
//
//                    }
//                } else {
//                    Log.e(
//                        "SyncDataService",
//                        "uploadFiles: Upload failed with status code: ${response.code}"
//                    )
//                    logMessage("uploadFiles: Upload failed with status code: ${response.code}")
////                    withContext(Dispatchers.Main){
////                        addLogMessage("SyncDataService, 836  Upload failed with status code: ${response.code}")
////
////                    }
//                    val database =
//                        AppDatabase.getDatabase(applicationContext) // Get the database instance
//                    val uploadedCallLogDao =
//                        database.uploadedCallLogDao()
//                    uploadedCallLogDao.insert(UploadedCallLog(uniqueCallId, "fileName", true, 0))
//                }
//            }
//        } catch (e: IOException) {
//            Log.e("SyncDataService", "uploadFile:IOException:: ${e.message}", e)
//            logMessage("uploadFile:IOException:: ${e.message}")
//
//        } catch (e: Exception) {
//            Log.e("SyncDataService", "uploadFile:Exception:: ${e.message}", e)
//            logMessage("uploadFile:Exception:: ${e.message}")
//
//        }
//    }
//
//    private suspend fun addAudioDetails(
//        uniqueId: String,
//        fileName: String,
//        fromId: String,
//        studentTag: String,
//        callDuration: Int,
//        recordStatus: String,
//        recordedDateTime: String
//    ) {
//        Log.d(
//            "SyncDataService",
//            "addAudioDetails ::Initiated::fileName::$fileName, fromId::$fromId,studentTag::$studentTag,callDuration::$callDuration,recordStatus::$recordStatus"
//        )
//        FirebaseCrashlytics.getInstance()
//            .log("SyncDataService ::addAudioDetails ::Initiated::fileName::$fileName, fromId::$fromId,studentTag::$studentTag,callDuration::$callDuration,recordStatus::$recordStatus")
//        logMessage("addAudioDetails ::Initiated::fileName::$fileName, fromId::$fromId,studentTag::$studentTag,callDuration::$callDuration,recordStatus::$recordStatus")
//
//        val publicApiKey = dataStoreRepo.readDataStoreValue.first().apiKey
//        val userId = dataStoreRepo.readDataStoreValue.first().userId
//        val response = appRepository.uploadAudioFileDetails(
//            publicApiKey,
//            userId.toInt(),
//            studentTag,
//            fromId,
//            fileName,
//            callDuration,
//            recordStatus,
//            recordedDateTime
//        ).execute()
//
//        val database = AppDatabase.getDatabase(applicationContext) // Get the database instance
//        val uploadedCallLogDao =
//            database.uploadedCallLogDao()
//
//        if (response.isSuccessful) {
//
//            Log.d(
//                "TAG", "addAudioDetails:Success::" +
//                        "${response.body().toString()} "
//            )
//            FirebaseCrashlytics.getInstance()
//                .log(
//                    "SyncDataService ::addAudioDetails addAudioDetails:Success::\" +\n" +
//                            "                        \"${response.body().toString()} \""
//                )
//
//            logMessage(
//                "SyncDataService ::addAudioDetails addAudioDetails:Success::\" +\n" +
//                        "                        \"${response.body().toString()} \""
//            )
//
//            logMessage("##########################3")
//
//
//            uploadedCallLogDao.insertAudioFile(AudioFile(id = uniqueId))
//            uploadedCallLogDao.insert(UploadedCallLog(uniqueCallId, fileName, true, 0))
//        } else {
//            Log.d(
//                "TAG", "addAudioDetails:FAILED::" +
//                        "${response.message()} "
//            )
//            FirebaseCrashlytics.getInstance()
//                .log(
//                    "SyncDataService ::addAudioDetails:FAILED::\" +\n" +
//                            "                        \"${response.message()} \""
//                )
//            logMessage(
//                "SyncDataService ::addAudioDetails:FAILED::\" +\n" +
//                        "                        \"${response.message()} \""
//            )
//
//            uploadedCallLogDao.insert(UploadedCallLog(uniqueCallId, fileName, false, 0))
//
//        }
//
//    }
//
//
//    private suspend fun uploadFileIfNeeded(
//        uniqueId: String,
//        context: Context,
//        fileUri: Uri,
//        base: String,
//        callDuration: String,
//        recordStatus: String,
//        fromId: String,
//        studentTag: String,
//        recordedDateTime: String
//    ): Boolean {
//        var result = false
//        Log.d("SyncDataService", "uploadFiles:uploadFileIfNeeded::Started ")
//        FirebaseCrashlytics.getInstance()
//            .log("SyncDataService up::loadFiles:uploadFileIfNeeded::Started")
//        val database = AppDatabase.getDatabase(applicationContext) // Get the database instance
//        val uploadedCallLogDao =
//            database.uploadedCallLogDao() // Check if the call log ID already exists in the database
//        val existingFile = uploadedCallLogDao.getAudioFileById(uniqueId)
//        if (existingFile == null) {
//            result = true
//            // File not uploaded yet, proceed to upload
//            uploadFile(
//                uniqueId,
//                context,
//                fileUri,
//                base,
//                callDuration,
//                recordStatus,
//                fromId,
//                studentTag,
//                recordedDateTime
//            )
//
//            // After successful upload, save the unique ID to Room
//            //  uploadedCallLogDao.insertAudioFile(AudioFile(id = fileUri.toString()))
//        } else {
//            result = false
//            Log.d("SyncDataService", "Skipping already uploaded file: $fileUri")
//            Log.d("FileUploader", "Skipping already uploaded file: $fileUri")
//            FirebaseCrashlytics.getInstance()
//                .log("SyncDataService Skipping already uploaded file: $fileUri")
//        }
//        return result
//    }
//
//    // Extension functions for handling null values in the cursor
//    private fun Cursor.getStringOrNull(columnIndex: Int): String? {
//        return if (columnIndex >= 0) getString(columnIndex) else null
//    }
//
//    private fun Cursor.getLongOrNull(columnIndex: Int): Long? {
//        return if (columnIndex >= 0) getLong(columnIndex) else null
//    }
//
//
//    private fun encodePayload(payload: String): String? {
//        return try {
//            URLEncoder.encode(payload, "UTF-8").replace("\\+".toRegex(), "%20")
//        } catch (e: UnsupportedEncodingException) {
//            e.printStackTrace()
//            null
//        }
//    }
//
//
//    private fun getCallType(type: Int): String {
//        return when (type) {
//            CallLog.Calls.INCOMING_TYPE -> "INCOMING"
//            CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
//            CallLog.Calls.MISSED_TYPE -> "MISSED"
//            CallLog.Calls.REJECTED_TYPE -> "REJECTED"
//            else -> "UNKNOWN"
//        }
//    }
//
//    private fun getContactName(phoneNumber: String): String {
//        val uri = Uri.withAppendedPath(
//            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
//            Uri.encode(phoneNumber)
//        )
//        val contentResolver: ContentResolver = contentResolver
//        val contactLookup = contentResolver.query(
//            uri,
//            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
//            null,
//            null,
//            null
//        )
//
//        var result: String? = null
//        contactLookup?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
//                if (nameIndex != -1) {
//                    result = cursor.getString(nameIndex)
//                }
//            }
//        } ?: Log.e("getContactName", "Cursor is null")
//
//        return result ?: phoneNumber
//    }
//
//
//    override fun onDestroy() {
//        super.onDestroy()
//        // Cancel the service job when the service is destroyed
//        serviceJob.cancel()
//    }
//
//
//    private fun Long.toFormattedDateTime(): String {
//        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
//        val date = Date(this)
//        return dateFormat.format(date)
//    }
//
//    private fun formatTimestamp(timestamp: Long): String {
//        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault())
//        return dateFormat.format(Date(timestamp))
//    }
//
//
//    private suspend fun logMessage(message: String) {
//        logFileUri?.let { uri ->
//            val dateFormat = SimpleDateFormat("dd:MM:yyyy HH:mm:ss", Locale.getDefault())
//            val timestamp = dateFormat.format(Date())
//            val logEntry = "$timestamp - $message\n"
//
//            try {
//                contentResolver.openOutputStream(uri, "wa")?.use { outputStream ->
//                    outputStream.write(logEntry.toByteArray())
//                    Log.d("LogService", "Logged message: $logEntry")
//                }
//            } catch (e: IOException) {
//                Log.e("LogService", "Error writing to file: ${e.message}")
//            }
//        } ?: Log.e("LogService", "Log file URI is null.")
//    }
//
//
//}