package com.horastrabajo.app.data.local

import com.horastrabajo.app.data.local.entity.PlantillaMesDetalleEntity
import com.horastrabajo.app.data.local.entity.PlantillaMesEntity
import com.horastrabajo.app.data.local.entity.PlantillaSemanaDetalleEntity
import com.horastrabajo.app.data.local.entity.PlantillaSemanaEntity

data class PlantillaSemanaBackupImport(
    val entity: PlantillaSemanaEntity,
    val detalles: List<PlantillaSemanaDetalleEntity>,
)

data class PlantillaMesBackupImport(
    val entity: PlantillaMesEntity,
    val detalles: List<PlantillaMesDetalleEntity>,
)
