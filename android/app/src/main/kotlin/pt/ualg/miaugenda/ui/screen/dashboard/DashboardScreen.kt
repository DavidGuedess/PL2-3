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
import pt.ualg.miaugenda.MiauGendaApp
import pt.ualg.miaugenda.data.model.Shift
import pt.ualg.miaugenda.data.model.resolvedStartTime
import pt.ualg.miaugenda.data.model.resolvedEndTime
import pt.ualg.miaugenda.data.model.resolvedName
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
    val diaInteiro: Boolean,
    val nota: String = ""
)

// ── Sub-ecrãs internos ────────────────────────────────────────────────────────
private enum class SubScreen {
    Home, TurnosAgendados, AguardaConfirmacao, TurnosAbertos,
    EmPausa, EmTurno, ShiftOffer, FolhasPonto,
    PedidoFerias, PedidoTroca, DisponibilidadePreferencia
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
    viewModel: DashboardViewModel = viewModel()
) {
    val context      = LocalContext.current
    val tokenManager = MiauGendaApp.getTokenManager(context)
    val uiState      by viewModel.uiState.collectAsState()
    val fullName     = tokenManager.getUserName() ?: "Xavier"
    val firstName    = fullName.split(" ").first()

    val today      = LocalDate.now()
    val weekStart  = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    LaunchedEffect(weekStart) { viewModel.loadWeekShifts(weekStart) }

    var subScreen        by remember { mutableStateOf(SubScreen.Home) }
    var showApproveDialog by remember { mutableStateOf(false) }

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
                today                 = today,
                isClocked             = uiState.isClocked,
                clockedInSince        = uiState.clockedInSince,
                onNavigate            = { subScreen = it },
                onLogout              = { tokenManager.clearTokens(); onLogout() },
                onProfileClick        = onProfileClick,
                onApprove             = { showApproveDialog = true },
                onClockIn             = { viewModel.clockIn {} },
                onClockOut            = { viewModel.clockOut {} },
                onSchedulerClick      = onSchedulerClick,
                onNotificationsClick  = onNotificationsClick,
                onInboxClick          = onInboxClick,
                onEquipaClick         = onEquipaClick
            )
            SubScreen.TurnosAgendados    -> ScreenTurnosAgendados(onBack = { subScreen = SubScreen.Home })
            SubScreen.AguardaConfirmacao -> ScreenAguardaConfirmacao(onBack = { subScreen = SubScreen.Home })
            SubScreen.TurnosAbertos      -> ScreenTurnosAbertos(onBack = { subScreen = SubScreen.Home })
            SubScreen.EmPausa            -> ScreenEmPausa(onBack = { subScreen = SubScreen.Home })
            SubScreen.EmTurno            -> ScreenEmTurno(onBack = { subScreen = SubScreen.Home })
            SubScreen.ShiftOffer         -> ScreenShiftOffer(onBack = { subScreen = SubScreen.Home })
            SubScreen.FolhasPonto        -> ScreenFolhasPonto(onBack = { subScreen = SubScreen.Home }, onSchedulerClick = onSchedulerClick, onNotificationsClick = onNotificationsClick, onInboxClick = onInboxClick)
            SubScreen.PedidoFerias       -> PedidoFeriasScreen(userName = fullName, onBack = { subScreen = SubScreen.Home })
            SubScreen.PedidoTroca        -> PedidoTrocaScreen(myShifts = uiState.shifts.filter { it.published }, onBack = { subScreen = SubScreen.Home })
            SubScreen.DisponibilidadePreferencia -> ScreenDisponibilidadePreferencia(onBack = { subScreen = SubScreen.Home })
        }
        if (showApproveDialog) {
            ApproveDialog(onClose = { showApproveDialog = false })
        }
    }
}

