package `in`.vertexdev.mobile.call_rec.ui.frags.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.databinding.FragmentAddLeadBinding
import `in`.vertexdev.mobile.call_rec.models.Employee
import `in`.vertexdev.mobile.call_rec.models.StatusItem
import `in`.vertexdev.mobile.call_rec.util.State
import `in`.vertexdev.mobile.call_rec.util.Utils.extractCountryCode
import `in`.vertexdev.mobile.call_rec.util.Utils.toast
import `in`.vertexdev.mobile.call_rec.viewModel.LogsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddLeadFragment : Fragment() {
    private val args: AddLeadFragmentArgs by navArgs()
    private val viewModel: LogsViewModel by activityViewModels()
    private var _binding: FragmentAddLeadBinding? = null
    private val binding get() = _binding!!

    private var branches: List<StatusItem> = mutableListOf()
    private var leadStatusFilters: List<StatusItem> = mutableListOf()
    private var employee: List<Employee> = mutableListOf()

    private lateinit var countryCodes: Array<String>
    private lateinit var countryCodes2: Array<String>
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        _binding = FragmentAddLeadBinding.inflate(inflater, container, false)
        requireActivity().findViewById<FloatingActionButton>(R.id.floatingActionButton).visibility =
            View.GONE

        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
            View.GONE

        binding.materialToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()

        }

        if(args.lead){
            binding.materialToolbar.title = "CRM Add Lead"
        }else{
            binding.materialToolbar.title = "CRM Add Student"
        }

        // Load the country codes from resources
        countryCodes = resources.getStringArray(R.array.country_codes)
        countryCodes2 = resources.getStringArray(R.array.country_codes)

        val countryCodeAutoComplete: AutoCompleteTextView = binding.countryCodeMob
        val countryCodeAutoCompleteAlt: AutoCompleteTextView = binding.countryCodeAlternate

        // Create an ArrayAdapter to provide suggestions
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            countryCodes
        )

        // Create an ArrayAdapter to provide suggestions
        val adapter2 = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            countryCodes2
        )

        countryCodeAutoCompleteAlt.setAdapter(adapter2)

        // Set the adapter to the AutoCompleteTextView
        countryCodeAutoComplete.setAdapter(adapter)

        // Optional: Set default hint
        // Set default selected value as India (+91)
        countryCodeAutoComplete.setText("+91 (India)", false)  // Set default country code
        countryCodeAutoCompleteAlt.setText("+91 (India)", false)


        // Handle item selections from the dropdown
