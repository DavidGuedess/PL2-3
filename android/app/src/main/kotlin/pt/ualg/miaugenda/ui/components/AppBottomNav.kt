package pt.ualg.miaugenda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DkSurface = Color(0xFF1C1C1E)
private val Blue      = Color(0xFF2979FF)
private val TxtGray   = Color(0xFF8E8E93)

enum class NavTab { HOME, SCHEDULER, INBOX, NOTIFICATIONS, MENU }

@Composable
fun AppBottomNav(
    active: NavTab = NavTab.HOME,
    onHomeClick: () -> Unit = {},
    onSchedulerClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    data class Item(val tab: NavTab, val label: String, val icon: ImageVector, val onClick: () -> Unit)

    val items = listOf(
        Item(NavTab.HOME,          "Inicio",       Icons.Outlined.Home,          onHomeClick),
        Item(NavTab.SCHEDULER,     "Agenda",       Icons.Outlined.DateRange,     onSchedulerClick),
        Item(NavTab.INBOX,         "Caixa",        Icons.Outlined.Inbox,         onInboxClick),
        Item(NavTab.NOTIFICATIONS, "Notificacoes", Icons.Outlined.Notifications, onNotificationsClick),
        Item(NavTab.MENU,          "Equipa",       Icons.Outlined.Group,         onMenuClick),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DkSurface)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(0.dp))
            .pointerInput(Unit) {}          // bloqueia cliques no conteúdo atrás
            .padding(top = 10.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isActive = item.tab == active
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { item.onClick() }
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = if (isActive) Blue else TxtGray,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    item.label,
                    fontSize = 10.sp,
                    color = if (isActive) Blue else TxtGray,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