// ── HomeScreen ────────────────────────────────────────────────────────────────
@Composable
private fun HomeScreen(
    firstName: String,
    shifts: List<Shift>,
    today: LocalDate,
    isClocked: Boolean = false,
    clockedInSince: String? = null,
    onNavigate: (SubScreen) -> Unit,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit = {},
    onApprove: () -> Unit,
    onClockIn: () -> Unit = {},
    onClockOut: () -> Unit = {},
    onSchedulerClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},
    onEquipaClick: () -> Unit = {}
) {
    var fabOpen by remember { mutableStateOf(false) }

    // Só turnos PUBLICADOS contam para o registo de ponto
    val todayShift = remember(shifts, today) {
        shifts.firstOrNull { s ->
            LocalDate.parse(s.date.substring(0, 10)) == today && s.published
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
    val greetingWord = when (LocalTime.now().hour) {
        in 5..11  -> "Bom dia"
        in 12..18 -> "Boa tarde"
        else      -> "Boa noite"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item { DashHeader(firstName, onProfileClick) }

                item {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("$greetingWord, ") }
                            append("$firstName!")
                        },
                        color = Color.White,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
                    )
                }

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

                item { SectionTitle("Pedidos para Si") }
                item { TimeOffCard(onApprove) }
                item { Spacer(Modifier.height(22.dp)) }

                item { SectionTitle("Resumo de Hoje") }
                item { TodaySnapshot(onNavigate) }
                item { SectionTitle("Resumo da Semana") }
                item { WeekSnapshot(onNavigate) }

                item { SectionTitle("Esta Semana", topPad = 8.dp) }
                item { EstaSemanaSection(shifts, today) }

                item { SectionTitle("Pedidos Enviados", topPad = 8.dp) }
                item { PedidosEnviadosSection() }
                item { Spacer(Modifier.height(28.dp)) }
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
                        onNavigate(SubScreen.DisponibilidadePreferencia)
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

        // Flag icon (center)
        Icon(
            imageVector = Icons.Outlined.Flag,
            contentDescription = null,
            tint = Blue,
            modifier = Modifier.size(30.dp)
        )

        // Help icon (right)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(1.5.dp, Color(0xFF333333), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Help,
                contentDescription = null,
                tint = TxtGray,
                modifier = Modifier.size(18.dp)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Break button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.FreeBreakfast,
                                contentDescription = "Pausa",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        // Clock Out button
                        Button(
                            onClick = onClockOut,
                            modifier = Modifier.weight(1f),
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
}

// ── TodaySnapshot ─────────────────────────────────────────────────────────────
@Composable
private fun TodaySnapshot(onNavigate: (SubScreen) -> Unit) {
    data class TodayItem(val icon: ImageVector, val label: String, val value: String, val sub: SubScreen?)

    val items = listOf(
        TodayItem(Icons.Outlined.CheckCircle,    "Com Entrada Registada", "1",   SubScreen.EmTurno),
        TodayItem(Icons.Outlined.Pause,              "Em Pausa",            "1",   SubScreen.EmPausa),
        TodayItem(Icons.Outlined.Warning,        "Em Atraso",             "0",   null),
        TodayItem(Icons.Outlined.Block,          "Folga",                 "1",   null),
        TodayItem(Icons.Outlined.Assignment,     "Listas de Tarefas",     "0/2", null),
    )
    ListCard(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 22.dp)) {
        items.forEachIndexed { i, item ->
            ListRow(
                icon      = item.icon,
                iconTint  = TxtGray,
                label     = item.label,
                value     = item.value,
                valueColor = Color.White,
                clickable = item.sub != null,
                onClick   = { item.sub?.let(onNavigate) }
            )
            if (i < items.lastIndex) DkDivider()
        }
    }
}

// ── WeekSnapshot ──────────────────────────────────────────────────────────────
@Composable
private fun WeekSnapshot(onNavigate: (SubScreen) -> Unit) {
    data class WeekItem(val icon: ImageVector, val label: String, val value: String, val valueColor: Color, val sub: SubScreen)

    val items = listOf(
        WeekItem(Icons.Outlined.DateRange,       "Total de Turnos Agendados",   "8", Color.White, SubScreen.TurnosAgendados),
        WeekItem(Icons.Outlined.Groups,          "A Aguardar Confirmação",      "1", Orange,      SubScreen.AguardaConfirmacao),
        WeekItem(Icons.Outlined.HelpOutline,     "Turnos Abertos por Reclamar", "1", Orange,      SubScreen.TurnosAbertos),
        WeekItem(Icons.Outlined.AccessTime,      "Folhas de Ponto Pendentes",   "2", Color.White, SubScreen.FolhasPonto),
    )
    ListCard(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 22.dp)) {
        items.forEachIndexed { i, item ->
            ListRow(
                icon       = item.icon,
                iconTint   = if (item.valueColor == Orange) Orange else TxtGray,
                label      = item.label,
                labelColor = item.valueColor,
                value      = item.value,
                valueColor = item.valueColor,
                clickable  = true,
                onClick    = { onNavigate(item.sub) }
            )
            if (i < items.lastIndex) DkDivider()
        }
    }
}

