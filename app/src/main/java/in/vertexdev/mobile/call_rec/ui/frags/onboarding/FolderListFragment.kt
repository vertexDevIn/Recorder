package `in`.vertexdev.mobile.call_rec.ui.frags.onboarding

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.databinding.FragmentFolderListBinding
import `in`.vertexdev.mobile.call_rec.databinding.FragmentSelectFolderBinding
import `in`.vertexdev.mobile.call_rec.models.Folder
import `in`.vertexdev.mobile.call_rec.rv.FolderAdapter
import `in`.vertexdev.mobile.call_rec.util.State
import `in`.vertexdev.mobile.call_rec.util.Utils.toast
import `in`.vertexdev.mobile.call_rec.viewModel.AuthViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File


class FolderListFragment : Fragment() {

    private var _binding: FragmentFolderListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()
    private lateinit var dialog: Dialog


    private val folderAdapter by lazy {
        FolderAdapter(
            this::onFolderClick,
        )
    }

    private fun onFolderClick(folder: Folder) {
        Log.d("TAG", "onFolderClick::folderPath::${folder.folderPath} ")
        viewModel.updateCurrentFolderPath(folder.folderPath)

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentFolderListBinding.inflate(inflater, container, false)
        View.GONE
        dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setUpRecyclerView()

        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch() {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentFolderPath.collect { path ->
                        val folders = viewModel.getFolders()
                        setDataToUi(folders)
                        binding.useThis.isEnabled =
                            path != Environment.getExternalStorageDirectory().path
                        val rootPath = Environment.getExternalStorageDirectory().path
                        if (path == rootPath) {
                            binding.materialToolbar.title =
                                "Select Folder" // Set title for the root folder
                            binding.materialToolbar.navigationIcon =
                                null // Hide back arrow when in the root folder
                        } else {
                            binding.materialToolbar.title =
                                File(path).name // Set title to the folder name
                            binding.materialToolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24) // Show back arrow
                            binding.materialToolbar.setNavigationOnClickListener {
                                viewModel.goBackHandled() // Handle back navigation
                            }
                        }
                    }
                }
            }
        }

        binding.useThis.setOnClickListener {
            showFolderConfirmDialog()
        }

        return binding.root
    }

    private fun showFolderConfirmDialog() = this.lifecycleScope.launch {
        val currentFolderPath = viewModel.currentFolderPath.first()
        val folderName = File(currentFolderPath).name
        dialog.setCancelable(false)
        // Set dialog layout
        dialog.setContentView(R.layout.dialog_confirm_folder)
        val yesBtn = dialog.findViewById(R.id.button4) as Button
        val noBtn = dialog.findViewById(R.id.button66) as Button
        val title = dialog.findViewById(R.id.textView6) as TextView
        val description = dialog.findViewById(R.id.textView7) as TextView

        title.text = "Confirm $folderName "
        description.text =
            "Please confirm that the $folderName folder is the location where call recordings will be saved"


        yesBtn.setOnClickListener {
            this@FolderListFragment.lifecycleScope.launch {
                viewModel.saveFolderPath(currentFolderPath).collect{
                    when(it){
                        is State.Failed -> {requireContext().toast(it.message)}
                        is State.Loading ->{}
                        is State.Success -> {
                            dialog.dismiss()
                            findNavController().navigate(R.id.action_folderListFragment_to_main)
                        }
                    }

                }

            }
        }

        noBtn.setOnClickListener {
            dialog.dismiss()
        }

        // Display the dialog
        dialog.show()

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Create a callback for back press handling
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Call the ViewModel's goBack function to navigate back
                val handled = viewModel.goBackHandled()
                if (!handled) {
                    requireActivity().finish()
                }
            }
        }

        // Add the callback to the back press dispatcher
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)


    }


    private fun setUpRecyclerView() {
        val layoutManager = GridLayoutManager(requireContext(), 3) // 3 columns

        binding.rvFolder.layoutManager = layoutManager
        binding.rvFolder.adapter = folderAdapter

    }

    private fun setDataToUi(folders: List<Folder>) {
        Log.d("TAG", "setDataToUi:folders::$folders ")
        if (folders.isEmpty()) {
            binding.noFolder.visibility = View.VISIBLE
        } else {
            binding.noFolder.visibility = View.INVISIBLE
        }
        folderAdapter.updateData(folders)
    }


}