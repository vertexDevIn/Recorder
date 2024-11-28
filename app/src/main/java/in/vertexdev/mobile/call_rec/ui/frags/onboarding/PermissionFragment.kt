package `in`.vertexdev.mobile.call_rec.ui.frags.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.databinding.FragmentPermissionBinding
import `in`.vertexdev.mobile.call_rec.viewModel.AuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class PermissionFragment : Fragment() {

    private var _binding: FragmentPermissionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()


    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        // Inflate the layout for this fragment
        _binding = FragmentPermissionBinding.inflate(inflater, container, false)
        // Initialize the launcher for requesting permissions

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions[android.Manifest.permission.READ_PHONE_STATE] == true &&
                    permissions[Manifest.permission.READ_CALL_LOG] == true
                ) {
                    // Permissions granted
                    Toast.makeText(requireContext(), "Permissions granted", Toast.LENGTH_SHORT)
                        .show()
                    proceedToNextScreen()


                } else {
                    // Permissions denied
                    viewModel.saveAllPermissionGranted(false)
                    Toast.makeText(
                        requireContext(),
                        "Please grant permission to continue",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        binding.buttonGrantAll.setOnClickListener {
            requestPermissions()
        }

        return binding.root
    }





    private fun requestPermissions() {
        val permissionToAskFor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS
            )
        } else {
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS

            )
        }

        requestPermissionLauncher.launch(permissionToAskFor)
    }


    private fun proceedToNextScreen() {
        lifecycleScope.launch {
            val folderSelected = viewModel.datastore.first().folderSelected
            viewModel.saveAllPermissionGranted(true)
            if (folderSelected) {
                findNavController().navigate(R.id.action_permissionFragment_to_main)
            } else {
                findNavController().navigate(R.id.action_permissionFragment_to_selectFolderFragment)
            }
        }
    }


}

