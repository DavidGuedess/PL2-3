package pt.ualg.miaugenda.ui.screen.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.derivedStateOf
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import pt.ualg.miaugenda.ui.components.AppBottomNav
import pt.ualg.miaugenda.ui.components.NavTab
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.MiauGendaApp
import pt.ualg.miaugenda.data.model.ActiveEmployee
import pt.ualg.miaugenda.data.model.AttendanceRecord
import pt.ualg.miaugenda.data.model.Availability
import pt.ualg.miaugenda.data.model.CreateAvailabilityBody
import pt.ualg.miaugenda.data.model.Shift
import pt.ualg.miaugenda.data.model.ShiftSwapRequest
import pt.ualg.miaugenda.data.model.TimeOffRequest
import pt.ualg.miaugenda.data.model.CreateTimeOffRequestBody
import pt.ualg.miaugenda.data.model.CreateShiftSwapRequestBody
import pt.ualg.miaugenda.data.model.resolvedStartTime
import pt.ualg.miaugenda.data.model.resolvedEndTime
import pt.ualg.miaugenda.data.model.resolvedName
import pt.ualg.miaugenda.data.remote.RetrofitClient
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// ── Cores ─────────────────────────────────────────────────────────────────────
private val DkBg       = Color(0xFF000000)
private val DkSurface  = Color(0xFF1C1C1E)
private val DkSurface2 = Color(0xFF2C2C2E)
private val Blue       = Color(0xFF2979FF)
private val Orange     = Color(0xFFFF8F00)
private val DkGreen    = Color(0xFF2ECC71)
private val DkPurple   = Color(0xFF9B59B6)
private val TxtGray    = Color(0xFF8E8E93)
private val RedBadge   = Color(0xFFFF3B30)
private val RedReject  = Color(0xFFE53935)
private val PurpleDk   = Color(0xFF7B1FA2)
private val AvatarGrad = Brush.linearGradient(
    listOf(Color(0xFF5B5FEF), Color(0xFFC850C0), Color(0xFFF0696B))
)

// ── Disponibilidade ───────────────────────────────────────────────────────────
data class AvailabilityPref(
    val tipo: String,        // "PREFERIDA" | "INDISPONIVEL"
    val nota: String = ""
)

// ── Sub-ecrãs internos ────────────────────────────────────────────────────────
private enum class SubScreen {
    Home, TurnosAgendados, EmTurno,
    PedidoFerias, PedidoTroca, ShiftSummary
}

private enum class ClockState { NO_SHIFT, HAS_SHIFT, CLOCKED_IN }

// ── Root ──────────────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {},
    onCheckInClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAttendanceHistoryClick: () -> Unit = {},
    onAttendanceMonitorClick: () -> Unit = {},
    onSchedulerClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},
    onEquipaClick: () -> Unit = {},
    onAvailabilityClick: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val context      = LocalContext.current
    val tokenManager = MiauGendaApp.getTokenManager(context)
    val uiState      by viewModel.uiState.collectAsState()
    val fullName     = tokenManager.getUserName() ?: "Xavier"
    val firstName    = fullName.split(" ").first()
    val currentUserId = tokenManager.getUserId()

    val today      = LocalDate.now()
    val weekStart  = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    LaunchedEffect(weekStart) {
        while (true) {
            viewModel.loadWeekShifts(weekStart)
            viewModel.loadTodayAttendance()
            viewModel.loadMyRequests()

            delay(3_000)
        }
    }

    var subScreen by remember { mutableStateOf(SubScreen.Home) }

    BackHandler(enabled = subScreen != SubScreen.Home) { subScreen = SubScreen.Home }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DkBg)
            .systemBarsPadding()
    ) {
        when (subScreen) {
            SubScreen.Home -> HomeScreen(
                firstName             = firstName,
                shifts                = uiState.shifts,
                attendanceRecords     = uiState.attendanceRecords,
                activeEmployees       = uiState.activeEmployees,
                allWeekShiftsCount    = uiState.allWeekShiftsCount,
                today                 = today,
                isClocked             = uiState.isClocked,
                clockedInSince        = uiState.clockedInSince,
                timeOffRequests       = uiState.timeOffRequests.filter { it.userId == currentUserId },
                shiftSwapRequests     = uiState.shiftSwapRequests.filter { it.requesterId == currentUserId },
                onNavigate            = { subScreen = it },
                onLogout              = {
                    pt.ualg.miaugenda.data.notif.stopPollingForRequests(context.applicationContext)
                    tokenManager.clearTokens()
                    onLogout()
                },
                onProfileClick        = onProfileClick,
                onClockIn             = { viewModel.clockIn {} },
                onClockOut            = { subScreen = SubScreen.ShiftSummary },
                onSchedulerClick      = onSchedulerClick,
                onNotificationsClick  = onNotificationsClick,
                onInboxClick          = onInboxClick,
                onEquipaClick         = onEquipaClick,
                onAvailabilityClick   = onAvailabilityClick
            )
            SubScreen.TurnosAgendados -> ScreenTurnosAgendados(
                shifts   = uiState.shifts.filter { it.published },
                userName = fullName,
                onBack   = { subScreen = SubScreen.Home }
            )
            SubScreen.EmTurno -> ScreenEmTurno(
                activeEmployees = uiState.activeEmployees,
                onBack          = { subScreen = SubScreen.Home }
            )
            SubScreen.PedidoFerias -> PedidoFeriasScreen(
                userName = fullName,
                userId   = tokenManager.getUserId(),
                onBack   = { subScreen = SubScreen.Home; viewModel.loadMyRequests() }
            )
            SubScreen.PedidoTroca -> PedidoTrocaScreen(
                myShifts = uiState.shifts.filter { it.published },
                userId   = tokenManager.getUserId(),
                onBack   = { subScreen = SubScreen.Home; viewModel.loadMyRequests() }
            )
            SubScreen.ShiftSummary -> ShiftSummaryScreen(
                clockedInSince = uiState.clockedInSince,
                todayShift = uiState.shifts.firstOrNull {
                    it.date.startsWith(today.toString()) && it.published
                },
                onCancel = { subScreen = SubScreen.Home },
                onFinish = { note ->
                    viewModel.clockOut(note) { success ->
                        if (success) {
                            subScreen = SubScreen.Home
                        }
                    }
                }
            )
        }
    }
}