// ── TodayTasksRow ─────────────────────────────────────────────────────────────
@Composable
private fun TodayTasksRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Assignment, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Text("Tarefas de Hoje", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DkSurface2)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("0/3", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TxtGray, modifier = Modifier.size(16.dp))
        }
    }
}

// ── TimeOffCard ───────────────────────────────────────────────────────────────
@Composable
private fun TimeOffCard(onApprove: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                DkAvatar(initials = "MD", color = Color(0xFFC87941), size = 46)
                Column {
                    Text(
                        "Maria Doe enviou-lhe um pedido de folga",
                        color = Color.White, fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 7.dp)
                    )
                    Text("Sex, 15 Mai 2026", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Dia Inteiro", color = Color.White, fontSize = 14.sp)
                }
            }
            DkDivider()
            Text("Consulta médica", color = TxtGray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 11.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onApprove,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Aprovar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── ShiftOfferCard ────────────────────────────────────────────────────────────
@Composable
private fun ShiftOfferCard(onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DkSurface)
            .clickable { onTap() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                DkAvatar(initials = "JL", color = Color(0xFF5B5FEF), size = 46)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pedido de Troca de Turno", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .border(1.dp, Blue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 9.dp, vertical = 2.dp)
                        ) {
                            Text("A Aguardar Aprovação", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Sex, 15 Mai 2026", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text("12:00 AM", color = Color.White, fontSize = 14.sp)
                        Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = TxtGray, modifier = Modifier.size(16.dp))
                        Text("04:00 AM", color = Color.White, fontSize = 14.sp)
                    }
                    Text("Vendas", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            DkDivider()
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.SwapVert, contentDescription = null, tint = TxtGray, modifier = Modifier.size(22.dp))
                // Overlapping avatars
                Box(modifier = Modifier.height(36.dp).width(58.dp)) {
                    Box(modifier = Modifier.offset(x = 0.dp)) {
                        DkAvatar(initials = "AS", color = Color(0xFF00ACC1), size = 36)
                        // Check badge
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(Blue)
                                .border(2.dp, DkSurface, CircleShape)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(9.dp))
                        }
                    }
                    Box(modifier = Modifier.offset(x = 22.dp)) {
                        DkAvatar(initials = "MD", color = Color(0xFFC87941), size = 36)
                    }
                }
            }
        }
    }
}