//        countryCodeAutoComplete.setOnItemClickListener { parent, _, position, _ ->
//            val selectedCountryCode = parent.getItemAtPosition(position).toString()
//            Toast.makeText(requireContext(), "Selected: $selectedCountryCode", Toast.LENGTH_SHORT)
//                .show()
//        }
        binding.button7.setOnClickListener {
            this.lifecycleScope.launch {
                if (checkIfAllFieldEntered()) {
                    // Log for fields check
                    Log.d("LeadEntry", "All fields entered successfully.")

                    // Extract country code
                    val countryCode = extractCountryCode(binding.countryCodeMob.text.toString())
                    Log.d("LeadEntry", "Country code: $countryCode")

                    // Find the selected branch
                    val branch = branches.find { it.name == binding.branch.text.toString() }
                    Log.d("LeadEntry", "Selected branch: ${branch?.name ?: "No branch selected"}")

                    // Find the assigned employee
                    val assign = employee.find { "${it.fname} ${it.lname}" == binding.assign.text.toString() }
                    Log.d("LeadEntry", "Assigned to: ${assign?.fname ?: "No employee assigned"} ${assign?.lname ?: ""}")

                    // Find the lead status
                    val status = leadStatusFilters.find { it.name == binding.moveToStatus.text.toString() }
                    Log.d("LeadEntry", "Lead status: ${status?.name ?: "No status selected"}")

                    // Log for reference
                    Log.d("LeadEntry", "Reference: ${binding.ref.text.toString()}")

                    val tag = if(args.lead){
                        "lead"
                    }else{
                        ""
                    }

                    // Call addLead function and collect response
                    viewModel.addLead(
                        firstName = binding.firstName.text.toString(),
                        middleName = binding.middleName.text.toString(),
                        surnameName = binding.surnameName.text.toString(),
                        countryCodeMob = countryCode,
                        mobileNumber = binding.mobileNumber.text.toString(),
                        alternateNumber = binding.alternateNumber.text.toString(),
                        email = binding.email.text.toString(),
                        branchId = branch?.id ?: "",
                        adminId = assign?.id ?: "",
                        leadStatus = status?.id ?: "",
                        binding.ref.text.toString(),
                        tag
                    ).collect {
                        when (it) {
                            is State.Failed -> {
                                Log.d("LeadEntry", "Lead entry failed with message: ${it.message}")
                            }
                            is State.Loading -> {
                                Log.d("LeadEntry", "Lead entry is loading...")
                            }
                            is State.Success -> {
                                Log.d("LeadEntry", "Lead entry successful with ID: ${it.data}")
                                requireContext().toast("Added Successfully")
                                findNavController().navigateUp()
                            }
                        }
                    }

                    // Log final field values
                    Log.d("LeadEntry", "Branch: ${binding.branch.text.toString()}")
                    Log.d("LeadEntry", "Assigned: ${binding.assign.text.toString()}")
                    Log.d("LeadEntry", "Lead status: ${binding.moveToStatus.text.toString()}")
                    Log.d("LeadEntry", "Reference: ${binding.ref.text.toString()}")
                } else {
                    Log.d("LeadEntry", "Some required fields are missing.")
                }
            }
        }


        viewLifecycleOwner.lifecycleScope.launch() {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getAllFilters()
                }
                launch {
                    viewModel.leadStatusFilters.collect {
                        when (it) {
                            is State.Failed -> {
                                withContext(Dispatchers.Main) {
                                    requireContext().toast("Failed to get Status::${it.message}")
                                }

                            }

                            is State.Loading -> {

                            }

                            is State.Success -> {
                                leadStatusFilters = it.data
                                setDataToStatusDropdown(it.data)


                            }
                        }

                    }
                }



                launch {
                    viewModel.sourceFilters.collect {
                        when (it) {
                            is State.Failed -> {
                                withContext(Dispatchers.Main) {
                                    requireContext().toast("Failed to get Status::${it.message}")
                                }
                            }

                            is State.Loading -> {

                            }

                            is State.Success -> {
                                setDataToSourceDropdown(it.data)
                            }
                        }

                    }
                }


                launch {
                    viewModel.branchFilters.collect {
                        when (it) {

                            is State.Failed -> {
                                withContext(Dispatchers.Main) {
                                    requireContext().toast("Failed to get Status::${it.message}")
                                }
                            }

                            is State.Loading -> {

                            }

                            is State.Success -> {
                                // setDataToChipGroup(false, binding.branchChipGroup, it.data)
                                branches = it.data
                                setDataToBranchDropdown(it.data)
                            }
                        }

                    }
                }

                launch {
                    viewModel.employees.collect {
                        when (it) {

                            is State.Failed -> {
                                withContext(Dispatchers.Main) {
                                    requireContext().toast("Failed to get Status::${it.message}")
                                }
                            }

                            is State.Loading -> {

                            }

                            is State.Success -> {
                                // setDataToChipGroup(false, binding.branchChipGroup, it.data)
                                employee = it.data
                                val userId = viewModel.datastore.first().userId
                                setDataToEmployeeDropdown(it.data,userId)
                            }
                        }

                    }
                }

            }
        }
        binding.branch.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
              binding.assign.requestFocus()
            }

        })

        binding.assign.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                binding.moveToStatus.requestFocus()
            }

        })
        binding.moveToStatus.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                binding.ref.requestFocus()
            }

        })

        binding.ref.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                binding.button7.requestFocus()
            }

        })
        binding.countryCodeMob.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                binding.mobileNumber.requestFocus()
            }

        })

        binding.countryCodeAlternate.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                binding.alternateNumber.requestFocus()
            }

        })


        return binding.root
    }

    private fun checkIfAllFieldEntered(): Boolean {
        var result = true

        if (binding.firstName.text.isNullOrEmpty()) {
            binding.firstName.error = "Required"
            binding.firstName.requestFocus()
            result = false
        }

        if (binding.countryCodeMob.text.isNullOrEmpty()) {
            binding.countryCodeMob.error = "Required"
            binding.countryCodeMob.requestFocus()
            result = false

        }
        if (binding.mobileNumber.text.isNullOrEmpty()) {
            binding.mobileNumber.error = "Required"
            binding.mobileNumber.requestFocus()
            result = false
        }
        if (binding.assign.text.isNullOrEmpty()) {
            binding.assign.error = "Required"
            binding.assign.requestFocus()
            result = false
        }


        return result
    }

    private fun setDataToEmployeeDropdown(data: List<Employee>,defaultEmployeeId:String) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            data
        )

        // Set the adapter to the AutoCompleteTextView
        binding.assign.setAdapter(adapter)
        // Find the default employee based on ID
        val defaultEmployee = data.find { it.id == defaultEmployeeId }

        // Set the default selected item if it exists
        defaultEmployee?.let {
            binding.assign.setText(it.fname + " " + it.lname, false) // `false` to prevent triggering dropdown popup
        }

    }

    private fun setDataToSourceDropdown(data: List<StatusItem>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            data
        )

        // Set the adapter to the AutoCompleteTextView
        binding.ref.setAdapter(adapter)

    }

    private fun setDataToBranchDropdown(data: List<StatusItem>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            data
        )

        // Set the adapter to the AutoCompleteTextView
        binding.branch.setAdapter(adapter)

    }

    private fun setDataToStatusDropdown(data: List<StatusItem>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            data
        )

        // Set the adapter to the AutoCompleteTextView
        binding.moveToStatus.setAdapter(adapter)


    }


}