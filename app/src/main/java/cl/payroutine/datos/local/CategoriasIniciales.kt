package cl.payroutine.datos.local

import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.TipoMovimiento
import java.util.Locale

internal val categoriasIniciales = listOf(
    Categoria(nombre = "Alimentación", tipo = TipoMovimiento.Gasto),
    Categoria(nombre = "Transporte", tipo = TipoMovimiento.Gasto),
    Categoria(nombre = "Hogar", tipo = TipoMovimiento.Gasto),
    Categoria(nombre = "Servicios", tipo = TipoMovimiento.Gasto),
    Categoria(nombre = "Ocio", tipo = TipoMovimiento.Gasto),
    Categoria(nombre = "Salud", tipo = TipoMovimiento.Gasto),
    Categoria(nombre = "Otros", tipo = TipoMovimiento.Gasto),
    Categoria(nombre = "Educación", tipo = TipoMovimiento.Gasto),
    Categoria(nombre = "Vestuario", tipo = TipoMovimiento.Gasto),
    Categoria(nombre = "Mascotas", tipo = TipoMovimiento.Gasto),
    Categoria(nombre = "Sueldo", tipo = TipoMovimiento.Ingreso),
    Categoria(nombre = "Freelance", tipo = TipoMovimiento.Ingreso),
    Categoria(nombre = "Ventas", tipo = TipoMovimiento.Ingreso),
    Categoria(nombre = "Arriendos", tipo = TipoMovimiento.Ingreso),
    Categoria(nombre = "Pensiones", tipo = TipoMovimiento.Ingreso),
    Categoria(nombre = "Otros ingresos", tipo = TipoMovimiento.Ingreso),
)

// Se compara sin modificar el nombre ni el identificador de los registros existentes.
internal fun normalizarNombreCategoria(nombre: String): String =
    nombre.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
