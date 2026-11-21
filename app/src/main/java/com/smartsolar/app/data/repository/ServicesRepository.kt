package com.smartsolar.app.data.repository

import com.smartsolar.app.data.remote.CreateInstallationRequestDto
import com.smartsolar.app.data.remote.CreateMaintenanceRequestDto
import com.smartsolar.app.data.remote.EngineerDto
import com.smartsolar.app.data.remote.MarkNotificationReadRequestDto
import com.smartsolar.app.data.remote.NetworkClientFactory
import com.smartsolar.app.data.remote.NetworkErrorType
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.data.remote.PhpEndpointContract
import com.smartsolar.app.data.remote.PlatformSettingsDto
import com.smartsolar.app.data.remote.SaveSolarDesignRequestDto
import com.smartsolar.app.data.remote.ServiceTaskDto
import com.smartsolar.app.data.remote.SubmitRatingRequestDto
import com.smartsolar.app.data.remote.UpdateAvailabilityRequestDto
import com.smartsolar.app.data.remote.UpdateTaskStatusRequestDto
import com.smartsolar.app.domain.calculator.SolarSystemCalculator
import com.smartsolar.app.domain.model.AppNotification
import com.smartsolar.app.domain.model.EngineerAvailability
import com.smartsolar.app.domain.model.EngineerProfile
import com.smartsolar.app.domain.model.EngineerRating
import com.smartsolar.app.domain.model.SavedSolarDesign
import com.smartsolar.app.domain.model.ServiceTask
import com.smartsolar.app.domain.model.ServiceType
import com.smartsolar.app.domain.model.SolarDesignResult
import com.smartsolar.app.domain.model.TaskStatus

