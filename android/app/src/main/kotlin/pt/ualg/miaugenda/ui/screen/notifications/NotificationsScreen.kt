package pt.ualg.miaugenda.ui.screen.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import pt.ualg.miaugenda.ui.components.AppBottomNav
import pt.ualg.miaugenda.ui.components.NavTab
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Cores (igual ao SchedulerScreen) ─────────────────────────────────────────
private val DkBg       = Color(0xFF000000)
private val DkSurface  = Color(0xFF1C1C1E)
private val Blue       = Color(0xFF2979FF)
private val Orange     = Color(0xFFFF8F00)
private val DkGreen    = Color(0xFF2ECC71)
private val TxtGray    = Color(0xFF8E8E93)
private val RedBadge   = Color(0xFFFF3B30)
private val AvatarGrad = Brush.linearGradient(listOf(Color(0xFF5B5FEF), Color(0xFFC850C0), Color(0xFFF0696B)))

// ── Modelos locais ────────────────────────────────────────────────────────────
private enum class NotifType { FOLHA_DE_HORAS, PEDIDO_DE_FOLGA }
private enum class NotifStatus { PENDENTE, APROVADO, REJEITADO }

private data class NotifItem(
    val id: Int,
    val type: NotifType,
    val shiftDateLabel: String,
    val duration: String,
    val description: String,
    val employeeName: String,
    val startTime: String?,
    val endTime: String?,
    val schedule: String,
    val position: String,
    val status: NotifStatus = NotifStatus.PENDENTE,
    val actionBy: String? = null
)

private val GROUP_DATE = "Qui, 14 de Mai 2026"

private val initialNotifications = listOf(
    NotifItem(
        id = 1,
        type = NotifType.FOLHA_DE_HORAS,
        shiftDateLabel = "Qua, 13 de Mai 2026",
        duration = "8h 0m",
        description = "",
        employeeName = "Miguel Santos",
        startTime = "09:00",
        endTime = "17:00",
        schedule = "Padrão",
        position = "Administrativo"
    ),
    NotifItem(
        id = 2,
        type = NotifType.PEDIDO_DE_FOLGA,
        shiftDateLabel = "Sex, 15 de Mai 2026",
        duration = "Dia inteiro",
        description = "Falta sem remuneração",
        employeeName = "Maria Fernandes",
        startTime = null,
        endTime = null,
        schedule = "Padrão",
        position = "Veterinário"
    )
)

