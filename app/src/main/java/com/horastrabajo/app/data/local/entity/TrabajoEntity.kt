package com.horastrabajo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trabajo")
data class TrabajoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val nombreUsuario: String = "",
    val simboloMoneda: String = "€",
    val fotoUri: String? = null,
)