// ── HomeScreen ────────────────────────────────────────────────────────────────
@Composable
private fun HomeScreen(
    firstName: String,
    shifts: List<Shift>,
    attendanceRecords: List<AttendanceRecord> = emptyList(),
    activeEmployees: List<ActiveEmployee> = emptyList(),
    allWeekShiftsCount: Int = 0,
    today: LocalDate,
    isClocked: Boolean = false,
    clockedInSince: String? = null,
    timeOffRequests: List<TimeOffRequest> = emptyList(),
    shiftSwapRequests: List<ShiftSwapRequest> = emptyList(),
    onNavigate: (SubScreen) -> Unit,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit = {},
    onClockIn: () -> Unit = {},
    onClockOut: () -> Unit = {},
    onSchedulerClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},
    onEquipaClick: () -> Unit = {},
    onAvailabilityClick: () -> Unit = {}
) {
    var fabOpen by remember { mutableStateOf(false) }

    // Minute-ticker para recalcular janela de registo de ponto
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); currentTime = LocalTime.now() }
    }

    // Só turnos PUBLICADOS contam para o registo de ponto
    val todayShift = remember(shifts, today, currentTime) {
        shifts.firstOrNull { s ->
            if (!s.published) return@firstOrNull false

            val shiftDate = runCatching {
                LocalDate.parse(s.date.substring(0, 10))
            }.getOrNull() ?: return@firstOrNull false

            val start = runCatching {
                LocalTime.parse(s.resolvedStartTime())
            }.getOrNull() ?: return@firstOrNull false

            val end = runCatching {
                LocalTime.parse(s.resolvedEndTime())
            }.getOrNull() ?: return@firstOrNull false

            val isOvernight = end.isBefore(start) || end == start

            shiftDate == today || (isOvernight && shiftDate == today.minusDays(1))
        }
    }

    // Estado do ponto derivado do ViewModel (backend)
    val clockState = when {
        isClocked    -> ClockState.CLOCKED_IN
        todayShift != null -> ClockState.HAS_SHIFT
        else         -> ClockState.NO_SHIFT
    }

    // Hora de entrada formatada a partir do timestamp ISO
    val clockedInAt = remember(clockedInSince) {
        clockedInSince?.let {
            runCatching {
                val instant = java.time.Instant.parse(it)
                java.time.LocalDateTime
                    .ofInstant(instant, java.time.ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
            }.getOrDefault("")
        } ?: ""
    }

    // Timer: inicializa com o elapsed real e conta a partir daí
    var clockedInSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(clockedInSince) {
        clockedInSec = clockedInSince?.let {
            runCatching {
                val since = java.time.Instant.parse(it)
                java.time.Duration.between(since, java.time.Instant.now()).seconds
                    .toInt().coerceAtLeast(0)
            }.getOrDefault(0)
        } ?: 0
    }
    LaunchedEffect(isClocked) {
        if (isClocked) { while (true) { delay(1000); clockedInSec++ } }
    }
    val timerStr = String.format(
        "%02d:%02d:%02d", clockedInSec / 3600, (clockedInSec % 3600) / 60, clockedInSec % 60
    )



    // Dynamic greeting
    val greetingWord = when (currentTime.hour) {
        in 5..11  -> "Bom dia"
        in 12..18 -> "Boa tarde"
        else      -> "Boa noite"
    }

    // A secção de registo de ponto só aparece:
    //   - Se estiver clocked-in (enquanto não chegar auto-saída do backend)
    //   - Se existir turno hoje E estiver dentro da janela [start-15min, end+15min]
    val showClockSection = when {
        isClocked -> true

        todayShift != null -> {
            val shiftStart = runCatching {
                LocalTime.parse(todayShift.resolvedStartTime())
            }.getOrNull()

            val shiftEnd = runCatching {
                LocalTime.parse(todayShift.resolvedEndTime())
            }.getOrNull()

            if (shiftStart == null || shiftEnd == null) {
                false
            } else {
                val startsBeforeEnds = shiftEnd.isAfter(shiftStart)

                if (startsBeforeEnds) {
                    // Turno normal: 09:00 - 17:00
                    val tooEarly = currentTime.isBefore(shiftStart.minusMinutes(15))
                    val tooLate = currentTime.isAfter(shiftEnd.plusMinutes(15))
                    !tooEarly && !tooLate
                } else {
                    // Turno overnight: 22:50 - 05:00
                    val afterStart = !currentTime.isBefore(shiftStart.minusMinutes(15))
                    val beforeEnd = !currentTime.isAfter(shiftEnd.plusMinutes(15))

                    afterStart || beforeEnd
                }
            }
        }

        else -> false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item { Spacer(Modifier.height(10.dp)) }
                item { DashHeader(firstName, onProfileClick) }

                item {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("$greetingWord, ") }
                            append("$firstName!")
                        },
                        color = Color.White,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
                    )
                }

                if (showClockSection) {
                    item {
                        ClockInSection(
                            clockState  = clockState,
                            todayShift  = todayShift,
                            timerStr    = timerStr,
                            clockedInAt = clockedInAt,
                            onClockIn   = onClockIn,
                            onClockOut  = onClockOut
                        )
                    }
                }

                item { SectionTitle("Resumo de Hoje", topPad = 8.dp) }
                item {
                    TodaySnapshot(
                        onNavigate           = onNavigate,
                        activeEmployees      = activeEmployees,
                        todayShift           = todayShift,
                        attendanceRecords    = attendanceRecords,
                        today                = today,
                        shifts               = shifts,
                        allWeekShiftsCount   = allWeekShiftsCount,
                        pendingRequestsCount = timeOffRequests.count { it.status == "PENDING" } + shiftSwapRequests.count { it.status == "PENDING" }
                    )
                }

                item { SectionTitle("Os Meus Turnos", topPad = 24.dp) }
                item { MeusTurnosSection(shifts, attendanceRecords, today) }

                item { SectionTitle("Pedidos Enviados", topPad = 32.dp) }
                item { PedidosEnviadosSection(timeOffRequests, shiftSwapRequests) }
                item { Spacer(Modifier.height(40.dp)) }
            }
            AppBottomNav(
                active               = NavTab.HOME,
                onSchedulerClick     = onSchedulerClick,
                onNotificationsClick = onNotificationsClick,
                onInboxClick         = onInboxClick,
                onMenuClick          = onEquipaClick
            )
        }

        // FAB overlay
        if (fabOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable { fabOpen = false }
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 152.dp)
                        .clickable(enabled = false) {},
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    FabMenuItem("Disponibilidade", Icons.Outlined.EventAvailable) {
                        fabOpen = false
                        onAvailabilityClick()
                    }
                    FabMenuItem("Pedido de Troca", Icons.Outlined.SwapHoriz) {
                        fabOpen = false
                        onNavigate(SubScreen.PedidoTroca)
                    }
                    FabMenuItem("Pedido de Férias", Icons.Outlined.WbSunny) {
                        fabOpen = false
                        onNavigate(SubScreen.PedidoFerias)
                    }
                }
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 80.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(Blue)
                .clickable { fabOpen = !fabOpen },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (fabOpen) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ── DashHeader ────────────────────────────────────────────────────────────────
