package `in`.vertexdev.mobile.call_rec.ui.frags.main

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.databinding.FragmentFilterBottomsheetBinding
import `in`.vertexdev.mobile.call_rec.models.StatusItem
import `in`.vertexdev.mobile.call_rec.util.State
import `in`.vertexdev.mobile.call_rec.util.Utils.toast
import `in`.vertexdev.mobile.call_rec.viewModel.LogsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FilterBottomsheetFragment : Fragment() {
    private var _binding: FragmentFilterBottomsheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LogsViewModel by activityViewModels()


    @OptIn(FlowPreview::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBottomsheetBinding.inflate(inflater, container, false)

        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
            View.GONE
        requireActivity().findViewById<FloatingActionButton>(R.id.floatingActionButton).visibility =
            View.GONE
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.leadStatusFilters.collect {
                        when (it) {
                            is State.Failed -> {
                                withContext(Dispatchers.Main){
                                    requireContext().toast("Failed to get Status::${it.message}")
                                }

                            }

                            is State.Loading -> {

                            }

                            is State.Success -> {

                                setDataToChipGroup(false, binding.leadStatusChipGroup, it.data)
                            }
                        }

                    }
                }

                launch {
                    viewModel.labelFilters.collect {
                        when (it) {
                            is State.Failed -> {
                                withContext(Dispatchers.Main){
                                    requireContext().toast("Failed to get Status::${it.message}")
                                }
                            }

                            is State.Loading -> {

                            }

                            is State.Success -> {
                                setDataToChipGroup(false, binding.labelChipGroup, it.data)
                            }
                        }

                    }
                }

                launch {
                    viewModel.sourceFilters.collect {
                        when (it) {
                            is State.Failed -> {
                                withContext(Dispatchers.Main){
                                    requireContext().toast("Failed to get Status::${it.message}")
                                }
                            }

                            is State.Loading -> {

                            }

                            is State.Success -> {
                                setDataToChipGroup(false, binding.sourceChipGroup, it.data)
                            }
                        }

                    }
                }
                launch {
                    viewModel.countryFilters.collect {
                        when (it) {
                            is State.Failed -> {
                                withContext(Dispatchers.Main){
                                    requireContext().toast("Failed to get Status::${it.message}")
                                }
                            }

                            is State.Loading -> {

                            }

                            is State.Success -> {
                                setDataToChipGroup(true, binding.countryChipgroup, it.data)
                            }
                        }

                    }
                }
                launch {
                    viewModel.intakeFilters.collect {
                        when (it) {
                            is State.Failed -> {
                                withContext(Dispatchers.Main){
                                    requireContext().toast("Failed to get Status::${it.message}")
                                }
                            }

                            is State.Loading -> {

                            }

                            is State.Success -> {
                                setDataToChipGroup(false, binding.intakeChipGroup, it.data)
                            }
                        }

                    }
                }
                launch {
                    viewModel.branchFilters.collect {
                        when (it) {

                            is State.Failed -> {
                                withContext(Dispatchers.Main){
                                    requireContext().toast("Failed to get Status::${it.message}")
                                }
                            }

                            is State.Loading -> {

                            }

                            is State.Success -> {
                                setDataToChipGroup(false, binding.branchChipGroup, it.data)
                            }
                        }

                    }
                }


            }
        }
        binding.button5.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.button4.setOnClickListener {
            // Use a helper function to get selected chip IDs from each ChipGroup
            viewModel.filterLeadStatus = getSelectedChipIds(binding.leadStatusChipGroup)
            viewModel.filterBranch = getSelectedChipIds(binding.branchChipGroup)
            viewModel.filterSource = getSelectedChipIdsName(binding.sourceChipGroup)
            viewModel.filterLabel = getSelectedChipIdsName(binding.labelChipGroup)
            viewModel.filterCountry = getSelectedChipIdsName(binding.countryChipgroup)
            viewModel.filterSeasons = getSelectedChipIdsName(binding.intakeChipGroup)

            viewModel.cardId = null
            viewModel.multipleFilterActive = true
            findNavController().navigateUp()
        }
        return binding.root
    }

    // Helper function to extract selected chip IDs from any ChipGroup
    private fun getSelectedChipIds(chipGroup: ChipGroup): MutableList<String> {
        return chipGroup.checkedChipIds.mapNotNull { chipId ->
            val chip = chipGroup.findViewById<Chip>(chipId)
            chip?.tag?.toString() // Convert tag to string or return null if no tag
        }.toMutableList() // Convert to mutable list
    }
    private fun getSelectedChipIdsName(chipGroup: ChipGroup): MutableList<String> {
        return chipGroup.checkedChipIds.mapNotNull { chipId ->
            val chip = chipGroup.findViewById<Chip>(chipId)
            chip?.text?.toString() // Convert tag to string or return null if no tag
        }.toMutableList() // Convert to mutable list
    }

    private suspend fun setDataToChipGroup(
        country: Boolean,
        chipGroup: ChipGroup,
        data: List<StatusItem>
    ) {
        Log.d("TAGWEE", "filterLeadStatus: ${viewModel.filterLeadStatus ?: "None"}")
        Log.d("TAGWEE", "filterBranch: ${viewModel.filterBranch ?: "None"}")
        Log.d("TAGWEE", "filterSource: ${viewModel.filterSource ?: "None"}")
        Log.d("TAGWEE", "filterLabel: ${viewModel.filterLabel ?: "None"}")
        Log.d("TAGWEE", "filterCountry: ${viewModel.filterCountry ?: "None"}")
        Log.d("TAGWEE", "filterSeasons: ${viewModel.filterSeasons ?: "None"}")


        withContext(Dispatchers.Main) {
            chipGroup.removeAllViews()
            chipGroup.visibility = View.GONE
        }


        // Add chips for each LeadCategory item in the list
        data.forEach { statusItem ->
            val name = if (country) {
                statusItem.country_name

            } else {
                statusItem.name
            }

            var checked = false
            Log.d("FILTER_UPDATE", "Lead Status: ${viewModel.filterLeadStatus}")
            // Create a mapping of ChipGroup IDs to filter lists
            val filterMap = mapOf(
                R.id.leadChipGroup to viewModel.filterLeadStatus,
                R.id.label_chip_group to viewModel.filterLabel,
                R.id.intake_chip_group to viewModel.filterSeasons,
                R.id.country_chipgroup to viewModel.filterCountry,
                R.id.source_chip_group to viewModel.filterSource,
                R.id.branch_chip_group to viewModel.filterBranch
            )

// Check if the statusItem.id is in the corresponding filter list
            checked = filterMap[chipGroup.id]?.find { it == statusItem.id } != null

// Log for debugging purposes (optional)
            Log.d(
                "TAG",
                "ChipGroup ID: ${chipGroup.id}, StatusItem ID: ${statusItem.id}, Checked: $checked"
            )

            Log.d("TAG", "setDataToChipGroup:$name::$checked ")
            val chip = Chip(chipGroup.context).apply {
                text = name

                isCheckable = true
                isClickable = true
                id = View.generateViewId() // Assign a unique ID to each chip
                tag = statusItem.id // Set the tag to the LeadCategory ID


            }

            // Add each chip to the ChipGroup
            withContext(Dispatchers.Main) {
                chipGroup.addView(chip)
                chip.isChecked =checked
            }


        }

        withContext(Dispatchers.Main) {
            chipGroup.visibility = View.VISIBLE
        }


    }


}