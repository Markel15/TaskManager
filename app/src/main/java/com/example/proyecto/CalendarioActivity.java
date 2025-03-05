package com.example.proyecto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;

public class CalendarioActivity extends BaseActivity implements CalendarFragment.OnDateSelectedListener {

    private int userId;  // ID del usuario autenticado

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Recuperar el ID del usuario desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        userId = prefs.getInt("idDeUsuario", -1);
        if (userId == -1) {
            // Si no hay usuario, redirigir a LoginActivity
            Intent intent = new Intent(CalendarioActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Cargar ambos fragmentos
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container_calendar, new CalendarFragment())
                    .commitNow();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container_tasks, ListaTareasFragment.newInstance(userId))
                    .commitNow();
        }
    }

    @Override
    public void onDateSelected(Date fecha) {
        // Calcular el inicio y fin del día seleccionado
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long comienzoDia = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        long finDia = cal.getTimeInMillis() - 1;

        ListaTareasFragment fragment = (ListaTareasFragment) getSupportFragmentManager().findFragmentById(R.id.container_tasks);
        if (fragment != null) {
            fragment.actualizarTareas(comienzoDia, finDia);
        }
    }

}