class ServicesRepository(
    private val networkFactory: NetworkClientFactory
) {
    private val api get() = networkFactory.apiService

    suspend fun testServerConnection(): NetworkResult<PlatformSettingsDto?> {
        // First try settings.php (JSON), fallback check _bootstrap.php reachability
        val settingsResult = networkFactory.safeApiCall(PhpEndpointContract.FILE_SETTINGS) {
            api.getSettings()
        }
        return when (settingsResult) {
            is NetworkResult.Success -> {
                NetworkResult.Success(
                    data = settingsResult.data.data,
                    message = settingsResult.data.resolvedMessage()
                        .ifBlank { "تم الاتصال بنجاح بخادم PHP وتلقي استجابة JSON صالحة من settings.php." }
                )
            }
            is NetworkResult.Error -> {
                if (settingsResult.type == NetworkErrorType.HTTP_NOT_FOUND) {
                    val bootstrapResult = networkFactory.safeApiCall(PhpEndpointContract.FILE_BOOTSTRAP) {
                        api.pingBootstrap()
                    }
                    when (bootstrapResult) {
                        is NetworkResult.Success -> {
                            val code = bootstrapResult.data.code()
                            if (bootstrapResult.data.isSuccessful) {
                                NetworkResult.Success(null, "تم الوصول إلى _bootstrap.php بنجاح (HTTP $code).")
                            } else {
                                NetworkResult.Error(
                                    type = NetworkErrorType.SERVER_ERROR,
                                    arabicMessage = "الخادم استجاب برمز HTTP $code لملف _bootstrap.php.",
                                    phpEndpoint = PhpEndpointContract.FILE_BOOTSTRAP
                                )
                            }
                        }
                        is NetworkResult.Error -> bootstrapResult
                        NetworkResult.Loading -> NetworkResult.Loading
                    }
                } else {
                    settingsResult
                }
            }
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun fetchEngineers(availabilityFilter: EngineerAvailability? = null): NetworkResult<List<EngineerProfile>> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_ENGINEERS) {
            api.getEngineers(availability = availabilityFilter?.apiValue)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (!resp.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر جلب قائمة المهندسين." },
                        phpEndpoint = PhpEndpointContract.FILE_ENGINEERS
                    )
                } else {
                    val list = (resp.data?.engineers ?: resp.data?.items.orEmpty()).map { it.toDomain() }
                    NetworkResult.Success(list)
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun updateEngineerAvailability(availability: EngineerAvailability): NetworkResult<EngineerAvailability> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_ENGINEERS) {
            api.updateEngineerAvailability(UpdateAvailabilityRequestDto(availability = availability.apiValue))
        }
        return when (result) {
            is NetworkResult.Success -> {
                if (!result.data.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = result.data.resolvedMessage().ifBlank { "تعذر تحديث حالة التوفر." },
                        phpEndpoint = PhpEndpointContract.FILE_ENGINEERS
                    )
                } else {
                    val returned = result.data.data?.availability?.let { EngineerAvailability.fromApi(it) } ?: availability
                    NetworkResult.Success(returned, "تم تحديث حالة التوفر إلى ${returned.arabicLabel}")
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun createInstallationRequest(
        location: String,
        systemDetails: String,
        description: String,
        requestedDate: String,
        engineerId: Long?
    ): NetworkResult<ServiceTask> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_INSTALLATIONS) {
            api.createInstallation(
                CreateInstallationRequestDto(
                    location = location.trim(),
                    systemDetails = systemDetails.trim(),
                    description = description.trim(),
                    requestedDate = requestedDate.trim(),
                    engineerId = engineerId
                )
            )
        }
        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (!resp.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر إرسال طلب التركيب." },
                        phpEndpoint = PhpEndpointContract.FILE_INSTALLATIONS
                    )
                } else {
                    val task = resp.data?.toDomain(ServiceType.INSTALLATION) ?: ServiceTask(
                        id = 0L,
                        requestNumber = "-",
                        serviceType = ServiceType.INSTALLATION,
                        location = location,
                        systemDetails = systemDetails,
                        description = description,
                        engineerId = engineerId,
                        requestedDate = requestedDate
                    )
                    NetworkResult.Success(task, resp.resolvedMessage().ifBlank { "تم إرسال طلب التركيب بنجاح." })
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun createMaintenanceRequest(
        systemDetails: String,
        issueType: String,
        faultDetails: String,
        location: String,
        requestedDate: String,
        engineerId: Long?
    ): NetworkResult<ServiceTask> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_MAINTENANCE) {
            api.createMaintenance(
                CreateMaintenanceRequestDto(
                    systemDetails = systemDetails.trim(),
                    issueType = issueType.trim(),
                    faultDetails = faultDetails.trim(),
                    location = location.trim(),
                    requestedDate = requestedDate.trim(),
                    engineerId = engineerId
                )
            )
        }
        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (!resp.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر إرسال طلب الصيانة." },
                        phpEndpoint = PhpEndpointContract.FILE_MAINTENANCE
                    )
                } else {
                    val task = resp.data?.toDomain(ServiceType.MAINTENANCE) ?: ServiceTask(
                        id = 0L,
                        requestNumber = "-",
                        serviceType = ServiceType.MAINTENANCE,
                        location = location,
                        systemDetails = systemDetails,
                        issueType = issueType,
                        description = faultDetails,
                        engineerId = engineerId,
                        requestedDate = requestedDate
                    )
                    NetworkResult.Success(task, resp.resolvedMessage().ifBlank { "تم إرسال طلب الصيانة بنجاح." })
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun fetchServiceTasks(role: String): NetworkResult<List<ServiceTask>> {
        val installRes = networkFactory.safeApiCall(PhpEndpointContract.FILE_INSTALLATIONS) {
            api.getInstallations(role = role)
        }
        val maintRes = networkFactory.safeApiCall(PhpEndpointContract.FILE_MAINTENANCE) {
            api.getMaintenanceRequests(role = role)
        }

        if (installRes is NetworkResult.Error && maintRes is NetworkResult.Error) {
            return installRes
        }

        val combined = mutableListOf<ServiceTask>()
        if (installRes is NetworkResult.Success && installRes.data.isOperationSuccess()) {
            val items = installRes.data.data?.installations
                ?: installRes.data.data?.tasks
                ?: installRes.data.data?.items
                ?: emptyList()
            combined.addAll(items.map { it.toDomain(ServiceType.INSTALLATION) })
        }
        if (maintRes is NetworkResult.Success && maintRes.data.isOperationSuccess()) {
            val items = maintRes.data.data?.maintenance
                ?: maintRes.data.data?.tasks
                ?: maintRes.data.data?.items
                ?: emptyList()
            combined.addAll(items.map { it.toDomain(ServiceType.MAINTENANCE) })
        }

        return NetworkResult.Success(combined)
    }

    suspend fun updateTaskStatus(
        task: ServiceTask,
        action: String,
        newStatus: TaskStatus,
        technicalReport: String
    ): NetworkResult<ServiceTask> {
        val endpoint = if (task.serviceType == ServiceType.INSTALLATION) {
            PhpEndpointContract.FILE_INSTALLATIONS
        } else {
            PhpEndpointContract.FILE_MAINTENANCE
        }

        val req = UpdateTaskStatusRequestDto(
            action = action,
            id = task.id,
            status = newStatus.apiValue,
            technicalReport = technicalReport.trim()
        )

        val result = networkFactory.safeApiCall(endpoint) {
            if (task.serviceType == ServiceType.INSTALLATION) {
                api.updateInstallationTask(req)
            } else {
                api.updateMaintenanceTask(req)
            }
        }

        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (!resp.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر تحديث حالة المهمة." },
                        phpEndpoint = endpoint
                    )
                } else {
                    val updated = resp.data?.toDomain(task.serviceType)
                        ?: task.copy(status = newStatus, technicalReport = technicalReport.trim())
                    NetworkResult.Success(updated, resp.resolvedMessage().ifBlank { "تم تحديث حالة المهمة بنجاح." })
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun saveSolarDesign(
        title: String,
        design: SolarDesignResult
    ): NetworkResult<SavedSolarDesign> {
        val note = SolarSystemCalculator.buildAssumptionsSummary(design.assumptions)
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_SOLAR_DESIGNS) {
            api.saveSolarDesign(
                SaveSolarDesignRequestDto(
                    title = title.trim().ifBlank { "تصميم منظومة شمسية" },
                    totalLoadWatts = design.totalLoadWatts,
                    dailyKwh = design.dailyConsumptionKwh,
                    systemKwp = design.recommendedSystemKwp,
                    panelCount = design.panelCount,
                    panelWatts = design.panelWattage,
                    batteryCapacityWh = design.requiredBatteryCapacityWh,
                    batteryCount = design.batteryCount,
                    inverterWatts = design.recommendedInverterWatts,
                    assumptionsNote = note
                )
            )
        }
        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (!resp.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر حفظ تصميم المنظومة في الخادم." },
                        phpEndpoint = PhpEndpointContract.FILE_SOLAR_DESIGNS
                    )
                } else {
                    val dto = resp.data
                    val saved = SavedSolarDesign(
                        id = dto?.id ?: 0L,
                        title = dto?.title ?: title,
                        totalLoadWatts = dto?.totalLoadWatts ?: design.totalLoadWatts,
                        dailyKwh = dto?.dailyKwh ?: design.dailyConsumptionKwh,
                        systemKwp = dto?.systemKwp ?: design.recommendedSystemKwp,
                        panelCount = dto?.panelCount ?: design.panelCount,
                        panelWatts = dto?.panelWatts ?: design.panelWattage,
                        batteryCapacityWh = dto?.batteryCapacityWh ?: design.requiredBatteryCapacityWh,
                        batteryCount = dto?.batteryCount ?: design.batteryCount,
                        inverterWatts = dto?.inverterWatts ?: design.recommendedInverterWatts,
                        assumptionsNote = dto?.assumptionsNote ?: note,
                        createdAt = dto?.createdAt.orEmpty()
                    )
                    NetworkResult.Success(saved, resp.resolvedMessage().ifBlank { "تم حفظ تصميم المنظومة الشمسية بنجاح." })
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun fetchSavedSolarDesigns(): NetworkResult<List<SavedSolarDesign>> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_SOLAR_DESIGNS) {
            api.getSolarDesigns()
        }
        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (!resp.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر جلب التصاميم المحفوظة." },
                        phpEndpoint = PhpEndpointContract.FILE_SOLAR_DESIGNS
                    )
                } else {
                    val list = (resp.data?.designs ?: resp.data?.items.orEmpty()).map { dto ->
                        SavedSolarDesign(
                            id = dto.id,
                            title = dto.title ?: "تصميم #${dto.id}",
                            totalLoadWatts = dto.totalLoadWatts ?: 0.0,
                            dailyKwh = dto.dailyKwh ?: 0.0,
                            systemKwp = dto.systemKwp ?: 0.0,
                            panelCount = dto.panelCount ?: 0,
                            panelWatts = dto.panelWatts ?: 550,
                            batteryCapacityWh = dto.batteryCapacityWh ?: 0.0,
                            batteryCount = dto.batteryCount ?: 0,
                            inverterWatts = dto.inverterWatts ?: 0,
                            assumptionsNote = dto.assumptionsNote.orEmpty(),
                            createdAt = dto.createdAt.orEmpty()
                        )
                    }
                    NetworkResult.Success(list)
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun fetchEngineerRatings(engineerId: Long): NetworkResult<List<EngineerRating>> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_RATINGS) {
            api.getEngineerRatings(engineerId)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (!resp.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر جلب التقييمات." },
                        phpEndpoint = PhpEndpointContract.FILE_RATINGS
                    )
                } else {
                    val list = (resp.data?.ratings ?: resp.data?.items.orEmpty()).map { dto ->
                        EngineerRating(
                            id = dto.id,
                            engineerId = dto.engineerId,
                            clientId = dto.clientId ?: 0L,
                            clientName = dto.clientName ?: "عميل",
                            serviceId = dto.serviceId ?: 0L,
                            serviceType = dto.serviceType.orEmpty(),
                            stars = dto.stars.coerceIn(1, 5),
                            comment = dto.comment.orEmpty(),
                            createdAt = dto.createdAt.orEmpty()
                        )
                    }
                    NetworkResult.Success(list)
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun submitEngineerRating(
        engineerId: Long,
        serviceId: Long,
        serviceType: ServiceType,
        stars: Int,
        comment: String
    ): NetworkResult<Unit> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_RATINGS) {
            api.submitRating(
                SubmitRatingRequestDto(
                    engineerId = engineerId,
                    serviceId = serviceId,
                    serviceType = serviceType.apiValue,
                    stars = stars.coerceIn(1, 5),
                    comment = comment.trim()
                )
            )
        }
        return when (result) {
            is NetworkResult.Success -> {
                if (!result.data.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = result.data.resolvedMessage().ifBlank { "تعذر إرسال التقييم أو تم تقييم هذه الخدمة مسبقاً." },
                        phpEndpoint = PhpEndpointContract.FILE_RATINGS
                    )
                } else {
                    NetworkResult.Success(Unit, result.data.resolvedMessage().ifBlank { "شكراً لك! تم إرسال تقييم المهندس بنجاح." })
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun fetchNotifications(): NetworkResult<List<AppNotification>> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_NOTIFICATIONS) {
            api.getNotifications()
        }
        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (!resp.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر جلب الإشعارات." },
                        phpEndpoint = PhpEndpointContract.FILE_NOTIFICATIONS
                    )
                } else {
                    val list = (resp.data?.notifications ?: resp.data?.items.orEmpty()).map { dto ->
                        AppNotification(
                            id = dto.id,
                            title = dto.title ?: "إشعار",
                            message = dto.message.orEmpty(),
                            type = dto.type.orEmpty(),
                            isRead = dto.isRead ?: false,
                            createdAt = dto.createdAt.orEmpty()
                        )
                    }
                    NetworkResult.Success(list)
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun markNotificationRead(notificationId: Long): NetworkResult<Unit> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_NOTIFICATIONS) {
            api.markNotificationRead(MarkNotificationReadRequestDto(id = notificationId))
        }
        return when (result) {
            is NetworkResult.Success -> {
                if (!result.data.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = result.data.resolvedMessage().ifBlank { "تعذر تحديث حالة الإشعار." },
                        phpEndpoint = PhpEndpointContract.FILE_NOTIFICATIONS
                    )
                } else {
                    NetworkResult.Success(Unit)
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    private suspend fun EngineerDto.toDomain(): EngineerProfile {
        return EngineerProfile(
            id = id,
            userId = userId ?: id,
            fullName = fullName ?: name ?: "مهندس طاقة شمسية",
            email = email.orEmpty(),
            phone = phone.orEmpty(),
            city = city.orEmpty(),
            age = age ?: 0,
            specialization = specialization ?: "أنظمة الطاقة الشمسية",
            qualification = qualification.orEmpty(),
            yearsOfExperience = yearsExperience ?: 0,
            bio = bio.orEmpty(),
            previousProjects = previousProjects.orEmpty(),
            avatarUrl = networkFactory.resolveAssetUrl(avatarUrl ?: avatar),
            portfolioImages = portfolioImages.orEmpty().map { networkFactory.resolveAssetUrl(it) }.filter { it.isNotBlank() },
            certificateImages = certificateImages.orEmpty().map { networkFactory.resolveAssetUrl(it) }.filter { it.isNotBlank() },
            averageRating = averageRating ?: rating ?: 0.0,
            ratingsCount = ratingsCount ?: 0,
            availability = EngineerAvailability.fromApi(availability)
        )
    }

    private fun ServiceTaskDto.toDomain(type: ServiceType): ServiceTask {
        return ServiceTask(
            id = id,
            requestNumber = requestNumber?.takeIf { it.isNotBlank() } ?: "#$id",
            serviceType = type,
            clientId = clientId ?: 0L,
            clientName = clientName.orEmpty(),
            clientPhone = clientPhone?.takeIf { it.isNotBlank() },
            location = location.orEmpty(),
            systemDetails = systemDetails.orEmpty(),
            issueType = issueType.orEmpty(),
            description = description ?: faultDetails.orEmpty(),
            engineerId = engineerId,
            engineerName = engineerName.orEmpty(),
            status = TaskStatus.fromApi(status),
            technicalReport = technicalReport.orEmpty(),
            requestedDate = requestedDate.orEmpty(),
            createdAt = createdAt.orEmpty(),
            isRated = isRated ?: false,
            canAccept = canAccept ?: (TaskStatus.fromApi(status) == TaskStatus.PENDING)
        )
    }
}
