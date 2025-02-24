package com.example.proyecto;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    miBD miDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        miDb = miBD.getMiBD(this);
        // Recuperar el ID del usuario autenticado
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("idDeUsuario", -1);
        if(userId == -1){  // Se ha cerrado la sesión, volver a la página de inicio de sesión
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        // Obtener las tareas del usuario
        List<Tarea> taskList = obtenerTarasParaUsu(userId);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.labarra));
        // Configurar los elementos a mostrar
        RecyclerView lalista= findViewById(R.id.recyclerViewTareas);
        TareaAdapter adapter = new TareaAdapter(taskList);
        lalista.setAdapter(adapter);
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary)); // Usar el color de la Toolbar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.definicion_menu,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.opcion1) {
            // Acción para "Favorito"
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @SuppressLint("Range")  // Omitir los warnings en los que las columnas no existan, la base de datos se va a crear con esos nombres
    public List<Tarea> obtenerTarasParaUsu(int usuId) {
        List<Tarea> tareas = new ArrayList<>();
        SQLiteDatabase bd = miDb.getReadableDatabase();
        Cursor cursor = bd.rawQuery("SELECT * FROM tareas WHERE usuarioId = ? ORDER BY FechaFinalizacion ASC",
                new String[]{String.valueOf(usuId)});
        if (cursor.moveToFirst()) {
            do {
                Tarea tarea = new Tarea();
                tarea.setId(cursor.getInt(cursor.getColumnIndex("id")));
                tarea.setTitulo(cursor.getString(cursor.getColumnIndex("titulo")));
                tarea.setDescripcion(cursor.getString(cursor.getColumnIndex("descripcion")));
                tarea.setFechaCreacion(cursor.getLong(cursor.getColumnIndex("fechaCreacion")));
                tarea.setFechaFinalizacion(cursor.getLong(cursor.getColumnIndex("FechaFinalizacion")));
                tarea.setCompletado(cursor.getInt(cursor.getColumnIndex("completado")) == 1);
                tarea.setPrioridad(cursor.getInt(cursor.getColumnIndex("prioridad")));
                tareas.add(tarea);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tareas;
    }


}