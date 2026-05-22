package pt.ualg.miaugenda.ui.screen.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import pt.ualg.miaugenda.MiauGendaApp

private val DkBg = Color(0xFF000000)
private val DkSurface = Color(0xFF1C1C1E)
private val DkDivider = Color(0xFF2C2C2E)
private val Blue = Color(0xFF2979FF)
private val TxtGray = Color(0xFF8E8E93)
private val DkGreen = Color(0xFF2ECC71)

private enum class ProfileSubScreen { Menu, UpdateProfile }

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    onAvailabilityClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val tokenManager = MiauGendaApp.getTokenManager(context)
    val userName = tokenManager.getUserName() ?: "Utilizador"

    var subScreen by remember { mutableStateOf(ProfileSubScreen.Menu) }

    when (subScreen) {
        ProfileSubScreen.Menu -> ProfileMenuContent(
            userName = userName,
            onBack = onBack,
            onUpdateProfile = { subScreen = ProfileSubScreen.UpdateProfile },
            onAvailability = onAvailabilityClick,
            onLogout = onLogout
        )
        ProfileSubScreen.UpdateProfile -> UpdateProfileContent(
            onBack = { subScreen = ProfileSubScreen.Menu }
        )
    }
}

@Composable
private fun ProfileMenuContent(
    userName: String,
    onBack: () -> Unit,
    onUpdateProfile: () -> Unit,
    onAvailability: () -> Unit,
    onLogout: () -> Unit
) {
    var emailNotif by remember { mutableStateOf(true) }
    var pushNotif by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )
                }
            }

            // Avatar + name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                AvatarCircle(name = userName, size = 90.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = userName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Section 1
            MenuSection {
                MenuRow(
                    icon = Icons.Default.Person,
                    label = "Atualizar Perfil",
                    onClick = onUpdateProfile
                )
                HorizontalDivider(
                    color = DkDivider,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 60.dp)
                )
                MenuRow(
                    icon = Icons.Default.DateRange,
                    label = "A Minha Disponibilidade",
                    onClick = onAvailability
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Preferences section
            Text(
                text = "Preferências",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
            MenuSection {
                MenuRowToggle(
                    icon = Icons.Default.Email,
                    label = "Notificações por Email",
                    checked = emailNotif,
                    onCheckedChange = { emailNotif = it }
                )
                HorizontalDivider(
                    color = DkDivider,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 60.dp)
                )
                MenuRowToggle(
                    icon = Icons.Default.Notifications,
                    label = "Notificações Push",
                    checked = pushNotif,
                    onCheckedChange = { pushNotif = it }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sign out
            MenuSection {
                MenuRow(
                    icon = Icons.Default.ExitToApp,
                    label = "Terminar Sessão",
                    labelColor = Color(0xFFFF453A),
                    iconBgColor = Color(0xFFFF453A),
                    showChevron = false,
                    onClick = onLogout
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun MenuSection(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DkSurface),
        content = content
    )
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    labelColor: Color = Color.White,
    iconBgColor: Color = Blue,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (showChevron) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TxtGray,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun MenuRowToggle(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Blue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Blue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF3A3A3C)
            )
        )
    }
}

@Composable
private fun AvatarCircle(name: String, size: Dp) {
    val initials = name.split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF2979FF), Color(0xFFE91E8C))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = (size.value * 0.35f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Update Profile Sub-Screen ─────────────────────────────────────────────

@Composable
private fun UpdateProfileContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = MiauGendaApp.getTokenManager(context)

    val viewModel: ProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(tokenManager) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user

    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    var contact by remember(user) { mutableStateOf(user?.contact ?: "") }
    val category = user?.category ?: ""
    var password by remember { mutableStateOf("") }
    var showBanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            showBanner = true
            delay(3000)
            showBanner = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar: Cancelar | Name | Guardar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text("Cancelar", color = Blue, fontSize = 16.sp)
                }
                Text(
                    text = name.ifBlank { "Perfil" },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
                TextButton(
                    onClick = {
                        viewModel.updateProfile(
                            name = name,
                            contact = contact,
                            password = password
                        )
                        password = ""
                    },
                    enabled = !uiState.isSaving && name.isNotBlank(),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        text = if (uiState.isSaving) "..." else "Guardar",
                        color = if (!uiState.isSaving && name.isNotBlank()) Blue else TxtGray,
                        fontSize = 16.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Avatar with camera overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box {
                        AvatarCircle(name = name.ifBlank { "?" }, size = 80.dp)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2C2C2E))
                                .border(2.dp, DkBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Blue, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Form fields
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DkSurface)
                ) {
                    ProfileTextField(
                        value = name,
                        placeholder = "Nome completo",
                        onValueChange = { name = it }
                    )
                    HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    ProfileTextField(
                        value = contact,
                        placeholder = "Contacto",
                        onValueChange = { contact = it }
                    )
                    if (category.isNotBlank()) {
                        HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                        ProfileFieldReadOnly(label = "Categoria", value = category)
                    }
                    HorizontalDivider(color = DkDivider, thickness = 0.5.dp)
                    ProfileTextField(
                        value = password,
                        placeholder = "Nova password (opcional)",
                        onValueChange = { password = it },
                        isPassword = true
                    )
                }

                uiState.error?.let { err ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = err,
                        color = Color(0xFFFF453A),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Success banner
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
                Text(
                    text = "Perfil atualizado com sucesso",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileTextField(
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
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(text = placeholder, color = TxtGray, fontSize = 15.sp)
            }
            innerTextField()
        }
    )
}

@Composable
private fun ProfileFieldReadOnly(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TxtGray, fontSize = 15.sp)
        Text(
            text = value.lowercase().replaceFirstChar { it.uppercase() },
            color = TxtGray,
            fontSize = 15.sp
        )
    }
}
