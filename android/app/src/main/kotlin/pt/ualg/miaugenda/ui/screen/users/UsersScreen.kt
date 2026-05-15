package pt.ualg.miaugenda.ui.screen.users

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.ualg.miaugenda.MiauGendaApp
import pt.ualg.miaugenda.data.model.CreateUserRequest
import pt.ualg.miaugenda.data.model.User

private val PurpleMain = Color(0xFF6F4BB2)
private val PurpleSoft = Color(0xFFF3ECFA)
private val BlueSoft = Color(0xFFEAF6FF)

private val VALID_ROLES = listOf("ADMIN", "MANAGER", "EMPLOYEE")
private val VALID_CATEGORIES = listOf("VETERINARIAN", "NURSE", "OPERATIONAL", "ADMINISTRATIVE")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    onBack: () -> Unit = {},
    viewModel: UsersViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = MiauGendaApp.getTokenManager(context)
    val role = tokenManager.getUserRole() ?: ""
    val isAdminOrManager = role == "ADMIN" || role == "MANAGER"

    if (!isAdminOrManager) {
        AccessDeniedScaffold(onBack = onBack)
        return
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Utilizadores") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Novo utilizador")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BlueSoft, PurpleSoft)))
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            uiState.error?.let {
                Banner(
                    text = it,
                    container = MaterialTheme.colorScheme.errorContainer,
                    onClose = { viewModel.clearMessages() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            uiState.successMessage?.let {
                Banner(
                    text = it,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    onClose = { viewModel.clearMessages() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                uiState.isLoading -> LoadingCard()
                uiState.users.isEmpty() -> EmptyCard()
                else -> UsersList(
                    users = uiState.users,
                    onDeactivate = { viewModel.requestDeactivate(it) }
                )
            }
        }
    }

    if (uiState.showCreateDialog) {
        CreateUserDialog(
            isSubmitting = uiState.isSubmitting,
            onDismiss = { viewModel.closeCreateDialog() },
            onSubmit = { viewModel.createUser(it) }
        )
    }

    uiState.pendingDeactivation?.let { user ->
        ConfirmDeactivateDialog(
            user = user,
            isSubmitting = uiState.isSubmitting,
            onConfirm = { viewModel.confirmDeactivate() },
            onCancel = { viewModel.cancelDeactivate() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessDeniedScaffold(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Utilizadores") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Acesso restrito a ADMIN e MANAGER.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun UsersList(users: List<User>, onDeactivate: (User) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(users, key = { it.id }) { user ->
            UserRow(user = user, onDeactivate = { onDeactivate(user) })
        }
    }
}

@Composable
private fun UserRow(user: User, onDeactivate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (!user.active) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Inativo") }
                        )
                    }
                }
                Text(
                    text = "${user.employeeNumber} - ${user.role}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PurpleMain
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = user.category,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (user.active) {
                IconButton(onClick = onDeactivate) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = "Desativar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun Banner(text: String, container: Color, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("OK") }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
    }
}

@Composable
private fun EmptyCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Sem utilizadores.",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CreateUserRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var employeeNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(VALID_ROLES[2]) }
    var category by remember { mutableStateOf(VALID_CATEGORIES[0]) }
    var roleExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Novo Utilizador",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = employeeNumber,
                    onValueChange = { employeeNumber = it },
                    label = { Text("Numero de funcionario *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password * (min. 6)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Papel *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        VALID_ROLES.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    role = r
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        VALID_CATEGORIES.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    category = c
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                localError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    ) { Text("Cancelar") }

                    Button(
                        onClick = {
                            localError = when {
                                name.isBlank() -> "Nome obrigatorio"
                                email.isBlank() -> "Email obrigatorio"
                                employeeNumber.isBlank() -> "Numero obrigatorio"
                                password.length < 6 -> "Password com pelo menos 6 caracteres"
                                else -> null
                            }
                            if (localError == null) {
                                onSubmit(
                                    CreateUserRequest(
                                        name = name.trim(),
                                        email = email.trim(),
                                        employeeNumber = employeeNumber.trim(),
                                        role = role,
                                        category = category,
                                        password = password
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    ) {
                        Text(if (isSubmitting) "A criar..." else "Criar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeactivateDialog(
    user: User,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Desativar utilizador") },
        text = {
            Text("Tem a certeza que pretende desativar \"${user.name}\" (${user.employeeNumber})?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (isSubmitting) "A desativar..." else "Desativar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel, enabled = !isSubmitting) {
                Text("Cancelar")
            }
        }
    )
}
