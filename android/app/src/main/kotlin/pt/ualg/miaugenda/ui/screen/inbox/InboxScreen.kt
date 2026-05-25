package pt.ualg.miaugenda.ui.screen.inbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.MiauGendaApp
import kotlinx.coroutines.async
import pt.ualg.miaugenda.data.model.Channel
import pt.ualg.miaugenda.data.model.ChannelMessage
import pt.ualg.miaugenda.data.model.CreateChannelRequest
import pt.ualg.miaugenda.data.model.SendMessageRequest
import pt.ualg.miaugenda.data.model.User
import pt.ualg.miaugenda.data.remote.RetrofitClient
import pt.ualg.miaugenda.ui.components.AppBottomNav
import pt.ualg.miaugenda.ui.components.NavTab
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.window.Dialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DkBg       = Color(0xFF000000)
private val DkSurface  = Color(0xFF1C1C1E)
private val DkSurface2 = Color(0xFF2C2C2E)
private val Blue       = Color(0xFF2979FF)
private val TxtGray    = Color(0xFF8E8E93)

private val channelColors = listOf(
    Color(0xFF5B5FEF), Color(0xFFE53935), Color(0xFF1A237E),
    Color(0xFF00838F), Color(0xFF2E7D32), Color(0xFF6A1B9A),
    Color(0xFFBF360C), Color(0xFF4527A0)
)

private val avatarColors = listOf(
    Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A),
    Color(0xFF00838F), Color(0xFFBF360C), Color(0xFF4E342E),
    Color(0xFF4527A0), Color(0xFF00695C)
)

private sealed class InboxSubScreen {
    object Main : InboxSubScreen()
    object BrowseChannels : InboxSubScreen()
    data class ChatRoom(
        val channelId: Int,
        val channelName: String,
        val channelType: String = "GROUP",
        val createdById: Int = 0
    ) : InboxSubScreen()
}

private const val MESSAGE_REFRESH_MS = 1000L
private const val INBOX_REFRESH_MS = 1000L

@Composable
fun InboxScreen(
    onSchedulerClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onEquipaClick: () -> Unit = {},
    dmWithUserId: Int? = null
) {
    val context = LocalContext.current
    val tokenManager = MiauGendaApp.getTokenManager(context)
    val currentUserId = tokenManager.getUserId()
    val currentUserRole = tokenManager.getUserRole() ?: "EMPLOYEE"

    var screen by remember { mutableStateOf<InboxSubScreen>(InboxSubScreen.Main) }

    // Auto-abrir DM quando navegado com um userId específico
    LaunchedEffect(dmWithUserId) {
        if (dmWithUserId != null && dmWithUserId > 0) {
            val channel = findOrCreateDmChannel(currentUserId, dmWithUserId)
            if (channel != null) {
                // Obter o nome do utilizador para mostrar como título da conversa
                val displayName = try {
                    RetrofitClient.userApi.getUsers().body()
                        ?.find { it.id == dmWithUserId }?.name ?: channel.name
                } catch (_: Exception) { channel.name }
                screen = InboxSubScreen.ChatRoom(channel.id, displayName, channel.type, channel.createdById)
            }
        }
    }

    BackHandler(enabled = screen !is InboxSubScreen.Main) {
        screen = InboxSubScreen.Main
    }

    Box(modifier = Modifier.fillMaxSize().background(DkBg).systemBarsPadding()) {
        when (val s = screen) {
            is InboxSubScreen.Main -> InboxMain(
                currentUserId        = currentUserId,
                onBrowse             = { screen = InboxSubScreen.BrowseChannels },
                onChannelClick       = { channel -> screen = InboxSubScreen.ChatRoom(channel.id, channel.name, channel.type, channel.createdById) },
                onSchedulerClick     = onSchedulerClick,
                onNotificationsClick = onNotificationsClick,
                onHomeClick          = onHomeClick,
                onEquipaClick        = onEquipaClick
            )
            is InboxSubScreen.BrowseChannels -> BrowseChannelsScreen(
                currentUserId    = currentUserId,
                currentUserRole  = currentUserRole,
                onBack           = { screen = InboxSubScreen.Main },
                onSelectChannel  = { channel, displayName -> screen = InboxSubScreen.ChatRoom(channel.id, displayName, channel.type, channel.createdById) }
            )
            is InboxSubScreen.ChatRoom -> ChatRoomScreen(
                channelId       = s.channelId,
                channelName     = s.channelName,
                channelType     = s.channelType,
                createdById     = s.createdById,
                currentUserId   = currentUserId,
                currentUserRole = currentUserRole,
                onBack          = { screen = InboxSubScreen.Main }
            )
        }
    }
}

