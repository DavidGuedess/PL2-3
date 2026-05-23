package pt.ualg.miaugenda.ui.screen.team

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.MiauGendaApp
import pt.ualg.miaugenda.data.model.CreateUserRequest
import pt.ualg.miaugenda.data.model.TimeOffRequest
import pt.ualg.miaugenda.data.model.UpdateUserRequest
import pt.ualg.miaugenda.ui.components.AppBottomNav
import pt.ualg.miaugenda.ui.components.NavTab
import pt.ualg.miaugenda.data.model.Shift
import pt.ualg.miaugenda.data.model.User
import pt.ualg.miaugenda.data.remote.RetrofitClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DkBg      = Color(0xFF000000)
private val DkSurface = Color(0xFF1C1C1E)
private val DkSurface2= Color(0xFF2C2C2E)
private val Blue      = Color(0xFF2979FF)
private val TxtGray   = Color(0xFF8E8E93)
private val DkGreen   = Color(0xFF2ECC71)
private val DkDivider = Color(0xFF2C2C2E)

// paleta de cores para os cards de funcionário (igual à imagem 60)
private val cardColors = listOf(
    Color(0xFF4A90D9), Color(0xFFE8786B), Color(0xFF5BBF8E),
    Color(0xFF4EC6C6), Color(0xFFE8C840), Color(0xFFB080D0),
    Color(0xFFE87830), Color(0xFF6080C8), Color(0xFF60B060)
)

private sealed class TeamSubScreen {
    object List : TeamSubScreen()
    data class Detail(val user: User) : TeamSubScreen()
    data class Calendar(val user: User) : TeamSubScreen()
    data class Ferias(val user: User) : TeamSubScreen()
    data class EditEmployee(val user: User) : TeamSubScreen()
    object AddEmployee : TeamSubScreen()
}

@Composable
fun TeamScreen(
    onBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToScheduler: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onMessageClick: (Int) -> Unit = {}
) {
    var subScreen by remember { mutableStateOf<TeamSubScreen>(TeamSubScreen.List) }

    BackHandler(enabled = subScreen !is TeamSubScreen.List) {
        subScreen = when (val s = subScreen) {
            is TeamSubScreen.Calendar    -> TeamSubScreen.Detail(s.user)
            is TeamSubScreen.Ferias      -> TeamSubScreen.Detail(s.user)
            is TeamSubScreen.EditEmployee -> TeamSubScreen.Detail(s.user)
            else -> TeamSubScreen.List
        }
    }

    when (val s = subScreen) {
        is TeamSubScreen.List -> TeamListScreen(
            onEmployeeClick = { subScreen = TeamSubScreen.Detail(it) },
            onAddEmployee = { subScreen = TeamSubScreen.AddEmployee },
            onNavEquipa = { subScreen = TeamSubScreen.List },
            onNavigateToHome = onNavigateToHome,
            onNavigateToScheduler = onNavigateToScheduler,
            onNavigateToInbox = onNavigateToInbox,
            onNavigateToNotifications = onNavigateToNotifications
        )
        is TeamSubScreen.Detail -> EmployeeDetailScreen(
            user = s.user,
            onBack = { subScreen = TeamSubScreen.List },
            onCalendarClick = { subScreen = TeamSubScreen.Calendar(s.user) },
            onFeriasClick   = { subScreen = TeamSubScreen.Ferias(s.user) },
            onEditClick     = { subScreen = TeamSubScreen.EditEmployee(s.user) },
            onMessageClick  = { onMessageClick(s.user.id) }
        )
        is TeamSubScreen.Calendar -> EmployeeCalendarScreen(
            user = s.user,
            onBack = { subScreen = TeamSubScreen.Detail(s.user) }
        )
        is TeamSubScreen.Ferias -> EmployeeVacationsScreen(
            user = s.user,
            onBack = { subScreen = TeamSubScreen.Detail(s.user) }
        )
        is TeamSubScreen.EditEmployee -> EditEmployeeScreen(
            user = s.user,
            onBack = { subScreen = TeamSubScreen.Detail(s.user) },
            onSaved = { subScreen = TeamSubScreen.List }
        )
        is TeamSubScreen.AddEmployee -> AddEmployeeScreen(
            onBack = { subScreen = TeamSubScreen.List },
            onSaved = { subScreen = TeamSubScreen.List }
        )
    }
}

