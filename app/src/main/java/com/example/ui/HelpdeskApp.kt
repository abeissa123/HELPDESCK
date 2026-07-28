package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpdeskApp(viewModel: HelpdeskViewModel) {
    val navController = rememberNavController()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // Rediriger vers l'écran de connexion si l'utilisateur n'est pas connecté
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        } else if (navController.currentDestination?.route == "login") {
            navController.navigate("dashboard") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(viewModel = viewModel)
            }
            composable("dashboard") {
                DashboardScreen(viewModel = viewModel, navController = navController)
            }
            composable("ticket_list") {
                TicketListScreen(viewModel = viewModel, navController = navController)
            }
            composable("ticket_create") {
                TicketCreateScreen(viewModel = viewModel, navController = navController)
            }
            composable(
                route = "ticket_detail/{ticketId}",
                arguments = listOf(navArgument("ticketId") { type = NavType.LongType })
            ) { backStackEntry ->
                val ticketId = backStackEntry.arguments?.getLong("ticketId") ?: -1L
                TicketDetailScreen(viewModel = viewModel, ticketId = ticketId, navController = navController)
            }
            composable("admin_panel") {
                AdminPanelScreen(viewModel = viewModel, navController = navController)
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel, navController = navController)
            }
            composable("emails_simules") {
                EmailsSimulesScreen(viewModel = viewModel, navController = navController)
            }
        }
    }
}