// ── EstaSemanaSection ─────────────────────────────────────────────────────────
@Composable
private fun EstaSemanaSection(shifts: List<Shift>, today: LocalDate) {
    val weekShifts = shifts.sortedBy { it.date }.take(5)

    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
        if (weekShifts.isEmpty()) {
            Text(
                "Sem turnos esta semana.",
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

                // Shift duration
                val durationHrs = runCatching {
                    val s = LocalTime.parse(shift.resolvedStartTime())
                    val e = LocalTime.parse(shift.resolvedEndTime())
                    val dur = java.time.Duration.between(s, e)
                    if (dur.isNegative) dur.plusHours(24).toHours() else dur.toHours()
                }.getOrDefault(0L)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .width(36.dp)
                    ) {
                        Text(dayNum, color = dayColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
                        Text(dayAbbr, color = dayColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DkSurface)
                    ) {
                        // Blue left border
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(Blue)
                        )
                        Row(modifier = Modifier.weight(1f).padding(start = 10.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    shift.resolvedName(),
                                    color = Blue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    "${shift.resolvedStartTime()} - ${shift.resolvedEndTime()}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (durationHrs > 0) {
                                    Text(
                                        "${durationHrs}h",
                                        color = TxtGray,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            // Status badge
                            if (isPast) {
                                DkStatusBadge("Concluído", TxtGray, TxtGray.copy(alpha = 0.15f))
                            } else if (isToday) {
                                DkStatusBadge("Hoje", Blue, Blue.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }
        }
        Text(
            "Mostrar mais...",
            color = TxtGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
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

// ── BottomNav ─────────────────────────────────────────────────────────────────
@Composable
private fun BottomNav(activeIndex: Int = 0, onSchedulerClick: () -> Unit = {}) {
    data class NavItem(val label: String, val icon: ImageVector, val badge: Int, val onClick: () -> Unit)

    val items = listOf(
        NavItem("Início",        Icons.Outlined.Home,          0) {},
        NavItem("Agenda",        Icons.Outlined.DateRange,      0, onSchedulerClick),
        NavItem("Caixa",         Icons.Outlined.Inbox,          1) {},
        NavItem("Notificações",  Icons.Outlined.Notifications,  2) {},
        NavItem("Menu",          Icons.Outlined.Menu,           0) {},
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DkSurface)
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.07f), shape = RectangleShape)
            .pointerInput(Unit) {}
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items.forEachIndexed { i, item ->
            val active = i == activeIndex
            Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable { item.onClick() }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        Icon(
                            if (active && item.label == "Início") Icons.Filled.Home else item.icon,
                            contentDescription = item.label,
                            tint = if (active) Blue else TxtGray,
                            modifier = Modifier.size(22.dp)
                        )
                        if (item.badge > 0) {
                            Box(
                                modifier = Modifier
                                    .size(17.dp)
                                    .clip(CircleShape)
                                    .background(RedBadge)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${item.badge}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    Text(
                        item.label,
                        fontSize = 10.sp,
                        color = if (active) Blue else TxtGray,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
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
        Column(modifier = Modifier.padding(start = 17.dp, top = 12.dp, end = 14.dp, bottom = 12.dp)) {
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
        // Borda esquerda colorida
        Box(modifier = Modifier.width(3.5.dp).matchParentSize().background(deptColor))
    }
}

// ── Sub-ecrãs ─────────────────────────────────────────────────────────────────

@Composable
private fun ScreenTurnosAgendados(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        SubHeader("Total de Turnos Agendados", onBack)
        StatRow(listOf("Turnos" to "8", "Funcionários" to "4", "Horas totais" to "62.0"))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
        ) {
            item {
                DayGroup("13", "Qua") {
                    ShiftCardItem("Segurança", DkPurple, "MD", Color(0xFFC87941), "Maria Doe", "9:00a - 5:00p • 0/3", "Concluído", TxtGray, TxtGray.copy(alpha = 0.15f))
                }
            }
            item {
                DayGroup("14", "Qui", Blue) {
                    ShiftCardItem("Vendas", Orange, "XB", Color(0xFF5B5FEF), "Xavier Bolotinha", "6:00p - 3:00a • 0/3", "Em Pausa", Orange, Orange.copy(alpha = 0.18f))
                    ShiftCardItem("Segurança", DkPurple, "MD", Color(0xFFC87941), "Maria Doe", "9:00p - 6:00a • 0/3", "Em Turno", DkGreen, DkGreen.copy(alpha = 0.15f))
                }
            }
            item {
                DayGroup("15", "Sex") {
                    ShiftCardItem("Vendas", Orange, "MDoe", Color(0xFF5B5FEF), "Michael Doe", "0:00a - 4:00a • 0/3", "A Iniciar", Blue, Blue.copy(alpha = 0.15f))
                    ShiftCardItem("Admin", DkPurple, "MDoe", Color(0xFF5B5FEF), "Michael Doe", "9:00a - 5:00p • 0/3", "Confirmado", Blue, Blue.copy(alpha = 0.15f))
                    ShiftCardItem("Vendas", Orange, "JD", Color(0xFF00ACC1), "Jane Doe", "9:00a - 5:00p • 0/3", "Confirmado", Blue, Blue.copy(alpha = 0.15f))
                    ShiftCardItem("Segurança", DkPurple, "MD", Color(0xFFC87941), "Maria Doe", "9:00a - 5:00p • 0/3", "Pendente", Orange, Orange.copy(alpha = 0.15f))
                }
            }
            item {
                DayGroup("16", "Sáb") {
                    OpenShiftCard()
                }
            }
        }
    }
}

@Composable
private fun ScreenAguardaConfirmacao(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        SubHeader("A Aguardar Confirmação", onBack)
        StatRow(listOf("Turnos" to "1", "Funcionários" to "1", "Horas totais" to "8.0"))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
        ) {
            item {
                DayGroup("15", "Sex") {
                    ShiftCardItem("Segurança", DkPurple, "MD", Color(0xFFC87941), "Maria Doe", "9:00a - 5:00p • 0/3", "Pendente", Orange, Orange.copy(alpha = 0.15f))
                }
            }
        }
    }
}

@Composable
private fun ScreenTurnosAbertos(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        SubHeader("Turnos Abertos por Reclamar", onBack)
        StatRow(listOf("Turnos" to "1", "Funcionários" to "0", "Horas totais" to "8.0"))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
        ) {
            item {
                DayGroup("16", "Sáb") {
                    OpenShiftCard(surfaceColor = Color(0xFF0D1B2A))
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
        Column(modifier = Modifier.padding(start = 17.dp, top = 12.dp, end = 14.dp, bottom = 12.dp)) {
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
        Box(modifier = Modifier.width(3.5.dp).matchParentSize().background(Blue))
    }
}

@Composable
private fun ScreenEmPausa(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clip(CircleShape).clickable { onBack() }.padding(4.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                "Quinta-feira, 14 de Maio",
                color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(30.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("EM PAUSA", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DkAvatar(initials = "XB", color = Color.Transparent, size = 56, gradient = AvatarGrad)
            Column {
                Text("Vendas", color = Orange, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                Text("Xavier Bolotinha", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("6:00p - 3:00a • 0/3", color = TxtGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ScreenEmTurno(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clip(CircleShape).clickable { onBack() }.padding(4.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                "Quinta-feira, 14 de Maio",
                color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(30.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("EM TURNO", color = DkGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DkAvatar(initials = "MD", color = Color(0xFFC87941), size = 56)
            Column {
                Text("Segurança", color = DkPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                Text("Maria Doe", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("9:00p - 6:00a • 0/3", color = TxtGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ScreenShiftOffer(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DkBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item { SubHeader("Pedido de Troca de Turno", onBack) }
        item {
            Text(
                "Michael Doe gostaria de oferecer este turno",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.Center) {
                DkStatusBadge("A Aguardar Aprovação", Blue, Blue.copy(alpha = 0.2f))
            }
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DkSurface)
                    .padding(20.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.Center) {
                        DkAvatar(initials = "MDoe", color = Color(0xFF7B68EE), size = 72)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Icon(Icons.Outlined.DateRange, contentDescription = null, tint = TxtGray, modifier = Modifier.size(22.dp))
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("12:00", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                Text("AM", color = TxtGray, fontSize = 16.sp)
                                Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = TxtGray, modifier = Modifier.size(20.dp))
                                Text("04:00", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                Text("AM", color = TxtGray, fontSize = 16.sp)
                            }
                            Text("Sexta-feira, 15 de Maio 2026", color = TxtGray, fontSize = 14.sp)
                        }
                    }
                    DkDivider()
                    Row(
                        modifier = Modifier.padding(top = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(0xFFFF5722)))
                        Text("Vendas", color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.UnfoldMore, contentDescription = null, tint = TxtGray, modifier = Modifier.size(28.dp))
            }
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DkSurface2)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Para os seguintes funcionários", color = TxtGray, fontSize = 14.sp)
            }
            Spacer(Modifier.height(20.dp))
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DkSurface)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DkAvatar(initials = "JD", color = Color(0xFF00ACC1), size = 44)
                            Text("Jane Doe", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {},
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue)
                        ) {
                            Text("Aprovar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    DkDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DkAvatar(initials = "MD", color = Color(0xFFC87941), size = 44)
                            Text("Maria Doe", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(DkSurface2)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Ainda não aceitou", color = TxtGray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenFolhasPonto(onBack: () -> Unit, onSchedulerClick: () -> Unit = {}, onNotificationsClick: () -> Unit = {}, onInboxClick: () -> Unit = {}) {
    data class Entry(val id: Int, val name: String, val duration: String, val type: String, val initials: String, val avatarColor: Color)

    val entries = listOf(
        Entry(1, "Maria Doe",   "7h 55m", "Do Turno", "MD",   Color(0xFFC87941)),
        Entry(2, "Michael Doe", "8h 0m",  "Manual",   "MDoe", Color(0xFF5B5FEF)),
    )

    var swipedId by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.clip(CircleShape).clickable { onBack() }.padding(4.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                "Folhas de Ponto",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
            )
            Icon(Icons.Outlined.FilterList, contentDescription = null, tint = Blue, modifier = Modifier.size(22.dp))
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(DkSurface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DkSurface2)
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF555555)).padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Pendentes", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier.weight(1f).padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Todos", color = TxtGray, fontSize = 15.sp)
            }
        }

        // Date header
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Qua, 13 de Maio", color = TxtGray, fontSize = 13.sp)
        }

        // Entries list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp)
        ) {
            items(entries) { entry ->
                val isSwiped = swipedId == entry.id
                val offsetX by animateDpAsState(targetValue = if (isSwiped) (-240).dp else 0.dp, label = "swipe")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RectangleShape)
                ) {
                    // Action buttons (behind the row)
                    Row(modifier = Modifier.align(Alignment.CenterEnd).height(74.dp)) {
                        Box(
                            modifier = Modifier.width(80.dp).fillMaxHeight().background(RedReject).clickable { swipedId = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                Text("Rejeitar", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier.width(80.dp).fillMaxHeight().background(PurpleDk).clickable { swipedId = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                Text("Arredondar", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier.width(80.dp).fillMaxHeight().background(Blue).clickable { swipedId = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                Text("Aprovar", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Main row (slides left on tap)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = offsetX)
                            .background(DkBg)
                            .clickable { swipedId = if (isSwiped) null else entry.id }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DkAvatar(initials = entry.initials, color = entry.avatarColor, size = 50)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(entry.duration, color = TxtGray, fontSize = 14.sp)
                            Text(entry.type, color = TxtGray, fontSize = 14.sp)
                        }
                        DkStatusBadge("Pendente", Orange, Orange.copy(alpha = 0.15f))
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TxtGray, modifier = Modifier.size(16.dp))
                    }
                }
                DkDivider()
            }
        }

        // Approve all button
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp).padding(bottom = 6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(DkSurface)
                    .clickable {}
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = Blue, modifier = Modifier.size(20.dp))
                    Text("Aprovar todas as folhas pendentes", color = Blue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        AppBottomNav(
            active               = NavTab.HOME,
            onSchedulerClick     = onSchedulerClick,
            onNotificationsClick = onNotificationsClick,
            onInboxClick         = onInboxClick
        )
    }

}

// ── ScreenDisponibilidadePreferencia ──────────────────────────────────────────
@Composable
fun ScreenDisponibilidadePreferencia(onBack: () -> Unit) {
    val context  = LocalContext.current
    val userName = MiauGendaApp.getTokenManager(context).getUserName() ?: "Xavier"

    val today        = remember { LocalDate.now() }
    var displayMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(today) }
    val prefs        = remember { mutableStateMapOf<LocalDate, List<AvailabilityPref>>() }
    var adicionandoPara by remember { mutableStateOf<LocalDate?>(null) }

    // Navegação interna: mostrar formulário de adição
    adicionandoPara?.let { dataAlvo ->
        AdicionarDisponibilidadeScreen(
            userName = userName,
            data     = dataAlvo,
            onSave   = { pref ->
                prefs[dataAlvo] = (prefs[dataAlvo] ?: emptyList()) + pref
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

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
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
                                val hasPref    = prefs[date]?.isNotEmpty() == true

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clickable {
                                            selectedDate = date
                                            if (!isCurrentMonth) displayMonth = YearMonth.from(date)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Círculo de fundo
                                    if (isSelected) {
                                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Blue))
                                    } else if (isToday) {
                                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF1A2540)))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            date.dayOfMonth.toString(),
                                            color = when {
                                                !isCurrentMonth -> TxtGray.copy(alpha = 0.3f)
                                                isSelected || isToday -> Color.White
                                                else -> Color.White
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (hasPref) {
                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(DkGreen))
                                        } else {
                                            Spacer(Modifier.height(4.dp))
                                        }
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
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {}
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Box(
                                        modifier = Modifier.size(18.dp).clip(CircleShape)
                                            .background(if (pref.tipo == "PREFERIDA") DkGreen else TxtGray)
                                    )
                                    Column {
                                        Text(
                                            if (pref.tipo == "PREFERIDA") "Preferida" else "Indisponível",
                                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            if (pref.diaInteiro) "Dia Inteiro" else if (pref.nota.isNotEmpty()) pref.nota else "Dia Inteiro",
                                            color = TxtGray, fontSize = 13.sp
                                        )
                                    }
                                }
                                Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(16.dp))
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
    onSave: (AvailabilityPref) -> Unit,
    onCancel: () -> Unit
) {
    val dateFmt  = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale("pt", "PT"))
    var tipo     by remember { mutableStateOf("INDISPONIVEL") }
    var diaInteiro by remember { mutableStateOf(true) }
    var nota     by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
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
            TextButton(onClick = { onSave(AvailabilityPref(tipo, diaInteiro, nota)) }) {
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
                            colors   = RadioButtonDefaults.colors(selectedColor = TxtGray, unselectedColor = TxtGray)
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

            // Dia Inteiro
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dia Inteiro", color = Color.White, fontSize = 15.sp)
                    Switch(
                        checked = diaInteiro, onCheckedChange = { diaInteiro = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Blue)
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }

            // Hora de Início
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hora de Início", color = Color.White, fontSize = 15.sp)
                    Text(
                        data.format(dateFmt).replaceFirstChar { it.uppercase() },
                        color = TxtGray, fontSize = 14.sp
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }

            // Hora de Fim
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hora de Fim", color = Color.White, fontSize = 15.sp)
                    Text(
                        data.format(dateFmt).replaceFirstChar { it.uppercase() },
                        color = TxtGray, fontSize = 14.sp
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }

            // Repetir
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {}
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Repetir", color = Color.White, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Não repete", color = TxtGray, fontSize = 14.sp)
                        Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(16.dp))
                    }
                }
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
private fun PedidoFeriasScreen(userName: String, onBack: () -> Unit) {
    val today = LocalDate.now()
    var fromDate     by remember { mutableStateOf(today) }
    var toDate       by remember { mutableStateOf(today) }
    var diaInteiro   by remember { mutableStateOf(true) }
    var motivo       by remember { mutableStateOf("") }
    var expandedField by remember { mutableStateOf<String?>(null) }
    var bannerMsg    by remember { mutableStateOf<String?>(null) }

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
                TextButton(onClick = { bannerMsg = "Pedido de férias enviado com sucesso" }) {
                    Text("Criar", color = Blue, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
            LaunchedEffect(msg) { delay(2800); bannerMsg = null; onBack() }
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
    }
}

// ── PedidoTrocaScreen ─────────────────────────────────────────────────────────
@Composable
private fun PedidoTrocaScreen(myShifts: List<Shift>, onBack: () -> Unit) {
    var selectedMyShift        by remember { mutableStateOf<Shift?>(null) }
    var selectedColleagueShift by remember { mutableStateOf<Shift?>(null) }
    var showMyPicker           by remember { mutableStateOf(false) }
    var showColleaguePicker    by remember { mutableStateOf(false) }
    var motivo                 by remember { mutableStateOf("") }
    var bannerMsg              by remember { mutableStateOf<String?>(null) }

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
                    onClick = { if (selectedMyShift != null && selectedColleagueShift != null) bannerMsg = "Pedido de troca enviado com sucesso" },
                    enabled = selectedMyShift != null && selectedColleagueShift != null
                ) {
                    Text(
                        "Criar",
                        color = if (selectedMyShift != null && selectedColleagueShift != null) Blue else TxtGray,
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
                shifts = emptyList(),
                mensagemVazia = "O carregamento de turnos de colegas\nserá implementado em breve.",
                onSelect = { selectedColleagueShift = it; showColleaguePicker = false },
                onDismiss = { showColleaguePicker = false }
            )
        }

        // Banner de sucesso
        bannerMsg?.let { msg ->
            LaunchedEffect(msg) { delay(2800); bannerMsg = null; onBack() }
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
private fun PedidosEnviadosSection() {
    // TODO: substituir por dados reais do backend
    Box(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .padding(bottom = 22.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DkSurface),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Icon(
                Icons.Outlined.Inbox,
                contentDescription = null,
                tint = TxtGray,
                modifier = Modifier.size(32.dp).align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sem pedidos enviados",
                color = TxtGray, fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Os seus pedidos de férias e trocas aparecerão aqui.",
                color = TxtGray.copy(alpha = 0.6f), fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}

// ── ApproveDialog ─────────────────────────────────────────────────────────────
@Composable
private fun ApproveDialog(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DkSurface2)
                .clickable {}
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Aprovar este pedido de folga?",
                    color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onClose) {
                        Text("APROVAR", color = Blue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onClose) {
                        Text("CANCELAR", color = Blue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
