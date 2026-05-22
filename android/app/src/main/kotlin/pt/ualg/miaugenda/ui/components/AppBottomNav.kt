package pt.ualg.miaugenda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
private val RedBadge  = Color(0xFFFF3B30)

enum class NavTab { HOME, SCHEDULER, INBOX, NOTIFICATIONS, MENU }

@Composable
fun AppBottomNav(
    active: NavTab = NavTab.HOME,
    notificationCount: Int = 0,
    inboxCount: Int = 0,
    onHomeClick: () -> Unit = {},
    onSchedulerClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    data class Item(val tab: NavTab, val label: String, val icon: ImageVector, val badge: Int, val onClick: () -> Unit)

    val items = listOf(
        Item(NavTab.HOME,          "Inicio",       Icons.Outlined.Home,          0,                 onHomeClick),
        Item(NavTab.SCHEDULER,     "Agenda",       Icons.Outlined.DateRange,     0,                 onSchedulerClick),
        Item(NavTab.INBOX,         "Caixa",        Icons.Outlined.Inbox,         inboxCount,        onInboxClick),
        Item(NavTab.NOTIFICATIONS, "Notificacoes", Icons.Outlined.Notifications, notificationCount, onNotificationsClick),
        Item(NavTab.MENU,          "Equipa",       Icons.Outlined.Group,         0,                 onMenuClick),
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
                Box {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (isActive) Blue else TxtGray,
                        modifier = Modifier.size(22.dp)
                    )
                    if (item.badge > 0) {
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(RedBadge)
                                .align(Alignment.TopEnd)
                                .offset(x = 5.dp, y = (-3).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${item.badge}",
                                color = Color.White,
                                fontSize = 7.sp,
                                lineHeight = 7.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
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
