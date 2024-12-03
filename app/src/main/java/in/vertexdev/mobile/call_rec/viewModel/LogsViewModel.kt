package `in`.vertexdev.mobile.call_rec.viewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.vertexdev.mobile.call_rec.models.Employee
import `in`.vertexdev.mobile.call_rec.models.Lead
import `in`.vertexdev.mobile.call_rec.models.LeadCategory
import `in`.vertexdev.mobile.call_rec.models.StatusItem
import `in`.vertexdev.mobile.call_rec.repo.AuthRepository
import `in`.vertexdev.mobile.call_rec.repo.DataStoreRepo
import `in`.vertexdev.mobile.call_rec.util.State
import `in`.vertexdev.mobile.call_rec.util.Utils.parseLeads
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

// AuthViewModel.kt
@HiltViewModel
class LogsViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val dataStore: DataStoreRepo,
) : ViewModel() {


    var multipleFilterActive: Boolean = false
    private val _leadChips = MutableStateFlow<State<List<LeadCategory>>>(State.loading())
    val leadChips: StateFlow<State<List<LeadCategory>>> = _leadChips.asStateFlow()

    private val _leadList = MutableStateFlow<State<List<Lead>>>(State.loading())
    val leadList: StateFlow<State<List<Lead>>> = _leadList.asStateFlow()

    //##########################
    var startDate: String? = null
    var endDate: String? = null
    var tagStudent: String = ""
    var cardId: String? = null
    var searchQuery: String = ""
    private var currentNo = "1"
    private var itemsPerPage = "0"
    private var totalItems = 0
    var moreItemsAvailable = true
    var fetchInProgress = false
    var filterLeadStatus: List<String>? = null
    var filterBranch: List<String>? = null
    var filterSource: List<String>? = null
    var filterLabel: List<String>? = null
    var filterCountry: List<String>? = null
    var filterSeasons: List<String>? = null
    //######################################


    val datastore = dataStore.readDataStoreValue


    fun getLeadChips(startDate: String?, endDate: String?) = viewModelScope.launch(Dispatchers.IO) {
        val apiKey = datastore.first().apiKey
        val userId: String = datastore.first().userId
        Log.d("TAG", "getLeadChips: apiKey::$apiKey")

        val response = repository.getApiData(apiKey, tagStudent, listOf(userId), startDate, endDate)
        if (response != null) {
            Log.d("TAG", "getLeadChips:${response.body()} ")
            if (response.isSuccessful) {
                val catData = response.body()
                if (catData != null) {
                    _leadChips.value = State.Success(catData)


                } else {
                    _leadChips.value = State.Failed("Data is null")
                }

            } else {
                _leadChips.value = State.Failed(response.message())
            }
        }

    }

    fun reloadLeadChips() {
        getLeadChips(startDate, endDate)

    }

    fun fetchData(first: Boolean) = viewModelScope.launch(Dispatchers.IO) {


        val currentLeads = (_leadList.value as? State.Success)?.data ?: emptyList()
        _leadList.value = State.loading()

        fetchInProgress = true
        if (first) {
            moreItemsAvailable = true
            currentNo = "1"
            itemsPerPage = "0"
        }
        val apiKey = datastore.first().apiKey
        val userId = datastore.first().userId

        filterLeadStatus =
            cardId?.takeIf { it.isNotEmpty() }?.let { listOf(it) } ?: filterLeadStatus
        Log.d("TAG", "fetchData:filterLeadStatus$filterLeadStatus ")


        val searchParam =
            if (searchQuery.isEmpty()) "" else "&search=" + Uri.encode(
                searchQuery
            )

        // Logging all variables
        Log.d("LogViewModel", "fetchData:first::$first ")
        Log.d("LogViewModel", "startDate: $startDate")
        Log.d("LogViewModel", "endDate: $endDate")
        Log.d("LogViewModel", "tagStudent: $tagStudent")
        Log.d("LogViewModel", "cardId: $cardId")
        Log.d("LogViewModel", "searchQuery: $searchQuery")
        Log.d("LogViewModel", "moreItemsAvailable: $moreItemsAvailable")
        Log.d("LogViewModel", "fetchInProgress: $fetchInProgress")
        Log.d("LogViewModel", "filterLeadStatus: ${filterLeadStatus?.joinToString()}")
        Log.d("LogViewModel", "filterBranch: ${filterBranch?.joinToString()}")
        Log.d("LogViewModel", "filterSource: ${filterSource?.joinToString()}")
        Log.d("LogViewModel", "filterLabel: ${filterLabel?.joinToString()}")
        Log.d("LogViewModel", "filterCountry: ${filterCountry?.joinToString()}")
        Log.d("LogViewModel", "filterSeasons: ${filterSeasons?.joinToString()}")
        Log.d("LogViewModel", "currentPageCount:$currentNo ")
        Log.d("LogViewModel", "itemsPerPage:$itemsPerPage ")


        val response = repository.fetchData(
            publicApiKey = apiKey,
            tagStudent = tagStudent,
            filterFromDate = startDate,
            filterToDate = endDate,
            filterData = 1,
            pageNo = currentNo,
            filterAdminIds = listOf(userId),
            filterLeadStatus = filterLeadStatus,
            search = searchParam,
            filterBranch = filterBranch,
            filterSource = filterSource,
            filterLabel = filterLabel,
            filterCountry = filterCountry,
            filterSeasons = filterSeasons,
            pageCount = itemsPerPage

        )

        val responseString = response.string()
        Log.d("TAG", "fetchData:$responseString ")

        if (responseString.contains("Developer Error")) {
            // Handle the error case
            if (first) {
                fetchInProgress = false
                _leadList.value = State.Success(listOf())
            }


        } else {
            // Parse the response as a list of leads
            if (responseString.isNotEmpty()) {
                val leads: List<Lead>? = parseLeads(responseString)
                if (leads != null) {

                    Log.d("TAG", "fetchData: leads   ::$leads ")
                    if (leads.size < 20) {
                        moreItemsAvailable = false
                        itemsPerPage = "0"

                    } else {
                        currentNo = (currentNo.toInt() + 1).toString()

                        val t = datastore.first().pageCount.toInt()

                        itemsPerPage = (itemsPerPage.toInt() + t).toString()
                        Log.d("TAG", "fetchDataitemsPerPage:$itemsPerPage ")
                    }


                    if (first) {
                        _leadList.value = State.Success(leads)
                    } else {

                        Log.d("TAG", "fetchData:currentLeads:$currentLeads ")
                        Log.d("TAG", "fetchData:leads:$leads ")
                        val updatedLeads =
                            currentLeads + leads // Append new leads to the existing list
                        Log.d("TAG", "fetchData:updatedLeads:$updatedLeads ")
                        _leadList.value = State.success(updatedLeads) // Update with the new list
                    }
                    fetchInProgress = false
                } else {
                    fetchInProgress = false
                    _leadList.value = State.Success(listOf())
                }

            } else {
                fetchInProgress = false
                _leadList.value = State.Success(listOf())
            }


        }


    }


    private val _leadStatusFilters = MutableStateFlow<State<List<StatusItem>>>(State.loading())
    val leadStatusFilters: StateFlow<State<List<StatusItem>>> = _leadStatusFilters.asStateFlow()

    private val _labelFilters = MutableStateFlow<State<List<StatusItem>>>(State.loading())
    val labelFilters: StateFlow<State<List<StatusItem>>> = _labelFilters.asStateFlow()


    private val _sourceFilters = MutableStateFlow<State<List<StatusItem>>>(State.loading())
    val sourceFilters: StateFlow<State<List<StatusItem>>> = _sourceFilters.asStateFlow()


    private val _countryFilters = MutableStateFlow<State<List<StatusItem>>>(State.loading())
    val countryFilters: StateFlow<State<List<StatusItem>>> = _countryFilters.asStateFlow()


    private val _intakeFilters = MutableStateFlow<State<List<StatusItem>>>(State.loading())
    val intakeFilters: StateFlow<State<List<StatusItem>>> = _intakeFilters.asStateFlow()


    private val _branchFilters = MutableStateFlow<State<List<StatusItem>>>(State.loading())
    val branchFilters: StateFlow<State<List<StatusItem>>> = _branchFilters.asStateFlow()

    private val _employees = MutableStateFlow<State<List<Employee>>>(State.loading())
    val employees: StateFlow<State<List<Employee>>> = _employees.asStateFlow()


    private fun getGetLeadFilters(tagStudent: String) = viewModelScope.launch(Dispatchers.IO) {
        val apiKey = datastore.first().apiKey
        val response = repository.getFilters("customer.api", "fetch.status.api", apiKey, tagStudent)
        if (response.isSuccessful) {
            Log.d("TAG", "getFilters: ${response.body()}")
            val data = response.body()
            if (data != null) {
                _leadStatusFilters.value = (State.success(data))
            } else {
                _leadStatusFilters.value = (State.failed("null"))
            }

        } else {
            Log.d("TAG", "getFilters: failed::${response.message()}")
            _leadStatusFilters.value = (State.Failed(response.message()))
        }

    }

    private fun getLabelFilters() = viewModelScope.launch(Dispatchers.IO) {
        val apiKey = datastore.first().apiKey
        val response = repository.getFilters("customer.labels.api", "fetch.api", apiKey, null)
        if (response.isSuccessful) {
            Log.d("TAG", "getFilters: ${response.body()}")
            val data = response.body()
            if (data != null) {
                _labelFilters.value = (State.success(data))
            } else {
                _labelFilters.value = (State.failed("null"))
            }

        } else {
            Log.d("TAG", "getFilters: failed::${response.message()}")
            _labelFilters.value = (State.Failed(response.message()))
        }

    }

    private fun getSourceFilters() = viewModelScope.launch(Dispatchers.IO) {

        val apiKey = datastore.first().apiKey
        val response = repository.getFilters("customer.api", "fetch.source.api", apiKey, null)
        if (response.isSuccessful) {
            Log.d("TAG", "getFilters: ${response.body()}")
            val data = response.body()
            if (data != null) {
                _sourceFilters.value = (State.success(data))
            } else {
                _sourceFilters.value = (State.failed("null"))
            }

        } else {
            Log.d("TAG", "getFilters: failed::${response.message()}")
            _sourceFilters.value = (State.Failed(response.message()))
        }

    }

    private fun getCountriesFilters() = viewModelScope.launch(Dispatchers.IO) {

        val apiKey = datastore.first().apiKey
        val response = repository.getFilters("countries.api", "fetch.api", apiKey, null)
        if (response.isSuccessful) {
            Log.d("TAG", "getFilters: ${response.body()}")
            val data = response.body()
            if (data != null) {
                _countryFilters.value = (State.success(data))
            } else {
                _countryFilters.value = (State.failed("null"))
            }

        } else {
            Log.d("TAG", "getFilters: failed::${response.message()}")
            _countryFilters.value = (State.Failed(response.message()))
        }

    }

    private fun getIntakeFilters() = viewModelScope.launch(Dispatchers.IO) {

        val apiKey = datastore.first().apiKey
        val response = repository.getFilters("intakes.api", "fetch.api", apiKey, null)
        if (response.isSuccessful) {
            Log.d("TAG", "getFilters: ${response.body()}")
            val data = response.body()
            if (data != null) {
                _intakeFilters.value = (State.success(data))
            } else {
                _intakeFilters.value = (State.failed("null"))
            }

        } else {
            Log.d("TAG", "getFilters: failed::${response.message()}")
            _intakeFilters.value = (State.Failed(response.message()))
        }

    }

    private fun getBranchesFilters() = viewModelScope.launch(Dispatchers.IO) {

        val apiKey = datastore.first().apiKey
        val response = repository.getFilters("branches.api", "fetch.api", apiKey, null)
        if (response.isSuccessful) {
            Log.d("TAG", "getFilters: ${response.body()}")
            val data = response.body()
            if (data != null) {
                _branchFilters.value = (State.success(data))
            } else {
                _branchFilters.value = (State.failed("null"))
            }

        } else {
            Log.d("TAG", "getFilters: failed::${response.message()}")
            _branchFilters.value = (State.Failed(response.message()))
        }

    }


    fun getAllFilters() = viewModelScope.launch(Dispatchers.IO) {
        getGetLeadFilters(tagStudent)
        getLabelFilters()
        getSourceFilters()
        getCountriesFilters()
        getIntakeFilters()
        getBranchesFilters()
        fetchEmployees()


    }


    fun resetFilters(

    ) = viewModelScope.launch(Dispatchers.IO) {
        cardId = null
        filterLeadStatus = null
        filterBranch = null
        filterSource = null
        filterLabel = null
        filterCountry = null
        filterSeasons = null
        currentNo = "1"
        itemsPerPage = "0"
        searchQuery = ""


    }

    fun updateInterestedCountries(
        selectedItems: ArrayList<StatusItem>,
        id: String,
        season: List<String>
    ) =
        flow<State<Boolean>> {
            emit(State.loading())
            val apiKey = datastore.first().apiKey
            val countries = mutableListOf<String>() // Mutable list of strings
            for (i in selectedItems) {
                i.country_name?.let { countries.add(it) }
            }

            val respose = repository.updateInterestedCountries(
                apiServiceId = "customer.api",
                apiCommand = "update.interested.api",
                publicApiKey = apiKey,
                tagStudent = tagStudent,
                userId = id,
                countryParams = countries,
                season = season

            ).execute()

            if (respose.isSuccessful) {
                emit(State.success(true))
            } else {
                emit(State.failed(respose.message()))
            }

        }.flowOn(Dispatchers.IO)

    //updateIntakes
    fun updateIntakes(selectedItems: ArrayList<StatusItem>, id: String, countries: List<String>) =
        flow<State<Boolean>> {
            emit(State.loading())
            val apiKey = datastore.first().apiKey
            val seasons = mutableListOf<String>() // Mutable list of strings
            for (i in selectedItems) {
                i.name?.let { seasons.add(it) }
            }

            val respose = repository.updateIntakes(
                apiServiceId = "customer.api",
                apiCommand = "update.interested.api",
                publicApiKey = apiKey,
                tagStudent = tagStudent,
                userId = id,
                season = seasons,
                countryParams = countries

            ).execute()

            if (respose.isSuccessful) {
                emit(State.success(true))
            } else {
                emit(State.failed(respose.message()))
            }

        }.flowOn(Dispatchers.IO)


    fun updateLabel(id: String, columnValue: String) =
        flow<State<Boolean>> {
            emit(State.loading())
            val apiKey = datastore.first().apiKey


            val respose = repository.updateLabel(
                apiServiceId = "customer.api",
                apiCommand = "update.api",
                publicApiKey = apiKey,
                tagStudent = tagStudent,
                columnValue = columnValue,
                id = id


            )

            if (respose.isSuccessful) {
                emit(State.success(true))
            } else {
                emit(State.failed(respose.message()))
            }

        }.flowOn(Dispatchers.IO)
    //updateStatus

    fun updateStatus(userId: String, statusId: String, note: String) =
        flow<State<Boolean>> {
            emit(State.loading())
            val apiKey = datastore.first().apiKey


            val respose = repository.updateStatus(
                apiServiceId = "customer.api",
                apiCommand = "update.student.status.api",
                publicApiKey = apiKey,
                tagStudent = tagStudent,
                statusId = statusId,
                userId = userId,
                note = note


            )

            if (respose.isSuccessful) {
                reloadLeadChips()
                emit(State.success(true))

            } else {
                emit(State.failed(respose.message()))
            }

        }.flowOn(Dispatchers.IO)


    fun updateStatusWithFollowup(
        userId: String,
        leadStatus: String,
        followupDate: String,
        followupTime: String,
        additionalNote: String
    ) =
        flow<State<Boolean>> {
            emit(State.loading())
            val apiKey = datastore.first().apiKey


            val respose = repository.updateStatusWithFollowup(
                apiServiceId = "customer.api",
                apiCommand = "update.student.status.api",
                publicApiKey = apiKey,
                tagStudent = tagStudent,
                userId = userId,
                leadStatus = leadStatus,
                followupDate = followupDate,
                followupTime = followupTime,
                additionalNote = additionalNote


            )

            if (respose.isSuccessful) {
                reloadLeadChips()
                emit(State.success(true))
            } else {
                emit(State.failed(respose.message()))
            }

        }.flowOn(Dispatchers.IO)

    fun search(it: String) = viewModelScope.launch(Dispatchers.IO) {
        try {

            val apiKey = datastore.first().apiKey

            val response = repository.search(apiKey, it, tagStudent, "0")


            if (response.isSuccessful) {
                val responseString = response.body()!!.string()
                Log.d("TAG", "search:responseString:$responseString ")

                if (responseString.contains("Developer Error")) {
                    // Handle the error case

                    fetchInProgress = false
                    _leadList.value = State.Success(listOf())
                } else {
                    // Parse the response as a list of leads
                    if (responseString.isNotEmpty()) {
                        val leads: List<Lead>? = parseLeads(responseString)
                        if (leads != null) {
                            if (leads.size < 20) {
                                moreItemsAvailable = false

                            } else {
                                currentNo = (currentNo.toInt() + 1).toString()
                            }

                            Log.d("TAG", "search: leads   ::$leads ")


                            _leadList.value = State.Success(leads)

                        } else {
                            _leadList.value = State.Success(listOf())
                        }

                    } else {

                        _leadList.value = State.Success(listOf())
                    }


                }

            }


        } catch (e: Exception) {
            Log.d("TAG", "search:Exception:$e ")
        }

    }


    fun fetchEmployees() = viewModelScope.launch(Dispatchers.IO) {
        val apiKey = datastore.first().apiKey
        val response = repository.getEmployees(apiKey)
        if (response.isSuccessful) {
            Log.d("TAG", "fetchEmployees:Sucesssfull:${response.body()} ")
            if (response.body() != null) {
                _employees.value = State.success(response.body()!!)
            } else {
                _employees.value = State.success(emptyList())
            }

        } else {
            Log.d("TAG", "fetchEmployees:Failed:${response.message()} ")
            _employees.value = State.success(emptyList())
        }
    }

    fun addLead(
        firstName: String,
        middleName: String,
        surnameName: String,
        countryCodeMob: String,
        mobileNumber: String,
        alternateNumber: String,
        email: String,
        branchId: String,
        adminId: String,
        leadStatus: String,
        source: String,
        tagStudent: String,

        ) = flow<State<Boolean>> {
        val apiKey = datastore.first().apiKey
        val response = repository.addLead(
            apiKey,
            firstName,
            middleName,
            surnameName,
            countryCodeMob,
            mobileNumber,
            alternateNumber,
            email,
            branchId,
            adminId,
            leadStatus,
            source,
            tagStudent
        )
        if (response.isSuccessful) {
            Log.d("TAG", "addLead:isSuccessful::${response.body()} ")
            emit(State.success(true))
        } else {
            Log.d("TAG", "addLead:isSuccessful::${response.message()} ")
            emit(State.success(false))
        }
    }

    fun clearEveryFilter() = viewModelScope.launch {
        cardId = null
        filterLeadStatus = null
        filterBranch = null
        filterSource = null
        filterLabel = null
        filterCountry = null
        filterSeasons = null
        currentNo = "1"
        itemsPerPage = "0"
        startDate = null
        endDate = null
        searchQuery = ""
        fetchData(
            true
        )


    }

}