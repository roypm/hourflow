package com.horastrabajo.app.data.export

import com.horastrabajo.app.data.repository.DineroExtraRepository
import com.horastrabajo.app.data.repository.EntradaHorasRepository
import com.horastrabajo.app.data.repository.TarifaMensualRepository
import com.horastrabajo.app.data.repository.TrabajoRepository
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.TarifaMensual
import com.horastrabajo.app.domain.model.Trabajo
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Repositorios en memoria para tests de dominio/export puros (sin Room ni Android).
 * Solo implementan de verdad lo que [JsonBackupManager] usa; el resto lanza si se llama
 * por error, para detectar en el test cualquier dependencia nueva no cubierta por el fake.
 */
class FakeTrabajoRepository : TrabajoRepository {
    private val siguienteId = AtomicLong(1)
    private val estado = MutableStateFlow<List<Trabajo>>(emptyList())

    override fun observeAll(): Flow<List<Trabajo>> = estado
    override fun observeById(trabajoId: Long): Flow<Trabajo?> = estado.map { lista -> lista.firstOrNull { it.id == trabajoId } }

    override suspend fun guardar(trabajo: Trabajo): Long {
        val id = if (trabajo.id == 0L) siguienteId.getAndIncrement() else trabajo.id
        estado.value = estado.value.filterNot { it.id == id } + trabajo.copy(id = id)
        return id
    }

    override suspend fun eliminar(trabajo: Trabajo) {
        estado.value = estado.value.filterNot { it.id == trabajo.id }
    }
}

class FakeEntradaHorasRepository : EntradaHorasRepository {
    private val siguienteId = AtomicLong(1)
    private val datos = mutableListOf<EntradaHoras>()

    override fun observePorMes(trabajoId: Long, anio: Int, mes: Int): Flow<List<EntradaHoras>> = error("no usado en este test")
    override fun observePorAnio(trabajoId: Long, anio: Int): Flow<List<EntradaHoras>> = error("no usado en este test")
    override fun observeTodas(): Flow<List<EntradaHoras>> = flowOf(datos.toList())
    override fun observePorFecha(trabajoId: Long, fecha: LocalDate): Flow<List<EntradaHoras>> = error("no usado en este test")

    override suspend fun guardar(entrada: EntradaHoras): Long {
        val id = if (entrada.id == 0L) siguienteId.getAndIncrement() else entrada.id
        datos.removeAll { it.id == id }
        datos += entrada.copy(id = id)
        return id
    }

    override suspend fun eliminar(entrada: EntradaHoras) {
        datos.removeAll { it.id == entrada.id }
    }

    override suspend fun obtenerTodasDelTrabajo(trabajoId: Long): List<EntradaHoras> =
        datos.filter { it.trabajoId == trabajoId }

    override suspend fun guardarVarias(entradas: List<EntradaHoras>) {
        entradas.forEach { guardar(it) }
    }

    override suspend fun restaurar(entrada: EntradaHoras) {
        datos.removeAll { it.id == entrada.id }
        datos += entrada
    }

    override suspend fun getEntradasSemana(primerDiaSemana: LocalDate, trabajoId: Long): List<EntradaHoras> {
        val ultimoDia = primerDiaSemana.plusDays(6)
        return datos.filter { it.trabajoId == trabajoId && it.fecha in primerDiaSemana..ultimoDia }
    }

    override suspend fun getEntradasMes(anio: Int, mes: Int, trabajoId: Long): List<EntradaHoras> {
        val yearMonth = YearMonth.of(anio, mes)
        return datos.filter { it.trabajoId == trabajoId && it.fecha in yearMonth.atDay(1)..yearMonth.atEndOfMonth() }
    }

    override suspend fun deleteEntradasEnFechas(fechas: List<LocalDate>, trabajoId: Long) {
        datos.removeAll { it.trabajoId == trabajoId && it.fecha in fechas }
    }

    override suspend fun reemplazarEnFechas(trabajoId: Long, fechas: List<LocalDate>, nuevas: List<EntradaHoras>) {
        deleteEntradasEnFechas(fechas, trabajoId)
        guardarVarias(nuevas)
    }

    override suspend fun deleteEntradasEnMes(trabajoId: Long, anio: Int, mes: Int) {
        val yearMonth = YearMonth.of(anio, mes)
        datos.removeAll { it.trabajoId == trabajoId && it.fecha in yearMonth.atDay(1)..yearMonth.atEndOfMonth() }
    }
}

class FakeTarifaMensualRepository : TarifaMensualRepository {
    private val siguienteId = AtomicLong(1)
    private val datos = mutableListOf<TarifaMensual>()

    override suspend fun obtenerTarifaVigente(trabajoId: Long, anio: Int, mes: Int): TarifaMensual? = error("no usado en este test")
    override suspend fun fijarTarifaDelMes(trabajoId: Long, anio: Int, mes: Int, precioPorHora: Dinero): Unit = error("no usado en este test")
    override fun observeTodasDelTrabajo(trabajoId: Long): Flow<List<TarifaMensual>> = error("no usado en este test")
    override fun observeTodas(): Flow<List<TarifaMensual>> = flowOf(datos.toList())

    override suspend fun obtenerTodasDelTrabajo(trabajoId: Long): List<TarifaMensual> =
        datos.filter { it.trabajoId == trabajoId }

    override suspend fun guardarVarias(tarifas: List<TarifaMensual>) {
        tarifas.forEach { tarifa ->
            val id = if (tarifa.id == 0L) siguienteId.getAndIncrement() else tarifa.id
            datos.removeAll { it.id == id }
            datos += tarifa.copy(id = id)
        }
    }

    override suspend fun eliminarTarifaDelMes(trabajoId: Long, anio: Int, mes: Int) {
        datos.removeAll { it.trabajoId == trabajoId && it.anio == anio && it.mes == mes }
    }
}

class FakeDineroExtraRepository : DineroExtraRepository {
    private val siguienteId = AtomicLong(1)
    private val datos = mutableListOf<DineroExtra>()

    override fun observePorMes(trabajoId: Long, anio: Int, mes: Int): Flow<List<DineroExtra>> = error("no usado en este test")
    override fun observePorAnio(trabajoId: Long, anio: Int): Flow<List<DineroExtra>> = error("no usado en este test")
    override fun observeTodo(): Flow<List<DineroExtra>> = flowOf(datos.toList())

    override suspend fun guardar(dineroExtra: DineroExtra): Long {
        val id = if (dineroExtra.id == 0L) siguienteId.getAndIncrement() else dineroExtra.id
        datos.removeAll { it.id == id }
        datos += dineroExtra.copy(id = id)
        return id
    }

    override suspend fun eliminar(dineroExtra: DineroExtra) {
        datos.removeAll { it.id == dineroExtra.id }
    }

    override suspend fun obtenerTodoDelTrabajo(trabajoId: Long): List<DineroExtra> =
        datos.filter { it.trabajoId == trabajoId }

    override suspend fun guardarVarios(dineroExtra: List<DineroExtra>) {
        dineroExtra.forEach { guardar(it) }
    }

    override suspend fun restaurar(item: DineroExtra) {
        datos.removeAll { it.id == item.id }
        datos += item
    }

    override suspend fun deletePorMes(trabajoId: Long, anio: Int, mes: Int) {
        val yearMonth = YearMonth.of(anio, mes)
        datos.removeAll { it.trabajoId == trabajoId && it.fecha in yearMonth.atDay(1)..yearMonth.atEndOfMonth() }
    }
}
