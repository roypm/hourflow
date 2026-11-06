package com.horastrabajo.app.ui.resumen

import androidx.lifecycle.viewModelScope
import com.horastrabajo.app.data.preferences.FormatoResumen
import com.horastrabajo.app.data.preferences.FormatoResumenPreferenceRepository
import com.horastrabajo.app.data.repository.DineroExtraRepository
import com.horastrabajo.app.data.repository.EntradaHorasRepository
import com.horastrabajo.app.data.repository.TarifaMensualRepository
import com.horastrabajo.app.data.repository.TrabajoRepository
import com.horastrabajo.app.ui.BaseResumenViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Carga el [ResumenMensual] del período y lo expone para la pantalla y los exports. */
class ResumenViewModel(
    trabajoRepository: TrabajoRepository,
    entradaHorasRepository: EntradaHorasRepository,
    tarifaMensualRepository: TarifaMensualRepository,
    dineroExtraRepository: DineroExtraRepository,
    formatoResumenPreferenceRepository: FormatoResumenPreferenceRepository,
) : BaseResumenViewModel(
    trabajoRepository = trabajoRepository,
    entradaHorasRepository = entradaHorasRepository,
    tarifaMensualRepository = tarifaMensualRepository,
    dineroExtraRepository = dineroExtraRepository,
) {

    /** Formato elegido en Ajustes; es el que generan Compartir y Descargar. */
    val formatoResumen: StateFlow<FormatoResumen> = formatoResumenPreferenceRepository.formatoResumen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FormatoResumen.PDF)
}
