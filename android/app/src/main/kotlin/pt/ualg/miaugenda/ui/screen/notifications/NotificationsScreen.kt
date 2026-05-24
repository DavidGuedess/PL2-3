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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.data.model.TimeOffRequest
import pt.ualg.miaugenda.data.model.ShiftSwapRequest
import pt.ualg.miaugenda.data.remote.RetrofitClient
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ── Cores ─────────────────────────────────────────────────────────────────────
private val DkBg       = Color(0xFF000000)
private val DkSurface  = Color(0xFF1C1C1E)
private val Blue       = Color(0xFF2979FF)
private val Orange     = Color(0xFFFF8F00)
private val DkGreen    = Color(0xFF2ECC71)
private val TxtGray    = Color(0xFF8E8E93)
private val RedBadge   = Color(0xFFFF3B30)
private val AvatarGrad = Brush.linearGradient(listOf(Color(0xFF5B5FEF), Color(0xFFC850C0), Color(0xFFF0696B)))

// ── Modelos locais ────────────────────────────────────────────────────────────
private enum class NotifType { PEDIDO_DE_FOLGA, PEDIDO_DE_TROCA }
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
    val status: NotifStatus = NotifStatus.PENDENTE,
    val approvedByName: String? = null,
    val requestCategory: String = "",
    val isRead: Boolean = false,
    // true quando o utilizador atual é o target do swap e ainda não respondeu
    val isTargetResponse: Boolean = false,
    val createdAt: String = "",
    // true quando é o próprio utilizador que fez o pedido (não pode aprovar/rejeitar)
    val isOwnRequest: Boolean = false
)

private val dateFmtShort = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale("pt", "PT"))

private fun torToNotifItem(req: TimeOffRequest): NotifItem {
    val start = runCatching { LocalDate.parse(req.startDate.substring(0, 10)) }.getOrNull()
    val end   = runCatching { LocalDate.parse(req.endDate.substring(0, 10)) }.getOrNull()
    val days  = if (start != null && end != null) ChronoUnit.DAYS.between(start, end) + 1 else 1L
    return NotifItem(
        id             = req.id,
        type           = NotifType.PEDIDO_DE_FOLGA,
        shiftDateLabel = "${start?.format(dateFmtShort) ?: req.startDate.substring(0,10)} → ${end?.format(dateFmtShort) ?: req.endDate.substring(0,10)}",
        duration       = if (req.allDay) "$days dia${if (days != 1L) "s" else ""}" else "Parcial",
        description    = req.reason ?: "",
        employeeName   = req.user?.name ?: "",
        startTime      = null,
        endTime        = null,
        schedule       = if (req.allDay) "Dia inteiro" else "Parcial",
        status         = when (req.status) {
            "APPROVED" -> NotifStatus.APROVADO
            "REJECTED" -> NotifStatus.REJEITADO
            else       -> NotifStatus.PENDENTE
        },
        approvedByName  = req.approvedByName,
        requestCategory = "timeOff",
        createdAt       = req.createdAt
    )
}

private fun ssrToNotifItem(req: ShiftSwapRequest, currentUserId: Int): NotifItem {
    val myDate  = req.requesterShift?.let { runCatching { LocalDate.parse(it.date.substring(0,10)).format(dateFmtShort) }.getOrDefault(it.date.substring(0,10)) } ?: ""
    val colDate = req.targetShift?.let { runCatching { LocalDate.parse(it.date.substring(0,10)).format(dateFmtShort) }.getOrDefault(it.date.substring(0,10)) } ?: ""
    val isTargetResponse = req.targetShift?.user?.id == currentUserId && req.targetAccepted == null
    val status = when {
        req.targetAccepted == false -> NotifStatus.REJEITADO
        req.status == "APPROVED"   -> NotifStatus.APROVADO
        req.status == "REJECTED"   -> NotifStatus.REJEITADO
        else                        -> NotifStatus.PENDENTE
    }
    return NotifItem(
        id              = req.id,
        type            = NotifType.PEDIDO_DE_TROCA,
        shiftDateLabel  = "$myDate ↔ $colDate",
        duration        = "${req.requesterShift?.let { "${it.startTime ?: ""} – ${it.endTime ?: ""}" } ?: ""} ↔ ${req.targetShift?.let { "${it.startTime ?: ""} – ${it.endTime ?: ""}" } ?: ""}",
        description     = req.reason ?: "",
        employeeName    = req.requester?.name ?: "",
        startTime       = req.requesterShift?.startTime,
        endTime         = req.requesterShift?.endTime,
        schedule        = "",
        status          = status,
        approvedByName  = req.approvedByName,
        requestCategory = "swap",
        isTargetResponse = isTargetResponse,
        createdAt       = req.createdAt
    )
}

