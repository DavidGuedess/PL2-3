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
import java.time.LocalDate
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
    val isTargetResponse: Boolean = false
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
        requestCategory = "timeOff"
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
        isTargetResponse = isTargetResponse
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

                val filteredSwaps = if (isManager) {
                    // gestor vê pedidos onde o target já aceitou (aguarda aprovação) ou já processados
                    rawSwaps.filter { it.targetAccepted == true || it.status != "PENDING" }
                } else {
                    // funcionário vê apenas pedidos onde é o target e ainda não respondeu
                    rawSwaps.filter { it.targetShift?.user?.id == currentUserId && it.targetAccepted == null }
                }

                val swapItems = filteredSwaps.map { ssrToNotifItem(it, currentUserId) }

                val torItems = if (isManager) {
                    RetrofitClient.requestApi.getTimeOffRequests().body().orEmpty().map { torToNotifItem(it) }
                } else {
                    emptyList()
                }

                notifications = (torItems + swapItems).sortedByDescending { it.id }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(Unit) { loadNotifications() }

    val pendingCount = notifications.count { it.status == NotifStatus.PENDENTE }
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
                    notifications = notifications.map {
                        if (it.id == item.id && it.requestCategory == item.requestCategory)
                            it.copy(status = NotifStatus.APROVADO, approvedByName = currentUserName)
                        else it
                    }
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
                    notifications = notifications.map {
                        if (it.id == item.id && it.requestCategory == item.requestCategory)
                            it.copy(status = NotifStatus.REJEITADO, approvedByName = currentUserName)
                        else it
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun respondToSwap(item: NotifItem, accept: Boolean) {
        scope.launch {
            try {
                val r = RetrofitClient.requestApi.respondToSwapRequest(item.id, mapOf("accept" to accept))
                if (r.isSuccessful) {
                    // Remove item da lista — já não precisa de resposta
                    notifications = notifications.filter { it.id != item.id || it.requestCategory != "swap" }
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
                        pendingCount  = pendingCount,
                        isManager     = isManager,
                        onReadAll     = { notifications = notifications.map { it.copy(isRead = true) } },
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

// ── Lista ─────────────────────────────────────────────────────────────────────
@Composable
private fun NotificationsList(
    notifications: List<NotifItem>,
    pendingCount: Int,
    isManager: Boolean,
    onReadAll: () -> Unit,
    onViewDetails: (NotifItem) -> Unit,
    onApprove: (NotifItem) -> Unit,
    onReject: (NotifItem) -> Unit,
    onAccept: (NotifItem) -> Unit,
    onRefuse: (NotifItem) -> Unit
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
                if (isManager) "Todos os pedidos" else "Pedidos de troca para si",
                color = TxtGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
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
        items(notifications) { notif ->
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
    val typeLabel = if (item.type == NotifType.PEDIDO_DE_TROCA) "PEDIDO DE TROCA" else "PEDIDO DE FOLGA"
    val typeColor = if (item.type == NotifType.PEDIDO_DE_TROCA) Color(0xFF9B59B6) else Blue

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DkSurface)
            .alpha(if (item.isRead) 0.45f else 1f)
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

                if (item.isTargetResponse) {
                    // Botões para o target do swap: Aceitar / Recusar
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
                        ) { Text("Ver detalhes", fontSize = 11.sp) }
                        Button(
                            onClick = onRefuse,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedBadge.copy(alpha = 0.15f),
                                contentColor = RedBadge
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) { Text("Recusar", fontSize = 11.sp) }
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DkGreen.copy(alpha = 0.15f),
                                contentColor = DkGreen
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) { Text("Aceitar", fontSize = 11.sp) }
                    }
                } else {
                    // Botões para gestor: Rejeitar / Aprovar
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
                        ) { Text("Ver detalhes", fontSize = 11.sp) }
                        Button(
                            onClick = onReject,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedBadge.copy(alpha = 0.15f),
                                contentColor = RedBadge
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) { Text("Rejeitar", fontSize = 11.sp) }
                        Button(
                            onClick = onApprove,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Blue.copy(alpha = 0.15f),
                                contentColor = Blue
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) { Text("Aprovar", fontSize = 11.sp) }
                    }
                }
            }

            NotifStatus.APROVADO, NotifStatus.REJEITADO -> {
                val isRejected = item.status == NotifStatus.REJEITADO
                val actionMsg = when {
                    isRejected && item.type == NotifType.PEDIDO_DE_TROCA -> "Recusou/rejeitou este pedido de troca."
                    isRejected                                             -> "Rejeitou este pedido de folga."
                    item.type  == NotifType.PEDIDO_DE_TROCA               -> "Aprovou este pedido de troca."
                    else                                                   -> "Aprovou este pedido de folga."
                }
                val pillLabel = when {
                    isRejected && item.type == NotifType.PEDIDO_DE_TROCA -> "TROCA REJEITADA"
                    isRejected                                             -> "PEDIDO REJEITADO"
                    item.type  == NotifType.PEDIDO_DE_TROCA               -> "TROCA APROVADA"
                    else                                                   -> "PEDIDO DE FOLGA APROVADO"
                }
                val pillColor = if (isRejected) RedBadge.copy(alpha = 0.8f) else DkGreen
                val byLabel   = if (isRejected) "Rejeitado por:" else "Aprovado por:"

                Text(actionMsg, color = TxtGray, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(pillColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(pillLabel, color = pillColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (!item.approvedByName.isNullOrBlank()) {
                    Text("$byLabel ${item.approvedByName}", color = TxtGray, fontSize = 13.sp)
                }
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