@Composable
private fun DashHeader(firstName: String, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Avatar gradient (left)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(AvatarGrad)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = firstName.firstOrNull()?.uppercase() ?: "X",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// ── ClockInSection ────────────────────────────────────────────────────────────
@Composable
private fun ClockInSection(
    clockState: ClockState,
    todayShift: Shift?,
    timerStr: String,
    clockedInAt: String,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit
) {
    // Late calculation
    val now = LocalTime.now()
    val shiftStart = todayShift?.let {
        runCatching { LocalTime.parse(it.resolvedStartTime()) }.getOrNull()
    }
    val isLate = shiftStart != null && now.isAfter(shiftStart) && clockState == ClockState.HAS_SHIFT
    val lateMin = if (isLate) java.time.Duration.between(shiftStart, now).toMinutes() else 0L
    val lateHrs = lateMin / 60
    val lateRemMin = lateMin % 60

    when (clockState) {
        ClockState.NO_SHIFT -> {
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 22.dp)
            ) {
                OutlinedButton(
                    onClick = onClockIn,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Blue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue)
                ) {
                    Text(
                        "Registar Entrada",
                        color = Blue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sem turno agendado — a registar como não programado.",
                    color = TxtGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        ClockState.HAS_SHIFT -> {
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 22.dp)
            ) {
                if (isLate) {
                    val lateMsg = when {
                        lateHrs > 0 -> "Está $lateHrs hora${if (lateHrs > 1) "s" else ""} e $lateRemMin minutos atrasado!"
                        else        -> "Está $lateMin minutos atrasado!"
                    }
                    Text(lateMsg, color = Color(0xFFFF3B30), fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
                todayShift?.let {
                    Text(
                        "${it.resolvedStartTime()} - ${it.resolvedEndTime()}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                Button(
                    onClick = onClockIn,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Text(
                        "Registar Entrada",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        ClockState.CLOCKED_IN -> {
            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 22.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Blue)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Entrada registada às $clockedInAt",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                        Text(timerStr, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    todayShift?.let {
                        Text(
                            "${it.resolvedStartTime()} - ${it.resolvedEndTime()}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Clock Out button
                    Button(
                        onClick = onClockOut,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E8C))
                    ) {
                        Icon(
                            Icons.Outlined.StopCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Registar Saída",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── ShiftSummaryScreen ────────────────────────────────────────────────────────
@Composable
private fun ShiftSummaryScreen(
    clockedInSince: String?,
    todayShift: Shift?,
    onCancel: () -> Unit,
    onFinish: (note: String) -> Unit
) {
    val exitTime  = remember { LocalTime.now() }
    val todayDate = remember { LocalDate.now() }

    val entryTime = remember(clockedInSince) {
        clockedInSince?.let {
            runCatching {
                val instant = java.time.Instant.parse(it)
                java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()).toLocalTime()
            }.getOrNull()
        }
    }

    val totalStr = remember(entryTime) {
        entryTime?.let {
            val dur = java.time.Duration.between(it, exitTime).abs()
            val h = dur.toHours()
            val m = dur.toMinutes() % 60
            when {
                h > 0 -> "${h}h ${m}min"
                m > 0 -> "${m}min"
                else  -> "${dur.seconds}s"
            }
        } ?: "—"
    }

    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val entryStr = entryTime?.format(fmt) ?: "—"
    val exitStr  = exitTime.format(fmt)

    val dateStr = todayDate.format(
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale("pt", "PT"))
    ).replaceFirstChar { it.uppercaseChar() }

    val shiftLabel = todayShift?.resolvedName() ?: "Padrão"

    var note      by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DkBg)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cancelar",
                color = Blue,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(onClick = onCancel)
                    .width(70.dp)
            )
            Text(
                "Resumo do Turno",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(70.dp))
        }
        HorizontalDivider(color = DkSurface2)

        Spacer(Modifier.height(24.dp))

        // Times
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.DateRange,
                contentDescription = null,
                tint = TxtGray,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entryStr, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("  →  ", color = TxtGray, fontSize = 18.sp)
                    Text(exitStr, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Text(dateStr, color = TxtGray, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = DkSurface2, modifier = Modifier.padding(horizontal = 20.dp))

        // Horário
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Horário", color = Color.White, fontSize = 15.sp)
            Text(shiftLabel, color = TxtGray, fontSize = 15.sp)
        }
        HorizontalDivider(color = DkSurface2, modifier = Modifier.padding(horizontal = 20.dp))

        // Total
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Total", color = Color.White, fontSize = 15.sp)
            }
            Text(totalStr, color = TxtGray, fontSize = 15.sp)
        }
        HorizontalDivider(color = DkSurface2, modifier = Modifier.padding(horizontal = 20.dp))

        // Note field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Blue),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (note.isEmpty()) Text("+ Adicionar nota...", color = TxtGray, fontSize = 15.sp)
                    inner()
                }
            )
        }

        Spacer(Modifier.weight(1f))

        // Terminar button
        Button(
            onClick = { isLoading = true; onFinish(note) },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DkSurface2)
        ) {
            Text(
                if (isLoading) "A processar..." else "Terminar",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

// ── TodaySnapshot ─────────────────────────────────────────────────────────────
@Composable
private fun TodaySnapshot(
    onNavigate: (SubScreen) -> Unit,
    activeEmployees: List<ActiveEmployee>,
    todayShift: Shift? = null,
    attendanceRecords: List<AttendanceRecord> = emptyList(),
    today: LocalDate = LocalDate.now(),
    shifts: List<Shift> = emptyList(),
    allWeekShiftsCount: Int = 0,
    pendingRequestsCount: Int = 0
) {
    data class TodayItem(
        val icon: ImageVector,
        val label: String,
        val value: String,
        val sub: SubScreen?,
        val valueColor: Color = Color.White
    )

    val todayHoursStr = remember(attendanceRecords, today) {
        val recs = attendanceRecords.filter { rec ->
            runCatching {
                java.time.Instant.parse(rec.timestamp)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today
            }.getOrDefault(false)
        }.sortedBy { it.timestamp }
        var totalMin = 0L
        var lastIn: java.time.Instant? = null
        for (rec in recs) {
            if (rec.type == "IN") {
                lastIn = runCatching { java.time.Instant.parse(rec.timestamp) }.getOrNull()
            } else if (rec.type == "OUT" && lastIn != null) {
                val out = runCatching { java.time.Instant.parse(rec.timestamp) }.getOrNull()
                if (out != null) totalMin += java.time.Duration.between(lastIn, out).toMinutes()
                lastIn = null
            }
        }
        val h = totalMin / 60; val m = totalMin % 60
        if (h > 0) "${h}h ${m}min" else if (m > 0) "${m}min" else "—"
    }

    val lateCount = remember(shifts, attendanceRecords) {
        shifts.filter { it.published }.count { shift ->
            val shiftDate = runCatching { LocalDate.parse(shift.date.substring(0, 10)) }.getOrNull()
                ?: return@count false
            val shiftStart = runCatching { LocalTime.parse(shift.resolvedStartTime()) }.getOrNull()
                ?: return@count false
            val dayRecs = attendanceRecords.filter { rec ->
                runCatching {
                    java.time.Instant.parse(rec.timestamp)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate() == shiftDate
                }.getOrDefault(false)
            }
            val inRec = dayRecs.firstOrNull { it.type == "IN" } ?: return@count false
            val actualIn = runCatching {
                java.time.Instant.parse(inRec.timestamp)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
            }.getOrNull() ?: return@count false
            java.time.Duration.between(shiftStart, actualIn).toMinutes() > 5
        }
    }

    val shiftTimeStr = todayShift?.let {
        val s = it.resolvedStartTime().take(5)
        val e = it.resolvedEndTime().take(5)
        "$s – $e"
    } ?: "Sem turno"

    val items = listOf(
        TodayItem(Icons.Outlined.CheckCircle, "Em Turno Agora", activeEmployees.size.toString(), SubScreen.EmTurno),
        TodayItem(Icons.Outlined.Schedule, "Turno de Hoje", shiftTimeStr, SubScreen.TurnosAgendados,
            if (todayShift != null) Color.White else TxtGray),
        TodayItem(Icons.Outlined.DateRange, "Turnos Agendados Esta Semana", allWeekShiftsCount.toString(), SubScreen.TurnosAgendados),
        TodayItem(Icons.Outlined.HourglassEmpty, "Pedidos Pendentes",
            if (pendingRequestsCount > 0) pendingRequestsCount.toString() else "—",
            null,
            if (pendingRequestsCount > 0) Orange else TxtGray),
    )
    ListCard(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 22.dp)) {
        items.forEachIndexed { i, item ->
            ListRow(
                icon       = item.icon,
                iconTint   = TxtGray,
                label      = item.label,
                value      = item.value,
                valueColor = item.valueColor,
                clickable  = item.sub != null,
                onClick    = { item.sub?.let(onNavigate) }
            )
            if (i < items.lastIndex) DkDivider()
        }
    }
}


// ── MeusTurnosSection ─────────────────────────────────────────────────────────
@Composable
private fun MeusTurnosSection(
    shifts: List<Shift>,
    attendanceRecords: List<AttendanceRecord>,
    today: LocalDate
) {
    val allWeekShifts = shifts.filter { it.published }.sortedBy { it.date }
    // Ponto de início: primeiro turno de hoje ou futuro; se não houver, mostra os últimos 3
    val startIdx = allWeekShifts.indexOfFirst {
        !LocalDate.parse(it.date.substring(0, 10)).isBefore(today)
    }.let { if (it < 0) maxOf(0, allWeekShifts.size - 3) else it }
    var showAll by remember { mutableStateOf(false) }
    val weekShifts = if (showAll) allWeekShifts else allWeekShifts.drop(startIdx).take(3)
    val hasMore = allWeekShifts.size > startIdx + 3

    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
        if (allWeekShifts.isEmpty()) {
            Text(
                "Sem turnos publicados esta semana.",
                color = TxtGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            weekShifts.forEach { shift ->
                val shiftDate = LocalDate.parse(shift.date.substring(0, 10))
                val dayNum   = shiftDate.dayOfMonth.toString()
                val dayAbbr  = shiftDate.format(DateTimeFormatter.ofPattern("EEE", Locale("pt", "PT")))
                    .replaceFirstChar { it.uppercase() }.take(3)
                val isToday  = shiftDate == today
                val isPast   = shiftDate.isBefore(today)
                val dayColor = if (isToday) Blue else Color.White

                // Registos de ponto para este dia (converter timestamp ISO → LocalDate)
                val dayRecords = attendanceRecords.filter { rec ->
                    runCatching {
                        java.time.Instant.parse(rec.timestamp)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate() == shiftDate
                    }.getOrDefault(false)
                }.sortedBy { it.timestamp }

                val inRecord  = dayRecords.firstOrNull { it.type == "IN" }
                val outRecord = dayRecords.lastOrNull  { it.type == "OUT" }

                // Hora real de entrada/saída em hora local
                val actualIn = inRecord?.let { rec ->
                    runCatching {
                        java.time.Instant.parse(rec.timestamp)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                    }.getOrNull()
                }
                val actualOut = outRecord?.let { rec ->
                    runCatching {
                        java.time.Instant.parse(rec.timestamp)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                    }.getOrNull()
                }

                // Atraso: entrada > hora de início do turno + 5 min de tolerância
                val shiftStart = runCatching { LocalTime.parse(shift.resolvedStartTime()) }.getOrNull()
                val delayMin = if (actualIn != null && shiftStart != null) {
                    val d = java.time.Duration.between(shiftStart, actualIn).toMinutes()
                    if (d > 5) d else 0L
                } else 0L

                val isFalta = isPast && inRecord == null
                val isLate  = delayMin > 0

                // Duração prevista do turno
                val durationStr = run {
                    val s = runCatching { LocalTime.parse(shift.resolvedStartTime()) }.getOrNull()
                    val e = runCatching { LocalTime.parse(shift.resolvedEndTime()) }.getOrNull()
                    if (s != null && e != null) {
                        val mins = if (e.isAfter(s))
                            java.time.Duration.between(s, e).toMinutes()
                        else
                            java.time.Duration.between(s, e).toMinutes() + 24 * 60
                        val h = mins / 60; val m = mins % 60
                        if (m == 0L) "${h}h" else "${h}h${m}m"
                    } else ""
                }
                // Tempo efetivamente trabalhado
                val workedStr = if (actualIn != null && actualOut != null) {
                    val dur = java.time.Duration.between(actualIn, actualOut).abs()
                    val h = dur.toHours(); val m = dur.toMinutes() % 60
                    if (m == 0L) "${h}h" else "${h}h${m}m"
                } else null

                // Status badge: Ausente / Concluído / Em Turno / Hoje / vazio
                val (badgeLabel, badgeTextColor, badgeBgColor) = when {
                    isFalta                         -> Triple("Ausente",   RedBadge, RedBadge.copy(alpha = 0.15f))
                    isPast && actualOut != null      -> Triple("Concluído", TxtGray,  DkSurface2)
                    isToday && actualIn != null      -> Triple("Em Turno", DkGreen,  DkGreen.copy(alpha = 0.15f))
                    isLate                          -> Triple("+${delayMin}min", Orange, Orange.copy(alpha = 0.15f))
                    isToday                         -> Triple("Hoje",      Blue,     Blue.copy(alpha = 0.15f))
                    else                            -> Triple(null, Color.Transparent, Color.Transparent)
                }

                val shiftPositionLabel = shift.shiftType?.name ?: "Sem Posição"

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 6.dp).width(36.dp)
                    ) {
                        Text(dayNum, color = dayColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
                        Text(dayAbbr, color = dayColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(DkSurface)
                    ) {
                        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(Blue))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
                        ) {
                            // Posição / tipo do turno em azul
                            Text(
                                shiftPositionLabel,
                                color = Blue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            // Hora + badge na mesma linha
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${shift.resolvedStartTime().take(5)} - ${shift.resolvedEndTime().take(5)}",
                                    color = if (isFalta) RedBadge else Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (badgeLabel != null) {
                                    DkStatusBadge(badgeLabel, badgeTextColor, badgeBgColor)
                                }
                            }
                            // Duração (ou tempo trabalhado/esperado)
                            val durationLine = when {
                                workedStr != null && workedStr != durationStr ->
                                    "$workedStr/$durationStr"
                                durationStr.isNotEmpty() -> durationStr
                                else -> ""
                            }
                            if (durationLine.isNotEmpty()) {
                                Text(
                                    durationLine,
                                    color = TxtGray,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (!showAll && hasMore) {
            Text(
                "Mostrar mais...",
                color = Blue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 8.dp)
                    .clickable { showAll = true }
            )
        } else if (showAll && allWeekShifts.size > 3) {
            Text(
                "Mostrar menos",
                color = TxtGray,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 8.dp)
                    .clickable { showAll = false }
            )
        }
    }
}

// ── FabMenuItem ───────────────────────────────────────────────────────────────
@Composable
private fun FabMenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Blue),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

// ── Componentes partilhados ───────────────────────────────────────────────────

@Composable
private fun DkAvatar(initials: String, color: Color, size: Int, gradient: Brush? = null) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .let { if (gradient != null) it.background(gradient) else it.background(color) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.take(2),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.33).sp
        )
    }
}

