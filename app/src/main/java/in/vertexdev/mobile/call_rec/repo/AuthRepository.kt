package `in`.vertexdev.mobile.call_rec.repo

import `in`.vertexdev.mobile.call_rec.models.CallLog
import `in`.vertexdev.mobile.call_rec.network.ApiService
import `in`.vertexdev.mobile.call_rec.util.Constants.APP_KEY
import okhttp3.internal.userAgent
import org.json.JSONArray
import retrofit2.http.Query
import java.time.Duration
import javax.inject.Inject

// AuthRepository.kt
class AuthRepository @Inject constructor(private val apiService: ApiService) {


    suspend fun login(apiKey: String) = apiService.login(
        apiServiceId = "customer.api",
        apiCommand = "fetch.api",
        apiKey = apiKey,
        appKey = APP_KEY,
        requestAccess = 1,
        checkKeyAuth = 1,
        grantAccess = 1
    )


    suspend fun checkAuth(apiKey: String, oneTimeToken: String, otp: String) = apiService.checkAuth(
        appKey = APP_KEY,
        publicApiKey = apiKey,
        dataAccessToken = oneTimeToken,
        dataAccessCode = otp


    )

    suspend fun resendOtp(apiKey: String) = apiService.resendOtp(
        appKey = APP_KEY,
        publicApiKey = apiKey,
    )


    suspend fun getStudentStats(

        publicApiKey: String,
        filterAdminIds: List<String>,
        filterFromDate: String,
        filterToDate: String

    ) = apiService.getStudentStats(
        appKey = APP_KEY, publicApiKey = publicApiKey, filterAdminIds = filterAdminIds,

        filterFromDate = filterFromDate, filterToDate = filterToDate
    )

    suspend fun getApiData(

        publicApiKey: String,
        tagStudent: String,
        filterAdminIds: List<String>,
        filterFromDate: String?,
        filterToDate: String?
    ) = apiService.getApiData(
        appKey = APP_KEY,
        publicApiKey = publicApiKey,
        tagStudent = tagStudent,
        filterFromDate = filterFromDate,
        filterToDate = filterToDate,
        filterAdminIds = filterAdminIds
    )


    // Define constants for default values if needed
    val DEFAULT_API_SERVICE_ID = "customer.api"
    val DEFAULT_API_COMMAND = "fetch.api"

    val DEFAULT_TAG_STUDENT = ""
    val DEFAULT_PAGE_NO = "1"
    val DEFAULT_PAGE_COUNT = "20"


    suspend fun fetchData(
        publicApiKey: String,
        tagStudent: String,
        filterFromDate: String?,
        filterToDate: String?,
        apiServiceId: String? = null, // Optional with default value
        apiCommand: String? = null,
        filterData: Int,
        eventId: String? = null,
        eventName: String? = null,
        filterAdminIds: List<String>,
        filterLeadStatus: List<String>? = null,
        filterBranch: List<String>? = null,
        filterSource: List<String>? = null,
        filterLabel: List<String>? = null,
        filterCountry: List<String>? = null,
        filterSeasons: List<String>? = null,
        filterLeadStatus2: List<String>? = null,
        search: String = "",
        pageNo: String? = null,
        pageCount: String,

        ) = apiService.fetchData(
        appKey = APP_KEY,
        publicApiKey = publicApiKey,
        tagStudent = tagStudent ?: DEFAULT_TAG_STUDENT, // Use default value if null
        filterFromDate = filterFromDate,
        filterToDate = filterToDate,
        apiServiceId = apiServiceId ?: DEFAULT_API_SERVICE_ID, // Use default value if null
        apiCommand = apiCommand ?: DEFAULT_API_COMMAND, // Use default value if null
        filterData = filterData, // Use default value if null
        eventId = eventId,
        eventName = eventName,
        filterAdminIds = filterAdminIds, // Use empty list if null
        filterLeadStatus = filterLeadStatus ?: listOf(),
        filterBranch = filterBranch ?: listOf(),
        filterSource = filterSource ?: listOf(),
        filterLabel = filterLabel ?: listOf(),
        filterCountry = filterCountry ?: listOf(),
        filterSeasons = filterSeasons ?: listOf(),
        filterLeadStatus2 = filterLeadStatus ?: listOf(),
        pageNo = pageNo ?: DEFAULT_PAGE_NO, // Use default value if null
         pageCount = pageCount,
        search = search
    )


    suspend fun getFilters(
        apiServiceId: String, apiCommand: String, publicApiKey: String

    ) = apiService.getFilters(
        appKey = APP_KEY,
        publicApiKey = publicApiKey,
        apiCommand = apiCommand,
        apiServiceId = apiServiceId

    )

