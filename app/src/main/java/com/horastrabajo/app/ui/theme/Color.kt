package com.horastrabajo.app.ui.theme

import androidx.compose.ui.graphics.Color

val Verde40 = Color(0xFF2E7D32)
val Verde80 = Color(0xFFA5D6A7)
val VerdeGris40 = Color(0xFF52634F)
val VerdeGris80 = Color(0xFFB9CCB4)

// Tonos "soft" para fondos/superficies: nunca negro ni blanco puro.
val FondoOscuro = Color(0xFF1E1E1E)
val SuperficieOscura = Color(0xFF252526)
val SuperficieVarianteOscura = Color(0xFF2D2D30)

val FondoClaro = Color(0xFFFAF9F6)
val SuperficieClara = Color(0xFFF1F0EC)
val SuperficieVarianteClara = Color(0xFFE7E5DF)

// Roles "container" de M3 (mismo matiz que Verde40/80 y VerdeGris40/80): sin esto, componentes
// como el FloatingActionButton caen en el morado por defecto de Material3 en vez del verde de marca.
val PrimarioContenedorClaro = Color(0xFFDAF1DB)
val OnPrimarioContenedorClaro = Color(0xFF153817)
val PrimarioContenedorOscuro = Color(0xFF2A6F2E)
val OnPrimarioContenedorOscuro = Color(0xFFDAF1DB)

val SecundarioContenedorClaro = Color(0xFFE4E8E3)
val OnSecundarioContenedorClaro = Color(0xFF232A22)
val SecundarioContenedorOscuro = Color(0xFF475544)
val OnSecundarioContenedorOscuro = Color(0xFFE4E8E3)
