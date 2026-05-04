package pt.ualg.miaugenda.ui.screen.attendancehistory

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.ualg.miaugenda.data.model.AttendanceRecord
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    onBack: () -> Unit,
    viewModel: AttendanceHistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedDay by remember { mutableStateOf<DailyAttendance?>(null) }

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, day: Int ->
                val formatted = String.format("%04d-%02d-%02d", year, month + 1, day)
                onDateSelected(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Presenças") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Histórico diário",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showDatePicker { selectedDate ->
                            viewModel.setFromDate(selectedDate)
                        }
                    }
                ) {
                    Text(uiState.fromDate ?: "Data início")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showDatePicker { selectedDate ->
                            viewModel.setToDate(selectedDate)
                        }
                    }
                ) {
                    Text(uiState.toDate ?: "Data fim")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }

                uiState.error != null -> {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.dailyAttendances.isEmpty() -> {
                    Text("Ainda não existem registos de presença para este intervalo.")
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.dailyAttendances) { day ->
                            DailyAttendanceCard(
                                day = day,
                                onClick = { selectedDay = day }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedDay?.let { day ->
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            confirmButton = {
                TextButton(onClick = { selectedDay = null }) {
                    Text("Fechar")
                }
            },
            icon = {
                Icon(Icons.Default.Close, contentDescription = null)
            },
            title = {
                Text("Detalhes do dia")
            },
            text = {
                Column {
                    InfoLine("Data", formatDate(day.date))
                    InfoLine("Total", formatMinutes(day.totalMinutes))

                    if (day.hasOpenRecord) {
                        InfoLine("Estado", "Existe entrada sem saída")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Registos:",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    day.records.forEach { record ->
                        Text(
                            text = "${formatType(record.type)} - ${formatOnlyTime(record.timestamp)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        )
    }
}

@Composable
private fun DailyAttendanceCard(
    day: DailyAttendance,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = formatDate(day.date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Total registado: ${formatMinutes(day.totalMinutes)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${day.records.size} registo(s) neste dia",
                style = MaterialTheme.typography.bodyMedium
            )

            if (day.hasOpenRecord) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Entrada em aberto",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Clique para ver detalhes",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium
    )
}

private fun formatDate(date: String): String {
    return try {
        val parsed = java.time.LocalDate.parse(date)
        parsed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (e: Exception) {
        date
    }
}

private fun formatOnlyTime(timestamp: String): String {
    return try {
        val cleanTimestamp = timestamp.replace("Z", "")
        val dateTime = LocalDateTime.parse(cleanTimestamp.substring(0, 19))
        dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        timestamp
    }
}

private fun formatType(type: String): String {
    return if (type == "IN") {
        "Entrada"
    } else {
        "Saída"
    }
}

private fun formatMinutes(minutes: Long): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    return if (hours > 0) {
        "${hours}h ${remainingMinutes}min"
    } else {
        "${remainingMinutes}min"
    }
}