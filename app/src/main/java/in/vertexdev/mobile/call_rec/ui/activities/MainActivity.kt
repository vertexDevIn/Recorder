package `in`.vertexdev.mobile.call_rec.ui.activities

import android.Manifest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import `in`.vertexdev.mobile.call_rec.R
import `in`.vertexdev.mobile.call_rec.util.Utils
import `in`.vertexdev.mobile.call_rec.viewModel.AuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController
    private var loggedIn: Boolean = false
    private var allPermissionGranted: Boolean = false
    private var authenticated: Boolean = false
    private var folderSelected: Boolean = false

    private val viewModel: AuthViewModel by viewModels()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.lifecycleScope.launch {

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
            if(Utils.areAllPermissionsGranted(this@MainActivity, permissionToCheckAre)){
                allPermissionGranted = true
                viewModel.saveAllPermissionGranted(true)


            }else{
                allPermissionGranted =false
                viewModel.saveAllPermissionGranted(false)
            }
            loggedIn = viewModel.datastore.first().isLoggedIn

            authenticated = viewModel.datastore.first().authenticated
            folderSelected = viewModel.datastore.first().folderSelected


            // Determine which navigation graph to use based on user state
            val startGraph = if (!loggedIn || !authenticated || !allPermissionGranted || !folderSelected) {
                // If any of the required conditions are not met, use the onboarding graph
                R.navigation.onboarding // Ensure this resource exists
            } else {
                // If all conditions are met, use the main graph
                R.navigation.main // Ensure this resource exists
            }

            navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

            navController = navHostFragment.navController


            val navGraph = navController.navInflater.inflate(startGraph)

            // If using the onboarding graph, modify the start destination based on conditions
            if (startGraph == R.navigation.onboarding) {
                when {
                    !authenticated -> {
                        // If not authenticated, set start destination to loginFragment and logout user
                        navGraph.setStartDestination(R.id.loginFragment)
                        navController.graph = navGraph
                        viewModel.logout() // Implement logout logic in your ViewModel
                    }
                    !allPermissionGranted -> {
                        // If permissions are not granted, set start destination to permissionsFragment
                        navGraph.setStartDestination(R.id.permissionFragment)
                        navController.graph = navGraph
                    }
                    !folderSelected -> {
                        // If folder is not selected, set start destination to selectFolderFragment
                        navGraph.setStartDestination(R.id.selectFolderFragment)
                        navController.graph = navGraph
                    }
                    else -> {
                        // If all conditions are met, set start destination to mainFragment (if exists in onboarding graph)
                       val navGraph2  = navController.navInflater.inflate(R.navigation.main)
                        navController.graph = navGraph2

                    }
                }
            }else{
                navController.graph = navGraph
            }

            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
           // bottomNavigationView.setupWithNavController(navController)

            // Override the navigation behavior for specific items
            bottomNavigationView.setOnItemSelectedListener { item ->
                when (item.itemId) {

                    R.id.allLeads -> {
                        val bundle = bundleOf(
                            "tagStudent" to "lead",
                            "title" to "All Leads"
                        )
                        navController.navigate(R.id.logsFragment, bundle)
                        true

                    }

                    R.id.homeFragment -> {
                        navController.navigate(R.id.homeFragment)
                        true
                    }

                    R.id.allStudent -> {
                        val bundle = bundleOf(
                            "tagStudent" to "",
                            "title" to "All Students"
                        )
                        navController.navigate(R.id.logsFragment,bundle)
                        true
                    }  R.id.profileFragment -> {
                        navController.navigate(R.id.profileFragment)
                        true
                    }
                    R.id.callLogsFragment->{
                        navController.navigate(R.id.callLogsFragment)
                        true

                    }


                    else -> {
                        true
                    }
                }
            }


        }


    }
}