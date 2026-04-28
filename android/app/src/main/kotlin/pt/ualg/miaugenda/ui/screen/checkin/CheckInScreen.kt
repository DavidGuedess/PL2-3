package pt.ualg.miaugenda.ui.screen.checkin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.data.remote.RetrofitClient
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    onBack: () -> Unit
) {
    var isWorking by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val attendanceApi = RetrofitClient.attendanceApi

    val currentDate = LocalDate.now()
    val currentTime = LocalTime.now()

    val displayDate = currentDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    val displayTime = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))

    fun loadMyAttendanceHistory() {
        scope.launch {
            isLoading = true
            message = null

            try {
                val response = attendanceApi.getMyHistory()

                if (response.isSuccessful) {
                    val records = response.body().orEmpty()
                    val lastRecord = records.firstOrNull()
                    isWorking = lastRecord?.type == "IN"
                } else if (response.code() == 401) {
                    message = "Utilizador não autorizado"
                } else {
                    message = "Erro ao carregar histórico"
                }
            } catch (e: Exception) {
                message = "Erro: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadMyAttendanceHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-In") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = displayDate, fontSize = 18.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = displayTime, fontSize = 32.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isWorking) "Em serviço" else "Fora de serviço",
                color = if (isWorking) Color(0xFF2E7D32) else Color.Red,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            message?.let {
                Text(
                    text = it,
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = null

                        try {
                            val typeToSend = if (isWorking) "OUT" else "IN"

                            val response = attendanceApi.register(
                                mapOf("type" to typeToSend)
                            )

                            if (response.isSuccessful) {
                                isWorking = !isWorking
                                message = if (typeToSend == "IN") {
                                    "Entrada registada com sucesso"
                                } else {
                                    "Saída registada com sucesso"
                                }
                            } else if (response.code() == 400) {
                                message = "Sequência inválida de registo"
                            } else if (response.code() == 401) {
                                message = "Utilizador não autorizado"
                            } else {
                                message = "Erro ao registar ponto"
                            }
                        } catch (e: Exception) {
                            message = "Erro: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(
                    text = when {
                        isLoading -> "A processar..."
                        isWorking -> "Sair"
                        else -> "Entrar"
                    }
                )
            }
        }
    }
}