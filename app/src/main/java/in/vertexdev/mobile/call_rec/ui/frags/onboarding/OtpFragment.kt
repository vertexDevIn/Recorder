package `in`.vertexdev.mobile.call_rec.ui.frags.onboarding

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.databinding.FragmentOtpBinding
import `in`.vertexdev.mobile.call_rec.util.State
import `in`.vertexdev.mobile.call_rec.util.Utils
import `in`.vertexdev.mobile.call_rec.util.Utils.toast
import `in`.vertexdev.mobile.call_rec.viewModel.AuthViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class OtpFragment : Fragment() {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    private var countDownTimer: CountDownTimer? = null
    private val RESEND_OTP_TIME: Long = 30000 // 30 sec


    private fun checkOtpInPut(text: String): Boolean {
        var value = true
        if (text.length > 6 || text.length < 6) {
            requireContext().toast("Invalid Input, Please enter a 6-digit OTP.")
            value = false
        }
        return value
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        startResendTimer()


        binding.verifyOtp.setOnClickListener {
            if (checkOtpInPut(binding.otp.text.toString())) {

                this.lifecycleScope.launch {
                    viewModel.verifyOtp(binding.otp.text.toString()).collect{
                        when(it){
                            is State.Failed -> {
                                Log.d("TAG", "verifyOtp::Failed::${it.message} ")
                               requireContext().toast(it.message)
                                binding.verifyOtp.isEnabled = true
                                binding.progressBar.visibility = View.INVISIBLE
                            }
                            is State.Loading -> {
                                binding.verifyOtp.isEnabled = false
                                binding.progressBar.visibility = View.VISIBLE

                            }
                            is State.Success ->{
                                Log.d("TAG", "verifyOtp::Success::${it.data} ")
                                checkPermissionsAndNavigate()
                            }
                        }
                    }
                }


            }
        }
        binding.textView19.setOnClickListener {
            this.lifecycleScope.launch {
                viewModel.resendOtp().collect{
                    when(it){
                        is State.Failed -> {requireContext().toast(it.message)}
                        is State.Loading -> {}
                        is State.Success -> {
                            requireContext().toast("Otp SentSuccessfully")
                            startResendTimer()
                        }
                    }
                }
            }

        }



        return binding.root
    }

    private fun checkPermissionsAndNavigate() {
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
                Manifest.permission.READ_CONTACTS

                )
        }
        if(Utils.areAllPermissionsGranted(requireContext(), permissionToCheckAre)){
            this.lifecycleScope.launch {
                val folderSelected =   viewModel.datastore.first().folderSelected
                if(folderSelected){
                    findNavController().navigate(R.id.action_otpFragment_to_main)
                }else{
                    findNavController().navigate(R.id.action_otpFragment_to_selectFolderFragment)
                }
            }

        }else{
            findNavController().navigate(R.id.action_otpFragment_to_permissionFragment)
        }
    }


    private fun startResendTimer() {
        binding.textView19.visibility = View.INVISIBLE // Disable resend button
        binding.textView20.visibility = View.VISIBLE // Show countdown text view

        // Create a new CountDownTimer
        countDownTimer = object : CountDownTimer(RESEND_OTP_TIME, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update the countdown text view with the remaining time
                val secondsRemaining = millisUntilFinished / 1000
                binding.textView20.text = "$secondsRemaining s"
            }

            override fun onFinish() {
                // When the timer finishes, enable the resend button and hide the countdown text view
                binding.textView19.visibility = View.VISIBLE
                binding.textView20.visibility = View.INVISIBLE
            }
        }

        countDownTimer?.start() // Start the timer
    }


}