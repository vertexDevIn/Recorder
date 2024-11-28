package `in`.vertexdev.mobile.call_rec.ui.frags.onboarding

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.databinding.FragmentLoginBinding
import `in`.vertexdev.mobile.call_rec.util.State
import `in`.vertexdev.mobile.call_rec.util.Utils.areAllPermissionsGranted
import `in`.vertexdev.mobile.call_rec.util.Utils.toast
import `in`.vertexdev.mobile.call_rec.viewModel.AuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.buttonLogin.setOnClickListener {
            if(binding.TextApiKey.text.isNullOrEmpty()){
                binding.TextApiKey.error = "Api Key is required"
                binding.TextApiKey.requestFocus()
                return@setOnClickListener
            }
            this.lifecycleScope.launch {
                viewModel.login(binding.TextApiKey.text.toString()).collect{
                    when(it){
                        is State.Failed -> {
                            requireContext().toast(it.message)
                            binding.progressBar.visibility = View.GONE
                            binding.buttonLogin.isEnabled = true
                        }
                        is State.Loading -> {
                          binding.progressBar.visibility = View.VISIBLE
                            binding.buttonLogin.isEnabled = false
                        }
                        is State.Success -> {
                            requireContext().toast("Login Successful")
                            checkIfPermissionAreGranted(it.data)
                        }
                    }
                }
            }

        }

        return binding.root
    }

    private fun checkIfPermissionAreGranted(navigateToOtp:Boolean) {
        val permissionToCheckAre = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                Manifest.permission.READ_CONTACTS,

                )
        }

        if(navigateToOtp){
            findNavController().navigate(R.id.action_loginFragment_to_otpFragment)

        }else{
            if(areAllPermissionsGranted(requireContext(),permissionToCheckAre)){
                this.lifecycleScope.launch {
                    val folderSelected =   viewModel.datastore.first().folderSelected
                    if(folderSelected){
                        findNavController().navigate(R.id.action_loginFragment_to_main)
                    }else{
                        findNavController().navigate(R.id.action_loginFragment_to_selectFolderFragment)
                    }
                }

            }else{
                findNavController().navigate(R.id.action_loginFragment_to_permissionFragment)
            }
        }

    }


}