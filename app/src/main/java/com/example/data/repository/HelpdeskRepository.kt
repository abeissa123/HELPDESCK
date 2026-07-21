package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class HelpdeskRepository(private val db: AppDatabase) {
    private val userDao = db.userDao()
    private val categoryDao = db.categoryDao()
    private val ticketDao = db.ticketDao()
    private val commentDao = db.commentDao()
    private val historyDao = db.historyDao()
    private val emailLogDao = db.emailLogDao()

    // Utilisateurs
    suspend fun getUserByEmail(email: String): Utilisateur? = userDao.getUserByEmail(email)
    suspend fun getUserById(id: Long): Utilisateur? = userDao.getUserById(id)
    fun getAllUsersFlow(): Flow<List<Utilisateur>> = userDao.getAllUsersFlow()
    fun getTechniciansAndAdminsFlow(): Flow<List<Utilisateur>> = userDao.getTechniciansAndAdminsFlow()
    suspend fun insertUser(user: Utilisateur): Long = userDao.insertUser(user)
    suspend fun updateUser(user: Utilisateur) = userDao.updateUser(user)
    suspend fun deleteUser(user: Utilisateur) = userDao.deleteUser(user)

    // Emails Log
    fun getAllEmailsFlow(): Flow<List<EmailLog>> = emailLogDao.getAllEmailsFlow()
    suspend fun insertEmail(email: EmailLog): Long = emailLogDao.insertEmail(email)
    suspend fun clearAllEmails() = emailLogDao.clearAllEmails()

    // Catégories
    fun getAllCategoriesFlow(): Flow<List<Categorie>> = categoryDao.getAllCategoriesFlow()
    suspend fun insertCategory(category: Categorie): Long = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: Categorie) = categoryDao.deleteCategory(category)

    // Tickets
    fun getAllTicketsFlow(): Flow<List<Ticket>> = ticketDao.getAllTicketsFlow()
    fun getTicketsByEmployeeFlow(employeeId: Long): Flow<List<Ticket>> = ticketDao.getTicketsByEmployeeFlow(employeeId)
    fun getTicketByIdFlow(id: Long): Flow<Ticket?> = ticketDao.getTicketByIdFlow(id)
    suspend fun getTicketById(id: Long): Ticket? = ticketDao.getTicketById(id)
    suspend fun insertTicket(ticket: Ticket): Long = ticketDao.insertTicket(ticket)
    suspend fun updateTicket(ticket: Ticket) = ticketDao.updateTicket(ticket)
    suspend fun deleteTicket(ticket: Ticket) = ticketDao.deleteTicket(ticket)

    // Commentaires
    fun getCommentsForTicketFlow(ticketId: Long): Flow<List<Commentaire>> = commentDao.getCommentsForTicketFlow(ticketId)
    suspend fun insertComment(commentaire: Commentaire): Long = commentDao.insertComment(commentaire)

    // Historique
    fun getHistoryForTicketFlow(ticketId: Long): Flow<List<Historique>> = historyDao.getHistoryForTicketFlow(ticketId)
    suspend fun insertHistory(historique: Historique): Long = historyDao.insertHistory(historique)

    // Pré-population de la base de données pour démonstration et tests
    suspend fun prepopulateIfNeeded() {
        // Toujours s'assurer que l'administrateur principal (direct) est présent
        val existingDirectAdmin = userDao.getUserByEmail("abeissajean66@gmail.com")
        if (existingDirectAdmin == null) {
            userDao.insertUser(
                Utilisateur(
                    nom = "Admin Principal (Direct)",
                    email = "abeissajean66@gmail.com",
                    motDePasse = "password123",
                    role = Role.ADMIN,
                    service = "Direction Informatique",
                    statut = UserStatus.VALIDE
                )
            )
        }

        val categories = categoryDao.getAllCategoriesFlow().first()
        if (categories.isEmpty()) {
            // Ajouter les catégories standard
            val catReseauId = categoryDao.insertCategory(Categorie(nom = "Réseau"))
            val catMaterielId = categoryDao.insertCategory(Categorie(nom = "Matériel"))
            val catLogicielId = categoryDao.insertCategory(Categorie(nom = "Logiciel"))
            val catAccesId = categoryDao.insertCategory(Categorie(nom = "Accès & Comptes"))

            // Ajouter les utilisateurs de démonstration
            val emp1Id = userDao.insertUser(
                Utilisateur(
                    nom = "Jean Dupont",
                    email = "employe@company.com",
                    motDePasse = "password123",
                    role = Role.EMPLOYE,
                    service = "Ressources Humaines"
                )
            )
            val emp2Id = userDao.insertUser(
                Utilisateur(
                    nom = "Alice Martin",
                    email = "employe2@company.com",
                    motDePasse = "password123",
                    role = Role.EMPLOYE,
                    service = "Comptabilité"
                )
            )
            val techId = userDao.insertUser(
                Utilisateur(
                    nom = "Pierre Tech",
                    email = "tech@company.com",
                    motDePasse = "password123",
                    role = Role.TECHNICIEN,
                    service = "Support Informatique"
                )
            )
            val adminId = userDao.insertUser(
                Utilisateur(
                    nom = "Directeur Admin",
                    email = "admin@company.com",
                    motDePasse = "password123",
                    role = Role.ADMIN,
                    service = "Direction Informatique"
                )
            )

            // Créer quelques tickets d'exemple pour que l'application ne soit pas vide au premier lancement
            val ticket1Id = ticketDao.insertTicket(
                Ticket(
                    titre = "Connexion Wi-Fi impossible",
                    description = "Je ne parviens plus à me connecter au réseau WiFi de l'entreprise depuis ce matin. Mon ordinateur affiche 'Impossible de se connecter à ce réseau'.",
                    categorieId = catReseauId,
                    priorite = Priorite.URGENTE,
                    statut = Statut.NOUVEAU,
                    utilisateurId = emp1Id
                )
            )
            historyDao.insertHistory(
                Historique(
                    ticketId = ticket1Id,
                    utilisateurId = emp1Id,
                    typeAction = "Création",
                    descriptionAction = "Ticket d'incident créé"
                )
            )

            val ticket2Id = ticketDao.insertTicket(
                Ticket(
                    titre = "Écran secondaire qui clignote",
                    description = "Mon deuxième écran d'ordinateur se met à clignoter en noir par intermittence, surtout lorsque j'ouvre l'outil comptable.",
                    categorieId = catMaterielId,
                    priorite = Priorite.NORMALE,
                    statut = Statut.EN_COURS,
                    utilisateurId = emp2Id,
                    technicienId = techId
                )
            )
            historyDao.insertHistory(
                Historique(
                    ticketId = ticket2Id,
                    utilisateurId = emp2Id,
                    typeAction = "Création",
                    descriptionAction = "Ticket d'incident créé"
                )
            )
            historyDao.insertHistory(
                Historique(
                    ticketId = ticket2Id,
                    utilisateurId = adminId,
                    typeAction = "Affectation",
                    descriptionAction = "Ticket assigné au technicien Pierre Tech"
                )
            )
            historyDao.insertHistory(
                Historique(
                    ticketId = ticket2Id,
                    utilisateurId = techId,
                    typeAction = "Statut",
                    descriptionAction = "Statut modifié de Nouveau à En cours"
                )
            )
            commentDao.insertComment(
                Commentaire(
                    ticketId = ticket2Id,
                    utilisateurId = techId,
                    contenu = "Bonjour Alice, j'ai bien pris en compte votre demande. Je passerai à votre bureau cet après-midi pour tester un autre câble HDMI."
                )
            )

            val ticket3Id = ticketDao.insertTicket(
                Ticket(
                    titre = "Problème d'accès à l'intranet",
                    description = "Mon mot de passe semble expiré ou bloqué sur l'intranet de l'entreprise, je n'arrive pas à me connecter.",
                    categorieId = catAccesId,
                    priorite = Priorite.FAIBLE,
                    statut = Statut.RESOLU,
                    utilisateurId = emp1Id,
                    technicienId = techId,
                    dateResolution = System.currentTimeMillis() - 7200000 // Il y a 2h
                )
            )
            historyDao.insertHistory(
                Historique(
                    ticketId = ticket3Id,
                    utilisateurId = emp1Id,
                    typeAction = "Création",
                    descriptionAction = "Ticket d'incident créé"
                )
            )
            historyDao.insertHistory(
                Historique(
                    ticketId = ticket3Id,
                    utilisateurId = techId,
                    typeAction = "Affectation",
                    descriptionAction = "Ticket pris en charge par Pierre Tech"
                )
            )
            historyDao.insertHistory(
                Historique(
                    ticketId = ticket3Id,
                    utilisateurId = techId,
                    typeAction = "Statut",
                    descriptionAction = "Statut modifié de Nouveau à En cours"
                )
            )
            historyDao.insertHistory(
                Historique(
                    ticketId = ticket3Id,
                    utilisateurId = techId,
                    typeAction = "Statut",
                    descriptionAction = "Statut modifié de En cours à Résolu"
                )
            )
            commentDao.insertComment(
                Commentaire(
                    ticketId = ticket3Id,
                    utilisateurId = techId,
                    contenu = "Le mot de passe a été réinitialisé. Vous avez dû recevoir un email temporaire pour configurer votre nouvel accès."
                )
            )
        }
    }
}
