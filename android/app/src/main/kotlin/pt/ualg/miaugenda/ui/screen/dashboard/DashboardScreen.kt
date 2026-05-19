package pt.ualg.miaugenda.ui.screen.dashboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
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

// ── Sub-ecrãs internos ────────────────────────────────────────────────────────
private enum class SubScreen {
    Home, TurnosAgendados, AguardaConfirmacao, TurnosAbertos,
    EmPausa, EmTurno, ShiftOffer, FolhasPonto
}

// ── Root ──────────────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {},
    onCheckInClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAttendanceHistoryClick: () -> Unit = {},
    onAttendanceMonitorClick: () -> Unit = {},
    onSchedulerClick: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val context      = LocalContext.current
    val tokenManager = MiauGendaApp.getTokenManager(context)
    val uiState      by viewModel.uiState.collectAsState()
    val firstName    = (tokenManager.getUserName() ?: "Xavier").split(" ").first()

    val today      = LocalDate.now()
    val weekStart  = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    LaunchedEffect(weekStart) { viewModel.loadWeekShifts(weekStart) }

    var subScreen        by remember { mutableStateOf(SubScreen.Home) }
    var showApproveDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DkBg)
            .systemBarsPadding()
    ) {
        when (subScreen) {
            SubScreen.Home -> HomeScreen(
                firstName        = firstName,
                shifts           = uiState.shifts,
                today            = today,
                onNavigate       = { subScreen = it },
                onLogout         = { tokenManager.clearTokens(); onLogout() },
                onApprove        = { showApproveDialog = true },
                onCheckIn        = onCheckInClick,
                onSchedulerClick = onSchedulerClick
            )
            SubScreen.TurnosAgendados    -> ScreenTurnosAgendados(onBack = { subScreen = SubScreen.Home })
            SubScreen.AguardaConfirmacao -> ScreenAguardaConfirmacao(onBack = { subScreen = SubScreen.Home })
            SubScreen.TurnosAbertos      -> ScreenTurnosAbertos(onBack = { subScreen = SubScreen.Home })
            SubScreen.EmPausa            -> ScreenEmPausa(onBack = { subScreen = SubScreen.Home })
            SubScreen.EmTurno            -> ScreenEmTurno(onBack = { subScreen = SubScreen.Home })
            SubScreen.ShiftOffer         -> ScreenShiftOffer(onBack = { subScreen = SubScreen.Home })
            SubScreen.FolhasPonto        -> ScreenFolhasPonto(onBack = { subScreen = SubScreen.Home }, onSchedulerClick = onSchedulerClick)
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
    onNavigate: (SubScreen) -> Unit,
    onLogout: () -> Unit,
    onApprove: () -> Unit,
    onCheckIn: () -> Unit,
    onSchedulerClick: () -> Unit = {}
) {
    var fabOpen by remember { mutableStateOf(false) }

    // Timer de pausa: arranca em 2h08m48s e conta
    var timerSec by remember { mutableIntStateOf(7728) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); timerSec++ }
    }
    val timerStr = String.format(
        "%02d:%02d:%02d", timerSec / 3600, (timerSec % 3600) / 60, timerSec % 60
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            item { DashHeader(firstName, onLogout) }

            item {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("Boa noite, ") }
                        append("$firstName!")
                    },
                    color = Color.White,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
                )
            }

            item { BreakBanner(timerStr) }
            item { SectionTitle("Resumo de Hoje") }
            item { TodaySnapshot(onNavigate) }
            item { SectionTitle("Resumo da Semana") }
            item { WeekSnapshot(onNavigate) }
            item { TodayTasksRow() }
            item { SectionTitle("Pedidos para Si") }
            item { TimeOffCard(onApprove) }
            item { Spacer(Modifier.height(12.dp)) }
            item { ShiftOfferCard(onTap = { onNavigate(SubScreen.ShiftOffer) }) }
            item { SectionTitle("Esta Semana", topPad = 22.dp) }
            item { EstaSemanaSection(shifts, today) }
            item { Spacer(Modifier.height(28.dp)) }
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
                        .padding(end = 20.dp, bottom = 115.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    FabMenuItem("Nova Folha de Ponto", Icons.Outlined.AccessTime) {
                        fabOpen = false; onCheckIn()
                    }
                    FabMenuItem("Novo Pedido de Folga", Icons.Outlined.EventBusy) {
                        fabOpen = false
                    }
                    FabMenuItem("Adicionar Disponibilidade", Icons.Outlined.EventAvailable) {
                        fabOpen = false
                    }
                }
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 94.dp)
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

        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomNav(onSchedulerClick = onSchedulerClick)
        }
    }
}

// ── DashHeader ────────────────────────────────────────────────────────────────
@Composable
private fun DashHeader(firstName: String, onLogout: () -> Unit) {
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
                .clickable { onLogout() },
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

// ── BreakBanner ───────────────────────────────────────────────────────────────
@Composable
private fun BreakBanner(timerStr: String) {
    Box(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .padding(bottom = 22.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pausa iniciada às 21:00", color = Blue, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("6:00p - 3:00a", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(timerStr, color = Orange, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = Orange, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                Icon(Icons.Outlined.Alarm, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(9.dp))
                Text("Terminar Pausa", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
    val nowTime = LocalTime.now()
    val upcoming = shifts
        .filter { s ->
            val d = LocalDate.parse(s.date.substring(0, 10))
            d.isAfter(today) || (d == today && LocalTime.parse(s.shiftType.endTime).isAfter(nowTime))
        }
        .sortedBy { it.date }
        .take(3)

    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
        if (upcoming.isEmpty()) {
            // Mock entry matching prototype
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 6.dp)) {
                    Text("14", color = Blue, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                    Text("Qui", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DkSurface)
                ) {
                    Column(modifier = Modifier.padding(start = 17.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)) {
                        Text("Vendas", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("6:00p - 3:00a", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text("9h • 0/3", color = TxtGray, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
                            }
                            DkStatusBadge("Em Pausa", Orange, Orange.copy(alpha = 0.18f))
                        }
                    }
                    Box(modifier = Modifier.width(3.5.dp).matchParentSize().background(Orange))
                }
            }
        } else {
            upcoming.forEach { shift ->
                val shiftDate = LocalDate.parse(shift.date.substring(0, 10))
                val dayNum  = shiftDate.dayOfMonth.toString()
                val dayName = shiftDate.format(DateTimeFormatter.ofPattern("EEE", Locale("pt", "PT")))
                    .replaceFirstChar { it.uppercase() }.take(3)
                val isToday = shiftDate == today
                val dayColor = if (isToday) Blue else Color.White
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 6.dp)) {
                        Text(dayNum, color = dayColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
                        Text(dayName, color = dayColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DkSurface)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(shift.shiftType.name, color = Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                            Text("${shift.shiftType.startTime} - ${shift.shiftType.endTime}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Text(
            "Mostrar mais...",
            color = TxtGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

// ── FabMenuItem ───────────────────────────────────────────────────────────────
@Composable
private fun FabMenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Blue)
                .clickable { onClick() },
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
            .padding(top = 10.dp, bottom = 22.dp),
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
private fun ScreenFolhasPonto(onBack: () -> Unit, onSchedulerClick: () -> Unit = {}) {
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

        BottomNav(onSchedulerClick = onSchedulerClick)
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
