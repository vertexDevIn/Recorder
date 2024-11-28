package `in`.vertexdev.mobile.call_rec.ui.frags.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.databinding.FragmentSelectFolderBinding
import `in`.vertexdev.mobile.call_rec.util.State
import `in`.vertexdev.mobile.call_rec.util.StoragePermissionUseCase
import `in`.vertexdev.mobile.call_rec.util.Utils.toast
import `in`.vertexdev.mobile.call_rec.viewModel.AuthViewModel
import kotlinx.coroutines.launch


class SelectFolderFragment : Fragment() {

    private var _binding: FragmentSelectFolderBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()


    private lateinit var storagePermissionUseCase: StoragePermissionUseCase

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.entries.all { it.value }
            if (allPermissionsGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        _binding = FragmentSelectFolderBinding.inflate(inflater, container, false)



        binding.buttonGrantAll.setOnClickListener {
            // pickFolder()
            checkForPermission()
        }

        return binding.root
    }



    override fun onResume() {
        super.onResume()
        checkIfGranted()
    }

    private fun checkIfGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                //Navigate
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permissions are granted
                    //Navigate
                    onPermissionGranted()

                }

                else -> { //do not navigate

                    onPermissionDenied()
                }
            }
        }
    }





    private fun checkForPermission() {
        // Initialize StoragePermissionUseCase with the necessary parameters
        storagePermissionUseCase = StoragePermissionUseCase(
            context = requireContext(),
            requestPermissionsLauncher = requestPermissionsLauncher,

            )
        // Get the package name and call the permission use case
        val packageName = requireActivity().packageName

        // Call the storage permission use case
        storagePermissionUseCase.checkAndRequestPermissions(
            packageName = packageName,
            onPermissionGranted = ::onPermissionGranted,
            onPermissionDenied = ::onPermissionDenied
        )

    }

    private fun onPermissionGranted() {
        // Perform any action after permission is granted
        requireContext().toast("Permission Granted")
        findNavController().navigate(R.id.action_selectFolderFragment_to_folderListFragment)
    }

    private fun onPermissionDenied() {
        // Handle the case when permission is denied
        requireContext().toast("Permission Not Granted")
    }


}