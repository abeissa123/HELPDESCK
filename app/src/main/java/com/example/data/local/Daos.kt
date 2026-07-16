package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter
    fun fromRole(role: Role): String = role.name

    @TypeConverter
    fun toRole(value: String): Role = Role.valueOf(value)

    @TypeConverter
    fun fromPriorite(priorite: Priorite): String = priorite.name

    @TypeConverter
    fun toPriorite(value: String): Priorite = Priorite.valueOf(value)

    @TypeConverter
    fun fromStatut(statut: Statut): String = statut.name

    @TypeConverter
    fun toStatut(value: String): Statut = Statut.valueOf(value)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM utilisateurs WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): Utilisateur?

    @Query("SELECT * FROM utilisateurs WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): Utilisateur?

    @Query("SELECT * FROM utilisateurs ORDER BY nom ASC")
    fun getAllUsersFlow(): Flow<List<Utilisateur>>

    @Query("SELECT * FROM utilisateurs WHERE role = 'TECHNICIEN' OR role = 'ADMIN' ORDER BY nom ASC")
    fun getTechniciansAndAdminsFlow(): Flow<List<Utilisateur>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: Utilisateur): Long

    @Update
    suspend fun updateUser(user: Utilisateur)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY nom ASC")
    fun getAllCategoriesFlow(): Flow<List<Categorie>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Categorie): Long

    @Delete
    suspend fun deleteCategory(category: Categorie)
}

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY dateCreation DESC")
    fun getAllTicketsFlow(): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE utilisateurId = :employeeId ORDER BY dateCreation DESC")
    fun getTicketsByEmployeeFlow(employeeId: Long): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    fun getTicketByIdFlow(id: Long): Flow<Ticket?>

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    suspend fun getTicketById(id: Long): Ticket?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: Ticket): Long

    @Update
    suspend fun updateTicket(ticket: Ticket)

    @Delete
    suspend fun deleteTicket(ticket: Ticket)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM commentaires WHERE ticketId = :ticketId ORDER BY dateCreation ASC")
    fun getCommentsForTicketFlow(ticketId: Long): Flow<List<Commentaire>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(commentaire: Commentaire): Long
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM historique WHERE ticketId = :ticketId ORDER BY dateAction ASC")
    fun getHistoryForTicketFlow(ticketId: Long): Flow<List<Historique>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(historique: Historique): Long
}

@Dao
interface EmailLogDao {
    @Query("SELECT * FROM emails_log ORDER BY dateEnvoi DESC")
    fun getAllEmailsFlow(): Flow<List<EmailLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: EmailLog): Long

    @Query("DELETE FROM emails_log")
    suspend fun clearAllEmails()
}
