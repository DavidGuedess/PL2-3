package pt.ualg.miaugenda.ui.screen.attendancemonitor

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.ualg.miaugenda.data.model.User
import pt.ualg.miaugenda.ui.screen.attendancehistory.DailyAttendance
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceMonitorScreen(
    onBack: () -> Unit,
    viewModel: AttendanceMonitorViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedDay by remember { mutableStateOf<DailyAttendance?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, day: Int ->
                onDateSelected(String.format("%04d-%02d-%02d", year, month + 1, day))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitorização de Presenças") },
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
            if (uiState.isLoadingUsers) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedUser?.let { "${it.name} (${it.employeeNumber})" }
                            ?: "Selecionar funcionário",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Funcionário") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        uiState.users.forEach { user ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(user.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${user.employeeNumber} · ${user.role}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectUser(user)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { showDatePicker { viewModel.setFromDate(it) } },
                        enabled = uiState.selectedUser != null
                    ) {
                        Text(uiState.fromDate ?: "Data início")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { showDatePicker { viewModel.setToDate(it) } },
                        enabled = uiState.selectedUser != null
                    ) {
                        Text(uiState.toDate ?: "Data fim")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    uiState.selectedUser == null -> {
                        Text("Selecione um funcionário para ver as presenças.")
                    }

                    uiState.isLoadingAttendance -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }

                    uiState.error != null -> {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    uiState.dailyAttendances.isEmpty() -> {
                        Text("Sem registos de presença para o período selecionado.")
                    }

                    else -> {
                        Text(
                            text = "Histórico de ${uiState.selectedUser!!.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(uiState.dailyAttendances) { day ->
                                MonitorDailyCard(day = day, onClick = { selectedDay = day })
                            }
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
                TextButton(onClick = { selectedDay = null }) { Text("Fechar") }
            },
            title = { Text("Detalhes do dia") },
            text = {
                Column {
                    MonitorInfoLine("Data", formatDate(day.date))
                    MonitorInfoLine("Total", formatMinutes(day.totalMinutes))
                    if (day.hasOpenRecord) {
                        MonitorInfoLine("Estado", "Entrada sem saída registada")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Registos:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    day.records.forEach { record ->
                        Text(
                            text = "${if (record.type == "IN") "Entrada" else "Saída"} - ${formatTime(record.timestamp)}",
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
private fun MonitorDailyCard(day: DailyAttendance, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formatDate(day.date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Total: ${formatMinutes(day.totalMinutes)}", style = MaterialTheme.typography.bodyMedium)
            Text("${day.records.size} registo(s)", style = MaterialTheme.typography.bodyMedium)
            if (day.hasOpenRecord) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Entrada em aberto",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Clique para ver detalhes", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MonitorInfoLine(label: String, value: String) {
    Spacer(modifier = Modifier.height(6.dp))
    Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
}

private fun formatDate(date: String): String = try {
    java.time.LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
} catch (e: Exception) { date }

private fun formatTime(timestamp: String): String = try {
    Instant.parse(timestamp).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
} catch (e: Exception) { timestamp }

private fun formatMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}min" else "${m}min"
}
