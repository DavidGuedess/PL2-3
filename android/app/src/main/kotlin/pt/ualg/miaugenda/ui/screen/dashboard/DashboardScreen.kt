package pt.ualg.miaugenda.ui.screen.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.ualg.miaugenda.MiauGendaApp
import pt.ualg.miaugenda.data.model.Shift
import androidx.compose.ui.platform.LocalContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val PurpleMain = Color(0xFF6F4BB2)
private val PurpleLight = Color(0xFFE8DDF8)
private val PurpleSoft = Color(0xFFF3ECFA)
private val BlueSoft = Color(0xFFEAF6FF)
private val CardGrey = Color(0xFFF6F6F6)
private val HeaderGrey = Color(0xFFE7E7E7)
private val MineBorder = Color(0xFF7B3FF2)
private val OtherBorder = Color(0xFF9FB3FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {},
    onCheckInClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAttendanceHistoryClick: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = MiauGendaApp.getTokenManager(context)
    val uiState by viewModel.uiState.collectAsState()

    val userName = tokenManager.getUserName() ?: "Utilizador"
    val role = tokenManager.getUserRole() ?: "Funcionário"

    var weekStart by remember {
        mutableStateOf(
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        )
    }

    val weekEnd = weekStart.plusDays(6)

    LaunchedEffect(weekStart) {
        viewModel.loadWeekShifts(weekStart)
    }

    Scaffold(
        topBar = {
            DashboardTopBar(
                userName = userName,
                role = role,
                onProfileClick = onProfileClick,
                onLogout = {
                    tokenManager.clearTokens()
                    onLogout()
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
                weekStart = weekStart,
                weekEnd = weekEnd,
                onPreviousWeek = { weekStart = weekStart.minusWeeks(1) },
                onNextWeek = { weekStart = weekStart.plusWeeks(1) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            DashboardMenu(
                onCheckInClick = onCheckInClick,
                onAttendanceHistoryClick = onAttendanceHistoryClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Horário",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            WeekSchedulePreview(
                weekStart = weekStart,
                shifts = uiState.shifts,
                isLoading = uiState.isLoading,
                currentUserName = userName
            )
        }
    }
}

@Composable
private fun DashboardTopBar(
    userName: String,
    role: String,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(72.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF6D7DF2), PurpleMain)
                )
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MIAUGENDA",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .clickable { onProfileClick() }
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = userName,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = role,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }

        IconButton(onClick = onProfileClick) {
            Icon(Icons.Default.Person, contentDescription = "Perfil", tint = Color.White)
        }

        IconButton(onClick = onLogout) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
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
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("pt", "PT"))

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
                text = "Semana ${weekStart.format(formatter)} - ${weekEnd.format(formatter)}",
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
private fun DashboardMenu(
    onCheckInClick: () -> Unit,
    onAttendanceHistoryClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuButton("Horário", selected = true, onClick = {}, modifier = Modifier.weight(1f))
            MenuButton("Presença", selected = false, onClick = onCheckInClick, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuButton("Histórico", selected = false, onClick = onAttendanceHistoryClick, modifier = Modifier.weight(1f))
            MenuButton("Trocas", selected = false, onClick = {}, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuButton("Férias", selected = false, onClick = {}, modifier = Modifier.weight(1f))
            MenuButton("Ausências", selected = false, onClick = {}, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PurpleMain,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(50.dp),
            border = BorderStroke(1.dp, PurpleMain),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleMain),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text)
        }
    }
}

@Composable
private fun WeekSchedulePreview(
    weekStart: LocalDate,
    shifts: List<Shift>,
    isLoading: Boolean,
    currentUserName: String
) {
    if (isLoading) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in 0..6) {
            val day = weekStart.plusDays(i.toLong())
            val dayShifts = shifts.filter {
                it.date.substring(0, 10) == day.toString()
            }

            DayScheduleCard(
                day = day,
                shifts = dayShifts,
                currentUserName = currentUserName
            )
        }
    }
}

@Composable
private fun DayScheduleCard(
    day: LocalDate,
    shifts: List<Shift>,
    currentUserName: String
) {
    val formatter = DateTimeFormatter.ofPattern("EEE d", Locale("pt", "PT"))
    var selectedShift by remember { mutableStateOf<Shift?>(null) }

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
                        .replace(".", "")
                        .uppercase(),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (shifts.isEmpty()) {
                Text(
                    text = "Sem turno",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                shifts.forEach { shift ->
                    ShiftItemCard(
                        shift = shift,
                        isMine = shift.user?.name == currentUserName,
                        onClick = { selectedShift = shift }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    selectedShift?.let { shift ->
        ShiftDetailsDialog(
            shift = shift,
            day = day,
            onDismiss = { selectedShift = null }
        )
    }
}

@Composable
private fun ShiftItemCard(
    shift: Shift,
    isMine: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isMine) PurpleLight else CardGrey
    val borderColor = if (isMine) MineBorder else OtherBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
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
                    .height(54.dp)
                    .background(borderColor, RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = PurpleMain
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "${shift.shiftType.startTime} - ${shift.shiftType.endTime}",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = shift.user?.name ?: "Funcionário",
                    fontWeight = if (isMine) FontWeight.Bold else FontWeight.Normal
                )

                Text(
                    text = shift.shiftType.name,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ShiftDetailsDialog(
    shift: Shift,
    day: LocalDate,
    onDismiss: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("pt", "PT"))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DETALHES DO TURNO",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "X",
                        modifier = Modifier
                            .background(Color(0xFFE0E0E0), RoundedCornerShape(50))
                            .clickable { onDismiss() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDDE6FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Data: ${day.format(formatter).replaceFirstChar { it.uppercase() }}")
                        Text("Horário: ${shift.shiftType.startTime} - ${shift.shiftType.endTime}")
                        Text("Turno: ${shift.shiftType.name}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "FUNCIONÁRIO NESTE TURNO:",
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${shift.user?.name ?: "Funcionário"} - ${shift.user?.role ?: ""}",
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}