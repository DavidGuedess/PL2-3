package pt.ualg.miaugenda

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppState {
    private val _tokenExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val tokenExpired = _tokenExpired.asSharedFlow()

    fun signalTokenExpired() {
        _tokenExpired.tryEmit(Unit)
    }
}
