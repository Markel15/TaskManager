package com.example.proyecto;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("MiAppPrefs", Context.MODE_PRIVATE);
            int userId = prefs.getInt("idDeUsuario", -1);

            // Si no hay un usuario válido, no se reprograman las notificaciones
            if (userId == -1) {
                return;
            }
            miBD baseDatos = miBD.getMiBD(context);
            SQLiteDatabase db = baseDatos.getReadableDatabase();

            long now = System.currentTimeMillis();
            // Consulta para obtener tareas pendientes cuya fecha finalización es futura
            String query = "SELECT id, titulo, FechaFinalizacion FROM tareas WHERE completado = 0 AND FechaFinalizacion > ? AND usuarioId = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(now), String.valueOf(userId)});

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    @SuppressLint("Range") int tareaId = cursor.getInt(cursor.getColumnIndex("id"));
                    @SuppressLint("Range") String tareaTitulo = cursor.getString(cursor.getColumnIndex("titulo"));
                    @SuppressLint("Range") long fechaFinalizacion = cursor.getLong(cursor.getColumnIndex("FechaFinalizacion"));

                    // Se reprograma la notificación; NotificacionAux se encarga de verificar el tiempo restante
                    NotificacionAux.programarNotificacion(context, tareaId, tareaTitulo, fechaFinalizacion);
                }
                cursor.close();
            }
            db.close();
        }
    }
}
