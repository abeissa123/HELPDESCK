package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.HelpdeskRepository
import com.example.util.NotificationHelper
import com.example.util.NotificationType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HelpdeskViewModel(
    private val repository: HelpdeskRepository,
    private val application: Application
) : ViewModel() {

    private val notificationHelper = NotificationHelper(application)

    // Historique des e-mails simulés envoyés
    val emailLogs: StateFlow<List<EmailLog>> = repository.getAllEmailsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Utilisateur actuellement connecté
    private val _currentUser = MutableStateFlow<Utilisateur?>(null)
    val currentUser: StateFlow<Utilisateur?> = _currentUser.asStateFlow()

    // Filtres de recherche et de liste
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<Long?>(null)
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private val _selectedPriorityFilter = MutableStateFlow<Priorite?>(null)
    val selectedPriorityFilter = _selectedPriorityFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<Statut?>(null)
    val selectedStatusFilter = _selectedStatusFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(TicketSortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    fun setSortOrder(order: TicketSortOrder) {
        _sortOrder.value = order
    }

    // Liste des utilisateurs (techniciens & admins pour l'assignation)
    val technicians: StateFlow<List<Utilisateur>> = repository.getTechniciansAndAdminsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<Utilisateur>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Liste des catégories
    val categories: StateFlow<List<Categorie>> = repository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Liste des tickets filtrés réactivement
    val filteredTickets: StateFlow<List<Ticket>> = combine(
        _currentUser,
        _searchQuery,
        _selectedCategoryFilter,
        _selectedPriorityFilter,
        _selectedStatusFilter,
        _sortOrder,
        repository.getAllTicketsFlow()
    ) { flows ->
        val user = flows[0] as? Utilisateur
        val query = flows[1] as String
        val catId = flows[2] as? Long
        val priority = flows[3] as? Priorite
        val status = flows[4] as? Statut
        val sort = flows[5] as? TicketSortOrder ?: TicketSortOrder.DATE_DESC
        val allTickets = flows[6] as? List<Ticket> ?: emptyList()

        if (user == null) return@combine emptyList()

        val baseList = if (user.role == Role.EMPLOYE) {
            allTickets.filter { it.utilisateurId == user.id }
        } else {
            allTickets
        }

        val filtered = baseList.filter { ticket ->
            val matchesSearch = ticket.titre.contains(query, ignoreCase = true) ||
                    ticket.description.contains(query, ignoreCase = true)
            val matchesCategory = catId == null || ticket.categorieId == catId
            val matchesPriority = priority == null || ticket.priorite == priority
            val matchesStatus = status == null || ticket.statut == status

            matchesSearch && matchesCategory && matchesPriority && matchesStatus
        }

        when (sort) {
            TicketSortOrder.DATE_DESC -> filtered.sortedByDescending { it.dateCreation }
            TicketSortOrder.DATE_ASC -> filtered.sortedBy { it.dateCreation }
            TicketSortOrder.PRIORITY_DESC -> filtered.sortedByDescending { it.priorite.ordinal }
            TicketSortOrder.PRIORITY_ASC -> filtered.sortedBy { it.priorite.ordinal }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Détail d'un ticket sélectionné
    private val _selectedTicketId = MutableStateFlow<Long?>(null)
    val selectedTicketId: StateFlow<Long?> = _selectedTicketId.asStateFlow()

    val selectedTicket: StateFlow<Ticket?> = _selectedTicketId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getTicketByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedTicketComments: StateFlow<List<Commentaire>> = _selectedTicketId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getCommentsForTicketFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedTicketHistory: StateFlow<List<Historique>> = _selectedTicketId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getHistoryForTicketFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistiques réactives (mises à jour automatiquement dès que les tickets changent)
    val stats: StateFlow<HelpdeskStats> = repository.getAllTicketsFlow()
        .map { tickets ->
            val total = tickets.size
            val nouveaux = tickets.count { it.statut == Statut.NOUVEAU }
            val enCours = tickets.count { it.statut == Statut.EN_COURS }
            val resolus = tickets.count { it.statut == Statut.RESOLU }
            val fermes = tickets.count { it.statut == Statut.FERME }

            val urgent = tickets.count { it.priorite == Priorite.URGENTE }
            val normal = tickets.count { it.priorite == Priorite.NORMALE }
            val faible = tickets.count { it.priorite == Priorite.FAIBLE }

            // Calcul du temps de résolution moyen (SLA) en heures
            val resolvedTickets = tickets.filter { it.statut == Statut.RESOLU || it.statut == Statut.FERME }
            val avgResolutionTimeHours = if (resolvedTickets.isNotEmpty()) {
                val sumMillis = resolvedTickets.sumOf { ticket ->
                    val resolutionTime = ticket.dateResolution ?: System.currentTimeMillis()
                    (resolutionTime - ticket.dateCreation).coerceAtLeast(0)
                }
                val avgMillis = sumMillis / resolvedTickets.size
                avgMillis.toDouble() / (1000 * 60 * 60)
            } else {
                0.0
            }

            HelpdeskStats(
                totalTickets = total,
                nouveaux = nouveaux,
                enCours = enCours,
                resolus = resolus,
                fermes = fermes,
                prioriteUrgente = urgent,
                prioriteNormale = normal,
                prioriteFaible = faible,
                avgResolutionTimeHours = avgResolutionTimeHours
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HelpdeskStats())

    fun updateNotificationPreferences(vibration: Boolean, email: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(notifVibration = vibration, notifEmail = email)
            repository.updateUser(updated)
            _currentUser.value = updated
        }
    }

    private suspend fun simulerEnvoiEmail(destinataireUser: Utilisateur, sujet: String, contenu: String) {
        if (destinataireUser.notifEmail) {
            val emailLog = EmailLog(
                destinataire = destinataireUser.email,
                sujet = sujet,
                contenu = contenu
            )
            repository.insertEmail(emailLog)
        }
    }

    private fun declencherNotifications(
        concerneUser: Utilisateur?,
        titreNotif: String,
        messageNotif: String,
        sujetEmail: String,
        contenuEmail: String,
        vibrationType: NotificationType
    ) {
        // Vibration pour l'utilisateur connecté s'il l'a activée
        val connectedUser = _currentUser.value
        if (connectedUser != null && connectedUser.notifVibration) {
            notificationHelper.vibrateForEvent(vibrationType)
        }

        // Notification système locale
        notificationHelper.showNotification(
            id = (System.currentTimeMillis() % 100000).toInt(),
            title = titreNotif,
            message = messageNotif
        )

        // Envoi d'e-mail simulé au destinataire concerné
        if (concerneUser != null) {
            viewModelScope.launch {
                simulerEnvoiEmail(concerneUser, sujetEmail, contenuEmail)
            }
        }
    }

    fun clearAllEmailsLog() {
        viewModelScope.launch {
            repository.clearAllEmails()
        }
    }

    fun markAllEmailsAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun markEmailAsRead(emailId: Long) {
        viewModelScope.launch {
            repository.markAsRead(emailId)
        }
    }

    fun envoyerEmailTest(user: Utilisateur, sujet: String, contenu: String) {
        viewModelScope.launch {
            simulerEnvoiEmail(user, sujet, contenu)
        }
    }

    init {
        viewModelScope.launch {
            // Initialisation et pré-population de démo
            repository.prepopulateIfNeeded()
        }
    }

    // ACTIONS UTILISATEUR

    fun selectTicket(ticketId: Long?) {
        _selectedTicketId.value = ticketId
    }

    fun login(email: String, motDePasse: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email.trim())
            if (user != null && user.motDePasse == motDePasse) {
                when (user.statut) {
                    UserStatus.VALIDE -> {
                        _currentUser.value = user
                        onSuccess()
                    }
                    UserStatus.EN_ATTENTE -> {
                        onError("Votre compte est en attente d'approbation par un administrateur.")
                    }
                    UserStatus.REFUSE -> {
                        onError("Votre compte a été refusé par l'administrateur.")
                    }
                }
            } else {
                onError("Email ou mot de passe incorrect.")
            }
        }
    }

    fun register(
        nom: String,
        email: String,
        motDePasse: String,
        role: Role,
        service: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (nom.isBlank() || email.isBlank() || motDePasse.isBlank()) {
                onError("Veuillez remplir tous les champs obligatoires.")
                return@launch
            }
            val existing = repository.getUserByEmail(email.trim())
            if (existing != null) {
                onError("Un compte existe déjà avec cette adresse email.")
                return@launch
            }
            val newUser = Utilisateur(
                nom = nom.trim(),
                email = email.trim(),
                motDePasse = motDePasse,
                role = role,
                service = service?.trim(),
                statut = UserStatus.EN_ATTENTE
            )
            repository.insertUser(newUser)
            onSuccess()
        }
    }

    fun approveUser(user: Utilisateur) {
        viewModelScope.launch {
            val updated = user.copy(statut = UserStatus.VALIDE)
            repository.updateUser(updated)
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = updated
            }
            // Déclencher la notification vibrante en temps réel pour l'approbation du compte
            notificationHelper.showApprovalNotification(user.nom, user.email)
        }
    }

    fun rejectUser(user: Utilisateur) {
        if (user.email.equals("abeissajean66@gmail.com", ignoreCase = true)) {
            return // Ne pas rejeter l'administrateur principal direct
        }
        viewModelScope.launch {
            val updated = user.copy(statut = UserStatus.REFUSE)
            repository.updateUser(updated)
            if (_currentUser.value?.id == user.id) {
                logout()
            }
        }
    }

    fun deleteUser(user: Utilisateur) {
        if (user.email.equals("abeissajean66@gmail.com", ignoreCase = true)) {
            return // Ne pas supprimer l'administrateur principal direct
        }
        viewModelScope.launch {
            repository.deleteUser(user)
            if (_currentUser.value?.id == user.id) {
                logout()
            }
        }
    }

    fun resetUserCredentials(
        user: Utilisateur,
        newEmail: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val emailTrimmed = newEmail.trim()
            val passwordTrimmed = newPassword.trim()
            if (emailTrimmed.isBlank() || passwordTrimmed.isBlank()) {
                onError("L'adresse e-mail et le mot de passe ne peuvent pas être vides.")
                return@launch
            }

            val existing = repository.getUserByEmail(emailTrimmed)
            if (existing != null && existing.id != user.id) {
                onError("Un autre compte utilise déjà cette adresse e-mail.")
                return@launch
            }

            val updated = user.copy(
                email = emailTrimmed,
                motDePasse = passwordTrimmed
            )
            repository.updateUser(updated)

            val emailLog = EmailLog(
                destinataire = emailTrimmed,
                sujet = "Réinitialisation de vos identifiants de connexion",
                contenu = "Bonjour ${user.nom},\n\nUn administrateur a mis à jour vos identifiants pour vous permettre d'accéder à nouveau à votre compte.\n\nNouvel e-mail : $emailTrimmed\nNouveau mot de passe : $passwordTrimmed\n\nVous pouvez désormais vous connecter."
            )
            repository.insertEmail(emailLog)

            if (_currentUser.value?.id == user.id) {
                _currentUser.value = updated
            }
            onSuccess()
        }
    }

    fun logout() {
        _currentUser.value = null
        _searchQuery.value = ""
        _selectedCategoryFilter.value = null
        _selectedPriorityFilter.value = null
        _selectedStatusFilter.value = null
        _selectedTicketId.value = null
    }

    fun createTicket(
        titre: String,
        description: String,
        categorieId: Long,
        priorite: Priorite,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            if (titre.isBlank() || description.isBlank()) {
                onError("Veuillez remplir tous les champs obligatoires.")
                return@launch
            }

            val newTicket = Ticket(
                titre = titre.trim(),
                description = description.trim(),
                categorieId = categorieId,
                priorite = priorite,
                utilisateurId = user.id,
                statut = Statut.NOUVEAU
            )

            val ticketId = repository.insertTicket(newTicket)

            // Enregistrer dans l'historique
            repository.insertHistory(
                Historique(
                    ticketId = ticketId,
                    utilisateurId = user.id,
                    typeAction = "Création",
                    descriptionAction = "Création du ticket"
                )
            )

            // Déclencher les notifications (Vibration pour le créateur + Mail)
            declencherNotifications(
                concerneUser = user,
                titreNotif = "Ticket d'incident créé !",
                messageNotif = "Votre ticket d'incident \"${titre.trim()}\" a bien été enregistré.",
                sujetEmail = "[Ticket #$ticketId] Confirmation d'ouverture : ${titre.trim()}",
                contenuEmail = "Bonjour ${user.nom},\n\nVotre ticket d'incident \"${titre.trim()}\" a été créé avec succès.\n\nDescription :\n${description.trim()}\n\nPriorité : ${priorite.getDisplayName()}\n\nUn technicien va examiner votre demande sous peu.\n\nCordialement,\nLe Support Helpdesk.",
                vibrationType = NotificationType.TICKET_CREATION
            )

            // Notifier également tous les techniciens et administrateurs d'un nouveau ticket disponible
            try {
                repository.getTechniciansAndAdminsFlow().first().forEach { staff ->
                    // Éviter de s'envoyer un mail d'alerte si le créateur est lui-même technicien/admin
                    if (staff.id != user.id) {
                        simulerEnvoiEmail(
                            destinataireUser = staff,
                            sujet = "[Nouveau Ticket #$ticketId] Priorité ${priorite.getDisplayName()} : ${titre.trim()}",
                            contenu = "Bonjour ${staff.nom},\n\nUn nouveau ticket d'incident a été ouvert par ${user.nom}.\n\nTitre : ${titre.trim()}\nDescription : ${description.trim()}\n\nMerci de vous rendre sur la console Helpdesk pour le prendre en charge."
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignorer en cas d'erreur de récupération des techniciens
            }

            onSuccess()
        }
    }

    fun changeTicketStatus(ticketId: Long, newStatus: Statut, performerId: Long) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId) ?: return@launch
            val oldStatus = ticket.statut

            if (oldStatus == newStatus) return@launch

            val isResolved = newStatus == Statut.RESOLU || newStatus == Statut.FERME
            val dateRes = if (isResolved) System.currentTimeMillis() else null

            val updatedTicket = ticket.copy(
                statut = newStatus,
                dateResolution = dateRes
            )

            repository.updateTicket(updatedTicket)

            // Enregistrer dans l'historique
            repository.insertHistory(
                Historique(
                    ticketId = ticketId,
                    utilisateurId = performerId,
                    typeAction = "Statut",
                    descriptionAction = "Statut modifié de ${oldStatus.name} à ${newStatus.name}"
                )
            )

            // Déclencher les notifications au créateur du ticket
            val creator = repository.getUserById(ticket.utilisateurId)
            if (creator != null) {
                val vibType = when (newStatus) {
                    Statut.RESOLU -> NotificationType.TICKET_RESOLVED
                    Statut.EN_COURS -> NotificationType.TICKET_IN_PROGRESS
                    else -> NotificationType.TICKET_IN_PROGRESS
                }

                val statusStr = when (newStatus) {
                    Statut.NOUVEAU -> "Nouveau"
                    Statut.EN_COURS -> "En cours de traitement"
                    Statut.RESOLU -> "Résolu"
                    Statut.FERME -> "Fermé"
                }

                val titleMsg = "Ticket #${ticketId} : $statusStr"
                val bodyMsg = "Le statut de votre incident \"${ticket.titre}\" est maintenant : $statusStr"

                declencherNotifications(
                    concerneUser = creator,
                    titreNotif = titleMsg,
                    messageNotif = bodyMsg,
                    sujetEmail = "[Ticket #$ticketId] Changement de statut : $statusStr",
                    contenuEmail = "Bonjour ${creator.nom},\n\nLe statut de votre ticket d'incident #${ticketId} \"${ticket.titre}\" a été mis à jour.\n\nAncien statut : ${oldStatus.name}\nNouveau statut : $statusStr\n\nVous pouvez suivre l'avancement ou ajouter des précisions directement depuis l'application.\n\nCordialement,\nLe Support Helpdesk.",
                    vibrationType = vibType
                )
            }
        }
    }

    fun assignTicket(ticketId: Long, technicianId: Long?, performerId: Long) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId) ?: return@launch
            val updatedTicket = ticket.copy(technicienId = technicianId)

            repository.updateTicket(updatedTicket)

            val techName = if (technicianId != null) {
                repository.getUserById(technicianId)?.nom ?: "Technicien"
            } else {
                "Non assigné"
            }

            // Enregistrer dans l'historique
            repository.insertHistory(
                Historique(
                    ticketId = ticketId,
                    utilisateurId = performerId,
                    typeAction = "Affectation",
                    descriptionAction = if (technicianId != null) "Ticket assigné à $techName" else "Ticket désassigné"
                )
            )

            // Déclencher les notifications
            val creator = repository.getUserById(ticket.utilisateurId)
            val technician = if (technicianId != null) repository.getUserById(technicianId) else null

            // 1. Notifier l'employé (créateur) que son ticket a changé d'affectation
            if (creator != null) {
                val actionMsg = if (technician != null) {
                    "Votre ticket \"${ticket.titre}\" a été assigné au technicien ${technician.nom}."
                } else {
                    "Votre ticket \"${ticket.titre}\" n'est plus assigné."
                }

                declencherNotifications(
                    concerneUser = creator,
                    titreNotif = "Technicien assigné",
                    messageNotif = actionMsg,
                    sujetEmail = "[Ticket #$ticketId] Affectation d'un technicien",
                    contenuEmail = "Bonjour ${creator.nom},\n\nNous vous informons qu'un intervenant technique a été affecté à votre ticket d'incident #${ticketId} \"${ticket.titre}\".\n\nTechnicien assigné : ${technician?.nom ?: "Aucun (Désassigné)"}\n\nCelui-ci va prendre en charge votre demande sous peu.\n\nCordialement,\nLe Support Helpdesk.",
                    vibrationType = NotificationType.TICKET_ASSIGNED
                )
            }

            // 2. Notifier le technicien de son affectation (e-mail d'alerte)
            if (technician != null && technicianId != performerId) {
                simulerEnvoiEmail(
                    destinataireUser = technician,
                    sujet = "[Nouveau Ticket Assigné #$ticketId] ${ticket.titre}",
                    contenu = "Bonjour ${technician.nom},\n\nLe ticket d'incident #${ticketId} \"${ticket.titre}\" créé par ${creator?.nom ?: "un employé"} vous a été assigné.\n\nDescription de l'incident :\n${ticket.description}\n\nPriorité : ${ticket.priorite.getDisplayName()}\n\nMerci de prendre en charge ce ticket depuis votre console dès que possible.\n\nCordialement,\nLa Plateforme Helpdesk."
                )
            }
        }
    }

    fun addComment(ticketId: Long, contenu: String, authorId: Long) {
        viewModelScope.launch {
            if (contenu.isBlank()) return@launch

            val comment = Commentaire(
                ticketId = ticketId,
                utilisateurId = authorId,
                contenu = contenu.trim()
            )

            repository.insertComment(comment)

            // Enregistrer dans l'historique
            repository.insertHistory(
                Historique(
                    ticketId = ticketId,
                    utilisateurId = authorId,
                    typeAction = "Commentaire",
                    descriptionAction = "Nouveau commentaire ajouté"
                )
            )

            // Déclencher les notifications
            try {
                val ticket = repository.getTicketById(ticketId) ?: return@launch
                val authorUser = repository.getUserById(authorId) ?: return@launch

                // Déterminer le destinataire de la notification
                val recipientId = if (authorId == ticket.utilisateurId) {
                    ticket.technicienId
                } else {
                    ticket.utilisateurId
                }

                val recipientUser = if (recipientId != null) repository.getUserById(recipientId) else null

                if (recipientUser != null) {
                    declencherNotifications(
                        concerneUser = recipientUser,
                        titreNotif = "Nouveau commentaire",
                        messageNotif = "${authorUser.nom} a écrit : \"${contenu.trim()}\"",
                        sujetEmail = "[Ticket #$ticketId] Nouveau commentaire de ${authorUser.nom}",
                        contenuEmail = "Bonjour ${recipientUser.nom},\n\nUn nouveau commentaire a été ajouté par ${authorUser.nom} concernant le ticket d'incident #${ticketId} \"${ticket.titre}\".\n\nCommentaire :\n\"${contenu.trim()}\"\n\nVous pouvez y répondre directement en vous connectant à l'application Helpdesk.\n\nCordialement,\nLe Support Helpdesk.",
                        vibrationType = NotificationType.COMMENT_ADDED
                    )
                } else {
                    // Si aucun destinataire (ex : ticket pas encore assigné), faire vibrer l'auteur connecté pour confirmation
                    val connectedUser = _currentUser.value
                    if (connectedUser != null && connectedUser.notifVibration) {
                        notificationHelper.vibrateForEvent(NotificationType.COMMENT_ADDED)
                    }
                }
            } catch (e: Exception) {
                // Ignorer en cas d'erreur de notification
            }
        }
    }

    fun rateTicketSatisfaction(ticketId: Long, note: Int) {
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId) ?: return@launch
            val updated = ticket.copy(satisfactionNote = note)
            repository.updateTicket(updated)

            // Enregistrer dans l'historique
            repository.insertHistory(
                Historique(
                    ticketId = ticketId,
                    utilisateurId = ticket.utilisateurId,
                    typeAction = "Évaluation",
                    descriptionAction = "Évaluation de satisfaction : $note/5"
                )
            )
        }
    }

    // Gestion des catégories (Admin uniquement)
    fun createCategory(nom: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (nom.isBlank()) {
                onError("Le nom de la catégorie ne peut pas être vide.")
                return@launch
            }
            repository.insertCategory(Categorie(nom = nom.trim()))
            onSuccess()
        }
    }

    fun deleteCategory(category: Categorie) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun deleteTicket(ticket: Ticket, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTicket(ticket)
            onSuccess()
        }
    }

    // Filtres
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun setPriorityFilter(priority: Priorite?) {
        _selectedPriorityFilter.value = priority
    }

    fun setStatusFilter(status: Statut?) {
        _selectedStatusFilter.value = status
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategoryFilter.value = null
        _selectedPriorityFilter.value = null
        _selectedStatusFilter.value = null
        _sortOrder.value = TicketSortOrder.DATE_DESC
    }
}

enum class TicketSortOrder {
    DATE_DESC,     // Plus récent d'abord
    DATE_ASC,      // Plus ancien d'abord
    PRIORITY_DESC, // Priorité Haute -> Basse
    PRIORITY_ASC;  // Priorité Basse -> Haute

    fun getDisplayName(): String {
        return when (this) {
            DATE_DESC -> "Date (Récent en premier)"
            DATE_ASC -> "Date (Ancien en premier)"
            PRIORITY_DESC -> "Priorité (Haute -> Basse)"
            PRIORITY_ASC -> "Priorité (Basse -> Haute)"
        }
    }
}

// Classe d'état pour les statistiques d'administration
data class HelpdeskStats(
    val totalTickets: Int = 0,
    val nouveaux: Int = 0,
    val enCours: Int = 0,
    val resolus: Int = 0,
    val fermes: Int = 0,
    val prioriteUrgente: Int = 0,
    val prioriteNormale: Int = 0,
    val prioriteFaible: Int = 0,
    val avgResolutionTimeHours: Double = 0.0
)

class HelpdeskViewModelFactory(
    private val repository: HelpdeskRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HelpdeskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HelpdeskViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
