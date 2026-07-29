package com.horastrabajo.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horastrabajo.app.data.preferences.TemaPreferido
import com.horastrabajo.app.ui.AppViewModelProvider
import com.horastrabajo.app.ui.ajustes.AjustesViewModel
import com.horastrabajo.app.ui.navigation.HorasTrabajoNavGraph
import com.horastrabajo.app.ui.theme.HorasTrabajoTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ajustesViewModel: AjustesViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val temaPreferido by ajustesViewModel.temaPreferido.collectAsState()
            val temaOscuro = when (temaPreferido) {
                TemaPreferido.SISTEMA -> isSystemInDarkTheme()
                TemaPreferido.CLARO -> false
                TemaPreferido.OSCURO -> true
            }
            HorasTrabajoTheme(darkTheme = temaOscuro) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HorasTrabajoNavGraph()
                }
            }
        }
    }
}
