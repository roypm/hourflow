package com.horastrabajo.app.domain.model

data class Trabajo(
    val id: Long = 0,
    val nombre: String,
    val nombreUsuario: String,
    val simboloMoneda: String = "€",
    val fotoUri: String? = null,
)
