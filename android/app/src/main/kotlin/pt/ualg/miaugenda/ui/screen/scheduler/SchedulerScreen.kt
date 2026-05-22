package pt.ualg.miaugenda.ui.screen.scheduler

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import pt.ualg.miaugenda.data.model.resolvedStartTime
import pt.ualg.miaugenda.data.model.resolvedEndTime
import pt.ualg.miaugenda.data.model.resolvedName
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import pt.ualg.miaugenda.data.model.CreateUserRequest
import pt.ualg.miaugenda.ui.components.AppBottomNav
import pt.ualg.miaugenda.ui.components.NavTab
import pt.ualg.miaugenda.data.model.Shift
import pt.ualg.miaugenda.data.model.ShiftType
import pt.ualg.miaugenda.data.model.User
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
private val CellBlue   = Color(0xFF1565C0)
private val CellPurple = Color(0xFF6C3483)
private val CellRed    = Color(0xFFC0392B)
private val AvatarGrad = Brush.linearGradient(listOf(Color(0xFF5B5FEF), Color(0xFFC850C0), Color(0xFFF0696B)))

// ── Sub-ecrãs ─────────────────────────────────────────────────────────────────
private sealed class SchedScreen {
    object Grid : SchedScreen()
    data class DayDetail(val date: LocalDate) : SchedScreen()
    data class ShiftDetail(val shift: Shift) : SchedScreen()
    data class AddShift(val user: User?, val date: LocalDate) : SchedScreen()
    data class EditarTurno(val shift: Shift) : SchedScreen()
    data class UserSchedule(val user: User) : SchedScreen()
    object PublicarHorario : SchedScreen()
    object FiltrarHorario : SchedScreen()
    object FiltrarPosicao : SchedScreen()
    object CriarFuncionario : SchedScreen()
}

// ── Utilitários ───────────────────────────────────────────────────────────────
private fun userColor(userId: Int): Color {
    val colors = listOf(
        Color(0xFF5B5FEF), Color(0xFFE74C3C), Color(0xFF27AE60),
        Color(0xFF00ACC1), Color(0xFFC87941), Color(0xFF8E44AD),
        Color(0xFF2ECC71), Color(0xFFE67E22)
    )
    return colors[userId % colors.size]
}

private fun userGradient(userId: Int): Brush? {
    return if (userId % 4 == 0) AvatarGrad else null
}

private fun shiftCellColor(startTime: String, published: Boolean): Color {
    if (!published) return Color(0xFF2A2A2A)
    return when {
        startTime >= "20:00" || startTime < "06:00" -> CellPurple
        startTime >= "13:00" -> CellRed
        else -> CellBlue
    }
}

private fun categoryLabel(category: String): String = when (category) {
    "VETERINARIAN"  -> "Veterinário"
    "NURSE"         -> "Enfermeiro"
    "OPERATIONAL"   -> "Operacional"
    "ADMINISTRATIVE"-> "Administrativo"
    else            -> category
}

private fun weekDays(weekStart: LocalDate): List<LocalDate> =
    (0..6).map { weekStart.plusDays(it.toLong()) }

private fun dayAbbr(date: LocalDate): String {
    val abbrs = listOf("Seg","Ter","Qua","Qui","Sex","Sab","Dom")
    return abbrs[date.dayOfWeek.value - 1]
}

private fun dayNum(date: LocalDate): String = date.dayOfMonth.toString()

private fun monthLabel(weekStart: LocalDate): String {
    val fmt = DateTimeFormatter.ofPattern("MMM", Locale("pt", "PT"))
    return weekStart.format(fmt).replaceFirstChar { it.uppercase() }
}

private fun yearLabel(weekStart: LocalDate): String = weekStart.year.toString()

private fun fullDayLabel(date: LocalDate): String {
    val fmt = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "PT"))
    return date.format(fmt).replaceFirstChar { it.uppercase() }
}

private fun dateStr(date: LocalDate): String =
    date.format(DateTimeFormatter.ISO_LOCAL_DATE)

// ── Time picker wheel ─────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
    values: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemH = 42.dp
    val pad = 2  // items above/below center
    // Pad with empty strings so first/last real items can appear centred
    val padded = remember(values) { List(pad) { "" } + values + List(pad) { "" } }
    // firstVisibleItemIndex == selectedIndex puts values[selectedIndex] in the centre slot
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val snap  = rememberSnapFlingBehavior(state)

    // Keep scroll position in sync when parent changes selectedIndex (e.g. initial load)
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
                            fontSize = if (sel) 24.sp else 17.sp,
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

@Composable
private fun TimePickerRow(
    label: String,
    hour: Int,
    minute: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 15.sp)
            Text("%02d:%02d".format(hour, minute),
                color = Blue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        if (expanded) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                WheelPicker(
                    values = (0..23).map { "%02d".format(it) },
                    selectedIndex = hour,
                    onIndexChange = onHourChange,
                    modifier = Modifier.weight(1f))
                Text(":", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp))
                WheelPicker(
                    values = (0..59).map { "%02d".format(it) },
                    selectedIndex = minute,
                    onIndexChange = onMinuteChange,
                    modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun weekRangeLabel(weekStart: LocalDate): String {
    val end = weekStart.plusDays(6)
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale("pt", "PT"))
    return "${weekStart.format(fmt)} - ${end.format(fmt)} ${weekStart.year}"
}

private fun calcDurationHours(start: String, end: String): Int {
    val (sh, sm) = start.split(":").map { it.toIntOrNull() ?: 0 }
    val (eh, em) = end.split(":").map { it.toIntOrNull() ?: 0 }
    var total = (eh * 60 + em) - (sh * 60 + sm)
    if (total < 0) total += 24 * 60
    return total / 60
}

private fun userInitials(name: String): String {
    val parts = name.trim().split(" ")
    return if (parts.size >= 2) "${parts.first()[0]}${parts.last()[0]}" else name.take(2)
}

private fun calcDuration(start: String, end: String, breakMin: Int = 0): String {
    val (sh, sm) = start.split(":").map { it.toIntOrNull() ?: 0 }
    val (eh, em) = end.split(":").map { it.toIntOrNull() ?: 0 }
    var total = (eh * 60 + em) - (sh * 60 + sm) - breakMin
    if (total < 0) total += 24 * 60
    val h = total / 60; val m = total % 60
    return if (m == 0) "${h}h 0m" else "${h}h ${m}m"
}

private val STATUS_FILTERS = listOf(
    Triple("Abertura",          Icons.Outlined.RadioButtonUnchecked, Color(0xFF888888)),
    Triple("A aguardar",        Icons.Outlined.AccessTime,           Orange),
    Triple("Rejeitado",         Icons.Outlined.Cancel,               RedBadge),
    Triple("Confirmado",        Icons.Outlined.CheckCircle,          DkGreen),
    Triple("Em Turno",          Icons.Outlined.PlayArrow,            Blue),
    Triple("Em Pausa",          Icons.Outlined.Pause,                Orange),
    Triple("Atrasado",          Icons.Outlined.Schedule,             RedBadge),
    Triple("Nao preenchido",    Icons.Outlined.RemoveCircle,         TxtGray),
    Triple("Ausente",           Icons.Outlined.Person,               RedBadge),
    Triple("Concluido",         Icons.Outlined.CheckCircleOutline,   TxtGray),
)

