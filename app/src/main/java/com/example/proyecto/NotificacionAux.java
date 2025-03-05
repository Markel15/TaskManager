package com.example.proyecto;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class NotificacionAux {

    public static void programarNotificacion(Context context, int tareaId, String tareaTitulo, long fechaFinalizacion) {
        long oneDayInMillis = 24 * 60 * 60 * 1000;
        // Calculamos el momento de notificar: 24 horas antes de la fecha de finalización
        long fechaDisparo = fechaFinalizacion - oneDayInMillis;
        long now = System.currentTimeMillis();

        // Si la fecha para disparar la notificacion ya pasó, por ejemplo, la tarea se crea con menos de 24h de antelación, esperamos 1 segundo para notificar
        if (fechaDisparo < now) {
            fechaDisparo = now + 1000;  // notifica en 1 segundo
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DeadlineReceiver.class);
        intent.setAction("avisoPersonalizado");
        intent.putExtra("tareaId", tareaId);
        intent.putExtra("tareaTitulo", tareaTitulo);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, tareaId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            // Comprobación de permisos extraída de https://developer.android.com/about/versions/14/changes/schedule-exact-alarms?hl=es-419
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fechaDisparo, pendingIntent);
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, fechaDisparo, pendingIntent);
                    }
                } else {
                    if (context instanceof Activity) {
                        Intent intentAjustes = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        ((Activity) context).startActivity(intentAjustes);
                    }
                }
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fechaDisparo, pendingIntent);
            }
            else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, fechaDisparo, pendingIntent);
            }
        }
    }

    public static void cancelarNotificacion(Context context, int taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DeadlineReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
