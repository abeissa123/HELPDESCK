package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log

class NotificationHelper(private val context: Context) {

    private val channelId = "helpdesk_notifications"
    private val channelName = "Notifications Helpdesk"
    private val channelDescription = "Notifications pour les événements de tickets et de commentaires"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Fait vibrer le téléphone.
     * @param pattern Un tableau de durées en millisecondes (silence, vibration, silence, vibration...)
     * @param repeat L'indice à partir duquel répéter le motif (-1 pour ne pas répéter)
     */
    fun vibrate(pattern: LongArray, repeat: Int = -1) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, repeat)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Erreur lors de la vibration", e)
        }
    }

    /**
     * Fait vibrer le téléphone pendant une durée simple.
     */
    fun vibrateSimple(durationMillis: Long = 200) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMillis)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Erreur lors de la vibration", e)
        }
    }

    /**
     * Déclenche une vibration correspondant à un événement particulier.
     */
    fun vibrateForEvent(type: NotificationType) {
        when (type) {
            NotificationType.TICKET_CREATION -> {
                // Vibration courte puis une autre un peu plus longue
                vibrate(longArrayOf(0, 100, 100, 250))
            }
            NotificationType.TICKET_ASSIGNED -> {
                // Trois vibrations courtes
                vibrate(longArrayOf(0, 100, 50, 100, 50, 100))
            }
            NotificationType.TICKET_IN_PROGRESS -> {
                // Vibration simple standard
                vibrateSimple(300)
            }
            NotificationType.TICKET_RESOLVED -> {
                // Deux vibrations longues de célébration / soulagement
                vibrate(longArrayOf(0, 300, 100, 400))
            }
            NotificationType.COMMENT_ADDED -> {
                // Vibration très courte discrète
                vibrateSimple(80)
            }
            NotificationType.ACCOUNT_APPROVED -> {
                // Vibration joyeuse d'approbation (vibration moyenne, pause courte, vibration moyenne, pause courte, vibration longue)
                vibrate(longArrayOf(0, 150, 100, 150, 100, 450))
            }
        }
    }

    /**
     * Affiche une notification système pour l'approbation du compte d'un utilisateur, avec vibration.
     */
    fun showApprovalNotification(userName: String, userEmail: String) {
        val title = "Compte Approuvé 🎉"
        val message = "Félicitations, le compte de $userName ($userEmail) a été approuvé avec succès par l'administrateur !"
        showNotification(2000 + userEmail.hashCode(), title, message)
        vibrateForEvent(NotificationType.ACCOUNT_APPROVED)
    }

    /**
     * Affiche une notification dans la barre d'état.
     */
    fun showNotification(id: Int, title: String, message: String) {
        try {
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(id, builder.build())
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Permission manquante pour afficher la notification", e)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Erreur lors de l'affichage de la notification", e)
        }
    }
}

enum class NotificationType {
    TICKET_CREATION,
    TICKET_ASSIGNED,
    TICKET_IN_PROGRESS,
    TICKET_RESOLVED,
    COMMENT_ADDED,
    ACCOUNT_APPROVED
}