// ── Root ──────────────────────────────────────────────────────────────────────
@Composable
fun SchedulerScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToEquipa: () -> Unit = {},
    viewModel: SchedulerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var screen by remember { mutableStateOf<SchedScreen>(SchedScreen.Grid) }
    var bannerMsg by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // message, isError
    val userRole = viewModel.userRole

    LaunchedEffect(Unit) { viewModel.loadWeek() }

    BackHandler(enabled = screen !is SchedScreen.Grid) { screen = SchedScreen.Grid }

    fun showBanner(msg: String, isError: Boolean = false) { bannerMsg = Pair(msg, isError) }

    // Objeto User mínimo para vista pessoal do EMPLOYEE
    val meUser = User(
        id             = viewModel.currentUserId,
        employeeNumber = viewModel.currentUserEmployeeNumber,
        name           = viewModel.currentUserName,
        email          = "",
        contact        = null,
        role           = userRole,
        category       = viewModel.currentUserCategory
    )

    Box(modifier = Modifier.fillMaxSize().background(DkBg).systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (val s = screen) {
                    is SchedScreen.Grid ->
                        if (userRole == "EMPLOYEE") {
                            // Funcionario ve apenas os seus proprios turnos
                            UserScheduleScreen(
                                user      = meUser,
                                uiState   = uiState,
                                weekStart = viewModel.getCurrentWeek(),
                                onBack    = { /* sem back na vista pessoal, e a root */ },
                                onShiftClick = { screen = SchedScreen.ShiftDetail(it) },
                                onPreviousWeek = { viewModel.previousWeek() },
                                onNextWeek     = { viewModel.nextWeek() }
                            )
                        } else {
                            SchedulerGrid(
                                uiState       = uiState,
                                viewModel     = viewModel,
                                userRole      = userRole,
                                onDayClick    = { screen = SchedScreen.DayDetail(it) },
                                onShiftClick  = { screen = SchedScreen.ShiftDetail(it) },
                                onEmptyClick  = { user, date -> screen = SchedScreen.AddShift(user, date) },
                                onUserClick   = { screen = SchedScreen.UserSchedule(it) },
                                onPublishClick = { screen = SchedScreen.PublicarHorario },
                                onFilterClick = { screen = SchedScreen.FiltrarHorario },
                                onCriarFuncionario = { screen = SchedScreen.CriarFuncionario },
                                onSuccess = { msg -> showBanner(msg) }
                            )
                        }
                    is SchedScreen.UserSchedule ->
                        UserScheduleScreen(
                            user     = s.user,
                            uiState  = uiState,
                            weekStart = viewModel.getCurrentWeek(),
                            onBack   = { screen = SchedScreen.Grid },
                            onShiftClick = { screen = SchedScreen.ShiftDetail(it) }
                        )
                    is SchedScreen.DayDetail ->
                        DayDetailScreen(
                            date       = s.date,
                            uiState    = uiState,
                            onBack     = { screen = SchedScreen.Grid },
                            onShiftClick = { screen = SchedScreen.ShiftDetail(it) },
                            onAddClick = { screen = SchedScreen.AddShift(null, s.date) }
                        )
                    is SchedScreen.ShiftDetail ->
                        ShiftDetailScreen(
                            shift     = s.shift,
                            uiState   = uiState,
                            onBack    = { screen = SchedScreen.Grid },
                            onEdit    = { screen = SchedScreen.EditarTurno(s.shift) },
                            onDelete  = { shiftId ->
                                viewModel.deleteShift(shiftId) { ok, msg ->
                                    showBanner(msg, !ok)
                                    screen = SchedScreen.Grid
                                }
                            },
                            onPublish = { msg -> showBanner(msg); screen = SchedScreen.Grid },
                            onPublishShift = { id, cb -> viewModel.publishShift(id, cb) }
                        )
                    is SchedScreen.AddShift ->
                        AddShiftScreen(
                            user       = s.user,
                            date       = s.date,
                            uiState    = uiState,
                            onBack     = { screen = SchedScreen.Grid },
                            onSuccess  = { userId, startTime, endTime, dateStr ->
                                viewModel.createShift(userId, dateStr, startTime, endTime) { ok, msg ->
                                    showBanner(msg, !ok)
                                    if (ok) screen = SchedScreen.Grid
                                }
                            }
                        )
                    is SchedScreen.EditarTurno ->
                        EditarTurnoScreen(
                            shift     = s.shift,
                            uiState   = uiState,
                            onBack    = { screen = SchedScreen.ShiftDetail(s.shift) },
                            onSuccess = { startTime, endTime, dateStr ->
                                viewModel.updateShift(s.shift.id, dateStr, startTime, endTime) { ok, msg ->
                                    showBanner(msg, !ok)
                                    if (ok) screen = SchedScreen.Grid
                                }
                            }
                        )
                    is SchedScreen.PublicarHorario ->
                        PublicarHorarioScreen(
                            uiState  = uiState,
                            onBack   = { screen = SchedScreen.Grid },
                            onSuccess = { msg -> showBanner(msg); screen = SchedScreen.Grid },
                            onPublishMultiple = { ids, cb -> viewModel.publishMultipleShifts(ids, cb) }
                        )
                    is SchedScreen.FiltrarHorario ->
                        FilterScreen(
                            onBack          = { screen = SchedScreen.Grid },
                            onPositionClick = { screen = SchedScreen.FiltrarPosicao },
                            onSuccess       = { showBanner("Filtros aplicados"); screen = SchedScreen.Grid }
                        )
                    is SchedScreen.FiltrarPosicao ->
                        PositionFilterScreen(
                            uiState = uiState,
                            onBack  = { screen = SchedScreen.FiltrarHorario }
                        )
                    is SchedScreen.CriarFuncionario ->
                        CriarFuncionarioScreen(
                            onBack = { screen = SchedScreen.Grid },
                            onCreateUser = { req, onResult -> viewModel.createUser(req) { ok, msg ->
                                if (ok) { showBanner(msg); screen = SchedScreen.Grid }
                                else onResult(false, msg)
                            }}
                        )
                }
            }
            if (screen is SchedScreen.Grid) {
                AppBottomNav(
                    active               = NavTab.SCHEDULER,
                    onHomeClick          = onNavigateToHome,
                    onNotificationsClick = onNavigateToNotifications,
                    onInboxClick         = onNavigateToInbox,
                    onMenuClick          = onNavigateToEquipa
                )
            }
        }

        // Banner de feedback (verde = sucesso, vermelho = erro; swipe para cima para fechar)
        bannerMsg?.let { (msg, isError) ->
            LaunchedEffect(msg) { delay(3500); bannerMsg = null }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isError) RedBadge else DkGreen)
                    .pointerInput(msg) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -20f) bannerMsg = null
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isError) Icons.Filled.Error else Icons.Filled.CheckCircle,
                        null, tint = Color.White, modifier = Modifier.size(20.dp)
                    )
                    Text(msg, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Grelha principal ──────────────────────────────────────────────────────────
@Composable
private fun SchedulerGrid(
    uiState: SchedulerUiState,
    viewModel: SchedulerViewModel,
    userRole: String,
    onDayClick: (LocalDate) -> Unit,
    onShiftClick: (Shift) -> Unit,
    onEmptyClick: (User?, LocalDate) -> Unit,
    onUserClick: (User) -> Unit,
    onPublishClick: () -> Unit,
    onFilterClick: () -> Unit,
    onCriarFuncionario: () -> Unit,
    onSuccess: (String) -> Unit = {}
) {
    val weekStart = viewModel.getCurrentWeek()
    val days = weekDays(weekStart)
    var showAddMenu by remember { mutableStateOf(false) }
    var showSelectUser by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showCopyShifts by remember { mutableStateOf(false) }
    var targetWeekForCopy by remember { mutableStateOf(weekStart.plusWeeks(1)) }
    var showCopyConfirm by remember { mutableStateOf(false) }
    var swipeDelta by remember { mutableStateOf(0f) }
    var selectedShiftIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val isSelectMode = selectedShiftIds.isNotEmpty()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var userMenuTarget by remember { mutableStateOf<User?>(null) }

    // Utilizadores visíveis na grelha: derivados do backend (weekAssignments + turnos sem assignment)
    val displayedUserIds = (
        uiState.weekAssignments.map { it.userId } +
        uiState.shifts.map { it.userId }
    ).toSet()
    val weekStr = viewModel.getCurrentWeek().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
    val draftCount = uiState.shifts.count { !it.published }
    // Ordenar pela ordem de inserção do weekAssignment (id crescente = quem entrou primeiro)
    val assignmentOrder = uiState.weekAssignments.sortedBy { it.id }.map { it.userId }
    val displayedUsers = (
        assignmentOrder.mapNotNull { uid -> uiState.users.find { it.id == uid } } +
        uiState.users.filter { it.id in displayedUserIds && it.id !in assignmentOrder }
    ).distinctBy { it.id }
    val availableUsers = uiState.users.filter { it.id !in displayedUserIds }

    Box(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (swipeDelta < -80) viewModel.nextWeek()
                    else if (swipeDelta > 80) viewModel.previousWeek()
                    swipeDelta = 0f
                },
                onDragCancel = { swipeDelta = 0f },
                onHorizontalDrag = { _, amount -> swipeDelta += amount }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Cabeçalho
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Agenda", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("•", color = Color.White, fontSize = 20.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Padrao", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Outlined.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.FilterList, null, tint = Blue,
                        modifier = Modifier.size(22.dp).clickable { onFilterClick() })
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(DkSurface2)
                            .clickable { showOptionsMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Navegação de semana
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ChevronLeft, null, tint = Blue,
                    modifier = Modifier.size(26.dp).clickable { viewModel.previousWeek() })
                Text(
                    "${monthLabel(weekStart)} ${yearLabel(weekStart)}",
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                )
                Icon(Icons.Filled.ChevronRight, null, tint = Blue,
                    modifier = Modifier.size(26.dp).clickable { viewModel.nextWeek() })
            }

            // Cabeçalho dos dias
            Row(modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(0.dp))) {
                Box(modifier = Modifier.width(56.dp).padding(6.dp)) {
                    Column {
                        Text(monthLabel(weekStart), color = TxtGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(yearLabel(weekStart), color = TxtGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                days.forEach { date ->
                    Column(
                        modifier = Modifier.weight(1f).clickable { onDayClick(date) }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(dayAbbr(date), color = TxtGray, fontSize = 9.sp)
                        Text(dayNum(date), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Blue)
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.WifiOff, null, tint = TxtGray, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(uiState.error, color = TxtGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadWeek() }, colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                            Text("Tentar novamente")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    // Linhas de funcionários
                    items(displayedUsers) { user ->
                        val userShifts = uiState.shifts.filter { it.userId == user.id }
                        val maxPerDay = days.maxOfOrNull { day ->
                            userShifts.count { it.date.startsWith(dateStr(day)) }
                        }?.coerceAtLeast(1) ?: 1
                        val rowH = (70 * maxPerDay).dp
                        val lineClr = Color.White.copy(alpha = 0.10f)
                        val totalHours = userShifts
                            .filter { it.published }
                            .sumOf { calcDurationHours(it.resolvedStartTime(), it.resolvedEndTime()) }
                        Row(
                            modifier = Modifier.fillMaxWidth().height(rowH)
                                .border(1.dp, lineClr, RoundedCornerShape(0.dp))
                        ) {
                            Column(
                                modifier = Modifier.width(56.dp).fillMaxHeight().padding(4.dp)
                                    .clickable { userMenuTarget = user },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                SchedAvatar(userInitials(user.name), userColor(user.id), userGradient(user.id), 32)
                                Text(userInitials(user.name), color = TxtGray, fontSize = 9.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (userShifts.isNotEmpty()) Text("${totalHours}h", color = TxtGray, fontSize = 9.sp)
                            }
                            days.forEach { day ->
                                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(lineClr))
                                val dayShifts = userShifts.filter { it.date.startsWith(dateStr(day)) }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(2.dp)) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Zona de turnos (ocupa o espaço disponível)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .let {
                                                    if (dayShifts.isEmpty() && !isSelectMode)
                                                        it.clickable { onEmptyClick(user, day) }
                                                    else it
                                                }
                                        ) {
                                            if (dayShifts.isNotEmpty()) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    dayShifts.forEach { shift ->
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            ShiftCell(
                                                                shift = shift,
                                                                isSelected = shift.id in selectedShiftIds,
                                                                isSelectMode = isSelectMode,
                                                                onLongClick = { selectedShiftIds = selectedShiftIds + shift.id },
                                                                onClick = {
                                                                    if (isSelectMode) {
                                                                        selectedShiftIds = if (shift.id in selectedShiftIds)
                                                                            selectedShiftIds - shift.id
                                                                        else
                                                                            selectedShiftIds + shift.id
                                                                    } else {
                                                                        onShiftClick(shift)
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        // Zona "..." para adicionar turno — sempre visível fora do modo seleção
                                        if (!isSelectMode) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(22.dp)
                                                    .clickable { onEmptyClick(user, day) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "···",
                                                    color = TxtGray.copy(alpha = 0.45f),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Linha vazia extra para facilitar adição de novos funcionários
                    val emptyCount = 1
                    items(emptyCount) { idx ->
                        val lineClr2 = Color.White.copy(alpha = 0.10f)
                        Row(
                            modifier = Modifier.fillMaxWidth().height(70.dp)
                                .border(1.dp, lineClr2, RoundedCornerShape(0.dp))
                        ) {
                            // Coluna esquerda: ícone de adicionar só na primeira linha vazia
                            Column(
                                modifier = Modifier.width(56.dp).fillMaxHeight().padding(4.dp)
                                    .let { m -> if (idx == 0) m.clickable { showAddMenu = true } else m },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (idx == 0) {
                                    Icon(Icons.Outlined.PersonAdd, null, tint = Blue, modifier = Modifier.size(22.dp))
                                }
                            }
                            // Células dos dias (vazias)
                            days.forEach { _ ->
                                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(lineClr2))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }

                    // Totais por dia
                    item {
                        Row(modifier = Modifier.fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(0.dp))
                            .padding(vertical = 6.dp)) {
                            Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                                val total = uiState.shifts
                                    .filter { it.published }
                                    .sumOf { calcDurationHours(it.resolvedStartTime(), it.resolvedEndTime()) }
                                Text("${total}h", color = TxtGray, fontSize = 10.sp)
                            }
                            days.forEach { day ->
                                val dayTotal = uiState.shifts
                                    .filter { it.published && it.date.startsWith(dateStr(day)) }
                                    .sumOf { calcDurationHours(it.resolvedStartTime(), it.resolvedEndTime()) }
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(if (dayTotal > 0) "${dayTotal}h" else "0h", color = TxtGray, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                }
            }
        }

        // Banner flutuante: publicar rascunhos
        if (draftCount > 0 && !isSelectMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DkSurface2)
                    .clickable { onPublishClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Blue, modifier = Modifier.size(18.dp))
                Text("Publicar $draftCount turnos rascunho",
                    color = Blue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(18.dp))
            }
        }

        // Barra de seleção múltipla
        if (isSelectMode) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(DkSurface2).padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { selectedShiftIds = emptySet() }) {
                        Text("Cancelar", color = TxtGray, fontSize = 14.sp)
                    }
                    Text(
                        "${selectedShiftIds.size} selecionado${if (selectedShiftIds.size != 1) "s" else ""}",
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedBadge),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Eliminar", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        // Diálogo confirmar eliminação múltipla
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = DkSurface,
                title = {
                    Text(
                        "Eliminar ${selectedShiftIds.size} turno${if (selectedShiftIds.size != 1) "s" else ""}?",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp
                    )
                },
                text = { Text("Esta ação não pode ser desfeita.", color = TxtGray, fontSize = 14.sp) },
                confirmButton = {
                    TextButton(onClick = {
                        val ids = selectedShiftIds.toList()
                        ids.forEach { id -> viewModel.deleteShift(id) { _, _ -> } }
                        selectedShiftIds = emptySet()
                        showDeleteConfirm = false
                    }) {
                        Text("ELIMINAR", color = RedBadge, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("CANCELAR", color = TxtGray)
                    }
                }
            )
        }

        // Menu do utilizador
        if (userMenuTarget != null) {
            val target = userMenuTarget!!
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                    .clickable { userMenuTarget = null },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(DkSurface).clickable {}
                        .padding(horizontal = 20.dp, vertical = 24.dp).padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        SchedAvatar(userInitials(target.name), userColor(target.id), userGradient(target.id), 40)
                        Column {
                            Text(target.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(categoryLabel(target.category), color = TxtGray, fontSize = 13.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
                            .clickable { userMenuTarget = null; onUserClick(target) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = TxtGray, modifier = Modifier.size(24.dp))
                        Text("Ver horario", color = Color.White, fontSize = 16.sp)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
                            .clickable {
                                val assignment = uiState.weekAssignments.find { it.userId == target.id }
                                userMenuTarget = null
                                if (assignment != null) {
                                    viewModel.removeUserFromWeek(assignment.id) { }
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Outlined.PersonRemove, null, tint = RedBadge, modifier = Modifier.size(24.dp))
                        Text("Remover da grelha", color = RedBadge, fontSize = 16.sp)
                    }
                }
            }
        }

        // Overlay adicionar funcionário
        if (showAddMenu) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showAddMenu = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(DkSurface).clickable {}
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
                ) {
                    Text("Adicionar funcionarios ao horario Padrao",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp))
                    if (userRole == "ADMIN") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { showAddMenu = false; onCriarFuncionario() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Outlined.PersonAdd, null, tint = TxtGray, modifier = Modifier.size(28.dp))
                            Text("Criar novo funcionario", color = Color.White, fontSize = 16.sp)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { showAddMenu = false; showSelectUser = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Outlined.Group, null, tint = TxtGray, modifier = Modifier.size(28.dp))
                        Text("Adicionar funcionario existente", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }

        // Overlay seleção de funcionário existente
        if (showSelectUser) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showSelectUser = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(DkSurface).clickable {}
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .heightIn(max = 480.dp)
                ) {
                    Text(
                        "Selecionar funcionario",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    if (availableUsers.isEmpty()) {
                        Text(
                            "Todos os funcionarios ja estao na grelha.",
                            color = TxtGray, fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(availableUsers) { user ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            showSelectUser = false
                                            viewModel.addUserToWeek(user.id, weekStr) { }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    SchedAvatar(userInitials(user.name), userColor(user.id), userGradient(user.id), 38)
                                    Column {
                                        Text(user.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Text(categoryLabel(user.category), color = TxtGray, fontSize = 12.sp)
                                    }
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Menu de opções (três pontos) ───────────────────────────────────────
        if (showOptionsMenu) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showOptionsMenu = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(DkSurface).clickable {}
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
                ) {
                    Text("Opcoes", color = Color.White, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            .clickable { /* Export - nao implementado */ showOptionsMenu = false },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Outlined.FileDownload, null, tint = TxtGray, modifier = Modifier.size(24.dp))
                        Text("Export Schedule", color = TxtGray, fontSize = 16.sp)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            .clickable {
                                showOptionsMenu = false
                                targetWeekForCopy = weekStart.plusWeeks(1)
                                showCopyShifts = true
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Text("Copy Shifts", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }

        // ── Copy Shifts — seletor de semana destino ────────────────────────────
        if (showCopyShifts) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showCopyShifts = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(DkSurface).clickable {}
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
                ) {
                    Text("Copiar turnos para", color = Color.White, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    Text("Semana de origem: ${weekRangeLabel(weekStart)}", color = TxtGray, fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DkSurface2)
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ChevronLeft, null, tint = Blue,
                            modifier = Modifier.size(28.dp).clickable {
                                targetWeekForCopy = targetWeekForCopy.minusWeeks(1)
                            })
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Semana destino", color = TxtGray, fontSize = 11.sp)
                            Text(weekRangeLabel(targetWeekForCopy), color = Color.White,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = Blue,
                            modifier = Modifier.size(28.dp).clickable {
                                targetWeekForCopy = targetWeekForCopy.plusWeeks(1)
                            })
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { showCopyShifts = false; showCopyConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Copy Shifts — confirmação ──────────────────────────────────────────
        if (showCopyConfirm) {
            AlertDialog(
                onDismissRequest = { showCopyConfirm = false },
                containerColor = DkSurface,
                title = {
                    Text("Copiar semana", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                },
                text = {
                    Text(
                        "Copiar todos os turnos de\n${weekRangeLabel(weekStart)}\npara\n${weekRangeLabel(targetWeekForCopy)}?",
                        color = TxtGray, fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showCopyConfirm = false
                        viewModel.copyWeek(targetWeekForCopy) { ok, msg -> onSuccess(msg) }
                    }) {
                        Text("Copiar", color = Blue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCopyConfirm = false }) {
                        Text("Cancelar", color = TxtGray)
                    }
                }
            )
        }
    }
}

// ── Célula de turno ───────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShiftCell(
    shift: Shift,
    isSelected: Boolean = false,
    isSelectMode: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val cellColor = shiftCellColor(shift.resolvedStartTime(), shift.published)
    val textAlpha = if (shift.published) 1f else 0.55f
    Box(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Blue.copy(alpha = 0.35f) else cellColor)
            .border(if (isSelected) 2.dp else 0.dp, if (isSelected) Blue else Color.Transparent, RoundedCornerShape(6.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "${shift.resolvedStartTime()}\n${shift.resolvedEndTime()}",
                color = Color.White.copy(alpha = textAlpha), fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                lineHeight = 13.sp, modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        if (isSelected) {
            Icon(
                Icons.Filled.CheckCircle, null,
                tint = Color.White,
                modifier = Modifier.size(12.dp).align(Alignment.BottomEnd)
            )
        } else if (!shift.published) {
            Icon(
                Icons.Outlined.Lock, null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(10.dp).align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun HatchedCell() {
    Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
        drawRect(Color(0xFF2A2A2A))
        val sp = 8.dp.toPx(); val sw = 4.dp.toPx(); var o = -size.height
        while (o < size.width + size.height) {
            drawLine(Color(0xFF1A1A1A), Offset(o, 0f), Offset(o + size.height, size.height), sw, StrokeCap.Butt)
            o += sp
        }
    }
}

// ── Ecrã: Detalhe do dia ──────────────────────────────────────────────────────
@Composable
private fun DayDetailScreen(
    date: LocalDate,
    uiState: SchedulerUiState,
    onBack: () -> Unit,
    onShiftClick: (Shift) -> Unit,
    onAddClick: () -> Unit
) {
    val dayShifts = uiState.shifts.filter { it.date.startsWith(dateStr(date)) }
    val uniqueUsers = dayShifts.map { it.userId }.distinct().size
    val totalH = dayShifts.sumOf {
        calcDuration(it.resolvedStartTime(), it.resolvedEndTime())
            .let { s -> val p = s.replace("h", "").replace("m","").trim().split(" ")
                (p.getOrNull(0)?.toIntOrNull() ?: 0) }
    }

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clip(CircleShape).clickable { onBack() }.padding(4.dp)) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(fullDayLabel(date), color = Color.White, fontSize = 16.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(Blue).clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("Turnos" to "${dayShifts.size}", "Funcionarios" to "$uniqueUsers", "Horas totais" to "${totalH}h").forEach { (l, v) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(l, color = TxtGray, fontSize = 13.sp)
                            Text(v, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (dayShifts.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A))
                        .padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("RASCUNHO", color = TxtGray, fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                    }
                }
                items(dayShifts) { shift ->
                    val user = uiState.users.find { it.id == shift.userId }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onShiftClick(shift) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SchedAvatar(
                            userInitials(user?.name ?: "?"),
                            userColor(shift.userId),
                            userGradient(shift.userId),
                            44
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(shift.resolvedName(), color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(user?.name ?: "Desconhecido", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("${shift.resolvedStartTime()} - ${shift.resolvedEndTime()}", color = TxtGray, fontSize = 13.sp)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(18.dp))
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("Sem turnos para este dia", color = TxtGray, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ── Ecrã: Detalhe do turno ────────────────────────────────────────────────────
@Composable
private fun ShiftDetailScreen(
    shift: Shift,
    uiState: SchedulerUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (Int) -> Unit,
    onPublish: (String) -> Unit,
    onPublishShift: (Int, (Boolean, String) -> Unit) -> Unit
) {
    var showNotifySheet by remember { mutableStateOf(false) }
    var showDotsMenu    by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val user = uiState.users.find { it.id == shift.userId }
    val userName = user?.name ?: shift.user?.name ?: "Desconhecido"

    Box(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.clip(CircleShape).clickable { onBack() }.padding(4.dp)) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(DkSurface2)
                        .clickable { showDotsMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
                item {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        SchedAvatar(userInitials(userName), userColor(shift.userId), userGradient(shift.userId), 80)
                        Text(userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
                    }
                }
                item {
                    Row(modifier = Modifier.padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Outlined.DateRange, null, tint = TxtGray, modifier = Modifier.size(22.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(shift.resolvedStartTime(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                Icon(Icons.Outlined.ArrowForward, null, tint = TxtGray, modifier = Modifier.size(20.dp))
                                Text(shift.resolvedEndTime(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Text(shift.date, color = TxtGray, fontSize = 14.sp)
                        }
                    }
                    HDiv()
                }
                item {
                    listOf(
                        Triple("Tipo de turno",   shift.resolvedName(), null),
                        Triple("Pausa nao paga",  "Nenhuma",            null),
                        Triple("Total",           calcDuration(shift.resolvedStartTime(), shift.resolvedEndTime()), null),
                    ).forEach { (label, value, dot) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(label, color = Color.White, fontSize = 15.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                dot?.let { Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color = it)) }
                                Text(value, color = TxtGray, fontSize = 15.sp)
                            }
                        }
                        HDiv()
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.LocationOn, null, tint = TxtGray, modifier = Modifier.size(18.dp))
                        Text("Local de trabalho", color = TxtGray, fontSize = 15.sp)
                        Text("Nenhum", color = TxtGray, fontSize = 15.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    HDiv()
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            if (shift.published) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                            null,
                            tint = if (shift.published) DkGreen else Orange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            if (shift.published) "Publicado" else "Rascunho",
                            color = if (shift.published) DkGreen else Orange,
                            fontSize = 15.sp
                        )
                    }
                    HDiv()
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Assignment, null, tint = TxtGray, modifier = Modifier.size(18.dp))
                        Text("Listas de Tarefas", color = Color.White, fontSize = 15.sp)
                        Text("Nenhuma (0/0)", color = TxtGray, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(16.dp))
                    }
                    HDiv()
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Comment, null, tint = TxtGray, modifier = Modifier.size(18.dp))
                        Text("Comentarios", color = Color.White, fontSize = 15.sp)
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            Text("0", color = TxtGray, fontSize = 15.sp)
                            Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(16.dp))
                        }
                    }
                    HDiv()
                }
                item {
                    Spacer(Modifier.height(20.dp))
                    if (!shift.published) {
                        Button(
                            onClick = { showNotifySheet = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue)
                        ) {
                            Text("Publicar turno", color = Color.White, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DkSurface2)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CheckCircle, null, tint = DkGreen, modifier = Modifier.size(18.dp))
                                Text("Turno publicado", color = DkGreen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }

        // Menu três pontos
        if (showDotsMenu) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
                .clickable { showDotsMenu = false }, contentAlignment = Alignment.TopEnd) {
                Column(modifier = Modifier.padding(top = 56.dp, end = 16.dp)
                    .clip(RoundedCornerShape(14.dp)).background(DkSurface2).clickable {}.width(200.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { showDotsMenu = false; onEdit() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("Editar turno", color = Color.White, fontSize = 15.sp)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { showDotsMenu = false; showDeleteDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Delete, null, tint = RedBadge, modifier = Modifier.size(18.dp))
                        Text("Eliminar turno", color = RedBadge, fontSize = 15.sp)
                    }
                }
            }
        }

        // Diálogo confirmar eliminação
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = DkSurface,
                title = { Text("Eliminar turno", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Tem a certeza que quer eliminar este turno?", color = TxtGray) },
                confirmButton = {
                    TextButton(onClick = { showDeleteDialog = false; onDelete(shift.id) }) {
                        Text("Eliminar", color = RedBadge, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = TxtGray) }
                }
            )
        }

        // Bottom sheet de notificação
        if (showNotifySheet) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                .clickable { showNotifySheet = false }, contentAlignment = Alignment.BottomCenter) {
                Column(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(DkSurface).clickable {}
                    .padding(horizontal = 20.dp, vertical = 24.dp).padding(bottom = 20.dp)) {
                    Text("Publicar turno", color = Color.White, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 20.dp))
                    listOf(
                        "Notificar funcionario" to Icons.Outlined.Notifications,
                        "Nao notificar"         to Icons.Outlined.NotificationsOff,
                    ).forEach { (option, icon) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    showNotifySheet = false
                                    onPublishShift(shift.id) { _, msg -> onPublish(msg) }
                                }
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = Blue, modifier = Modifier.size(24.dp))
                            Text(option, color = Color.White, fontSize = 15.sp)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    }
                }
            }
        }
    }
}

// ── Ecrã: Editar turno ────────────────────────────────────────────────────────
@Composable
private fun EditarTurnoScreen(
    shift: Shift,
    uiState: SchedulerUiState,
    onBack: () -> Unit,
    onSuccess: (String, String, String?) -> Unit
) {
    val initStart = shift.resolvedStartTime()
    val initEnd   = shift.resolvedEndTime()
    var startHour   by remember { mutableStateOf(initStart.split(":")[0].toIntOrNull() ?: 8) }
    var startMinute by remember { mutableStateOf(initStart.split(":").getOrNull(1)?.toIntOrNull() ?: 0) }
    var endHour     by remember { mutableStateOf(initEnd.split(":")[0].toIntOrNull() ?: 20) }
    var endMinute   by remember { mutableStateOf(initEnd.split(":").getOrNull(1)?.toIntOrNull() ?: 0) }
    var startExpanded by remember { mutableStateOf(false) }
    var endExpanded   by remember { mutableStateOf(false) }

    val user = uiState.users.find { it.id == shift.userId }
    val startStr = "%02d:%02d".format(startHour, startMinute)
    val endStr   = "%02d:%02d".format(endHour, endMinute)
    val duration = calcDuration(startStr, endStr)

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Cancelar", color = Blue, fontSize = 16.sp) }
            Text("Editar Turno", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { onSuccess(startStr, endStr, null) }) {
                Text("Guardar", color = Blue, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                FormCard(modifier = Modifier.padding(bottom = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Funcionario", color = Color.White, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SchedAvatar(userInitials(user?.name ?: "?"), userColor(shift.userId), null, 24)
                            Text(user?.name ?: "Desconhecido", color = TxtGray, fontSize = 15.sp)
                        }
                    }
                }
            }
            item {
                FormCard(modifier = Modifier.padding(bottom = 16.dp)) {
                    FormRow("Data", shift.date.take(10))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    TimePickerRow(
                        label = "Hora de inicio",
                        hour = startHour, minute = startMinute,
                        expanded = startExpanded,
                        onToggle = { startExpanded = !startExpanded; if (startExpanded) endExpanded = false },
                        onHourChange = { startHour = it },
                        onMinuteChange = { startMinute = it }
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    TimePickerRow(
                        label = "Hora de fim",
                        hour = endHour, minute = endMinute,
                        expanded = endExpanded,
                        onToggle = { endExpanded = !endExpanded; if (endExpanded) startExpanded = false },
                        onHourChange = { endHour = it },
                        onMinuteChange = { endMinute = it }
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.CenterEnd) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(DkSurface2).padding(horizontal = 14.dp, vertical = 6.dp)) {
                            Text(duration, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

// ── Ecrã: Publicar horário ────────────────────────────────────────────────────
@Composable
private fun PublicarHorarioScreen(
    uiState: SchedulerUiState,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit,
    onPublishMultiple: (List<Int>, (Boolean, String) -> Unit) -> Unit
) {
    var notifyOption by remember { mutableStateOf(0) }
    val drafts = uiState.shifts.filter { !it.published }
    val selected = remember(drafts.size) {
        mutableStateListOf(*Array(drafts.size) { true })
    }

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Cancelar", color = Blue, fontSize = 16.sp) }
            Text("Publicar Horario", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(70.dp))
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                Text("NOTIFICACAO", color = TxtGray, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 10.dp))
                Column(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(DkSurface)) {
                    listOf(
                        "Notificar funcionarios",
                        "Nao notificar"
                    ).forEachIndexed { i, opt ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { notifyOption = i }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(opt, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            RadioDot(checked = notifyOption == i)
                        }
                        if (i < 2) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("RASCUNHOS (${selected.count { it }})", color = TxtGray, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Text(
                        if (selected.all { it }) "Desselecionar tudo" else "Selecionar tudo",
                        color = Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { val all = selected.all { it }; selected.indices.forEach { selected[it] = !all } }
                    )
                }
            }
            if (drafts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("Sem rascunhos para publicar", color = TxtGray, fontSize = 15.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            items(drafts.size) { i ->
                val shift = drafts[i]
                val user = uiState.users.find { it.id == shift.userId }
                val topShape = if (i == 0) RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp) else RoundedCornerShape(0.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().clip(topShape).background(DkSurface)
                        .clickable { selected[i] = !selected[i] }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = selected[i], onCheckedChange = { selected[i] = it },
                        colors = CheckboxDefaults.colors(checkedColor = Blue, uncheckedColor = TxtGray))
                    SchedAvatar(userInitials(user?.name ?: "?"), userColor(shift.userId), null, 36)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(shift.date, color = TxtGray, fontSize = 12.sp)
                        Text(user?.name ?: "Desconhecido", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${shift.resolvedStartTime()} - ${shift.resolvedEndTime()} • ${shift.resolvedName()}", color = TxtGray, fontSize = 12.sp)
                    }
                }
                if (i < drafts.lastIndex) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
            }
            item {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)).background(DkSurface).height(8.dp))
                Spacer(Modifier.height(16.dp))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().background(DkBg).padding(horizontal = 16.dp, vertical = 12.dp)) {
            val selCount = selected.count { it }
            Button(
                onClick = {
                    val ids = drafts.indices.filter { selected[it] }.map { drafts[it].id }
                    onPublishMultiple(ids) { _, msg -> onSuccess(msg) }
                },
                enabled = selCount > 0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                Text("Publicar $selCount turnos", color = Color.White,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

// ── Ecrã: Adicionar turno ─────────────────────────────────────────────────────
@Composable
private fun AddShiftScreen(
    user: User?,
    date: LocalDate,
    uiState: SchedulerUiState,
    onBack: () -> Unit,
    onSuccess: (Int, String, String, String) -> Unit
) {
    var selectedUser    by remember { mutableStateOf(user) }
    var showUserPicker  by remember { mutableStateOf(false) }
    var startHour       by remember { mutableStateOf(8) }
    var startMinute     by remember { mutableStateOf(0) }
    var endHour         by remember { mutableStateOf(20) }
    var endMinute       by remember { mutableStateOf(0) }
    var startExpanded   by remember { mutableStateOf(false) }
    var endExpanded     by remember { mutableStateOf(false) }

    val startStr = "%02d:%02d".format(startHour, startMinute)
    val endStr   = "%02d:%02d".format(endHour, endMinute)
    val duration = calcDuration(startStr, endStr)

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Cancelar", color = Blue, fontSize = 16.sp) }
            Text("Novo Turno", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            TextButton(
                onClick = {
                    val uid = selectedUser?.id
                    if (uid != null) onSuccess(uid, startStr, endStr, dateStr(date))
                },
                enabled = selectedUser != null
            ) {
                Text("Adicionar", color = if (selectedUser != null) Blue else TxtGray,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                FormCard(modifier = Modifier.padding(bottom = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { showUserPicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Funcionario", color = Color.White, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedUser != null) {
                                SchedAvatar(userInitials(selectedUser!!.name), userColor(selectedUser!!.id), null, 24)
                                Text(selectedUser!!.name, color = TxtGray, fontSize = 15.sp)
                            } else {
                                Text("Selecionar...", color = TxtGray, fontSize = 15.sp)
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            item {
                FormCard(modifier = Modifier.padding(bottom = 16.dp)) {
                    FormRow("Data", dateStr(date))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    TimePickerRow(
                        label = "Hora de inicio",
                        hour = startHour, minute = startMinute,
                        expanded = startExpanded,
                        onToggle = { startExpanded = !startExpanded; if (startExpanded) endExpanded = false },
                        onHourChange = { startHour = it },
                        onMinuteChange = { startMinute = it }
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    TimePickerRow(
                        label = "Hora de fim",
                        hour = endHour, minute = endMinute,
                        expanded = endExpanded,
                        onToggle = { endExpanded = !endExpanded; if (endExpanded) startExpanded = false },
                        onHourChange = { endHour = it },
                        onMinuteChange = { endMinute = it }
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.CenterEnd) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(DkSurface2).padding(horizontal = 14.dp, vertical = 6.dp)) {
                            Text(duration, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    // Picker de funcionário
    if (showUserPicker) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
            .clickable { showUserPicker = false }, contentAlignment = Alignment.BottomCenter) {
            Column(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(DkSurface).clickable {}
                .padding(horizontal = 20.dp, vertical = 24.dp).padding(bottom = 20.dp)) {
                Text("Selecionar funcionario", color = Color.White, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                uiState.users.forEach { u ->
                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { selectedUser = u; showUserPicker = false }
                        .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        SchedAvatar(userInitials(u.name), userColor(u.id), userGradient(u.id), 40)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(u.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(categoryLabel(u.category), color = TxtGray, fontSize = 13.sp)
                        }
                        if (selectedUser?.id == u.id)
                            Icon(Icons.Filled.Check, null, tint = Blue, modifier = Modifier.size(20.dp))
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                }
            }
        }
    }
}

// ── Ecrã: Filtrar ─────────────────────────────────────────────────────────────
@Composable
private fun FilterScreen(onBack: () -> Unit, onPositionClick: () -> Unit, onSuccess: () -> Unit) {
    val statusSel = remember { mutableStateListOf(*Array(STATUS_FILTERS.size) { false }) }
    var warnSel by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Cancelar", color = Blue, fontSize = 16.sp) }
            Text("Filtrar", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { statusSel.indices.forEach { statusSel[it] = false }; warnSel = false }) {
                Text("Repor", color = Blue, fontSize = 16.sp)
            }
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                Text("POSICAO", color = TxtGray, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 10.dp))
                Column(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(DkSurface)) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { onPositionClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Posicoes", color = Color.White, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Todos", color = TxtGray, fontSize = 15.sp)
                            Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("ESTADO", color = TxtGray, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 10.dp))
                Column(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(DkSurface)) {
                    STATUS_FILTERS.forEachIndexed { i, (label, icon, color) ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { statusSel[i] = !statusSel[i] }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                                Text(label, color = Color.White, fontSize = 15.sp)
                            }
                            RadioDot(checked = statusSel[i])
                        }
                        if (i < STATUS_FILTERS.lastIndex) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("AVISOS", color = TxtGray, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 10.dp))
                Column(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(DkSurface)) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { warnSel = !warnSel }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Icon(Icons.Outlined.Warning, null, tint = Orange, modifier = Modifier.size(22.dp))
                            Text("Tem avisos", color = Color.White, fontSize = 15.sp)
                        }
                        RadioDot(checked = warnSel)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().background(DkBg).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Button(onClick = onSuccess, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                Text("Aplicar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

// ── Ecrã: Filtrar por categoria ───────────────────────────────────────────────
@Composable
private fun PositionFilterScreen(uiState: SchedulerUiState, onBack: () -> Unit) {
    val categories = listOf("VETERINARIAN","NURSE","OPERATIONAL","ADMINISTRATIVE")
    var allSelected by remember { mutableStateOf(true) }
    val catSel = remember { mutableStateListOf(*Array(categories.size) { false }) }
    val catColors = listOf(Color(0xFF27AE60), Color(0xFF2980B9), Color(0xFFE67E22), Color(0xFF8E44AD))

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Cancelar", color = Blue, fontSize = 16.sp) }
            Text("Categoria", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(70.dp))
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Column(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(DkSurface)) {
                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { allSelected = true; catSel.indices.forEach { catSel[it] = false } }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Todos", color = Color.White, fontSize = 15.sp)
                        RadioDot(checked = allSelected)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("CATEGORIAS", color = TxtGray, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 10.dp))
                Column(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(DkSurface)) {
                    categories.forEachIndexed { i, cat ->
                        Row(modifier = Modifier.fillMaxWidth()
                            .clickable { allSelected = false; catSel[i] = !catSel[i] }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(catColors[i]))
                                Text(categoryLabel(cat), color = Color.White, fontSize = 15.sp)
                            }
                            RadioDot(checked = catSel[i])
                        }
                        if (i < categories.lastIndex) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    }
                }
            }
        }
    }
}

// ── Ecrã: Horário do funcionário ──────────────────────────────────────────────
@Composable
private fun UserScheduleScreen(
    user: User,
    uiState: SchedulerUiState,
    weekStart: LocalDate,
    onBack: () -> Unit,
    onShiftClick: (Shift) -> Unit,
    onPreviousWeek: (() -> Unit)? = null,
    onNextWeek: (() -> Unit)? = null
) {
    val userShifts = uiState.shifts
        .filter { it.userId == user.id }
        .sortedBy { it.date }
    val totalHours = userShifts.sumOf { calcDurationHours(it.resolvedStartTime(), it.resolvedEndTime()) }
    val today = LocalDate.now()

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        // Cabeçalho
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clip(CircleShape).clickable { onBack() }.padding(4.dp)) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(8.dp))
            SchedAvatar(userInitials(user.name), userColor(user.id), userGradient(user.id), 32)
            Spacer(Modifier.width(10.dp))
            Text(user.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
        }

        // Subtítulo
        Text(
            "Todos os turnos da semana",
            color = TxtGray, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (onPreviousWeek != null) {
                Icon(Icons.Filled.ChevronLeft, null, tint = Blue,
                    modifier = Modifier.size(26.dp).clickable { onPreviousWeek() })
            } else { Spacer(Modifier.size(26.dp)) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(weekRangeLabel(weekStart), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("•", color = TxtGray, fontSize = 15.sp)
                Text("${totalHours}h", color = Blue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            if (onNextWeek != null) {
                Icon(Icons.Filled.ChevronRight, null, tint = Blue,
                    modifier = Modifier.size(26.dp).clickable { onNextWeek() })
            } else { Spacer(Modifier.size(26.dp)) }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))

        if (userShifts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sem turnos esta semana", color = TxtGray, fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(userShifts) { shift ->
                    val shiftDate = try {
                        LocalDate.parse(shift.date.take(10))
                    } catch (e: Exception) { weekStart }
                    val isToday = shiftDate == today
                    val accentColor = shiftCellColor(shift.resolvedStartTime(), true)
                    val hours = calcDurationHours(shift.resolvedStartTime(), shift.resolvedEndTime())

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coluna da data
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(40.dp)
                        ) {
                            Text(
                                dayNum(shiftDate),
                                color = if (isToday) Blue else Color.White,
                                fontSize = 22.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                dayAbbr(shiftDate),
                                color = if (isToday) Blue else TxtGray,
                                fontSize = 12.sp
                            )
                        }

                        // Cartão do turno
                        Row(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DkSurface)
                                .clickable { onShiftClick(shift) }
                        ) {
                            // Barra colorida lateral
                            Box(modifier = Modifier.width(4.dp).height(80.dp).background(accentColor))
                            Column(
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    categoryLabel(user.category),
                                    color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${shift.resolvedStartTime()} - ${shift.resolvedEndTime()}",
                                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                                )
                                Text("${hours}h", color = TxtGray, fontSize = 12.sp)
                            }
                            // Badge estado
                            Box(
                                modifier = Modifier.padding(end = 12.dp).align(Alignment.CenterVertically)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (shift.published) Blue.copy(alpha = 0.15f) else DkSurface2)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        if (shift.published) "Publicado" else "Rascunho",
                                        color = if (shift.published) Blue else TxtGray,
                                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// ── BottomNav ─────────────────────────────────────────────────────────────────
@Composable
private fun SchedBottomNav(
    onHomeClick: () -> Unit,
    onNotificationsClick: () -> Unit = {}
) {
    val items  = listOf("Inicio" to Icons.Outlined.Home, "Agenda" to Icons.Outlined.DateRange, "Caixa" to Icons.Outlined.Inbox, "Notificacoes" to Icons.Outlined.Notifications, "Menu" to Icons.Outlined.Menu)
    val badges = listOf(0, 0, 0, 0, 0)
    Row(
        modifier = Modifier.fillMaxWidth().background(DkSurface)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(0.dp))
            .pointerInput(Unit) {}
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items.forEachIndexed { i, (label, icon) ->
            val active = i == 1
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clickable {
                        when (i) {
                            0 -> onHomeClick()
                            3 -> onNotificationsClick()
                            else -> {}
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Box {
                    Icon(icon, contentDescription = label, tint = if (active) Blue else TxtGray, modifier = Modifier.size(22.dp))
                    if (badges[i] > 0) {
                        Box(modifier = Modifier.size(17.dp).clip(CircleShape).background(RedBadge)
                            .align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                            contentAlignment = Alignment.Center) {
                            Text("${badges[i]}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Text(label, fontSize = 10.sp, color = if (active) Blue else TxtGray,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

// ── Componentes partilhados ───────────────────────────────────────────────────
@Composable
private fun SchedAvatar(initials: String, color: Color?, gradient: Brush?, size: Int) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).let {
            if (gradient != null) it.background(gradient) else it.background(color ?: TxtGray)
        },
        contentAlignment = Alignment.Center
    ) {
        Text(initials.take(2), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size * 0.3).sp)
    }
}

@Composable
private fun RadioDot(checked: Boolean) {
    Box(
        modifier = Modifier.size(22.dp).clip(CircleShape)
            .border(1.5.dp, if (checked) Blue else Color(0xFF555555), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Blue))
    }
}

@Composable
private fun FormCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DkSurface), content = content)
}

@Composable
private fun FormRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp)
        Text(value, color = Blue, fontSize = 15.sp)
    }
}

@Composable
private fun FormRowClickable(label: String, value: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, color = Blue, fontSize = 15.sp)
            Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun HDiv() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
}

// ── Ecrã: Criar funcionário (ADMIN only) ─────────────────────────────────────
@Composable
private fun CriarFuncionarioScreen(
    onBack: () -> Unit,
    onCreateUser: (CreateUserRequest, (Boolean, String) -> Unit) -> Unit
) {
    var name      by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var empNumber by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var role      by remember { mutableStateOf("EMPLOYEE") }
    var category  by remember { mutableStateOf("NURSE") }
    var showRolePicker     by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    val roles      = listOf("EMPLOYEE" to "Funcionario", "MANAGER" to "Gestor", "ADMIN" to "Administrador")
    val categories = listOf(
        "NURSE"          to "Enfermeiro",
        "VETERINARIAN"   to "Veterinario",
        "OPERATIONAL"    to "Operacional",
        "ADMINISTRATIVE" to "Administrativo"
    )

    val emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val isValid = name.isNotBlank() && emailValid && empNumber.isNotBlank() && password.length >= 6

    Column(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Cancelar", color = Blue, fontSize = 16.sp) }
            Text("Novo Funcionario", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            TextButton(
                onClick = {
                    errorMsg = null
                    onCreateUser(CreateUserRequest(
                        name = name.trim(), email = email.trim().lowercase(),
                        employeeNumber = empNumber.trim(), role = role,
                        category = category, password = password
                    )) { _, msg -> errorMsg = msg }
                },
                enabled = isValid
            ) {
                Text("Criar", color = if (isValid) Blue else Color(0xFF3A3A3C), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                if (errorMsg != null) {
                    Text(
                        errorMsg!!,
                        color = RedBadge,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(RedBadge.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Text("DADOS PESSOAIS", color = TxtGray, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 10.dp))
                FormCard(modifier = Modifier.padding(bottom = 16.dp)) {
                    CriarFormField("Nome", name, "Nome completo") { name = it }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    CriarFormField("Email", email, "email@clinica.com") { email = it }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    CriarFormField("Numero de funcionario", empNumber, "EMP007") { empNumber = it }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    CriarFormField("Password", password, "Min. 6 caracteres", isPassword = true) { password = it }
                }
                Text("FUNCAO", color = TxtGray, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 10.dp))
                FormCard(modifier = Modifier.padding(bottom = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { showRolePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Cargo", color = Color.White, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(roles.find { it.first == role }?.second ?: role, color = Blue, fontSize = 15.sp)
                            Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(16.dp))
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    Row(modifier = Modifier.fillMaxWidth().clickable { showCategoryPicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Categoria", color = Color.White, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(categories.find { it.first == category }?.second ?: category, color = Blue, fontSize = 15.sp)
                            Icon(Icons.Outlined.ChevronRight, null, tint = TxtGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showRolePicker) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
            .clickable { showRolePicker = false }, contentAlignment = Alignment.BottomCenter) {
            Column(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(DkSurface).clickable {}
                .padding(horizontal = 20.dp, vertical = 24.dp).padding(bottom = 20.dp)) {
                Text("Selecionar cargo", color = Color.White, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                roles.forEach { (value, label) ->
                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { role = value; showRolePicker = false }
                        .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = Color.White, fontSize = 15.sp)
                        if (role == value) Icon(Icons.Filled.Check, null, tint = Blue, modifier = Modifier.size(20.dp))
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                }
            }
        }
    }

    if (showCategoryPicker) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
            .clickable { showCategoryPicker = false }, contentAlignment = Alignment.BottomCenter) {
            Column(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(DkSurface).clickable {}
                .padding(horizontal = 20.dp, vertical = 24.dp).padding(bottom = 20.dp)) {
                Text("Selecionar categoria", color = Color.White, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                categories.forEach { (value, label) ->
                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { category = value; showCategoryPicker = false }
                        .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = Color.White, fontSize = 15.sp)
                        if (category == value) Icon(Icons.Filled.Check, null, tint = Blue, modifier = Modifier.size(20.dp))
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                }
            }
        }
    }
}

@Composable
private fun CriarFormField(
    label: String,
    value: String,
    placeholder: String,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(0.4f))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (isPassword)
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            else
                androidx.compose.ui.text.input.VisualTransformation.None,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Blue, fontSize = 15.sp,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.weight(0.6f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) Text(placeholder, color = Color(0xFF48484A), fontSize = 15.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    inner()
                }
            }
        )
    }
}

@Composable
private fun BreakPickerDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val options = listOf(0, 15, 30, 45, 60, 90)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DkSurface,
        title = { Text("Pausa nao paga", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEachIndexed { i, opt ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onConfirm(opt); onDismiss() }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (opt == 0) "Nenhuma" else "$opt minutos", color = Color.White, fontSize = 15.sp)
                        RadioDot(checked = current == opt)
                    }
                    if (i < options.lastIndex) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TxtGray) } }
    )
}
