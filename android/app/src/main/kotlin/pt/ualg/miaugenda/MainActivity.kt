package pt.ualg.miaugenda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import pt.ualg.miaugenda.navigation.NavGraph
import pt.ualg.miaugenda.navigation.getStartDestination
import pt.ualg.miaugenda.ui.theme.MiauGendaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiauGendaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = getStartDestination()
                    
                    NavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
