package `in`.vertexdev.mobile.call_rec.services

import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat

import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.models.ShouldUpload
import `in`.vertexdev.mobile.call_rec.repo.AuthRepository
import `in`.vertexdev.mobile.call_rec.repo.DataStoreRepo
import `in`.vertexdev.mobile.call_rec.room.database.AppDatabase
import `in`.vertexdev.mobile.call_rec.room.entitiy.UploadedCallLog
import `in`.vertexdev.mobile.call_rec.util.Constants.APP_KEY
import `in`.vertexdev.mobile.call_rec.util.Constants.UPLOAD_URL_BASE
import `in`.vertexdev.mobile.call_rec.util.NotificationDismissedReceiver
import `in`.vertexdev.mobile.call_rec.util.State
import `in`.vertexdev.mobile.call_rec.util.Utils.parseLeads
import kotlinx.coroutines.*

import kotlinx.coroutines.isActive


import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * SyncDataService is a foreground service that syncs call logs and associated audio recordings
 * to a remote server. The service periodically checks for new call logs, attempts to locate
 * matching recordings, and uploads them if found.
 */
@AndroidEntryPoint
class SyncDataService2 : Service() {

    @Inject
    lateinit var appRepository: AuthRepository

    @Inject
    lateinit var dataStoreRepo: DataStoreRepo
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var fileUploadDomain: String? = null

    @Volatile
    private var isCallLogSyncRunning = false

    @Volatile
    private var firstTime = true

    @Volatile
    private var isCallFileSyncRunning = false


    private val fileAttemptMap: MutableMap<String, Int> = mutableMapOf()
    private val MAX_ATTEMPTS = 10

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Starts or stops the sync process based on the intent action.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START.name -> startSyncProcess()
            Actions.STOP.name -> stopSelf()
        }
        return START_STICKY
    }

    /**
     * Initializes the foreground service with a notification and begins syncing call logs.
     */
    private fun startSyncProcess() {
        startForegroundServiceNotification()
        serviceScope.launch {
            fileUploadDomain = fetchFileUploadDomain()
            Log.d("TAG", "startSyncProcess:fileUploadDomain$fileUploadDomain ")
            fileUploadDomain?.let {
                if (!isCallLogSyncRunning) {
                    isCallLogSyncRunning = true
                    launch {
                        try {
                            startCallLogSyncLoop()
                        } finally {
                            isCallLogSyncRunning = false // Ensure flag resets if loop ends
                        }
                    }
                } else {
                    logMessage("Call log sync is already running.")
                }

                if (!isCallFileSyncRunning) {
                    isCallFileSyncRunning = true
                    launch {
                        try {
                            startCallFilesSyncLoop()
                        } finally {
                            isCallFileSyncRunning = false // Ensure flag resets if loop ends
                        }
                    }
                } else {
                    logMessage("Call file sync is already running.")
                }
            }
            if (fileUploadDomain == null) {
                logMessage("fileUploadDomain is null")
            }
        }
    }

    /**
     * Starts a loop to continuously sync call recording files from the folder.
     * Acquires the folder path from the datastore and calls processNewRecordingFile for each file.
     */
    private suspend fun startCallFilesSyncLoop() {
        val folderPath = dataStoreRepo.readDataStoreValue.first().folderPath
        val folder = File(folderPath)

        while (true) {
            folder.listFiles()?.forEach { file ->
                processNewRecordingFile(file.absolutePath)
            }
            delay(10000) // Delay between loops to prevent excessive CPU usage
        }
    }

    /**
     * Displays a persistent notification to keep the service in the foreground.
     */
    private fun startForegroundServiceNotification() {
        //Intent to detect notification removal
        val deleteIntent = Intent(this, NotificationDismissedReceiver::class.java)
        val deletePendingIntent = PendingIntent.getBroadcast(
            this, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, "file_uploader")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Data Sync in Progress")
            .setContentText("Call-Rec is syncing data in background")
            .setDeleteIntent(deletePendingIntent) // Set the delete intent here
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    /**
     * Fetches the domain URL required for uploading files to the server.
     */
    private suspend fun fetchFileUploadDomain(): String? {
        return withContext(Dispatchers.IO) {
            val publicApiKey = dataStoreRepo.readDataStoreValue.first().apiKey
            try {
                val response = appRepository.getFileUploadDomain(publicApiKey).execute()
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            // Parse the response as a JSON Array
                            val jsonArray = JSONArray(responseBody.string())

                            if (jsonArray.length() > 0) {
                                // Get the first object in the array
                                val jsonObject = jsonArray.getJSONObject(0)
                                // Extract and return the server_domain
                                jsonObject.getString("server_domain")
                            } else {
                                logMessage("fetchFileUploadDomain: No data returned in response array")
                                null
                            }
                        } catch (e: Exception) {
                            logMessage("Error parsing JSON response: ${e.message}")
                            null
                        }
                    }
                } else {
                    logMessage("fetchFileUploadDomain: Response unsuccessful, code: ${response.code()}")
                    null
                }
            } catch (e: IOException) {
                logMessage("Error fetching file upload domain: ${e.message}")
                null
            }
        }
    }

    private val logsMutex = Mutex()

    /**
     * Main loop that checks and uploads new call logs and recordings periodically.
     */
    private suspend fun startCallLogSyncLoop() {
        while (coroutineContext.isActive) {
            logsMutex.withLock {
                /// logToFile("Starting call log sync iteration...")
                Log.i("TAG", "startSyncLoop:Starting sync iteration... ")
                try {
                    uploadCallLogs()
                } catch (e: Exception) {
                    //  logToFile("Sync iteration failed: ${e.message}")
                    Log.i("TAG", "ync iteration failed: ${e.message}")
                }
            }
            delay(10000)
        }
    }

    /**
     * Uploads recent call logs to the server after verifying eligibility.
     *
     * This function runs in the I/O context to efficiently handle database queries and network calls.
     * It either retrieves the recent call logs from the last hour or all call logs, depending on the value of `firstTime`.
     * For each call log entry, it checks if the log has already been uploaded, whether it is eligible for upload,
     * and uploads it if necessary. If the log should not be uploaded, it marks it as ignored.
     *
     * **Behavior**:
     * - On the **first run** (`firstTime` is `true`):
     *   - Retrieves call logs from the last hour.
     * - On **subsequent runs** (`firstTime` is `false`):
     *   - Retrieves the last 5 call logs.
     * - Updates `firstTime` to `false` after the first execution.
     *
     * **Process Flow**:
     * 1. Queries the call logs using `queryRecentCallLogs()`.
     * 2. Iterates over each call log retrieved from the cursor.
     * 3. Checks if the call log has already been uploaded.
     * 4. If not uploaded:
     *    - Checks if the call log should be uploaded using `checkIfThisCallShouldBeUploaded()`.
     *    - If the result indicates that the call should be uploaded, uploads the log.
     *    - If not, marks the call log as ignored.
     *
     * **Parameters**:
     * - This function does not accept any parameters, but it utilizes:
     *   - `firstTime` (Boolean): Determines if it is the first run to decide whether to query call logs from the last hour or all available logs.
     *   - `dataStoreRepo.readDataStoreValue` to get the `publicApiKey`.
     *
     * **Returns**:
     * - This is a `suspend` function and does not return any value. It performs actions such as uploading call logs, updating the database, and logging errors.
     *
     * **Notes**:
     * - This function runs with `Dispatchers.IO` for background thread management, given that it handles database queries and network calls.
     * - Call logs are uploaded only if they haven't been uploaded before.
     * - If the call log is deemed ineligible for upload, it will be marked as ignored in the database.
     * - Exceptions encountered during processing each call log are logged for future reference.
     */
    private suspend fun uploadCallLogs() = withContext(Dispatchers.IO) {
        // Calculate the time one hour ago
        val oneHourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)


        // Retrieve the public API key from the DataStore
        val publicApiKey = dataStoreRepo.readDataStoreValue.first().apiKey

        val cursor =   queryRecentCallLogs(oneHourAgo)

        // Query recent call logs based on the value of firstTime
