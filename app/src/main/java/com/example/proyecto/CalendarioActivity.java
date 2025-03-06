package com.example.proyecto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.Calendar;
import java.util.Date;

public class CalendarioActivity extends BaseActivity implements CalendarFragment.OnDateSelectedListener {

    private int userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24px);
        }

        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        userId = prefs.getInt("idDeUsuario", -1);
        if (userId == -1) {
            Intent intent = new Intent(CalendarioActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container_calendar, new CalendarFragment()).commitNow();
            getSupportFragmentManager().beginTransaction().replace(R.id.container_tasks, ListaTareasFragment.newInstance(userId)).commitNow();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();// Vuelve a la actividad anterior
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDateSelected(Date fecha) {
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
