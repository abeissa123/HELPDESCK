package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Role {
    EMPLOYE,
    TECHNICIEN,
    ADMIN
}

enum class UserStatus {
    EN_ATTENTE,
    VALIDE,
    REFUSE;

    fun getDisplayName(): String {
        return when (this) {
            EN_ATTENTE -> "En attente"
            VALIDE -> "Validé / Approuvé"
            REFUSE -> "Refusé"
        }
    }
}

enum class Priorite {
    FAIBLE,
    NORMALE,
    URGENTE;

    fun getDisplayName(): String {
        return when (this) {
            FAIBLE -> "Basse"
            NORMALE -> "Moyenne"
            URGENTE -> "Haute"
        }
    }
}

enum class Statut {
    NOUVEAU,
    EN_COURS,
    RESOLU,
    FERME
}

@Entity(
    tableName = "utilisateurs",
    indices = [Index(value = ["email"], unique = true)]
)
data class Utilisateur(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val email: String,
    val motDePasse: String,
    val role: Role,
    val service: String? = null,
    val notifVibration: Boolean = true,
    val notifEmail: Boolean = true,
    val statut: UserStatus = UserStatus.VALIDE
)

@Entity(tableName = "categories")
data class Categorie(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String
)

@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titre: String,
    val description: String,
    val categorieId: Long,
    val priorite: Priorite,
    val statut: Statut = Statut.NOUVEAU,
    val utilisateurId: Long, // Créateur du ticket
    val technicienId: Long? = null, // Technicien assigné
    val dateCreation: Long = System.currentTimeMillis(),
    val dateResolution: Long? = null,
    val satisfactionNote: Int? = null // Évaluation de satisfaction (1 à 5)
)

@Entity(tableName = "commentaires")
data class Commentaire(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticketId: Long,
    val utilisateurId: Long, // Auteur du commentaire
    val contenu: String,
    val dateCreation: Long = System.currentTimeMillis()
)

@Entity(tableName = "historique")
data class Historique(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticketId: Long,
    val utilisateurId: Long, // Auteur de l'action
    val typeAction: String, // Ex: "Création", "Changement de statut", "Affectation"
    val descriptionAction: String, // Ex: "Statut modifié de Nouveau à En cours"
    val dateAction: Long = System.currentTimeMillis()
)

@Entity(tableName = "emails_log")
data class EmailLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val destinataire: String,
    val sujet: String,
    val contenu: String,
    val dateEnvoi: Long = System.currentTimeMillis(),
    val lu: Boolean = false
)
