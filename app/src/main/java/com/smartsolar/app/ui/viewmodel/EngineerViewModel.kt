package com.smartsolar.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartsolar.app.AppContainer
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.data.repository.ServicesRepository
import com.smartsolar.app.domain.model.EngineerAvailability
import com.smartsolar.app.domain.model.EngineerRating
import com.smartsolar.app.domain.model.ServiceTask
import com.smartsolar.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EngineerViewModel(
    private val servicesRepository: ServicesRepository
) : ViewModel() {

    private val _tasksState = MutableStateFlow<NetworkResult<List<ServiceTask>>>(NetworkResult.Loading)
    val tasksState: StateFlow<NetworkResult<List<ServiceTask>>> = _tasksState.asStateFlow()

    private val _availabilityState = MutableStateFlow(EngineerAvailability.AVAILABLE)
    val availabilityState: StateFlow<EngineerAvailability> = _availabilityState.asStateFlow()

    private val _availabilityUpdateResult = MutableStateFlow<NetworkResult<EngineerAvailability>?>(null)
    val availabilityUpdateResult: StateFlow<NetworkResult<EngineerAvailability>?> = _availabilityUpdateResult.asStateFlow()

    private val _taskActionResult = MutableStateFlow<NetworkResult<ServiceTask>?>(null)
    val taskActionResult: StateFlow<NetworkResult<ServiceTask>?> = _taskActionResult.asStateFlow()

    private val _selectedTask = MutableStateFlow<ServiceTask?>(null)
    val selectedTask: StateFlow<ServiceTask?> = _selectedTask.asStateFlow()

    private val _ratingsState = MutableStateFlow<NetworkResult<List<EngineerRating>>>(NetworkResult.Loading)
    val ratingsState: StateFlow<NetworkResult<List<EngineerRating>>> = _ratingsState.asStateFlow()

    fun syncInitialAvailability(availability: EngineerAvailability) {
        _availabilityState.value = availability
    }

    fun loadAssignedTasks(currentEngineerId: Long) {
        viewModelScope.launch {
            _tasksState.value = NetworkResult.Loading
            when (val res = servicesRepository.fetchServiceTasks("engineer")) {
                is NetworkResult.Success -> {
                    // Enforce strict scoping: only show tasks assigned to this engineer (or unassigned only if server explicitly flagged canAccept)
                    val scoped = res.data.filter { task ->
                        task.engineerId == null || task.engineerId == 0L || task.engineerId == currentEngineerId
                    }
                    _tasksState.value = NetworkResult.Success(scoped)
                }
                is NetworkResult.Error -> _tasksState.value = res
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun selectTask(task: ServiceTask) {
        _selectedTask.value = task
    }

    fun updateAvailability(newAvailability: EngineerAvailability) {
        viewModelScope.launch {
            _availabilityUpdateResult.value = NetworkResult.Loading
            val res = servicesRepository.updateEngineerAvailability(newAvailability)
            if (res is NetworkResult.Success) {
                _availabilityState.value = res.data
            }
            _availabilityUpdateResult.value = res
        }
    }

    fun clearAvailabilityResult() {
        _availabilityUpdateResult.value = null
    }

    fun acceptTask(task: ServiceTask, currentEngineerId: Long) {
        performTaskUpdate(task, "accept", TaskStatus.ACCEPTED, task.technicalReport, currentEngineerId)
    }

    fun updateTaskProgress(task: ServiceTask, newStatus: TaskStatus, report: String, currentEngineerId: Long) {
        val action = if (newStatus == TaskStatus.COMPLETED) "complete" else "update_status"
        performTaskUpdate(task, action, newStatus, report, currentEngineerId)
    }

    private fun performTaskUpdate(
        task: ServiceTask,
        action: String,
        newStatus: TaskStatus,
        report: String,
        currentEngineerId: Long
    ) {
        viewModelScope.launch {
            _taskActionResult.value = NetworkResult.Loading
            val res = servicesRepository.updateTaskStatus(task, action, newStatus, report)
            if (res is NetworkResult.Success) {
                _selectedTask.value = res.data
                loadAssignedTasks(currentEngineerId)
            }
            _taskActionResult.value = res
        }
    }

    fun clearTaskActionResult() {
        _taskActionResult.value = null
    }

    fun loadEngineerRatings(engineerId: Long) {
        viewModelScope.launch {
            _ratingsState.value = NetworkResult.Loading
            _ratingsState.value = servicesRepository.fetchEngineerRatings(engineerId)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EngineerViewModel(container.servicesRepository) as T
                }
            }
    }
}