// ── Ecrã principal ────────────────────────────────────────────────────────────
@Composable
fun NotificationsScreen(
    currentUserId: Int = -1,
    currentRole: String = "EMPLOYEE",
    currentUserName: String = "",
    onNavigateToScheduler: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToEquipa: () -> Unit = {}
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val isManager = currentRole == "ADMIN" || currentRole == "MANAGER"

    var notifications by remember { mutableStateOf<List<NotifItem>>(emptyList()) }
    var detailItemId  by remember { mutableStateOf<Int?>(null) }
    var rejectTargetId  by remember { mutableStateOf<Int?>(null) }
    var approveTargetId by remember { mutableStateOf<Int?>(null) }
    var acceptSwapId    by remember { mutableStateOf<Int?>(null) }
    var refuseSwapId    by remember { mutableStateOf<Int?>(null) }

    fun loadNotifications() {
        scope.launch {
            try {
                val rawSwaps = RetrofitClient.requestApi.getShiftSwapRequests().body().orEmpty()
                val rawTor   = RetrofitClient.requestApi.getTimeOffRequests().body().orEmpty()

                val swapItems: List<NotifItem>
                val torItems: List<NotifItem>

                if (isManager) {
                    // gestor vê pedidos onde o target já aceitou (aguarda aprovação) ou já processados
                    // mas marca os seus próprios pedidos como isOwnRequest (não pode aprovar os seus)
                    swapItems = rawSwaps
                        .filter { it.targetAccepted == true || it.status != "PENDING" }
                        .map { ssr ->
                            val item = ssrToNotifItem(ssr, currentUserId)
                            if (ssr.requesterId == currentUserId) item.copy(isOwnRequest = true) else item
                        }
                    torItems = rawTor.map { tor ->
                        val item = torToNotifItem(tor)
                        if (tor.userId == currentUserId) item.copy(isOwnRequest = true) else item
                    }
                } else {
                    // pedidos onde o funcionário é target e ainda não respondeu (aceitar/recusar troca)
                    val targetSwaps = rawSwaps
                        .filter { it.targetShift?.user?.id == currentUserId && it.targetAccepted == null }
                        .map { ssrToNotifItem(it, currentUserId) }
                    // pedidos próprios do funcionário (ver estado)
                    val ownSwaps = rawSwaps
                        .filter { it.requesterId == currentUserId }
                        .map { ssrToNotifItem(it, currentUserId).copy(isOwnRequest = true) }
                    swapItems = (targetSwaps + ownSwaps).distinctBy { it.id }
                    torItems = rawTor
                        .filter { it.userId == currentUserId }
                        .map { torToNotifItem(it).copy(isOwnRequest = true) }
                }

                notifications = (torItems + swapItems).sortedByDescending { it.id }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(Unit) { loadNotifications() }

    val detailItem   = detailItemId?.let { id -> notifications.find { it.id == id } }

    BackHandler(enabled = detailItem != null) { detailItemId = null }

    val rejectTarget  = rejectTargetId?.let  { id -> notifications.find { it.id == id } }
    val approveTarget = approveTargetId?.let { id -> notifications.find { it.id == id } }
    val acceptSwap    = acceptSwapId?.let    { id -> notifications.find { it.id == id } }
    val refuseSwap    = refuseSwapId?.let    { id -> notifications.find { it.id == id } }

    fun approve(item: NotifItem) {
        scope.launch {
            try {
                val r = when (item.requestCategory) {
                    "timeOff" -> RetrofitClient.requestApi.updateTimeOffRequestStatus(item.id, mapOf("status" to "APPROVED"))
                    "swap"    -> RetrofitClient.requestApi.updateShiftSwapRequestStatus(item.id, mapOf("status" to "APPROVED"))
                    else      -> null
                }
                if (r?.isSuccessful == true) {
                    detailItemId = null
                    loadNotifications()
                }
            } catch (_: Exception) { }
        }
    }

    fun reject(item: NotifItem) {
        scope.launch {
            try {
                val r = when (item.requestCategory) {
                    "timeOff" -> RetrofitClient.requestApi.updateTimeOffRequestStatus(item.id, mapOf("status" to "REJECTED"))
                    "swap"    -> RetrofitClient.requestApi.updateShiftSwapRequestStatus(item.id, mapOf("status" to "REJECTED"))
                    else      -> null
                }
                if (r?.isSuccessful == true) {
                    detailItemId = null
                    loadNotifications()
                }
            } catch (_: Exception) { }
        }
    }

    fun respondToSwap(item: NotifItem, accept: Boolean) {
        scope.launch {
            try {
                val r = RetrofitClient.requestApi.respondToSwapRequest(item.id, mapOf("accept" to accept))
                if (r.isSuccessful) {
                    loadNotifications()
                }
            } catch (_: Exception) { }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DkBg).systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                if (detailItem != null) {
                    NotificationDetailScreen(
                        item       = detailItem,
                        isManager  = isManager,
                        onBack     = { detailItemId = null },
                        onApprove  = { approveTargetId = detailItem.id },
                        onReject   = { rejectTargetId  = detailItem.id },
                        onAccept   = { acceptSwapId = detailItem.id },
                        onRefuse   = { refuseSwapId = detailItem.id }
                    )
                } else {
                    NotificationsList(
                        notifications = notifications,
                        isManager     = isManager,
                        onViewDetails = { detailItemId   = it.id },
                        onApprove     = { approveTargetId = it.id },
                        onReject      = { rejectTargetId  = it.id },
                        onAccept      = { acceptSwapId = it.id },
                        onRefuse      = { refuseSwapId = it.id }
                    )
                }
            }
            AppBottomNav(
                active               = NavTab.NOTIFICATIONS,
                onHomeClick          = onNavigateToHome,
                onSchedulerClick     = onNavigateToScheduler,
                onInboxClick         = onNavigateToInbox,
                onMenuClick          = onNavigateToEquipa
            )
        }

        // Diálogo Aprovar
        if (approveTarget != null) {
            AlertDialog(
                onDismissRequest = { approveTargetId = null },
                containerColor = DkSurface,
                title = { Text("Aprovar pedido?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text  = { Text("O funcionário será notificado.", color = TxtGray, fontSize = 14.sp) },
                confirmButton = {
                    TextButton(onClick = { approve(approveTarget); approveTargetId = null }) {
                        Text("APROVAR", color = Blue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { approveTargetId = null }) { Text("CANCELAR", color = TxtGray) }
                }
            )
        }

        // Diálogo Rejeitar
        if (rejectTarget != null) {
            AlertDialog(
                onDismissRequest = { rejectTargetId = null },
                containerColor = DkSurface,
                title = { Text("Rejeitar pedido?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text  = { Text("Esta ação não pode ser desfeita.", color = TxtGray, fontSize = 14.sp) },
                confirmButton = {
                    TextButton(onClick = { reject(rejectTarget); rejectTargetId = null }) {
                        Text("REJEITAR", color = RedBadge, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rejectTargetId = null }) { Text("CANCELAR", color = TxtGray) }
                }
            )
        }

        // Diálogo Aceitar Troca (target employee)
        if (acceptSwap != null) {
            AlertDialog(
                onDismissRequest = { acceptSwapId = null },
                containerColor = DkSurface,
                title = { Text("Aceitar troca de turno?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text  = { Text("O pedido será enviado ao gestor para aprovação final.", color = TxtGray, fontSize = 14.sp) },
                confirmButton = {
                    TextButton(onClick = { respondToSwap(acceptSwap, true); acceptSwapId = null }) {
                        Text("ACEITAR", color = DkGreen, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { acceptSwapId = null }) { Text("CANCELAR", color = TxtGray) }
                }
            )
        }

        // Diálogo Recusar Troca (target employee)
        if (refuseSwap != null) {
            AlertDialog(
                onDismissRequest = { refuseSwapId = null },
                containerColor = DkSurface,
                title = { Text("Recusar troca de turno?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text  = { Text("O pedido será cancelado.", color = TxtGray, fontSize = 14.sp) },
                confirmButton = {
                    TextButton(onClick = { respondToSwap(refuseSwap, false); refuseSwapId = null }) {
                        Text("RECUSAR", color = RedBadge, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { refuseSwapId = null }) { Text("CANCELAR", color = TxtGray) }
                }
            )
        }
    }
}

private val dateFmtGroupHeader = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale("pt", "PT"))

private fun createdLocalDate(createdAt: String): LocalDate? = runCatching {
    Instant.parse(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
}.getOrNull() ?: runCatching { LocalDate.parse(createdAt.take(10)) }.getOrNull()

private fun createdTimeStr(createdAt: String): String = runCatching {
    LocalDateTime.ofInstant(Instant.parse(createdAt), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault("")

// ── Lista ─────────────────────────────────────────────────────────────────────
@Composable
private fun NotificationsList(
    notifications: List<NotifItem>,
    isManager: Boolean,
    onViewDetails: (NotifItem) -> Unit,
    onApprove: (NotifItem) -> Unit,
    onReject: (NotifItem) -> Unit,
    onAccept: (NotifItem) -> Unit,
    onRefuse: (NotifItem) -> Unit
) {
    val pendingCount = notifications.count { it.status == NotifStatus.PENDENTE }

    // Agrupa por data de criação (mais recente primeiro)
    val grouped = notifications
        .groupBy { createdLocalDate(it.createdAt) ?: LocalDate.MIN }
        .entries
        .sortedByDescending { it.key }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Notificações",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (pendingCount > 0) {
                    Text(
                        "Ler tudo ($pendingCount)",
                        color = Blue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sem pedidos por agora.", color = TxtGray, fontSize = 14.sp)
                }
            }
        }

        grouped.forEach { (date, group) ->
            item {
                val dateLabel = runCatching {
                    date.format(dateFmtGroupHeader)
                        .replaceFirstChar { it.uppercaseChar() }
                }.getOrDefault(date.toString())
                Text(
                    dateLabel,
                    color = TxtGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            items(group) { notif ->
                NotifCard(
                    item = notif,
                    onViewDetails = { onViewDetails(notif) },
                    onApprove = { onApprove(notif) },
                    onReject  = { onReject(notif) },
                    onAccept  = { onAccept(notif) },
                    onRefuse  = { onRefuse(notif) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Cartão ────────────────────────────────────────────────────────────────────
@Composable
private fun NotifCard(
    item: NotifItem,
    onViewDetails: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onAccept: () -> Unit,
    onRefuse: () -> Unit
) {
    val isProcessed = item.status != NotifStatus.PENDENTE

    val typeLabel = when {
        item.status == NotifStatus.REJEITADO && item.type == NotifType.PEDIDO_DE_TROCA -> "TROCA REJEITADA"
        item.status == NotifStatus.REJEITADO                                            -> "FOLGA REJEITADA"
        item.status == NotifStatus.APROVADO  && item.type == NotifType.PEDIDO_DE_TROCA -> "TROCA APROVADA"
        item.status == NotifStatus.APROVADO                                             -> "FOLGA APROVADA"
        item.type   == NotifType.PEDIDO_DE_TROCA                                       -> "PEDIDO DE TROCA"
        else                                                                            -> "PEDIDO DE FOLGA"
    }
    val typeLabelColor = when (item.status) {
        NotifStatus.APROVADO  -> DkGreen
        NotifStatus.REJEITADO -> Orange
        else -> if (item.type == NotifType.PEDIDO_DE_TROCA) Color(0xFF9B59B6) else Blue
    }

    val timeStr = createdTimeStr(item.createdAt)

    val dividerColor = Color(0xFF2C2C2E)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DkSurface)
            .alpha(if (isProcessed) 0.55f else 1f)
    ) {
        // ── Cabeçalho: tipo + hora ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                typeLabel,
                color = typeLabelColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(timeStr, color = TxtGray, fontSize = 12.sp)
        }

        // ── Corpo ─────────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            Text(
                item.shiftDateLabel,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (item.schedule.isNotBlank()) {
                Text(item.schedule, color = TxtGray, fontSize = 14.sp)
            }
            if (item.description.isNotBlank()) {
                Text(item.description, color = TxtGray, fontSize = 13.sp)
            }
            if (item.employeeName.isNotBlank()) {
                Text("Funcionário: ${item.employeeName}", color = TxtGray, fontSize = 13.sp)
            }
            if (isProcessed && !item.approvedByName.isNullOrBlank()) {
                val actionLabel = if (item.status == NotifStatus.REJEITADO) "Rejeitado por:" else "Aprovado por:"
                Text(
                    "$actionLabel ${item.approvedByName}",
                    color = TxtGray.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Botões flat com divisores ─────────────────────────────────────────
        HorizontalDivider(color = dividerColor)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewDetails() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Ver detalhes", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        if (!isProcessed) {
            if (item.isTargetResponse) {
                HorizontalDivider(color = dividerColor)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRefuse() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Recusar", color = RedBadge, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = dividerColor)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAccept() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aceitar", color = DkGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            } else if (!item.isOwnRequest) {
                HorizontalDivider(color = dividerColor)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReject() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Rejeitar", color = RedBadge, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = dividerColor)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onApprove() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aprovar", color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Ecrã de detalhe ───────────────────────────────────────────────────────────
@Composable
private fun NotificationDetailScreen(
    item: NotifItem,
    isManager: Boolean,
    onBack: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onAccept: () -> Unit,
    onRefuse: () -> Unit
) {
    val title = if (item.type == NotifType.PEDIDO_DE_TROCA) "Pedido de Troca" else "Pedido de Folga"
    val isPending  = item.status == NotifStatus.PENDENTE
    val isRejected = item.status == NotifStatus.REJEITADO

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, null, tint = Blue,
                    modifier = Modifier.size(22.dp).clickable { onBack() })
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
                    if (item.description.isNotBlank()) {
                        HorizontalDivider(color = Color(0xFF2C2C2E))
                        DetailRow("Motivo", item.description)
                    }
                    HorizontalDivider(color = Color(0xFF2C2C2E))
                    DetailRow("Data", item.shiftDateLabel)
                    HorizontalDivider(color = Color(0xFF2C2C2E))
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
                            Icon(Icons.Filled.KeyboardArrowDown, null, tint = TxtGray, modifier = Modifier.size(18.dp))
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Estado", color = Color.White, fontSize = 15.sp)
                        val (statusText, statusColor) = when (item.status) {
                            NotifStatus.PENDENTE  -> "Pendente" to Orange
                            NotifStatus.APROVADO  -> "Aprovado" to DkGreen
                            NotifStatus.REJEITADO -> "Rejeitado" to RedBadge
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(statusText, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }

            item {
                Text(
                    "NOTAS",
                    color = TxtGray, fontSize = 12.sp, fontWeight = FontWeight.Bold,
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
                    val submitAction = if (item.type == NotifType.PEDIDO_DE_TROCA)
                        "Submeteu o pedido de troca" else "Submeteu o pedido de folga"
                    NoteEntry(name = item.employeeName, action = submitAction)

                    if (item.status != NotifStatus.PENDENTE) {
                        HorizontalDivider(color = Color(0xFF2C2C2E))
                        val managerAction = when {
                            item.status == NotifStatus.APROVADO && item.type == NotifType.PEDIDO_DE_TROCA -> "Aprovou o pedido de troca"
                            item.status == NotifStatus.APROVADO -> "Aprovou o pedido de folga"
                            item.type   == NotifType.PEDIDO_DE_TROCA -> "Rejeitou o pedido de troca"
                            else -> "Rejeitou o pedido de folga"
                        }
                        NoteEntry(
                            name = item.approvedByName.takeUnless { it.isNullOrBlank() } ?: "Gestor",
                            action = managerAction
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
                Spacer(Modifier.weight(1f))
                if (isPending) {
                    if (item.isTargetResponse) {
                        Button(
                            onClick = onRefuse,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedBadge.copy(alpha = 0.15f),
                                contentColor = RedBadge
                            )
                        ) { Text("Recusar", fontSize = 15.sp) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onAccept,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DkGreen)
                        ) { Text("Aceitar ✓", color = Color.White, fontSize = 15.sp) }
                    } else {
                        Button(
                            onClick = onReject,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedBadge.copy(alpha = 0.15f),
                                contentColor = RedBadge
                            )
                        ) { Text("Rejeitar", fontSize = 15.sp) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onApprove,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue)
                        ) { Text("Aprovar ✓", color = Color.White, fontSize = 15.sp) }
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
private fun NoteEntry(name: String, action: String) {
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
                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(action, color = TxtGray, fontSize = 13.sp)
        }
    }
}