// ── Ecrã principal ────────────────────────────────────────────────────────────

@Composable
private fun InboxMain(
    currentUserId: Int,
    onBrowse: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    onSchedulerClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onHomeClick: () -> Unit,
    onEquipaClick: () -> Unit
) {
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload(showLoading: Boolean = false) {
        scope.launch {
            if (showLoading) {
                isLoading = true
            }

            errorMsg = null

            try {
                val resp = RetrofitClient.messagingApi.getChannels()

                when {
                    resp.isSuccessful -> channels = resp.body().orEmpty()
                    resp.code() == 401 -> errorMsg = "Sessão expirada. Faz login novamente."
                    else -> errorMsg = "Erro do servidor (${resp.code()})"
                }
            } catch (e: Exception) {
                errorMsg = "Sem ligação ao servidor"
            } finally {
                if (showLoading) {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        reload(showLoading = true)

        while (true) {
            delay(INBOX_REFRESH_MS)
            reload(showLoading = false)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Caixa", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Blue).clickable { onBrowse() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Procurar", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        when {
            isLoading -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue)
            }
            errorMsg != null -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = TxtGray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMsg!!, color = TxtGray, fontSize = 15.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { reload() }, colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                        Text("Tentar novamente")
                    }
                }
            }
            channels.isEmpty() -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = TxtGray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Sem canais disponíveis", color = TxtGray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Cria um canal para começar a comunicar", color = TxtGray, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
            else -> LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(channels, key = { it.id }) { ch ->
                    ChannelRow(channel = ch, onClick = { onChannelClick(ch) })
                    Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 80.dp).background(Color.White.copy(alpha = 0.07f)))
                }
            }
        }

        // Linha "Outros" — sempre visível
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onBrowse() }.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1C1C3A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = Blue, modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Outros", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Procurar canais ou enviar mensagem privada", color = TxtGray, fontSize = 13.sp, maxLines = 1)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
        }

        AppBottomNav(
            active               = NavTab.INBOX,
            onHomeClick          = onHomeClick,
            onSchedulerClick     = onSchedulerClick,
            onNotificationsClick = onNotificationsClick,
            onMenuClick          = onEquipaClick
        )
    }
}

