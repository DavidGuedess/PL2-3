package pt.ualg.miaugenda.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.ualg.miaugenda.data.local.TokenManager

class LoginViewModelFactory(
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(tokenManager = tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
