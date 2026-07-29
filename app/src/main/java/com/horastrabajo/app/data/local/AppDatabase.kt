package com.horastrabajo.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.horastrabajo.app.data.local.dao.DineroExtraDao
import com.horastrabajo.app.data.local.dao.EntradaHorasDao
import com.horastrabajo.app.data.local.dao.PlantillaMesDao
import com.horastrabajo.app.data.local.dao.PlantillaSemanaDao
import com.horastrabajo.app.data.local.dao.TarifaMensualDao
import com.horastrabajo.app.data.local.dao.TrabajoDao
import com.horastrabajo.app.data.local.entity.DineroExtraEntity
import com.horastrabajo.app.data.local.entity.EntradaHorasEntity
import com.horastrabajo.app.data.local.entity.PlantillaMesDetalleEntity
import com.horastrabajo.app.data.local.entity.PlantillaMesEntity
import com.horastrabajo.app.data.local.entity.PlantillaSemanaDetalleEntity
import com.horastrabajo.app.data.local.entity.PlantillaSemanaEntity
import com.horastrabajo.app.data.local.entity.TarifaMensualEntity
import com.horastrabajo.app.data.local.entity.TrabajoEntity

@Database(
    entities = [
        TrabajoEntity::class,
        EntradaHorasEntity::class,
        TarifaMensualEntity::class,
        DineroExtraEntity::class,
        PlantillaSemanaEntity::class,
        PlantillaSemanaDetalleEntity::class,
        PlantillaMesEntity::class,
        PlantillaMesDetalleEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trabajoDao(): TrabajoDao
    abstract fun entradaHorasDao(): EntradaHorasDao
    abstract fun tarifaMensualDao(): TarifaMensualDao
    abstract fun dineroExtraDao(): DineroExtraDao
    abstract fun plantillaSemanaDao(): PlantillaSemanaDao
    abstract fun plantillaMesDao(): PlantillaMesDao

    @Transaction
    suspend fun importarTrabajosCompletos(
        trabajos: List<TrabajoEntity>,
        entradas: List<List<EntradaHorasEntity>>,
        tarifas: List<List<TarifaMensualEntity>>,
        dinerosExtra: List<List<DineroExtraEntity>>,
    ) {
        for (i in trabajos.indices) {
            val nuevoId = trabajoDao().insert(trabajos[i])
            entradas.getOrNull(i)?.let { lista ->
                entradaHorasDao().insertAll(lista.map { it.copy(trabajoId = nuevoId) })
            }
            tarifas.getOrNull(i)?.let { lista ->
                tarifaMensualDao().insertAll(lista.map { it.copy(trabajoId = nuevoId) })
            }
            dinerosExtra.getOrNull(i)?.let { lista ->
                dineroExtraDao().insertAll(lista.map { it.copy(trabajoId = nuevoId) })
            }
        }
    }

    companion object {
        private const val DB_NAME = "horas_trabajo.db"

        /** Añade el nombre de la persona por Trabajo (campo nuevo, obligatorio en la app). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trabajo ADD COLUMN nombreUsuario TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Tarifa por hora custom para un turno concreto, distinta de la tarifa del mes. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entrada_horas ADD COLUMN precioPorHoraCustomCentimos INTEGER")
            }
        }

        /** Foto opcional del Trabajo: se guarda como URI persistente seleccionada por el usuario. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trabajo ADD COLUMN fotoUri TEXT")
            }
        }

        /** Plantillas de semana y mes. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS plantilla_semana (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        trabajo_id INTEGER NOT NULL,
                        nombre TEXT NOT NULL,
                        descripcion TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(trabajo_id) REFERENCES trabajos(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plantilla_semana_trabajo_id ON plantilla_semana(trabajo_id)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS plantilla_semana_detalle (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        plantilla_semana_id INTEGER NOT NULL,
                        dia_semana INTEGER NOT NULL,
                        hora_entrada TEXT,
                        hora_salida TEXT,
                        es_dia_siguiente INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(plantilla_semana_id) REFERENCES plantilla_semana(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plantilla_semana_detalle_plantilla_semana_id ON plantilla_semana_detalle(plantilla_semana_id)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS plantilla_mes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        trabajo_id INTEGER NOT NULL,
                        nombre TEXT NOT NULL,
                        descripcion TEXT NOT NULL DEFAULT '',
                        plantilla_semana_default_id INTEGER,
                        FOREIGN KEY(trabajo_id) REFERENCES trabajos(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plantilla_mes_trabajo_id ON plantilla_mes(trabajo_id)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS plantilla_mes_detalle (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        plantilla_mes_id INTEGER NOT NULL,
                        semana_del_mes INTEGER NOT NULL,
                        plantilla_semana_id INTEGER,
                        es_semana_libre INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(plantilla_mes_id) REFERENCES plantilla_mes(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plantilla_mes_detalle_plantilla_mes_id ON plantilla_mes_detalle(plantilla_mes_id)")
            }
        }

        /**
         * La base de datos arranca vacía en una instalación nueva: es la pantalla de
         * Trabajos la que da la bienvenida con su estado vacío y guía a crear el primer
         * trabajo, en vez de insertar datos de muestra que el usuario tendría que borrar.
         */
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
    }
}
