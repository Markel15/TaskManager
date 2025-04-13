package com.example.proyecto;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TaskWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_AUTO_UPDATE = "com.example.ACTION_AUTO_UPDATE_WIDGET";

    // Código adaptado del tema 17 de eGela: Widgets
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            actualizarWidget(context, appWidgetManager, widgetId);
        }
    }

    // Método que actualiza una instancia concreta del widget
    public static void actualizarWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        // Obtén la tarea aleatoria. Puedes hacer uso de tu clase miBD para acceder a la base de datos.
        String tareaTexto = obtenerTareaAleatoria(context);

        // Crea y configura las RemoteViews del widget
        @SuppressLint("RemoteViewLayout") RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task);
        views.setTextViewText(R.id.textViewTask, tareaTexto);

        // Actualiza el widget
        appWidgetManager.updateAppWidget(widgetId, views);
    }

    // Método para configurar la alarma al agregar la primera instancia del widget
    @Override
    public void onEnabled(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TaskWidgetProvider.class);
        intent.setAction(ACTION_AUTO_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        // Intervalo de 5 minutos (5 * 60 * 1000 ms)
        long interval = 1 * 60 * 1000;
        // Programar la alarma para que se repita cada 5 minutos
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, interval, pendingIntent);
    }

    // Cancelar la alarma cuando se elimina la última instancia del widget
    @Override
    public void onDisabled(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TaskWidgetProvider.class);
        intent.setAction(ACTION_AUTO_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    // Método para recibir eventos extra (por ejemplo, la acción de refrescar)
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_AUTO_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, TaskWidgetProvider.class);
            int[] widgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, widgetIds);
        }
    }

    // Método auxiliar para obtener una tarea aleatoria de las tareas no completadas
    private static String obtenerTareaAleatoria(Context context) {
        // Abre la base de datos usando tu SQLiteOpenHelper
        miBD dbHelper = miBD.getMiBD(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SharedPreferences prefs = context.getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("idDeUsuario", -1);

        // Consulta las tareas no completadas del usuario
        Cursor cursor = db.rawQuery("SELECT titulo FROM tareas WHERE completado = 0 AND usuarioId = ?", new String[]{ String.valueOf(userId)});
        List<String> tareas = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String titulo = cursor.getString(cursor.getColumnIndex("titulo"));
                tareas.add(titulo);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Si no hay tareas pendientes, mostrar un mensaje por defecto.
        if (tareas.isEmpty()) {
            return "No hay tareas pendientes";
        } else {
            // Seleccionar una tarea al azar
            int indice = new Random().nextInt(tareas.size());
            return tareas.get(indice);
        }
    }
}