@Composable
private fun DkStatusBadge(text: String, textColor: Color, bg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DkDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.08f))
    )
}

@Composable
private fun SectionTitle(title: String, topPad: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .padding(top = topPad, bottom = 12.dp)
    )
}

@Composable
private fun ListCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(DkSurface)
    ) {
        Column(content = content)
    }
}

@Composable
private fun ListRow(
    icon: ImageVector,
    iconTint: Color = TxtGray,
    label: String,
    labelColor: Color = Color.White,
    value: String,
    valueColor: Color = Color.White,
    clickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            Text(label, color = labelColor, fontSize = 15.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (clickable) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TxtGray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SubHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onBack() }
                .padding(4.dp)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.size(30.dp))
    }
}

@Composable
private fun StatRow(items: List<Pair<String, String>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items.forEach { (label, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = TxtGray, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DayGroup(day: String, dayName: String, dayColor: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 6.dp).width(36.dp)
        ) {
            Text(day, color = dayColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
            Text(dayName, color = dayColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f), content = content)
    }
}

@Composable
private fun ShiftCardItem(
    dept: String,
    deptColor: Color,
    initials: String,
    avatarColor: Color,
    name: String,
    time: String,
    status: String,
    statusColor: Color,
    statusBg: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DkSurface)
    ) {
        Row {
            // Borda esquerda colorida
            Box(modifier = Modifier.width(3.5.dp).fillMaxHeight().background(deptColor))
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 12.dp)) {
                Text(dept, color = deptColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DkAvatar(initials = initials, color = avatarColor, size = 30)
                        Column {
                            Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(time, color = TxtGray, fontSize = 13.sp)
                        }
                    }
                    DkStatusBadge(status, statusColor, statusBg)
                }
            }
        }
    }
}

// ── Sub-ecrãs ─────────────────────────────────────────────────────────────────