//        val cursor = if (firstTime) {
//            queryRecentCallLogs(oneHourAgo)  // Get call logs from the last hour if it's the first run
//        } else {
//            queryRecentCallLogs(fiveMinutesAgo)  // Get the last 5 call logs for subsequent runs
//        }

        // Update the value of firstTime after the first run
//        if (firstTime) {
//            firstTime = false
//        }

        // Process each call log retrieved from the cursor
        cursor?.use {
            while (cursor.moveToNext()) {
                try {
                    // Extract call log data
                    val callLogData = extractCallLogData(cursor)

                    // Generate a unique call ID based on call log details
                    val uniqueCallId = generateCallLogId(
                        callLogData.number,
                        callLogData.timestamp,
                        callLogData.type
                    )

                    // Get the DAO instance for uploaded call logs
                    val uploadedCallLogDao =
                        AppDatabase.getDatabase(applicationContext).uploadedCallLogDao()

                    // Check if this call log has already been uploaded
                    val isAlreadyUploaded =
                        uploadedCallLogDao.getUploadedCallLogById(uniqueCallId) != null

                    // If not uploaded, check if the call should be uploaded
                    if (!isAlreadyUploaded) {
                        val result = checkIfThisCallShouldBeUploaded(callLogData.cleanedPhoneNumber)
                        val shouldUpload = (result as? State.Success)?.data?.upload ?: false
                        val data = (result as? State.Success)?.data

                        if (data != null) {
                            if (shouldUpload) {
                                // Upload the call log
                                uploadCallLog(callLogData, publicApiKey, uniqueCallId, data)
                            } else {
                                // Mark the call log as ignored if it shouldn't be uploaded
                                markCallLogAsIgnore(
                                    uniqueCallId,
                                    callLogData.contactName,
                                    callLogData.callEndTimeMillis,
                                    callLogData
                                )
                            }
                        }
                    } else {
                        // Log the already uploaded call log
                        Log.d("TAG", "uploadCallLogs:Already uploaded:: ${callLogData.number} ")
                    }

                } catch (e: Exception) {
                    // Log any exceptions that occur during processing
                    logMessage("Error processing call log: ${e.message}")
                }
            }
        }
    }


    /**
     * Queries recent call logs either from the last specified milliseconds
     **/

    private fun queryRecentCallLogs(sinceMillis: Long): Cursor? {
        return contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            "${CallLog.Calls.DATE} >= ?",
            arrayOf(sinceMillis.toString()),
            "${CallLog.Calls.DATE} DESC"
        )
    }

    /**
     * Extracts call log data from a cursor.
     */
    private fun extractCallLogData(cursor: Cursor): CallLogData {
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.getDefault())

        // Retrieve column indexes with validation
        val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER).takeIf { it >= 0 }
        val durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION).takeIf { it >= 0 }
        val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE).takeIf { it >= 0 }
        val typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE).takeIf { it >= 0 }

        // Use default values or throw an exception if required columns are missing
        if (numberIndex == null || durationIndex == null || dateIndex == null || typeIndex == null) {
            throw IllegalArgumentException("Required column(s) missing in the call log query.")
        }

        // Retrieve values using the validated indexes
        val number = cursor.getString(numberIndex)
        val duration = cursor.getInt(durationIndex)
        val timestamp = cursor.getLong(dateIndex)
        val type = cursor.getInt(typeIndex)

        val callEndTimeMillis = timestamp + (duration * 1000)
        val cleanedPhoneNumber = cleanPhoneNumber(number)
        val dateTime = dateFormat.format(Date(timestamp))
        Log.d("TAG", "extractCallLogData:$dateTime ")
        val callType = getCallType(type)
        val contactName = getContactName(number)



        return CallLogData(
            number,
            duration,
            timestamp,
            type,
            callEndTimeMillis,
            cleanedPhoneNumber,
            dateTime,
            callType,
            contactName
        )
    }

    /**
     * Cleans a phone number by removing the country code if present.
     */
    private fun cleanPhoneNumber(number: String): String {
        val countryCode = "+91"
        return if (number.startsWith(countryCode)) number.removePrefix(countryCode) else number
    }

    private suspend fun shouldUploadCallLog(
        callLogData: CallLogData,
        uniqueCallId: String
    ): Boolean {
        return try {
            Log.d("TAG", "shouldUploadCallLog:#####################33 ")
            val uploadedCallLogDao =
                AppDatabase.getDatabase(applicationContext).uploadedCallLogDao()
            val isAlreadyUploaded = uploadedCallLogDao.getUploadedCallLogById(uniqueCallId) != null

            // Use the updated check function to get a single result
            val result = checkIfThisCallShouldBeUploaded(callLogData.cleanedPhoneNumber)

            val shouldUpload = (result as? State.Success)?.data?.upload ?: false
            Log.d("TAG", "shouldUploadCallLog:##################### shouldUpload:$shouldUpload ")
            Log.d(
                "TAG",
                "shouldUploadCallLog:##################### isAlreadyUploaded:$isAlreadyUploaded "
            )

            !isAlreadyUploaded && shouldUpload
        } catch (e: Exception) {
            Log.d("TAG", "shouldUploadCallLog:Exception:$e ")
            false // Return false or any other default value in case of an exception
        }
    }

    /**
     * Uploads a call log to the server.
     */
    private suspend fun uploadCallLog(
        callLogData: CallLogData,
        publicApiKey: String,
        uniqueCallId: String,
        dataLead: ShouldUpload

    ) {
        val payload = createPayload(callLogData)
        val encodedPayload = encodePayload(payload)

        if (encodedPayload != null) {
            val requestUrl =
                "$UPLOAD_URL_BASE&payload=$encodedPayload&public_api_key=$publicApiKey&app_key=$APP_KEY&grant_access=1"
            startUploadLogTask(
                requestUrl,
                callLogData,
                uniqueCallId,
                dataLead

            )
            // logToFile("Uploaded call log for ${callLogData.cleanedPhoneNumber}")
        } else {
            //    logToFile("Encoded payload is null for ${callLogData.cleanedPhoneNumber}")
        }
    }

    private suspend fun startUploadLogTask(
        requestUrl: String,
        callLogData: CallLogData,
        uniqueCallId: String,
        dataLead: ShouldUpload

    ) = withContext(Dispatchers.IO) {

        var urlConnection: HttpURLConnection? = null
        var reader: BufferedReader? = null

        try {
            val url = URL(requestUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"

            // Log the response code
            val responseCode = urlConnection.responseCode
            Log.i("SyncDataService", "Response Code: $responseCode")
            logMessage("startUploadLogTask - Response Code: $responseCode")


            //  logToFile("SyncDataService - startUploadLogTask - Response Code: $responseCode")

            // Determine the input stream based on the response code
            val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                urlConnection.inputStream
            } else {
                urlConnection.errorStream
            }

            // Read the response from the server
            reader = BufferedReader(InputStreamReader(inputStream))
            val response = StringBuilder()
            reader.useLines { lines -> lines.forEach { response.append(it) } }

            // Log the response content
            Log.i("SyncDataService", "Response: ${response.toString()}")

            // logToFile("SyncDataService - Response - ${response.toString()}")

            FirebaseCrashlytics.getInstance()
                .log("SyncDataService - startUploadLogTask - Response: ${response.toString()}")
            logMessage("startUploadLogTask - Response: ${response.toString()}")


            // Check for success message in JSON response
            if (isResponseSuccess(response.toString())) {
                Log.i("SyncDataService", "Response: Succccccesss")
                markCallLogAsUploaded(
                    uniqueCallId,
                    callLogData.contactName,
                    callLogData.timestamp,
                    callLogData.callEndTimeMillis,
                    callLogData.duration.toString(),
                    callLogData.callType,
                    dataLead.tagStudent,
                    callLogData.cleanedPhoneNumber,
                    callLogData.number,
                    dataLead.id,

                    )
            } else {
                Log.i("SyncDataService", "Response: Faaaaailed")
            }

        } catch (e: Exception) {
            // Handle exceptions and log them
            Log.e("SyncDataService", "Error: ${e.message}")
            FirebaseCrashlytics.getInstance()
                .log("SyncDataService - startUploadLogTask - Error: ${e.message}")
            //logToFile("startUploadLogTask - Error: ${e.message}")

        } finally {
            // Close resources
            urlConnection?.disconnect()
            reader?.close()
        }
    }

    /**
     * Marks a call log as uploaded in the database.
     */
    private suspend fun markCallLogAsUploaded(
        uniqueCallId: String,
        contactName: String,
        callStart: Long,
        callEndTimeMillis: Long,
        callDuration: String,
        callType: String,
        tag: String,
        cleanedNumber: String,
        number: String,
        leadId: String
    ) {
        val uploadedCallLogDao = AppDatabase.getDatabase(applicationContext).uploadedCallLogDao()
        uploadedCallLogDao.insert(
            UploadedCallLog(
                uniqueCallId,
                contactName,
                logsUploaded = true,
                fileUploaded = false,
                ignoreThis = false,
                callEndTimeStamp = callEndTimeMillis,
                callDuration = callDuration,
                callType = callType,
                isLead = tag,
                cleanedNumber = cleanedNumber,
                number = number,
                leadId = leadId,
                callStarted = callStart

            )
        )
        // logMessage("Marked call log as uploaded for $contactName")
    }


    /**
     * Marks a call log as uploaded in the database.
     */
    private suspend fun markCallLogAsIgnore(
        uniqueCallId: String,
        contactName: String,
        callEndTimeMillis: Long,
        callLogData: CallLogData,
    ) {
        val uploadedCallLogDao = AppDatabase.getDatabase(applicationContext).uploadedCallLogDao()
        uploadedCallLogDao.insert(
            UploadedCallLog(
                uniqueCallId,
                contactName,
                logsUploaded = true,
                fileUploaded = false,
                ignoreThis = true,
                callLogData.timestamp,
                callEndTimeMillis,
                callLogData.duration.toString(),
                callLogData.callType,
                "",
                callLogData.cleanedPhoneNumber,
                callLogData.number,
                "",


                )
        )
        // logMessage("Marked call log as uploaded for $contactName")
    }

    /**
     * Creates the JSON payload for the call log data.
     */
    private fun createPayload(callLogData: CallLogData): String {
        val logObject = JSONObject().apply {
            put("dateTime", callLogData.dateTime)
            put("duration", callLogData.duration)
            put("name", callLogData.contactName)
            put("phoneNumber", callLogData.cleanedPhoneNumber)
            put("rawType", callLogData.type)
            put("timestamp", callLogData.timestamp)
            put("type", callLogData.callType.uppercase(Locale.ROOT))
        }
        return "[${logObject}]"
    }

    /**
     * Data class for holding call log information.
     */
    data class CallLogData(
        val number: String,
        val duration: Int,
        val timestamp: Long,
        val type: Int,
        val callEndTimeMillis: Long,
        val cleanedPhoneNumber: String,
        val dateTime: String,
        val callType: String,
        val contactName: String
    )


    /**
     * Checks if the response indicates a success.
     */
    private fun isResponseSuccess(response: String): Boolean {
        return try {
            val jsonResponse = JSONObject(response)
            jsonResponse.has("Success")  // Checks if JSON contains "Success" key
        } catch (e: Exception) {
            false  // If parsing fails, assume the response is not successful
        }
    }


    /**
     * Logs error messages to Firebase Crashlytics for remote debugging.
     */
    private suspend fun logError(message: String) {
        FirebaseCrashlytics.getInstance().log("SyncDataService - $message")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * Enum to define possible service actions (START or STOP).
     */
    enum class Actions { START, STOP }


    /**
     * Checks with the server if a specific call log should be uploaded.
     * First, it performs a lead search, and if no leads are found, it searches for students.
     */
    private suspend fun checkIfThisCallShouldBeUploaded(phoneNumber: String): State<ShouldUpload> {
        val publicKey = dataStoreRepo.readDataStoreValue.first().apiKey

        // Step 1: Search for leads
        val searchLeadResponse = appRepository.search(publicKey, phoneNumber, "lead", "1")
        if (searchLeadResponse.isSuccessful) {
            val responseString = searchLeadResponse.body()?.string() ?: ""
            Log.d("SyncDataService", "search:leadResponseString:$responseString ")

            if (!responseString.contains("Developer Error")) {
                val leads = parseLeads(responseString)
                leads?.find { it.student_mobile == phoneNumber }?.let { lead ->
                    return State.Success(
                        ShouldUpload(
                            upload = true,
                            id = lead.id,
                            tagStudent = "lead"
                        )
                    )
                }
            }
        }

        // Step 2: Search for students if no lead found
        val searchStudentResponse = appRepository.search(publicKey, phoneNumber, "", "1")
        if (searchStudentResponse.isSuccessful) {
            val responseString = searchStudentResponse.body()?.string() ?: ""
            Log.d("SyncDataService", "search:studentResponseString:$responseString ")

            if (!responseString.contains("Developer Error")) {
                val students = parseLeads(responseString)
                students?.find { it.student_mobile == phoneNumber }?.let { student ->
                    return State.Success(
                        ShouldUpload(
                            upload = true,
                            id = student.id,
                            tagStudent = ""
                        )
                    )
                }
            }
        }

        // No matching lead or student found
        return State.Success(ShouldUpload(upload = false, id = "", tagStudent = ""))
    }

    /**
     * Safely retrieves a string value from a cursor at the specified column index.
     */
    private fun Cursor.getStringOrNull(columnIndex: Int): String? {
        return if (columnIndex >= 0 && !isNull(columnIndex)) getString(columnIndex) else null
    }

    /**
     * Logs messages for tracking sync progress. Uses FirebaseCrashlytics and optionally saves locally.
     */
    private suspend fun logMessage(message: String) {
        Log.d("TAG", "logMessage:message$message ")
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val logEntry = "$timestamp - $message\n"

        // Log to Crashlytics for centralized logging
        FirebaseCrashlytics.getInstance().log("SyncDataService - $message")

        Log.d("SyncDataService", "logMessage:$message ")

        // Optionally save to a local log file (requires additional file setup)
        logToFile(logEntry)
    }

    private var logFileUri: Uri? = null
    private var logfilePath: String? = null

    /**
     * Creates or retrieves a log file URI in the Downloads directory.
     * Ensures that the file exists before attempting to log.
     */

    private suspend fun createLogFile() {
        val fileName = "sync_log_${
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(Date())
        }.txt"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android Q and above: Use MediaStore to create the file in the Downloads directory
            val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)

            val queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            contentResolver.query(queryUri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        // File already exists; get its URI
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                        val id = cursor.getLong(idColumn)
                        logFileUri = ContentUris.withAppendedId(queryUri, id)
                        Log.d("LogService", "Log file already exists at: $logFileUri")

                        // Update the existing file content
                        logFileUri?.let { uri ->
                            contentResolver.openOutputStream(uri, "wa")?.use { outputStream ->
                                outputStream.write("\nUpdated log at: ${Date()}".toByteArray())
                                Log.d("LogService", "Log file updated at: $uri")
                            } ?: Log.e("LogService", "Failed to update log file.")
                        }
                    } else {
                        // File does not exist; create it
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        logFileUri = contentResolver.insert(queryUri, contentValues)
                        logFileUri?.let { uri ->
                            contentResolver.openOutputStream(uri)?.use { outputStream ->
                                outputStream.write("Log created at: ${Date()}".toByteArray())
                                Log.d("LogService", "Log file created at: $uri")
                            } ?: Log.e("LogService", "Failed to create log file.")
                        }
                    }
                } ?: Log.e("LogService", "Failed to query the Downloads directory.")
        } else {
            // Below Android Q: Use File API to create the log file in the Downloads directory
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val logFile = File(downloadsDir, fileName)

            if (logFile.exists()) {
                Log.d("LogService", "Log file already exists at: ${logFile.absolutePath}")
                try {
                    logFile.appendText("\nUpdated log at: ${Date()}")
                    Log.d("LogService", "Log file updated at: ${logFile.absolutePath}")
                    logfilePath = logFile.absolutePath
                } catch (e: IOException) {
                    Log.e("LogService", "Failed to update log file: ${e.message}")
                }
            } else {
                try {
                    logFile.writeText("Log created at: ${Date()}")
                    Log.d("LogService", "Log file created at: ${logFile.absolutePath}")
                } catch (e: IOException) {
                    Log.e("LogService", "Failed to create log file: ${e.message}")
                }
            }
        }
    }

    /**
     * Logs a message to the log file, appending it to the existing content.
     */


    // Helper function to check if the file exists at the given URI
    private fun isLogFilePresent(uri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { it.read() }
            true // File exists if no exception is thrown
        } catch (e: FileNotFoundException) {
            false // File does not exist
        } catch (e: IOException) {
            Log.e("LogService", "Error checking log file presence: ${e.message}")
            false
        }
    }

    // Helper function to check if the file exists at the specified file path
    private fun isLogFilePathPresent(path: String): Boolean {
        return File(path).exists() // Returns true if file exists at the given path, false otherwise
    }

    private suspend fun logToFile(message: String) {


        // Check for Android version to handle URI-based access (API 29+) or file path access (below API 29)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            // Ensure the log file URI is created and accessible
            if (logFileUri == null || !isLogFilePresent(logFileUri!!)) {
                createLogFile() // Attempt to create the log file if it does not exist
            }
            logFileUri?.let { uri ->
                try {
                    // Open an output stream to the log file in "wa" mode (append mode)
                    contentResolver.openOutputStream(uri, "wa")?.use { outputStream ->
                        // Write the message to the log file with a newline
                        outputStream.write((message + "\n").toByteArray())
                        Log.d("LogService", "Logged message: $message")
                    }
                } catch (e: IOException) {
                    // Log any I/O exceptions that occur during file writing
                    Log.e("LogService", "Error writing to log file: ${e.message}")
                }
            }
        } else {
            // Handle logging for devices running below Android Q (API 29)
            // Use the absolute path stored in `logFilePath` to access the log file
            if (logfilePath == null || !isLogFilePathPresent(logfilePath!!)) {
                createLogFile() // Attempt to create the log file if it does not exist
            }
            logfilePath?.let { path ->
                try {
                    // Append the message to the file at the specified path
                    File(path).appendText(message + "\n")
                    Log.d("LogService", "Logged message to path: $message")
                } catch (e: IOException) {
                    // Log any I/O exceptions that occur during file writing
                    Log.e("LogService", "Error writing to log file path: ${e.message}")
                }
            }
        }

    }

    /**
     * Generates a unique ID for each call log based on phone number, timestamp, and call type.
     */
    private fun generateCallLogId(phoneNumber: String, timestamp: Long, type: Int): String {
        return phoneNumber + "_" + timestamp + "_" + type
    }


    private fun getCallType(type: Int): String {
        return when (type) {
            CallLog.Calls.INCOMING_TYPE -> "INCOMING"
            CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
            CallLog.Calls.MISSED_TYPE -> "MISSED"
            CallLog.Calls.REJECTED_TYPE -> "REJECTED"
            else -> "UNKNOWN"
        }
    }

    private fun getContactName(phoneNumber: String): String {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val contentResolver: ContentResolver = contentResolver
        val contactLookup = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )

        var result: String? = null
        contactLookup?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (nameIndex != -1) {
                    result = cursor.getString(nameIndex)
                }
            }
        } ?: Log.e("getContactName", "Cursor is null")

        return result ?: phoneNumber
    }

    private fun encodePayload(payload: String): String? {
        return try {
            URLEncoder.encode(payload, "UTF-8").replace("\\+".toRegex(), "%20")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            null
        }
    }

    // MutableMap to track the number of attempts for each file


    /**
     * Main function to process new recording files. Triggered whenever a new file is added to the folder.
     */
    private suspend fun processNewRecordingFile(filePath: String) {
        val file = File(filePath)

        // Check if the file exists, retrying after a short delay if necessary
        if (!file.exists()) {
            logMessage("File does not exist at path: $filePath. Retrying...")
            delay(1000) // Retry after a short delay
            if (!file.exists()) {
                logMessage("File still not found after retry: $filePath")
                return
            }
        }

        // Check if the file is still being written to by another process
        if (isFileBeingWritten(file)) {
            logMessage("File is still being written to: ${file.name}. Delaying processing...")
            delay(1000) // Delay to allow recording to complete
            if (isFileBeingWritten(file)) {
                logMessage("File is still being written to after delay: ${file.name}. Aborting processing for now.")
                return
            }
        }
        //Directly  upload file to server
        uploadFileWithDummyData(file) // Upload file with dummy data if no timestamp is found


        //UNCOMMENT TO CHECK IN DEVICE

        // Extract the timestamp from the filename
//        val timestamp = extractTimestampFromFileName(file.name) ?: run {
//            logMessage("No valid timestamp found in filename: ${file.name}. Uploading with dummy data.")
//            uploadFileWithDummyData(file) // Upload file with dummy data if no timestamp is found
//            return
//        }
//        Log.d("TAG", "processNewRecordingFile:extractTimestampFromFileName::$timestamp ")

        // Find a matching call log from the database
        //     var matchingCallLog = findMatchingCallLog(timestamp, file)
//        if (matchingCallLog != null) {
//            logMessage("Matching call log found for timestamp matchingCallLog:: $matchingCallLog")
//            // If a match is found, upload the file with call log data
//            val success = uploadFileWithCallLogData(file, matchingCallLog)
//            if (success) {
//                markCallLogAsUploaded(matchingCallLog)
//                moveToUploadedCallLogs(filePath) // Move file only after successful upload
//            } else {
//                logMessage("Failed to upload file: $filePath with matched call log data. Retrying...")
//                retryUpload(file, matchingCallLog) // Retry upload if initial upload fails
//            }
//        } else {
//            // Retry mechanism for call log match to handle late addition of logs
//            logMessage("No matching call log found for timestamp. Retrying after delay of 10 seconds...")
//            delay(10000) // Delay to allow for possible call log update
////            matchingCallLog = findMatchingCallLog(timestamp, file)
////            if (matchingCallLog != null) {
////                logMessage("Matching call log found on retry.")
////                val success = uploadFileWithCallLogData(file, matchingCallLog)
////                if (success) {
////                    markCallLogAsUploaded(matchingCallLog)
////                    moveToUploadedCallLogs(filePath)
////                } else {
////                    logMessage("Failed to upload file: $filePath with matched call log data. Retrying...")
////                    retryUpload(file, matchingCallLog) // Retry upload if initial upload fails
////                }
////            } else {
////                logMessage("No matching call log found after retry. Uploading with dummy data.")
////                // Check and update the attempt count for the file
////                val attempts = fileAttemptMap.getOrDefault(file.name, 0) + 1
////                fileAttemptMap[file.name] = attempts
////
////                if (attempts > MAX_ATTEMPTS) {
////                    logMessage("Exceeded maximum attempts for file: $filePath. Uploading with dummy data.")
////                    uploadFileWithDummyData(file)
////                    fileAttemptMap.remove(file.name) // Clean up the map
////                    return
////                }
////
////            }
//        }


    }

    private suspend fun markCallLogAsUploaded(matchingCallLog: UploadedCallLog) {
        Log.d("TAG", "markCallLogAsUploaded:matchingCallLog::$matchingCallLog ")
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.uploadedCallLogDao()
        dao.updateFileUploadStatus(
            matchingCallLog.id, true, matchingCallLog.fileUploadAttempts + 1, ""
        )

    }

    /**
     * Checks if the file is still being written to by another process.
     */
    private fun isFileBeingWritten(file: File): Boolean {
        if (!file.exists()) {
            Log.e("TAG", "File does not exist.")
            return false
        }

        val initialSize = file.length()
        Thread.sleep(500) // Wait for a short time interval (e.g., 500 milliseconds)
        val newSize = file.length()

        val isBeingWritten = initialSize != newSize
        Log.d(
            "TAG",
            "isFileBeingWritten: $isBeingWritten (Initial size: $initialSize, New size: $newSize)"
        )
        return isBeingWritten
    }

    /**
     * Extracts timestamp from the filename in either YYYYMMDDHHMMSS, YYMMDDHHMMSS, or YYMMDDHHMM format.
     */
    private suspend fun extractTimestampFromFileName(fileName: String): Long? {
        // Regex to match timestamp after the last '-' or '_'
        val timestampRegex =
            Regex("[-_](\\d{10,14})") // Matches valid timestamp patterns (10-14 digits after '-' or '_')
        val matchResult = timestampRegex.find(fileName)
        val timestampString = matchResult?.groupValues?.get(1) ?: return null
        Log.d("TAG", "extractTimestampFromFileName:timestampString::$timestampString ")

        // Determine the date format based on the length of the timestamp
        val dateFormat = when (timestampString.length) {
            14 -> SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()) // YYYYMMDDHHMMSS
            12 -> SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())   // YYMMDDHHMMSS
            10 -> SimpleDateFormat("yyMMddHHmm", Locale.getDefault())     // YYMMDDHHMM
            else -> return null
        }

        // Set the date format to the device's local timezone
        dateFormat.timeZone = TimeZone.getDefault()

        return try {
            // Parse the date in local timezone
            val localDate = dateFormat.parse(timestampString) ?: return null
            Log.d("TAG", "extractTimestampFromFileName:localDate::$localDate ")

            // Convert the local date to a timestamp in milliseconds since epoch (in UTC)
            localDate.time
        } catch (e: Exception) {
            Log.e("TAG", "Error parsing timestamp from filename: ${e.message}")
            null
        }
    }


    /**
     * Finds the closest matching call log in the Room database based on the timestamp and duration.
     */
    private suspend fun findMatchingCallLog(timestamp: Long, file: File): UploadedCallLog? {
        logMessage("findMatchingCallLog - Timestamp: $timestamp, Filepath: ${file.absolutePath}")

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.uploadedCallLogDao()

        // Get file duration and round to seconds
        val fileDuration = getAudioFileDuration(applicationContext, file) ?: return null
        val fileDurationSeconds = Math.round(fileDuration / 1000.0).toInt()
        Log.d("findMatchingCallLog", "File Duration (seconds): $fileDurationSeconds")

        // Extract name and phone number from file name
        val extractedData = extractNameAndPhoneNumber(file.name)
        Log.d("findMatchingCallLog", "Extracted Data: $extractedData")

        // Check if name and phone number are both null
        if (extractedData.name == null && extractedData.cleanedPhoneNumber == null) {
            return null
        }

        // Find the matching call log based on available data
        return when {
            extractedData.cleanedPhoneNumber != null -> {
                dao.findClosestMatchingCallLogNumber(
                    timestamp,
                    fileDurationSeconds,
                    extractedData.cleanedPhoneNumber
                )
            }

            extractedData.name != null -> {
                dao.findClosestMatchingCallLogName(
                    timestamp,
                    fileDurationSeconds,
                    extractedData.name
                )
            }

            else -> null
        }
    }


    /**
     * Model ExtractedData
     */
    data class ExtractedData(
        val name: String?,
        val cleanedPhoneNumber: String?
    )


    /**
     * Extract Name and number from file name
     */

    private fun extractNameAndPhoneNumber(fileName: String): ExtractedData {
        // Remove the file extension
        val baseName = fileName.substringBeforeLast(".mp3")

        // Split by common delimiters to isolate components
        val parts = baseName.split("-", "_", "(", ")")

        var name: String? = null
        var phoneNumber: String? = null

        for (part in parts) {
            val trimmedPart = part.trim()

            // Check if the part is a timestamp (exactly 10 digits in `YYMMDDHHmm` format)
            if (trimmedPart.all { it.isDigit() } && trimmedPart.length == 10) {
                continue // Ignore timestamps
            }
            // Check if the part is a valid phone number
            else if (trimmedPart.startsWith("+") || (trimmedPart.all { it.isDigit() } && trimmedPart.length in 7..15)) {
                phoneNumber = trimmedPart
            }
            // Otherwise, treat it as part of the name
            else if (trimmedPart.isNotEmpty()) {
                name = name?.let { "$it $trimmedPart" } ?: trimmedPart
            }
        }

        return ExtractedData(
            name = name?.takeIf { it.isNotBlank() },
            cleanedPhoneNumber = phoneNumber?.takeIf { it.isNotBlank() }
        )
    }


    /**
     * Retrieves the audio file duration in milliseconds using MediaMetadataRetriever.
     */
    private suspend fun getAudioFileDuration(context: Context, file: File): Int? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.fromFile(file))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
        } catch (e: Exception) {
            logMessage("Error retrieving file duration: ${e.message}")
            null
        } finally {
            retriever.release() // Ensure resources are released
        }
    }

    /**
     * Uploads the file with matching call log data and adds audio details.
     */
    private suspend fun uploadFileWithCallLogData(file: File, callLog: UploadedCallLog): Boolean {
        val url = "https://$fileUploadDomain/gallery/receiver?app_key=$APP_KEY"

        // Extract common fields to reduce redundancy
        val cleanedNumber = callLog.cleanedNumber
        val fromId = callLog.leadId
        val callDuration = callLog.callDuration.toIntOrNull() ?: 0
        val recordedDateTime = formatTimestamp(callLog.callStarted)
        val attempts = callLog.fileUploadAttempts
        val callLogsId = callLog.id
        val callType = callLog.callType
        val studentTag = callLog.isLead
        val filePath = file.absolutePath

        // Upload the file
        val uploadedFilePath = uploadFile(
            filePath = filePath,
            base = url,
        )

        if (uploadedFilePath != null) {

            return addAudioDetails(
                fileName = uploadedFilePath,
                fromId = fromId,
                studentTag = studentTag,
                callDuration = callDuration,
                recordStatus = callType,
                recordedDateTime = recordedDateTime,
                attempts = attempts,
                callLogsId = callLogsId,

                )

        } else {
            logMessage("Uploaded FileName ull")
            return false
        }


    }

    /**
     * Uploads the file with dummy or default data if no match is found.
     */
    private suspend fun uploadFileWithDummyData(file: File) {
        try {
            logMessage("Uploading file with dummy data: ${file.name}")
            val url = "https://$fileUploadDomain/gallery/receiver?app_key=$APP_KEY"

            val fileDuration = getAudioFileDuration(applicationContext, file) ?: 0

            Log.d("TAG", "findMatchingCallLog:fileDuration::$fileDuration ")
            val seconds = Math.round(fileDuration / 1000.0).toInt()

            val uploadedFilePath = uploadFile(
                filePath = file.absolutePath,
                base = url,

                )


            if (uploadedFilePath != null) {

                val detailsAdded = addAudioDetails(
                    fileName = uploadedFilePath,
                    fromId = "",
                    studentTag = "",
                    callDuration = seconds,
                    recordStatus = "Unknown",
                    recordedDateTime = getFileCreatedDateString(file),
                    attempts = 0,
                    callLogsId = "",

                    )

                if (detailsAdded) {
                    moveToUploadedCallLogs(file.absolutePath)
                } else {
                    logMessage("Unable to add Audio Details")
                }

            } else {
                logMessage("Uploaded FileName null")

            }
        } catch (e: Exception) {
            logMessage("uploadFileWithDummyData:Failed::${e.message} ")
        }

    }


    private fun getFileCreatedDateString(file: File): String {
        return if (file.exists()) {
            val lastModifiedMillis = file.lastModified()
            val dateFormat = SimpleDateFormat("HH:mm dd-MM-yyyy", Locale.getDefault())
            dateFormat.format(Date(lastModifiedMillis))
        } else {
            "Unknown"
        }
    }

    /**
     * Moves the file to the UploadedCallLogs folder.
     */
    private suspend fun moveToUploadedCallLogs(filePath: String): Boolean {
        val file = File(filePath)
        val parentDir = file.parentFile?.parentFile ?: return false
        val uploadedCallLogsDir = File(parentDir, "UploadedCallLogs")

        if (!uploadedCallLogsDir.exists() && !uploadedCallLogsDir.mkdirs()) {
            logMessage("Failed to create UploadedCallLogs directory.")
            return false
        }

        val newFilePath = File(uploadedCallLogsDir, file.name)
        return try {
            file.copyTo(newFilePath, overwrite = true)
            file.delete()
            logMessage("File moved successfully to UploadedCallLogs: ${file.name}")
            true
        } catch (e: IOException) {
            logMessage("Error moving file: ${e.message}")
            false
        }
    }

    /**
     * Ensures the recording folder is always empty, with error handling for any deletion failures.
     */
    private suspend fun cleanRecordingFolderSafely() {
        val recordingFolder = File("path/to/recording/folder")
        recordingFolder.listFiles()?.forEach { file ->
            try {
                file.deleteRecursively()
                logMessage("Deleted file: ${file.name}")
            } catch (e: Exception) {
                logMessage("Error deleting file ${file.name}: ${e.message}")
            }
        }
    }

    /**
     * Retries the upload of a file multiple times if initial upload fails.
     */
    private suspend fun retryUpload(file: File, callLog: UploadedCallLog) {
        repeat(3) { attempt ->
            logMessage("Retrying upload for file: ${file.name}, attempt: ${attempt + 1}")
            val success = uploadFileWithCallLogData(file, callLog)
            if (success) {
                markCallLogAsUploaded(callLog)
                moveToUploadedCallLogs(file.absolutePath)
                return
            }
            delay(1000) // Delay between retries
        }
        logMessage("All retry attempts failed for file: ${file.name}")
    }


    private suspend fun uploadFile(
        filePath: String,
        base: String,

        ): String? {

        val client = OkHttpClient()
        val file = File(filePath)

        // Check if the file exists before proceeding
        if (!file.exists()) {
            logMessage("File does not exist: $filePath")
            return null
        }

        // Create request body with multipart data
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "sendfile",
                file.name,
                file.asRequestBody("audio/*".toMediaTypeOrNull())
            )

            .build()

        // Build request
        val request = Request.Builder()
            .url(base)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logMessage("Failed to upload file: ${response.code}")
                    return null
                }

                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val jsonArray = JSONArray(responseBody)
                        val jsonObject = jsonArray.getJSONObject(0)
                        jsonObject.getString("filename")
                    } catch (e: Exception) {
                        logMessage("Error parsing response JSON: ${e.message}")
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: IOException) {
            logMessage("IOException during file upload: ${e.message}")
            null
        }
    }


    private suspend fun addAudioDetails(
        fileName: String,
        fromId: String,
        studentTag: String,
        callDuration: Int,
        recordStatus: String,
        recordedDateTime: String,
        attempts: Int,
        callLogsId: String,

        ): Boolean {
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.uploadedCallLogDao()

        val call = appRepository.uploadAudioFileDetails(
            publicApiKey = dataStoreRepo.readDataStoreValue.first().apiKey,
            userId = fromId,
            tagStudent = studentTag,
            fromId = dataStoreRepo.readDataStoreValue.first().userId.toInt(),
            fileName = fileName,
            callDuration = callDuration,
            recordStatus = recordStatus,
            recordedDateTime = recordedDateTime
        )

// Log the full URL
        val url = call.request().url
        logMessage("####addAudioDetails - url ::$url ")

// Execute the call
        val response = call.execute()


        logMessage("####addAudioDetails - response ::${response.body()} ")

        return if (response.isSuccessful) {
            dao.updateFileUploadStatus(callLogsId, true, attempts + 1, "")
            logMessage("Audio details added successfully for file: $fileName")
            true
        } else {
            dao.updateFileUploadAttemptsAndError(
                callLogsId,
                attempts + 1,
                "Failed to add audio details: ${response.message()}"
            )
            logMessage("Failed to add audio details: ${response.message()}")
            false
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }


}



