package `in`.vertexdev.mobile.call_rec.network

import `in`.vertexdev.mobile.call_rec.models.CallLog
import `in`.vertexdev.mobile.call_rec.models.Employee
import `in`.vertexdev.mobile.call_rec.models.Lead
import `in`.vertexdev.mobile.call_rec.models.LeadCategory
import `in`.vertexdev.mobile.call_rec.models.StatusItem
import `in`.vertexdev.mobile.call_rec.models.UserResponse
import `in`.vertexdev.mobile.call_rec.models.VerifyOtpResponse
import `in`.vertexdev.mobile.call_rec.util.Constants.APP_KEY
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Url


// ApiService.kt
interface ApiService {
    @GET("recordrr/auth/check.service")
    suspend fun login(

        @Query("api_service_id") apiServiceId: String,
        @Query("api_command") apiCommand: String,
        @Query("public_api_key") apiKey: String,
        @Query("app_key") appKey: String,
        @Query("request_access") requestAccess: Int,
        @Query("check_key_authentication") checkKeyAuth: Int,
        @Query("grant_access") grantAccess: Int

    ): Response<ArrayList<UserResponse>>

    @GET("recordrr/auth/check.service")
    suspend fun checkAuth(
        @Query("app_key") appKey: String,
        @Query("public_api_key") publicApiKey: String,
        @Query("data_access_token") dataAccessToken: String,
        @Query("data_access_code") dataAccessCode: String
    ): Response<List<VerifyOtpResponse>>


    @GET("recordrr/auth/check.service")
    suspend fun resendOtp(
        @Query("app_key") appKey: String,
        @Query("public_api_key") publicApiKey: String,
        @Query("api_service_id") apiServiceId: String = "customer.api",
        @Query("api_command") apiCommand: String = "fetch.api",
        @Query("request_access") requestAccess: Int = 1,
        @Query("check_key_authentication") checkKeyAuthentication: Int = 1,
        @Query("grant_access") grantAccess: Int = 1
    ): Response<List<UserResponse>>


    @GET("recordrr/auth/check.service")
    suspend fun getApiData(
        @Query("app_key") appKey: String,
        @Query("filter_admin_id[]") filterAdminIds: List<String>,
        @Query("public_api_key") publicApiKey: String,
        @Query("api_service_id") apiServiceId: String = "customer.api",
        @Query("api_command") apiCommand: String = "report.by.student.status.api",
        @Query("tag_student") tagStudent: String = "lead", // As specified in API_URL_TEMPLATE
        @Query("filterdata") filterData: Int = 1,
        @Query("get_report") getReport: String = "allleads",
        @Query("get_sub_report") getSubReport: String = "assigned",
        @Query("filter_from_date") filterFromDate: String?,
        @Query("filter_to_date") filterToDate: String?
    ): Response<List<LeadCategory>>? // Replace YourResponseType with the actual response model

    @GET("recordrr/auth/check.service")
    suspend fun getStudentStats(
        @Query("app_key") appKey: String,
        @Query("filter_admin_id[]") filterAdminIds: List<String>,
        @Query("public_api_key") publicApiKey: String,
        @Query("api_service_id") apiServiceId: String = "customer.api",
        @Query("api_command") apiCommand: String = "report.by.student.status.api",
        @Query("tag_student") tagStudent: String = "", // As specified in STUDENT_STATS_API_URL_TEMPLATE
        @Query("get_report") getReport: String = "allleads",
        @Query("get_sub_report") getSubReport: String = "assigned",
        @Query("filterdata") filterData: Int = 1,
        @Query("filter_from_date") filterFromDate: String,
        @Query("filter_to_date") filterToDate: String
    ): Response<List<LeadCategory>>?// Replace YourResponseType with the actual response model


