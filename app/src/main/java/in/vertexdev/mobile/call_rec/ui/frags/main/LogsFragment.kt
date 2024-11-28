package `in`.vertexdev.mobile.call_rec.ui.frags.main

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.databinding.FragmentLogsBinding
import `in`.vertexdev.mobile.call_rec.models.Lead
import `in`.vertexdev.mobile.call_rec.models.LeadCategory
import `in`.vertexdev.mobile.call_rec.models.StatusItem
import `in`.vertexdev.mobile.call_rec.rv.LeadAdapter
import `in`.vertexdev.mobile.call_rec.rv.OptionAdapter
import `in`.vertexdev.mobile.call_rec.util.CustomAutoCompleteTextView
import `in`.vertexdev.mobile.call_rec.util.State
import `in`.vertexdev.mobile.call_rec.util.Utils.pickDateRange
import `in`.vertexdev.mobile.call_rec.util.Utils.pickSingleDate
import `in`.vertexdev.mobile.call_rec.util.Utils.pickSingleTime
import `in`.vertexdev.mobile.call_rec.util.Utils.toListOfStrings
import `in`.vertexdev.mobile.call_rec.util.Utils.toast
import `in`.vertexdev.mobile.call_rec.viewModel.LogsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class LogsFragment : Fragment() {
    private val args: LogsFragmentArgs by navArgs()


    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LogsViewModel by activityViewModels()
    private val allChip = View.generateViewId() // Store the generated ID
    private lateinit var backPressCallback: OnBackPressedCallback


    private val leadAdapter by lazy {
        LeadAdapter(
            onCall = this::OnCall,
            onCountryClick = this::onCountryClick,
            onIntakeClick = this::onIntakeClick,
            onLabelClick = this::onLabelClick,
            onStatusClick = this::onStatusClick,



        )

    }


    private fun OnCall(lead: Lead) {
        Log.d("TAG", "OnCall: ")
        callNumber(lead.student_mobile)

    }

    fun onCountryClick(lead: Lead) {
        showCountryUpdateDialog(lead)

    }


    fun onIntakeClick(lead: Lead) {
        showIntakeUpdateDialog(lead)

    }

    fun onLabelClick(lead: Lead) {
        showLabelUpdateDialog(lead)

    }

    fun onStatusClick(lead: Lead) {
        showStatusUpdateDialog(lead)

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLogsBinding.inflate(inflater, container, false)

        // Set up the SwipeRefreshLayout listener
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Perform data refresh operation here
            // For example, fetch new data from network or reload existing data
            viewModel.fetchData(true)
        }


        Log.d(
            "LogsFragment",
            "LogsFragmentArgs::tagStudent::${args.tagStudent}:::startDate::: ${args.startDate}" +
                    " :: endDate::${args.endDate} ::: cardId:::  ${args.cardId}"
        )

        if (args.startDate == "empty") {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
            // Set the end date to today
            if(args.fromAction){
                viewModel.startDate = dateFormat.format(calendar.time)
                Log.d("TAG", "startDate::start ${viewModel.startDate} ")
            }else{
                viewModel.startDate = null
            }


        } else {
            viewModel.startDate = args.startDate

        }

        if (args.endDate == "empty") {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
            // Set the end date to today


            if(args.fromAction){
                viewModel.endDate = dateFormat.format(calendar.time)
                Log.d("TAG", "startDate::end ${viewModel.endDate} ")
            }else{
                viewModel.endDate = null
            }
        } else {
            viewModel.endDate = args.endDate
        }

        if (args.tagStudent == "empty") {
            viewModel.tagStudent = ""
        } else {
            viewModel.tagStudent = args.tagStudent

        }

        if (args.cardId == "empty") {
            viewModel.cardId = null
        } else {
            viewModel.cardId = args.cardId
        }

        if (args.fromAction) {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
                View.GONE
            requireActivity().findViewById<FloatingActionButton>(R.id.floatingActionButton).visibility =
                View.GONE
            binding.toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
            binding.toolbar.setNavigationOnClickListener {
                viewModel.resetFilters()
                findNavController().navigateUp()
            }
        }else{
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
                View.VISIBLE
            requireActivity().findViewById<FloatingActionButton>(R.id.floatingActionButton).visibility =
                View.VISIBLE
            requireActivity().findViewById<FloatingActionButton>(R.id.floatingActionButton).setOnClickListener{
                val action = LogsFragmentDirections.actionLogsFragmentToAddLeadFragment(false)
                findNavController().navigate(action)
            }
        }

        binding.toolbar.title = args.title



        fetchAllData(viewModel.startDate, viewModel.endDate)


        // Set click listener on the menu items
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_calendar -> {
                    // Handle the click on the specific menu item
                    // Do something, e.g., show a Toast or navigate
                    openPicker()
                    true // Return true to indicate the click was handled
                }

                R.id.action_filter -> {
                    // Handle the click on the specific menu item
                    // Do something, e.g., show a Toast or navigate
                    findNavController().navigate(R.id.action_logsFragment_to_filterBottomsheetFragment)

                    true // Return true to indicate the click was handled
                }

                else -> false
            }
        }
        val menu = binding.toolbar.menu
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView

        // Optional: customize SearchView properties (e.g., hint text, query hint, etc.)
        searchView.queryHint = "Search items..." // Set query hint
        searchView.isIconified = false

        // Set up a listener to handle search queries
        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search query submission
                query?.let {
                    binding.leadChipGroup.visibility = View.GONE
                    performSearch(it)  // Call a function to handle the search logic
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle search query text change (for live search as user types)
                newText?.let {
                    if (it.length > 3) {
                       // binding.leadChipGroup.visibility = View.GONE
                        performSearch(it)
                    }
                    if (it.isEmpty()) {
                        Log.d("TAG", "setOnCloseListener: ")
                       // binding.leadChipGroup.visibility = View.VISIBLE
                        viewModel.searchQuery = ""
                        viewModel.fetchData(true)
                    }
                    // Optionally handle the live search
                }
                return true
            }
        })
        // Set up the OnCloseListener to handle when the SearchView is closed

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                binding.leadChipGroup.visibility = View.GONE
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                Log.d("TAG", "setOnCloseListener: ")
                viewModel.searchQuery = ""
                binding.leadChipGroup.visibility = View.VISIBLE
                viewModel.fetchData(true)
                return true

            }

        })

        setUpRvs()
        dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        viewLifecycleOwner.lifecycleScope.launch() {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.leadChips.collect() {
                        when (it) {
                            is State.Failed -> {
                                binding.progressBar2.visibility = View.GONE
                            }

                            is State.Loading -> {
                                binding.progressBar2.visibility = View.VISIBLE
                            }

                            is State.Success -> {
                                binding.progressBar2.visibility = View.GONE
                                if (it.data.isNotEmpty()) {
                                    val availableCount = it.data.filter { it.count != "0"}
                                    setDataToUi(availableCount)
                                }

                            }
                        }

                    }
                }
                launch {
                    viewModel.leadList.collect() {
                        when (it) {
                            is State.Failed -> {}
                            is State.Loading -> {}
                            is State.Success -> {
                                binding.swipeRefreshLayout.isRefreshing = false
                                Log.d("leadList", "leadList:${it.data} ")
                                if (it.data.isNotEmpty()) {
                                    binding.textView21.visibility = View.GONE
                                    leadAdapter.updateData(it.data,viewModel.tagStudent == "lead" )
                                } else {
                                    binding.textView21.visibility = View.VISIBLE
                                    leadAdapter.updateData(listOf(), true)
                                }

                            }
                        }

                    }
                }

            }
        }

        // Add the scroll listener to detect the end of the content
        binding.leadRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Check if the user has scrolled down
                if (dy > 0) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()

                    // Check if we have reached the end of the list
                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                        // User has scrolled to the end
                        println("End of content reached")
                        if (viewModel.moreItemsAvailable) {
                            if (!viewModel.fetchInProgress) {
                                viewModel.fetchData(false)
                            }

                        } else {
                           // requireContext().toast("No more data")

                        }
                    }
                }
            }
        })
        viewModel.getAllFilters()
        binding.button8.setOnClickListener {
            viewModel.clearEveryFilter()
            binding.leadChipGroup.check(allChip)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add back press callback
        backPressCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back press
                Log.d("YourFragment", "Back pressed in YourFragment")
                // Add your logic here, e.g., dismiss a dialog or navigate back
                // You can also use findNavController() to navigate back if using Navigation Component
                viewModel.resetFilters()
                findNavController().navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)
    }


    private fun callNumber(phoneNumber: String) {
        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("TAG", "callNumber:#phoneNumber ")
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        }
    }

    private fun performSearch(it: String) {
        viewModel.searchQuery = it
        viewModel.search(it)

    }

    private fun setDataToUi(data: List<LeadCategory>) {
        // Remove all existing views from the ChipGroup
        binding.leadChipGroup.removeAllViews()

        // Enable single selection mode to ensure only one chip is checked at a time
        binding.leadChipGroup.isSingleSelection = true




        // Add an "All" chip as the first item in the ChipGroup
        val allChip = Chip(binding.leadChipGroup.context).apply {
            text = "All"
            isCheckable = true
            isChecked = false
            isClickable = true
            id = allChip // Assign a unique ID
            tag = "all" // Tag for "All" selection

            // Set click listener for the "All" chip
            setOnClickListener {
                getDataForStatusId("all") // Handle "All" selection
            }
        }

        if(viewModel.cardId == null && !viewModel.multipleFilterActive){
            allChip.isChecked =true
        }

        // Add the "All" chip to the ChipGroup
        binding.leadChipGroup.addView(allChip)

        // Add chips for each LeadCategory item in the list
        data.forEach { leadCategory ->
            val name = "${leadCategory.name} ${leadCategory.count}"

            val chip = Chip(binding.leadChipGroup.context).apply {
                text = name
                isCheckable = true
                isClickable = true
                id = View.generateViewId() // Assign a unique ID to each chip
                tag = leadCategory.id // Set the tag to the LeadCategory ID

                // Set a click listener for the chip
                setOnClickListener {
                    val statusId = tag as? String // Safely cast the tag to a String
                    statusId?.let { id ->
                        getDataForStatusId(id)
                    }
                }
            }

            if(viewModel.cardId == leadCategory.id){
                chip.isChecked = true
            }

            // Add each chip to the ChipGroup
            binding.leadChipGroup.addView(chip)
        }
    }

    // Function to handle the status ID
    private fun getDataForStatusId(statusId: String) {
        when (statusId) {
            "all" -> {
                // Handle fetching data for "All" selection
                this.lifecycleScope.launch {
                    viewModel.resetFilters()
                    viewModel.fetchData(first = true)
                }
            }

            else -> {
                // Handle fetching data for a specific LeadCategory ID
                // Your implementation for fetching data based on status ID

                this.lifecycleScope.launch {
                    viewModel.resetFilters()
                    delay(200)
                    viewModel.cardId = statusId
                    viewModel.fetchData(true)
                }

            }
        }
    }


    private fun fetchAllData(startDate: String?, endDate: String?) {
        Log.d("TAG", "fetchAllData:$startDate , $endDate ")
        fetchChipData(startDate, endDate)
        fetchInitialData()
    }

    private fun fetchInitialData() {
        this.lifecycleScope.launch {
            viewModel.fetchData(true)
        }

    }

    private fun fetchChipData(startDate: String?, endDate: String?) {

        this.lifecycleScope.launch {
            viewModel.getLeadChips(startDate, endDate)
        }

    }

    private fun openPicker() = this.lifecycleScope.launch {

        val date = pickDateRange(requireContext(), "dd-MMM-yyyy")
        Log.d("TAG", "Selected date::$date ")
        Toast.makeText(requireContext(), "Selected date:$date", Toast.LENGTH_SHORT).show()
        if (date != null) {
            viewModel.startDate = date.first
            viewModel.endDate = date.second
            viewModel.fetchData(true)
            viewModel.reloadLeadChips()

        }

    }


    private fun setUpRvs() {
        val layoutManager1 = LinearLayoutManager(requireContext())
        layoutManager1.orientation = LinearLayoutManager.VERTICAL
        binding.leadRv.layoutManager = layoutManager1
        binding.leadRv.adapter = leadAdapter
    }

    private lateinit var dialog: Dialog

    val selectedItems = ArrayList<StatusItem>()


    private fun showCountryUpdateDialog(lead: Lead) {
        selectedItems.clear()
        Log.d("showCountryUpdateDialog", "Dialog initiated for lead: ${lead.id}")

        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_update_dialog)

        val yesBtn = dialog.findViewById<Button>(R.id.button3)
        val noBtn = dialog.findViewById<Button>(R.id.button6)
        val title1 = dialog.findViewById<TextView>(R.id.title)
        val title2 = dialog.findViewById<TextView>(R.id.title2)
        val dropdownAutoComplete = dialog.findViewById<CustomAutoCompleteTextView>(R.id.dropdown_auto_complete)
        val chipGroupSelected = dialog.findViewById<ChipGroup>(R.id.chip_group_selected)
        val group = dialog.findViewById<Group>(R.id.group_appontment)
        val dateText = dialog.findViewById<EditText>(R.id.dateText)
        val timeText = dialog.findViewById<EditText>(R.id.timeText)
        val remarkText = dialog.findViewById<EditText>(R.id.remarkText)

        title1.text = "Update Country"
        title2.text = "Country"
        group.visibility = View.GONE

        Log.d("showCountryUpdateDialog", "Dialog UI set up.")

        noBtn.setOnClickListener {
            chipGroupSelected.removeAllViews()
            selectedItems.clear()
            Log.d("showCountryUpdateDialog", "Dialog dismissed, selectedItems cleared.")
            dialog.dismiss()
        }

        this.lifecycleScope.launch {
            viewModel.countryFilters.collect { state ->
                when (state) {
                    is State.Failed -> {
                        Log.e("showCountryUpdateDialog", "Failed to load country filters: ${state.message}")
                    }
                    is State.Loading -> {
                        Log.d("showCountryUpdateDialog", "Loading country filters...")
                    }
                    is State.Success -> {
                        Log.d("showCountryUpdateDialog", "Country filters loaded successfully.")

                        // Set up the AutoCompleteTextView with a custom ArrayAdapter
                        val adapter = OptionAdapter(
                            requireContext(),
                            state.data,
                            dropdownAutoComplete,
                            true,
                            true
                        ) { selected: Boolean, item: StatusItem, adapter: OptionAdapter ->

                            Log.d("showCountryUpdateDialog", "SelectedItems: $selectedItems")
                            Log.d("showCountryUpdateDialog", "item: $item")

                            if (selected) {
                                if (!selectedItems.any { it.id == item.id }) { // Use id for comparison
                                    selectedItems.add(item)
                                    Log.d("showCountryUpdateDialog", "Added item: $item to selectedItems")
                                    addChipToGroup(chipGroupSelected, item, true, adapter)
                                } else {
                                    Log.d("showCountryUpdateDialogff", "Item already in selectedItems: $item")
                                }
                            } else {
                                if (selectedItems.any { it.id == item.id }) { // Use id for comparison
                                    selectedItems.remove(item)
                                    Log.d("showCountryUpdateDialog", "Removed item: $item from selectedItems")
                                    chipGroupSelected.removeView(
                                        chipGroupSelected.findViewWithTag<Chip>(item.id)
                                    ) // Remove chip
                                    Log.d("showCountryUpdateDialog", "Removed chip with tag: ${item.id}")
                                } else {
                                    Log.d("showCountryUpdateDialog", "Item not found in selectedItems: $item")
                                }
                            }
                        }

                        // Pre-select countries that are already in the lead's `goingto_country`
                        for (i in state.data) {
                            val countryName = i.country_name ?: continue
                            Log.d("showCountryUpdateDialog", "Checking country: $countryName")
                            if (lead.goingto_country?.contains(countryName) == true) {
                                selectedItems.add(i)
                                Log.d("showCountryUpdateDialog", "Pre-selected country: $countryName")
                            }
                        }

                        adapter.updateList(selectedItems)

                        // Add pre-selected chips to the ChipGroup
                        for (i in selectedItems) {
                            addChipToGroup(chipGroupSelected, i, true, adapter)
                            Log.d("showCountryUpdateDialog", "Added chip for pre-selected item: ${i.country}")
                        }

                        dropdownAutoComplete.setAdapter(adapter)
                    }
                }
            }
        }

        yesBtn.setOnClickListener {
            if (selectedItems.isNotEmpty()) {
                Log.d("showCountryUpdateDialog", "Yes button clicked with selected items: $selectedItems")
                this.lifecycleScope.launch {
                    val seasons = lead.goingto_intake?.toListOfStrings() ?: emptyList()
                    viewModel.updateInterestedCountries(selectedItems, lead.id, seasons).collect { result ->
                        when (result) {
                            is State.Failed -> {
                                Log.e("showCountryUpdateDialog", "Failed to update countries: ${result.message}")
                                requireContext().toast(result.message)
                            }
                            is State.Loading -> {
                                Log.d("showCountryUpdateDialog", "Updating interested countries...")
                            }
                            is State.Success -> {
                                Log.d("showCountryUpdateDialog", "Successfully updated interested countries.")
                                selectedItems.clear()
                                chipGroupSelected.removeAllViews()
                                dialog.dismiss()
                                viewModel.fetchData(true)
                            }
                        }
                    }
                }
            } else {
                Log.d("showCountryUpdateDialog", "Yes button clicked with no selected items.")
                requireContext().toast("Please select an item to update")
            }
        }

        dialog.show()
        Log.d("showCountryUpdateDialog", "Dialog shown.")
    }


    private fun showIntakeUpdateDialog(lead: Lead) {
        selectedItems.clear()
        dialog.setCancelable(false)
        // Set dialog layout
        dialog.setContentView(R.layout.custom_update_dialog)
        val yesBtn = dialog.findViewById(R.id.button3) as Button
        val noBtn = dialog.findViewById(R.id.button6) as Button
        val title1 = dialog.findViewById<TextView>(R.id.title)
        val title2 = dialog.findViewById<TextView>(R.id.title2)
        val dropdownAutoComplete =
            dialog.findViewById<CustomAutoCompleteTextView>(R.id.dropdown_auto_complete)
        val chipGroupSelected =
            dialog.findViewById<ChipGroup>(R.id.chip_group_selected)

        val group = dialog.findViewById<Group>(R.id.group_appontment)
        val dateText = dialog.findViewById<EditText>(R.id.dateText)
        val timeText = dialog.findViewById<EditText>(R.id.timeText)
        val remarkText = dialog.findViewById<EditText>(R.id.remarkText)

        title1.text = "Update Intake"
        title2.text = "Intake"
        group.visibility = View.GONE

        noBtn.setOnClickListener {
            chipGroupSelected.removeAllViews()
            selectedItems.clear()
            dialog.dismiss()
        }


        this.lifecycleScope.launch {
            viewModel.intakeFilters.collect {
                when (it) {
                    is State.Failed -> {}
                    is State.Loading -> {}
                    is State.Success -> {

                        // Set up the AutoCompleteTextView with a custom ArrayAdapter
                        val adapter = OptionAdapter(
                            requireContext(),
                            it.data,
                            dropdownAutoComplete,
                            false,
                            true
                        ) { selected: Boolean, item: StatusItem, adapter: OptionAdapter ->
                            Log.d(
                                "TAG",
                                "showCountryUpdateDialog:selected::$selected::item::$item "
                            )
                            if (selected) {
                                selectedItems.add(item)
                                addChipToGroup(chipGroupSelected, item, false, adapter)
                            } else {
                                selectedItems.remove(item)
                                chipGroupSelected.removeView(
                                    chipGroupSelected.findViewWithTag<Chip>(
                                        item.id
                                    )
                                ) // Remove chip

                            }

                        }

                         val alreadyAdded = lead.goingto_intake?.toListOfStrings()

                        for (i in it.data) {
                            Log.d("TAG", "showCountryUpdateDialog:${lead.goingto_country} ")
                            Log.d("TAG", "showCountryUpdateDialog d:${i.country_name} ")
                            i.name.let { name ->
                                if (alreadyAdded?.contains(name) == true) {
                                    Log.d("TAG", "showIntakeUpdateDialog:lead.goingto_intake::${lead.goingto_intake}")
                                    Log.d("TAG", "showIntakeUpdateDialog:lead.goingto_intake::${i.name}")
                                    selectedItems.add(i)
                                }
                            }
                        }
                        adapter.updateList(selectedItems)
                        for (i in selectedItems) {
                            addChipToGroup(chipGroupSelected, i, false, adapter)
                        }


                        dropdownAutoComplete.setAdapter(adapter)

                    }
                }
            }
        }


        yesBtn.setOnClickListener {
            if (selectedItems.size > 0) {
                this.lifecycleScope.launch {

                    val countries = lead.goingto_country?.toListOfStrings() ?: emptyList()
                    viewModel.updateIntakes(selectedItems, lead.id, countries).collect {
                        when (it) {
                            is State.Failed -> {
                                requireContext().toast(it.message)
                            }

                            is State.Loading -> {}
                            is State.Success -> {
                                selectedItems.clear()
                                chipGroupSelected.removeAllViews()
                                dialog.dismiss()
                                viewModel.fetchData(true)
                            }
                        }
                    }
                }

            } else {
                requireContext().toast("Please select an item to update")
            }


        }





        dialog.show()


    }


    private fun showLabelUpdateDialog(lead: Lead) {
        selectedItems.clear()
        dialog.setCancelable(false)
        // Set dialog layout
        dialog.setContentView(R.layout.custom_update_dialog)
        val yesBtn = dialog.findViewById(R.id.button3) as Button
        val noBtn = dialog.findViewById(R.id.button6) as Button
        val title1 = dialog.findViewById<TextView>(R.id.title)
        val title2 = dialog.findViewById<TextView>(R.id.title2)
        val dropdownAutoComplete =
            dialog.findViewById<CustomAutoCompleteTextView>(R.id.dropdown_auto_complete)
        val chipGroupSelected =
            dialog.findViewById<ChipGroup>(R.id.chip_group_selected)

        val group = dialog.findViewById<Group>(R.id.group_appontment)
        val dateText = dialog.findViewById<EditText>(R.id.dateText)
        val timeText = dialog.findViewById<EditText>(R.id.timeText)
        val remarkText = dialog.findViewById<EditText>(R.id.remarkText)
        chipGroupSelected.visibility = View.GONE

        title1.text = "Update Label"
        title2.text = lead.student_label_name
        group.visibility = View.GONE

        noBtn.setOnClickListener {
            dialog.dismiss()
        }

        var selectedItem = ""


        this.lifecycleScope.launch {
            viewModel.labelFilters.collect {
                when (it) {
                    is State.Failed -> {}
                    is State.Loading -> {}
                    is State.Success -> {

                        // Set up the AutoCompleteTextView with a custom ArrayAdapter
                        val adapter = OptionAdapter(
                            requireContext(),
                            it.data,
                            dropdownAutoComplete,
                            false,
                            false
                        ) { selected: Boolean, item: StatusItem, adapter: OptionAdapter ->
                            Log.d(
                                "TAG",
                                "showCountryUpdateDialog:selected::$selected::item::$item "
                            )
                            if (selected) {
                                selectedItem = item.id
                                title2.text = item.name

                            }

                        }




                        dropdownAutoComplete.setAdapter(adapter)

                    }
                }
            }
        }


        yesBtn.setOnClickListener {

            this.lifecycleScope.launch {


                viewModel.updateLabel(lead.id, selectedItem).collect {
                    when (it) {
                        is State.Failed -> {
                            requireContext().toast(it.message)
                        }

                        is State.Loading -> {}
                        is State.Success -> {
                            selectedItems.clear()
                            chipGroupSelected.removeAllViews()
                            dialog.dismiss()
                            viewModel.fetchData(true)
                        }
                    }
                }
            }


        }





        dialog.show()


    }


    private fun showStatusUpdateDialog(lead: Lead) {
        selectedItems.clear()
        dialog.setCancelable(false)
        // Set dialog layout
        dialog.setContentView(R.layout.custom_update_dialog)
        val yesBtn = dialog.findViewById(R.id.button3) as Button
        val noBtn = dialog.findViewById(R.id.button6) as Button
        val title1 = dialog.findViewById<TextView>(R.id.title)
        val title2 = dialog.findViewById<TextView>(R.id.title2)
        val dropdownAutoComplete =
            dialog.findViewById<CustomAutoCompleteTextView>(R.id.dropdown_auto_complete)
        val chipGroupSelected =
            dialog.findViewById<ChipGroup>(R.id.chip_group_selected)

        val group = dialog.findViewById<Group>(R.id.group_appontment)
        val dateText = dialog.findViewById<EditText>(R.id.dateText)
        val dateTextTi = dialog.findViewById<TextInputLayout>(R.id.textInputLayoutDate)
        val timeText = dialog.findViewById<EditText>(R.id.timeText)
        val timeTextTi = dialog.findViewById<TextInputLayout>(R.id.textInputLayoutTime)
        val remarkText = dialog.findViewById<EditText>(R.id.remarkText)
        val textAreaTi = dialog.findViewById<TextInputLayout>(R.id.multi_line_input_layout)
        chipGroupSelected.visibility = View.GONE
        textAreaTi.visibility = View.VISIBLE
        remarkText.visibility = View.VISIBLE

        title1.text = "Update Status"
        title2.text = lead.public_student_status
        group.visibility = View.GONE

        noBtn.setOnClickListener {
            dialog.dismiss()
        }

        var selectedItem = ""
        var followUpSelected = false

        dateText.setOnClickListener {
            this.lifecycleScope.launch {
                val date = pickSingleDate(requireContext())
                if (!date.isNullOrEmpty()) {
                    dateText.setText(date)
                }

            }

        }

        dateTextTi.setOnClickListener {
            this.lifecycleScope.launch {
                val date = pickSingleDate(requireContext())
                if (!date.isNullOrEmpty()) {
                    dateText.setText(date)
                }

            }

        }

        timeTextTi.setOnClickListener {
            this.lifecycleScope.launch {
                val time = pickSingleTime(requireContext())
                if (!time.isNullOrEmpty()) {
                    timeText.setText(time)
                }
            }

        }
        timeText.setOnClickListener {
            this.lifecycleScope.launch {
                val time = pickSingleTime(requireContext())
                if (!time.isNullOrEmpty()) {
                    timeText.setText(time)
                }
            }

        }


        this.lifecycleScope.launch {
            viewModel.leadStatusFilters.collect {
                when (it) {
                    is State.Failed -> {}
                    is State.Loading -> {}
                    is State.Success -> {



                        // Set up the AutoCompleteTextView with a custom ArrayAdapter
                        val adapter = OptionAdapter(
                            requireContext(),
                            it.data,
                            dropdownAutoComplete,
                            false,
                            false
                        ) { selected: Boolean, item: StatusItem, adapter: OptionAdapter ->
                            Log.d(
                                "TAG",
                                "showCountryUpdateDialog:selected::$selected::item::$item "
                            )
                            if (selected) {
                                if (item.name == "Followup") {
                                    Log.d("TAG", "Followup: ")
                                    followUpSelected = true
                                    group.visibility = View.VISIBLE
                                    val dateTime = getCurrentDateTime()
                                    dateText.setText(dateTime.first)
                                    timeText.setText(dateTime.second)
                                    remarkText.visibility = View.VISIBLE

                                } else {
                                    Log.d("TAG", " No Followup: ")
                                    // remarkText.visibility = View.GONE
                                    group.visibility = View.GONE
                                }

                                selectedItem = item.id
                                title2.text = item.name

                            }

                        }




                        dropdownAutoComplete.setAdapter(adapter)

                    }
                }
            }
        }


        yesBtn.setOnClickListener {

            this.lifecycleScope.launch {
                if (followUpSelected) {
                    viewModel.updateStatusWithFollowup(
                        lead.id,
                        selectedItem,
                        dateText.text.toString(),
                        timeText.text.toString(),
                        remarkText.text.toString()
                    ).collect {
                        when (it) {
                            is State.Failed -> {
                                requireContext().toast(it.message)
                            }

                            is State.Loading -> {}
                            is State.Success -> {
                                selectedItems.clear()
                                chipGroupSelected.removeAllViews()
                                dialog.dismiss()
                                viewModel.fetchData(true)
                            }
                        }
                    }

                } else {
                    viewModel.updateStatus(lead.id, selectedItem).collect {
                        when (it) {
                            is State.Failed -> {
                                requireContext().toast(it.message)
                            }

                            is State.Loading -> {}
                            is State.Success -> {
                                selectedItems.clear()
                                chipGroupSelected.removeAllViews()
                                dialog.dismiss()
                                viewModel.fetchData(true)
                            }
                        }
                    }

                }


            }


        }





        dialog.show()


    }

    private fun getCurrentDateTime(): Pair<String, String> {
        // Get the current date and time
        val currentDateTime = LocalDateTime.now()

        // Define the format for the date (dd-MM-yyyy)
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        // Define the format for the time (HH.mm)
        val timeFormatter = DateTimeFormatter.ofPattern("HH.mm")

        // Format the current date and time
        val formattedDate = currentDateTime.format(dateFormatter)
        val formattedTime = currentDateTime.format(timeFormatter)

        return Pair(formattedDate, formattedTime)
    }


    // Function to add a Chip to the ChipGroup
    private fun addChipToGroup(
        chipGroup: ChipGroup,
        option: StatusItem,
        country: Boolean,
        adapter: OptionAdapter
    ) {
        // Determine the name based on the country flag
        val name = if (country) {
            Log.d("addChipToGroup", "Country flag is true, using country_name: ${option.country_name}")
            option.country_name
        } else {
            Log.d("addChipToGroup", "Country flag is false, using name: ${option.name}")
            option.name
        }

        // Create a new Chip and configure it
        val chip = Chip(requireContext()).apply {
            Log.d("addChipToGroup", "Creating a new Chip with tag: ${option.id} and text: $name")

            tag = option.id
            text = name
            isCloseIconVisible = true

            // Set a click listener for the close icon
            setOnCloseIconClickListener {
                Log.d("addChipToGroup", "Close icon clicked for chip with tag: $tag")

                // Remove the Chip from the ChipGroup
                chipGroup.removeView(this)
                Log.d("addChipToGroup", "Chip removed from ChipGroup")

                // Check if the option is in selectedItems and remove it
                if (selectedItems.find { it.id == option.id } != null) {
                    Log.d("addChipToGroup", "Option found in selectedItems, removing it: $option")
                    selectedItems.remove(option)

                    // Update the adapter with the new selectedItems list
                    adapter.updateList(selectedItems)
                    Log.d("addChipToGroup", "Adapter updated with selectedItems: $selectedItems")

                    // If there are no more chips, hide the ChipGroup
                    if (chipGroup.childCount == 0) {
                        chipGroup.visibility = View.GONE
                        Log.d("addChipToGroup", "ChipGroup is empty, hiding it")
                    }
                } else {
                    Log.d("addChipToGroup", "Option not found in selectedItems, no removal needed")
                }
            }
        }

        // Add the newly created Chip to the ChipGroup
        chipGroup.addView(chip)
        Log.d("addChipToGroup", "Chip added to ChipGroup, current child count: ${chipGroup.childCount}")

        // If the ChipGroup has one or more chips, make it visible
        if (chipGroup.childCount > 0) {
            chipGroup.visibility = View.VISIBLE
            Log.d("addChipToGroup", "ChipGroup made visible")
        } else {
            Log.d("addChipToGroup", "ChipGroup remains invisible")
        }
    }


    override fun onStop() {
        viewModel.resetFilters()
        super.onStop()
    }
}