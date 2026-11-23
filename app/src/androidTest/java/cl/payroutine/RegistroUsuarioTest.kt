package cl.payroutine

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.MIGRACION_5_6
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegistroUsuarioTest {
    private val instrumentacion = InstrumentationRegistry.getInstrumentation()
    private val contexto = instrumentacion.targetContext

    @Test
    fun migracionConservaDatosYRegistroConservaUnSoloUuid() = runBlocking {
        val nombreBase = "rf19-${UUID.randomUUID()}.db"
        try {
            crearBaseAnterior(nombreBase, version = 1)
            val id = conBase(nombreBase) { base ->
                val repositorio = RepositorioFinanzas(base)
                assertNull(repositorio.observarUsuario().first())
                val categorias = repositorio.observarCategorias().first()
                assertEquals("Alimentación", categorias.single { it.id == 1 }.nombre)
                assertFalse(categorias.single { it.id == 1 }.esPredefinida)
                assertEquals(16, categorias.count { it.esPredefinida })
                assertEquals("Compra", repositorio.observarMovimientos().first().single().nota)
                assertEquals(40000.0, repositorio.observarPresupuestos().first().single().montoPresupuestado, 0.0)
                assertEquals("Internet", repositorio.observarPagos().first().single().descripcion)

                val nombre = "María-José D'Ávila"
                val apodo = "Jo Ávila"
                assertTrue(runCatching { repositorio.registrarUsuario("   ", apodo) }.exceptionOrNull() is IllegalArgumentException)
                assertTrue(runCatching { repositorio.registrarUsuario(nombre, "   ") }.exceptionOrNull() is IllegalArgumentException)
                assertNull(repositorio.observarUsuario().first())
                List(5) { async { repositorio.registrarUsuario("  $nombre ", " $apodo  ") } }.awaitAll()
                val usuario = requireNotNull(repositorio.observarUsuario().first())
                assertEquals(nombre, usuario.nombreCompleto)
                assertEquals(apodo, usuario.apodo)
                assertEquals(usuario.id, UUID.fromString(usuario.id).toString())
                base.openHelper.readableDatabase.query("SELECT COUNT(*) FROM Usuario").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(1, cursor.getInt(0))
                }
                assertEquals(1, repositorio.actualizarDatosUsuario(usuario.id, " Ana D'Ávila ", " Áni "))
                usuario.id
            }

            conBase(nombreBase) { base ->
                val repositorio = RepositorioFinanzas(base)
                repositorio.registrarUsuario("Reintento", "Reintento")
                val usuario = requireNotNull(repositorio.observarUsuario().first())
                assertEquals(id, usuario.id)
                assertEquals("Ana D'Ávila", usuario.nombreCompleto)
                assertEquals("Áni", usuario.apodo)
            }
        } finally {
            contexto.deleteDatabase(nombreBase)
        }
        Unit
    }

    @Test
    fun migracionDesdeVersionDosAgregaCatalogoYConservaCategoriasYPerfil() = runBlocking {
        val nombreBase = "categorias-rf21-${UUID.randomUUID()}.db"
        val idUsuario = UUID.randomUUID().toString()
        try {
            crearBaseAnterior(nombreBase, version = 2, idUsuario = idUsuario)
            conBase(nombreBase) { base ->
                val repositorio = RepositorioFinanzas(base)
                val categorias = repositorio.observarCategorias().first()
                assertEquals(idUsuario, repositorio.observarUsuario().first()?.id)
                assertEquals(1, repositorio.observarMovimientos().first().single().categoriaId)
                assertEquals(1, repositorio.observarPresupuestos().first().single().categoriaId)
                assertEquals(17, categorias.size)
                assertFalse(categorias.single { it.id == 1 }.esPredefinida)
                assertEquals(16, categorias.count { it.esPredefinida })
                assertEquals(2, categorias.count { it.nombre == "Alimentación" })
            }
        } finally {
            contexto.deleteDatabase(nombreBase)
        }
        Unit
    }

    private inline fun <T> conBase(nombre: String, usar: (BaseDatosPayRoutine) -> T): T {
        val base = Room.databaseBuilder(contexto, BaseDatosPayRoutine::class.java, nombre).addMigrations(MIGRACION_5_6).build()
        try {
            return usar(base)
        } finally {
            base.close()
        }
    }

    private fun crearBaseAnterior(nombre: String, version: Int, idUsuario: String? = null) {
        val esquema = instrumentacion.context.assets.open(
            "${BaseDatosPayRoutine::class.java.name}/$version.json",
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val archivo = contexto.getDatabasePath(nombre)
        archivo.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(archivo, null).use { base ->
            val entidades = esquema.getJSONArray("entities")
            for (indice in 0 until entidades.length()) {
                val entidad = entidades.getJSONObject(indice)
                val tabla = entidad.getString("tableName")
                base.execSQL(entidad.getString("createSql").replace("\${TABLE_NAME}", tabla))
                val indices = entidad.optJSONArray("indices") ?: continue
                for (i in 0 until indices.length()) {
                    base.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", tabla))
                }
            }
            val consultas = esquema.getJSONArray("setupQueries")
            for (i in 0 until consultas.length()) base.execSQL(consultas.getString(i))
            base.execSQL("INSERT INTO Categoria VALUES (1, 'Alimentación', 'Gasto')")
            base.execSQL("INSERT INTO Movimiento VALUES (1, 'Gasto', 5000, 0, 1, 'Compra')")
            base.execSQL("INSERT INTO Presupuesto VALUES (1, 1, 40000, 11, 2026)")
            base.execSQL("INSERT INTO PagoPendiente VALUES (1, 'Internet', 20000, 0, 0, NULL, NULL, 0)")
            if (idUsuario != null) {
                base.execSQL(
                    "INSERT INTO Usuario (id, nombre_completo, apodo) VALUES (?, ?, ?)",
                    arrayOf(idUsuario, "María Pérez", "Mari"),
                )
            }
            base.version = version
        }
    }
}
