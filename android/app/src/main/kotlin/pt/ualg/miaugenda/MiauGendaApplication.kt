package pt.ualg.miaugenda

import android.app.Application
import android.util.Log

class MiauGendaApplication : Application() {
    companion object {
        private const val TAG = "MiauGendaApp"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Log.d(TAG, "Inicializando MiauGendaApp...")
            MiauGendaApp.initialize(this)
            Log.d(TAG, "MiauGendaApp inicializado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar MiauGendaApp", e)
        }
    }
}