// ----------------------------------------------------
// 1. LOGIN & REGISTER SCREEN
// ----------------------------------------------------
@Composable
fun LoginScreen(viewModel: HelpdeskViewModel) {
    var isSignUp by remember { mutableStateOf(false) }

    // Formulaire de connexion
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Formulaire d'inscription
    var name by remember { mutableStateOf("") }
    var service by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(Role.EMPLOYE) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Helpdesk Support",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Service d'assistance informatique de l'entreprise",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Onglets Connexion / Inscription
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isSignUp) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable {
                                        isSignUp = false
                                        errorMessage = null
                                        successMessage = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Connexion",
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isSignUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSignUp) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable {
                                        isSignUp = true
                                        errorMessage = null
                                        successMessage = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Inscription",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSignUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (successMessage != null) {
                            Surface(
                                color = Color(0xFFE8F5E9), // Light green container
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = successMessage!!,
                                    color = Color(0xFF2E7D32), // Dark green text
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Formulaire commun
                        if (isSignUp) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nom complet *") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("signup_name"),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email professionnel *") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth().testTag("email_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Mot de passe *") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                val image = if (isPasswordVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                }
                                val description = if (isPasswordVisible) {
                                    "Masquer le mot de passe"
                                } else {
                                    "Afficher le mot de passe"
                                }
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(imageVector = image, contentDescription = description)
                                }
                            },
                            visualTransformation = if (isPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth().testTag("password_input"),
                            singleLine = true
                        )

                        if (isSignUp) {
                            OutlinedTextField(
                                value = service,
                                onValueChange = { service = it },
                                label = { Text("Service / Département (optionnel)") },
                                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("signup_service"),
                                singleLine = true
                            )

                            Text(
                                "Sélectionnez votre rôle :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Role.values().forEach { role ->
                                    val isSelected = selectedRole == role
                                    val frenchRole = when (role) {
                                        Role.EMPLOYE -> "Employé"
                                        Role.TECHNICIEN -> "Technicien"
                                        Role.ADMIN -> "Admin"
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedRole = role },
                                        label = { Text(frenchRole) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                errorMessage = null
                                if (isSignUp) {
                                    viewModel.register(
                                        nom = name,
                                        email = email,
                                        motDePasse = password,
                                        role = selectedRole,
                                        service = service.ifBlank { null },
                                        onSuccess = {
                                            successMessage = "Compte créé ! Votre inscription est en attente d'approbation par un administrateur."
                                            isSignUp = false // switch to Login tab
                                            name = ""
                                            service = ""
                                        },
                                        onError = { errorMessage = it }
                                    )
                                } else {
                                    viewModel.login(
                                        email = email,
                                        motDePasse = password,
                                        onSuccess = {},
                                        onError = { errorMessage = it }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                if (isSignUp) "Créer un compte" else "Se connecter",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Informations de sécurité et approbation
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 Sécurité : Tout nouvel utilisateur inscrit commence avec le statut 'En attente'. Un administrateur doit l'approuver depuis son espace d'administration avant qu'il ne puisse se connecter.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. DASHBOARD SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: HelpdeskViewModel, navController: NavHostController) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val tickets by viewModel.filteredTickets.collectAsStateWithLifecycle()
    val emailLogs by viewModel.emailLogs.collectAsStateWithLifecycle()
    val unreadCount = emailLogs.count { !it.lu }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tableau de Bord", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate("emails_simules") }) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(text = unreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Journal des e-mails")
                        }
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Paramètres de notifications")
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Déconnexion")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bannière de bienvenue
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Bonjour, ${user?.nom ?: "Utilisateur"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val roleText = when (user?.role) {
                                    Role.EMPLOYE -> "Employé"
                                    Role.TECHNICIEN -> "Technicien"
                                    Role.ADMIN -> "Administrateur"
                                    null -> "Inconnu"
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = roleText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (user?.service != null) {
                                    Text(
                                        "• ${user!!.service}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Grille des statistiques
            item {
                Text(
                    text = "Aperçu de l'activité",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(
                            title = "Nouveau",
                            count = stats.nouveaux,
                            icon = Icons.Default.FiberNew,
                            color = Color(0xFF0288D1),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "En Cours",
                            count = stats.enCours,
                            icon = Icons.Default.Pending,
                            color = Color(0xFFF57C00),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(
                            title = "Résolu",
                            count = stats.resolus,
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF388E3C),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Fermé",
                            count = stats.fermes,
                            icon = Icons.Default.Archive,
                            color = Color(0xFF616161),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Temps de résolution SLA pour les techniciens & admins
            if (user?.role == Role.TECHNICIEN || user?.role == Role.ADMIN) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Temps de Résolution Moyen (SLA)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    "Basé sur les tickets résolus de l'entreprise",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = if (stats.avgResolutionTimeHours > 0) {
                                    String.format(Locale.FRANCE, "%.1fh", stats.avgResolutionTimeHours)
                                } else {
                                    "--"
                                },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Actions rapides
            item {
                Text(
                    text = "Actions rapides",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Les employés peuvent créer
                    if (user?.role == Role.EMPLOYE || user?.role == Role.ADMIN) {
                        Button(
                            onClick = { navController.navigate("ticket_create") },
                            modifier = Modifier.weight(1f).testTag("signaler_incident_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Signaler incident", fontSize = 12.sp, maxLines = 1)
                        }
                    }

                    Button(
                        onClick = { navController.navigate("ticket_list") },
                        modifier = Modifier.weight(1f).testTag("voir_tickets_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (user?.role == Role.EMPLOYE) "Mes tickets" else "Tous les tickets",
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    if (user?.role == Role.ADMIN) {
                        Button(
                            onClick = { navController.navigate("admin_panel") },
                            modifier = Modifier.weight(1f).testTag("admin_panel_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gérer admin", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            // Titre tickets récents
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (user?.role == Role.EMPLOYE) "Mes tickets récents" else "Tickets récents",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Voir tout",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { navController.navigate("ticket_list") }
                    )
                }
            }

            // Liste des tickets récents
            val recentTickets = tickets.take(4)
            if (recentTickets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(36.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Aucun ticket récent", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(recentTickets) { ticket ->
                    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()
                    val catName = categoriesList.find { it.id == ticket.categorieId }?.nom ?: "Catégorie"
                    TicketItem(
                        ticket = ticket,
                        categoryName = catName,
                        onClick = {
                            viewModel.selectTicket(ticket.id)
                            navController.navigate("ticket_detail/${ticket.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, count: Int, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(count.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. TICKET LIST SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketListScreen(viewModel: HelpdeskViewModel, navController: NavHostController) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val tickets by viewModel.filteredTickets.collectAsStateWithLifecycle()
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedPriority by viewModel.selectedPriorityFilter.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()

    var showFiltersDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    var showExportMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val categoriesMap = categoriesList.associate { it.id to it.nom }
                val csvContent = generateCsv(tickets, categoriesMap)
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Liste exportée en CSV avec succès !", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur d'export CSV : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                val categoriesMap = categoriesList.associate { it.id to it.nom }
                val pdfBytes = generatePdf(tickets, categoriesMap)
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(pdfBytes)
                }
                Toast.makeText(context, "Liste exportée en PDF avec succès !", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur d'export PDF : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tickets d'incidents", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showFiltersDialog = true }) {
                        val isAnyFilterActive = selectedCategory != null || selectedPriority != null || selectedStatus != null
                        BadgedBox(
                            badge = {
                                if (isAnyFilterActive) {
                                    Badge { Text("!") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtres")
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showExportMenu = true },
                            modifier = Modifier.testTag("export_menu_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Exporter")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Exporter en CSV") },
                                leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                                onClick = {
                                    showExportMenu = false
                                    csvLauncher.launch("tickets_${System.currentTimeMillis()}.csv")
                                },
                                modifier = Modifier.testTag("export_csv_item")
                            )
                            DropdownMenuItem(
                                text = { Text("Exporter en PDF") },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                onClick = {
                                    showExportMenu = false
                                    pdfLauncher.launch("tickets_${System.currentTimeMillis()}.pdf")
                                },
                                modifier = Modifier.testTag("export_pdf_item")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (user?.role == Role.EMPLOYE || user?.role == Role.ADMIN) {
                FloatingActionButton(
                    onClick = { navController.navigate("ticket_create") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_ticket_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Créer un ticket")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Barre de recherche
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Rechercher un ticket...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("search_field"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Ligne d'informations & de tri
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tickets.size} ticket${if (tickets.size > 1) "s" else ""}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )

                Box {
                    Row(
                        modifier = Modifier
                            .clickable { showSortMenu = true }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                            .testTag("sort_selector"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Trier",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Trier : ${sortOrder.getDisplayName()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        TicketSortOrder.values().forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.getDisplayName()) },
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    showSortMenu = false
                                },
                                modifier = Modifier.testTag("sort_item_${order.name.lowercase(Locale.FRANCE)}")
                            )
                        }
                    }
                }
            }

            // Indicateur de filtres actifs
            if (selectedCategory != null || selectedPriority != null || selectedStatus != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtres actifs :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Effacer tout",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { viewModel.clearFilters() }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedCategory != null) {
                        val catName = categoriesList.find { it.id == selectedCategory }?.nom ?: "Catégorie"
                        FilterChipActive(label = catName, onDismiss = { viewModel.setCategoryFilter(null) })
                    }
                    if (selectedPriority != null) {
                        FilterChipActive(label = selectedPriority!!.getDisplayName(), onDismiss = { viewModel.setPriorityFilter(null) })
                    }
                    if (selectedStatus != null) {
                        FilterChipActive(label = selectedStatus!!.name, onDismiss = { viewModel.setStatusFilter(null) })
                    }
                }
            }

            // Liste de tickets
            if (tickets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Text(
                            "Aucun ticket trouvé",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            "Vérifiez vos filtres ou signalez un nouvel incident.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tickets) { ticket ->
                        val catName = categoriesList.find { it.id == ticket.categorieId }?.nom ?: "Catégorie"
                        TicketItem(
                            ticket = ticket,
                            categoryName = catName,
                            onClick = {
                                viewModel.selectTicket(ticket.id)
                                navController.navigate("ticket_detail/${ticket.id}")
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogue pour configurer les filtres
    if (showFiltersDialog) {
        AlertDialog(
            onDismissRequest = { showFiltersDialog = false },
            title = { Text("Filtrer les incidents") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text("Catégorie :", fontWeight = FontWeight.Bold, fontSize = 14.dp.value.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { viewModel.setCategoryFilter(null) },
                                label = { Text("Tout") },
                                modifier = Modifier.padding(2.dp)
                            )
                            categoriesList.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat.id,
                                    onClick = { viewModel.setCategoryFilter(cat.id) },
                                    label = { Text(cat.nom) },
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }

                    item {
                        Text("Priorité :", fontWeight = FontWeight.Bold, fontSize = 14.dp.value.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = selectedPriority == null,
                                onClick = { viewModel.setPriorityFilter(null) },
                                label = { Text("Tout") },
                                modifier = Modifier.padding(2.dp)
                            )
                            Priorite.values().forEach { prio ->
                                FilterChip(
                                    selected = selectedPriority == prio,
                                    onClick = { viewModel.setPriorityFilter(prio) },
                                    label = { Text(prio.getDisplayName()) },
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }

                    item {
                        Text("Statut :", fontWeight = FontWeight.Bold, fontSize = 14.dp.value.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = selectedStatus == null,
                                onClick = { viewModel.setStatusFilter(null) },
                                label = { Text("Tout") },
                                modifier = Modifier.padding(2.dp)
                            )
                            Statut.values().forEach { stat ->
                                FilterChip(
                                    selected = selectedStatus == stat,
                                    onClick = { viewModel.setStatusFilter(stat) },
                                    label = { Text(stat.name) },
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFiltersDialog = false }) {
                    Text("Appliquer")
                }
            }
        )
    }
}

@Composable
fun FilterChipActive(label: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(12.dp).clickable { onDismiss() },
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun TicketItem(ticket: Ticket, categoryName: String, onClick: () -> Unit) {
    val dateText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(ticket.dateCreation))

    // Coloration selon statut
    val statusColor = when (ticket.statut) {
        Statut.NOUVEAU -> Color(0xFF0288D1)
        Statut.EN_COURS -> Color(0xFFF57C00)
        Statut.RESOLU -> Color(0xFF388E3C)
        Statut.FERME -> Color(0xFF616161)
    }

    // Coloration selon priorité
    val priorityColor = when (ticket.priorite) {
        Priorite.URGENTE -> Color(0xFFD32F2F)
        Priorite.NORMALE -> Color(0xFFF57C00)
        Priorite.FAIBLE -> Color(0xFF388E3C)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("ticket_item_${ticket.id}"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge catégorie
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = categoryName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Date de création
                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Titre du ticket
            Text(
                text = ticket.titre,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Description courte
            Text(
                text = ticket.description,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priorité
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = priorityColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Priorité ${ticket.priorite.getDisplayName()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor
                    )
                }

                // Statut
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = ticket.statut.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 4. CREATE TICKET SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketCreateScreen(viewModel: HelpdeskViewModel, navController: NavHostController) {
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedPriority by remember { mutableStateOf(Priorite.NORMALE) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Définir la catégorie par défaut une fois qu'elles sont chargées
    LaunchedEffect(categoriesList) {
        if (categoriesList.isNotEmpty() && selectedCategoryId == null) {
            selectedCategoryId = categoriesList.first().id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signaler un Incident", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Veuillez renseigner les détails de votre problème informatique.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            if (errorMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Objet / Titre de l'incident *") },
                    placeholder = { Text("Ex: Imprimante bloquée, Accès WiFi refusé") },
                    modifier = Modifier.fillMaxWidth().testTag("ticket_title_input"),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description détaillée *") },
                    placeholder = { Text("Veuillez décrire l'erreur, l'emplacement physique ou les messages d'erreur affichés...") },
                    modifier = Modifier.fillMaxWidth().height(160.dp).testTag("ticket_desc_input"),
                    maxLines = 6
                )
            }

            item {
                Text("Catégorie de l'incident :", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categoriesList.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategoryId = cat.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id }
                            )
                            Text(cat.nom, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            item {
                Text("Niveau de priorité :", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priorite.values().forEach { prio ->
                        val isSelected = selectedPriority == prio
                        val color = when (prio) {
                            Priorite.URGENTE -> Color(0xFFD32F2F)
                            Priorite.NORMALE -> Color(0xFFF57C00)
                            Priorite.FAIBLE -> Color(0xFF388E3C)
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPriority = prio }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) color else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) color.copy(alpha = 0.08f) else Color.Transparent
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prio.getDisplayName(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        errorMessage = null
                        if (selectedCategoryId == null) {
                            errorMessage = "Aucune catégorie sélectionnée."
                            return@Button
                        }
                        viewModel.createTicket(
                            titre = title,
                            description = description,
                            categorieId = selectedCategoryId!!,
                            priorite = selectedPriority,
                            onSuccess = {
                                navController.navigateUp()
                            },
                            onError = {
                                errorMessage = it
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_ticket_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Créer le ticket d'incident", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 5. TICKET DETAIL SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(viewModel: HelpdeskViewModel, ticketId: Long, navController: NavHostController) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val ticket by viewModel.selectedTicket.collectAsStateWithLifecycle()
    val comments by viewModel.selectedTicketComments.collectAsStateWithLifecycle()
    val history by viewModel.selectedTicketHistory.collectAsStateWithLifecycle()
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()
    val technicians by viewModel.technicians.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    var commentText by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: Détails & Suivi, 1: Fil d'actualité (Timeline)
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    // Charger le ticket au démarrage de l'écran
    LaunchedEffect(ticketId) {
        viewModel.selectTicket(ticketId)
    }

    if (ticket == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Détail du Ticket") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val currentTicket = ticket!!
    val dateText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(currentTicket.dateCreation))
    val categoryName = categoriesList.find { it.id == currentTicket.categorieId }?.nom ?: "Catégorie"
    val creator = allUsers.find { it.id == currentTicket.utilisateurId }

    val statusColor = when (currentTicket.statut) {
        Statut.NOUVEAU -> Color(0xFF0288D1)
        Statut.EN_COURS -> Color(0xFFF57C00)
        Statut.RESOLU -> Color(0xFF388E3C)
        Statut.FERME -> Color(0xFF616161)
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Supprimer le ticket", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Êtes-vous sûr de vouloir supprimer définitivement le ticket #${currentTicket.id} (\"${currentTicket.titre}\") ? Cette action est irréversible.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        viewModel.deleteTicket(currentTicket, onSuccess = {
                            viewModel.selectTicket(null)
                            navController.navigateUp()
                        })
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmationDialog = false },
                    modifier = Modifier.testTag("cancel_delete_btn")
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ticket #${currentTicket.id}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.selectTicket(null)
                        navController.navigateUp()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (user?.role == Role.TECHNICIEN || user?.role == Role.ADMIN) {
                        IconButton(
                            onClick = { showDeleteConfirmationDialog = true },
                            modifier = Modifier.testTag("delete_ticket_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer le ticket",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sélecteur d'onglets pour un design propre
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Détails & Actions", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        val totalEvents = comments.size + history.size
                        Text("Fil de suivi ($totalEvents)", fontWeight = FontWeight.Bold)
                    }
                )
            }

            if (activeTab == 0) {
                // CONTENU DE L'ONGLET 1 : DÉTAILS ET ACTIONS SUPPORT
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Statut, Priorité, Catégorie
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Statut actuel :", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Surface(
                                        color = statusColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = currentTicket.statut.name,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Priorité :", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    val priorityColor = when (currentTicket.priorite) {
                                        Priorite.URGENTE -> Color(0xFFD32F2F)
                                        Priorite.NORMALE -> Color(0xFFF57C00)
                                        Priorite.FAIBLE -> Color(0xFF388E3C)
                                    }
                                    Text(
                                        text = currentTicket.priorite.getDisplayName(),
                                        fontWeight = FontWeight.Bold,
                                        color = priorityColor,
                                        fontSize = 13.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Catégorie :", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(categoryName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Date de création :", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(dateText, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Objet et Description
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = currentTicket.titre,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                                Text(
                                    text = currentTicket.description,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // Informations sur le créateur
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Signalé par : ${creator?.nom ?: "Utilisateur de démo"}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Service : ${creator?.service ?: "Non renseigné"} • ${creator?.email ?: ""}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    // Assignation Technicien
                    item {
                        val assignedTech = allUsers.find { it.id == currentTicket.technicienId }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Assignation du technicien :", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = assignedTech?.nom ?: "Non assigné",
                                            fontSize = 13.sp,
                                            fontWeight = if (assignedTech != null) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    // Si technicien et non assigné -> S'assigner
                                    if (user?.role == Role.TECHNICIEN && currentTicket.technicienId == null) {
                                        Button(
                                            onClick = { viewModel.assignTicket(currentTicket.id, user!!.id, user!!.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.testTag("self_assign_btn")
                                        ) {
                                            Text("Prendre en charge", fontSize = 11.sp)
                                        }
                                    }
                                }

                                // Si Admin -> Permettre d'assigner à n'importe quel technicien
                                if (user?.role == Role.ADMIN) {
                                    var expandedTechMenu by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(
                                            onClick = { expandedTechMenu = true },
                                            modifier = Modifier.fillMaxWidth().testTag("assign_tech_btn")
                                        ) {
                                            Text(if (assignedTech != null) "Modifier l'assignation" else "Assigner un technicien")
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(
                                            expanded = expandedTechMenu,
                                            onDismissRequest = { expandedTechMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Désassigner (Aucun)") },
                                                onClick = {
                                                    viewModel.assignTicket(currentTicket.id, null, user!!.id)
                                                    expandedTechMenu = false
                                                }
                                            )
                                            technicians.forEach { tech ->
                                                DropdownMenuItem(
                                                    text = { Text("${tech.nom} (${tech.role.name})") },
                                                    onClick = {
                                                        viewModel.assignTicket(currentTicket.id, tech.id, user!!.id)
                                                        expandedTechMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Changer de Statut (pour Techniciens et Admins)
                    if (user?.role == Role.TECHNICIEN || user?.role == Role.ADMIN) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Changer le statut du ticket :", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Statut.values().forEach { stat ->
                                            val isCurrent = currentTicket.statut == stat
                                            val statFrench = when (stat) {
                                                Statut.NOUVEAU -> "Nouveau"
                                                Statut.EN_COURS -> "En Cours"
                                                Statut.RESOLU -> "Résolu"
                                                Statut.FERME -> "Fermé"
                                            }
                                            Button(
                                                onClick = { viewModel.changeTicketStatus(currentTicket.id, stat, user!!.id) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isCurrent) statusColor else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.weight(1f).testTag("status_btn_${stat.name}"),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(statFrench, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ÉVALUATION DE SATISFACTION (Pour le créateur du ticket si statut résolu/fermé)
                    if (user?.id == currentTicket.utilisateurId &&
                        (currentTicket.statut == Statut.RESOLU || currentTicket.statut == Statut.FERME)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFF9C4) // Jaune chaleureux
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFBC02D))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Votre avis nous intéresse !",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF5D4037)
                                    )
                                    Text(
                                        "Veuillez évaluer la qualité de la résolution de votre incident informatique :",
                                        fontSize = 12.sp,
                                        color = Color(0xFF5D4037).copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (star in 1..5) {
                                            val isSelected = currentTicket.satisfactionNote != null && currentTicket.satisfactionNote!! >= star
                                            IconButton(
                                                onClick = { viewModel.rateTicketSatisfaction(currentTicket.id, star) }
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                                                    contentDescription = "Étoile $star",
                                                    tint = if (isSelected) Color(0xFFFBC02D) else Color.Gray,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (currentTicket.satisfactionNote != null) {
                                        Text(
                                            "Merci pour votre retour de satisfaction : ${currentTicket.satisfactionNote}/5 !",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF388E3C)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // CONTENU DE L'ONGLET 2 : TIMELINE / FIL DE SUIVI CHRONOLOGIQUE
                // Combiner les commentaires et l'historique dans un seul flux chronologique !
                val timelineEvents = remember(comments, history) {
                    val events = mutableListOf<TimelineEvent>()

                    comments.forEach { comm ->
                        events.add(
                            TimelineEvent(
                                timestamp = comm.dateCreation,
                                userId = comm.utilisateurId,
                                isComment = true,
                                title = "Commentaire",
                                body = comm.contenu
                            )
                        )
                    }

                    history.forEach { hist ->
                        events.add(
                            TimelineEvent(
                                timestamp = hist.dateAction,
                                userId = hist.utilisateurId,
                                isComment = false,
                                title = hist.typeAction,
                                body = hist.descriptionAction
                            )
                        )
                    }

                    events.sortBy { it.timestamp }
                    events
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (timelineEvents.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Aucun événement dans le fil de suivi.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(timelineEvents) { event ->
                            val eventAuthor = allUsers.find { it.id == event.userId }
                            TimelineItemCard(event = event, author = eventAuthor)
                        }
                    }
                }

                // Bloc d'écriture de commentaire permanent au bas de la timeline
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Écrire un commentaire...") },
                            modifier = Modifier.weight(1f).testTag("comment_input"),
                            maxLines = 3,
                            shape = RoundedCornerShape(20.dp)
                        )
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    viewModel.addComment(currentTicket.id, commentText, user!!.id)
                                    commentText = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(44.dp).testTag("send_comment_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Envoyer")
                        }
                    }
                }
            }
        }
    }
}

// Modèle unifié pour le flux chronologique
data class TimelineEvent(
    val timestamp: Long,
    val userId: Long,
    val isComment: Boolean,
    val title: String,
    val body: String
)

@Composable
fun TimelineItemCard(event: TimelineEvent, author: Utilisateur?) {
    val dateText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(event.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isComment) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        border = if (event.isComment) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else null
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (event.isComment) Icons.Default.Comment else Icons.Default.History,
                        contentDescription = null,
                        tint = if (event.isComment) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (event.isComment) "Commentaire" else event.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (event.isComment) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                Text(dateText, fontSize = 11.sp, color = Color.Gray)
            }

            Text(
                text = event.body,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Par : ${author?.nom ?: "Système / Démo"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                if (author?.role != null && author.role != Role.EMPLOYE) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = author.role.name,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 6. ADMIN PANEL SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(viewModel: HelpdeskViewModel, navController: NavHostController) {
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var newCategoryName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var userToReset by remember { mutableStateOf<Utilisateur?>(null) }
    var resetEmail by remember { mutableStateOf("") }
    var resetPassword by remember { mutableStateOf("") }
    var resetError by remember { mutableStateOf<String?>(null) }

    var categoryToDelete by remember { mutableStateOf<Categorie?>(null) }
    var userToDelete by remember { mutableStateOf<Utilisateur?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Administrateur", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SLA & Performance KPI
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Métrique SLA - Temps de résolution moyen :", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = if (stats.avgResolutionTimeHours > 0) {
                                    String.format(Locale.FRANCE, "%.2f heures de prise en charge", stats.avgResolutionTimeHours)
                                } else {
                                    "Aucun ticket résolu pour le calcul SLA"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Gestion des Catégories
            item {
                Text("Gestion des Catégories d'incidents", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                placeholder = { Text("Nouvelle catégorie...") },
                                modifier = Modifier.weight(1f).testTag("category_input"),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    errorMessage = null
                                    viewModel.createCategory(
                                        nom = newCategoryName,
                                        onSuccess = {
                                            newCategoryName = ""
                                        },
                                        onError = {
                                            errorMessage = it
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.testTag("add_category_btn")
                            ) {
                                Text("Ajouter")
                            }
                        }

                        Divider()

                        categoriesList.forEach { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(cat.nom, fontSize = 14.sp)
                                }
                                IconButton(
                                    onClick = { categoryToDelete = cat },
                                    modifier = Modifier.testTag("delete_category_${cat.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Liste des Utilisateurs inscrits
            item {
                Text("Utilisateurs & Équipe Support (${allUsers.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allUsers.forEach { user ->
                            val colorRole = when (user.role) {
                                Role.EMPLOYE -> MaterialTheme.colorScheme.primary
                                Role.TECHNICIEN -> Color(0xFFE65100)
                                Role.ADMIN -> Color(0xFFC2185B)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(user.nom, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        
                                        // Status badge
                                        val statusColor = when (user.statut) {
                                            UserStatus.VALIDE -> Color(0xFF388E3C)
                                            UserStatus.EN_ATTENTE -> Color(0xFFFBC02D)
                                            UserStatus.REFUSE -> Color(0xFFD32F2F)
                                        }
                                        val statusBgColor = statusColor.copy(alpha = 0.12f)
                                        Surface(
                                            color = statusBgColor,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = when (user.statut) {
                                                    UserStatus.VALIDE -> "Approuvé"
                                                    UserStatus.EN_ATTENTE -> "En attente"
                                                    UserStatus.REFUSE -> "Refusé"
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${user.email} • Service : ${user.service ?: "Aucun"}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Role badge
                                    Surface(
                                        color = colorRole.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = user.role.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorRole,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Action buttons for other users (excluding self and primary direct admin)
                                    if (user.id != currentUser?.id && !user.email.equals("abeissajean66@gmail.com", ignoreCase = true)) {
                                        if (user.statut != UserStatus.VALIDE) {
                                            IconButton(
                                                onClick = { viewModel.approveUser(user) },
                                                modifier = Modifier.size(28.dp).testTag("approve_user_${user.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Accepter / Valider / Approuver",
                                                    tint = Color(0xFF388E3C),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (user.statut != UserStatus.REFUSE) {
                                            IconButton(
                                                onClick = { viewModel.rejectUser(user) },
                                                modifier = Modifier.size(28.dp).testTag("reject_user_${user.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Refuser",
                                                    tint = Color(0xFFD32F2F),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { 
                                                userToReset = user
                                                resetEmail = user.email
                                                resetPassword = user.motDePasse ?: ""
                                                resetError = null
                                            },
                                            modifier = Modifier.size(28.dp).testTag("reset_user_${user.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Réinitialiser les identifiants",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { userToDelete = user },
                                            modifier = Modifier.size(28.dp).testTag("delete_user_${user.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Supprimer",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (userToReset != null) {
        AlertDialog(
            onDismissRequest = { userToReset = null },
            title = {
                Text(
                    text = "Réinitialiser les identifiants 🔐",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Modifier l'adresse e-mail ou définir un nouveau mot de passe pour ${userToReset?.nom} afin de lui redonner accès.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var passwordVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { 
                            resetEmail = it
                            resetError = null
                        },
                        label = { Text("Adresse e-mail") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reset_email_input"),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = resetPassword,
                        onValueChange = { 
                            resetPassword = it
                            resetError = null
                        },
                        label = { Text("Nouveau mot de passe") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reset_password_input"),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = if (passwordVisible) "Masquer" else "Afficher")
                            }
                        }
                    )

                    if (resetError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = resetError!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetUser = userToReset
                        if (targetUser != null) {
                            viewModel.resetUserCredentials(
                                user = targetUser,
                                newEmail = resetEmail,
                                newPassword = resetPassword,
                                onSuccess = {
                                    userToReset = null
                                    resetEmail = ""
                                    resetPassword = ""
                                    resetError = null
                                },
                                onError = {
                                    resetError = it
                                }
                            )
                        }
                    },
                    modifier = Modifier.testTag("confirm_reset_btn")
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { userToReset = null }
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Supprimer la catégorie 🏷️", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Êtes-vous sûr de vouloir supprimer la catégorie \"${categoryToDelete?.nom}\" ? Cette action est irréversible.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetCat = categoryToDelete
                        if (targetCat != null) {
                            viewModel.deleteCategory(targetCat)
                        }
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_category_btn")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { categoryToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_category_btn")
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Supprimer l'utilisateur 👤", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Êtes-vous sûr de vouloir supprimer définitivement le compte de ${userToDelete?.nom} (${userToDelete?.email}) ? Cette action est irréversible.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetUser = userToDelete
                        if (targetUser != null) {
                            viewModel.deleteUser(targetUser)
                        }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_user_btn")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { userToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_user_btn")
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

// ----------------------------------------------------
// 8. SETTINGS SCREEN (NOTIFICATIONS & VIBRATIONS)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: HelpdeskViewModel, navController: NavHostController) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var vibrationActive by remember(currentUser) { mutableStateOf(currentUser?.notifVibration ?: true) }
    var emailActive by remember(currentUser) { mutableStateOf(currentUser?.notifEmail ?: true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Préférences de notifications",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Configurez comment vous souhaitez être alerté lors de la création d'un ticket, d'une mise en cours, d'une résolution ou d'un nouveau commentaire.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            // Carte Vibrations
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Vibration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text("Vibrations tactiles", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Vibrer lors des alertes", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = vibrationActive,
                            onCheckedChange = { checked ->
                                vibrationActive = checked
                                viewModel.updateNotificationPreferences(checked, emailActive)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            // Tester un pattern de vibration
                            val helper = com.example.util.NotificationHelper(navController.context)
                            helper.vibrateForEvent(com.example.util.NotificationType.TICKET_RESOLVED)
                        },
                        enabled = vibrationActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tester la vibration", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Carte E-mails
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text("Notifications par e-mail", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(currentUser?.email ?: "votre-email@company.com", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = emailActive,
                            onCheckedChange = { checked ->
                                emailActive = checked
                                viewModel.updateNotificationPreferences(vibrationActive, checked)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            currentUser?.let { user ->
                                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE).format(Date())
                                viewModel.envoyerEmailTest(
                                    user = user,
                                    sujet = "📧 [Test] Validation de votre adresse de notification",
                                    contenu = "Bonjour ${user.nom},\n\nCeci est un e-mail de test envoyé automatiquement par votre plateforme de Helpdesk.\n\nFélicitations, votre configuration de notification par e-mail fonctionne parfaitement !\n\nDate : $dateStr\n\nCordialement,\nL'équipe administrative Helpdesk."
                                )
                                // Faire vibrer également pour confirmer
                                val helper = com.example.util.NotificationHelper(navController.context)
                                helper.vibrateSimple(150)
                            }
                        },
                        enabled = emailActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simuler un e-mail de test", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 9. EMAILS SIMULÉS SCREEN (GMAIL-LIKE INBOX)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailsSimulesScreen(viewModel: HelpdeskViewModel, navController: NavHostController) {
    val emailLogs by viewModel.emailLogs.collectAsStateWithLifecycle()
    val sdf = remember { SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE) }

    LaunchedEffect(Unit) {
        viewModel.markAllEmailsAsRead()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mails Envoyés (Simulés)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (emailLogs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllEmailsLog() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Vider le journal", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    ) { innerPadding ->
        if (emailLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Drafts,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Text(
                        "Aucun e-mail envoyé",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        "Créez des tickets, modifiez un statut ou ajoutez un commentaire pour voir s'activer le simulateur de messagerie.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Journal des communications SMTP simulées",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(emailLogs) { mail ->
                    var isExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        colors = CardDefaults.cardColors(
                            containerColor = if (!mail.lu) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        ),
                        border = BorderStroke(
                            width = if (!mail.lu) 1.5.dp else 1.dp,
                            color = if (!mail.lu) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (!mail.lu) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = mail.destinataire.take(1).uppercase(Locale.FRANCE),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "A : ${mail.destinataire}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (!mail.lu) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(8.dp)
                                                ) {}
                                            }
                                        }
                                        Text(
                                            text = "De : noreply@company.com",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Text(
                                    text = sdf.format(Date(mail.dateEnvoi)),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = mail.sujet,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            if (isExpanded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = mail.contenu,
                                    fontSize = 13.sp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Text(
                                    text = mail.contenu.replace("\n", " "),
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateCsv(tickets: List<Ticket>, categories: Map<Long, String>): String {
    val sb = StringBuilder()
    sb.append("ID;Titre;Description;Categorie;Priorite;Statut;DateCreation;EmployeId;TechnicienId\n")
    tickets.forEach { ticket ->
        val catName = categories[ticket.categorieId] ?: "Inconnue"
        val id = ticket.id
        val title = escapeCsv(ticket.titre)
        val description = escapeCsv(ticket.description)
        val priority = ticket.priorite.getDisplayName()
        val status = ticket.statut.name
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE).format(Date(ticket.dateCreation))
        val employeId = ticket.utilisateurId
        val technicienId = ticket.technicienId ?: ""
        sb.append("$id;$title;$description;$catName;$priority;$status;$formattedDate;$employeId;$technicienId\n")
    }
    return sb.toString()
}

private fun escapeCsv(value: String): String {
    var escaped = value.replace("\"", "\"\"")
    escaped = escaped.replace("\n", " ").replace("\r", " ")
    if (escaped.contains(";") || escaped.contains("\"") || escaped.contains(",")) {
        escaped = "\"$escaped\""
    }
    return escaped
}

private fun generatePdf(tickets: List<Ticket>, categories: Map<Long, String>): ByteArray {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    val titlePaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 18f
        color = AndroidColor.BLACK
    }

    val headerPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 11f
        color = AndroidColor.DKGRAY
    }

    val textPaint = Paint().apply {
        typeface = Typeface.DEFAULT
        textSize = 9f
        color = AndroidColor.BLACK
    }

    val linePaint = Paint().apply {
        color = AndroidColor.LTGRAY
        strokeWidth = 1f
    }

    var y = 50f

    canvas.drawText("Rapport de Tickets Helpdesk", 50f, y, titlePaint)
    y += 20f
    canvas.drawText("Généré le: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date()), 50f, y, textPaint)
    y += 30f

    canvas.drawText("ID", 50f, y, headerPaint)
    canvas.drawText("Titre", 90f, y, headerPaint)
    canvas.drawText("Catégorie", 260f, y, headerPaint)
    canvas.drawText("Priorité", 360f, y, headerPaint)
    canvas.drawText("Statut", 450f, y, headerPaint)
    y += 8f
    canvas.drawLine(50f, y, 545f, y, linePaint)
    y += 18f

    var pageNumber = 1

    tickets.forEach { ticket ->
        if (y > 800f) {
            pdfDocument.finishPage(page)
            pageNumber++
            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDocument.startPage(newPageInfo)
            canvas = page.canvas
            y = 50f

            canvas.drawText("ID", 50f, y, headerPaint)
            canvas.drawText("Titre", 90f, y, headerPaint)
            canvas.drawText("Catégorie", 260f, y, headerPaint)
            canvas.drawText("Priorité", 360f, y, headerPaint)
            canvas.drawText("Statut", 450f, y, headerPaint)
            y += 8f
            canvas.drawLine(50f, y, 545f, y, linePaint)
            y += 18f
        }

        val idStr = "#${ticket.id}"
        val catName = categories[ticket.categorieId] ?: "Inconnue"
        val titleStr = if (ticket.titre.length > 25) ticket.titre.take(22) + "..." else ticket.titre
        val catStr = if (catName.length > 15) catName.take(12) + "..." else catName
        val priorityStr = ticket.priorite.getDisplayName()
        val statusStr = ticket.statut.name

        canvas.drawText(idStr, 50f, y, textPaint)
        canvas.drawText(titleStr, 90f, y, textPaint)
        canvas.drawText(catStr, 260f, y, textPaint)
        canvas.drawText(priorityStr, 360f, y, textPaint)
        canvas.drawText(statusStr, 450f, y, textPaint)

        y += 18f
    }

    pdfDocument.finishPage(page)

    val outputStream = java.io.ByteArrayOutputStream()
    pdfDocument.writeTo(outputStream)
    pdfDocument.close()
    return outputStream.toByteArray()
}
