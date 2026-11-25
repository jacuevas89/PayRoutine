package cl.payroutine.datos.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.math.BigDecimal

internal val MIGRACION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val tablas = listOf("RecordatorioPago", "PagoPendiente", "ObligacionPago", "Movimiento", "Presupuesto", "PresupuestoMensual")
        db.execSQL("CREATE TEMP TABLE respaldo_secuencias AS SELECT name, seq FROM sqlite_sequence")
        for (tabla in tablas) {
            db.execSQL("CREATE TEMP TABLE respaldo_$tabla AS SELECT * FROM $tabla")
            db.execSQL("DROP TABLE $tabla")
        }
        db.execSQL("""
            CREATE TABLE Movimiento (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tipo TEXT NOT NULL, monto INTEGER NOT NULL,
                fecha INTEGER NOT NULL, categoria_id INTEGER, nota TEXT, nombre TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(categoria_id) REFERENCES Categoria(id) ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX index_Movimiento_categoria_id ON Movimiento(categoria_id)")
        db.execSQL("""
            INSERT INTO Movimiento SELECT m.id, m.tipo, CAST(ROUND(m.monto) AS INTEGER), m.fecha, c.id, m.nota, m.nombre
            FROM respaldo_Movimiento m LEFT JOIN Categoria c ON c.id = m.categoria_id
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE ObligacionPago (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, categoria_id INTEGER,
                FOREIGN KEY(categoria_id) REFERENCES Categoria(id) ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX index_ObligacionPago_categoria_id ON ObligacionPago(categoria_id)")
        db.execSQL("""
            INSERT INTO ObligacionPago SELECT o.id, o.nombre, c.id FROM respaldo_ObligacionPago o
            LEFT JOIN Categoria c ON c.id = o.categoria_id
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE PagoPendiente (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, descripcion TEXT NOT NULL,
                monto INTEGER NOT NULL, fecha_vencimiento INTEGER NOT NULL, es_cuota INTEGER NOT NULL,
                cuota_actual INTEGER, total_cuotas INTEGER, pagado INTEGER NOT NULL, obligacion_id INTEGER, movimiento_id INTEGER,
                FOREIGN KEY(obligacion_id) REFERENCES ObligacionPago(id),
                FOREIGN KEY(movimiento_id) REFERENCES Movimiento(id)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX index_PagoPendiente_obligacion_id ON PagoPendiente(obligacion_id)")
        db.execSQL("CREATE UNIQUE INDEX index_PagoPendiente_movimiento_id ON PagoPendiente(movimiento_id)")
        db.execSQL("""
            INSERT INTO PagoPendiente
            SELECT p.id, p.descripcion, CAST(ROUND(p.monto) AS INTEGER), p.fecha_vencimiento, p.es_cuota,
                   p.cuota_actual, p.total_cuotas, p.pagado, o.id, m.id
            FROM respaldo_PagoPendiente p LEFT JOIN ObligacionPago o ON o.id = p.obligacion_id
            LEFT JOIN Movimiento m ON m.id = p.movimiento_id
        """.trimIndent())
        repartirAgrupados(db, "PagoPendiente", "monto",
            "SELECT id, monto, obligacion_id FROM respaldo_PagoPendiente WHERE es_cuota = 1 AND obligacion_id IS NOT NULL ORDER BY obligacion_id, cuota_actual, id")
        db.execSQL("""
            UPDATE Movimiento SET monto = (SELECT p.monto FROM PagoPendiente p WHERE p.movimiento_id = Movimiento.id)
            WHERE id IN (SELECT movimiento_id FROM PagoPendiente WHERE movimiento_id IS NOT NULL)
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE RecordatorioPago (
                vencimientoId INTEGER NOT NULL PRIMARY KEY, vencimientoProgramado INTEGER,
                diaVencimientoProgramado INTEGER, diaProgramado INTEGER, ultimoAviso INTEGER,
                FOREIGN KEY(vencimientoId) REFERENCES PagoPendiente(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("INSERT INTO RecordatorioPago SELECT r.* FROM respaldo_RecordatorioPago r JOIN PagoPendiente p ON p.id = r.vencimientoId")
        db.execSQL("""
            CREATE TABLE ConfiguracionAvisoPago (
                clave TEXT NOT NULL PRIMARY KEY, usuarioId TEXT NOT NULL, obligacionId INTEGER,
                pagoAnteriorId INTEGER, activo INTEGER NOT NULL, anticipacion INTEGER NOT NULL,
                minuto INTEGER NOT NULL, frecuencia TEXT NOT NULL,
                FOREIGN KEY(usuarioId) REFERENCES Usuario(id) ON DELETE CASCADE,
                FOREIGN KEY(obligacionId) REFERENCES ObligacionPago(id) ON DELETE CASCADE,
                FOREIGN KEY(pagoAnteriorId) REFERENCES PagoPendiente(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX index_ConfiguracionAvisoPago_usuarioId ON ConfiguracionAvisoPago(usuarioId)")
        db.execSQL("CREATE UNIQUE INDEX index_ConfiguracionAvisoPago_obligacionId ON ConfiguracionAvisoPago(obligacionId)")
        db.execSQL("CREATE UNIQUE INDEX index_ConfiguracionAvisoPago_pagoAnteriorId ON ConfiguracionAvisoPago(pagoAnteriorId)")
        db.execSQL("""
            INSERT OR IGNORE INTO ConfiguracionAvisoPago
            SELECT CASE WHEN p.obligacion_id IS NULL THEN 'pago:' || p.id ELSE 'obligacion:' || p.obligacion_id END,
                a.usuarioId, p.obligacion_id, CASE WHEN p.obligacion_id IS NULL THEN p.id END,
                a.activo, a.anticipacion, a.minuto, a.frecuencia
            FROM RecordatorioPago r JOIN PagoPendiente p ON p.id = r.vencimientoId
            CROSS JOIN PreferenciasAvisosPago a
        """.trimIndent())
        db.execSQL("DROP TABLE PreferenciasAvisosPago")
        db.execSQL("""
            CREATE TABLE Presupuesto (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, categoria_id INTEGER,
                monto_presupuestado INTEGER NOT NULL, mes INTEGER NOT NULL, anio INTEGER NOT NULL,
                FOREIGN KEY(categoria_id) REFERENCES Categoria(id) ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX index_Presupuesto_categoria_id ON Presupuesto(categoria_id)")
        db.execSQL("""
            INSERT INTO Presupuesto SELECT p.id, c.id, CAST(ROUND(p.monto_presupuestado) AS INTEGER), p.mes, p.anio
            FROM respaldo_Presupuesto p LEFT JOIN Categoria c ON c.id = p.categoria_id
        """.trimIndent())
        repartirAgrupados(db, "Presupuesto", "monto_presupuestado",
            "SELECT id, monto_presupuestado, anio * 12 + mes FROM respaldo_Presupuesto ORDER BY anio, mes, id")
        db.execSQL("""
            CREATE TABLE PresupuestoMensual (
                anio INTEGER NOT NULL, mes INTEGER NOT NULL, modalidad TEXT NOT NULL, limiteGeneral INTEGER,
                generado INTEGER NOT NULL, ingresoReferencia INTEGER, PRIMARY KEY(anio, mes)
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO PresupuestoMensual SELECT anio, mes, modalidad, CAST(ROUND(limiteGeneral) AS INTEGER),
                generado, CAST(ROUND(ingresoReferencia) AS INTEGER) FROM respaldo_PresupuestoMensual
        """.trimIndent())
        db.execSQL("CREATE TABLE ConsejoAhorro (id TEXT NOT NULL PRIMARY KEY, titulo TEXT NOT NULL, detalle TEXT NOT NULL, orden INTEGER NOT NULL)")
        db.execSQL("""
            CREATE TABLE AvisoEmitido (
                id TEXT NOT NULL PRIMARY KEY, usuarioId TEXT NOT NULL, titulo TEXT NOT NULL, mensaje TEXT NOT NULL,
                fecha INTEGER NOT NULL, mostradoEnAndroid INTEGER NOT NULL, leido INTEGER NOT NULL,
                FOREIGN KEY(usuarioId) REFERENCES Usuario(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX index_AvisoEmitido_usuarioId ON AvisoEmitido(usuarioId)")
        for (tabla in tablas) db.execSQL("DROP TABLE respaldo_$tabla")
        db.execSQL("""
            UPDATE sqlite_sequence SET seq = MAX(seq, COALESCE((SELECT r.seq FROM respaldo_secuencias r WHERE r.name = sqlite_sequence.name), 0))
        """.trimIndent())
        db.execSQL("""
            INSERT INTO sqlite_sequence(name, seq) SELECT name, seq FROM respaldo_secuencias
            WHERE name NOT IN (SELECT name FROM sqlite_sequence)
        """.trimIndent())
        db.execSQL("DROP TABLE respaldo_secuencias")
    }

    private fun repartirAgrupados(db: SupportSQLiteDatabase, tabla: String, campo: String, consulta: String) {
        val grupos = linkedMapOf<Long, MutableList<Pair<Long, BigDecimal>>>()
        db.query(consulta).use { filas ->
            while (filas.moveToNext()) {
                grupos.getOrPut(filas.getLong(2)) { mutableListOf() }.add(
                    filas.getLong(0) to BigDecimal.valueOf(filas.getDouble(1)),
                )
            }
        }
        for (grupo in grupos.values) {
            val importes = repartirPesos(grupo.map { it.second })
            for (i in grupo.indices) db.execSQL(
                "UPDATE $tabla SET $campo = ? WHERE id = ?", arrayOf(importes[i], grupo[i].first),
            )
        }
    }
}
