package `in`.vertexdev.mobile.call_rec.ui.frags.main

import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.databinding.FragmentHomeBinding
import `in`.vertexdev.mobile.call_rec.models.Items
import `in`.vertexdev.mobile.call_rec.models.LeadCategory
import `in`.vertexdev.mobile.call_rec.rv.ItemAdapter

import `in`.vertexdev.mobile.call_rec.services.SyncDataService2
import `in`.vertexdev.mobile.call_rec.util.State
import `in`.vertexdev.mobile.call_rec.util.Utils.generateDummyItems
import `in`.vertexdev.mobile.call_rec.util.Utils.isServiceRunning
import `in`.vertexdev.mobile.call_rec.util.Utils.toast
import `in`.vertexdev.mobile.call_rec.viewModel.AuthViewModel
import `in`.vertexdev.mobile.call_rec.viewModel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class HomeFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var startDate:String = ""
    private var endDate:String = ""
    private var setChipForData1 = ""
    private var setChipForData2 = ""

    private val myTaskAdapter by lazy {
        ItemAdapter(
            this::onMyTaskClicked,
        )
    }

    private fun onMyTaskClicked(items: LeadCategory) {
        val action = HomeFragmentDirections.actionHomeFragmentToLogsFragment()
        action.tagStudent = "lead"
        action.title = items.name + " leads"
        action.fromAction = true
        action.cardId = items.id
        action.startDate = startDate
        action.endDate = endDate
        findNavController().navigate(action)

    }

    private val studentAdapter by lazy {
        ItemAdapter(
            this::onStudentClicked,
        )
    }

    private fun onStudentClicked(items: LeadCategory) {

        val action = HomeFragmentDirections.actionHomeFragmentToLogsFragment()
        action.tagStudent = ""
        action.title = items.name + "Student"
        action.fromAction = true
        action.cardId = items.id
        action.startDate = startDate
        action.endDate = endDate
        findNavController().navigate(action)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setUpRvs()


       // myTaskAdapter.updateData(tasks as ArrayList<Items>)

        requireActivity().findViewById<FloatingActionButton>(R.id.floatingActionButton).visibility =
            View.VISIBLE

        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
            View.VISIBLE

        requireActivity().findViewById<FloatingActionButton>(R.id.floatingActionButton).setOnClickListener {

            val action =  HomeFragmentDirections.actionHomeFragmentToAddLeadFragment(true)
            findNavController().navigate(action)
        }

        val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        Calendar.getInstance()
        startDate = formatter.format(Calendar.getInstance().time)
        endDate =  formatter.format(Calendar.getInstance().time)

        binding.swipeRefreshLayout.setOnRefreshListener {
            this.lifecycleScope.launch {
                getAllData()
            }

        }


        viewLifecycleOwner.lifecycleScope.launch() {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch{
                   viewModel.getData1("today")
                    binding.chip.isChecked = true
                    setChipForData1 = "today"
                    setChipForData2 = "today"
                }
                launch {
                    viewModel.getData2("today")
                }
                launch {
                    viewModel.leadCategory.collect{
                        when(it){
                            is State.Failed -> {
                                requireContext().toast(it.message)
                                myTaskAdapter.updateData(ArrayList())
                            }
                            is State.Loading -> {

                            }
                            is State.Success -> {
                                Log.d("dsdsdsds", "leadCategory:${it.data} ")
                                val availableCount = it.data.filter { it.count != "0"}
                                myTaskAdapter.updateData(availableCount as ArrayList<LeadCategory>)
                                binding.swipeRefreshLayout.isRefreshing = false

                            }
                        }
                    }
                }
                launch {
                    viewModel.studentCategory.collect{
                        when(it){
                            is State.Failed -> {
                                requireContext().toast(it.message)
                                studentAdapter.updateData(ArrayList())
                            }
                            is State.Loading -> {

                            }
                            is State.Success -> {
                                Log.d("dsdsdsds", "studentCategory:${it.data} ")
                                val availableCount = it.data.filter { it.count != "0"}
                                studentAdapter.updateData(availableCount as ArrayList<LeadCategory>)
                                binding.swipeRefreshLayout.isRefreshing = false

                            }
                        }
                    }

                }
                launch {
                    viewModel.getAndSaveServerInfo()
                }

            }
        }
        binding.buttonff.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToLogsFragment()
            action.tagStudent = ""
            action.fromAction = true
            action.startDate = startDate
            action.endDate = endDate
            action.title = "All Students"
            findNavController().navigate(action)
        }

        binding.chipGroup.setOnCheckedChangeListener { group, checkedId ->
            when(checkedId){
                R.id.chip->{
                    viewModel.getData1("today")
                    viewModel.getData2("today")
                    setDates("today")
                    setChipForData1 = "today"
                    setChipForData2 = "today"
                }
                R.id.chip2->{

                    viewModel.getData1("last_7_days")
                    viewModel.getData2("last_7_days")
                    setDates("last_7_days")
                    setChipForData1 = "last_7_days"
                    setChipForData2 = "last_7_days"
                }
                R.id.chip3->{
                    viewModel.getData1("last_30_days")
                    viewModel.getData2("last_30_days")
                    setDates("last_30_days")
                    setChipForData1 = "last_30_days"
                    setChipForData2 = "last_30_days"

                }
                R.id.chip4->{
                    viewModel.getData1("last_6_months")
                    viewModel.getData2("last_6_months")
                    setDates("last_6_months")
                    setChipForData1 = "last_6_months"
                    setChipForData2 = "last_6_months"
                }

            }
        }

        lifecycleScope.launch {
            val context = requireContext()

            if (!isServiceRunning(context, SyncDataService2::class.java)) {
                // Start the service if it is not running
                val serviceIntent = Intent(context, SyncDataService2::class.java).apply {
                    action = SyncDataService2.Actions.START.toString()
                }
                context.startForegroundService(serviceIntent)
            } else {
                // Stop the service and restart it
                val stopIntent = Intent(context, SyncDataService2::class.java).apply {
                    action = SyncDataService2.Actions.STOP.toString()
                }
                context.startService(stopIntent)

                // Suspend coroutine to wait for a short moment
                delay(500) // Delay in milliseconds to ensure the service is stopped

                // Restart the service
                val restartIntent = Intent(context, SyncDataService2::class.java).apply {
                    action = SyncDataService2.Actions.START.toString()
                }
                context.startForegroundService(restartIntent)
            }
        }


        binding.button.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToLogsFragment()
            action.tagStudent = "lead"
            action.fromAction =true
            action.title = "All leads"
            action.startDate = startDate
            action.endDate = endDate
            findNavController().navigate(action)

        }

        return binding.root
    }

    private suspend fun getAllData(){
        viewModel.getData1(setChipForData1)
        viewModel.getData2(setChipForData2)

    }

    private fun setDates(s: String) {
        val toDate = Calendar.getInstance()
        val fromDate = toDate.clone() as Calendar

        when (s) {
            "today" -> {
                // No changes needed for today's date
            }

            "last_7_days" -> fromDate.add(Calendar.DAY_OF_YEAR, -7)
            "last_30_days" -> fromDate.add(Calendar.DAY_OF_YEAR, -30)
            "last_6_months" -> fromDate.add(Calendar.MONTH, -6)
            else -> throw IllegalArgumentException("Invalid filter: $s")
        }
        val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
         startDate = if(s == "today"){
             formatter.format(fromDate.time)
         }else{
             formatter.format(fromDate.time)
         }
        endDate = formatter.format(toDate.time)

    }

    private fun setUpRvs() {
        val layoutManager1 = LinearLayoutManager(requireContext())
        layoutManager1.orientation = LinearLayoutManager.HORIZONTAL

        val layoutManager2 = LinearLayoutManager(requireContext())
        layoutManager2.orientation = LinearLayoutManager.HORIZONTAL

        binding.rvLeads.layoutManager = layoutManager1
        binding.rvLeads.adapter = myTaskAdapter

        binding.recyclerView2.layoutManager = layoutManager2
        binding.recyclerView2.adapter = studentAdapter
    }


}