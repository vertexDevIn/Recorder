package `in`.vertexdev.mobile.call_rec.ui.frags.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.databinding.FragmentCallLogsBinding
import `in`.vertexdev.mobile.call_rec.room.database.AppDatabase
import `in`.vertexdev.mobile.call_rec.rv.CallLogsRv
import `in`.vertexdev.mobile.call_rec.util.Utils.toast
import kotlinx.coroutines.launch


class CallLogsFragment : Fragment() {
    private var _binding: FragmentCallLogsBinding? = null
    private val binding get() = _binding!!



    private val logsAdapter by lazy {
        CallLogsRv(
            onViewError =  this::onViewError,

        )
    }

    private fun onViewError(s: String) {
        requireContext().toast(s)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallLogsBinding.inflate(inflater, container, false)
        setUpRvs()
        requireActivity().findViewById<FloatingActionButton>(R.id.floatingActionButton).visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch() {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    val uploadedCallLogDao =
                        AppDatabase.getDatabase(requireContext()).uploadedCallLogDao()
                    uploadedCallLogDao.getAllUploadedCallLogs().collect(){
                        Log.d("TAG", "getAllUploadedCallLogs: $it")
                        logsAdapter.updateData(it)
                    }
                }
            }
        }
        return binding.root
    }

    private fun setUpRvs() {

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.rvLogs.layoutManager = layoutManager
        binding.rvLogs.adapter = logsAdapter

    }

}