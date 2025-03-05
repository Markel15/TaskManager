package com.example.proyecto;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;
import java.util.Objects;

public class DeadlineReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), "avisoPersonalizado")) {
            int tareaId = intent.getIntExtra("tareaId", -1);
            String tareaTitulo = intent.getStringExtra("tareaTitulo");

            // configurar idioma ya que el contexto del BroadcastReceiver no hereda automáticamente la configuración de la aplicación
            SharedPreferences prefs = context.getSharedPreferences("MiAppPrefs", Context.MODE_PRIVATE);
            String idioma = prefs.getString("idioma", "es");  // "es" como valor predeterminado si no se encuentra

            Locale locale = new Locale(idioma);  // Usamos el idioma de las preferencias
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.setLocale(locale);
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("deadline_channel", "Deadline Notifications", NotificationManager.IMPORTANCE_HIGH);
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "deadline_channel")
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle(context.getString(R.string.Atencion))
                    .setContentText(context.getString(R.string.la_tarea) + " " + tareaTitulo + " " + context.getString(R.string.vence_24))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            notificationManager.notify(tareaId, builder.build());
        }
    }
}
