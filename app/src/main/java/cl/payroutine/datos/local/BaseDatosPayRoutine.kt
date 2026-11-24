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
import cl.payroutine.datos.local.dao.ObligacionPagoDao
import cl.payroutine.datos.local.dao.PagoPendienteDao
import cl.payroutine.datos.local.dao.PresupuestoDao
import cl.payroutine.datos.local.dao.PresupuestoMensualDao
import cl.payroutine.datos.local.dao.RecordatorioMovimientoDao
import cl.payroutine.datos.local.dao.UsuarioDao
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.ObligacionPago
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
        PresupuestoMensual::class, PreferenciasPresupuesto::class, ObligacionPago::class,
    ],
    version = 7,
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
    abstract fun obligacionPagoDao(): ObligacionPagoDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun recordatorioMovimientoDao(): RecordatorioMovimientoDao

    companion object {
        fun crear(contexto: Context, nombre: String = "payroutine.db"): BaseDatosPayRoutine =
            Room.databaseBuilder(
                contexto.applicationContext,
                BaseDatosPayRoutine::class.java,
                nombre,
            ).addMigrations(MIGRACION_5_6, MIGRACION_6_7).build()
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

private val MIGRACION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // La versión 5 local de Pagos tenía obligaciones en lugar de recordatorios.
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS RecordatorioMovimiento (
                usuario_id TEXT NOT NULL PRIMARY KEY, activo INTEGER NOT NULL, minutoDelDia INTEGER NOT NULL,
                fechaInicial INTEGER, fechaReferencia INTEGER NOT NULL, recurrencia TEXT NOT NULL,
                programacionId TEXT NOT NULL, ultimoAviso INTEGER,
                FOREIGN KEY(usuario_id) REFERENCES Usuario(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ObligacionPago (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, categoria_id INTEGER NOT NULL,
                FOREIGN KEY(categoria_id) REFERENCES Categoria(id) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ObligacionPago_categoria_id ON ObligacionPago(categoria_id)")
        val tieneObligacion = db.query("PRAGMA table_info(PagoPendiente)").use { columnas ->
            val nombre = columnas.getColumnIndexOrThrow("name")
            generateSequence { if (columnas.moveToNext()) columnas.getString(nombre) else null }
                .any { it == "obligacion_id" }
        }
        if (!tieneObligacion) {
            db.execSQL("ALTER TABLE PagoPendiente ADD COLUMN obligacion_id INTEGER REFERENCES ObligacionPago(id)")
        }
        db.execSQL("CREATE INDEX IF NOT EXISTS index_PagoPendiente_obligacion_id ON PagoPendiente(obligacion_id)")
    }
}
