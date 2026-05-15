package pt.ualg.miaugenda.ui.screen.myschedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.ualg.miaugenda.data.model.Shift
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PurpleMain = Color(0xFF6F4BB2)
private val PurpleSoft = Color(0xFFF3ECFA)
private val BlueSoft = Color(0xFFEAF6FF)
private val HeaderGrey = Color(0xFFE7E7E7)
private val PurpleLight = Color(0xFFE8DDF8)
private val MineBorder = Color(0xFF7B3FF2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScheduleScreen(
    onBack: () -> Unit = {},
    viewModel: MyScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val weekEnd = uiState.weekStart.plusDays(6)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Horário") },
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
                .background(Brush.verticalGradient(listOf(BlueSoft, PurpleSoft)))
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            WeekSelectorCard(
                weekStart = uiState.weekStart,
                weekEnd = weekEnd,
                onPreviousWeek = { viewModel.previousWeek() },
                onNextWeek = { viewModel.nextWeek() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> LoadingCard()
                uiState.error != null -> ErrorCard(message = uiState.error!!)
                else -> WeekShiftsList(weekStart = uiState.weekStart, shifts = uiState.shifts)
            }
        }
    }
}

@Composable
private fun WeekSelectorCard(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("pt", "PT"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousWeek) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Semana anterior")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${weekStart.format(formatter)} – ${weekEnd.format(formatter)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onNextWeek) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Semana seguinte")
            }
        }
    }
}

@Composable
private fun WeekShiftsList(weekStart: LocalDate, shifts: List<Shift>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in 0..6) {
            val day = weekStart.plusDays(i.toLong())
            val dayShifts = shifts.filter { it.date.substring(0, 10) == day.toString() }
            DayCard(day = day, shifts = dayShifts)
        }
    }
}

@Composable
private fun DayCard(day: LocalDate, shifts: List<Shift>) {
    val formatter = DateTimeFormatter.ofPattern("EEEE d MMM", Locale("pt", "PT"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderGrey, RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.format(formatter)
                        .replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (shifts.isEmpty()) {
                Text(text = "Sem turno", style = MaterialTheme.typography.bodyMedium)
            } else {
                shifts.forEach { shift ->
                    ShiftRow(shift = shift)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ShiftRow(shift: Shift) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PurpleLight),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(MineBorder, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(Icons.Default.DateRange, contentDescription = null, tint = PurpleMain)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "${shift.shiftType.startTime} – ${shift.shiftType.endTime}",
                    fontWeight = FontWeight.Bold
                )
                Text(text = shift.shiftType.name, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