// ─── Ecrã da lista de funcionários ────────────────────────────────────────────

@Composable
private fun TeamListScreen(
    onEmployeeClick: (User) -> Unit,
    onAddEmployee: () -> Unit,
    onNavEquipa: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToScheduler: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentRole = MiauGendaApp.getTokenManager(context).getUserRole() ?: "EMPLOYEE"
    val canAddEmployee = currentRole == "ADMIN"

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val resp = RetrofitClient.userApi.getUsers()
            if (resp.isSuccessful) users = resp.body() ?: emptyList()
        } catch (_: Exception) {}
        isLoading = false
    }

    val filtered = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // agrupar em linhas de 3
    val rows = filtered.chunked(3)

    Box(modifier = Modifier.fillMaxSize().background(DkBg).systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra de topo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "A Minha Equipa",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (canAddEmployee) {
                    IconButton(onClick = onAddEmployee) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar funcionário", tint = Color.White)
                    }
                }
            }

            // Barra de pesquisa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DkSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(Blue),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) Text("Pesquisar", color = TxtGray, fontSize = 15.sp)
                            inner()
                        }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Blue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rows) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { user ->
                                val colorIdx = users.indexOf(user) % cardColors.size
                                EmployeeCard(
                                    user = user,
                                    cardColor = cardColors[colorIdx],
                                    modifier = Modifier.weight(1f),
                                    onClick = { onEmployeeClick(user) }
                                )
                            }
                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            // Barra de navegação em baixo
            AppBottomNav(
                active               = NavTab.MENU,
                onHomeClick          = onNavigateToHome,
                onSchedulerClick     = onNavigateToScheduler,
                onInboxClick         = onNavigateToInbox,
                onNotificationsClick = onNavigateToNotifications,
                onMenuClick          = onNavEquipa
            )
        }
    }
}

@Composable
private fun EmployeeCard(
    user: User,
    cardColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        if (user.active) cardColor else Color(0xFF555555),
                        if (user.active) cardColor.copy(alpha = 0.7f) else Color(0xFF333333)
                    )
                )
            )
            .clickable(onClick = onClick)
    ) {
        // Iniciais centradas
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = user.name.split(" ").filter { it.isNotEmpty() }.take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() },
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // Nome em baixo
        Text(
            text = user.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )
        // Indicador de inativo
        if (!user.active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF453A))
            )
        }
    }
}

// ─── Detalhe de funcionário ────────────────────────────────────────────────────

