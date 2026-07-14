package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.HelpdeskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HelpdeskViewModel(private val repository: HelpdeskRepository) : ViewModel() {

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
        repository.getAllTicketsFlow()
    ) { flows ->
        val user = flows[0] as? Utilisateur
        val query = flows[1] as String
        val catId = flows[2] as? Long
        val priority = flows[3] as? Priorite
        val status = flows[4] as? Statut
        val allTickets = flows[5] as? List<Ticket> ?: emptyList()

        if (user == null) return@combine emptyList()

        val baseList = if (user.role == Role.EMPLOYE) {
            allTickets.filter { it.utilisateurId == user.id }
        } else {
            allTickets
        }

        baseList.filter { ticket ->
            val matchesSearch = ticket.titre.contains(query, ignoreCase = true) ||
                    ticket.description.contains(query, ignoreCase = true)
            val matchesCategory = catId == null || ticket.categorieId == catId
            val matchesPriority = priority == null || ticket.priorite == priority
            val matchesStatus = status == null || ticket.statut == status

            matchesSearch && matchesCategory && matchesPriority && matchesStatus
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
                _currentUser.value = user
                onSuccess()
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
                service = service?.trim()
            )
            repository.insertUser(newUser)
            // Se connecter directement après inscription
            val createdUser = repository.getUserByEmail(email.trim())
            _currentUser.value = createdUser
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

class HelpdeskViewModelFactory(private val repository: HelpdeskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HelpdeskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HelpdeskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
