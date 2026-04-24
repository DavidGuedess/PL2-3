package com.miaugenda.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miaugenda.MainActivity
import com.miaugenda.data.api.AttendanceService
import com.miaugenda.data.api.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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

    // Token mock temporário enquanto ainda não existe login no frontend
    val mockToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsImVtYWlsIjoiYWRtaW5AbWlhdy5jb20iLCJyb2xlIjoiQURNSU4iLCJpYXQiOjE3NzYzNjMyNjMsImV4cCI6MTc3NjM2NDE2M30.mhEnAoBP3xtQyHLA3mD29ZUs2lLC3qRnKdP69zt76C4"

    val currentDate = LocalDate.now()
    val currentTime = LocalTime.now()

    val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val displayDate = currentDate.format(displayDateFormatter)
    val displayTime = currentTime.format(timeFormatter)

    fun loadMyAttendanceHistory() {
        scope.launch {
            isLoading = true
            message = null

            try {
                val response = attendanceService.getMyHistory(mockToken)

                if (response.isSuccessful) {
                    val records = response.body().orEmpty()
                    val lastRecord = records.firstOrNull()

                    isWorking = lastRecord?.type == "IN"
                } else if (response.code() == 401) {
                    message = "Utilizador não autorizado"
                } else {
                    message = "Erro ao carregar histórico"
                }
            } catch (_: Exception) {
                message = "Erro de ligação ao servidor"
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
                    IconButton(
                        onClick = {
                            val intent = Intent(context, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            context.startActivity(intent)
                        }
                    ) {
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
            Text(
                text = displayDate,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = displayTime,
                fontSize = 32.sp
            )

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

                            val response = attendanceService.register(
                                mockToken,
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