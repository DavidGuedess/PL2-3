package pt.ualg.miaugenda

import android.app.Application
import android.util.Log
import pt.ualg.miaugenda.data.notif.createRequestNotificationChannel
import pt.ualg.miaugenda.data.notif.schedulePollingForRequests

class MiauGendaApplication : Application() {
    companion object {
        private const val TAG = "MiauGendaApp"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Log.d(TAG, "Inicializando MiauGendaApp...")
            MiauGendaApp.initialize(this)
            createRequestNotificationChannel(this)
            if (MiauGendaApp.getTokenManager(this).getAccessToken() != null) {
                schedulePollingForRequests(this)
            }
            Log.d(TAG, "MiauGendaApp inicializado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar MiauGendaApp", e)
        }
    }
}