    suspend fun updateInterestedCountries(
        apiServiceId: String,
        apiCommand: String,
        publicApiKey: String,
        tagStudent: String,
        userId: String,
        countryParams: List<String>,
        season: List<String>,


        ) = apiService.updateInterestedCountries(
        apiServiceId = apiServiceId,
        apiCommand = apiCommand,
        tagStudent = tagStudent,
        userId = userId,
        publicApiKey = publicApiKey,
        appKey = APP_KEY,
        country = countryParams,
        season = season,

        )
    //updateIntakes

    suspend fun updateIntakes(
        apiServiceId: String,
        apiCommand: String,
        publicApiKey: String,
        tagStudent: String,
        userId: String,
        season: List<String> = listOf(),
        countryParams: List<String> = listOf()


    ) = apiService.updateIntakes(
        apiServiceId = apiServiceId,
        apiCommand = apiCommand,
        tagStudent = tagStudent,
        userId = userId,
        publicApiKey = publicApiKey,
        appKey = APP_KEY,
        season = season,
        country = countryParams

    )


    suspend fun updateLabel(
        apiServiceId: String,
        apiCommand: String,
        publicApiKey: String,
        tagStudent: String,
        columnValue: String,
        id: String


    ) = apiService.updateLabel(
        apiServiceId = apiServiceId,

        tagStudent = tagStudent,
        publicApiKey = publicApiKey,
        appKey = APP_KEY,
        columnValue = columnValue,
        id = id
    )

    //updateStatus
    suspend fun updateStatus(
        apiServiceId: String,
        apiCommand: String,
        publicApiKey: String,
        tagStudent: String,
        statusId: String,
        userId: String


    ) = apiService.updateStatus(
        apiServiceId = apiServiceId,
        apiCommand = apiCommand,
        tagStudent = tagStudent,
        publicApiKey = publicApiKey,
        appKey = APP_KEY,
        userId = userId,
        leadStatusId = statusId


    )

    //updateStatusWithFollowup
    suspend fun updateStatusWithFollowup(
        apiServiceId: String,
        apiCommand: String,
        publicApiKey: String,
        tagStudent: String,
        leadStatus: String,
        userId: String,
        followupDate: String,
        followupTime: String,
        additionalNote: String


    ) = apiService.updateStatusWithFollowup(
        apiServiceId = apiServiceId,
        apiCommand = apiCommand,
        tagStudent = tagStudent,
        publicApiKey = publicApiKey,
        appKey = APP_KEY,
        userId = userId,
        leadStatus = leadStatus,
        followupDate = followupDate,
        followupTime = followupTime,
        additionalNote = additionalNote


    )

    //uploadCallLogs

    suspend fun uploadCallLogs(
        url: String
    ) = apiService.uploadCallLogs(
        url
    )

    suspend fun getFileUploadDomain(publicApiKey: String) =
        apiService.getFileUploadDomain(appKey = APP_KEY, publicApiKey = publicApiKey)


    suspend fun uploadAudioFileDetails(
        publicApiKey: String,
        fromId: Int,
        tagStudent: String,
        userId: String,
        fileName: String,
        callDuration: Int,
        recordStatus: String,
        recordedDateTime:String
    ) = apiService.uploadAudioFileDetails(
        publicApiKey = publicApiKey,
        appKey = APP_KEY,
        fromId = fromId,
        tagStudent = tagStudent,
        userId = userId,
        file = fileName,
        callDuration = callDuration,
        recordStatus = recordStatus,
        recordedDateTime = recordedDateTime


    )

    suspend fun search(
        publicApiKey: String, search: String, tagStudent: String, skipAccess: String
    ) = apiService.search(
        publicApiKey = publicApiKey,
        appKey = APP_KEY,
        search = search,
        tagStudent = tagStudent,
        skipAppAccess = skipAccess

    )

    suspend fun getEmployees(publicApiKey: String) =
        apiService.getEmployees(publicApiKey = publicApiKey, appKey = APP_KEY)

    suspend fun addLead(
        publicApiKey: String,
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
        tagStudent: String
    ) = apiService.addLeadStudent(
        publicApiKey = publicApiKey,
        fname = firstName,
        lname = surnameName,
        email = email,
        mobile = mobileNumber,
        branchId = branchId,
        adminId = adminId,
        leadStatus = leadStatus,
        source = source,
        countryCode = countryCodeMob,
        altMobile = alternateNumber,
        tagStudent = tagStudent


    )

    suspend fun getUser(publicApiKey: String) = apiService.getUser(publicApiKey = publicApiKey)
}
