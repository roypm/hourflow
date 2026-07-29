package com.horastrabajo.app.ui.plantillas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horastrabajo.app.data.repository.PlantillaRepository
import com.horastrabajo.app.domain.model.PlantillaMes
import com.horastrabajo.app.domain.model.PlantillaSemana
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlantillasViewModel(
    private val plantillaRepository: PlantillaRepository,
) : ViewModel() {

    private val _trabajoId = MutableStateFlow(0L)

    val plantillasSemana: StateFlow<List<PlantillaSemana>> =
        _trabajoId.flatMapLatest { plantillaRepository.getPlantillasSemana(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plantillasMes: StateFlow<List<PlantillaMes>> =
        _trabajoId.flatMapLatest { plantillaRepository.getPlantillasMes(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cargar(trabajoId: Long) {
        _trabajoId.value = trabajoId
    }

    fun savePlantillaSemana(plantilla: PlantillaSemana) = viewModelScope.launch {
        plantillaRepository.savePlantillaSemana(plantilla)
    }

    fun deletePlantillaSemana(plantilla: PlantillaSemana) = viewModelScope.launch {
        plantillaRepository.deletePlantillaSemana(plantilla)
    }

    fun savePlantillaMes(plantilla: PlantillaMes) = viewModelScope.launch {
        plantillaRepository.savePlantillaMes(plantilla)
    }

    fun deletePlantillaMes(plantilla: PlantillaMes) = viewModelScope.launch {
        plantillaRepository.deletePlantillaMes(plantilla)
    }
}
