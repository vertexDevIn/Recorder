package `in`.vertexdev.mobile.call_rec.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.models.ChoiceItem
import `in`.vertexdev.mobile.call_rec.models.Items
import `in`.vertexdev.mobile.call_rec.models.Lead
import `in`.vertexdev.mobile.call_rec.models.LeadCategory
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

object Utils {

    fun Context.toast(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()


    fun showCustomToast(message: String, context:Context) {
        Handler(Looper.getMainLooper()).post {
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.toast_layout, null)

            // Find the TextView and set the long message
            val toastText = layout.findViewById<TextView>(R.id.toast_text)
            toastText.text = message

            // Create the Toast with the custom layout
            val toast = Toast(context)
            toast.duration = Toast.LENGTH_SHORT
            toast.view = layout
            toast.show()
        }
    }

    /**
     * Checks if all permissions in the provided array are granted.
     *
     * @param context The context from which to get the permission status.
     * @param permissions An array of permission strings to check.
     * @return True if all permissions are granted, false otherwise.
     */
    fun areAllPermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun generateDummyItems(): List<Items> {
        // Define the titles
        val titles = listOf(
            "High Potential",
            " Newly Added",
            "Important",
            "Follow Up",
            "Closed",
            "Not Interested",
            "Not Answered",
            "Raw Leads",
            "WaitingList",
            "Interested",
            "Withdrawn",
            "Deferred",
            "Walk In",
            "Call",
            "Bharat",
            "Bharath",
            "Soon",
            "Call Me Back",
            "Call ASAP",
            "Important Candidate",
            "Dilip",
            "Color Black",
            "Color White"
        )

        // Generate a list of Items with dummy data
        return titles.mapIndexed { index, title ->
            Items(
                id = "item_$index", // Generate a dummy id
                title = title,
                value = "0" // Generate a dummy value based on the title
            )
        }
    }

    suspend fun pickDateRange(
        context: Context,
        dateFormat: String = "dd/MM/yyyy" // Default date format parameter
    ): Pair<String, String>? = suspendCancellableCoroutine { continuation ->

        // Create calendar constraints allowing dates until today
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())

        // Create a date range picker
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        // Show the date range picker
        dateRangePicker.show(
            (context as AppCompatActivity).supportFragmentManager,
            "DATE_RANGE_PICKER"
        )

        // Flag to ensure continuation is resumed only once
        var isResumed = false

