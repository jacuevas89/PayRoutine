package cl.payroutine.datos.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.payroutine.datos.local.dao.CategoriaDao
import cl.payroutine.datos.local.dao.MovimientoDao
import cl.payroutine.datos.local.dao.PagoPendienteDao
import cl.payroutine.datos.local.dao.PresupuestoDao
import cl.payroutine.datos.local.dao.PresupuestoMensualDao
import cl.payroutine.datos.local.dao.RecordatorioMovimientoDao
import cl.payroutine.datos.local.dao.UsuarioDao
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.Presupuesto
import cl.payroutine.datos.local.entidad.PresupuestoMensual
import cl.payroutine.datos.local.entidad.PreferenciasPresupuesto
import cl.payroutine.datos.local.entidad.RecordatorioMovimiento
import cl.payroutine.datos.local.entidad.Usuario

@Database(
    entities = [
        Categoria::class, Movimiento::class, Presupuesto::class,
        PagoPendiente::class, Usuario::class, RecordatorioMovimiento::class,
        PresupuestoMensual::class, PreferenciasPresupuesto::class,
    ],
    version = 6,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = MigracionCategoriasPredefinidas::class),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
@TypeConverters(ConvertidoresRoom::class)
abstract class BaseDatosPayRoutine : RoomDatabase() {
    abstract fun categoriaDao(): CategoriaDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun presupuestoDao(): PresupuestoDao
    abstract fun presupuestoMensualDao(): PresupuestoMensualDao
    abstract fun pagoPendienteDao(): PagoPendienteDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun recordatorioMovimientoDao(): RecordatorioMovimientoDao

    companion object {
        fun crear(contexto: Context): BaseDatosPayRoutine =
            Room.databaseBuilder(
                contexto.applicationContext,
                BaseDatosPayRoutine::class.java,
                "payroutine.db",
            ).addMigrations(MIGRACION_5_6).build()
    }
}

internal val MIGRACION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE PresupuestoMensual (
                anio INTEGER NOT NULL, mes INTEGER NOT NULL, modalidad TEXT NOT NULL,
                limiteGeneral REAL, generado INTEGER NOT NULL, ingresoReferencia REAL,
                PRIMARY KEY(anio, mes)
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO PresupuestoMensual (anio, mes, modalidad, generado)
            SELECT DISTINCT anio, mes, 'Categorias', 0 FROM Presupuesto
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE PreferenciasPresupuesto (
                usuarioId TEXT NOT NULL PRIMARY KEY, configuracionPendiente INTEGER NOT NULL,
                primerMesAutomatico INTEGER, avisoActivo INTEGER NOT NULL, diaAviso INTEGER NOT NULL,
                minutoAviso INTEGER NOT NULL, ultimoMesAvisado INTEGER,
                FOREIGN KEY(usuarioId) REFERENCES Usuario(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
    }
}
