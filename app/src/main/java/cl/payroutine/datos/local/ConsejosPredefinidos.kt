package cl.payroutine.datos.local

import cl.payroutine.datos.local.entidad.ConsejoAhorro

internal val consejosPredefinidos = listOf(
    ConsejoAhorro("gastos-hormiga", "Anota tus gastos hormiga",
        "Los gastos chicos del día a día se acumulan. Registrarlos te ayuda a verlos.", 1),
    ConsejoAhorro("ahorrar-al-recibir", "Ahorra apenas recibas tu ingreso",
        "Separa un monto fijo al inicio del mes, aunque sea pequeño, antes de gastar.", 2),
    ConsejoAhorro("vencimientos", "Revisa tus pagos pendientes",
        "Mira los vencimientos de la semana para evitar atrasos y recargos.", 3),
    ConsejoAhorro("presupuesto-semanal", "Compara tu presupuesto cada semana",
        "Si una categoría va muy alta a mitad de mes, ajústala antes de que termine.", 4),
)
