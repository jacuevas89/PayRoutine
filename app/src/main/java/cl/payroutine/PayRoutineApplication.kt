package cl.payroutine

import android.app.Application
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.repositorio.RepositorioFinanzas

class PayRoutineApplication : Application() {
    val repositorio by lazy {
        RepositorioFinanzas(BaseDatosPayRoutine.crear(this))
    }
}