    @GET("recordrr/auth/check.service")
    suspend fun fetchData(
        @Query("app_key") appKey: String,
        @Query("public_api_key") publicApiKey: String,
        @Query("api_service_id") apiServiceId: String = "customer.api",
        @Query("api_command") apiCommand: String = "fetch.api",
        @Query("filterdata") filterData: Int = 1,
        @Query("tag_student") tagStudent: String, // For student empty
        @Query("search") search: String,
        @Query("event_id") eventId: String? = null,
        @Query("event_name") eventName: String? = null,
        @Query("get_report") getReport: String = "allleads",
        @Query("get_sub_report") getSubReport: String = "assigned",
        @Query("filter_from_date") filterFromDate: String?,
        @Query("filter_to_date") filterToDate: String?,
        @Query("filter_admin_id[]") filterAdminIds: List<String>,
        @Query("filter_lead_status[]") filterLeadStatus: List<String>,
        @Query("filter_branch[]") filterBranch: List<String>,
        @Query("filter_source[]") filterSource: List<String>,
        @Query("filter_label[]") filterLabel: List<String>,
        @Query("filter_country[]") filterCountry: List<String>,
        @Query("filter_seasons[]") filterSeasons: List<String>,
        @Query("filter_lead_status2[]") filterLeadStatus2: List<String>,
        @Query("page_count") pageCount: String = "0",
        @Query("page_no") pageNo: String = "1",

        ): ResponseBody


    @GET("recordrr/auth/check.service")
    suspend fun getFilters(
        @Query("api_service_id") apiServiceId: String,
        @Query("api_command") apiCommand: String,
        @Query("public_api_key") publicApiKey: String,
        @Query("app_key") appKey: String
    ): Response<List<StatusItem>>


    @GET("recordrr/auth/check.service")
    fun updateInterestedCountries(
        @Query("api_service_id") apiServiceId: String = "customer.api",
        @Query("api_command") apiCommand: String = "update.interested.api",
        @Query("tag_student") tagStudent: String,
        @Query("user_id") userId: String,
        @Query("public_api_key") publicApiKey: String,
        @Query("app_key") appKey: String,
        @Query("country[]") country: List<String>,
        @Query("season[]") season: List<String>,

        ): Call<ResponseBody> // Adjust the return type based on your API response

    @GET("recordrr/auth/check.service")
    fun updateIntakes(
        @Query("api_service_id") apiServiceId: String = "customer.api",
        @Query("api_command") apiCommand: String = "update.interested.api",
        @Query("tag_student") tagStudent: String,
        @Query("user_id") userId: String,
        @Query("public_api_key") publicApiKey: String,
        @Query("app_key") appKey: String,
        @Query("season[]") season: List<String>,
        @Query("country[]") country: List<String>

    ): Call<ResponseBody> // Adjust the return type based on your API response


    @GET("recordrr/auth/check.service")
    suspend fun updateLabel(
        @Query("app_key") appKey: String,
        @Query("public_api_key") publicApiKey: String,
        @Query("api_service_id") apiServiceId: String = "customer.api",
        @Query("api_command") apiCommand: String = "update.student.label.api",
        @Query("tag_student") tagStudent: String,
        @Query("user_id") id: String,  // This will be dynamic, e.g., id = 1151
        @Query("label_id") columnValue: String,// Dynamic value, e.g., "Sample Label"
        @Query("external_integration") externalIntegration: Int = 1,
        @Query("external_form") externalForm: String = "1",
        @Query("grant_access") grantAccess: String = "1"
    ): Response<ResponseBody>  // Adjust the return type if the API provides a response body


    @GET("recordrr/auth/check.service")
    suspend fun updateStatus(
        @Query("public_api_key") publicApiKey: String,
        @Query("app_key") appKey: String,
        @Query("api_service_id") apiServiceId: String = "customer.api",
        @Query("api_command") apiCommand: String = "update.student.status.api",
        @Query("tag_student") tagStudent: String, // Lead or empty based on isLead()
        @Query("user_id") userId: String,         // ID parameter
        @Query("lead_status") leadStatusId: String,  // Status parameter
        @Query("external_integration") externalIntegration: Int = 1,
        @Query("external_form") externalForm: String = "1",
        @Query("grant_access") grantAccess: String = "1"
    ): Response<ResponseBody>

    @GET("recordrr/auth/check.service")
    suspend fun updateStatusWithFollowup(
        @Query("public_api_key") publicApiKey: String,
        @Query("app_key") appKey: String,
        @Query("api_service_id") apiServiceId: String = "customer.api",
        @Query("api_command") apiCommand: String = "update.student.status.api",
        @Query("tag_student") tagStudent: String, // Lead or empty based on isLead()
        @Query("user_id") userId: String,         // ID parameter
        @Query("lead_status") leadStatus: String, // Status parameter
        @Query("fdate") followupDate: String,     // Follow-up date
        @Query("ftime") followupTime: String,     // Follow-up time
        @Query("addnote") additionalNote: String,  // Additional note
        @Query("external_integration") externalIntegration: Int = 1,
        @Query("external_form") externalForm: String = "1",
        @Query("grant_access") grantAccess: String = "1"
    ): Response<ResponseBody>


