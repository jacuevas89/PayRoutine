package cl.payroutine.datos.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cl.payroutine.datos.local.dao.CategoriaDao
import cl.payroutine.datos.local.dao.MovimientoDao
import cl.payroutine.datos.local.dao.PagoPendienteDao
import cl.payroutine.datos.local.dao.PresupuestoDao
import cl.payroutine.datos.local.dao.UsuarioDao
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.Presupuesto
import cl.payroutine.datos.local.entidad.Usuario

@Database(
    entities = [Categoria::class, Movimiento::class, Presupuesto::class, PagoPendiente::class, Usuario::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = MigracionCategoriasPredefinidas::class),
    ],
)
@TypeConverters(ConvertidoresRoom::class)
abstract class BaseDatosPayRoutine : RoomDatabase() {
    abstract fun categoriaDao(): CategoriaDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun presupuestoDao(): PresupuestoDao
    abstract fun pagoPendienteDao(): PagoPendienteDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        fun crear(contexto: Context): BaseDatosPayRoutine =
            Room.databaseBuilder(
                contexto.applicationContext,
                BaseDatosPayRoutine::class.java,
                "payroutine.db",
            ).build()
    }
}
