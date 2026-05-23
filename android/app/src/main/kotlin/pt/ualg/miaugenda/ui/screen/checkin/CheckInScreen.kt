package pt.ualg.miaugenda.ui.screen.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.data.model.AttendanceRecord
import pt.ualg.miaugenda.data.model.Shift
import pt.ualg.miaugenda.data.model.resolvedEndTime
import pt.ualg.miaugenda.data.model.resolvedStartTime
import pt.ualg.miaugenda.data.model.CreateAttendanceBody
import pt.ualg.miaugenda.data.remote.RetrofitClient
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val PurpleMain = Color(0xFF6F4BB2)
private val PurpleSoft = Color(0xFFF3ECFA)
private val BlueSoft = Color(0xFFEAF6FF)
private val InfoBlue = Color(0xFFDDE8FF)
private val SuccessGreen = Color(0xFF08A979)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    onBack: () -> Unit
) {
    var isWorking by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var records by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var todayShift by remember { mutableStateOf<Shift?>(null) }

    val scope = rememberCoroutineScope()
    val attendanceApi = RetrofitClient.attendanceApi

    fun loadTodayShift() {
        scope.launch {
            try {
                val today = LocalDate.now()
                val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
                val response = RetrofitClient.shiftApi.getMyShifts(week = weekStart)
                if (response.isSuccessful) {
                    todayShift = response.body().orEmpty().firstOrNull { s ->
                        s.date.substring(0, 10) == today.toString() && s.published
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun loadMyAttendanceHistory() {
        scope.launch {
            isLoading = true
            message = null

            try {
                val response = attendanceApi.getMyHistory()

                if (response.isSuccessful) {
                    records = response.body().orEmpty()
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
        loadTodayShift()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MIAUGENDA",
                        fontWeight = FontWeight.Bold
                    )
                },
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
                .background(
                    Brush.verticalGradient(
                        listOf(BlueSoft, PurpleSoft)
                    )
                )
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "REGISTO DE PRESENÇA",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            CurrentShiftCard(
                isWorking = isWorking,
                isLoading = isLoading,
                todayShift = todayShift,
                onRegisterIn = {
                    scope.launch {
                        isLoading = true
                        message = null

                        try {
                            val response = attendanceApi.register(
                                CreateAttendanceBody(type = "IN")
                            )

                            if (response.isSuccessful) {
                                message = "Entrada registada com sucesso"
                                loadMyAttendanceHistory()
                            } else if (response.code() == 400) {
                                message = "Sequência inválida de registo"
                            } else if (response.code() == 401) {
                                message = "Utilizador não autorizado"
                            } else {
                                message = "Erro ao registar entrada"
                            }
                        } catch (e: Exception) {
                            message = "Erro: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                onRegisterOut = {
                    scope.launch {
                        isLoading = true
                        message = null

                        try {
                            val response = attendanceApi.register(
                                CreateAttendanceBody(type = "OUT")
                            )

                            if (response.isSuccessful) {
                                message = "Saída registada com sucesso"
                                loadMyAttendanceHistory()
                            } else if (response.code() == 400) {
                                message = "Sequência inválida de registo"
                            } else if (response.code() == 401) {
                                message = "Utilizador não autorizado"
                            } else {
                                message = "Erro ao registar saída"
                            }
                        } catch (e: Exception) {
                            message = "Erro: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            )

            message?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "HISTÓRICO DE REGISTOS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (records.isEmpty()) {
                EmptyHistoryCard()
            } else {
                records.take(5).forEach { record ->
                    HistoryRow(record = record)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CurrentShiftCard(
    isWorking: Boolean,
    isLoading: Boolean,
    todayShift: Shift?,
    onRegisterIn: () -> Unit,
    onRegisterOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Turno Atual:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = InfoBlue
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    if (todayShift != null) {
                        val dateFormatted = runCatching {
                            val d = LocalDate.parse(todayShift.date.substring(0, 10))
                            d.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", Locale("pt", "PT")))
                                .replaceFirstChar { it.uppercase() }
                        }.getOrDefault(todayShift.date.substring(0, 10))
                        InfoText("Data", dateFormatted)
                        InfoText("Horário", "${todayShift.resolvedStartTime()} - ${todayShift.resolvedEndTime()}")
                        todayShift.shiftType?.name?.let { InfoText("Posição", it) }
                    } else {
                        InfoText("Estado", "Sem turno agendado para hoje")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRegisterIn,
                    modifier = Modifier.weight(1f),
                    enabled = !isWorking && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen,
                        contentColor = Color.White,
                        disabledContainerColor = Color.LightGray,
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isLoading) "A processar..." else "Registar Entrada",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onRegisterOut,
                    modifier = Modifier.weight(1f),
                    enabled = isWorking && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen,
                        contentColor = Color.White,
                        disabledContainerColor = Color.LightGray,
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isLoading) "A processar..." else "Registar Saída",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoText(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun HistoryRow(record: AttendanceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formatRecordDate(record.timestamp),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Registo de presença",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "${formatType(record.type)}: ${formatOnlyTime(record.timestamp)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.85f)
        )
    ) {
        Text(
            text = "Ainda não existem registos de presença.",
            modifier = Modifier.padding(14.dp)
        )
    }
}

private fun formatType(type: String): String {
    return if (type == "IN") {
        "Entrada"
    } else {
        "Saída"
    }
}

private fun formatOnlyTime(timestamp: String): String {
    return try {
        val dateTime = Instant.parse(timestamp)
            .atZone(ZoneId.systemDefault())

        dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        timestamp
    }
}

private fun formatRecordDate(timestamp: String): String {
    return try {
        val dateTime = Instant.parse(timestamp)
            .atZone(ZoneId.systemDefault())

        dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (e: Exception) {
        timestamp
    }
}