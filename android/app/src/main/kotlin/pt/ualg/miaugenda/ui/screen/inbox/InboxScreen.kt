package pt.ualg.miaugenda.ui.screen.inbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ualg.miaugenda.ui.components.AppBottomNav
import pt.ualg.miaugenda.ui.components.NavTab

// ── Cores ─────────────────────────────────────────────────────────────────────
private val DkBg       = Color(0xFF000000)
private val DkSurface  = Color(0xFF1C1C1E)
private val DkSurface2 = Color(0xFF2C2C2E)
private val Blue       = Color(0xFF2979FF)
private val TxtGray    = Color(0xFF8E8E93)
private val AvatarGrad = Brush.linearGradient(listOf(Color(0xFF5B5FEF), Color(0xFFC850C0), Color(0xFFF0696B)))

// ── Navegação interna ─────────────────────────────────────────────────────────
private sealed class InboxSubScreen {
    object Main : InboxSubScreen()
    object NewChat : InboxSubScreen()
    data class ChatRoom(val name: String, val subtitle: String) : InboxSubScreen()
}

// ── Dados mock ────────────────────────────────────────────────────────────────
private data class Channel(
    val name: String,
    val subtitle: String,
    val iconBg: Color,
    val icon: ImageVector
)

private val channels = listOf(
    Channel("Todos",     "Canal público para todos os funcionários", Color(0xFF5B5FEF), Icons.Outlined.Group),
    Channel("Anúncios",  "Para anúncios da gestão",                  Color(0xFFE53935), Icons.Outlined.Campaign),
    Channel("Outros",    "Ver canais ou criar um novo",               Color(0xFF1A237E), Icons.Outlined.Search),
)

private data class Member(val name: String, val initials: String, val color: Color)

private val members = listOf(
    Member("Administrador 1", "A1", Color(0xFF1565C0)),
    Member("Administrador 2", "A2", Color(0xFF2E7D32)),
    Member("Gerente 1",       "G1", Color(0xFF6A1B9A)),
    Member("Gerente 2",       "G2", Color(0xFF00838F)),
    Member("Funcionario 1",   "F1", Color(0xFFBF360C)),
    Member("Funcionario 2",   "F2", Color(0xFF4E342E)),
)

// ── Root ──────────────────────────────────────────────────────────────────────
@Composable
fun InboxScreen(
    onSchedulerClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    var screen by remember { mutableStateOf<InboxSubScreen>(InboxSubScreen.Main) }

    BackHandler(enabled = screen !is InboxSubScreen.Main) {
        screen = InboxSubScreen.Main
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DkBg)
            .systemBarsPadding()
    ) {
        when (val s = screen) {
            is InboxSubScreen.Main -> InboxMain(
                onNewChat       = { screen = InboxSubScreen.NewChat },
                onChannelClick  = { ch ->
                    if (ch.name == "Outros") screen = InboxSubScreen.NewChat
                    else screen = InboxSubScreen.ChatRoom(ch.name, ch.subtitle)
                },
                onSchedulerClick      = onSchedulerClick,
                onNotificationsClick  = onNotificationsClick,
                onHomeClick           = onHomeClick
            )
            is InboxSubScreen.NewChat -> NewChatScreen(
                onBack        = { screen = InboxSubScreen.Main },
                onSelectChat  = { name, sub -> screen = InboxSubScreen.ChatRoom(name, sub) }
            )
            is InboxSubScreen.ChatRoom -> ChatRoomScreen(
                name     = s.name,
                subtitle = s.subtitle,
                onBack   = { screen = InboxSubScreen.Main }
            )
        }
    }
}

// ── Ecrã principal da Caixa ───────────────────────────────────────────────────
@Composable
private fun InboxMain(
    onNewChat: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    onSchedulerClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Caixa",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Blue)
                    .clickable { onNewChat() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Novo chat", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        // Lista de canais
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
        ) {
            items(channels) { ch ->
                ChannelRow(
                    channel  = ch,
                    onClick  = { onChannelClick(ch) }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(start = 80.dp)
                        .background(Color.White.copy(alpha = 0.07f))
                )
            }
        }

        AppBottomNav(
            active               = NavTab.INBOX,
            onSchedulerClick     = onSchedulerClick,
            onNotificationsClick = onNotificationsClick,
            onHomeClick          = onHomeClick
        )
    }
}

@Composable
private fun ChannelRow(channel: Channel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(channel.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(channel.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(channel.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(channel.subtitle, color = TxtGray, fontSize = 13.sp, maxLines = 1)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
    }
}

// ── Novo Chat / Outros ────────────────────────────────────────────────────────
@Composable
private fun NewChatScreen(
    onBack: () -> Unit,
    onSelectChat: (String, String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filteredChannels = remember(query) {
        channels.filter { it.name != "Outros" && (query.isBlank() || it.name.contains(query, ignoreCase = true)) }
    }
    val filteredMembers = remember(query) {
        members.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("Cancelar", color = Blue, fontSize = 16.sp)
            }
            Text(
                "Novo Chat",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(80.dp))
        }

        // Barra de pesquisa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DkSurface)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
                if (query.isEmpty()) {
                    Text("Pesquisar funcionários...", color = TxtGray, fontSize = 15.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = SolidColor(Blue),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Criar novo canal
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {}
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Blue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Text("Criar novo canal de chat", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 80.dp).background(Color.White.copy(alpha = 0.07f)))
            }

            // Secção sugeridos
            item {
                Text(
                    "Sugeridos",
                    color = TxtGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DkSurface2)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Canais sugeridos
            items(filteredChannels) { ch ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectChat(ch.name, ch.subtitle) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ch.iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(ch.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ch.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(ch.subtitle, color = TxtGray, fontSize = 13.sp, maxLines = 1)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 80.dp).background(Color.White.copy(alpha = 0.07f)))
            }

            // Membros
            items(filteredMembers) { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectChat(m.name, "") }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(m.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(m.initials, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(m.name, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TxtGray, modifier = Modifier.size(18.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 80.dp).background(Color.White.copy(alpha = 0.07f)))
            }
        }
    }
}

// ── Ecrã de Chat ──────────────────────────────────────────────────────────────
@Composable
private fun ChatRoomScreen(
    name: String,
    subtitle: String,
    onBack: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                name,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Box {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DkSurface2)
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.MoreHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(DkSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Detalhes do canal", color = Color.White, fontSize = 15.sp) },
                        onClick = { showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Silenciar notificações", color = Color.White, fontSize = 15.sp) },
                        onClick = { showMenu = false }
                    )
                }
            }
        }

        // Empty state
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(DkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (name == "Todos" || name == "Anúncios") Icons.Outlined.Campaign else Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = TxtGray,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    subtitle.ifBlank { "Chat com $name" },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Todos os membros da equipa recebem notificações das últimas mensagens.",
                    color = TxtGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        // Input de mensagem
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DkSurface)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(0.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DkSurface2)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (message.isEmpty()) {
                        Text("Enviar uma mensagem...", color = TxtGray, fontSize = 15.sp)
                    }
                    BasicTextField(
                        value = message,
                        onValueChange = { message = it },
                        singleLine = false,
                        maxLines = 4,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(Blue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (message.isNotBlank()) Blue else DkSurface2)
                        .clickable(enabled = message.isNotBlank()) { message = "" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "Enviar",
                        tint = if (message.isNotBlank()) Color.White else TxtGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