@Composable
private fun ScreenTurnosAgendados(shifts: List<Shift>, userName: String, onBack: () -> Unit) {
    val today = LocalDate.now()
    val dayNames = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    val grouped = shifts
        .sortedBy { it.date }
        .groupBy { it.date.substring(0, 10) }
    val initials = userName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    val avatarColor = Color(0xFF2979FF)
    val totalHours = shifts.sumOf { shift ->
        runCatching {
            val start = java.time.LocalTime.parse(shift.resolvedStartTime())
            val end   = java.time.LocalTime.parse(shift.resolvedEndTime())
            val mins  = if (end.isAfter(start)) java.time.Duration.between(start, end).toMinutes()
                        else java.time.Duration.between(start, end).toMinutes() + 24 * 60
            mins / 60.0
        }.getOrDefault(0.0)
    }

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        SubHeader("Total de Turnos Agendados", onBack)
        StatRow(listOf(
            "Turnos" to "${shifts.size}",
            "Esta semana" to "${shifts.count { it.date.substring(0,10) >= today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString() }}",
            "Horas totais" to "${"%.1f".format(totalHours)}h"
        ))
        if (shifts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sem turnos agendados.", color = TxtGray, fontSize = 15.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                grouped.forEach { (dateStr, dayShifts) ->
                    item {
                        val date = runCatching { LocalDate.parse(dateStr) }.getOrNull()
                        val dayNum  = date?.dayOfMonth?.toString() ?: dateStr.takeLast(2)
                        val dayName = date?.let { dayNames[(it.dayOfWeek.value - 1) % 7] } ?: ""
                        val isToday = date == today
                        DayGroup(dayNum, dayName, if (isToday) Blue else Color.White) {
                            dayShifts.forEach { shift ->
                                val shiftName = shift.shiftType?.name ?: "Turno"
                                val timeStr   = "${shift.resolvedStartTime()} – ${shift.resolvedEndTime()}"
                                val (statusLabel, statusColor) = when {
                                    date != null && date.isBefore(today) -> "Concluído" to TxtGray
                                    isToday -> "Hoje" to DkGreen
                                    else    -> "Agendado" to Blue
                                }
                                ShiftCardItem(
                                    dept        = shiftName,
                                    deptColor   = Blue,
                                    initials    = initials,
                                    avatarColor = avatarColor,
                                    name        = userName,
                                    time        = timeStr,
                                    status      = statusLabel,
                                    statusColor = statusColor,
                                    statusBg    = statusColor.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun OpenShiftCard(surfaceColor: Color = DkSurface) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
    ) {
        Row {
            Box(modifier = Modifier.width(3.5.dp).fillMaxHeight().background(Blue))
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 12.dp)) {
                Text("Sem Posição", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
                            Text("Turno Aberto", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("9:00a - 5:00p • 0/3", color = TxtGray, fontSize = 13.sp)
                    }
                    DkStatusBadge("Vaga", Blue, Blue.copy(alpha = 0.15f))
                }
            }
        }
    }
}

// ── ScreenEmTurno — lista real de funcionários com ponto ativo ────────────────
@Composable
private fun ScreenEmTurno(
    activeEmployees: List<ActiveEmployee>,
    onBack: () -> Unit
) {
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clip(CircleShape).clickable { onBack() }.padding(4.dp)) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                "Em Turno Agora",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            // badge com total
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(DkGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "${activeEmployees.size} ativo${if (activeEmployees.size != 1) "s" else ""}",
                    color = DkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

        if (activeEmployees.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Outlined.EventAvailable, null, tint = TxtGray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Nenhum funcionário em turno", color = TxtGray, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeEmployees) { emp ->
                    val initials = emp.name.split(" ").filter { it.isNotEmpty() }.take(2)
                        .joinToString("") { it.first().uppercase() }

                    val clockedInTime = runCatching {
                        java.time.Instant.parse(emp.clockedInSince)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                            .format(timeFmt)
                    }.getOrDefault("--:--")

                    val elapsed = runCatching {
                        val since = java.time.Instant.parse(emp.clockedInSince)
                        val dur = java.time.Duration.between(since, java.time.Instant.now()).abs()
                        val h = dur.toHours(); val m = dur.toMinutes() % 60
                        if (h > 0) "${h}h ${m}min" else "${m}min"
                    }.getOrDefault("")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DkSurface)
                    ) {
                        Row {
                            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(DkGreen))
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                DkAvatar(initials = initials, color = Blue, size = 46)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(emp.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(emp.employeeNumber, color = TxtGray, fontSize = 13.sp)
                                    if (emp.shiftStart != null && emp.shiftEnd != null) {
                                        Text(
                                            "${emp.shiftStart} - ${emp.shiftEnd}",
                                            color = Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    DkStatusBadge("EM TURNO", DkGreen, DkGreen.copy(alpha = 0.15f))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Entrada: $clockedInTime", color = TxtGray, fontSize = 12.sp)
                                    if (elapsed.isNotEmpty()) {
                                        Text(elapsed, color = TxtGray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── ScreenDisponibilidadePreferencia ──────────────────────────────────────────
@Composable
fun ScreenDisponibilidadePreferencia(onBack: () -> Unit) {
    val context    = LocalContext.current
    val tokenMgr   = MiauGendaApp.getTokenManager(context)
    val userName   = tokenMgr.getUserName() ?: "Xavier"
    val currentUserId = tokenMgr.getUserId()
    val scope      = rememberCoroutineScope()

    val today        = remember { LocalDate.now() }
    var displayMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(today) }
    // Map de date -> Availability (da API)
    val availMap     = remember { mutableStateMapOf<LocalDate, Availability>() }
    // Derivado: converte para AvailabilityPref compatível com a UI
    val prefs by remember { derivedStateOf {
        availMap.mapValues { (_, av) ->
            listOf(AvailabilityPref(
                tipo = if (av.type == "PREFERRED") "PREFERIDA" else "INDISPONIVEL",
                nota = av.note ?: ""
            ))
        }
    }}
    var adicionandoPara by remember { mutableStateOf<LocalDate?>(null) }

    // Carregar apenas as disponibilidades do utilizador actual
    LaunchedEffect(Unit) {
        try {
            val r = RetrofitClient.availabilityApi.getAvailabilities(userId = currentUserId)
            if (r.isSuccessful) {
                availMap.clear()
                r.body()?.forEach { av ->
                    val date = LocalDate.parse(av.date.substring(0, 10))
                    availMap[date] = av
                }
            }
        } catch (_: Exception) { }
    }

    // Interceptar botão retroceder do sistema quando sub-ecrã está aberto
    BackHandler(enabled = adicionandoPara != null) { adicionandoPara = null }

    if (adicionandoPara != null) {
        val dataAlvo = adicionandoPara!!
        AdicionarDisponibilidadeScreen(
            userName = userName,
            data     = dataAlvo,
            onSave   = { tipo, startDate, endDate, nota ->
                val apiType = if (tipo == "PREFERIDA") "PREFERRED" else "UNAVAILABLE"
                scope.launch {
                    try {
                        var d = startDate
                        while (!d.isAfter(endDate)) {
                            val r = RetrofitClient.availabilityApi.createAvailability(
                                CreateAvailabilityBody(
                                    date = d.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                    type = apiType,
                                    note = nota.ifEmpty { null }
                                )
                            )
                            if (r.isSuccessful) r.body()?.let { av -> availMap[d] = av }
                            d = d.plusDays(1)
                        }
                    } catch (_: Exception) { }
                }
                adicionandoPara = null
            },
            onCancel = { adicionandoPara = null }
        )
        return
    }

    // Células do calendário
    val firstOfMonth = remember(displayMonth) { displayMonth.atDay(1) }
    val daysInMonth  = remember(displayMonth) { displayMonth.lengthOfMonth() }
    val startOffset  = remember(firstOfMonth) { firstOfMonth.dayOfWeek.value - 1 } // 0=Seg
    val prevMonth    = remember(displayMonth) { displayMonth.minusMonths(1) }
    val prevDays     = remember(prevMonth) { prevMonth.lengthOfMonth() }

    val cells = remember(displayMonth) {
        val list = mutableListOf<LocalDate>()
        for (i in startOffset downTo 1) list.add(prevMonth.atDay(prevDays - i + 1))
        for (d in 1..daysInMonth) list.add(displayMonth.atDay(d))
        val nextMonth = displayMonth.plusMonths(1)
        var nd = 1
        while (list.size % 7 != 0) list.add(nextMonth.atDay(nd++))
        list
    }

    val listDates = remember(selectedDate) { (0..27).map { selectedDate.plusDays(it.toLong()) } }

    Column(modifier = Modifier.fillMaxSize().background(DkBg).statusBarsPadding()) {
        // Cabeçalho
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clip(CircleShape).clickable { onBack() }.padding(4.dp)) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("A Minha Disponibilidade", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Outlined.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .border(1.5.dp, Blue, CircleShape)
                    .clickable { displayMonth = YearMonth.now(); selectedDate = today },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Schedule, null, tint = Blue, modifier = Modifier.size(18.dp))
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Calendário
            item {
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    // Navegação de mês
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }) {
                            Icon(Icons.Outlined.ChevronLeft, null, tint = Color.White)
                        }
                        Text(
                            displayMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "PT")))
                                .replaceFirstChar { it.uppercase() },
                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { displayMonth = displayMonth.plusMonths(1) }) {
                            Icon(Icons.Outlined.ChevronRight, null, tint = Color.White)
                        }
                    }

                    // Cabeçalho dias da semana
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Seg","Ter","Qua","Qui","Sex","Sáb","Dom").forEach { label ->
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(label, color = TxtGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    // Grelha do calendário
                    cells.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                val isCurrentMonth = date.month == displayMonth.month
                                val isToday    = date == today
                                val isSelected = date == selectedDate
                                val availType  = availMap[date]?.type // "PREFERRED" | "UNAVAILABLE" | null

                                val circleBg = when {
                                    isSelected -> Blue
                                    isToday    -> Color(0xFF1A2540)
                                    else       -> Color.Transparent
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clickable {
                                            selectedDate = date
                                            if (!isCurrentMonth) displayMonth = YearMonth.from(date)
                                        }
                                ) {
                                    // Círculo de seleção / hoje — centrado na célula
                                    if (circleBg != Color.Transparent) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(circleBg)
                                                .align(Alignment.Center)
                                        )
                                    }
                                    // Número do dia centrado no círculo
                                    Text(
                                        date.dayOfMonth.toString(),
                                        color = if (!isCurrentMonth) TxtGray.copy(alpha = 0.3f) else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    // Ponto de disponibilidade fora do círculo, em baixo
                                    if (availType != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (availType == "PREFERRED") DkGreen else Color(0xFFE53935))
                                                .align(Alignment.BottomCenter)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }

            // Lista de dias a partir da data selecionada
            listDates.forEach { date ->
                val dayPrefs = prefs[date] ?: emptyList()
                val isHighlighted = date == selectedDate
                val dateFmt = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale("pt", "PT"))

                item(key = date.toString()) {
                    // Cabeçalho do dia
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(if (isHighlighted) Color(0xFF0D1420) else DkBg)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            date.format(dateFmt).replaceFirstChar { it.uppercase() },
                            color = if (isHighlighted) Blue else TxtGray,
                            fontSize = 14.sp,
                            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).clickable { adicionandoPara = date },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, null, tint = Blue, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Conteúdo do dia
                    if (dayPrefs.isEmpty()) {
                        Text(
                            "Sem preferências definidas",
                            color = Color.White, fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                    } else {
                        dayPrefs.forEach { pref ->
                            val avEntry = availMap[date]
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Box(
                                        modifier = Modifier.size(18.dp).clip(CircleShape)
                                            .background(if (pref.tipo == "PREFERIDA") DkGreen else Color(0xFFE53935))
                                    )
                                    Column {
                                        Text(
                                            if (pref.tipo == "PREFERIDA") "Preferida" else "Indisponível",
                                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                                        )
                                        if (pref.nota.isNotEmpty()) {
                                            Text(pref.nota, color = TxtGray, fontSize = 13.sp)
                                        }
                                    }
                                }
                                if (avEntry != null) {
                                    Icon(
                                        Icons.Filled.Close, null,
                                        tint = TxtGray,
                                        modifier = Modifier.size(18.dp).clickable {
                                            scope.launch {
                                                try {
                                                    RetrofitClient.availabilityApi.deleteAvailability(avEntry.id)
                                                    availMap.remove(date)
                                                } catch (_: Exception) { }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
                }
            }
        }
    }
}

// ── AdicionarDisponibilidadeScreen ────────────────────────────────────────────
@Composable
fun AdicionarDisponibilidadeScreen(
    userName: String,
    data: LocalDate,
    onSave: (tipo: String, startDate: LocalDate, endDate: LocalDate, nota: String) -> Unit,
    onCancel: () -> Unit
) {
    var tipo          by remember { mutableStateOf("INDISPONIVEL") }
    var nota          by remember { mutableStateOf("") }
    var startDate     by remember { mutableStateOf(data) }
    var endDate       by remember { mutableStateOf(data) }
    var startExpanded by remember { mutableStateOf(false) }
    var endExpanded   by remember { mutableStateOf(false) }

    val effectiveEnd = if (!endDate.isBefore(startDate)) endDate else startDate

    Column(modifier = Modifier.fillMaxSize().background(DkBg).statusBarsPadding()) {
        // Barra de topo
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) { Text("Cancelar", color = Blue, fontSize = 16.sp) }
            Text(
                "Adicionar Disponibilidade",
                color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onSave(tipo, startDate, effectiveEnd, nota) }) {
                Text("Guardar", color = Blue, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))

        LazyColumn(modifier = Modifier.weight(1f)) {
            // Funcionário
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Funcionário", color = Color.White, fontSize = 15.sp)
                    Text(userName, color = TxtGray, fontSize = 15.sp)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }

            // Botões de rádio: tipo de preferência
            item {
                Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { tipo = "INDISPONIVEL" }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RadioButton(
                            selected = tipo == "INDISPONIVEL",
                            onClick  = { tipo = "INDISPONIVEL" },
                            colors   = RadioButtonDefaults.colors(selectedColor = Color(0xFFE53935), unselectedColor = Color(0xFFE53935))
                        )
                        Text("Indisponível para trabalhar", color = Color.White, fontSize = 15.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { tipo = "PREFERIDA" }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RadioButton(
                            selected = tipo == "PREFERIDA",
                            onClick  = { tipo = "PREFERIDA" },
                            colors   = RadioButtonDefaults.colors(selectedColor = DkGreen, unselectedColor = DkGreen)
                        )
                        Text("Preferência para trabalhar", color = Color.White, fontSize = 15.sp)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }

            // Data de Início
            item {
                DatePickerRow(
                    label    = "Data de Início",
                    date     = startDate,
                    expanded = startExpanded,
                    onToggle = { startExpanded = !startExpanded; if (startExpanded) endExpanded = false },
                    onDateChange = { startDate = it }
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }

            // Data de Fim
            item {
                DatePickerRow(
                    label    = "Data de Fim",
                    date     = effectiveEnd,
                    expanded = endExpanded,
                    onToggle = { endExpanded = !endExpanded; if (endExpanded) startExpanded = false },
                    onDateChange = { endDate = it }
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }

            // Nota
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    BasicTextField(
                        value = nota,
                        onValueChange = { nota = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Blue),
                        decorationBox = { inner ->
                            if (nota.isEmpty()) Text("Nota", color = TxtGray.copy(alpha = 0.55f), fontSize = 15.sp)
                            inner()
                        }
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }

            // Caixa informativa
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Info, null, tint = TxtGray, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                        Text(
                            "Para indisponibilidades por férias, doença, etc., submeta um pedido de férias.",
                            color = TxtGray, fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ── WheelPickerDash ───────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPickerDash(
    values: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemH = 42.dp
    val pad = 2
    val padded = remember(values) { List(pad) { "" } + values + List(pad) { "" } }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val snap  = rememberSnapFlingBehavior(state)
    LaunchedEffect(selectedIndex) {
        if (!state.isScrollInProgress) state.scrollToItem(selectedIndex)
    }
    Box(modifier = modifier.height(itemH * (pad * 2 + 1))) {
        Box(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(itemH)
                .background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(8.dp))
        )
        LazyColumn(state = state, flingBehavior = snap, modifier = Modifier.fillMaxSize()) {
            items(padded.size) { i ->
                val sel by remember { derivedStateOf { state.firstVisibleItemIndex + pad == i } }
                Box(Modifier.fillMaxWidth().height(itemH), contentAlignment = Alignment.Center) {
                    if (padded[i].isNotEmpty()) {
                        Text(
                            padded[i],
                            color = if (sel) Color.White else TxtGray.copy(alpha = 0.35f),
                            fontSize = if (sel) 22.sp else 16.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            val idx = state.firstVisibleItemIndex.coerceIn(0, values.size - 1)
            if (idx != selectedIndex) onIndexChange(idx)
        }
    }
}

// ── DatePickerRow ─────────────────────────────────────────────────────────────
@Composable
private fun DatePickerRow(
    label: String,
    date: LocalDate,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDateChange: (LocalDate) -> Unit
) {
    val months = listOf(
        "Janeiro","Fevereiro","Março","Abril","Maio","Junho",
        "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"
    )
    val years = (2025..2030).map { it.toString() }

    var dayIdx   by remember(date) { mutableIntStateOf(date.dayOfMonth - 1) }
    var monthIdx by remember(date) { mutableIntStateOf(date.monthValue - 1) }
    var yearIdx  by remember(date) { mutableIntStateOf(years.indexOf(date.year.toString()).coerceAtLeast(0)) }

    val daysInMonth = remember(monthIdx, yearIdx) {
        val y = years.getOrNull(yearIdx)?.toIntOrNull() ?: 2026
        YearMonth.of(y, (monthIdx + 1).coerceIn(1, 12)).lengthOfMonth()
    }
    val days = remember(daysInMonth) { (1..daysInMonth).map { "%02d".format(it) } }
    val safeDayIdx = dayIdx.coerceIn(0, daysInMonth - 1)

    LaunchedEffect(safeDayIdx, monthIdx, yearIdx) {
        val y = years.getOrNull(yearIdx)?.toIntOrNull() ?: 2026
        val m = (monthIdx + 1).coerceIn(1, 12)
        val d = (safeDayIdx + 1).coerceIn(1, daysInMonth)
        runCatching { onDateChange(LocalDate.of(y, m, d)) }
    }

    val dateLbl = remember(date) {
        date.format(DateTimeFormatter.ofPattern("EEE, d 'de' MMMM yyyy", Locale("pt", "PT")))
            .replaceFirstChar { it.uppercase() }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 15.sp)
            Box(
                modifier = if (expanded)
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Blue.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 4.dp)
                else Modifier
            ) {
                Text(dateLbl, color = if (expanded) Blue else Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
            }
        }
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPickerDash(values = days,   selectedIndex = safeDayIdx, onIndexChange = { dayIdx   = it }, modifier = Modifier.weight(1f))
                WheelPickerDash(values = months, selectedIndex = monthIdx,   onIndexChange = { monthIdx = it }, modifier = Modifier.weight(2.2f))
                WheelPickerDash(values = years,  selectedIndex = yearIdx,    onIndexChange = { yearIdx  = it }, modifier = Modifier.weight(1.5f))
            }
        }
    }
}

// ── PedidoFeriasScreen ────────────────────────────────────────────────────────
@Composable
private fun PedidoFeriasScreen(userName: String, userId: Int = -1, onBack: () -> Unit) {
    val today = LocalDate.now()
    var fromDate     by remember { mutableStateOf(today) }
    var toDate       by remember { mutableStateOf(today) }
    var diaInteiro   by remember { mutableStateOf(true) }
    var motivo       by remember { mutableStateOf("") }
    var expandedField by remember { mutableStateOf<String?>(null) }
    var bannerMsg    by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val totalDias = remember(fromDate, toDate) {
        if (!toDate.isBefore(fromDate)) ChronoUnit.DAYS.between(fromDate, toDate) + 1L else 1L
    }

    Box(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra de topo
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Cancelar", color = Blue, fontSize = 16.sp) }
                Text("Novo Pedido de Férias", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            errorMsg = null
                            try {
                                val r = RetrofitClient.requestApi.createTimeOffRequest(
                                    CreateTimeOffRequestBody(
                                        startDate = fromDate.toString(),
                                        endDate   = toDate.toString(),
                                        allDay    = diaInteiro,
                                        reason    = motivo.trim().ifEmpty { null }
                                    )
                                )
                                if (r.isSuccessful) {
                                    bannerMsg = "Pedido de férias enviado com sucesso"
                                } else {
                                    errorMsg = "Erro ao enviar pedido. Tente novamente."
                                }
                            } catch (_: Exception) {
                                errorMsg = "Sem ligação ao servidor."
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    Text("Criar", color = if (!isSubmitting) Blue else TxtGray, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))

            LazyColumn(modifier = Modifier.weight(1f)) {
                // Funcionário
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Funcionário", color = Color.White, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(userName, color = TxtGray, fontSize = 15.sp)
                            Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(16.dp))
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                }

                // Dia Inteiro
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dia Inteiro", color = Color.White, fontSize = 15.sp)
                        Switch(
                            checked = diaInteiro,
                            onCheckedChange = { diaInteiro = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Blue)
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                }

                // De
                item {
                    DatePickerRow(
                        label = "De",
                        date = fromDate,
                        expanded = expandedField == "from",
                        onToggle = { expandedField = if (expandedField == "from") null else "from" },
                        onDateChange = { d -> fromDate = d; if (toDate.isBefore(d)) toDate = d }
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                }

                // Até
                item {
                    DatePickerRow(
                        label = "Até",
                        date = toDate,
                        expanded = expandedField == "to",
                        onToggle = { expandedField = if (expandedField == "to") null else "to" },
                        onDateChange = { d -> toDate = if (!d.isBefore(fromDate)) d else fromDate }
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                }

                // Dias Totais
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dias Totais", color = Color.White, fontSize = 15.sp)
                        Text("$totalDias dia${if (totalDias != 1L) "s" else ""}", color = TxtGray, fontSize = 15.sp)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                }

                // Motivo
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        BasicTextField(
                            value = motivo,
                            onValueChange = { motivo = it },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Blue),
                            decorationBox = { inner ->
                                if (motivo.isEmpty()) Text("Motivo", color = TxtGray.copy(alpha = 0.55f), fontSize = 15.sp)
                                inner()
                            }
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                }
            }
        }

        // Banner de sucesso
        bannerMsg?.let { msg ->
            LaunchedEffect(msg) { delay(2800); onBack() }
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    .padding(top = 70.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DkGreen)
                    .pointerInput(msg) { detectVerticalDragGestures { _, d -> if (d < -20f) bannerMsg = null } }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(msg, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
            }
        }
        errorMsg?.let { msg ->
            LaunchedEffect(msg) { delay(3000); errorMsg = null }
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    .padding(top = 70.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RedBadge)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Error, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(msg, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── PedidoTrocaScreen ─────────────────────────────────────────────────────────
@Composable
private fun PedidoTrocaScreen(myShifts: List<Shift>, userId: Int = -1, onBack: () -> Unit) {
    var selectedMyShift        by remember { mutableStateOf<Shift?>(null) }
    var selectedColleagueShift by remember { mutableStateOf<Shift?>(null) }
    var showMyPicker           by remember { mutableStateOf(false) }
    var showColleaguePicker    by remember { mutableStateOf(false) }
    var motivo                 by remember { mutableStateOf("") }
    var bannerMsg              by remember { mutableStateOf<String?>(null) }
    var isSubmitting           by remember { mutableStateOf(false) }
    var errorMsg               by remember { mutableStateOf<String?>(null) }
    var colleagueShifts        by remember { mutableStateOf<List<Shift>>(emptyList()) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Load colleague shifts on screen open
    LaunchedEffect(Unit) {
        try {
            val today = LocalDate.now().toString()
            val r = RetrofitClient.shiftApi.getShifts(week = today)
            if (r.isSuccessful) {
                colleagueShifts = r.body().orEmpty().filter { it.published && it.user?.id != userId }
            }
        } catch (_: Exception) { }
    }

    val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale("pt", "PT"))
    fun shiftLabel(s: Shift): String {
        val d = runCatching { LocalDate.parse(s.date.substring(0, 10)).format(dateFmt) }.getOrDefault("")
        return "${s.resolvedName()} — $d ${s.resolvedStartTime()}-${s.resolvedEndTime()}"
    }

    Box(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra de topo
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Cancelar", color = Blue, fontSize = 16.sp) }
                Text("Novo Pedido de Troca", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = {
                        val myShift = selectedMyShift
                        val colleagueShift = selectedColleagueShift
                        if (myShift != null && colleagueShift != null) {
                            scope.launch {
                                isSubmitting = true
                                errorMsg = null
                                try {
                                    val r = RetrofitClient.requestApi.createShiftSwapRequest(
                                        CreateShiftSwapRequestBody(
                                            requesterShiftId = myShift.id,
                                            targetShiftId    = colleagueShift.id,
                                            reason           = motivo.trim().ifEmpty { null }
                                        )
                                    )
                                    if (r.isSuccessful) {
                                        bannerMsg = "Pedido de troca enviado com sucesso"
                                    } else {
                                        errorMsg = "Erro ao enviar pedido. Tente novamente."
                                    }
                                } catch (_: Exception) {
                                    errorMsg = "Sem ligação ao servidor."
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                    },
                    enabled = selectedMyShift != null && selectedColleagueShift != null && !isSubmitting
                ) {
                    Text(
                        "Criar",
                        color = if (selectedMyShift != null && selectedColleagueShift != null && !isSubmitting) Blue else TxtGray,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))

            // Caixa de informação
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF111827)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Info, null, tint = TxtGray, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                    Text(
                        "Selecione o seu turno e o turno do colega que pretende trocar. Ambos os turnos têm de estar publicados.",
                        color = TxtGray, fontSize = 13.sp
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))

            // Meu Turno
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showMyPicker = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Meu Turno", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        selectedMyShift?.let { shiftLabel(it) } ?: "Toque para selecionar",
                        color = if (selectedMyShift != null) Blue else TxtGray,
                        fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(20.dp))
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))

            // Separador com ícone de troca
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(DkSurface2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.SwapVert, null, tint = TxtGray, modifier = Modifier.size(22.dp))
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))

            // Turno do Colega
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showColleaguePicker = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Turno do Colega", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        selectedColleagueShift?.let { shiftLabel(it) } ?: "Toque para selecionar",
                        color = if (selectedColleagueShift != null) Blue else TxtGray,
                        fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(20.dp))
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))

            // Motivo
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("Motivo", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DkSurface2)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Blue),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                        decorationBox = { inner ->
                            if (motivo.isEmpty()) {
                                Text("Descreva o motivo da troca (opcional)...", color = TxtGray, fontSize = 14.sp)
                            }
                            inner()
                        }
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
        }

        // Picker — Meu Turno
        if (showMyPicker) {
            TurnoPickerOverlay(
                title = "Selecionar o Meu Turno",
                shifts = myShifts,
                mensagemVazia = "Não tem turnos publicados disponíveis.",
                onSelect = { selectedMyShift = it; showMyPicker = false },
                onDismiss = { showMyPicker = false }
            )
        }

        // Picker — Turno do Colega
        if (showColleaguePicker) {
            TurnoPickerOverlay(
                title = "Selecionar Turno do Colega",
                shifts = colleagueShifts,
                mensagemVazia = "Sem turnos de colegas publicados esta semana.",
                onSelect = { selectedColleagueShift = it; showColleaguePicker = false },
                onDismiss = { showColleaguePicker = false }
            )
        }

        // Banner de sucesso
        bannerMsg?.let { msg ->
            LaunchedEffect(msg) { delay(2800); onBack() }
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    .padding(top = 70.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DkGreen)
                    .pointerInput(msg) { detectVerticalDragGestures { _, d -> if (d < -20f) bannerMsg = null } }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(msg, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
            }
        }
        errorMsg?.let { msg ->
            LaunchedEffect(msg) { delay(3000); errorMsg = null }
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    .padding(top = 70.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RedBadge)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Error, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(msg, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── TurnoPickerOverlay ────────────────────────────────────────────────────────
@Composable
private fun TurnoPickerOverlay(
    title: String,
    shifts: List<Shift>,
    mensagemVazia: String,
    onSelect: (Shift) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale("pt", "PT"))
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(DkSurface)
                .clickable {}
                .padding(top = 16.dp, bottom = 32.dp)
        ) {
            Text(
                title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
            )
            DkDivider()
            if (shifts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text(mensagemVazia, color = TxtGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(shifts) { shift ->
                        val d = runCatching { LocalDate.parse(shift.date.substring(0, 10)).format(dateFmt) }.getOrDefault("")
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(shift) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(shift.resolvedName(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text("$d  •  ${shift.resolvedStartTime()} - ${shift.resolvedEndTime()}", color = TxtGray, fontSize = 13.sp)
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(16.dp))
                        }
                        DkDivider()
                    }
                }
            }
        }
    }
}

// ── PedidosEnviadosSection ────────────────────────────────────────────────────
@Composable
private fun PedidosEnviadosSection(
    timeOffRequests: List<TimeOffRequest> = emptyList(),
    shiftSwapRequests: List<ShiftSwapRequest> = emptyList()
) {
    val dateFmtLong = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale("pt", "PT"))

    fun formatDate(iso: String) = runCatching {
        LocalDate.parse(iso.substring(0, 10)).format(dateFmtLong).replaceFirstChar { it.uppercaseChar() }
    }.getOrDefault(iso.substring(0, 10))

    // Só mostra pedidos PENDENTES
    val pendingTor = timeOffRequests.filter { it.status == "PENDING" }
    val pendingSsr = shiftSwapRequests.filter { it.status == "PENDING" }
    val hasAny = pendingTor.isNotEmpty() || pendingSsr.isNotEmpty()

    Column(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 22.dp).fillMaxWidth()) {
        if (!hasAny) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)).background(DkSurface)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Inbox, null, tint = TxtGray, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sem pedidos pendentes",
                        color = TxtGray, fontSize = 14.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            pendingTor.forEach { req ->
                val dateRange = if (req.startDate.substring(0, 10) == req.endDate.substring(0, 10)) {
                    formatDate(req.startDate)
                } else {
                    "${formatDate(req.startDate)} – ${formatDate(req.endDate)}"
                }
                val durationLabel = if (req.allDay) "Dia Inteiro" else "Parcial"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DkSurface)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("Pedido de Férias", color = TxtGray, fontSize = 13.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(dateRange, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(durationLabel, color = TxtGray, fontSize = 14.sp)
                    }
                    DkDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("↳", color = TxtGray, fontSize = 14.sp)
                        Text(
                            "Aguarda aprovação da gestão",
                            color = TxtGray,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            pendingSsr.forEach { req ->
                val myDate = req.requesterShift?.let { formatDate(it.date) } ?: ""
                val colDate = req.targetShift?.let { formatDate(it.date) } ?: ""
                val swapLabel = when {
                    req.targetAccepted == null -> "Aguarda resposta do colega"
                    req.targetAccepted == true -> "Aguarda aprovação da gestão"
                    else                       -> "Pendente"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DkSurface)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("Pedido de Troca", color = TxtGray, fontSize = 13.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (myDate.isNotEmpty() && colDate.isNotEmpty()) "$myDate ↔ $colDate" else myDate,
                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        val myTime = req.requesterShift?.let { "${it.resolvedStartTime().take(5)} – ${it.resolvedEndTime().take(5)}" }
                        if (!myTime.isNullOrBlank()) {
                            Text(myTime, color = TxtGray, fontSize = 14.sp)
                        }
                    }
                    DkDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("↳", color = TxtGray, fontSize = 14.sp)
                        Text(swapLabel, color = TxtGray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

