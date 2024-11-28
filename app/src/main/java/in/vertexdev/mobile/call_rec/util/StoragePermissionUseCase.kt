package `in`.vertexdev.mobile.call_rec.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class StoragePermissionUseCase(
    private val context: Context,
    private val requestPermissionsLauncher: ActivityResultLauncher<Array<String>>,

) {

    // Function to check and request storage permissions
    fun checkAndRequestPermissions(
        packageName: String,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above, request MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                onPermissionDenied()
                requestManageExternalStoragePermission(packageName)
            } else {
                onPermissionGranted()
            }
        } else {
            // For Android 10 and below, request READ/WRITE_EXTERNAL_STORAGE
            val readPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val writePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            if (readPermission == PackageManager.PERMISSION_GRANTED &&
                writePermission == PackageManager.PERMISSION_GRANTED
            ) {
                // Permissions are granted
                onPermissionGranted()
            } else {
                // Request permissions
                onPermissionDenied()
                requestPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    // Function to request MANAGE_EXTERNAL_STORAGE permission for Android 11+
    @SuppressLint("QueryPermissionsNeeded")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestManageExternalStoragePermission(packageName: String) {
        Log.d("TAG", "requestManageExternalStoragePermission:$packageName ")
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            // Use startActivity(intent) directly
            context.startActivity(intent)
        } else {
            // Fallback to app settings if the intent is not available
            openAppSettings(packageName)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun openAppSettings(packageName: String) {
        val appSettingsIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        if (appSettingsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(appSettingsIntent)
            Toast.makeText(context, "Please grant storage permissions manually.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Unable to open app settings.", Toast.LENGTH_LONG).show()
        }
    }
}
