package com.smartsolar.app.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * Retrofit service mapping the 11 PHP endpoints in solar-admin/api/:
 * auth.php, products.php, orders.php, settings.php, solar_designs.php,
 * engineers.php, installations.php, maintenance.php, ratings.php,
 * notifications.php, _bootstrap.php
 */
interface SolarApiService {

    // 1. _bootstrap.php & settings.php
    @GET(PhpEndpointContract.FILE_BOOTSTRAP)
    suspend fun pingBootstrap(): Response<ResponseBody>

    @GET(PhpEndpointContract.FILE_SETTINGS)
    suspend fun getSettings(): ApiBaseResponse<PlatformSettingsDto>

    // 2. auth.php
    @POST(PhpEndpointContract.FILE_AUTH)
    suspend fun login(@Body request: LoginRequestDto): ApiBaseResponse<AuthPayloadDto>

    @POST(PhpEndpointContract.FILE_AUTH)
    suspend fun registerClient(@Body request: RegisterClientRequestDto): ApiBaseResponse<AuthPayloadDto>

    @Multipart
    @POST(PhpEndpointContract.FILE_AUTH)
    suspend fun registerEngineerMultipart(
        @Part("action") action: RequestBody,
        @Part("role") role: RequestBody,
        @Part("full_name") fullName: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part("password") password: RequestBody,
        @Part("specialization") specialization: RequestBody,
        @Part("qualification") qualification: RequestBody,
        @Part("years_experience") yearsExperience: RequestBody,
        @Part("bio") bio: RequestBody,
        @Part avatar: MultipartBody.Part?,
        @Part portfolioImages: List<MultipartBody.Part>,
        @Part certificateImages: List<MultipartBody.Part>
    ): ApiBaseResponse<AuthPayloadDto>

    @Multipart
    @POST(PhpEndpointContract.FILE_AUTH)
    suspend fun registerVendorMultipart(
        @Part("action") action: RequestBody,
        @Part("role") role: RequestBody,
        @Part("full_name") fullName: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part("password") password: RequestBody,
        @Part("store_name") storeName: RequestBody,
        @Part("commercial_register") commercialRegister: RequestBody,
        @Part storeLogo: MultipartBody.Part?,
        @Part storeImage: MultipartBody.Part?
    ): ApiBaseResponse<AuthPayloadDto>

    @GET(PhpEndpointContract.FILE_AUTH)
    suspend fun fetchProfile(@Query("action") action: String = "profile"): ApiBaseResponse<AuthPayloadDto>

    @POST(PhpEndpointContract.FILE_AUTH)
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): ApiBaseResponse<AuthPayloadDto>

    @POST(PhpEndpointContract.FILE_AUTH)
    suspend fun logout(@Body request: GenericActionRequestDto = GenericActionRequestDto("logout")): ApiBaseResponse<Unit>

    // 3. products.php
    @GET(PhpEndpointContract.FILE_PRODUCTS)
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("vendor_id") vendorId: Long? = null
    ): ApiBaseResponse<ProductsPayloadDto>

    @Multipart
    @POST(PhpEndpointContract.FILE_PRODUCTS)
    suspend fun saveVendorProductMultipart(
        @Part("action") action: RequestBody,
        @Part("id") id: RequestBody?,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part("price") price: RequestBody,
        @Part("stock") stock: RequestBody,
        @Part image: MultipartBody.Part?
    ): ApiBaseResponse<ProductDto>

    @POST(PhpEndpointContract.FILE_PRODUCTS)
    suspend fun deleteVendorProduct(
        @Body request: GenericActionRequestDto
    ): ApiBaseResponse<Unit>

    // 4. orders.php
    @GET(PhpEndpointContract.FILE_ORDERS)
    suspend fun getOrders(
        @Query("role") role: String? = null
    ): ApiBaseResponse<OrdersPayloadDto>

    @POST(PhpEndpointContract.FILE_ORDERS)
    suspend fun createOrder(
        @Body request: CreateOrderRequestDto
    ): ApiBaseResponse<OrderDto>

    // 5. solar_designs.php
    @GET(PhpEndpointContract.FILE_SOLAR_DESIGNS)
    suspend fun getSolarDesigns(): ApiBaseResponse<SolarDesignsPayloadDto>

    @POST(PhpEndpointContract.FILE_SOLAR_DESIGNS)
    suspend fun saveSolarDesign(
        @Body request: SaveSolarDesignRequestDto
    ): ApiBaseResponse<SolarDesignDto>

    // 6. engineers.php
    @GET(PhpEndpointContract.FILE_ENGINEERS)
    suspend fun getEngineers(
        @Query("availability") availability: String? = null
    ): ApiBaseResponse<EngineersPayloadDto>

    @POST(PhpEndpointContract.FILE_ENGINEERS)
    suspend fun updateEngineerAvailability(
        @Body request: UpdateAvailabilityRequestDto
    ): ApiBaseResponse<EngineerDto>

    // 7. installations.php
    @GET(PhpEndpointContract.FILE_INSTALLATIONS)
    suspend fun getInstallations(
        @Query("role") role: String? = null
    ): ApiBaseResponse<ServiceTasksPayloadDto>

    @POST(PhpEndpointContract.FILE_INSTALLATIONS)
    suspend fun createInstallation(
        @Body request: CreateInstallationRequestDto
    ): ApiBaseResponse<ServiceTaskDto>

    @POST(PhpEndpointContract.FILE_INSTALLATIONS)
    suspend fun updateInstallationTask(
        @Body request: UpdateTaskStatusRequestDto
    ): ApiBaseResponse<ServiceTaskDto>

    // 8. maintenance.php
    @GET(PhpEndpointContract.FILE_MAINTENANCE)
    suspend fun getMaintenanceRequests(
        @Query("role") role: String? = null
    ): ApiBaseResponse<ServiceTasksPayloadDto>

    @POST(PhpEndpointContract.FILE_MAINTENANCE)
    suspend fun createMaintenance(
        @Body request: CreateMaintenanceRequestDto
    ): ApiBaseResponse<ServiceTaskDto>

    @POST(PhpEndpointContract.FILE_MAINTENANCE)
    suspend fun updateMaintenanceTask(
        @Body request: UpdateTaskStatusRequestDto
    ): ApiBaseResponse<ServiceTaskDto>

    // 9. ratings.php
    @GET(PhpEndpointContract.FILE_RATINGS)
    suspend fun getEngineerRatings(
        @Query("engineer_id") engineerId: Long
    ): ApiBaseResponse<RatingsPayloadDto>

    @POST(PhpEndpointContract.FILE_RATINGS)
    suspend fun submitRating(
        @Body request: SubmitRatingRequestDto
    ): ApiBaseResponse<EngineerRatingDto>

    // 10. notifications.php
    @GET(PhpEndpointContract.FILE_NOTIFICATIONS)
    suspend fun getNotifications(): ApiBaseResponse<NotificationsPayloadDto>

    @POST(PhpEndpointContract.FILE_NOTIFICATIONS)
    suspend fun markNotificationRead(
        @Body request: MarkNotificationReadRequestDto
    ): ApiBaseResponse<Unit>
}