@Composable
private fun ChannelRow(channel: Channel, onClick: () -> Unit) {
    val lastMsg = channel.messages.firstOrNull()
    val bgColor = channelColors[channel.id % channelColors.size]

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                channel.name.first().uppercaseChar().toString(),
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(channel.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (lastMsg != null) {
                Text(
                    "${lastMsg.user.name}: ${lastMsg.content}",
                    color = TxtGray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(channel.description ?: "Sem mensagens", color = TxtGray, fontSize = 13.sp, maxLines = 1)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (lastMsg != null) {
                Text(formatMessageTime(lastMsg.createdAt), color = TxtGray, fontSize = 11.sp)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Procurar Canais / Mensagem Privada ────────────────────────────────────────

private suspend fun findOrCreateDmChannel(currentUserId: Int, targetUserId: Int): Channel? {
    val dmName = "dm-${minOf(currentUserId, targetUserId)}-${maxOf(currentUserId, targetUserId)}"
    val createResp = RetrofitClient.messagingApi.createChannel(
        CreateChannelRequest(name = dmName, description = null, isPublic = false, type = "DM", memberIds = listOf(targetUserId))
    )
    if (createResp.isSuccessful) return createResp.body()
    if (createResp.code() == 409) {
        val allResp = RetrofitClient.messagingApi.getChannels()
        if (allResp.isSuccessful) return allResp.body()?.find { it.name == dmName }
    }
    return null
}

@Composable
private fun BrowseChannelsScreen(
    currentUserId: Int,
    currentUserRole: String,
    onBack: () -> Unit,
    onSelectChannel: (Channel, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var createStep by remember { mutableStateOf<String?>(null) } // null | "type" | "group" | "announcement"
    var newChannelName by remember { mutableStateOf("") }
    var newChannelDesc by remember { mutableStateOf("") }
    var selectedMemberIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var addAllMembers by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    var openingDm by remember { mutableStateOf(false) }

    fun resetCreate() {
        createStep = null; newChannelName = ""; newChannelDesc = ""
        selectedMemberIds = emptySet(); addAllMembers = false; createError = null
    }
    val scope = rememberCoroutineScope()
    val canCreate = currentUserRole == "ADMIN" || currentUserRole == "MANAGER"

    LaunchedEffect(Unit) {
        try {
            val chDeferred = scope.async { RetrofitClient.messagingApi.getChannels() }
            val usDeferred = scope.async { RetrofitClient.userApi.getUsers() }
            val chR = chDeferred.await()
            val usR = usDeferred.await()
            if (chR.isSuccessful) channels = chR.body().orEmpty()
            if (usR.isSuccessful) users = usR.body().orEmpty().filter { it.id != currentUserId }
        } catch (_: Exception) {}
        isLoading = false
    }

    val filteredChannels = remember(channels, query) {
        if (query.isBlank()) channels else channels.filter { it.name.contains(query, ignoreCase = true) }
    }
    val filteredUsers = remember(users, query) {
        if (query.isBlank()) users else users.filter { it.name.contains(query, ignoreCase = true) }
    }

    // ── Passo 1: escolher tipo ──────────────────────────────────────────────────
    if (createStep == "type") {
        AlertDialog(
            onDismissRequest = { createStep = null },
            containerColor = DkSurface,
            title = { Text("Novo Canal", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(DkSurface2).clickable { createStep = "group" }.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Group, null, tint = Blue, modifier = Modifier.size(26.dp))
                        Column {
                            Text("Grupo", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Chat com membros específicos", color = TxtGray, fontSize = 12.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(DkSurface2).clickable { createStep = "announcement" }.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Announcement, null, tint = Color(0xFFFF8F00), modifier = Modifier.size(26.dp))
                        Column {
                            Text("Canal de Anúncios", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Difundir mensagens à equipa", color = TxtGray, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { createStep = null }) { Text("Cancelar", color = TxtGray) }
            }
        )
    }

    // ── Passo 2: formulário de criação ──────────────────────────────────────────
    if (createStep == "group" || createStep == "announcement") {
        val isAnnouncement = createStep == "announcement"
        Dialog(onDismissRequest = { resetCreate() }) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DkSurface)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    if (isAnnouncement) "Canal de Anúncios" else "Novo Grupo",
                    color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = newChannelName, onValueChange = { newChannelName = it },
                    label = { Text(if (isAnnouncement) "Nome do canal" else "Nome do grupo", color = TxtGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue, unfocusedBorderColor = DkSurface2,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newChannelDesc, onValueChange = { newChannelDesc = it },
                    label = { Text("Detalhes (opcional)", color = TxtGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue, unfocusedBorderColor = DkSurface2,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                if (isAnnouncement) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Adicionar todos os membros", color = Color.White, fontSize = 14.sp)
                        Switch(
                            checked = addAllMembers, onCheckedChange = { addAllMembers = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Blue, checkedTrackColor = Blue.copy(alpha = 0.4f))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (!addAllMembers || !isAnnouncement) {
                    Text(
                        if (isAnnouncement) "Ou selecionar membros" else "Adicionar membros",
                        color = TxtGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        users.forEach { user ->
                            val checked = user.id in selectedMemberIds
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        selectedMemberIds = if (checked)
                                            selectedMemberIds - user.id else selectedMemberIds + user.id
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        selectedMemberIds = if (it)
                                            selectedMemberIds + user.id else selectedMemberIds - user.id
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Blue, uncheckedColor = TxtGray)
                                )
                                Text(user.name, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
                createError?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = Color(0xFFFF453A), fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { resetCreate() }) { Text("Cancelar", color = TxtGray) }
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        enabled = newChannelName.isNotBlank() && !isCreating,
                        onClick = {
                            isCreating = true; createError = null
                            scope.launch {
                                try {
                                    val members = if (isAnnouncement && addAllMembers)
                                        users.map { it.id }
                                    else
                                        selectedMemberIds.toList()
                                    val resp = RetrofitClient.messagingApi.createChannel(
                                        CreateChannelRequest(
                                            name = newChannelName.trim(),
                                            description = newChannelDesc.trim().ifBlank { null },
                                            type = if (isAnnouncement) "ANNOUNCEMENT" else "GROUP",
                                            memberIds = members
                                        )
                                    )
                                    if (resp.isSuccessful) {
                                        resp.body()?.let { onSelectChannel(it, it.name) }
                                        resetCreate()
                                    } else if (resp.code() == 409) createError = "Já existe um canal com esse nome"
                                    else createError = "Erro ao criar canal (${resp.code()})"
                                } catch (_: Exception) { createError = "Sem ligação ao servidor" }
                                isCreating = false
                            }
                        }
                    ) {
                        Text(if (isCreating) "A criar..." else "Criar", color = Blue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("Cancelar", color = Blue, fontSize = 16.sp) }
            Text("Procurar Canais", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(80.dp))
        }

        // Barra de pesquisa
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp)).background(DkSurface)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
                Box {
                    if (query.isEmpty()) Text("Pesquisar membros da equipa", color = TxtGray, fontSize = 15.sp)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(Blue), modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                // Criar novo canal (ADMIN/MANAGER)
                if (canCreate) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { createStep = "type" }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Blue), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Text("Criar novo canal de grupo", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 80.dp).background(Color.White.copy(alpha = 0.07f)))
                    }
                }

                // Secção "Sugeridos" — canais
                if (filteredChannels.isNotEmpty()) {
                    item {
                        Text("Sugeridos", color = TxtGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth().background(DkSurface2).padding(horizontal = 20.dp, vertical = 8.dp))
                    }
                    items(filteredChannels, key = { "ch-${it.id}" }) { ch ->
                        val bgColor = channelColors[ch.id % channelColors.size]
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelectChannel(ch, ch.name) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(bgColor), contentAlignment = Alignment.Center) {
                                Text(ch.name.first().uppercaseChar().toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ch.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                ch.description?.let { Text(it, color = TxtGray, fontSize = 13.sp, maxLines = 1) }
                            }
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 80.dp).background(Color.White.copy(alpha = 0.07f)))
                    }
                }

                // Secção de utilizadores para mensagem privada
                if (filteredUsers.isNotEmpty()) {
                    item {
                        Text("Membros da equipa", color = TxtGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth().background(DkSurface2).padding(horizontal = 20.dp, vertical = 8.dp))
                    }
                    items(filteredUsers, key = { "user-${it.id}" }) { user ->
                        val color = avatarColors[user.id % avatarColors.size]
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable(enabled = !openingDm) {
                                    openingDm = true
                                    scope.launch {
                                        try {
                                            val ch = findOrCreateDmChannel(currentUserId, user.id)
                                            ch?.let { onSelectChannel(it, user.name) }
                                        } catch (_: Exception) {}
                                        openingDm = false
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(user.name.first().uppercaseChar().toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(user.name, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (openingDm) {
                                CircularProgressIndicator(color = Blue, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 80.dp).background(Color.White.copy(alpha = 0.07f)))
                    }
                }
            }
        }
    }
}

// ── Ecrã de Chat ──────────────────────────────────────────────────────────────

@Composable
private fun ChatRoomScreen(
    channelId: Int,
    channelName: String,
    channelType: String = "GROUP",
    createdById: Int = 0,
    currentUserId: Int,
    currentUserRole: String = "EMPLOYEE",
    onBack: () -> Unit
) {
    val isAdmin = currentUserRole == "ADMIN"
    val isEmployee = currentUserRole == "EMPLOYEE"
    val isAnnouncement = channelType == "ANNOUNCEMENT"
    // Regras de eliminação: admin pode apagar tudo; outros só podem apagar DMs ou grupos que criaram
    // exceto "Todos" (nome especial) e canais de anúncios
    val canDelete = when {
        isAdmin -> true
        channelName == "Todos" -> false
        isAnnouncement -> false
        channelType == "DM" -> true
        createdById == currentUserId -> true
        else -> false
    }
    // EMPLOYEE não pode enviar mensagens em canais de anúncios
    val canSend = !(isEmployee && isAnnouncement)
    var messages by remember { mutableStateOf<List<ChannelMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    suspend fun loadMessages() {
        try {
            val resp = RetrofitClient.messagingApi.getMessages(channelId)
            if (resp.isSuccessful) messages = resp.body().orEmpty()
        } catch (_: Exception) {}
    }

    LaunchedEffect(channelId) {
        loadMessages()
        while (true) {
            delay(1000)
            loadMessages()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clip(CircleShape).clickable { onBack() }.padding(4.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                channelName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            Box {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(DkSurface2).clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.MoreHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(DkSurface)) {
                    DropdownMenuItem(
                        text = { Text("Detalhes do canal", color = Color.White, fontSize = 15.sp) },
                        onClick = { showMenu = false }
                    )
                    if (canDelete) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                        DropdownMenuItem(
                            text = { Text("Apagar conversa", color = Color(0xFFFF3B30), fontSize = 15.sp) },
                            onClick = { showMenu = false; showDeleteDialog = true }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

        if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = TxtGray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sem mensagens ainda", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Envia a primeira mensagem para o canal!", color = TxtGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(message = msg, isOwn = msg.userId == currentUserId)
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = DkSurface,
                title = { Text("Apagar conversa?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text = { Text("O canal é apagado para todos. As mensagens ficam guardadas na base de dados.", color = TxtGray, fontSize = 14.sp) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            try {
                                val r = RetrofitClient.messagingApi.deleteChannel(channelId)
                                if (r.isSuccessful) onBack()
                            } catch (_: Exception) {}
                        }
                    }) { Text("APAGAR", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("CANCELAR", color = TxtGray) }
                }
            )
        }

        if (!canSend) {
            Box(
                modifier = Modifier.fillMaxWidth().background(DkSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(0.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Apenas gestores podem enviar mensagens neste canal.", color = TxtGray, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().background(DkSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(0.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(DkSurface2)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (messageText.isEmpty()) Text("Enviar uma mensagem...", color = TxtGray, fontSize = 15.sp)
                        BasicTextField(
                            value = messageText, onValueChange = { messageText = it },
                            singleLine = false, maxLines = 4,
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            cursorBrush = SolidColor(Blue), modifier = Modifier.fillMaxWidth()
                        )
                    }
                    val canSendMsg = messageText.isNotBlank() && !isSending
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(if (canSendMsg) Blue else DkSurface2)
                            .clickable(enabled = canSendMsg) {
                                val content = messageText.trim()
                                messageText = ""
                                isSending = true
                                scope.launch {
                                    try {
                                        val resp = RetrofitClient.messagingApi.sendMessage(channelId, SendMessageRequest(content))
                                        if (resp.isSuccessful) resp.body()?.let { messages = messages + it }
                                    } catch (_: Exception) {}
                                    isSending = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Enviar",
                            tint = if (canSendMsg) Color.White else TxtGray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChannelMessage, isOwn: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        if (!isOwn) {
            val initial = message.user.name.first().uppercaseChar()
            val color = avatarColors[message.userId % avatarColors.size]
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(message.user.name, color = Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
                    Box(
                        modifier = Modifier.widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                            .background(DkSurface).padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(message.content, color = Color.White, fontSize = 15.sp, lineHeight = 20.sp)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(Blue).padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(message.content, color = Color.White, fontSize = 15.sp, lineHeight = 20.sp)
            }
        }
        Text(
            formatMessageTime(message.createdAt),
            color = TxtGray, fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp, start = if (isOwn) 0.dp else 36.dp, end = 4.dp)
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatMessageTime(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val now = Instant.now()
        val zoned = instant.atZone(ZoneId.systemDefault())
        when {
            ChronoUnit.MINUTES.between(instant, now) < 1 -> "agora"
            ChronoUnit.HOURS.between(instant, now) < 24 -> zoned.format(DateTimeFormatter.ofPattern("HH:mm"))
            ChronoUnit.DAYS.between(instant, now) < 7 -> zoned.format(DateTimeFormatter.ofPattern("EEE HH:mm"))
            else -> zoned.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
        }
    } catch (_: Exception) {
        ""
    }
}