@Composable
private fun EmployeeDetailScreen(
    user: User,
    onBack: () -> Unit,
    onCalendarClick: () -> Unit,
    onFeriasClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onMessageClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentRole = MiauGendaApp.getTokenManager(context).getUserRole() ?: "EMPLOYEE"
    val isAdmin = currentRole == "ADMIN"
    val isManager = currentRole == "MANAGER"
    val isEmployee = currentRole == "EMPLOYEE"
    var isActive by remember { mutableStateOf(user.active) }
    var isActionLoading by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Header com gradiente + nome
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A6B8A), Color(0xFF0D3D52))
                        )
                    )
            ) {
                // Avatar grande
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Blue, Color(0xFFE91E8C)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.split(" ").filter { it.isNotEmpty() }.take(2)
                                .joinToString("") { it.first().uppercaseChar().toString() },
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Botão voltar
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.statusBarsPadding().padding(4.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                // Nome sobreposto em baixo
                Text(
                    text = user.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp)
                )
            }

            // Botões de ação
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DkSurface)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(Icons.Outlined.CalendarMonth, "Calendário", onCalendarClick)
                if (isAdmin || isManager) {
                    ActionButton(Icons.Outlined.BeachAccess, "Férias", onFeriasClick)
                }
                ActionButton(Icons.Outlined.Message, "Mensagem", onMessageClick)
                if (isAdmin) {
                    ActionButton(Icons.Outlined.Edit, "Editar", onEditClick)
                }
            }

            // Campos de info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                InfoRow("Nome", user.name)
                HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                InfoRow("Nº Funcionário", user.employeeNumber)
                if (!isEmployee) {
                    HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    InfoRow("Email", user.email)
                    HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    InfoRow("Contacto", user.contact ?: "Não adicionado")
                }
                HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                InfoRow("Categoria", user.category.lowercase().replaceFirstChar { it.uppercase() })
                HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                InfoRow("Função", user.role.lowercase().replaceFirstChar { it.uppercase() })
                HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                InfoRow("Estado", if (isActive) "Ativo" else "Inativo")
            }

            // Botão desativar/ativar (apenas ADMIN)
            if (isAdmin) {
                Spacer(modifier = Modifier.height(16.dp))
                actionMessage?.let {
                    Text(
                        text = it,
                        color = if (it.startsWith("Erro")) Color(0xFFFF453A) else DkGreen,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        scope.launch {
                            isActionLoading = true
                            actionMessage = null
                            try {
                                val resp = if (isActive) {
                                    RetrofitClient.userApi.deactivateUser(user.id)
                                } else {
                                    RetrofitClient.userApi.activateUser(user.id)
                                }
                                if (resp.isSuccessful) {
                                    isActive = !isActive
                                    actionMessage = if (isActive) "Utilizador reativado" else "Utilizador desativado"
                                } else {
                                    actionMessage = "Erro (${resp.code()})"
                                }
                            } catch (e: Exception) {
                                actionMessage = "Erro: ${e.message}"
                            } finally {
                                isActionLoading = false
                            }
                        }
                    },
                    enabled = !isActionLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) Color(0xFFFF453A) else DkGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isActionLoading) "A processar..." else if (isActive) "Desativar Conta" else "Reativar Conta",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Blue, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DkBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TxtGray, fontSize = 15.sp)
        Text(
            value,
            color = Color.White,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

// ─── Calendário do funcionário ─────────────────────────────────────────────────

@Composable
private fun EmployeeCalendarScreen(user: User, onBack: () -> Unit) {
    var shifts by remember { mutableStateOf<List<Shift>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val today = LocalDate.now()
    val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM", Locale("pt", "PT"))
    val weekFmt = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "PT"))

    LaunchedEffect(user.id) {
        try {
            // Carregar mês atual + 2 meses seguintes (backend exige week ou month param)
            val months = (0..2).map { java.time.YearMonth.now().plusMonths(it.toLong()) }
            val allShifts = mutableListOf<pt.ualg.miaugenda.data.model.Shift>()
            for (m in months) {
                val resp = RetrofitClient.shiftApi.getShifts(
                    month  = m.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                    userId = user.id
                )
                if (resp.isSuccessful) allShifts.addAll(resp.body() ?: emptyList())
            }
            shifts = allShifts
                .filter { it.published }
                .filter {
                    val d = runCatching { LocalDate.parse(it.date.substring(0, 10)) }.getOrNull()
                    d != null && !d.isBefore(today)
                }
                .sortedBy { it.date }
        } catch (_: Exception) {}
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                // Mini avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Blue, Color(0xFFE91E8C)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.split(" ").filter { it.isNotEmpty() }.take(2)
                            .joinToString("") { it.first().uppercaseChar().toString() },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    user.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Blue, modifier = Modifier.size(22.dp).padding(end = 4.dp))
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Blue)
                }
            } else if (shifts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TxtGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Sem turnos publicados futuros", color = TxtGray, fontSize = 15.sp)
                    }
                }
            } else {
                // Subtítulo com intervalo
                val firstDate = runCatching { LocalDate.parse(shifts.first().date.substring(0, 10)) }.getOrNull()
                val lastDate  = runCatching { LocalDate.parse(shifts.last().date.substring(0, 10)) }.getOrNull()
                if (firstDate != null && lastDate != null) {
                    Text(
                        "${firstDate.format(weekFmt)} – ${lastDate.format(weekFmt)}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                HorizontalDivider(color = DkDivider, thickness = 0.5.dp)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(shifts) { shift ->
                        val shiftDate = runCatching { LocalDate.parse(shift.date.substring(0, 10)) }.getOrNull()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Coluna da data
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(40.dp)
                            ) {
                                val isToday = shiftDate == today
                                Text(
                                    shiftDate?.dayOfMonth?.toString() ?: "",
                                    color = if (isToday) Blue else Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    shiftDate?.format(DateTimeFormatter.ofPattern("EEE", Locale("pt","PT")))?.uppercase() ?: "",
                                    color = if (isToday) Blue else TxtGray,
                                    fontSize = 11.sp
                                )
                            }

                            // Card do turno com barra colorida
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DkSurface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(Blue)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        shift.shiftType?.name ?: "Sem posição",
                                        color = Blue,
                                        fontSize = 12.sp
                                    )
                                    val start = shift.startTime ?: shift.shiftType?.startTime ?: ""
                                    val end   = shift.endTime   ?: shift.shiftType?.endTime   ?: ""
                                    Text(
                                        "$start – $end",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = DkDivider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 68.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Adicionar Funcionário ─────────────────────────────────────────────────────

private val ROLES       = listOf("EMPLOYEE", "MANAGER", "ADMIN")
private val CATEGORIES  = listOf("VETERINARIAN", "NURSE", "ADMINISTRATIVE", "OPERATIONAL")

@Composable
private fun AddEmployeeScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var name         by remember { mutableStateOf("") }
    var email        by remember { mutableStateOf("") }
    var employeeNum  by remember { mutableStateOf("") }
    var contact      by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var roleIdx      by remember { mutableStateOf(0) }
    var categoryIdx  by remember { mutableStateOf(0) }
    var isSaving     by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }
    var showBanner   by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val canSave = name.isNotBlank() && email.isNotBlank() && employeeNum.isNotBlank() && password.isNotBlank()

    LaunchedEffect(showBanner) {
        if (showBanner) {
            delay(2500)
            showBanner = false
            onSaved()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("Cancelar", color = Blue, fontSize = 16.sp)
                }
                Text(
                    text = name.ifBlank { "Novo Funcionário" },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
                TextButton(
                    onClick = {
                        if (!canSave) return@TextButton
                        isSaving = true
                        errorMsg = null
                        scope.launch {
                            try {
                                val req = CreateUserRequest(
                                    name = name.trim(),
                                    email = email.trim(),
                                    employeeNumber = employeeNum.trim(),
                                    role = ROLES[roleIdx],
                                    category = CATEGORIES[categoryIdx],
                                    password = password
                                )
                                val resp = RetrofitClient.userApi.createUser(req)
                                if (resp.isSuccessful) {
                                    showBanner = true
                                } else {
                                    errorMsg = "Erro ao criar funcionário (${resp.code()})"
                                }
                            } catch (e: Exception) {
                                errorMsg = e.message
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = canSave && !isSaving,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        text = if (isSaving) "..." else "Guardar",
                        color = if (canSave && !isSaving) Blue else TxtGray,
                        fontSize = 16.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF2979FF), Color(0xFFE91E8C)))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (name.isBlank()) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                            } else {
                                Text(
                                    text = name.split(" ").filter { it.isNotEmpty() }.take(2)
                                        .joinToString("") { it.first().uppercaseChar().toString() },
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(DkSurface2)
                                .border(2.dp, DkBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Campos
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DkSurface)
                ) {
                    AddField(value = name, placeholder = "Nome completo", onValueChange = { name = it })
                    HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    AddField(value = email, placeholder = "Email", onValueChange = { email = it })
                    HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    AddField(value = employeeNum, placeholder = "Nº Funcionário (ex: EMP007)", onValueChange = { employeeNum = it })
                    HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    AddField(value = contact, placeholder = "Contacto (opcional)", onValueChange = { contact = it })
                    HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    AddField(value = password, placeholder = "Password", onValueChange = { password = it }, isPassword = true)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Função
                Text("Função", color = TxtGray, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DkSurface)
                ) {
                    ROLES.forEachIndexed { idx, role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { roleIdx = idx }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when (role) {
                                    "EMPLOYEE" -> "Funcionário"
                                    "MANAGER"  -> "Gerente"
                                    else       -> "Administrador"
                                },
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            if (roleIdx == idx) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Blue, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (idx < ROLES.lastIndex) HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Categoria
                Text("Categoria", color = TxtGray, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DkSurface)
                ) {
                    CATEGORIES.forEachIndexed { idx, cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryIdx = idx }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when (cat) {
                                    "VETERINARIAN"  -> "Veterinário"
                                    "NURSE"         -> "Enfermeiro"
                                    "ADMINISTRATIVE"-> "Administrativo"
                                    else            -> "Operacional"
                                },
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            if (categoryIdx == idx) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Blue, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (idx < CATEGORIES.lastIndex) HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    }
                }

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(it, color = Color(0xFFFF453A), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Banner de sucesso
        AnimatedVisibility(
            visible = showBanner,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DkGreen)
                    .statusBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("Funcionário criado com sucesso!", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun AddField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
        singleLine = true,
        cursorBrush = SolidColor(Blue),
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation()
                               else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, color = TxtGray, fontSize = 15.sp)
            inner()
        }
    )
}

// ─── Ecrã de Férias do funcionário (ADMIN/MANAGER) ───────────────────────────

@Composable
private fun EmployeeVacationsScreen(user: User, onBack: () -> Unit) {
    var vacations by remember { mutableStateOf<List<TimeOffRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var removeTarget by remember { mutableStateOf<TimeOffRequest?>(null) }
    var removeReason by remember { mutableStateOf("") }
    var isRemoving by remember { mutableStateOf(false) }
    var bannerMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("pt", "PT"))

    LaunchedEffect(user.id) {
        try {
            val resp = RetrofitClient.requestApi.getTimeOffRequests()
            if (resp.isSuccessful) {
                vacations = resp.body().orEmpty()
                    .filter { it.userId == user.id && it.status == "APPROVED" }
                    .sortedBy { it.startDate }
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    LaunchedEffect(bannerMsg) {
        if (bannerMsg != null) { delay(2500); bannerMsg = null }
    }

    Box(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Text(
                    "${user.name.split(" ").first()} — Férias",
                    color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Outlined.BeachAccess, contentDescription = null, tint = Blue,
                    modifier = Modifier.size(22.dp).padding(end = 4.dp))
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Blue)
                }
            } else if (vacations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.BeachAccess, contentDescription = null, tint = TxtGray,
                            modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Sem férias aprovadas", color = TxtGray, fontSize = 15.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vacations) { vac ->
                        val start = runCatching { LocalDate.parse(vac.startDate.substring(0, 10)).format(dateFmt) }.getOrDefault(vac.startDate.substring(0, 10))
                        val end   = runCatching { LocalDate.parse(vac.endDate.substring(0, 10)).format(dateFmt) }.getOrDefault(vac.endDate.substring(0, 10))
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DkSurface)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("$start → $end", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                if (!vac.reason.isNullOrBlank()) {
                                    Text(vac.reason, color = TxtGray, fontSize = 13.sp)
                                }
                            }
                            TextButton(onClick = { removeTarget = vac; removeReason = "" }) {
                                Text("Remover", color = Color(0xFFFF453A), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Diálogo de confirmação de remoção
        if (removeTarget != null) {
            AlertDialog(
                onDismissRequest = { removeTarget = null },
                containerColor = DkSurface,
                title = { Text("Remover férias?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Insira um motivo para o funcionário.", color = TxtGray, fontSize = 14.sp)
                        BasicTextField(
                            value = removeReason,
                            onValueChange = { removeReason = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(Blue),
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DkSurface2)
                                .padding(10.dp),
                            decorationBox = { inner ->
                                if (removeReason.isEmpty()) Text("Motivo (opcional)", color = TxtGray, fontSize = 14.sp)
                                inner()
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isRemoving,
                        onClick = {
                            val target = removeTarget ?: return@TextButton
                            isRemoving = true
                            scope.launch {
                                try {
                                    val r = RetrofitClient.requestApi.updateTimeOffRequestStatus(
                                        target.id, mapOf("status" to "REJECTED")
                                    )
                                    if (r.isSuccessful) {
                                        vacations = vacations.filter { it.id != target.id }
                                        bannerMsg = "Férias removidas"
                                    }
                                } catch (_: Exception) {}
                                isRemoving = false
                                removeTarget = null
                            }
                        }
                    ) {
                        Text(if (isRemoving) "A processar..." else "CONFIRMAR",
                            color = Color(0xFFFF453A), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { removeTarget = null }) { Text("CANCELAR", color = TxtGray) }
                }
            )
        }

        // Banner de feedback
        bannerMsg?.let { msg ->
            Box(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .background(DkGreen).statusBarsPadding().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(msg, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Ecrã de Edição de Funcionário (ADMIN only) ───────────────────────────────

@Composable
private fun EditEmployeeScreen(user: User, onBack: () -> Unit, onSaved: () -> Unit) {
    var name        by remember { mutableStateOf(user.name) }
    var email       by remember { mutableStateOf(user.email) }
    var contact     by remember { mutableStateOf(user.contact ?: "") }
    var roleIdx     by remember { mutableStateOf(ROLES.indexOf(user.role).coerceAtLeast(0)) }
    var categoryIdx by remember { mutableStateOf(CATEGORIES.indexOf(user.category).coerceAtLeast(0)) }
    var isSaving    by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    var showBanner  by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val canSave = name.isNotBlank() && email.isNotBlank() && !isSaving

    LaunchedEffect(showBanner) {
        if (showBanner) { delay(2500); showBanner = false; onSaved() }
    }

    Box(modifier = Modifier.fillMaxSize().background(DkBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("Cancelar", color = Blue, fontSize = 16.sp)
                }
                Text(
                    "Editar Funcionário",
                    color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
                TextButton(
                    onClick = {
                        if (!canSave) return@TextButton
                        isSaving = true; errorMsg = null
                        scope.launch {
                            try {
                                val resp = RetrofitClient.userApi.updateUser(
                                    user.id,
                                    UpdateUserRequest(
                                        name = name.trim(),
                                        email = email.trim(),
                                        contact = contact.trim().ifBlank { null },
                                        role = ROLES[roleIdx],
                                        category = CATEGORIES[categoryIdx]
                                    )
                                )
                                if (resp.isSuccessful) showBanner = true
                                else errorMsg = "Erro (${resp.code()})"
                            } catch (e: Exception) {
                                errorMsg = e.message
                            }
                            isSaving = false
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        if (isSaving) "..." else "Guardar",
                        color = if (canSave) Blue else TxtGray, fontSize = 16.sp
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(12.dp))

                // Campos básicos
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp)).background(DkSurface)
                ) {
                    AddField(value = name, placeholder = "Nome completo", onValueChange = { name = it })
                    HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    AddField(value = email, placeholder = "Email", onValueChange = { email = it })
                    HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    AddField(value = contact, placeholder = "Contacto (opcional)", onValueChange = { contact = it })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Função
                Text("Função", color = TxtGray, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp)).background(DkSurface)
                ) {
                    ROLES.forEachIndexed { idx, role ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { roleIdx = idx }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when (role) {
                                    "EMPLOYEE" -> "Funcionário"
                                    "MANAGER"  -> "Gerente"
                                    else       -> "Administrador"
                                },
                                color = Color.White, fontSize = 15.sp
                            )
                            if (roleIdx == idx) {
                                Icon(Icons.Default.Check, null, tint = Blue, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (idx < ROLES.lastIndex) HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Categoria
                Text("Categoria", color = TxtGray, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp)).background(DkSurface)
                ) {
                    CATEGORIES.forEachIndexed { idx, cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { categoryIdx = idx }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when (cat) {
                                    "VETERINARIAN"   -> "Veterinário"
                                    "NURSE"          -> "Enfermeiro"
                                    "ADMINISTRATIVE" -> "Administrativo"
                                    else             -> "Operacional"
                                },
                                color = Color.White, fontSize = 15.sp
                            )
                            if (categoryIdx == idx) {
                                Icon(Icons.Default.Check, null, tint = Blue, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (idx < CATEGORIES.lastIndex) HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    }
                }

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(it, color = Color(0xFFFF453A), fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp))
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Banner de sucesso
        AnimatedVisibility(
            visible = showBanner,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().background(DkGreen)
                    .statusBarsPadding().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("Funcionário atualizado!", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}
