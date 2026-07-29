package com.horastrabajo.app.ui.resumen

import com.horastrabajo.app.data.repository.DineroExtraRepository
import com.horastrabajo.app.data.repository.EntradaHorasRepository
import com.horastrabajo.app.data.repository.TarifaMensualRepository
import com.horastrabajo.app.data.repository.TrabajoRepository
import com.horastrabajo.app.ui.BaseResumenViewModel

class ResumenViewModel(
    trabajoRepository: TrabajoRepository,
    entradaHorasRepository: EntradaHorasRepository,
    tarifaMensualRepository: TarifaMensualRepository,
    dineroExtraRepository: DineroExtraRepository,
) : BaseResumenViewModel(
    trabajoRepository = trabajoRepository,
    entradaHorasRepository = entradaHorasRepository,
    tarifaMensualRepository = tarifaMensualRepository,
    dineroExtraRepository = dineroExtraRepository,
)
