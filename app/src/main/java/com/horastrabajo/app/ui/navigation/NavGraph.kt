package com.horastrabajo.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.horastrabajo.app.ui.ajustes.AjustesScreen
import com.horastrabajo.app.ui.anio.AnioScreen
import com.horastrabajo.app.ui.mes.MesScreen
import com.horastrabajo.app.ui.plantillas.PlantillasScreen
import com.horastrabajo.app.ui.resumen.ResumenScreen
import com.horastrabajo.app.ui.trabajos.TrabajosScreen

private const val RUTA_TRABAJOS = "trabajos"
private const val RUTA_AJUSTES = "ajustes"
private const val RUTA_ANIO = "anio/{trabajoId}"
private const val RUTA_MES = "mes/{trabajoId}/{anio}/{mes}"
private const val RUTA_RESUMEN = "resumen/{trabajoId}/{anio}/{mes}"
private const val RUTA_PLANTILLAS = "plantillas/{trabajoId}"

private fun rutaAnio(trabajoId: Long) = "anio/$trabajoId"
private fun rutaMes(trabajoId: Long, anio: Int, mes: Int) = "mes/$trabajoId/$anio/$mes"
private fun rutaResumen(trabajoId: Long, anio: Int, mes: Int) = "resumen/$trabajoId/$anio/$mes"
private fun rutaPlantillas(trabajoId: Long) = "plantillas/$trabajoId"

private fun Int.validarMes(): Int? = if (this in 1..12) this else null

@Composable
fun HorasTrabajoNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = RUTA_TRABAJOS) {
        composable(RUTA_TRABAJOS) {
            TrabajosScreen(
                onTrabajoSeleccionado = { trabajoId -> navController.navigate(rutaAnio(trabajoId)) },
                onAbrirAjustes = { navController.navigate(RUTA_AJUSTES) },
            )
        }
        composable(RUTA_AJUSTES) {
            AjustesScreen(
                onVolver = { navController.popBackStack() },
            )
        }
        composable(RUTA_ANIO) { backStackEntry ->
            val trabajoId = backStackEntry.arguments?.getString("trabajoId")?.toLongOrNull() ?: return@composable
            AnioScreen(
                trabajoId = trabajoId,
                onMesSeleccionado = { anio, mes -> navController.navigate(rutaMes(trabajoId, anio, mes)) },
                onVolver = { navController.popBackStack() },
                onAbrirPlantillas = { navController.navigate(rutaPlantillas(trabajoId)) },
            )
        }
        composable(RUTA_PLANTILLAS) { backStackEntry ->
            val trabajoId = backStackEntry.arguments?.getString("trabajoId")?.toLongOrNull() ?: return@composable
            PlantillasScreen(
                trabajoId = trabajoId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(RUTA_MES) { backStackEntry ->
            val trabajoId = backStackEntry.arguments?.getString("trabajoId")?.toLongOrNull() ?: return@composable
            val anio = backStackEntry.arguments?.getString("anio")?.toIntOrNull() ?: return@composable
            val mes = backStackEntry.arguments?.getString("mes")?.toIntOrNull()?.validarMes() ?: return@composable
            MesScreen(
                trabajoId = trabajoId,
                anio = anio,
                mes = mes,
                onVerResumen = { navController.navigate(rutaResumen(trabajoId, anio, mes)) },
                onVolver = { navController.popBackStack() },
            )
        }
        composable(RUTA_RESUMEN) { backStackEntry ->
            val trabajoId = backStackEntry.arguments?.getString("trabajoId")?.toLongOrNull() ?: return@composable
            val anio = backStackEntry.arguments?.getString("anio")?.toIntOrNull() ?: return@composable
            val mes = backStackEntry.arguments?.getString("mes")?.toIntOrNull()?.validarMes() ?: return@composable
            ResumenScreen(
                trabajoId = trabajoId,
                anio = anio,
                mes = mes,
                onVolver = { navController.popBackStack() },
            )
        }
    }
}