// ── Ecrã principal ────────────────────────────────────────────────────────────
@Composable
fun NotificationsScreen(
    managerName: String = "Xavier Bolotinha",
    onNavigateToScheduler: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToEquipa: () -> Unit = {}
) {
    var notifications by remember { mutableStateOf(initialNotifications) }
    var detailItemId by remember { mutableStateOf<Int?>(null) }
    var rejectTargetId by remember { mutableStateOf<Int?>(null) }
    var approveTargetId by remember { mutableStateOf<Int?>(null) }

    val pendingCount = notifications.count { it.status == NotifStatus.PENDENTE }
    val detailItem = detailItemId?.let { id -> notifications.find { it.id == id } }

    BackHandler(enabled = detailItem != null) { detailItemId = null }
    val rejectTarget = rejectTargetId?.let { id -> notifications.find { it.id == id } }
    val approveTarget = approveTargetId?.let { id -> notifications.find { it.id == id } }

    fun approve(id: Int) {
        notifications = notifications.map {
            if (it.id == id) it.copy(status = NotifStatus.APROVADO, actionBy = managerName) else it
        }
    }

    fun reject(id: Int) {
        notifications = notifications.map {
            if (it.id == id) it.copy(status = NotifStatus.REJEITADO, actionBy = managerName) else it
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DkBg).systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                if (detailItem != null) {
                    NotificationDetailScreen(
                        item = detailItem,
                        onBack = { detailItemId = null },
                        onApprove = { approveTargetId = detailItem.id },
                        onReject = { rejectTargetId = detailItem.id }
                    )
                } else {
                    NotificationsList(
                        notifications = notifications,
                        pendingCount = pendingCount,
                        onReadAll = {
                            notifications = notifications.map {
                                if (it.status == NotifStatus.PENDENTE)
                                    it.copy(status = NotifStatus.APROVADO, actionBy = managerName)
                                else it
                            }
                        },
                        onViewDetails = { detailItemId = it.id },
                        onApprove = { approveTargetId = it.id },
                        onReject = { rejectTargetId = it.id }
                    )
                }
            }
            AppBottomNav(
                active               = NavTab.NOTIFICATIONS,
                notificationCount    = pendingCount,
                onHomeClick          = onNavigateToHome,
                onSchedulerClick     = onNavigateToScheduler,
                onInboxClick         = onNavigateToInbox,
                onMenuClick          = onNavigateToEquipa
            )
        }

        // Diálogo de aprovação
        if (approveTarget != null) {
            val typeLabel = if (approveTarget.type == NotifType.FOLHA_DE_HORAS) "folha de horas" else "pedido de folga"
            AlertDialog(
                onDismissRequest = { approveTargetId = null },
                containerColor = DkSurface,
                title = {
                    Text(
                        "Aprovar esta $typeLabel?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text("O funcionário será notificado.", color = TxtGray, fontSize = 14.sp)
                },
                confirmButton = {
                    TextButton(onClick = {
                        approve(approveTarget.id)
                        approveTargetId = null
                    }) {
                        Text("APROVAR", color = Blue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { approveTargetId = null }) {
                        Text("CANCELAR", color = TxtGray)
                    }
                }
            )
        }

        // Diálogo de rejeição
        if (rejectTarget != null) {
            val typeLabel = if (rejectTarget.type == NotifType.FOLHA_DE_HORAS) "folha de horas" else "pedido de folga"
            AlertDialog(
                onDismissRequest = { rejectTargetId = null },
                containerColor = DkSurface,
                title = {
                    Text(
                        "Rejeitar esta $typeLabel?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text("Esta ação não pode ser desfeita.", color = TxtGray, fontSize = 14.sp)
                },
                confirmButton = {
                    TextButton(onClick = {
                        reject(rejectTarget.id)
                        rejectTargetId = null
                    }) {
                        Text("REJEITAR", color = RedBadge, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rejectTargetId = null }) {
                        Text("CANCELAR", color = TxtGray)
                    }
                }
            )
        }
    }
}

// ── Lista de notificações ─────────────────────────────────────────────────────
@Composable
private fun NotificationsList(
    notifications: List<NotifItem>,
    pendingCount: Int,
    onReadAll: () -> Unit,
    onViewDetails: (NotifItem) -> Unit,
    onApprove: (NotifItem) -> Unit,
    onReject: (NotifItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notificações", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                if (pendingCount > 0) {
                    TextButton(onClick = onReadAll) {
                        Text("Ler tudo ($pendingCount)", color = Blue, fontSize = 14.sp)
                    }
                }
            }
        }
        item {
            Text(
                GROUP_DATE,
                color = TxtGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
        items(notifications) { notif ->
            NotifCard(
                item = notif,
                onViewDetails = { onViewDetails(notif) },
                onApprove = { onApprove(notif) },
                onReject = { onReject(notif) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Cartão de notificação ─────────────────────────────────────────────────────
@Composable
private fun NotifCard(
    item: NotifItem,
    onViewDetails: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val typeLabel = if (item.type == NotifType.FOLHA_DE_HORAS) "FOLHA DE HORAS" else "PEDIDO DE FOLGA"
    val typeColor = if (item.type == NotifType.FOLHA_DE_HORAS) Orange else Blue

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DkSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (item.status) {
            NotifStatus.PENDENTE -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeColor.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(typeLabel, color = typeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(item.shiftDateLabel, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(item.duration, color = TxtGray, fontSize = 13.sp)
                if (item.description.isNotEmpty()) {
                    Text(item.description, color = TxtGray, fontSize = 13.sp)
                }
                Text("Funcionário: ${item.employeeName}", color = TxtGray, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewDetails,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF444444)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text("Ver detalhes", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedBadge.copy(alpha = 0.15f),
                            contentColor = RedBadge
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text("Rejeitar", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue.copy(alpha = 0.15f),
                            contentColor = Blue
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text("Aprovar", fontSize = 11.sp)
                    }
                }
            }

            NotifStatus.APROVADO, NotifStatus.REJEITADO -> {
                val isRejected = item.status == NotifStatus.REJEITADO
                val actionMsg = when {
                    isRejected && item.type == NotifType.FOLHA_DE_HORAS -> "Rejeitou esta folha de horas."
                    isRejected -> "Rejeitou este pedido de folga."
                    item.type == NotifType.FOLHA_DE_HORAS -> "Aprovou esta folha de horas."
                    else -> "Aprovou este pedido de folga."
                }
                val pillLabel = when {
                    isRejected && item.type == NotifType.FOLHA_DE_HORAS -> "FOLHA REJEITADA"
                    isRejected -> "PEDIDO REJEITADO"
                    item.type == NotifType.FOLHA_DE_HORAS -> "FOLHA DE HORAS APROVADA"
                    else -> "PEDIDO DE FOLGA APROVADO"
                }
                val pillColor = if (isRejected) Orange else TxtGray
                val byLabel = if (isRejected) "Rejeitado por:" else "Aprovado por:"

                Text(actionMsg, color = TxtGray, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(pillColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(pillLabel, color = pillColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text("$byLabel ${item.actionBy ?: ""}", color = TxtGray, fontSize = 13.sp)
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF444444))
                ) {
                    Text("Ver detalhes", fontSize = 12.sp)
                }
            }
        }
    }
}

// ── Ecrã de detalhe ───────────────────────────────────────────────────────────
@Composable
private fun NotificationDetailScreen(
    item: NotifItem,
    onBack: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val title = if (item.type == NotifType.FOLHA_DE_HORAS) "Folha de Horas" else "Pedido de Folga"
    val isPending = item.status == NotifStatus.PENDENTE
    val isRejected = item.status == NotifStatus.REJEITADO

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de topo
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.ArrowBack, null,
                    tint = Blue,
                    modifier = Modifier.size(22.dp).clickable { onBack() }
                )
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Outlined.MoreVert, null, tint = TxtGray, modifier = Modifier.size(22.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DkSurface)
                ) {
                    DetailRow("Funcionário", item.employeeName)
                    HorizontalDivider(color = Color(0xFF2C2C2E))
                    DetailRow("Data", item.shiftDateLabel)
                    HorizontalDivider(color = Color(0xFF2C2C2E))
                    // Total com seta de expansão
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total", color = Color.White, fontSize = 15.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(item.duration, color = Blue, fontSize = 15.sp)
                            Icon(
                                Icons.Filled.KeyboardArrowDown, null,
                                tint = TxtGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (item.startTime != null) {
                        HorizontalDivider(color = Color(0xFF2C2C2E))
                        DetailRow("Hora Início", item.startTime)
                    }
                    if (item.endTime != null) {
                        HorizontalDivider(color = Color(0xFF2C2C2E))
                        DetailRow("Hora Fim", item.endTime)
                    }
                    HorizontalDivider(color = Color(0xFF2C2C2E))
                    DetailRow("Horário", item.schedule)
                    HorizontalDivider(color = Color(0xFF2C2C2E))
                    DetailRow("Posição", item.position)
                    HorizontalDivider(color = Color(0xFF2C2C2E))
                    // Estado
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Estado", color = Color.White, fontSize = 15.sp)
                        val (statusText, statusColor) = when (item.status) {
                            NotifStatus.PENDENTE -> "Pendente" to Orange
                            NotifStatus.APROVADO -> "Aprovado" to DkGreen
                            NotifStatus.REJEITADO -> "Rejeitado" to RedBadge
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                statusText,
                                color = statusColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }

            item {
                Text(
                    "NOTAS",
                    color = TxtGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DkSurface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val submitAction = if (item.type == NotifType.FOLHA_DE_HORAS)
                        "Submeteu a folha de horas" else "Submeteu o pedido de folga"
                    NoteEntry(name = item.employeeName, action = submitAction, time = "há 6 dias")

                    if (item.status != NotifStatus.PENDENTE) {
                        HorizontalDivider(color = Color(0xFF2C2C2E))
                        val managerAction = when {
                            item.status == NotifStatus.APROVADO && item.type == NotifType.FOLHA_DE_HORAS ->
                                "Aprovou a folha de horas"
                            item.status == NotifStatus.APROVADO ->
                                "Aprovou o pedido de folga"
                            item.type == NotifType.FOLHA_DE_HORAS ->
                                "Rejeitou a folha de horas"
                            else ->
                                "Rejeitou o pedido de folga"
                        }
                        NoteEntry(
                            name = item.actionBy ?: "",
                            action = managerAction,
                            time = "há 3 min"
                        )
                    }
                }
            }
        }

        // Barra inferior de ações
        Column(modifier = Modifier.fillMaxWidth().background(DkSurface)) {
            HorizontalDivider(color = Color(0xFF2C2C2E))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {}) {
                    Text("Adicionar nota", color = TxtGray, fontSize = 15.sp)
                }
                VerticalDivider(
                    modifier = Modifier.height(24.dp).padding(horizontal = 4.dp),
                    color = Color(0xFF3C3C3E)
                )
                if (isPending) {
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onReject,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedBadge.copy(alpha = 0.15f),
                            contentColor = RedBadge
                        )
                    ) {
                        Text("Rejeitar", fontSize = 15.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue)
                    ) {
                        Text("Aprovar ✓", color = Color.White, fontSize = 15.sp)
                    }
                } else if (isRejected) {
                    TextButton(onClick = {}) {
                        Text("Reenviar", color = Blue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Utilitários ───────────────────────────────────────────────────────────────
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 15.sp)
        Text(value, color = Blue, fontSize = 15.sp)
    }
}

@Composable
private fun NoteEntry(name: String, action: String, time: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(AvatarGrad),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.trim().split(" ").let { parts ->
                    if (parts.size >= 2) "${parts.first()[0]}${parts.last()[0]}" else name.take(2)
                }.uppercase(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(action, color = TxtGray, fontSize = 13.sp)
            Text(time, color = TxtGray, fontSize = 12.sp)
        }
    }
}

// ── Barra de navegação inferior ───────────────────────────────────────────────
@Composable
private fun NotifBottomNav(
    pendingCount: Int,
    onHomeClick: () -> Unit,
    onSchedulerClick: () -> Unit
) {
    val items = listOf(
        "Inicio" to Icons.Outlined.Home,
        "Agenda" to Icons.Outlined.DateRange,
        "Caixa" to Icons.Outlined.Inbox,
        "Notificacoes" to Icons.Outlined.Notifications,
        "Menu" to Icons.Outlined.Menu
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DkSurface)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(0.dp))
            .pointerInput(Unit) {}
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items.forEachIndexed { i, (label, icon) ->
            val active = i == 3
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable {
                    when (i) {
                        0 -> onHomeClick()
                        1 -> onSchedulerClick()
                        else -> {}
                    }
                }
            ) {
                Box {
                    Icon(
                        icon, contentDescription = label,
                        tint = if (active) Blue else TxtGray,
                        modifier = Modifier.size(22.dp)
                    )
                    if (i == 3 && pendingCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .clip(CircleShape)
                                .background(RedBadge)
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$pendingCount",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Text(
                    label,
                    fontSize = 10.sp,
                    color = if (active) Blue else TxtGray,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
