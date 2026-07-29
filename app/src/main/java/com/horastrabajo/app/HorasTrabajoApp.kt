package com.horastrabajo.app

import android.app.Application
import com.horastrabajo.app.data.export.JsonBackupManager
import com.horastrabajo.app.data.local.AppDatabase
import com.horastrabajo.app.data.preferences.ThemePreferenceRepository
import com.horastrabajo.app.data.repository.DineroExtraRepository
import com.horastrabajo.app.data.repository.DineroExtraRepositoryImpl
import com.horastrabajo.app.data.repository.EntradaHorasRepository
import com.horastrabajo.app.data.repository.EntradaHorasRepositoryImpl
import com.horastrabajo.app.data.repository.PlantillaRepository
import com.horastrabajo.app.data.repository.TarifaMensualRepository
import com.horastrabajo.app.data.repository.TarifaMensualRepositoryImpl
import com.horastrabajo.app.data.repository.TrabajoRepository
import com.horastrabajo.app.data.repository.TrabajoRepositoryImpl

class HorasTrabajoApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var trabajoRepository: TrabajoRepository
        private set

    lateinit var entradaHorasRepository: EntradaHorasRepository
        private set

    lateinit var tarifaMensualRepository: TarifaMensualRepository
        private set

    lateinit var dineroExtraRepository: DineroExtraRepository
        private set

    lateinit var plantillaRepository: PlantillaRepository
        private set

    lateinit var jsonBackupManager: JsonBackupManager
        private set

    lateinit var themePreferenceRepository: ThemePreferenceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.build(this)
        trabajoRepository = TrabajoRepositoryImpl(database.trabajoDao())
        entradaHorasRepository = EntradaHorasRepositoryImpl(database.entradaHorasDao())
        tarifaMensualRepository = TarifaMensualRepositoryImpl(database.tarifaMensualDao())
        dineroExtraRepository = DineroExtraRepositoryImpl(database.dineroExtraDao())
        plantillaRepository = PlantillaRepository(
            semanaDao = database.plantillaSemanaDao(),
            mesDao = database.plantillaMesDao(),
        )
        jsonBackupManager = JsonBackupManager(
            database = database,
            trabajoRepository = trabajoRepository,
            entradaHorasRepository = entradaHorasRepository,
            tarifaMensualRepository = tarifaMensualRepository,
            dineroExtraRepository = dineroExtraRepository,
        )
        themePreferenceRepository = ThemePreferenceRepository(this)
    }
}