    @GET
    suspend fun uploadCallLogs(
        @Url url: String
    ): Response<String>


    @GET("recordrr/auth/check.service")
    fun getFileUploadDomain(
        @Query("app_key") appKey: String,
        @Query("public_api_key") publicApiKey: String,
        @Query("api_service_id") apiServiceId: String = "server.api",
        @Query("api_command") apiCommand: String = "fetch.limited.api"
    ): Call<ResponseBody>


    @GET("recordrr/auth/check.service")
    fun uploadAudioFileDetails(
        @Query("public_api_key") publicApiKey: String,
        @Query("app_key") appKey: String,

        @Query("api_service_id") apiServiceId: String = "enquiry.notes.api",
        @Query("api_command") apiCommand: String = "save.api",

        @Query("from_id") fromId: Int,
        @Query("tag_student") tagStudent: String,
        @Query("user_id") userId: String ,
        @Query("sendfile") file: String,
        @Query("student_status") studentStatus: Int = 0,
        @Query("addnote") addNote: String = "Uploaded",
        @Query("grant_access") grantAccess: Int = 1,
        @Query("call_duration") callDuration: Int,
        @Query("record_status") recordStatus: String,
        @Query("app_date") recordedDateTime: String
    ): Call<ResponseBody>

    @GET("recordrr/auth/check.service")
    suspend fun search(
        @Query("app_key") appKey: String,
        @Query("public_api_key") publicApiKey: String,
        @Query("api_service_id") apiServiceId: String = "customer.api",
        @Query("api_command") apiCommand: String = "fetch.api",
        @Query("filterdata") filterData: Int = 1,
        @Query("skip_app_access") skipAppAccess: String = "0",
        @Query("search") search: String,
        @Query("tag_student") tagStudent: String
    ): Response<ResponseBody>

    @GET("recordrr/auth/check.service")
    suspend fun getEmployees(
        @Query("app_key") appKey: String,
        @Query("public_api_key") publicApiKey: String,
        @Query("api_service_id") apiServiceId: String = "users.api",
        @Query("api_command") apiCommand: String = "fetch.user.limited.api",
        @Query("grant_access") grantAccess: Int = 1
    ): Response<List<Employee>>


    @GET("recordrr/auth/check.service")
    suspend fun addLeadStudent(
        @Query("app_key") appKey: String = APP_KEY,
        @Query("public_api_key") publicApiKey: String,
        @Query("api_service_id") apiServiceId: String = "customer.api",
        @Query("api_command") apiCommand: String = "register.api",
        @Query("fname") fname: String,
        @Query("lname") lname: String = "",
        @Query("email") email: String = "",
        @Query("mobile") mobile: String = "",
//        @Query("vendor_id") vendorId: Int = 0,
        @Query("branch_id") branchId: String = "",
        @Query("admin_id") adminId: String = "",
        @Query("lead_status") leadStatus: String = "",
        @Query("source") source: String = "",
        @Query("country_code") countryCode: String = "91",
        @Query("event_id") eventId: String? = null,
        @Query("event_checked_in") eventCheckedIn: String? = null,
        @Query("transfer_no") transferNo: String? = null,
        @Query("alt_country_code") altCountryCode: String? = null,
        @Query("alt_mobile") altMobile: String? = null,
        @Query("reference") reference: String = "",
        @Query("tag_student") tagStudent: String = "lead",
        @Query("mname") mname: String = "api",
        @Query("interested_country") interestedCountry: String = "",
        @Query("campaign_name") campaignName: String = "",
        @Query("sub_campaign_name") subCampaignName: String = "",
        @Query("label") label: String = "",
        @Query("interested_university") interestedUniversity: String = "",
        @Query("current_university") currentUniversity: String = "",
        @Query("intake") intake: String = "",
        @Query("external_integration") externalIntegration: Int = 1,
        @Query("external_form") externalForm: Int = 1
    ): Response<ResponseBody>


    @GET("recordrr/auth/check.service")
    suspend fun getUser(

        @Query("app_key") appKey: String = APP_KEY,
        @Query("public_api_key") publicApiKey: String,
        @Query("api_service_id") apiServiceId: String = "users.api",
        @Query("api_command") apiCommand: String = "verify.public.key.api",
        @Query("grant_access") grantAccess: String = "1"


    ): Response<ArrayList<UserResponse>>
}


