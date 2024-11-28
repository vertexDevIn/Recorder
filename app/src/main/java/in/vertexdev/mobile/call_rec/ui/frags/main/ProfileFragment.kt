package `in`.vertexdev.mobile.call_rec.ui.frags.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.databinding.FragmentProfileBinding
import `in`.vertexdev.mobile.call_rec.models.UserResponse
import `in`.vertexdev.mobile.call_rec.util.State
import `in`.vertexdev.mobile.call_rec.util.StoragePermissionUseCase
import `in`.vertexdev.mobile.call_rec.util.Utils.toast
import `in`.vertexdev.mobile.call_rec.viewModel.AuthViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()
    private var askActive = false

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
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        requireActivity().findViewById<FloatingActionButton>(R.id.floatingActionButton).visibility =
            View.GONE



        binding.button2.setOnClickListener {
            checkForPermission()
        }
        this.lifecycleScope.launch {
            binding.textView16dccddf.text = viewModel.datastore.first().folderPath.toString()
        }

        viewLifecycleOwner.lifecycleScope.launch() {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch{
                    viewModel.getUser().collect{
                        when(it){
                            is State.Failed -> {}
                            is State.Loading -> {}
                            is State.Success -> {
                                setDataToUi(it.data)
                            }

                        }
                    }
                }
                launch{
                 val deleteRecordings  =    viewModel.datastore.first().deleteRecordings
                    binding.switch1.isChecked = deleteRecordings
                }

            }
        }
        binding.switch1.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.saveDeleteRecordings(isChecked)
        }
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    private fun setDataToUi(data: UserResponse) {
        binding.textView15.text = data.fname + " " + data.lname
        binding.textView16.text = "Mobile No: "+ data.mobile
        binding.textView16df.text = "Email: "+ data.email
        binding.textView16d.text = "Branch: " + data.branch
        binding.textView16dccd.text = "Custom Role: " + data.customRole



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


    override fun onResume() {
        super.onResume()
        if(askActive){
            askActive = false
            checkIfGranted()
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
        askActive = true

    }

    private fun onPermissionGranted() {
        // Perform any action after permission is granted
        requireContext().toast("Permission Granted")
        findNavController().navigate(R.id.action_profileFragment_to_folderListFragment2)
    }

    private fun onPermissionDenied() {
        // Handle the case when permission is denied
        requireContext().toast("Permission Not Granted")
    }


}