        // Handle positive button click
        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            if (!isResumed) {
                val startDate = selection.first
                val endDate = selection.second

                val format =
                    SimpleDateFormat(dateFormat, Locale.ENGLISH) // Use the provided date format

                val startDateStr = format.format(Date(startDate))
                val endDateStr = format.format(Date(endDate))

                // Resume the continuation with the selected date range
                continuation.resume(Pair(startDateStr, endDateStr))
                isResumed = true
            }
        }

        // Handle cancellation
        dateRangePicker.addOnCancelListener {
            if (!isResumed) {
                continuation.resume(null)
                isResumed = true
            }
        }

        // Handle dismiss
        dateRangePicker.addOnDismissListener {
            if (!isResumed) {
                continuation.resume(null)
                isResumed = true
            }
        }
    }

    // Function to pick a single date
    suspend fun pickSingleDate(context: Context): String? =
        suspendCancellableCoroutine { continuation ->

            // Create calendar constraints allowing dates until today
//            val constraintsBuilder = CalendarConstraints.Builder()
//                .setValidator(DateValidatorPointBackward.now())

            // Create a single date picker
            val singleDatePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                //.setCalendarConstraints(constraintsBuilder.build())
                .build()

            // Show the single date picker
            singleDatePicker.show(
                (context as AppCompatActivity).supportFragmentManager,
                "SINGLE_DATE_PICKER"
            )

            // Use a flag to track whether the coroutine has been resumed
            var resumed = false

            // Handle positive button click
            singleDatePicker.addOnPositiveButtonClickListener { selection ->
                if (!resumed) {
                    val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    val selectedDateStr = format.format(Date(selection))

                    // Resume the continuation with the selected date
                    continuation.resume(selectedDateStr)
                    resumed = true
                }
            }

            // Handle cancellation
            singleDatePicker.addOnCancelListener {
                if (!resumed) {
                    continuation.resume(null)
                    resumed = true
                }
            }

            // Handle dismissal
            singleDatePicker.addOnDismissListener {
                if (!resumed) {
                    continuation.resume(null)
                    resumed = true
                }
            }
        }



    // Function to pick a single time
    suspend fun pickSingleTime(context: Context): String? =
        suspendCancellableCoroutine { continuation ->

            // Create a TimePicker
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H) // Use 24-hour format
                .setHour(12) // Default hour
                .setMinute(0) // Default minute
                .setTitleText("Select Time")
                .build()

            // Show the TimePicker
            timePicker.show(
                (context as AppCompatActivity).supportFragmentManager,
                "SINGLE_TIME_PICKER"
            )

            // Use a flag to track whether the coroutine has been resumed
            var resumed = false

            // Handle positive button click
            timePicker.addOnPositiveButtonClickListener {
                if (!resumed) {
                    // Get the selected hour and minute
                    val selectedHour = timePicker.hour
                    val selectedMinute = timePicker.minute

                    // Format the time as hh:mm
                    val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)

                    // Return the selected time as a string
                    continuation.resume(formattedTime)
                    resumed = true
                }
            }

            // Handle cancellation
            timePicker.addOnCancelListener {
                if (!resumed) {
                    continuation.resume(null)
                    resumed = true
                }
            }

            // Handle dismiss
            timePicker.addOnDismissListener {
                if (!resumed) {
                    continuation.resume(null)
                    resumed = true
                }
            }
        }




    // Extension function to sort a list of LeadCategory by name
    fun List<LeadCategory>.sortedByName(): List<LeadCategory> {
        return this.sortedBy { it.name }
    }

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = manager.getRunningServices(Integer.MAX_VALUE)
        for (service in services) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }


    /**
     * Reusable function to show a single-choice dialog.
     * @param items List of ChoiceItem with ID and text
     * @param onItemSelected Callback function to handle the selected item
     */
    private fun showSingleChoiceDialog(
        context: Context,
        title: String,
        positiveText: String,
        negativeText: String,
        items: List<ChoiceItem>,
        onItemSelected: (ChoiceItem) -> Unit // Callback function
    ) {
        var selectedItemIndex = 0 // Default selected index
        val itemTexts = items.map { it.text }.toTypedArray() // Extract text for display in dialog

        AlertDialog.Builder(context)
            .setTitle(title)
            .setSingleChoiceItems(itemTexts, selectedItemIndex) { _, which ->
                selectedItemIndex = which // Update the selected item index
            }
            .setPositiveButton(positiveText) { dialog, _ ->
                // Pass the selected item to the callback
                val selectedItem = items[selectedItemIndex]
                onItemSelected(selectedItem)
                dialog.dismiss()
            }
            .setNegativeButton(negativeText) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun String.toListOfStrings(): List<String> {
        return this.split(",").map { it.trim() }
    }

     fun parseLeads(jsonString: String): List<Lead>? {
        // Remove the {"bbids":"..."} block from the JSON string
        var modifiedJsonString = jsonString.replace(Regex("\\{\"bbids\":\"[^\"]*\"\\}"), "")

        // Remove any trailing commas before the closing square bracket
        modifiedJsonString = modifiedJsonString.replace(Regex(",\\s*(\\]|\\})"), "$1")

        // Log the modified JSON for debugging
        Log.d("TAG", "parseLeads: $modifiedJsonString")

        // Now, parse the modified JSON string to a list of Lead objects
        val gson = Gson()
        val leadListType = object : TypeToken<List<Lead>>() {}.type
        return gson.fromJson(modifiedJsonString, leadListType)
    }
    fun extractCountryCode(country: String): String {
        return country.replace(Regex("[^0-9]"), "") // Keep only the digits
    }


}

