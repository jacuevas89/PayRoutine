package cl.payroutine.datos.local

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.payroutine.datos.local.entidad.TipoMovimiento
import java.util.Locale

internal val categoriasPredefinidas = mapOf(
    TipoMovimiento.Gasto to listOf(
        "Alimentación", "Transporte", "Hogar", "Servicios", "Ocio", "Salud", "Otros",
        "Educación", "Vestuario", "Mascotas",
    ),
    TipoMovimiento.Ingreso to listOf("Sueldo", "Freelance", "Ventas", "Arriendos", "Pensiones", "Otros ingresos"),
)

// Se compara sin modificar el nombre ni el identificador de los registros existentes.
internal fun normalizarNombreCategoria(nombre: String): String =
    nombre.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

internal const val INSERTAR_CATEGORIA_PREDEFINIDA = """
    INSERT INTO Categoria (nombre, tipo, es_predefinida)
    SELECT :nombre, :tipo, 1
    WHERE NOT EXISTS (
        SELECT 1 FROM Categoria WHERE nombre = :nombre AND tipo = :tipo AND es_predefinida = 1
    )
"""

class MigracionCategoriasPredefinidas : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        for ((tipo, nombres) in categoriasPredefinidas) {
            for (nombre in nombres) {
                db.execSQL(INSERTAR_CATEGORIA_PREDEFINIDA, arrayOf(nombre, tipo.name))
            }
        }
    }
}
