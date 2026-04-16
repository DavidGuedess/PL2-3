package com.miaugenda.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miaugenda.data.api.AttendanceService
import com.miaugenda.data.api.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import android.content.Intent
import com.miaugenda.MainActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen() {
    var isWorking by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val attendanceService = remember {
        RetrofitClient.create(context).create(AttendanceService::class.java)
    }

    /* Para testar as gateways pq ainda n está conectado a bd */
    val mockUserId = "13e8d266-ace1-4850-8fb5-58db1d8537c4"

    val currentDate = LocalDate.now()
    val currentTime = LocalTime.now()

    val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val apiDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val displayDate = currentDate.format(displayDateFormatter)
    val apiDate = currentDate.format(apiDateFormatter)
    val apiTime = currentTime.format(timeFormatter)

    LaunchedEffect(Unit) {
        isLoading = true
        message = null

        try {
            val response = attendanceService.getMyHistory(mockUserId)

            if (response.isSuccessful) {
                val attendances = response.body().orEmpty()

                val todayAttendance = attendances.find {
                    it.date == apiDate && it.checkOut == null
                }

                isWorking = todayAttendance != null
            } else {
                message = "Erro ao carregar histórico"
            }
        } catch (_: Exception) {
            message = "Erro de ligação ao servidor"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-In") },
                navigationIcon = {
                    IconButton(onClick = {
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        context.startActivity(intent)
                    }) {
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

            Text(text = apiTime, fontSize = 32.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isWorking) "Em serviço" else "Fora de serviço",
                color = if (isWorking) Color.Green else Color.Red,
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
                            if (!isWorking) {
                                val body = mapOf(
                                    "date" to apiDate,
                                    "checkIn" to apiTime
                                )

                                val response = attendanceService.register(mockUserId, body)

                                if (response.isSuccessful) {
                                    isWorking = true
                                    message = "Entrada registada com sucesso"
                                } else if (response.code() == 409) {
                                    val historyResponse = attendanceService.getMyHistory(mockUserId)

                                    if (historyResponse.isSuccessful) {
                                        val attendances = historyResponse.body().orEmpty()

                                        val todayOpenAttendance = attendances.find {
                                            it.date == apiDate && it.checkOut == null
                                        }

                                        isWorking = todayOpenAttendance != null

                                        message = if (isWorking) {
                                            "Já existe registo aberto para hoje"
                                        } else {
                                            "Já existe registo fechado para hoje"
                                        }
                                    } else {
                                        message = "Já existe registo para hoje"
                                    }
                                } else if (response.code() == 400) {
                                    message = "Dados inválidos"
                                } else if (response.code() == 401) {
                                    message = "Utilizador não autorizado"
                                } else {
                                    message = "Erro ao registar entrada"
                                }
                            } else {
                                val body = mapOf(
                                    "date" to apiDate,
                                    "checkOut" to apiTime
                                )

                                val response = attendanceService.checkout(mockUserId, body)

                                if (response.isSuccessful) {
                                    isWorking = false
                                    message = "Saída registada com sucesso"
                                } else if (response.code() == 404) {
                                    message = "Não foi encontrado registo aberto para hoje"
                                } else if (response.code() == 400) {
                                    message = "Dados inválidos"
                                } else if (response.code() == 401) {
                                    message = "Utilizador não autorizado"
                                } else {
                                    message = "Erro ao registar saída"
                                }
                            }
                        } catch (_: Exception) {
                            message = "Erro de ligação ao servidor"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(
                    when {
                        isLoading -> "A processar..."
                        isWorking -> "Sair"
                        else -> "Entrar"
                    }
                )
            }
        }
    }
}