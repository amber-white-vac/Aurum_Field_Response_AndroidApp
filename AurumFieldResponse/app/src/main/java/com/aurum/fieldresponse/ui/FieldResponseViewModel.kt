package com.aurum.fieldresponse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurum.fieldresponse.data.FieldResponseRepository
import com.aurum.fieldresponse.data.SyntheticTrackerSource
import com.aurum.fieldresponse.domain.HazardClassifier
import com.aurum.fieldresponse.domain.IncidentFlag
import com.aurum.fieldresponse.domain.ResponseWorkflow
import com.aurum.fieldresponse.domain.TrackerReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FieldResponseUiState(
    val readings: List<TrackerReading> = emptyList(),
    val flags: List<IncidentFlag> = emptyList(),
    val selectedFlag: IncidentFlag? = null,
    val activeWorkflow: ResponseWorkflow = ResponseWorkflow.DoseAlarm,
    val latestAlert: IncidentFlag? = null,
)

class FieldResponseViewModel(
    repository: FieldResponseRepository = FieldResponseRepository(
        trackerSource = SyntheticTrackerSource(),
        hazardClassifier = HazardClassifier(),
    ),
) : ViewModel() {
    private val _uiState = MutableStateFlow(FieldResponseUiState())
    val uiState: StateFlow<FieldResponseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.snapshots().collect { snapshot ->
                val previousIds = _uiState.value.flags.map { it.id }.toSet()
                val newestFlag = snapshot.flags.firstOrNull { it.id !in previousIds }
                _uiState.value = _uiState.value.copy(
                    readings = snapshot.readings,
                    flags = snapshot.flags,
                    selectedFlag = _uiState.value.selectedFlag ?: newestFlag,
                    activeWorkflow = newestFlag?.workflow ?: _uiState.value.activeWorkflow,
                    latestAlert = newestFlag,
                )
            }
        }
    }

    fun selectFlag(flag: IncidentFlag) {
        _uiState.value = _uiState.value.copy(
            selectedFlag = flag,
            activeWorkflow = flag.workflow,
        )
    }

    fun selectWorkflow(workflow: ResponseWorkflow) {
        _uiState.value = _uiState.value.copy(activeWorkflow = workflow)
    }

    fun clearAlert() {
        _uiState.value = _uiState.value.copy(latestAlert = null)
    }
}
